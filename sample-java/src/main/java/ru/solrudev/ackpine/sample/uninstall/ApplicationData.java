/*
 * Copyright (C) 2023 Ilya Fomichev
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

package ru.solrudev.ackpine.sample.uninstall;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import java.util.Objects;

public final class ApplicationData {

	@NonNull
	private final String name;

	@NonNull
	private final String packageName;

	@NonNull
	private final Drawable icon;

	public ApplicationData(@NonNull String name, @NonNull String packageName, @NonNull Drawable icon) {
		this.name = name;
		this.packageName = packageName;
		this.icon = icon;
	}

	@NonNull
	public String name() {
		return name;
	}

	@NonNull
	public String packageName() {
		return packageName;
	}

	@NonNull
	public Drawable icon() {
		return icon;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ApplicationData that = (ApplicationData) o;
		return name.equals(that.name) && packageName.equals(that.packageName) && icon.equals(that.icon);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, packageName, icon);
	}

	@NonNull
	@Override
	public String toString() {
		return "ApplicationData{" +
				"name='" + name + '\'' +
				", packageName='" + packageName + '\'' +
				", icon=" + icon +
				'}';
	}
}
