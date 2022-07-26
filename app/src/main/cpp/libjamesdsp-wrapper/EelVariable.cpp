//
// Created by tim on 01.07.22.
//

#include "EelVariable.h"

#define TAG "EelVariable_JNI"
#include <Log.h>

EelVariable::EelVariable(JNIEnv *env, const char *name, const char *value, bool isString) : IJavaObject(env) {

    auto arrayClass = _env->FindClass("me/timschneeberger/rootlessjamesdsp/native/struct/EelVariable");
    if (arrayClass == nullptr)
    {
        LOGE("JArrayList::ctor: EelVariable class not found");
        return;
    }

    jmethodID methodInit = _env->GetMethodID(arrayClass, "<init>",
                                             "(Ljava/lang/String;Ljava/lang/String;Z)V");
    if (methodInit == nullptr)
    {
        LOGE("JArrayList::ctor: EelVariable<init>(Ljava/lang/String;Ljava/lang/String;Z)V method not found");
        return;
    }

    auto jName = _env->NewStringUTF(name);
    auto jValue = _env->NewStringUTF(value);
    innerObject = _env->NewObject(arrayClass, methodInit, jName, jValue, isString);

    if (innerObject == nullptr)
    {
        LOGE("JArrayList::ctor: Failed to allocate EelVariable object");
        return;
    }

    _isValid = true;
}

jobject EelVariable::getJavaReference() {
    return innerObject;
}

bool EelVariable::isValid() const {
    return _isValid;
}

