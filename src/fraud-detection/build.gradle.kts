import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.google.protobuf.gradle.*

plugins {
    kotlin("jvm") version "1.8.21"
    application
    id("java")
    id("idea")
    id("com.google.protobuf") version "0.9.4"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "io.opentelemetry"
version = "1.0"

val grpcVersion = "1.70.0"
val protobufVersion = "4.29.3"

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}

dependencies {
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    testImplementation(kotlin("test"))
    implementation(kotlin("script-runtime"))
    
    // Simplify Kafka dependencies
    implementation("org.apache.kafka:kafka-clients:3.7.0")
    //implementation("org.apache.kafka:kafka_2.13:3.9.0")  // This includes CommonClientConfigs
    
    implementation("com.google.api.grpc:proto-google-common-protos:2.53.0")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-netty:$grpcVersion")
    implementation("io.grpc:grpc-services:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:1.3.0")  // Added for Kotlin gRPC support
    implementation("io.opentelemetry:opentelemetry-api:1.47.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.47.0")
    implementation("io.opentelemetry:opentelemetry-extension-annotations:1.18.0")
    implementation("org.apache.logging.log4j:log4j-core:2.24.3")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("com.google.protobuf:protobuf-kotlin:$protobufVersion")
    implementation("dev.openfeature:sdk:1.14.1")
    implementation("dev.openfeature.contrib.providers:flagd:0.11.3")

    if (JavaVersion.current().isJava9Compatible) {
        implementation("javax.annotation:javax.annotation-api:1.3.2")
    }
}

tasks {
    shadowJar {
        mergeServiceFiles()
    }
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        id("kotlin") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.3.0:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc")
                id("kotlin")
            }
            // Remove the kotlin builtin as it's already handled by the kotlin plugin
            // task.builtins {
            //     id("kotlin")
            // }
            task.generateDescriptorSet = true
        }
    }
    sourceSets {
        main {
            proto {
                srcDir("../../pb")
            }
        }
    }
}

application {
    mainClass.set("frauddetection.MainKt")
}

tasks.jar {
    manifest.attributes["Main-Class"] = "frauddetection.MainKt"
}
