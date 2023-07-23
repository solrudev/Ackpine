package ru.solrudev.ackpine.sample.util

import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import ru.solrudev.ackpine.sample.R

fun Fragment.findAppBarLayout(): AppBarLayout {
	return requireActivity().findViewById(R.id.appBarLayout_nav_host)
}