#ifndef ROOTLESSJAMESDSP_DSPMODULE_H
#define ROOTLESSJAMESDSP_DSPMODULE_H

#include "IJavaObject.h"

class DspModule : IJavaObject {
public:
    DspModule(JNIEnv* env, int index, const char* internalName, const char* displayName, bool enabled, bool requiresLock);
    bool isValid() const;

    jobject getJavaReference();

private:
    jobject innerObject;
    bool _isValid = false;
};

#endif //ROOTLESSJAMESDSP_DSPMODULE_H
