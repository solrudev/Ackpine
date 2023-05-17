package ru.solrudev.ackpine

import android.net.Uri

public class UnsupportedUriSchemeException(uri: Uri) : IllegalArgumentException(
	"Scheme of the provided URI is not supported: $uri"
)