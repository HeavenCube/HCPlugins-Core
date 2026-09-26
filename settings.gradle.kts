pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        exclusiveContent {
            forRepository {
                maven {
                    name = "papermc"
                    url = uri("https://repo.papermc.io/repository/maven-public/")
                }
            }
            filter {
                includeGroup("io.papermc.paper")
                includeGroup("com.mojang")
                includeGroup("net.md-5")
            }
        }
    }
}

rootProject.name = "HCPlugins-Core"
include(":core-api", ":core-plugin")
