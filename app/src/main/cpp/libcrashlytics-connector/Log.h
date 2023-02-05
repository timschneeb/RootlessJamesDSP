//
// Created by tim on 08.07.22.
// Do NOT include this file in a header
//
#ifndef ROOTLESSJAMESDSP_LOG_H
#define ROOTLESSJAMESDSP_LOG_H

#ifndef TAG
#define TAG "Global_JNI"
#endif

#ifndef NO_CRASHLYTICS
#include "crashlytics.h"
#endif

#include <android/log.h>
#include <memory>
#include <string>
#include <stdexcept>

namespace log {
    void toCrashlytics(const char* level, const char* tag, const char* fmt, ...);
}

#define LOGE(...) \
    __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__); \
    log::toCrashlytics("E", TAG, __VA_ARGS__);
#define LOGD(...) \
    __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__); \
    log::toCrashlytics("D", TAG, __VA_ARGS__);
#define LOGI(...) \
    __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__); \
    log::toCrashlytics("I", TAG, __VA_ARGS__);
#define LOGW(...) \
    __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__); \
    log::toCrashlytics("W", TAG, __VA_ARGS__);
#define LOGF(...) \
    __android_log_print(ANDROID_LOG_FATAL, TAG, __VA_ARGS__); \
    log::toCrashlytics("F", TAG, __VA_ARGS__);
#define LOGV(...) \
    __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__); \
    log::toCrashlytics("V", TAG, __VA_ARGS__);

#endif //ROOTLESSJAMESDSP_LOG_H
