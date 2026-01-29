package ru.solrudev.ackpine.remote;

import ru.solrudev.ackpine.remote.ISessionStateListener;

interface ISession {
	String getId();
	boolean launch();
	boolean commit();
	void cancel();
	void addStateListener(ISessionStateListener listener);
	void removeStateListener(ISessionStateListener listener);
}