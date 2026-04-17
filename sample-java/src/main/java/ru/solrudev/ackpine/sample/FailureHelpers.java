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

package ru.solrudev.ackpine.sample;

import com.topjohnwu.superuser.Shell;

import java.io.IOException;

import ru.solrudev.ackpine.libsu.NoRootException;
import ru.solrudev.ackpine.session.Failure;

public class FailureHelpers {

	public static void closeShellOnNoRootException(Failure failure) {
		if (failure instanceof Failure.Exceptional f && f.getException() instanceof NoRootException) {
			final var shell = Shell.getCachedShell();
			if (shell != null) {
				try {
					shell.close();
				} catch (IOException e) { // ignore
				}
			}
		}
	}
}