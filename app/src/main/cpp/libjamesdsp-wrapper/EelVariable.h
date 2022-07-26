//
// Created by tim on 30.06.22.
//

#ifndef ROOTLESSJAMESDSP_EELVARIABLE_H
#define ROOTLESSJAMESDSP_EELVARIABLE_H

#include "IJavaObject.h"

class EelVariable : IJavaObject {
public:
    EelVariable(JNIEnv* env, const char* name, const char* value, bool isString);
    bool isValid() const;

    jobject getJavaReference();

private:
    jobject innerObject;
    bool _isValid = false;
};

#endif //ROOTLESSJAMESDSP_EELVARIABLE_H
