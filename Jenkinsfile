// CI for the werunplugged Ackpine fork: 18 library modules, consumed by the UP Store as Maven dependencies from
// Artifactory (unplugged-libraries), so it publishes with Gradle rather than through DeploymentHelper/AndroidBuilderTest,
// which uploads a single file.
//
// Only the main branch publishes: a merge to main (or master, the fork's default branch) publishes every library as
// <upstream version>-unplugged.<build>, e.g. 0.25.4-unplugged.12. Every other branch (UNP-*, version branches, ...)
// runs the ABI check, build and tests only, and its 0.25.4-unplugged.ci.<build> version never leaves the agent.
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
        stage('Resolve Version') {
            steps {
                script {
                    def branch = env.BRANCH_NAME ?: ''
                    def isMainBranch = branch in ['main', 'master']
                    // TEMPORARY (UNP-9252): lets this branch publish once more, to verify the com.unplugged.ackpine group
                    // and the all-modules overwrite check, as 0.25.4-unplugged.test.<build>. Remove once verified.
                    def isTestPublishBranch = branch == 'UNP-9252'
                    env.CHANNEL = isMainBranch ? "unplugged.${env.BUILD_NUMBER}" :
                        (isTestPublishBranch ? "unplugged.test.${env.BUILD_NUMBER}" : "unplugged.ci.${env.BUILD_NUMBER}")
                    env.PUBLISH = (isMainBranch || isTestPublishBranch).toString()
                    env.GRADLE_ARGS = "--no-daemon --console=plain -Packpine.version.channel=${env.CHANNEL} " +
                        '-Packpine.publishing.sign=false -Pkotlin.daemon.jvmargs=-Xmx3g'
                    echo "🌿 Branch: ${branch} | channel: ${env.CHANNEL} | publish: ${env.PUBLISH == 'true' ? 'yes' : 'no, only the main branch publishes'}"
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
                    echo "📦 Publishing ${env.ACKPINE_VERSION}"
                }
                withCredentials([
                    usernamePassword(credentialsId: 'artifactory-credentials', usernameVariable: 'ORG_GRADLE_PROJECT_unpluggedUsername', passwordVariable: 'ORG_GRADLE_PROJECT_unpluggedPassword')
                ]) {
                    // Refuse to overwrite a published version, e.g. after the job was recreated and its build numbers
                    // restarted: builds that already resolved that version would keep the old artifacts. The search
                    // covers every module, not just ackpine-core, so a publish that failed halfway is caught too
                    // ("*" also matches compress-android's 1.28.0-<version>). Anything but an empty 200 answer
                    // refuses. The credentials go to curl on stdin so they never appear in a process list.
                    sh '''
                        set +x
                        search="https://unplugged.jfrog.io/artifactory/api/search/gavc?g=com.unplugged.ackpine&v=*$ACKPINE_VERSION&repos=unplugged-libraries"
                        status=$(printf 'user = "%s:%s"\\n' "$ORG_GRADLE_PROJECT_unpluggedUsername" "$ORG_GRADLE_PROJECT_unpluggedPassword" \
                            | curl -s -K - -o search.json -w '%{http_code}' "$search")
                        if [ "$status" != 200 ]; then
                            echo "❌ Can't check whether $ACKPINE_VERSION is already in Artifactory (HTTP $status)"
                            exit 1
                        fi
                        if grep -q '"uri"' search.json; then
                            echo "❌ $ACKPINE_VERSION already exists in Artifactory:"
                            cat search.json
                            exit 1
                        fi
                        ./gradlew $GRADLE_ARGS -Packpine.publishing.unplugged.url="$UNPLUGGED_REPOSITORY_URL" publishAllPublicationsToUnpluggedRepository
                    '''
                }
                script {
                    env.JFROG_URL = "https://unplugged.jfrog.io/ui/repos/tree/General/unplugged-libraries/com/unplugged/ackpine/ackpine-core/${env.ACKPINE_VERSION}"
                    currentBuild.description = "📦 New version ${env.ACKPINE_VERSION} published to JFrog (all 18 libraries, under com/unplugged/ackpine):\n${env.JFROG_URL}"
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
                if (env.PUBLISH == 'true') {
                    echo "📦 New version ${env.ACKPINE_VERSION} is in JFrog: ${env.JFROG_URL}"
                } else {
                    currentBuild.description = "✅ Built and tested | ${env.CHANNEL}"
                }
            }
        }
    }
}
