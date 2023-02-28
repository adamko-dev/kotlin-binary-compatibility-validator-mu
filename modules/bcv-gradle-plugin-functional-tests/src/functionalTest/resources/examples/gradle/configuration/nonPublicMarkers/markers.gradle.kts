/*
 * Copyright 2016-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

configure<dev.adamko.kotlin.binary_compatibility_validator.BCVProjectExtension> {
    nonPublicMarkers.add("foo.HiddenField")
    nonPublicMarkers.add("foo.HiddenProperty")
}
