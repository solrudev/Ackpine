# Serializable
-keep class ru.solrudev.ackpine.session.parameters.ResolvableString { *; }
-keep class ru.solrudev.ackpine.session.parameters.ResolvableString$* { *; }
-keep class * extends ru.solrudev.ackpine.session.parameters.ResolvableString$Resource
-keep class ru.solrudev.ackpine.session.parameters.DefaultNotificationString { *; }
-keep class ru.solrudev.ackpine.session.parameters.Empty { *; }
-keep class ru.solrudev.ackpine.session.parameters.Raw { *; }
-keep class ru.solrudev.ackpine.installer.InstallFailure { *; }
-keep class ru.solrudev.ackpine.installer.InstallFailure$* { *; }
-keep class ru.solrudev.ackpine.uninstaller.UninstallFailure { *; }
-keep class ru.solrudev.ackpine.uninstaller.UninstallFailure$* { *; }

# Fields updated with Atomic*FieldUpdater
-keepclassmembers class ru.solrudev.ackpine.impl.session.AbstractSession {
    private volatile ru.solrudev.ackpine.session.Session$State state;
}