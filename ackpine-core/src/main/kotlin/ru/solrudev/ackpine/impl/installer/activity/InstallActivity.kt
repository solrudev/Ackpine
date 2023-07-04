package ru.solrudev.ackpine.impl.installer.activity

import android.os.Bundle
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.impl.activity.LauncherActivity
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.session.ProgressSession

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal abstract class InstallActivity(
	tag: String,
	requestCode: Int = -1
) : LauncherActivity<ProgressSession<InstallFailure>, InstallFailure>(
	tag, requestCode,
	abortedStateFailureFactory = InstallFailure::Aborted
) {

	override val ackpineSessionFuture by lazy {
		ackpinePackageInstaller.getSessionAsync(ackpineSessionId!!)
	}

	private lateinit var ackpinePackageInstaller: PackageInstaller

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		ackpinePackageInstaller = PackageInstaller.getInstance(this)
	}
}