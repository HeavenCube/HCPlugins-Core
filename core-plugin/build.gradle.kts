plugins {
    java
    alias(libs.plugins.shadow)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

dependencies {
    implementation(project(":core-api"))
    compileOnly(libs.paperApi)

    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.junitPlatformLauncher)
    testCompileOnly(libs.paperApi)
    testRuntimeOnly(libs.paperApi)
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:all")
}

tasks.processResources {
    val pluginVersion = project.version.toString()
    inputs.property("version", pluginVersion)
    filesMatching("paper-plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.jar { enabled = false }

tasks.shadowJar {
    archiveFileName.set("HCCore.jar")
    archiveClassifier.set("")
}

tasks.build { dependsOn(tasks.shadowJar) }
tasks.test { useJUnitPlatform() }
