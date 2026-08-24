

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    idea
}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        kotlin.srcDir("src/integrationTest/kotlin")
        resources.srcDir("src/integrationTest/resources")
    }
}

configurations {
    named("integrationTestImplementation") { extendsFrom(testImplementation.get()) }
    named("integrationTestRuntimeOnly") { extendsFrom(testRuntimeOnly.get()) }
}

dependencies {
    implementation(project(":bili-api-grpc"))

    implementation(libs.brotli)
    implementation(libs.jsoup)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.serialization.kotlinx)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

val integrationTest =
    tasks.register<Test>("integrationTest") {
        useJUnitPlatform()
        testClassesDirs = sourceSets.getByName("integrationTest").output.classesDirs
        classpath = sourceSets.getByName("integrationTest").runtimeClasspath
        maxParallelForks = 1
        forkEvery = 1
    }
