#include "DspModule.h"

#define TAG "DspModule_JNI"
#include <Log.h>

DspModule::DspModule(JNIEnv *env, int index, const char *internalName, const char *displayName, bool enabled, bool requiresLock) : IJavaObject(env) {

    auto moduleClass = _env->FindClass("me/timschneeberger/rootlessjamesdsp/interop/structure/DspModule");
    if (moduleClass == nullptr)
    {
        LOGE("DspModule::ctor: DspModule class not found");
        return;
    }

    jmethodID methodInit = _env->GetMethodID(moduleClass, "<init>",
                                             "(ILjava/lang/String;Ljava/lang/String;ZZ)V");
    if (methodInit == nullptr)
    {
        LOGE("DspModule::ctor: DspModule<init>(ILjava/lang/String;Ljava/lang/String;ZZ)V method not found");
        return;
    }

    auto jInternalName = _env->NewStringUTF(internalName);
    auto jDisplayName = _env->NewStringUTF(displayName);
    innerObject = _env->NewObject(moduleClass, methodInit, index, jInternalName, jDisplayName, enabled, requiresLock);

    if (innerObject == nullptr)
    {
        LOGE("DspModule::ctor: Failed to allocate DspModule object");
        return;
    }

    _isValid = true;
}

jobject DspModule::getJavaReference() {
    return innerObject;
}

bool DspModule::isValid() const {
    return _isValid;
}
