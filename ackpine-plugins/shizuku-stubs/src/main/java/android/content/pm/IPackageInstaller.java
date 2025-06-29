/*
 * Copyright (C) 2025 Ilya Fomichev
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

package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

/**
 * Stub for {@code android.content.pm.IPackageInstaller}.
 */
public interface IPackageInstaller extends IInterface {
	void abandonSession(int sessionId);
	IPackageInstallerSession openSession(int sessionId);
	abstract class Stub extends Binder implements IPackageInstaller {
		public static IPackageInstaller asInterface(IBinder binder) {
			throw new UnsupportedOperationException();
		}
	}
}