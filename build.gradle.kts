plugins { base }

group = "fr.noltox.hcplugins"
version = providers.gradleProperty("version").get()

subprojects {
    group = rootProject.group
    version = rootProject.version
}
