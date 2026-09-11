// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.vanillaextract.configurations.Capabilities.clientClasses
import cc.tweaked.vanillaextract.configurations.Capabilities.commonClasses

plugins {
    id("cc-tweaked.forge")
    id("cc-tweaked.publishing")
}

dependencies {
    api(project(":core-api"))

    compileOnly(commonClasses(project(":common-api")))
    clientApi(clientClasses(project(":common-api")))
    embeddedProject(project(":common-api"))
}

tasks.javadoc {
    include("dan200/computercraft/api/**/*.java")
}
