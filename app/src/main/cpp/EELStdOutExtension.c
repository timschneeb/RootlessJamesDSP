#include "EELStdOutExtension.h"
#include "Effects/eel2/ns-eel.h"

static stdOutHandler _stdOutHandlerPtr = NULL;
static void* _stdOutHandlerUserPtr = NULL;

void writeCircularStringBuf(char *cmdCur)
{
    if(_stdOutHandlerPtr != NULL)
    {
        _stdOutHandlerPtr(cmdCur, _stdOutHandlerUserPtr);
    }
}

void setStdOutHandler(stdOutHandler funcPtr, void* userData)
{
    _stdOutHandlerPtr = funcPtr;
    _stdOutHandlerUserPtr = userData;
}

int isStdOutHandlerSet()
{
    return _stdOutHandlerPtr != NULL;
}
