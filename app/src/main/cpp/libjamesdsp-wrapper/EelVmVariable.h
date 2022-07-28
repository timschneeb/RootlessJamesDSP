//
// Created by tim on 30.06.22.
//

#ifndef ROOTLESSJAMESDSP_EELVMVARIABLE_H
#define ROOTLESSJAMESDSP_EELVMVARIABLE_H

#include "IJavaObject.h"

class EelVmVariable : IJavaObject {
public:
    EelVmVariable(JNIEnv* env, const char* name, const char* value, bool isString);
    bool isValid() const;

    jobject getJavaReference();

private:
    jobject innerObject;
    bool _isValid = false;
};

#endif //ROOTLESSJAMESDSP_EELVMVARIABLE_H
