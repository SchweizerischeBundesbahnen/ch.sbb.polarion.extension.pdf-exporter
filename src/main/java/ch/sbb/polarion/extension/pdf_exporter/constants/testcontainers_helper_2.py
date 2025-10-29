import logging
import os
import pathlib
import re
import shutil
import subprocess  # noqa: S404 - subprocess is required for Maven integration
import tempfile
import time
from dataclasses import dataclass
from datetime import datetime, timedelta
from typing import Any

import docker
from docker.models.networks import Network
from testcontainers.core.container import DockerContainer

from python_sbb_polarion.core.base import PolarionRestApiConnection  # pylint: disable=E0402
from python_sbb_polarion.core.factory import ExtensionApiFactory  # pylint: disable=E0402
from python_sbb_polarion.util.argparse import get_script_arguments  # pylint: disable=E0402


logger = logging.getLogger(__name__)


TIMEOUT_IN_SEC = 120
INITIAL_LOGIN = "admin"
POLARION_EXTENSIONS_PATH = "/opt/polarion/polarion/extensions/"
DEFAULT_ADMIN_UTILITY_VERSION = "1.8.0"
WEASYPRINT_NETWORK = "test-weasyprint-network"


class TestContainersHelper:
    """Helper for managing Polarion test containers"""

    polarion_container: DockerContainer | None = None
    weasyprint_service_container: DockerContainer | None = None
    network: Network | None = None
    systest_extensions_root: str | None = None

    def create_test_container_if_required(self, extension_name: str) -> None:
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
    def get_parameters(args: Any) -> "TestContainerParameters":
        polarion_image_name = TestContainersHelper.get_parameter("TC_POLARION_IMAGE_NAME", args.tc_polarion_image_name)
        weasyprint_service_image_name = TestContainersHelper.get_parameter("TC_WEASYPRINT_SERVICE_IMAGE_NAME", args.tc_weasyprint_service_image_name)
        extension_version = TestContainersHelper.get_parameter("TC_EXTENSION_VERSION", args.tc_extension_version)
        additional_bundles_artifacts = TestContainersHelper.get_parameter("TC_ADDITIONAL_BUNDLES", args.tc_additional_bundles)
        admin_utility_version = TestContainersHelper.get_parameter("TC_ADMIN_UTILITY_VERSION", args.tc_admin_utility_version)
        additional_bundles_list = TestContainersHelper.parse_additional_bundles(additional_bundles_artifacts)
        return TestContainerParameters(
            polarion_image_name=polarion_image_name or "",
            weasyprint_service_image_name=weasyprint_service_image_name or "",
            extension_version=extension_version or "",
            additional_bundles=additional_bundles_list,
            admin_utility_version=admin_utility_version or "",
        )

    @staticmethod
    def get_parameter(env_param_name: str, script_argument_value: str | None) -> str | None:
        param = os.environ.get(env_param_name)
        if not param:
            param = script_argument_value
        return param

    @staticmethod
    def parse_additional_bundles(bundles: str | None) -> list["ArtifactInfo"] | None:
        if bundles is None:
            return None
        bundles_list = bundles.split(",")
        artifacts_info_list = []
        for bundle in bundles_list:
            artifact_details = bundle.split(":")
            artifacts_info_list.append(ArtifactInfo(group_id=artifact_details[0], artifact_id=artifact_details[1], version=artifact_details[2]))
        return artifacts_info_list

    def create_network(self, network_name: str) -> None:
        client = docker.from_env()
        self.network = client.networks.create(network_name, driver="bridge")

    def create_weasyprint_service_container(self, parameters: "TestContainerParameters") -> str:
        container_name = "test-weasyprint-service-container"
        port = 9080
        try:
            logger.info("Starting %s ...", container_name)
            container = DockerContainer(image=parameters.weasyprint_service_image_name).with_bind_ports(port).with_name(container_name)
            container.start()
            if self.network:
                self.network.connect(container.get_wrapped_container().short_id)

            base_url = f"http://{container_name}:{port}"
            logger.info("Weasyprint service in bridge network is accessible through: %s", base_url)
            self.weasyprint_service_container = container
        except Exception as ex:
            self.tear_down()
            raise ContainerSetupError("Cannot setup Weasyprint Service container: " + str(ex)) from ex
        else:
            return base_url

    def create_polarion_container(self, extension_name: str, parameters: "TestContainerParameters", weasyprint_service_endpoint: str | None) -> tuple[str, str]:
        container_name = "test-polarion-container"
        port = 80
        try:
            logger.info("Starting %s ...", container_name)
            self.prepare_systest_extensions(extension_name, parameters)
            # Define the custom Docker container using your image
            container = DockerContainer(image=parameters.polarion_image_name).with_bind_ports(port).with_name(container_name).with_volume_mapping(self.systest_extensions_root, POLARION_EXTENSIONS_PATH)
            if weasyprint_service_endpoint:
                container = container.with_env("WEASYPRINT_SERVICE_ENDPOINT", weasyprint_service_endpoint)

            container.start()
            if self.network:
                self.network.connect(container.get_wrapped_container().short_id)

            exposed_port = container.get_exposed_port(port)
            base_url = f"http://localhost:{exposed_port}"
            logger.info("Polarion is accessible through: %s", base_url)
            self.polarion_container = container

            logger.debug("Waiting for Polarion http connection ...")
            time.sleep(10)

            # Setup container: activate license and create token
            token = self.setup_polarion_container(base_url)
        except Exception as ex:
            self.tear_down()
            raise ContainerSetupError("Cannot setup Polarion container: " + str(ex)) from ex
        else:
            return base_url, token

    def prepare_systest_extensions(self, extension_name: str, parameters: "TestContainerParameters") -> None:
        systest_extensions_jars_path = self.create_host_extensions_path()
        systest_extensions = [
            ArtifactInfo("ch.sbb.polarion.extensions", f"ch.sbb.polarion.extension.{extension_name}", parameters.extension_version),
            ArtifactInfo("ch.sbb.polarion.extensions", "ch.sbb.polarion.extension.admin-utility", parameters.admin_utility_version or DEFAULT_ADMIN_UTILITY_VERSION),
        ]
        if parameters.additional_bundles:
            systest_extensions.extend(parameters.additional_bundles)

        for systest_extension in systest_extensions:
            self.copy_dependency(systest_extensions_jars_path, systest_extension.group_id, systest_extension.artifact_id, systest_extension.version)

    def tear_down(self) -> None:
        # Stop and remove the container after the tests
        if self.weasyprint_service_container is not None and self.weasyprint_service_container.get_wrapped_container() is not None:
            logger.info("Stopping Weasyprint Servce test container ...")
            self.weasyprint_service_container.stop()
        if self.polarion_container is not None and self.polarion_container.get_wrapped_container() is not None:
            logger.info("Stopping Polarion test container and cleaning temp directory ...")
            self.polarion_container.stop()
        if self.systest_extensions_root is not None and pathlib.Path(self.systest_extensions_root).exists():
            shutil.rmtree(self.systest_extensions_root)
        if self.network:
            self.network.remove()

    @staticmethod
    def setup_polarion_container(base_url: str) -> str:
        polarion_connection = PolarionRestApiConnection(url=base_url, username=INITIAL_LOGIN, password=INITIAL_LOGIN)
        polarion_admin_utility_api = ExtensionApiFactory.get_extension_api_by_name(extension_name="admin-utility", polarion_connection=polarion_connection)
        TestContainersHelper.wait_for_start_and_activate(polarion_admin_utility_api)
        return TestContainersHelper.issue_security_token(polarion_admin_utility_api)

    @staticmethod
    def wait_for_start_and_activate(polarion_admin_utility_api: Any) -> None:
        polarion_admin_utility_api.polarion_connection.set_print_error(False)
        activate_response: Any = None
        start = time.time()
        try:
            while time.time() - start < TIMEOUT_IN_SEC:
                activate_response = polarion_admin_utility_api.activate_trial_license()
                logger.debug("Waiting for Polarion container readiness, status: %s", activate_response.status_code)
                if activate_response.status_code == 503:
                    time.sleep(1)
                    continue
                if activate_response.status_code == 200:
                    TestContainersHelper.check_default_activation_response(activate_response)
                    break
                error_message = activate_response.content.decode("utf-8") if activate_response.content is not None else ""
                raise PolarionStartupError("Polarion license activation failure: status = " + str(activate_response.status_code) + "; message = " + error_message)

            if activate_response is not None and activate_response.status_code != 200:
                raise PolarionStartupError("Polarion start timeout")
        finally:
            polarion_admin_utility_api.polarion_connection.set_print_error(True)

    @staticmethod
    def check_default_activation_response(activate_response: Any) -> None:
        content_type = activate_response.headers.get("Content-Type")
        if content_type is not None and "text/plain" in activate_response.headers.get("Content-Type") and activate_response.content is not None and '"activated":true' in activate_response.content.decode():
            raise PolarionStartupError("admin-utility extension is not available in Polarion")

    @staticmethod
    def issue_security_token(polarion_admin_utility_api: Any) -> str:
        now = datetime.now()
        now_plus_10 = now + timedelta(minutes=5)
        return polarion_admin_utility_api.create_security_token("test", now_plus_10.strftime("%Y-%m-%dT%H:%M:%SZ"))

    @staticmethod
    def copy_dependency(systest_extensions_jars_path: str, group_id: str, artifact_id: str, version: str | None) -> None:
        mvn_path = TestContainersHelper.get_maven_location()
        if not version:
            version = TestContainersHelper.get_latest_artifact_version(group_id, artifact_id)
        command = f"{mvn_path} dependency:copy -Dartifact={group_id}:{artifact_id}:{version} -DoutputDirectory={systest_extensions_jars_path}"
        # S602: shell=True is required for Maven command execution
        result = subprocess.run(command, check=False, shell=True)  # noqa: S602
        if result.returncode != 0:
            raise MavenError(f"Failed to copy artifact {group_id}:{artifact_id}:{version}, code: " + str(result.returncode))

    @staticmethod
    def get_latest_artifact_version(group_id: str, artifact_id: str) -> str:
        repo_location = TestContainersHelper.get_maven_repo_location()
        artifact_path = pathlib.Path(repo_location) / group_id.replace(".", "/") / artifact_id
        artifact_dir_content = [item.name for item in artifact_path.iterdir()]
        regex = re.compile(r"^(?:\d+\.)?(?:\d+\.)?(?:\*|\d+)(?:-SNAPSHOT)?$")

        def is_version(dir_name: str) -> bool:
            return regex.match(dir_name) is not None

        versions = list(filter(is_version, artifact_dir_content))
        versions.sort(reverse=True)
        return versions[0]

    @staticmethod
    def get_maven_repo_location() -> str:
        mvn_location = TestContainersHelper.get_maven_location()
        command = f"{mvn_location} -q help:evaluate -Dexpression=settings.localRepository -DforceStdout=true"
        # S602: shell=True is required for Maven command execution
        result = subprocess.run(command, check=False, shell=True, stdout=subprocess.PIPE, text=True)  # noqa: S602
        if result.returncode != 0:
            raise MavenError("Cannot determine local maven repository, code: " + str(result.returncode))
        return str(pathlib.Path(result.stdout.strip()).expanduser())

    @staticmethod
    def get_maven_location() -> str:
        mvn_path = shutil.which("mvn")
        if not mvn_path:
            raise MavenError("Maven is not available, please, install mvn and try again.")
        return mvn_path

    def create_host_extensions_path(self) -> str:
        self.systest_extensions_root = f"{tempfile.gettempdir()}/systest"
        systest_extensions_jars_path = f"{self.systest_extensions_root}/sbb-extensions/eclipse/plugins"
        if pathlib.Path(systest_extensions_jars_path).exists():
            shutil.rmtree(systest_extensions_jars_path)
        pathlib.Path(systest_extensions_jars_path).mkdir(parents=True)
        return systest_extensions_jars_path


@dataclass
class TestContainerParameters:
    """Class for keeping command line arguments."""

    polarion_image_name: str
    weasyprint_service_image_name: str
    extension_version: str
    additional_bundles: list["ArtifactInfo"] | None
    admin_utility_version: str


@dataclass
class ArtifactInfo:
    """Class for keeping extension information."""

    group_id: str
    artifact_id: str
    version: str


class ContainerSetupError(Exception):
    """Error during container setup"""


class PolarionStartupError(Exception):
    """Error during Polarion startup"""


class MavenError(Exception):
    """Error during Maven operations"""
