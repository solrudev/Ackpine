package ru.solrudev.ackpine.impl.uninstaller.activity.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RestrictTo

/**
 * Returns package uninstall [UninstallContract] for current API level.
 */
@Suppress("FunctionName")
@JvmSynthetic
internal fun UninstallPackageContract(): UninstallContract {
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
		return ActionUninstallPackageContract()
	}
	return ActionDeletePackageContract()
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface UninstallContract {
	fun createIntent(context: Context, input: String): Intent
	fun parseResult(context: Context, resultCode: Int): Boolean
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class ActionDeletePackageContract : UninstallContract {

	private lateinit var packageName: String

	override fun createIntent(context: Context, input: String): Intent {
		packageName = input
		val packageUri = Uri.parse("package:$input")
		return Intent(Intent.ACTION_DELETE, packageUri)
	}

	override fun parseResult(context: Context, resultCode: Int) = !context.isPackageInstalled(packageName)
}

internal class ActionUninstallPackageContract : UninstallContract {

	@Suppress("DEPRECATION")
	override fun createIntent(context: Context, input: String) = Intent().apply {
		action = Intent.ACTION_UNINSTALL_PACKAGE
		data = Uri.parse("package:$input")
		putExtra(Intent.EXTRA_RETURN_RESULT, true)
	}

	override fun parseResult(context: Context, resultCode: Int) = resultCode == Activity.RESULT_OK
}

@JvmSynthetic
internal fun Context.isPackageInstalled(packageName: String) = try {
	packageManager.getPackageInfoCompat(packageName, PackageManager.GET_ACTIVITIES)
	true
} catch (_: PackageManager.NameNotFoundException) {
	false
}

@Suppress("DEPRECATION")
@JvmSynthetic
internal fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int): PackageInfo {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
	} else {
		getPackageInfo(packageName, flags)
	}
}