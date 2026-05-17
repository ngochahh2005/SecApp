// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.sonar)
}

detekt {
    ignoreFailures = true
}

sonar {
    properties {
        property("sonar.projectKey", "NguyenTuKien_SecApp")
        property("sonar.organization", "nguyentukien")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}