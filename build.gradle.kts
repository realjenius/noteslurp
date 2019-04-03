import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "realjenius"
version = "1.0-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.3.21"
}

repositories {
    mavenCentral()
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.github.microutils:kotlin-logging:1.6.24") {
        excludeKotlin()
    }
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.8") {
        excludeKotlin()
    }
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.9.8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.8")
    implementation("com.github.ajalt:clikt:1.7.0") {
        excludeKotlin()
    }
    implementation("io.projectreactor:reactor-core:3.2.8.RELEASE")
    implementation("com.evernote:evernote-api:1.25.1")
    api(kotlin("stdlib-jdk8"))
    api(kotlin("reflect"))
}

val fatJar = task("fat-jar", type = Jar::class) {
    baseName = "${project.name}-full"
    manifest {
        attributes["Implementation-Title"] = "NoteSlurp"
        attributes["Implementation-Version"] = version
        attributes["Main-Class"] = "realjenius.evernote.noteslurp.NoteSlurp"
    }
    from(configurations.compileClasspath.map { if (it.isDirectory) it else zipTree(it) })
    with(tasks["jar"] as CopySpec)
}

tasks {
    withType<KotlinCompile> { kotlinOptions.jvmTarget = "1.8" }
    "build" { dependsOn(fatJar) }
}


fun ExternalModuleDependency.excludeKotlin() = exclude(group = "org.jetbrains.kotlin", module = "*")