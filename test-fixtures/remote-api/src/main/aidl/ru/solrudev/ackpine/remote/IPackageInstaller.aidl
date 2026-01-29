package ru.solrudev.ackpine.remote;

import ru.solrudev.ackpine.remote.ISession;

interface IPackageInstaller {
	ISession createImmediateSession(String type, String uri, boolean requireUserAction);
	ISession createDeferredSession(String type, String uri, String notificationTitle, boolean requireUserAction);
	ISession getSession(String id);
}