// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

plugins {
    `java-library`
    `maven-publish`
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            artifactId = base.archivesName.get()
            from(components["java"])
            suppressAllPomMetadataWarnings()

            pom {
                name = "CC: Tweaked"
                description = "CC: Tweaked is a fork of ComputerCraft, adding programmable computers, turtles and more to Minecraft."
                url = "https://github.com/cc-tweaked/CC-Tweaked"

                scm {
                    url = "https://github.com/cc-tweaked/CC-Tweaked.git"
                }

                issueManagement {
                    system = "github"
                    url = "https://github.com/cc-tweaked/CC-Tweaked/issues"
                }

                licenses {
                    license {
                        name = "ComputerCraft Public License, Version 1.0"
                        url = "https://github.com/cc-tweaked/CC-Tweaked/blob/HEAD/LICENSE"
                    }
                }
            }
        }
    }

    repositories {
        maven("https://maven.squiddev.cc") {
            name = "SquidDev"

            credentials(PasswordCredentials::class)
        }
    }
}
