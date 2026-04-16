---
icon: lucide/scroll-text
---

Logging
=======

Ackpine can emit structured log events for runtime operations such as session lifecycle transitions, errors, and warnings. Logging is **disabled by default** — no output is produced until a logger is installed.

Enabling logcat logging
-----------------------

The quickest way to enable logging is to use the built-in [`AckpineLogger.Logcat`](../api/ackpine-core/ru.solrudev.ackpine/-ackpine-logger/-logcat/index.html) implementation, which forwards all events to Android logcat:

=== "Kotlin"

    ```kotlin
    Ackpine.enableLogcatLogger()
    ```

=== "Java"

    ```java
    Ackpine.enableLogcatLogger();
    ```

This is equivalent to calling `Ackpine.setLogger(AckpineLogger.Logcat())`.

Custom logger
-------------

For custom log routing — for example, forwarding to a third-party logging framework — implement the [`AckpineLogger`](../api/ackpine-core/ru.solrudev.ackpine/-ackpine-logger/index.html) interface and pass it to [`Ackpine.setLogger()`](../api/ackpine-core/ru.solrudev.ackpine/-ackpine/set-logger.html):

=== "Kotlin"

    ```kotlin
    Ackpine.setLogger { level, tag, template, throwable, args ->
        val message = template.format(*args)
        when (level) {
            AckpineLogger.Level.VERBOSE -> Timber.tag(tag).v(throwable, message)
            AckpineLogger.Level.DEBUG -> Timber.tag(tag).d(throwable, message)
            AckpineLogger.Level.INFO -> Timber.tag(tag).i(throwable, message)
            AckpineLogger.Level.WARN -> Timber.tag(tag).w(throwable, message)
            AckpineLogger.Level.ERROR -> Timber.tag(tag).e(throwable, message)
        }
    }
    ```

=== "Java"

    ```java
    Ackpine.setLogger((level, tag, template, throwable, args) -> {
        String message = String.format(Locale.ROOT, template, args);
        switch (level) {
            case VERBOSE -> Timber.tag(tag).v(throwable, message);
            case DEBUG -> Timber.tag(tag).d(throwable, message);
            case INFO -> Timber.tag(tag).i(throwable, message);
            case WARN -> Timber.tag(tag).w(throwable, message);
            case ERROR -> Timber.tag(tag).e(throwable, message);
        }
    });
    ```

!!! Note
    Message templates are [`String.format`](https://developer.android.com/reference/java/lang/String#format(java.lang.String,java.lang.Object...))-compatible. The `args` array must be applied to `template` to produce the final message. [`AckpineLogger.Logcat`](../api/ackpine-core/ru.solrudev.ackpine/-ackpine-logger/-logcat/index.html) renders templates using `Locale.ROOT`.

Disabling logging
-----------------

Pass `null` to `Ackpine.setLogger()` to remove the current logger and stop all output:

=== "Kotlin"

    ```kotlin
    Ackpine.setLogger(null)
    ```

=== "Java"

    ```java
    Ackpine.setLogger(null);
    ```

Log levels
----------

[`AckpineLogger.Level`](../api/ackpine-core/ru.solrudev.ackpine/-ackpine-logger/-level/index.html) mirrors the standard Android log levels:

| Level     | Description                                    |
|-----------|------------------------------------------------|
| `VERBOSE` | Fine-grained diagnostic detail                 |
| `DEBUG`   | Diagnostic messages useful for troubleshooting |
| `INFO`    | Normal operation and notable events            |
| `WARN`    | Unexpected or recoverable conditions           |
| `ERROR`   | Failures or invalid conditions                 |

!!! Note
    If the logger itself throws an exception, Ackpine catches it and logs a fallback message to logcat under the `Ackpine` tag.