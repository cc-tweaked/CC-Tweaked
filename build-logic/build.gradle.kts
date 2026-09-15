// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

plugins {
    alias(libs.plugins.gradleVersions)
    alias(libs.plugins.versionCatalogUpdate)
}

versionCatalogUpdate {
    sortByKey = false
    keep { keepUnusedVersions = true }
    catalogFile = file("../gradle/libs.versions.toml")
}
