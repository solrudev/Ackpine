# Serializable
-keep interface ru.solrudev.ackpine.session.parameters.DrawableId { *; }
-keep class * implements ru.solrudev.ackpine.session.parameters.DrawableId { *; }
-keep class ru.solrudev.ackpine.installer.InstallFailure { *; }
-keep class * extends ru.solrudev.ackpine.installer.InstallFailure { *; }
-keep interface ru.solrudev.ackpine.uninstaller.UninstallFailure { *; }
-keep class * implements ru.solrudev.ackpine.uninstaller.UninstallFailure { *; }
-keep interface ru.solrudev.ackpine.installer.parameters.InstallConstraints$TimeoutStrategy { *; }
-keep class * implements ru.solrudev.ackpine.installer.parameters.InstallConstraints$TimeoutStrategy { *; }

# Plugins
-keep interface ru.solrudev.ackpine.plugability.AckpinePlugin { *; }
-keep class * implements ru.solrudev.ackpine.plugability.AckpinePlugin { *; }