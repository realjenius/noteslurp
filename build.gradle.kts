import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "realjenius"
version = "1.1-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.4.20"
    id("com.github.johnrengelman.shadow").version("6.0.0")
}

repositories {
    mavenCentral()
    jcenter()
}

tasks.withType<Jar> {
    manifest {
        attributes["Implementation-Title"] = "NoteSlurp"
        attributes["Implementation-Version"] = archiveVersion
        attributes["Main-Class"] = "realjenius.evernote.noteslurp.NoteSlurp"
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.github.microutils:kotlin-logging:1.8.3") {
        excludeKotlin()
    }
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.1") {
        excludeKotlin()
    }
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.11.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.1")
    implementation("com.github.ajalt:clikt:2.8.0") {
        excludeKotlin()
    }
    implementation("io.projectreactor:reactor-core:3.3.7.RELEASE")
    implementation("com.evernote:evernote-api:1.25.1")
    api(kotlin("stdlib-jdk8"))
    api(kotlin("reflect"))

    implementation("io.javalin:javalin:3.12.0")
    implementation("at.favre.lib:bcrypt:0.9.0")

}

tasks {
    withType<KotlinCompile> { kotlinOptions.jvmTarget = "1.8" }
    "build" { dependsOn(shadowJar) }
}


fun ExternalModuleDependency.excludeKotlin() = exclude(group = "org.jetbrains.kotlin", module = "*")