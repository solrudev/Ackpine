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

package ru.solrudev.ackpine.sample.install;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

@SuppressWarnings("unused")
public class HideFabOnScrollBehavior<V extends View> extends HideBottomViewOnScrollBehavior<V> {

	public HideFabOnScrollBehavior(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public HideFabOnScrollBehavior() {
		super();
	}

	@Override
	public boolean layoutDependsOn(@NonNull CoordinatorLayout parent, @NonNull V child, @NonNull View dependency) {
		return child instanceof ExtendedFloatingActionButton && dependency instanceof RecyclerView;
	}

	@Override
	public boolean onLayoutChild(@NonNull CoordinatorLayout parent, @NonNull V child, int layoutDirection) {
		var canScroll = false;
		final var childCount = parent.getChildCount();
		for (var i = 0; i < childCount; i++) {
			final var dependency = parent.getChildAt(i);
			if (dependency instanceof RecyclerView) {
				canScroll = dependency.canScrollVertically(1) || dependency.canScrollVertically(-1);
				break;
			}
		}
		if (child instanceof ExtendedFloatingActionButton && !canScroll && isScrolledDown()) {
			slideUp(child);
		}
		return super.onLayoutChild(parent, child, layoutDirection);
	}
}