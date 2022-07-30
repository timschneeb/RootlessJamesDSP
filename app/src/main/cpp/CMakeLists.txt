# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.18.1)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -ffunction-sections -fdata-sections -Ofast -ftree-vectorize -DDEBUG")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffunction-sections -fdata-sections -Ofast -ftree-vectorize -DDEBUG")

# Declares and names the project.
project(jamesdsp LANGUAGES C)
project(jamesdsp-wrapper LANGUAGES CXX)

include(libcrashlytics-connector/CMakeLists.txt)
include(libjdspimptoolbox/CMakeLists.txt)
include(libjamesdsp-wrapper/CMakeLists.txt)

file(GLOB_RECURSE LIBJAMESDSP_SOURCE_FILES CONFIGURE_DEPENDS
        ${CMAKE_CURRENT_LIST_DIR}/libjamesdsp/Main/libjamesdsp/jni/jamesdsp/jdsp/*.c
        ${CMAKE_CURRENT_LIST_DIR}/libjamesdsp/Main/libjamesdsp/jni/jamesdsp/jdsp/*.h)

add_library( # Sets the name of the library.
        jamesdsp

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${LIBJAMESDSP_SOURCE_FILES}
        EELStdOutExtension.c
        EELStdOutExtension.h)

target_compile_options(jamesdsp PRIVATE -O2)
target_compile_options(jamesdsp PRIVATE -std=gnu11)
target_include_directories(jamesdsp PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/libjamesdsp/Main/libjamesdsp/jni/jamesdsp/jdsp/)

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

target_link_libraries( # Specifies the target library.
        jamesdsp

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})
