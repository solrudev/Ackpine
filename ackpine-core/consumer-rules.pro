# Serializable
-keep class ru.solrudev.ackpine.installer.InstallFailure { *; }
-keep class ru.solrudev.ackpine.installer.InstallFailure$* { *; }
-keep class ru.solrudev.ackpine.uninstaller.UninstallFailure { *; }
-keep class ru.solrudev.ackpine.uninstaller.UninstallFailure$* { *; }

# Fields updated with Atomic*FieldUpdater
-keepclassmembers class ru.solrudev.ackpine.impl.session.AbstractSession {
    private volatile ru.solrudev.ackpine.session.Session$State state;
}