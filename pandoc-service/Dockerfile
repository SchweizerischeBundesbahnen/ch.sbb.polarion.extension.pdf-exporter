FROM pandoc/extra:latest-ubuntu
LABEL maintainer="SBB Polarion Team <polarion-opensource@sbb.ch>"

ARG APP_IMAGE_VERSION=0.0.0-dev

RUN apt-get update && \
    apt-get --yes --no-install-recommends install dbus python3-brotli python3-cffi vim && \
    apt-get clean autoclean && \
    apt-get --yes autoremove && \
    rm -rf /var/lib/apt/lists/*

ENV WORKING_DIR="/opt/pandoc"
ENV PANDOC_SERVICE_VERSION=${APP_IMAGE_VERSION}

WORKDIR ${WORKING_DIR}

RUN BUILD_TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ") && \
    echo ${BUILD_TIMESTAMP} > "${WORKING_DIR}/.build_timestamp"

COPY requirements.txt ${WORKING_DIR}/requirements.txt

RUN pip3 install --no-cache-dir --break-system-packages -r ${WORKING_DIR}/requirements.txt

COPY ./app/*.py ${WORKING_DIR}/app/

COPY entrypoint.sh ${WORKING_DIR}/entrypoint.sh
RUN chmod +x ${WORKING_DIR}/entrypoint.sh

ENTRYPOINT [ "./entrypoint.sh" ]
