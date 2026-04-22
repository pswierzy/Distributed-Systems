FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Europe/Warsaw

RUN apt-get update && apt-get install -y \
    build-essential \
    wget \
    iputils-ping \
    net-tools \    
    curl \
    git \
    cmake \
    pkg-config \
    unzip \
    software-properties-common \
    python3 \
    python3-pip \
    python3-venv \
    openjdk-25-jdk \
    maven \
    ca-certificates \
    lsb-release \
    && rm -rf /var/lib/apt/lists/*


# ==========================================================
# ================== GRPC: protoc + plugin =================
# ==========================================================

ARG PROTOC_VERSION=34.1
RUN wget -q https://github.com/protocolbuffers/protobuf/releases/download/v${PROTOC_VERSION}/protoc-${PROTOC_VERSION}-linux-x86_64.zip -O /tmp/protoc.zip && \
    unzip -q /tmp/protoc.zip -d /usr/local && \
    rm /tmp/protoc.zip


# ---- Install gRPC Java plugin 1.80.0 ----
RUN curl -L -o /usr/local/bin/protoc-gen-grpc-java \
    https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/1.80.0/protoc-gen-grpc-java-1.80.0-linux-x86_64.exe && \
    chmod +x /usr/local/bin/protoc-gen-grpc-java

# ==========================================================
# ====================== gRPC Python Tools ==================
# ==========================================================

RUN python3 -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

RUN pip install --upgrade pip \
    && pip install grpcio grpcio-tools

# ==========================================================
# ========================== gRPCurl =======================
# ==========================================================

RUN curl -L https://github.com/fullstorydev/grpcurl/releases/download/v1.9.3/grpcurl_1.9.3_linux_x86_64.tar.gz \
    -o /tmp/grpcurl.tar.gz && \
    tar -xzf /tmp/grpcurl.tar.gz -C /tmp && \
    mv /tmp/grpcurl /usr/local/bin/grpcurl && \
    chmod +x /usr/local/bin/grpcurl && \
    rm -rf /tmp/grpcurl*

RUN echo "Protoc:" && protoc --version

WORKDIR /workspace
CMD ["bash"]