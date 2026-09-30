// CI for the werunplugged Ackpine fork: 18 library modules, consumed by the UP Store as Maven dependencies from
// Artifactory (unplugged-libraries), so it publishes with Gradle rather than through DeploymentHelper/AndroidBuilderTest,
// which uploads a single file.
//
// The branch picks the release channel, which is appended to the upstream version from version.json:
//   main               production   0.25.4-unplugged.<build>        published
//   v<version>         staging      0.25.4-unplugged.rc.<build>     published
//   unplugged-develop  development  0.25.4-unplugged.dev.<build>    published
//   anything else      -            0.25.4-unplugged.ci.<build>     ABI check, build and tests only
//
// Job setup: a Multibranch Pipeline for werunplugged/up_ackpine, with the artifactory-credentials credential. The
// repository is public: don't enable "Discover pull requests from forks", or anyone's code would run on the agent.
pipeline {
    agent {
        dockerfile {
            dir 'ci'
            label 'bs1'
            // Gradle daemon (-Xmx6g in gradle.properties) + Kotlin daemon (3g) + test workers.
            args '-m 12g --cpus=4'
        }
    }
    options {
        ansiColor('xterm')
        disableConcurrentBuilds()
        timeout(time: 90, unit: 'MINUTES')
    }
    environment {
        // The container runs as the agent's uid, which has no home directory in the image, so give Gradle, AGP and
        // Robolectric (which reads user.home) one inside the container. Nothing is shared between builds.
        HOME = '/ci/home'
        GRADLE_USER_HOME = '/ci/gradle'
        ANDROID_USER_HOME = '/ci/home/.android'
        JAVA_TOOL_OPTIONS = '-Duser.home=/ci/home'
        // Pinned so that an ANDROID_HOME set on the agent can't override the image's SDK.
        ANDROID_HOME = '/usr/local/android-sdk'
        UNPLUGGED_REPOSITORY_URL = 'https://unplugged.jfrog.io/artifactory/unplugged-libraries'
    }
    stages {
        stage('Resolve Channel') {
            steps {
                script {
                    def branch = env.BRANCH_NAME ?: ''
                    def environment = null
                    def channel
                    if (branch == 'main') {
                        environment = 'production'
                        channel = "unplugged.${env.BUILD_NUMBER}"
                    } else if (branch ==~ /v\d+(\.\d+)*/) {
                        environment = 'staging'
                        channel = "unplugged.rc.${env.BUILD_NUMBER}"
                    } else if (branch == 'unplugged-develop') {
                        environment = 'development'
                        channel = "unplugged.dev.${env.BUILD_NUMBER}"
                    } else {
                        channel = "unplugged.ci.${env.BUILD_NUMBER}"
                    }
                    env.ENVIRONMENT = environment ?: 'none'
                    env.CHANNEL = channel
                    env.PUBLISH = (environment != null).toString()
                    env.GRADLE_ARGS = "--no-daemon --console=plain -Packpine.version.channel=${env.CHANNEL} " +
                        '-Packpine.publishing.sign=false -Pkotlin.daemon.jvmargs=-Xmx3g'
                    echo "🌿 Branch: ${branch} | environment: ${env.ENVIRONMENT} | channel: ${env.CHANNEL} | publish: ${env.PUBLISH}"
                }
            }
        }

        stage('Build & Test') {
            steps {
                // The test fixtures are signed with the repository's debug keystore, as in upstream's CI: AGP doesn't
                // create one for a release signing config in a fresh environment.
                sh 'mkdir -p "$ANDROID_USER_HOME" && cp .github/debug.keystore "$ANDROID_USER_HOME/debug.keystore"'
                sh './gradlew $GRADLE_ARGS :checkAckpineAbi :buildAckpine test'
            }
        }

        stage('Publish to Artifactory') {
            when { environment name: 'PUBLISH', value: 'true' }
            steps {
                script {
                    env.ACKPINE_VERSION = sh(
                        script: './gradlew $GRADLE_ARGS -q :ackpine-core:properties --property version | sed -n "s/^version: //p"',
                        returnStdout: true
                    ).trim()
                    if (!env.ACKPINE_VERSION) {
                        error('❌ Could not determine the Ackpine version')
                    }
                    echo "📦 Publishing ${env.ACKPINE_VERSION} (${env.ENVIRONMENT})"
                }
                withCredentials([
                    usernamePassword(credentialsId: 'artifactory-credentials', usernameVariable: 'ORG_GRADLE_PROJECT_unpluggedUsername', passwordVariable: 'ORG_GRADLE_PROJECT_unpluggedPassword')
                ]) {
                    // Refuse to overwrite a published version, e.g. after the job was recreated and its build numbers
                    // restarted: builds that already resolved that version would keep the old artifacts. The
                    // credentials go to curl on stdin so they never appear in a process list.
                    sh '''
                        set +x
                        pom="$UNPLUGGED_REPOSITORY_URL/ru/solrudev/ackpine/ackpine-core/$ACKPINE_VERSION/ackpine-core-$ACKPINE_VERSION.pom"
                        status=$(printf 'user = "%s:%s"\\n' "$ORG_GRADLE_PROJECT_unpluggedUsername" "$ORG_GRADLE_PROJECT_unpluggedPassword" \
                            | curl -s -K - -o /dev/null -w '%{http_code}' -I "$pom")
                        if [ "$status" != 404 ]; then
                            echo "❌ ackpine-core $ACKPINE_VERSION already exists in Artifactory or can't be checked (HTTP $status)"
                            exit 1
                        fi
                        ./gradlew $GRADLE_ARGS -Packpine.publishing.unplugged.url="$UNPLUGGED_REPOSITORY_URL" publishAllPublicationsToUnpluggedRepository
                    '''
                }
                script {
                    currentBuild.description = "📦 ${env.ENVIRONMENT} | ${env.ACKPINE_VERSION}"
                }
            }
        }
    }
    post {
        always {
            junit allowEmptyResults: true, testResults: '**/build/test-results/**/TEST-*.xml'
            cleanWs notFailBuild: true
        }
        success {
            script {
                if (env.PUBLISH != 'true') {
                    currentBuild.description = "✅ Built and tested | ${env.CHANNEL}"
                }
            }
        }
    }
}
