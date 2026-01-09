/*
 * Copyright (C) 2026 Ilya Fomichev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.solrudev.ackpine.test

import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.UninstallFailure

/**
 * A controllable install [Session] test double for usage with [TestPackageInstaller].
 *
 * State and progress listeners are invoked on the calling thread, and the current state and progress are delivered
 * immediately when a listener is added. Use [TestSession.controller] to drive state and progress transitions directly
 * or to script transitions tied to [Session.launch] and [Session.commit] calls.
 */
public typealias TestInstallSession = TestProgressSession<InstallFailure>

/**
 * A controllable uninstall [Session] test double for usage with [TestPackageUninstaller].
 *
 * State listeners are invoked on the calling thread, and the current state is delivered immediately when a
 * listener is added. Use [TestSession.controller] to drive state transitions directly or to script transitions tied to
 * [Session.launch] and [Session.commit] calls.
 */
public typealias TestUninstallSession = TestSession<UninstallFailure>