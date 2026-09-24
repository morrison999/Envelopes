plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("org.jetbrains.compose") version "1.5.12"
}

group = "com.envelopes"
version = "1.0.0"

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
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = "Envelopes"
            packageVersion = "1.0.0"
            description = "Envelope PDF Generator"
            vendor = "David Morrison"

            windows {
                console = true
                menu = true
                shortcut = true
                dirChooser = true
                menuGroup = "Envelopes"
                upgradeUuid = "a6c9cf85-3b91-4c12-9c92-7fcf1e2b6942"
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
