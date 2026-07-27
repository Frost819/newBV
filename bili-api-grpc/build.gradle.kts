
import com.google.protobuf.gradle.proto

plugins {
    alias(libs.plugins.google.protobuf)
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(libs.grpc.kotlin.stub)
    api(libs.grpc.okhttp)
    api(libs.grpc.protobuf)
    api(libs.grpc.stub)
    api(libs.protobuf.kotlin)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.truth)
}

sourceSets["main"].proto {
    srcDir("./proto")
    ProtobufConfiguration.excludeProtoFiles.forEach(::exclude)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.5"
    }
    plugins {
        create("java") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.72.0"
        }
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.72.0"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.builtins {
                named("java") {
                }
                create("kotlin") {
                }
            }
            it.plugins {
                create("grpc") {
                }
                create("grpckt") {
                }
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
