FROM gitpod/workspace-base

USER gitpod

RUN sudo apt-get -q update \
 && sudo apt-get install -yq openjdk-16-jdk python3-pip npm \
 && sudo pip3 install pre-commit \
 && sudo update-java-alternatives --set java-1.16.0-openjdk-amd64
