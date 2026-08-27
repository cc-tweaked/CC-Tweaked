// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

plugins {
    id("cc-tweaked.java-convention")
    id("cc-tweaked.publishing")
    id("cc-tweaked")
}

dependencies {
    compileOnlyApi(libs.bundles.annotations)
}

// Don't build Javadoc here. We build combined docs in the common-api project.
tasks.javadoc { isEnabled = false }

cct.linters(minecraft = false, loader = null)
