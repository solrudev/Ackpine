Building
========

Root `Ackpine` project has the following Gradle tasks:

- `:buildAckpine`: a lifecycle task which builds release versions of all Ackpine library projects;
- `:buildReleaseSamples`: a lifecycle task which builds and gathers release versions of all Ackpine sample app APKs and R8 mappings into `release` directory;
- `:releaseChangelog`: an actionable task which extracts changelog for the last release from `docs/changelog.md` file to `changelog.txt`.

Other useful tasks provided by third-party Gradle plugins:

- `:dokkaGenerate`: generates API documentation for the library projects and places it into `docs/api` directory;
- `apiCheck`: validates public API surface of the library projects against dumps;
- `apiDump`: dumps public API surface of the library projects;
- `publishAndReleaseToMavenCentral`: publishes the library projects to Maven Central repository. For further information see [plugin's documentation](https://vanniktech.github.io/gradle-maven-publish-plugin/central/#secrets).

To serve documentation website on localhost, execute this command (requires Python 3 and Material for MkDocs to be installed):
```
mkdocs serve
```
Documentation website styling is optimized for Material for MkDocs v9.3.2 and lower.

To build release versions of sample apps you'll need to provide a keystore. For this, create a `keystore.properties` file in the root directory with the following contents:
```properties
APP_SIGNING_KEY_ALIAS=YOUR_KEY_ALIAS
APP_SIGNING_KEY_PASSWORD=YOUR_KEY_PASSWORD
APP_SIGNING_KEY_STORE_PASSWORD=YOUR_KEYSTORE_PASSWORD
APP_SIGNING_KEY_STORE_PATH=PATH_TO_YOUR_KEYSTORE_FILE
```
The other way to provide these values is through environment variables.