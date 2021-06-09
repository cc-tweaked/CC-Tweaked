FROM gitpod/workspace-base

USER gitpod

# Install custom tools, runtime, etc. using apt-get
# For example, the command below would install "bastet" - a command line tetris clone:
#
# RUN sudo apt-get -q update && \
#     sudo apt-get install -yq bastet && \
#     sudo rm -rf /var/lib/apt/lists/*
#
# More information: https://www.gitpod.io/docs/config-docker/

# Install Java 8 and 16
RUN sudo apt-get -q update && \
    sudo apt install -yq openjdk-8-jdk openjdk-16-jdk

# This is so that you can use java 8 until such a time as you switch to java 16
RUN sudo update-java-alternatives --set java-1.8.0-openjdk-amd64
