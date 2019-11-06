# Dockerfile based on https://github.com/ekreative/android-docker/blob/master/Dockerfile

FROM openjdk:8-jdk

ENV ANDROID_SDK_VER 4333796
ENV ANDROID_SDK_HASH 92ffee5a1d98d856634e8b71132e8a95d96c83a63fde1099be3d86df3106def9
ENV ANDROID_SDK_URL https://dl.google.com/android/repository/sdk-tools-linux-${ANDROID_SDK_VER}.zip
WORKDIR /opt

RUN wget $ANDROID_SDK_URL -O android-sdk.zip \
    && (echo "${ANDROID_SDK_HASH} android-sdk.zip" | sha256sum -c ) \
    && unzip android-sdk.zip -d android-sdk-linux \
    && rm -f android-sdk.zip \
    && chown -R root:root android-sdk-linux

ENV ANDROID_SDK_ROOT /opt/android-sdk-linux
ENV ANDROID_HOME $ANDROID_SDK_ROOT
ENV PATH ${ANDROID_SDK_ROOT}/tools/bin:${ANDROID_SDK_ROOT}/tools:${ANDROID_SDK_ROOT}/platform-tools:$PATH

ENV ANDROID_PLATFORM_VERSION 28
ENV ANDROID_BUILD_TOOLS_VERSION 28.0.3
ENV ANDROID_EXTRA_PACKAGES "build-tools;28.0.0" "build-tools;28.0.1" "build-tools;28.0.2"
ENV ANDROID_REPOSITORIES "extras;android;m2repository" "extras;google;m2repository"
ENV ANDROID_CONSTRAINT_PACKAGES "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.2" "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.1" "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.0"
RUN yes | sdkmanager --verbose "platform-tools" "platforms;android-$ANDROID_PLATFORM_VERSION" "build-tools;$ANDROID_BUILD_TOOLS_VERSION" $ANDROID_EXTRA_PACKAGES $ANDROID_REPOSITORIES $ANDROID_CONSTRAINT_PACKAGES

WORKDIR /src
