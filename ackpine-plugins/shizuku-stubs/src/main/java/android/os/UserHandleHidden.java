/*
 * Copyright (C) 2026 Ilya Fomichev
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

package android.os;

import dev.rikka.tools.refine.RefineAs;

/**
 * Stub for {@link UserHandle} which will be renamed and removed during transformation.
 */
@RefineAs(UserHandle.class)
public class UserHandleHidden {
	public static int myUserId() {
		throw new UnsupportedOperationException();
	}
}