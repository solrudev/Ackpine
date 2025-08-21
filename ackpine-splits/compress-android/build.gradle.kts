/*
 * Copyright (C) 2025 Ilya Fomichev
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

import ru.solrudev.ackpine.gradle.versioning.ackpineVersion

description = "Fork of Apache Commons Compress, compatible with Android API 19+. " +
		"Contains only a subset of Compress API."
version = "1.28.0-${ackpineVersion.get()}"

plugins {
	id("ru.solrudev.ackpine.library.base")
	id("ru.solrudev.ackpine.library-publish")
	id("ru.solrudev.ackpine.optional-dependencies")
}

ackpine {
	id = "compress-android"
	minSdk = 19
	artifact {
		name = "Ackpine Compress"
		inceptionYear = "2025"
	}
}

mavenPublishing {
	pom {
		developers {
			developer {
				name = "Torsten Curdt"
				id = "tcurdt"
				email = "tcurdt at apache.org"
			}
			developer {
				name = "Stefan Bodewig"
				id = "bodewig"
				email = "bodewig at apache.org"
			}
			developer {
				name = "Sebastian Bazley"
				id = "sebb"
				email = "sebb at apache.org"
			}
			developer {
				name = "Christian Grobmeier"
				id = "grobmeier"
				email = "grobmeier at apache.org"
			}
			developer {
				name = "Julius Davies"
				id = "julius"
				email = "julius at apache.org"
			}
			developer {
				name = "Damjan Jovanovic"
				id = "damjan"
				email = "damjan at apache.org"
			}
			developer {
				name = "Emmanuel Bourg"
				id = "ebourg"
				email = "ebourg at apache.org"
			}
			developer {
				id = "ggregory"
				name = "Gary Gregory"
				email = "ggregory at apache.org"
				url = "https://www.garygregory.com"
				organization = "The Apache Software Foundation"
				organizationUrl = "https://www.apache.org/"
				roles.add("PMC Member")
				timezone = "America/New_York"
				properties = mapOf(
					"picUrl" to "https://people.apache.org/~ggregory/img/garydgregory80.png"
				)
			}
			developer {
				name = "Rob Tompkins"
				id = "chtompki"
				email = "chtompki at apache.org"
			}
			developer {
				name = "Peter Alfred Lee"
				id = "peterlee"
				email = "peterlee at apache.org"
			}
		}
		contributors {
			contributor {
				name = "Wolfgang Glas"
				email = "wolfgang.glas at ev-i.at"
			}
			contributor {
				name = "Christian Kohlschütte"
				email = "ck@newsclub.de"
			}
			contributor {
				name = "Bear Giles"
				email = "bgiles@coyotesong.com"
			}
			contributor {
				name = "Michael Kuss"
				email = "mail at michael minus kuss.de"
			}
			contributor {
				name = "Lasse Collin"
				email = "lasse.collin@tukaani.org"
			}
			contributor {
				name = "John Kodis"
			}
			contributor {
				name = "BELUGA BEHR"
			}
			contributor {
				name = "Simon Spero"
				email = "sesuncedu@gmail.com"
			}
			contributor {
				name = "Michael Hausegger"
				email = "hausegger.michael@googlemail.com"
			}
			contributor {
				name = "Arturo Bernal"
				email = "arturobernalg@yahoo.com"
			}
		}
	}
}

dependencies {
	implementation(androidx.core)
	implementation(libs.apache.commons.io)
	optional(libs.zstd)
	optional(libs.xz)
}