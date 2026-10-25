plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "cl.electricalc.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "cl.electricalc.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main") {
            assets.srcDir(layout.buildDirectory.dir("generated/catalogo/assets"))
        }
    }
}

// El menú sale de spec/: catalogo-v2.json (estructura y RIC), catalogo.json (títulos en inglés e
// íconos) y sinonimos.json (buscador). Se copian a los assets en cada compilación para no duplicarlos.
val copiarCatalogo by tasks.registering(Copy::class) {
    from(rootProject.files("spec/catalogo.json", "spec/catalogo-v2.json", "spec/sinonimos.json"))
    into(layout.buildDirectory.dir("generated/catalogo/assets"))
}
tasks.named("preBuild") { dependsOn(copiarCatalogo) }

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core-calc"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    // org.json real: el de android.jar en los tests unitarios es solo un stub.
    testImplementation(libs.org.json)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
