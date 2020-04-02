# configurate-serialization

[Kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) support for [configurate](https://github.com/SpongePowered/Configurate).

# Usage

### Gradle

```kotlin
plugins {
    kotlin("jvm") version "1.3.71"
    kotlin("plugin.serialization") version "1.3.71"
}

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven")
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")
    
    implementation("org.spongepowered:configurate-core:3.6")

    implementation("com.github.ItsDoot:configurate-serialization:0.1.0")
}
```

### Example

```kotlin
@Serializable
data class MyConfig(val enabled: Boolean, val limit: Int, val name: String)

fun parse(node: ConfigurationNode) {
    val config = ConfigurationNodeParser.parse(node, MyConfig.serializer())
    println("enabled? ${config.enabled}")
    println("limit = ${config.limit}")
    println("name = ${config.name}")
}
```