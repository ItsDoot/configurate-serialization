plugins {
    java
    kotlin("jvm") version "1.3.71"
    kotlin("plugin.serialization") version "1.3.71"
}

group = "pw.dotdash"
version = "0.1.1"

apply(plugin = "maven")

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven")
}

dependencies {
    api(kotlin("stdlib-jdk8"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")

    api("org.spongepowered:configurate-core:3.6")

    testImplementation("org.spongepowered:configurate-hocon:3.6")
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