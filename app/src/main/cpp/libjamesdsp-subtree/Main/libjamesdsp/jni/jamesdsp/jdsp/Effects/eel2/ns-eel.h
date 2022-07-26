/*
  Nullsoft Expression Evaluator Library (NS-EEL)
  Copyright (C) 1999-2003 Nullsoft, Inc.
  ns-eel.h: main application interface header
  This software is provided 'as-is', without any express or implied
  warranty.  In no event will the authors be held liable for any damages
  arising from the use of this software.
  Permission is granted to anyone to use this software for any purpose,
  including commercial applications, and to alter it and redistribute it
  freely, subject to the following restrictions:
  1. The origin of this software must not be misrepresented; you must not
     claim that you wrote the original software. If you use this software
     in a product, an acknowledgment in the product documentation would be
     appreciated but is not required.
  2. Altered source versions must be plainly marked as such, and must not be
     misrepresented as being the original software.
  3. This notice may not be removed or altered from any source distribution.
*/
#ifndef __NS_EEL_H__
#define __NS_EEL_H__
// put standard includes here
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <float.h>
#ifdef _MSC_VER
typedef __int64 WDL_INT64;
typedef unsigned __int64 WDL_UINT64;
#else
typedef long long WDL_INT64;
typedef unsigned long long WDL_UINT64;
#endif
#ifdef _WIN32
#include <windows.h>
#else
#include <stdint.h>
typedef intptr_t INT_PTR;
typedef uintptr_t UINT_PTR;
#endif
#if !defined(max)
#define max(x,y) ((x)<(y)?(y):(x))
#define min(x,y) ((x)<(y)?(x):(y))
#endif
#ifndef _WIN32
#ifndef strnicmp 
#define strnicmp(x,y,z) strncasecmp(x,y,z)
#endif
#ifndef stricmp 
#define stricmp(x,y) strcasecmp(x,y)
#endif
#endif
#ifdef _MSC_VER
#define NSEEL_CGEN_CALL __cdecl
#else
#define NSEEL_CGEN_CALL 
#endif
#ifdef __cplusplus
extern "C" {
#endif
// host should implement these (can be empty stub functions if no VM will execute code in multiple threads at once)
  // implement if you will be running the code in same VM from multiple threads, 
  // or VMs that have the same GRAM pointer from different threads, or multiple
  // VMs that have a NULL GRAM pointer from multiple threads.
  // if you give each VM it's own unique GRAM and only run each VM in one thread, then you can leave it blank.
  // or if you're daring....
void NSEEL_HOSTSTUB_EnterMutex();
void NSEEL_HOSTSTUB_LeaveMutex();
typedef void *NSEEL_VMCTX;
typedef void *NSEEL_CODEHANDLE;
void NSEEL_start(); // Init global variables
void NSEEL_quit(); // Delete global variables
int32_t *NSEEL_getstats(); // returns a pointer to 5 ints... source bytes, static code bytes, call code bytes, data bytes, number of code handles
void NSEEL_VM_freevars(NSEEL_VMCTX _ctx);
void NSEEL_init_string(NSEEL_VMCTX ctx);
NSEEL_VMCTX NSEEL_VM_alloc(); // return a handle
void NSEEL_VM_free(NSEEL_VMCTX ctx); // free when done with a VM and ALL of its code have been freed, as well
// validateFunc can return error message if not permitted
void NSEEL_VM_SetFunctionValidator(NSEEL_VMCTX, const char * (*validateFunc)(const char *fn_name, void *user), void *user);
void NSEEL_VM_remove_unused_vars(NSEEL_VMCTX _ctx);
void NSEEL_VM_clear_var_refcnts(NSEEL_VMCTX _ctx);
void NSEEL_VM_remove_all_nonreg_vars(NSEEL_VMCTX _ctx);
void NSEEL_VM_enumallvars(NSEEL_VMCTX ctx, int32_t (*func)(const char *name, float *val, void *ctx), void *userctx); // return false from func to stop
float *NSEEL_VM_regvar(NSEEL_VMCTX ctx, const char *name); // register a variable (before compilation)
float *NSEEL_VM_getvar(NSEEL_VMCTX ctx, const char *name); // get a variable (if registered or created by code)
int32_t  NSEEL_VM_get_var_refcnt(NSEEL_VMCTX _ctx, const char *name); // returns -1 if not registered, or >=0
typedef struct eelstrSegRec
{
  struct eelstrSegRec *_next;
  const char *str_start; // escaped characters, including opening/trailing characters
  int32_t str_len; 
} eelStringSegmentRec;
void NSEEL_VM_SetStringFunc(NSEEL_VMCTX ctx, float (*onString)(void *caller_this, eelStringSegmentRec *list));
NSEEL_CODEHANDLE NSEEL_code_compile(NSEEL_VMCTX ctx, const char *code, int32_t lineoffs);
#define NSEEL_CODE_COMPILE_FLAG_COMMONFUNCS 1 // allows that code's functions to be used in other code (note you shouldn't destroy that codehandle without destroying others first if used)
#define NSEEL_CODE_COMPILE_FLAG_COMMONFUNCS_RESET 2 // resets common code functions
NSEEL_CODEHANDLE NSEEL_code_compile_ex(NSEEL_VMCTX ctx, const char *code, int32_t lineoffs, int32_t flags);
char *NSEEL_code_getcodeerror(NSEEL_VMCTX ctx);
int32_t NSEEL_code_geterror_flag(NSEEL_VMCTX ctx);
void NSEEL_code_execute(NSEEL_CODEHANDLE code);
void NSEEL_code_free(NSEEL_CODEHANDLE code);
int32_t *NSEEL_code_getstats(NSEEL_CODEHANDLE code); // 4 ints...source bytes, static code bytes, call code bytes, data bytes
// configuration:
#define NSEEL_MAX_VARIABLE_NAMELEN 128  // define this to override the max variable length
#define NSEEL_MAX_EELFUNC_PARAMETERS 40
#define NSEEL_MAX_FUNCSIG_NAME 2048 // longer than variable maxlen, due to multiple namespaces
#include <limits.h>
#define NSEEL_NATIVE_FLT_MAX_INT (1 << FLT_MANT_DIG) // Next value will no longer be accurately represented
#define NSEEL_MAX_FUNCTION_SIZE_FOR_INLINE 2048
#define NSEEL_RAM_ITEMSPERBLOCK_LOG2 24 // How many float precision floating point items per block
#define NSEEL_RAM_ITEMSPERBLOCK NSEEL_NATIVE_FLT_MAX_INT // Must be smaller or equal than max floating pointing representable integer
#define EEL_BC_TYPE int32_t
#ifdef CUSTOM_CMD
extern void writeCircularStringBuf(char *cmdCur);
#define EEL_STRING_STDOUT_WRITE(x,len) { writeCircularStringBuf(x); }
#else
#ifndef __ANDROID__
#define EEL_STRING_STDOUT_WRITE(x,len) { fwrite(x,len,1,stdout); fflush(stdout); }
#else
#include <android/log.h>
#define EEL_STRING_STDOUT_WRITE(x,len) { __android_log_print(ANDROID_LOG_INFO, "LiveProg", "%s", x); }
#endif
#endif
#ifdef __cplusplus
}
#endif
#endif//__NS_EEL_H__
