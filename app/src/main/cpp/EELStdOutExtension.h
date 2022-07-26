#ifndef EELSTDOUTEXTENSION_H
#define EELSTDOUTEXTENSION_H

typedef void (*stdOutHandler)(const char*, void*);

extern void setStdOutHandler(stdOutHandler funcPtr, void* userData);
extern int isStdOutHandlerSet();

#endif // EELSTDOUTEXTENSION_H
