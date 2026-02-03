package ru.solrudev.ackpine.remote;

import ru.solrudev.ackpine.remote.IPackageInstaller;
import ru.solrudev.ackpine.remote.IPackageUninstaller;

interface IAckpineRemoteService {
	IPackageInstaller getPackageInstaller();
	IPackageUninstaller getPackageUninstaller();
}