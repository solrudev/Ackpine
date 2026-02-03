package ru.solrudev.ackpine.remote;

import ru.solrudev.ackpine.remote.ISession;

interface IPackageUninstaller {
	ISession createImmediateSession(String type, String packageName);
	ISession createDeferredSession(String type, String packageName, String notificationTitle);
	ISession getSession(String id);
}