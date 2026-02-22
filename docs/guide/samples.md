Samples
=======

Sample apps showcasing the usage of Ackpine can be found in the [GitHub repository](https://github.com/solrudev/Ackpine). Each sample demonstrates different aspects of the library and serves as a reference for common integration patterns.

Feature matrix
--------------

| Feature                        | sample-java                             | sample-ktx          | sample-api34                  |
|--------------------------------|-----------------------------------------|---------------------|-------------------------------|
| Language                       | Java                                    | Kotlin              | Kotlin                        |
| Async work                     | Guava's `ListenableFuture` + `LiveData` | Coroutines + `Flow` | Coroutines + `Flow`           |
| Install from APK               | ✅                                       | ✅                   | ✅ (direct streaming from web) |
| Install from zipped splits     | ✅                                       | ✅                   | ❌                             |
| Uninstall apps                 | ✅                                       | ✅                   | ❌                             |
| Progress tracking              | ✅                                       | ✅                   | ✅                             |
| Process death handling         | ✅                                       | ✅                   | ✅                             |
| Preapproval (API 34+)          | ❌                                       | ❌                   | ✅                             |
| Install constraints (API 34+)  | ❌                                       | ❌                   | ✅                             |
| Unit tests with `ackpine-test` | ✅                                       | ✅                   | ✅                             |

### [`sample-java`](https://github.com/solrudev/Ackpine/tree/master/sample-java)

Fully written in Java. Demonstrates the callback-based API with `Session.TerminalStateListener` and `DisposableSubscriptionContainer` for lifecycle management.

### [`sample-ktx`](https://github.com/solrudev/Ackpine/tree/master/sample-ktx)

Leverages the `ackpine-ktx` artifact with Kotlin coroutines and DSL APIs. Uses [`Session.await()`](/api/ackpine-ktx/ru.solrudev.ackpine.session/await.html) for session lifecycle management.

### [`sample-api34`](https://github.com/solrudev/Ackpine/tree/master/sample-api34)

Showcases features supported on API level 34 and higher, such as [install pre-commit preapproval](configuration.md#preapproval) and [installation constraints](configuration.md#constraints). Installs an APK from web resource with direct streaming using OkHttp.

Key files to study
------------------

### Session management patterns

The ViewModel files show how to create sessions, handle state transitions, and manage process death:

- `sample-java` — [`InstallViewModel.java`](https://github.com/solrudev/Ackpine/blob/master/sample-java/src/main/java/ru/solrudev/ackpine/sample/install/InstallViewModel.java), [`UninstallViewModel.java`](https://github.com/solrudev/Ackpine/blob/master/sample-java/src/main/java/ru/solrudev/ackpine/sample/uninstall/UninstallViewModel.java)
- `sample-ktx` — [`InstallViewModel.kt`](https://github.com/solrudev/Ackpine/blob/master/sample-ktx/src/main/kotlin/ru/solrudev/ackpine/sample/install/InstallViewModel.kt), [`UninstallViewModel.kt`](https://github.com/solrudev/Ackpine/blob/master/sample-ktx/src/main/kotlin/ru/solrudev/ackpine/sample/uninstall/UninstallViewModel.kt)
- `sample-api34` — [`MainViewModel.kt`](https://github.com/solrudev/Ackpine/blob/master/sample-api34/src/main/kotlin/ru/solrudev/ackpine/sample/updater/MainViewModel.kt)

### Zipped split APKs

The Fragment files show how to read zipped split APKs:

- `sample-java` — [`InstallFragment.java`](https://github.com/solrudev/Ackpine/blob/master/sample-java/src/main/java/ru/solrudev/ackpine/sample/install/InstallFragment.java)
- `sample-ktx` — [`InstallFragment.kt`](https://github.com/solrudev/Ackpine/blob/master/sample-ktx/src/main/kotlin/ru/solrudev/ackpine/sample/install/InstallFragment.kt)

### Testing patterns

The test files demonstrate how to use `ackpine-test` for unit testing session-based logic:

- `sample-java` — [`InstallViewModelTest.java`](https://github.com/solrudev/Ackpine/blob/master/sample-java/src/test/java/ru/solrudev/ackpine/sample/install/InstallViewModelTest.java), [`UninstallViewModelTest.java`](https://github.com/solrudev/Ackpine/blob/master/sample-java/src/test/java/ru/solrudev/ackpine/sample/uninstall/UninstallViewModelTest.java)
- `sample-ktx` — [`InstallViewModelTest.kt`](https://github.com/solrudev/Ackpine/blob/master/sample-ktx/src/test/kotlin/ru/solrudev/ackpine/sample/install/InstallViewModelTest.kt), [`UninstallViewModelTest.kt`](https://github.com/solrudev/Ackpine/blob/master/sample-ktx/src/test/kotlin/ru/solrudev/ackpine/sample/uninstall/UninstallViewModelTest.kt)
- `sample-api34` — [`MainViewModelTest.kt`](https://github.com/solrudev/Ackpine/blob/master/sample-api34/src/test/kotlin/ru/solrudev/ackpine/sample/updater/MainViewModelTest.kt)