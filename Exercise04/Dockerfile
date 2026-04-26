FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Europe/Warsaw

# --- Podstawowe narzędzia i środowiska ---
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
# ====================== ICE 3.7 ===========================
# ==========================================================

RUN wget -q https://download.zeroc.com/ice/3.7/ubuntu24.04/ice-repo-3.7_1.0.0_all.deb -O /tmp/ice-repo.deb && \
    dpkg -i /tmp/ice-repo.deb && \
    rm /tmp/ice-repo.deb && \
    apt-get update && \
    apt-get install -y zeroc-ice-all-runtime zeroc-ice-all-dev zeroc-ice-compilers

RUN echo "ICE slice tools:" && which slice2java && which slice2cpp && which slice2py && which slice2cs

WORKDIR /workspace
CMD ["bash"]