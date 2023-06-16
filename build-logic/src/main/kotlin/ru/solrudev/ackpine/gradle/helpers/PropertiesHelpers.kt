package ru.solrudev.ackpine.gradle.helpers

import java.io.File
import java.util.Properties

inline fun <R> File.withProperties(action: Properties.() -> R): R = Properties().run {
	inputStream().use(::load)
	return action()
}