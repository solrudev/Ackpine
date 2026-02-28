package ru.solrudev.ackpine.remote;

import java.util.List;
import ru.solrudev.ackpine.remote.ISession;

interface IPackageInstaller {

	ISession createSession(int type,
						   in List<String> uri,
						   int confirmation,
						   String notificationTitle,
						   String notificationText,
						   String preapprovalPackageName,
						   String preapprovalLabel,
						   String preapprovalLanguageTag,
						   String preapprovalIconUri,
						   boolean fallbackToOnDemandApproval,
						   boolean requireUserAction);

	ISession getSession(String id);
}