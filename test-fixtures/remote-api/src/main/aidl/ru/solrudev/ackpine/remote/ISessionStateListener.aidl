package ru.solrudev.ackpine.remote;

oneway interface ISessionStateListener {
	void onStateChanged(String id, String state);
	void onFailed(String id, String failure);
}