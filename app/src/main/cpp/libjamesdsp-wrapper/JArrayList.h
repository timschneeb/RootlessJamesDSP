//
// Created by tim on 01.07.22.
//

#ifndef ROOTLESSJAMESDSP_JARRAYLIST_H
#define ROOTLESSJAMESDSP_JARRAYLIST_H

#include "IJavaObject.h"

class JArrayList : IJavaObject {
public:
    JArrayList(JNIEnv* env);
    bool isValid() const;
    bool add(jobject object);
    jobject getJavaReference();

private:
    jclass arrayClass;
    jobject innerArrayList;
    jmethodID methodAdd;

    bool _isValid = false;
};


#endif //ROOTLESSJAMESDSP_JARRAYLIST_H
