plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("org.jetbrains.compose") version "1.5.12"
}

group = "com.envelopes"
version = "1.0.0"

val appName = "Envelopes"
val appVersion = "1.0.0"
val appDescription = "Envelope PDF Generator"
val appVendor = "David Morrison"
val winMenuGroup = "Envelopes"
val winUpgradeUuid = "a6c9cf85-3b91-4c12-9c92-7fcf1e2b6942"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "com.envelopes.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi
            )
            packageName = appName
            packageVersion = appVersion
            description = appDescription
            vendor = appVendor

            windows {
                menu = true
                shortcut = true
                dirChooser = true
                menuGroup = winMenuGroup
                upgradeUuid = winUpgradeUuid
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

// Compose's packageMsi can't use a custom WiX template, so the Windows installer is built here
// with jpackage from the Compose app image, using packaging/windows/main.wxs. That template asks
// whether to replace an existing installation or cancel.
tasks.register<Exec>("packageWindowsMsi") {
    group = "compose desktop"
    description = "Builds the Windows MSI installer with the replace-or-cancel prompt."
    onlyIf { System.getProperty("os.name").startsWith("Windows") }
    dependsOn("createDistributable")
    // Compose downloads WiX 3.11 via its unzipWix task (only registered on Windows, and only
    // when WIX_PATH isn't set); reuse it rather than relying on WiX being installed.
    dependsOn(provider { listOfNotNull(tasks.findByName("unzipWix")) })

    val appImage = layout.buildDirectory.dir("compose/binaries/main/app/$appName")
    val destDir = layout.buildDirectory.dir("compose/binaries/main/msi")
    val resourceDir = layout.projectDirectory.dir("packaging/windows")
    inputs.dir(appImage)
    inputs.dir(resourceDir)
    outputs.dir(destDir)

    executable = File(System.getProperty("java.home"), "bin/jpackage.exe").path
    args(
        "--type", "msi",
        "--app-image", appImage.get().asFile.path,
        "--dest", destDir.get().asFile.path,
        "--resource-dir", resourceDir.asFile.path,
        "--name", appName,
        "--app-version", appVersion,
        "--description", appDescription,
        "--vendor", appVendor,
        "--win-upgrade-uuid", winUpgradeUuid,
        "--win-menu",
        "--win-menu-group", winMenuGroup,
        "--win-shortcut",
        "--win-dir-chooser"
    )

    doFirst {
        project.delete(destDir)
        val wixDir = System.getenv("WIX_PATH") ?: (tasks.findByName("unzipWix") as? Copy)?.destinationDir?.path
        if (wixDir != null) {
            environment("PATH", wixDir + File.pathSeparator + System.getenv("PATH"))
        }
    }
}
