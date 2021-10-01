plugins {
    java
    `maven-publish`
    kotlin("jvm") version "1.5.21"
    kotlin("plugin.serialization") version "1.5.21"
}

group = "pw.dotdash"
version = "0.1.2"

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven")
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.21")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:1.0-M1-1.4.0-rc")

    api("org.spongepowered:configurate-core:4.1.2")

    testImplementation("org.spongepowered:configurate-hocon:4.1.2")
    testImplementation("junit:junit:4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}
