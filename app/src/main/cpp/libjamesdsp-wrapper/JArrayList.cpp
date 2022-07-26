//
// Created by tim on 01.07.22.
//

#include "JArrayList.h"

#define TAG "JArrayList_JNI"
#include <Log.h>

JArrayList::JArrayList(JNIEnv* env) : IJavaObject(env)
{
    arrayClass = _env->FindClass("java/util/ArrayList");
    if (arrayClass == nullptr)
    {
        LOGE("JArrayList::ctor: java/util/ArrayList class not found");
        return;
    }

    jmethodID methodInit = _env->GetMethodID(arrayClass, "<init>", "()V");
    if (methodInit == nullptr)
    {
        LOGE("JArrayList::ctor: java/util/ArrayList<init>()V method not found");
        return;
    }

    innerArrayList = _env->NewObject(arrayClass, methodInit);
    if (innerArrayList == nullptr)
    {
        LOGE("JArrayList::ctor: Failed to allocate ArrayList object");
        return;
    }

    methodAdd = _env->GetMethodID(arrayClass, "add", "(Ljava/lang/Object;)Z");
    if (methodAdd == nullptr)
    {
        LOGE("JArrayList::ctor: java/util/ArrayList.add(Ljava/lang/Object;)Z method not found");
        return;
    }

    _isValid = true;
}

bool JArrayList::isValid() const {
    return _isValid;
}

bool JArrayList::add(jobject object) {
    return _env->CallBooleanMethod(innerArrayList, methodAdd, object);
}

jobject JArrayList::getJavaReference() {
    return innerArrayList;
}
