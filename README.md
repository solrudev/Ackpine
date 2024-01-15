![Ackpine](docs/images/logo-text-colored.svg)

[![CI](https://github.com/solrudev/Ackpine/actions/workflows/ci.yml/badge.svg)](https://github.com/solrudev/Ackpine/actions/workflows/ci.yml)
![Maven Central](https://img.shields.io/maven-central/v/ru.solrudev.ackpine/ackpine-core)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://github.com/solrudev/Ackpine/blob/master/LICENSE)
[![Android Weekly](https://androidweekly.net/issues/issue-593/badge)](https://androidweekly.net/issues/issue-593)

<span style="color:#808080">_**Ackpine** - **A**ndroid **C**oroutines-friendly **K**otlin-first **P**ackage **In**staller **e**xtensions_</span>

See the [project website](https://solrudev.github.io/Ackpine/) for documentation and API reference.

Article about the library: [Painless building of an Android package installer app](https://medium.com/@solrudev/painless-building-of-an-android-package-installer-app-d5a09b5df432)

A library providing consistent APIs for installing and uninstalling apps on an Android device.

Key features of Ackpine:

- **Ease of use**: Ackpine reduces complexity of dealing with system `PackageInstaller` APIs.
- **Unifying of different system APIs**: Ackpine provides an ability to choose system package installer API which will be used to install a package.
- **Built-in split APKs support**: Ackpine provides simple lazy sequences-based APIs for reading, parsing and filtering split APKs.
- **Persistent**: Ackpine persists every session so it can properly handle process death.
- **Deferred**: Ackpine allows to defer user's intervention via customizable high-priority notification.
- **Progress and state updates**: it's easy to observe every session's state and progress updates.
- **100% Java and Kotlin-friendly**: while maintaining full Java compatibility, Ackpine was developed as a Kotlin-first library.
- **Compatibility**: Ackpine supports Android versions starting from API level 16.

Download
--------

Ackpine is available on `mavenCentral()`.

Ackpine depends on Jetpack libraries, so it's necessary to declare the `google()` Maven repository.

```kotlin
dependencies {
    val ackpineVersion = "0.4.2"
    implementation("ru.solrudev.ackpine:ackpine-core:$ackpineVersion")

    // optional - Kotlin extensions and Coroutines support
    implementation("ru.solrudev.ackpine:ackpine-ktx:$ackpineVersion")

    // optional - utilities for working with split APKs
    implementation("ru.solrudev.ackpine:ackpine-splits:$ackpineVersion")

    // optional - support for asset files inside of application's package
    implementation("ru.solrudev.ackpine:ackpine-assets:$ackpineVersion")
}
```

License
-------

    Copyright (C) 2023-2024 Ilya Fomichev
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
