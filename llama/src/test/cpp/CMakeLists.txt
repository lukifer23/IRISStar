cmake_minimum_required(VERSION 3.10)
project(jni_utils_test)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)
FetchContent_Declare(
  googletest
  URL https://github.com/google/googletest/archive/refs/tags/v1.14.0.zip
)
# For Windows: Prevent overriding the parent project's compiler/linker settings
set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(googletest)

find_package(JNI REQUIRED)

add_executable(jni_utils_test jni_utils_test.cpp)
target_include_directories(jni_utils_test PRIVATE ../../main/cpp ${JNI_INCLUDE_DIRS})
target_link_libraries(jni_utils_test gtest_main ${JNI_LIBRARIES})

enable_testing()
add_test(NAME jni_utils_test COMMAND jni_utils_test)
