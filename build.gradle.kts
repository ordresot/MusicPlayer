plugins {
    // Exact versions controlled by libs.versions.toml
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
}

buildscript {
    dependencies {
        // classpath(libs.android.gradle.plugin) // Managed by plugins block + catalog
    }
}
