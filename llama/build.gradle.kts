plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "android.llama.cpp"
    compileSdk = 35

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
        externalNativeBuild {
            cmake {
                arguments += "-DLLAMA_BUILD_COMMON=ON"
                arguments += "-DCMAKE_BUILD_TYPE=Release"
                arguments += "-DLLAMA_CURL=OFF"
                // Enable GPU backends as shared libs for runtime loading
                arguments += "-DGGML_BACKEND_SHARED=ON"
                // Enable Vulkan backend (we link against NDK libvulkan stubs at API 33)
                arguments += "-DGGML_VULKAN=ON"
                arguments += "-DGGML_OPENCL=ON"
                arguments += "-DGGML_OPENCL_EMBED_KERNELS=ON"
                arguments += "-DGGML_OPENCL_USE_ADRENO_KERNELS=ON"
                // Point Vulkan shaders to host glslc
                val glslcPath = System.getenv("GLSLC") ?: "/opt/homebrew/bin/glslc"
                arguments += "-DVulkan_GLSLC_EXECUTABLE=${glslcPath}"
                // Point Vulkan headers include dir (Homebrew Vulkan-Headers)
                arguments += "-DVulkan_INCLUDE_DIR=/opt/homebrew/include"
                arguments += "-DVulkan_INCLUDE_DIRS=/opt/homebrew/include"
                // Use a modern Android platform so NDK Vulkan stubs export 1.2 symbols
                arguments += "-DANDROID_PLATFORM=android-33"
                // Ensure Vulkan-Headers are visible to the cross-compiler
                cppFlags += listOf("-I/opt/homebrew/include")
                arguments += listOf()

                cppFlags("")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.compose.runtime.android)
    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
