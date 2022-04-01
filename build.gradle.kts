import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.6.4"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("com.adarshr.test-logger") version "3.2.0"

    kotlin("jvm") version "1.6.10"
    kotlin("kapt") version "1.6.10"
    kotlin("plugin.spring") version "1.6.20"
}

group = "org.slorinc"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    maven {
        url = uri("https://jitpack.io")
    }
    mavenCentral()
}

configurations.all {
    exclude(mapOf("module" to "spring-boot-starter-logging"))
}

dependencies {
    // bootstrap
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework:spring-aspects")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.springdoc:springdoc-openapi-ui:${properties["open.api.version"]}")
    implementation("org.springdoc:springdoc-openapi-data-rest:${properties["open.api.version"]}")
    implementation("org.springdoc:springdoc-openapi-kotlin:${properties["open.api.version"]}")

    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.1.0")
    implementation("org.apache.logging.log4j:log4j-api:${properties["log4j.version"]}")
    implementation("org.apache.logging.log4j:log4j-core:${properties["log4j.version"]}")
    implementation("com.lmax:disruptor:3.4.4")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    implementation("com.ninja-squad:springmockk:3.1.1")

}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
