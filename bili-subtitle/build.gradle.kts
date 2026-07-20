plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}


dependencies {
    implementation(libs.kotlinx.serialization)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.truth)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
