import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.6.0-SNAPSHOT"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("com.google.protobuf") version "0.8.13"
    kotlin("jvm") version "1.5.21"
    kotlin("plugin.spring") version "1.5.21"
}

buildscript {
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.13")
    }
}

group = "de.romqu"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_14

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

sourceSets {
    main {
        proto {
            srcDir("src/main/kotlin/de/romqu/schimmelhofapi/entrypoint/proto")
        }

        java {
            srcDir("build/generated/source/proto/main/java")
        }
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    implementation("redis.clients:jedis:3.6.3")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("org.jsoup:jsoup:1.14.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")

    implementation("com.google.protobuf:protobuf-java:3.17.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "16"
    }
    dependsOn("generateProto")
}

protobuf {

    protoc {
        artifact = "com.google.protobuf:protoc:3.17.3"
    }

    generateProtoTasks {
        ofSourceSet("main").forEach { task ->
            task.builtins {}
        }
    }
}
