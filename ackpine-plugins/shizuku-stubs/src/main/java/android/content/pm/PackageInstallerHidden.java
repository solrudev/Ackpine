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

import android.content.Context;
import android.content.IntentSender;
import android.os.Build;

import androidx.annotation.RequiresApi;

import dev.rikka.tools.refine.RefineAs;

/**
 * Stub for {@link PackageInstaller} which will be renamed and removed during transformation.
 */
@RefineAs(PackageInstaller.class)
public class PackageInstallerHidden {

	@RequiresApi(Build.VERSION_CODES.S)
	public PackageInstallerHidden(IPackageInstaller packageInstaller,
								  String installerPackageName,
								  String attributionTag,
								  int userId) {
		throw new UnsupportedOperationException();
	}

	@RequiresApi(Build.VERSION_CODES.O)
	public PackageInstallerHidden(IPackageInstaller packageInstaller,
								  String installerPackageName,
								  int userId) {
		throw new UnsupportedOperationException();
	}

	public PackageInstallerHidden(Context context,
								  PackageManager packageManager,
								  IPackageInstaller packageInstaller,
								  String installerPackageName,
								  int userId) {
		throw new UnsupportedOperationException();
	}

	public void uninstall(String packageName, int flags, IntentSender statusReceiver) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Stub for {@link PackageInstaller.SessionParams} which will be removed during transformation.
	 */
	public static class SessionParams {
		public int installFlags;
	}

	/**
	 * Stub for {@link PackageInstaller.Session} which will be removed during transformation.
	 */
	public static class Session {
		public Session(IPackageInstallerSession session) {
			throw new UnsupportedOperationException();
		}
	}
}
