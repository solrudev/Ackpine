package ru.solrudev.ackpine.sample.uninstall;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import java.util.Objects;

public final class ApplicationData {

	private final int id;

	@NonNull
	private final String name;

	@NonNull
	private final String packageName;

	@NonNull
	private final Drawable icon;

	public ApplicationData(int id, @NonNull String name, @NonNull String packageName, @NonNull Drawable icon) {
		this.id = id;
		this.name = name;
		this.packageName = packageName;
		this.icon = icon;
	}

	public int id() {
		return id;
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
		return id == that.id && name.equals(that.name) && packageName.equals(that.packageName) && icon.equals(that.icon);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, packageName, icon);
	}

	@NonNull
	@Override
	public String toString() {
		return "ApplicationData{" +
				"id=" + id +
				", name='" + name + '\'' +
				", packageName='" + packageName + '\'' +
				", icon=" + icon +
				'}';
	}
}
