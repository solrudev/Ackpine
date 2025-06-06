Building
========

Root `Ackpine` project has the following Gradle tasks:

- `:buildAckpine`: a lifecycle task which builds release AARs of all Ackpine library projects;
- `:buildSamples`: a lifecycle task which builds and gathers release versions of all Ackpine sample app APKs and R8 mappings into `release` directory;
- `:releaseChangelog`: an actionable task which extracts changelog for the last release from `docs/changelog.md` file to `changelog.txt`.

Other useful tasks provided by third-party Gradle plugins:

- `:api-documentation:dokkaGenerate`: generates API documentation for the library projects and places it into `docs/api` directory;
- `apiCheck`: validates public API surface of the library projects against dumps;
- `apiDump`: dumps public API surface of the library projects;
- `publishAndReleaseToMavenCentral`: publishes the library projects to Maven Central repository. For further information see [plugin's documentation](https://vanniktech.github.io/gradle-maven-publish-plugin/central/#secrets).

Projects are added to `:buildAckpine` and `:buildSamples` tasks through `dependencies` block in root `build.gradle.kts`. Library project should have `ru.solrudev.ackpine.library` Gradle plugin applied, and sample app project should have `ru.solrudev.ackpine.app-release` Gradle plugin applied.

Projects are added to `:api-documentation:dokkaGenerate` task through `dependencies` block in `build.gradle.kts` of `api-documentation` project. Documented project should have `ru.solrudev.ackpine.dokka` Gradle plugin applied.

To serve documentation website on localhost, execute this command (requires Python 3 and Material for MkDocs to be installed):
```
mkdocs serve
```

To build release versions of sample apps you'll need to provide a keystore. For this, create a `keystore.properties` file in the root directory with the following contents:
```properties
APP_SIGNING_KEY_ALIAS=YOUR_KEY_ALIAS
APP_SIGNING_KEY_PASSWORD=YOUR_KEY_PASSWORD
APP_SIGNING_KEY_STORE_PASSWORD=YOUR_KEYSTORE_PASSWORD
APP_SIGNING_KEY_STORE_PATH=PATH_TO_YOUR_KEYSTORE_FILE
```
The other way to provide these values is through environment variables. `keystore.properties` file takes precedence over environment variables.