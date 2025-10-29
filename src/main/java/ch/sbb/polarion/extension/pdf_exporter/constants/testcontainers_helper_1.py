import os
import re
import shutil
import subprocess
import tempfile
import time
from dataclasses import dataclass
from datetime import datetime, timedelta

import docker
from docker.models.networks import Network
from testcontainers.core.container import DockerContainer

from python_wzu_utils.polarion.api.extension_api_factory import ExtensionApiFactory  # pylint: disable=E0402
from python_wzu_utils.polarion.api.generic import PolarionRestApiConnection  # pylint: disable=E0402

from ....common.util_argparse import get_script_arguments  # pylint: disable=E0402

TIMEOUT_IN_SEC = 120
INITIAL_LOGIN = "admin"
POLARION_EXTENSIONS_PATH = "/opt/polarion/polarion/extensions/"
DEFAULT_ADMIN_UTILITY_VERSION = "1.8.0"
WEASYPRINT_NETWORK = "test-weasyprint-network"


class TestContainersHelper:
    polarion_container: DockerContainer = None
    weasyprint_service_container: DockerContainer = None
    network: Network = None
    systest_extensions_root = None

    def create_test_container_if_required(self, extension_name):
        args = get_script_arguments(None)
        parameters = TestContainersHelper.get_parameters(args)
        if parameters.weasyprint_service_image_name is not None:
            self.create_network(WEASYPRINT_NETWORK)
            weasyprint_service_endpoint = self.create_weasyprint_service_container(parameters)
        else:
            weasyprint_service_endpoint = None

        if parameters.polarion_image_name is not None:
            app_url, app_token = self.create_polarion_container(extension_name, parameters, weasyprint_service_endpoint)
            os.environ["APP_URL"] = app_url
            os.environ["APP_TOKEN"] = app_token

    @staticmethod
    def get_parameters(args):
        polarion_image_name = TestContainersHelper.get_parameter("TC_POLARION_IMAGE_NAME", args.tc_polarion_image_name)
        weasyprint_service_image_name = TestContainersHelper.get_parameter("TC_WEASYPRINT_SERVICE_IMAGE_NAME", args.tc_weasyprint_service_image_name)
        extension_version = TestContainersHelper.get_parameter("TC_EXTENSION_VERSION", args.tc_extension_version)
        additional_bundles_artifacts = TestContainersHelper.get_parameter("TC_ADDITIONAL_BUNDLES", args.tc_additional_bundles)
        admin_utility_version = TestContainersHelper.get_parameter("TC_ADMIN_UTILITY_VERSION", args.tc_admin_utility_version)
        additional_bundles_list = TestContainersHelper.parse_additional_bundles(additional_bundles_artifacts)
        return TestContainerParameters(
            polarion_image_name=polarion_image_name,
            weasyprint_service_image_name=weasyprint_service_image_name,
            extension_version=extension_version,
            additional_bundles=additional_bundles_list,
            admin_utility_version=admin_utility_version,
        )

    @staticmethod
    def get_parameter(env_param_name, script_argument_value):
        param = os.environ.get(env_param_name)
        if not param:
            param = script_argument_value
        return param

    @staticmethod
    def parse_additional_bundles(bundles):
        if bundles is None:
            return None
        bundles_list = bundles.split(",")
        artifacts_info_list = []
        for bundle in bundles_list:
            artifact_details = bundle.split(":")
            artifacts_info_list.append(ArtifactInfo(group_id=artifact_details[0], artifact_id=artifact_details[1], version=artifact_details[2]))
        return artifacts_info_list

    def create_network(self, network_name):
        client = docker.from_env()
        self.network = client.networks.create(network_name, driver="bridge")

    def create_weasyprint_service_container(self, parameters):
        container_name = "test-weasyprint-service-container"
        port = 9080
        try:
            print(f"Starting {container_name} ...")
            container = DockerContainer(image=parameters.weasyprint_service_image_name).with_bind_ports(port).with_name(container_name)
            container.start()
            self.network.connect(container.get_wrapped_container().short_id)

            base_url = f"http://{container_name}:{port}"
            print("Weasyprint service in bridge network is accessible through: " + base_url)
            self.weasyprint_service_container = container
            return base_url
        except Exception as ex:
            self.tear_down()
            raise ContainerSetupException("Cannot setup Weasyprint Service container: " + str(ex), ex)

    def create_polarion_container(self, extension_name, parameters, weasyprint_service_endpoint):
        container_name = "test-polarion-container"
        port = 80
        try:
            print(f"Starting {container_name} ...")
            self.prepare_systest_extensions(extension_name, parameters)
            # Define the custom Docker container using your image
            container = (
                DockerContainer(image=parameters.polarion_image_name)
                .with_bind_ports(port)
                .with_name(container_name)
                .with_volume_mapping(self.systest_extensions_root, POLARION_EXTENSIONS_PATH)
            )
            if weasyprint_service_endpoint:
                container = container.with_env("WEASYPRINT_SERVICE_ENDPOINT", weasyprint_service_endpoint)

            container.start()
            if self.network:
                self.network.connect(container.get_wrapped_container().short_id)

            exposed_port = container.get_exposed_port(port)
            base_url = f"http://localhost:{exposed_port}"
            print("Polarion is accessible through: " + base_url)
            self.polarion_container = container

            print("Waiting for Polarion http connection ...")
            time.sleep(10)

            # Setup container: activate license and create token
            token = self.setup_polarion_container(base_url)

            return base_url, token
        except Exception as ex:
            self.tear_down()
            raise ContainerSetupException("Cannot setup Polarion container: " + str(ex), ex)

    def prepare_systest_extensions(self, extension_name, parameters):
        systest_extensions_jars_path = self.create_host_extensions_path()
        systest_extensions = [
            ArtifactInfo("ch.sbb.polarion.extensions", f"ch.sbb.polarion.extension.{extension_name}", parameters.extension_version),
            ArtifactInfo("ch.sbb.polarion.extensions", "ch.sbb.polarion.extension.admin-utility", parameters.admin_utility_version or DEFAULT_ADMIN_UTILITY_VERSION),
        ]
        if parameters.additional_bundles:
            systest_extensions.extend(parameters.additional_bundles)

        for systest_extension in systest_extensions:
            self.copy_dependency(systest_extensions_jars_path, systest_extension.group_id, systest_extension.artifact_id, systest_extension.version)

    def tear_down(self):
        # Stop and remove the container after the tests
        if self.weasyprint_service_container is not None and self.weasyprint_service_container.get_wrapped_container() is not None:
            print("Stopping Weasyprint Servce test container ...")
            self.weasyprint_service_container.stop()
        if self.polarion_container is not None and self.polarion_container.get_wrapped_container() is not None:
            print("Stopping Polarion test container and cleaning temp directory ...")
            self.polarion_container.stop()
        if self.systest_extensions_root is not None and os.path.exists(self.systest_extensions_root):
            shutil.rmtree(self.systest_extensions_root)
        if self.network:
            self.network.remove()

    @staticmethod
    def setup_polarion_container(base_url):
        polarion_connection = PolarionRestApiConnection(url=base_url, username=INITIAL_LOGIN, password=INITIAL_LOGIN)
        polarion_admin_utility_api = ExtensionApiFactory.get_extension_api_by_name(extension_name="admin-utility", polarion_connection=polarion_connection)
        TestContainersHelper.wait_for_start_and_activate(polarion_admin_utility_api)
        token = TestContainersHelper.issue_security_token(polarion_admin_utility_api)
        return token

    @staticmethod
    def wait_for_start_and_activate(polarion_admin_utility_api):
        polarion_admin_utility_api.polarion_connection.set_print_error(False)
        activate_response = None
        start = time.time()
        try:
            while time.time() - start < TIMEOUT_IN_SEC:
                activate_response = polarion_admin_utility_api.activate_trial_license()
                print("Waiting for Polarion container readiness, status: " + str(activate_response.status_code))
                if activate_response.status_code == 503:
                    time.sleep(1)
                    continue
                elif activate_response.status_code == 200:
                    TestContainersHelper.check_default_activation_response(activate_response)
                    break
                else:
                    error_message = activate_response.content.decode("utf-8") if activate_response.content is not None else ""
                    raise PolarionStartupException("Polarion license activation failure: status = " + str(activate_response.status_code) + "; message = " + error_message)

            if activate_response.status_code != 200:
                raise PolarionStartupException("Polarion start timeout")
        finally:
            polarion_admin_utility_api.polarion_connection.set_print_error(True)

    @staticmethod
    def check_default_activation_response(activate_response):
        content_type = activate_response.headers.get("Content-Type")
        if (
            content_type is not None
            and activate_response.headers.get("Content-Type").__contains__("text/plain")
            and activate_response.content is not None
            and activate_response.content.decode().__contains__('"activated":true')
        ):
            raise PolarionStartupException("admin-utility extension is not available in Polarion")

    @staticmethod
    def issue_security_token(polarion_admin_utility_api):
        now = datetime.now()
        now_plus_10 = now + timedelta(minutes=5)
        token = polarion_admin_utility_api.create_security_token("test", now_plus_10.strftime("%Y-%m-%dT%H:%M:%SZ"))
        return token

    @staticmethod
    def copy_dependency(systest_extensions_jars_path, group_id, artifact_id, version):
        mvn_path = TestContainersHelper.get_maven_location()
        if not version:
            version = TestContainersHelper.get_latest_artifact_version(group_id, artifact_id)
        command = f"{mvn_path} dependency:copy -Dartifact={group_id}:{artifact_id}:{version} -DoutputDirectory={systest_extensions_jars_path}"
        result = subprocess.run(command, shell=True)
        if result.returncode != 0:
            raise MavenException(f"Failed to copy artifact {group_id}:{artifact_id}:{version}, code: " + str(result.returncode))

    @staticmethod
    def get_latest_artifact_version(group_id, artifact_id):
        repo_location = TestContainersHelper.get_maven_repo_location()
        artifact_dir_content = os.listdir(os.path.join(repo_location, group_id.replace(".", os.sep), artifact_id))
        regex = re.compile("^(?:\\d+\\.)?(?:\\d+\\.)?(?:\\*|\\d+)(?:-SNAPSHOT)?$")
        versions = list(filter(lambda dir_name: regex.match(dir_name), artifact_dir_content))
        versions.sort(reverse=True)
        return versions[0]

    @staticmethod
    def get_maven_repo_location():
        mvn_location = TestContainersHelper.get_maven_location()
        command = f"{mvn_location} -q help:evaluate -Dexpression=settings.localRepository -DforceStdout=true"
        result = subprocess.run(command, shell=True, stdout=subprocess.PIPE, text=True)
        if result.returncode != 0:
            raise MavenException("Cannot determine local maven repository, code: " + str(result.returncode))
        return os.path.expanduser(result.stdout.strip())

    @staticmethod
    def get_maven_location():
        mvn_path = shutil.which("mvn")
        if not mvn_path:
            raise MavenException("Maven is not available, please, install mvn and try again.")
        return mvn_path

    def create_host_extensions_path(self):
        self.systest_extensions_root = f"{tempfile.gettempdir()}/systest"
        systest_extensions_jars_path = f"{self.systest_extensions_root}/sbb-extensions/eclipse/plugins"
        if os.path.exists(systest_extensions_jars_path):
            shutil.rmtree(systest_extensions_jars_path)
        os.makedirs(systest_extensions_jars_path)
        return systest_extensions_jars_path


@dataclass
class TestContainerParameters:
    """Class for keeping command line arguments."""

    polarion_image_name: str
    weasyprint_service_image_name: str
    extension_version: str
    additional_bundles: []
    admin_utility_version: str


@dataclass
class ArtifactInfo:
    """Class for keeping extension information."""

    group_id: str
    artifact_id: str
    version: str


class ContainerSetupException(Exception):
    pass


class PolarionStartupException(Exception):
    pass


class MavenException(Exception):
    pass
