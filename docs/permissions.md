---
hide:
  - navigation
---

Permissions
===========

Ackpine adds the following permissions to `AndroidManifest.xml`:

- `WRITE_EXTERNAL_STORAGE` — used by `INTENT_BASED` package installer to create temporary APK copy;
- `REQUEST_INSTALL_PACKAGES` and `REQUEST_DELETE_PACKAGES` — self-explanatory;
- `VIBRATE` — to be able to set heads-up notifications' vibration when using `DEFERRED` confirmation;
- `POST_NOTIFICATIONS` — for posting notifications when using `DEFERRED` confirmation;
- `UPDATE_PACKAGES_WITHOUT_USER_ACTION` — to be able to leverage `requireUserAction` feature on API 31+;
- `ENFORCE_UPDATE_OWNERSHIP` — to be able to leverage update ownership enforcement feature on API 34+.

If you don't need some of the features listed above and don't want to have unneeded permissions in your app, you can remove them from the resulting merged `AndroidManifest.xml`, but do so carefully to not break the library functionality you use:

```xml
<uses-permission
    android:name="android.permission.PERMISSION_TO_REMOVE"
    tools:node="remove" />
```

For getting correct uninstall results, your app must have permission to query the uninstalled package. Add this to your `AndroidManifest.xml`:
```xml
<queries>
    <package android:name="com.your.uninstalled.package" />
</queries>
```

Or, if you must, add the `QUERY_ALL_PACKAGES` permission. Be careful about not violating Google Play policies in regards to this permission.