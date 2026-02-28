package ru.solrudev.ackpine.remote;

import ru.solrudev.ackpine.remote.ISession;

interface IPackageUninstaller {

	ISession createSession(int type,
						   String packageName,
						   int confirmation,
						   String notificationTitle,
						   String notificationText);

	ISession getSession(String id);
}