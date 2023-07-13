project(jamesdsp-wrapper LANGUAGES CXX)

file(GLOB_RECURSE SOURCE_FILES CONFIGURE_DEPENDS ${CMAKE_CURRENT_LIST_DIR}/*.cpp ${CMAKE_CURRENT_LIST_DIR}/*.h)

add_library( # Sets the name of the library.
        jamesdsp-wrapper

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${SOURCE_FILES}
        )


find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

target_include_directories(jamesdsp-wrapper PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/libjamesdsp/Main/libjamesdsp/jni/jamesdsp/jdsp/)
target_include_directories(jamesdsp-wrapper PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/libjdspimptoolbox/main)
target_include_directories(jamesdsp-wrapper PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/libcrashlytics-connector)

# Link libjamesdsp with the wrapper library
target_link_libraries( # Specifies the target library.
        jamesdsp-wrapper
        jdspimprestoolbox
        crashlytics-connector
        jamesdsp

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})
