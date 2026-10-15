plugins {
    java
    application
    id("org.jetbrains.kotlin.jvm") version "2.4.20"
    id("org.javamodularity.moduleplugin") version "1.8.15"
    id("org.openjfx.javafxplugin") version "0.0.13"
}

group = "org.dilbo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val coroutinesVersion = "1.11.0"
val ktorVersion = "3.5.2"
val controlsfxVersion = "11.2.1"
val javafxVersion = "21.0.12"
val junitVersion = "5.12.1"

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("org.dilbo.converter")
    mainClass.set("org.dilbo.converter.LauncherKt")
}
kotlin {
    jvmToolchain(17)
}

javafx {
    version = "21.0.6"
    modules = listOf("javafx.controls")
}

// needed to avoid duplicate error like "Entry org/dilbo/converter/Main$Companion.class is a duplicate but no duplicate handling strategy has been set."
tasks {
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")

    implementation("org.controlsfx:controlsfx:$controlsfxVersion")

    implementation("org.openjfx:javafx-base:$javafxVersion")
    implementation("org.openjfx:javafx-controls:$javafxVersion")
    implementation("org.openjfx:javafx-graphics:$javafxVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}
