/*
 * Copyright (C) 2024 Ilya Fomichev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.solrudev.ackpine.sample.install

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

@Suppress("Unused")
class HideFabOnScrollBehavior<V : View> : HideBottomViewOnScrollBehavior<V> {

	constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
	constructor() : super()

	override fun layoutDependsOn(parent: CoordinatorLayout, child: V, dependency: View): Boolean {
		return child is ExtendedFloatingActionButton && dependency is RecyclerView
	}

	override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
		val dependency = parent.children.firstOrNull { it is RecyclerView }
			?: return super.onLayoutChild(parent, child, layoutDirection)
		val canScroll = dependency.canScrollVertically(1) || dependency.canScrollVertically(-1)
		if (child is ExtendedFloatingActionButton && !canScroll && isScrolledDown) {
			slideUp(child)
		}
		return super.onLayoutChild(parent, child, layoutDirection)
	}
}