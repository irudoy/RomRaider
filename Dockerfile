# Java 17 build environment for RomRaider.

FROM ubuntu:24.04 AS rr_builder

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        ca-certificates \
        fontconfig \
        fonts-dejavu-core \
        libharfbuzz0b \
        openjdk-17-jdk-headless \
        unzip && \
    apt-get install -y --no-install-recommends \
        ant \
        ant-optional && \
    java_17_home="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")" && \
    update-alternatives --set java "${java_17_home}/bin/java" && \
    ln -s "${java_17_home}" /opt/java-17 && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/opt/java-17
ENV PATH="${JAVA_HOME}/bin:${PATH}"

RUN useradd --create-home --shell /bin/bash romraider && \
    chmod 0755 /home/romraider && \
    install --directory \
        --owner romraider \
        --group romraider \
        /home/romraider/RomRaider

USER romraider:romraider
WORKDIR /home/romraider/RomRaider

RUN java -version && \
    javac -version && \
    ant -version

CMD ["ant", "all"]
