import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()
    
    js {
        browser()
        binaries.executable()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            
            // LWJGL for OpenGL
            implementation(libs.lwjgl)
            implementation(libs.lwjgl.opengl)
            implementation(libs.lwjgl.glfw)
            
            // JOML for math
            implementation(libs.joml)
            
            // Native libraries based on OS
            val lwjglVersion = libs.versions.lwjgl.get()
            val os = System.getProperty("os.name").lowercase()
            val arch = System.getProperty("os.arch").lowercase()
            
            when {
                os.contains("win") -> {
                    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-windows")
                    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-windows")
                    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-windows")
                }
                os.contains("mac") -> {
                    if (arch.contains("aarch64") || arch.contains("arm")) {
                        runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-macos-arm64")
                        runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-macos-arm64")
                        runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-macos-arm64")
                    } else {
                        runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-macos")
                        runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-macos")
                        runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-macos")
                    }
                }
                os.contains("nix") || os.contains("nux") -> {
                    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-linux")
                    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-linux")
                    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-linux")
                }
            }
        }
    }
}


compose.desktop {
    application {
        mainClass = "edu.ericm.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "edu.ericm"
            packageVersion = "1.0.0"
        }
    }
}
