project(crashlytics-connector LANGUAGES CXX)

add_library( # Sets the name of the library.
        crashlytics-connector

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${CMAKE_CURRENT_LIST_DIR}/crashlytics.h
        ${CMAKE_CURRENT_LIST_DIR}/Log.h
        ${CMAKE_CURRENT_LIST_DIR}/Log.cpp
        )


find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

target_link_libraries( # Specifies the target library.
        crashlytics-connector

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})
