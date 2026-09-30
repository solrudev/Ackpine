# Build image for the werunplugged Ackpine fork, built by the Jenkinsfile's `agent { dockerfile }` on every run and
# never pushed: the repository is public, so a shared image tag could be overwritten from any branch.
# JDK 21 is Ackpine's toolchain; the SDK packages are the ones its build uses (see AckpineSettingsPlugin).
FROM amazoncorretto:21-al2023

# procps-ng provides the `ps` Jenkins needs to stop a build on abort or timeout.
RUN yum -y update \
    && yum -y install unzip findutils which tar gzip procps-ng \
    && yum clean all

ENV ANDROID_HOME=/usr/local/android-sdk \
    CMDLINE_TOOLS_URL=https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip

RUN mkdir -p "$ANDROID_HOME/cmdline-tools" \
    && cd "$ANDROID_HOME/cmdline-tools" \
    && curl -fsSL -o tools.zip "$CMDLINE_TOOLS_URL" \
    && unzip -q tools.zip \
    && rm tools.zip \
    && mv cmdline-tools latest

# compileSdk 36 and build-tools 36.1.0 for the libraries, compileSdk 35 for a test fixture.
RUN yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null \
    && "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
        "platforms;android-36" "platforms;android-35" "build-tools;36.1.0" "platform-tools"

# Jenkins runs the container as the agent's uid, which has no home directory here. Everything the build writes to must
# be writable by any user: the SDK (AGP installs missing packages on demand) and /ci, the home the Jenkinsfile gives
# Gradle, AGP and Robolectric.
RUN chmod -R a+rwX "$ANDROID_HOME" \
    && mkdir -p /ci/gradle /ci/home \
    && chmod -R 777 /ci

CMD ["/bin/bash"]
