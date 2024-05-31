#FROM dockerio.docker.bin.sbb.ch/library/debian:12-slim
FROM debian:12-slim
LABEL maintainer="Team Dev Tools (CLEW/WZU/POLARION) <mwe@sbb.ch>"

RUN apt-get update && \
    apt-get --yes --no-install-recommends install python3-pip python3-venv python3-cffi python3-brotli libpango-1.0-0 libpangoft2-1.0-0 fonts-liberation && \
    apt-get clean autoclean && \
    apt-get --yes autoremove && \
    rm -rf /var/lib/apt/lists/*

ENV VIRTUAL_ENV=/opt/.venv

RUN ln -s "$(which python3.11)" /usr/bin/python && \
    ln -sf "$(which python3.11)" /usr/bin/python3 && \
    python -m venv ${VIRTUAL_ENV}

ENV PATH="$VIRTUAL_ENV/bin:$PATH"

RUN pip install --no-cache-dir weasyprint==v61.2

ENTRYPOINT [ "weasyprint" ]