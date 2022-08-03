//
// Created by tim on 01.07.22.
//

#include "EelVmVariable.h"

#define TAG "EelVmVariable_JNI"
#include <Log.h>

EelVmVariable::EelVmVariable(JNIEnv *env, const char *name, const char *value, bool isString) : IJavaObject(env) {

    auto arrayClass = _env->FindClass("me/timschneeberger/rootlessjamesdsp/interop/structure/EelVmVariable");
    if (arrayClass == nullptr)
    {
        LOGE("JArrayList::ctor: EelVmVariable class not found");
        return;
    }

    jmethodID methodInit = _env->GetMethodID(arrayClass, "<init>",
                                             "(Ljava/lang/String;Ljava/lang/String;Z)V");
    if (methodInit == nullptr)
    {
        LOGE("JArrayList::ctor: EelVmVariable<init>(Ljava/lang/String;Ljava/lang/String;Z)V method not found");
        return;
    }

    auto jName = _env->NewStringUTF(name);
    auto jValue = _env->NewStringUTF(value);
    innerObject = _env->NewObject(arrayClass, methodInit, jName, jValue, isString);

    if (innerObject == nullptr)
    {
        LOGE("JArrayList::ctor: Failed to allocate EelVmVariable object");
        return;
    }

    _isValid = true;
}

jobject EelVmVariable::getJavaReference() {
    return innerObject;
}

bool EelVmVariable::isValid() const {
    return _isValid;
}

