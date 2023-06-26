package ru.solrudev.ackpine.impl.session

import ru.solrudev.ackpine.DisposableSubscription
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session

internal interface ProgressSession<F : Failure> : Session<F> {
	fun addProgressListener(listener: PackageInstaller.ProgressListener): DisposableSubscription
	fun removeProgressListener(listener: PackageInstaller.ProgressListener)
}