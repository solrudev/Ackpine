Architecture
============

Ackpine library has a concept of `Session`. `Session` manages the flow of installation or uninstallation process.

One can launch multiple different sessions at once, but can't create them directly. To create a session, one needs to use `PackageInstaller` or `PackageUninstaller`.

In essence, `PackageInstaller` is a _repository_ of `ProgressSessions`, and `PackageUninstaller` is a _repository_ of `Sessions`. They track and persist every session launched.

Session by itself is passive, it doesn't do anything until client code says so. One can say that session is a finite-state machine which transfers from one state to another, but for this to be possible client needs to react to state changes and call necessary methods on the session. This is done to make sessions persistable and suspendable.

If any of the steps while the session is active is interrupted (e.g. with process death), it can be re-executed later by examining last session's state and executing the steps which weren't finished. The library provides ready-to-use implementations of such listeners in the form of `Session.TerminalStateListener` and `Session.await()`. They can be safely re-attached after interruption.

`Uri` is chosen as a sole input type of APKs. It makes them persistable and allows to plug in any APK source via `ContentProvider`. The library leverages this in `ackpine-splits` and `ackpine-assets` modules to read APKs from zipped files and app's asset files respectively.