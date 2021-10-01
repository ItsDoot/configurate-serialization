# configurate-serialization

[Kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) support for [configurate](https://github.com/SpongePowered/Configurate).

# Usage

### Gradle

```kotlin
plugins {
    kotlin("jvm") version "1.5.21"
    kotlin("plugin.serialization") version "1.5.21"
}

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven")
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.21")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:1.0-M1-1.4.0-rc")
    
    implementation("org.spongepowered:configurate-core:4.1.2")

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
