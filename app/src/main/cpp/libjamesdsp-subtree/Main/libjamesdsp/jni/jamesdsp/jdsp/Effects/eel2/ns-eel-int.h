/*
  Nullsoft Expression Evaluator Library (NS-EEL)
  Copyright (C) 1999-2003 Nullsoft, Inc.
  ns-eel-int.h: internal code definition header.
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
#ifndef __NS_EELINT_H__
#define __NS_EELINT_H__
#include "eelCommon.h"
#ifdef __cplusplus
extern "C" {
#endif
enum { 
  // these ignore fn in opcodes, just use fntype to determine function
  FN_MULTIPLY=0,
  FN_DIVIDE,
  FN_JOIN_STATEMENTS,
  FN_DENORMAL_LIKELY,
  FN_DENORMAL_UNLIKELY,
  FN_ADD,
  FN_SUB,
  FN_AND,
  FN_OR,
  FN_UMINUS,
  FN_NOT,
  FN_NOTNOT,
  FN_XOR,
  FN_SHL,
  FN_SHR,
  FN_MOD,
  FN_POW,
  FN_LT,
  FN_GT,
  FN_LTE,
  FN_GTE,
  FN_EQ,
  FN_EQ_EXACT,
  FN_NE,
  FN_NE_EXACT,
  FN_LOGICAL_AND,
  FN_LOGICAL_OR,
  FN_IF_ELSE,
  FN_MEMORY,
  FN_NONCONST_BEGIN,
  FN_ASSIGN=FN_NONCONST_BEGIN,
  FN_ADD_OP,
  FN_SUB_OP,
  FN_MOD_OP,
  FN_OR_OP,
  FN_AND_OP,
  FN_XOR_OP,
  FN_DIV_OP,
  FN_MUL_OP,
  FN_POW_OP,
  FN_WHILE,
  FN_LOOP,
  FUNCTYPE_SIMPLEMAX,
  FUNCTYPE_FUNCTIONTYPEREC=1000, // fn is a functionType *
  FUNCTYPE_EELFUNC, // fn is a _codeHandleFunctionRec *
};
#define YYSTYPE opcodeRec *
#define NSEEL_CLOSEFACTOR 0.00001f
typedef struct opcodeRec opcodeRec;
typedef struct _codeHandleFunctionRec 
{
  struct _codeHandleFunctionRec *next; // main linked list (only used for high level functions)
  struct _codeHandleFunctionRec *derivedCopies; // separate linked list, head being the main function, other copies being derived versions
  void *startptr; // compiled code (may be cleared + recompiled when shraed)
  opcodeRec *opcodes;
  int32_t startptr_size; 
  int32_t tmpspace_req;
  int32_t num_params;
  int32_t rvMode; // RETURNVALUE_*
  int32_t fpStackUsage; // 0-8, usually
  int32_t canHaveDenormalOutput;
  // local storage's first items are the parameters, then locals. Note that the opcodes will reference localstorage[] via VARPTRPTR, but 
  // the values localstorage[x] points are reallocated from context-to-context, if it is a common function.
  // separately allocated list of pointers, the contents of the list should be zeroed on context changes if a common function
  // note that when making variations on a function (context), it is shared, but since it is zeroed on context changes, it is context-local
  int32_t localstorage_size;
  float **localstorage;
  int32_t isCommonFunction;
  int32_t usesNamespaces;
  uint32_t parameterAsNamespaceMask;
  char fname[NSEEL_MAX_FUNCSIG_NAME+1];
} _codeHandleFunctionRec;  
#define LLB_DSIZE (65536-64)
typedef struct _llBlock {
  struct _llBlock *next;
  int32_t sizeused;
  char block[LLB_DSIZE];
} llBlock;
typedef struct {
  llBlock *blocks, 
          *blocks_data;
  void *workTable; // references a chunk in blocks_data
  void *code;
  int32_t code_size; // in case the caller wants to write it out
  int32_t code_stats[4];
  void *ramPtr;
  int32_t workTable_size; // size (minus padding/extra space) of workTable -- only used if EEL_VALIDATE_WORKTABLE_USE set, but might be handy to have around too
} codeHandleType;
#include "s_str.h"
typedef struct
{
	int32_t slot, inuse;
	int32_t *map;
	s_str  *m_literal_strings;
} eel_string_context_state;
#define MAX_CMD_LEN 8192
#define HISTORY_COUNT 15000
#include "cpthread.h"
enum thread_state
{
	EEL_SETUP,
	EEL_IDLE,
	EEL_WORKING,
	EEL_GET_OFF_FROM_WORK
};
typedef struct
{
	enum thread_state state;
	pthread_cond_t work_cond;
	pthread_mutex_t work_mtx;
	pthread_cond_t boss_cond;
	pthread_mutex_t boss_mtx;
	NSEEL_CODEHANDLE codePtr;
	pthread_t threadID;
} abstractThreads;
typedef struct
{
	uint32_t numberOfThreads;
	uint32_t threadMap[1024];
	abstractThreads *codePtrThreadSink[1024];
	pthread_mutex_t globalLocker;
	uint32_t numberOfConvolver;
	uint32_t *convolverMap;
	uint32_t *convolverType;
	void **convolverSink;
  const char *(*func_check)(const char *fn_name, void *user); // return error message if not permitted
  void *func_check_user;
  float **varTable_Values;
  char   ***varTable_Names;
  int32_t varTable_numBlocks;
  int32_t errVar,gotEndOfInput;
  opcodeRec *result;
  char last_error_string[256];
  void *scanner;
  const char *rdbuf_start, *rdbuf, *rdbuf_end;
  llBlock *tmpblocks_head, // used while compiling, and freed after compiling
          *blocks_head,  // used while compiling, transferred to code context (these are pages marked as executable)
          *blocks_head_data, // used while compiling, transferred to code context
          *pblocks; // persistent blocks, stores data used by varTable_Names, varTable_Values, etc.
  int32_t l_stats[4]; // source bytes, static code bytes, call code bytes, data bytes
  _codeHandleFunctionRec *functions_local, *functions_common;
  // state used while generating functions
  struct opcodeRec *directValueCache; // linked list using fn as next
  int32_t isSharedFunctions;
  int32_t isGeneratingCommonFunction;
  int32_t function_usesNamespaces;
  int32_t function_globalFlag; // set if restrict globals to function_localTable_Names[2]
  // [0] is parameter+local symbols (combined space)
  // [1] is symbols which get implied "this." if used
  // [2] is globals permitted
  int32_t function_localTable_Size[3]; // for parameters only
  char **function_localTable_Names[3]; // lists of pointers
  float **function_localTable_ValuePtrs;
  const char *function_curName; // name of current function
  float (*onString)(void *caller_this, eelStringSegmentRec *list);
  codeHandleType *tmpCodeHandle;
  float ram_state[NSEEL_RAM_ITEMSPERBLOCK];
  void *caller_this;
  eel_string_context_state *m_string_context;
  char printfbuf[20000];
} compileContext;
void *NSEEL_PProc_RAM(void *data, int32_t data_size, compileContext *ctx);
void *NSEEL_PProc_THIS(void *data, int32_t data_size, compileContext *ctx);
extern EEL_BC_TYPE _asm_generic3parm[]; // 3 float * parms, returning float *
extern EEL_BC_TYPE _asm_generic3parm_retd[]; // 3 float * parms, returning float
extern EEL_BC_TYPE _asm_generic2parm[]; // 2 float * parms, returning float *
extern EEL_BC_TYPE _asm_generic2parm_retd[]; // 2 float * parms, returning float
extern EEL_BC_TYPE _asm_generic1parm[]; // 1 float * parms, returning float *
extern EEL_BC_TYPE _asm_generic1parm_retd[]; // 1 float * parms, returning float 
extern const void *const _asm_generic1parm_retd_end;
extern const void *const _asm_generic1parm_end;
extern const void *const _asm_generic2parm_retd_end;
extern const void *const _asm_generic2parm_end;
extern const void *const _asm_generic3parm_retd_end;
extern const void *const _asm_generic3parm_end;
#define NSEEL_VARS_PER_BLOCK 64
#define NSEEL_NPARAMS_FLAG_CONST 0x80000
typedef void *(*NSEEL_PPPROC)(void *data, int32_t data_size, compileContext *userfunc_data);
typedef struct
{
      const char *name;
      void *afunc;
      void *func_e;
      int32_t nParams;
      void *replptrs[4];
      NSEEL_PPPROC pProc;
} functionType;
typedef struct
{
  int32_t refcnt;
  char isreg;
} varNameHdr;
opcodeRec *nseel_createCompiledValue(compileContext *ctx, float value);
opcodeRec *nseel_createCompiledValuePtr(compileContext *ctx, float *addrValue, const char *namestr);
opcodeRec *nseel_createMoreParametersOpcode(compileContext *ctx, opcodeRec *code1, opcodeRec *code2);
opcodeRec *nseel_createSimpleCompiledFunction(compileContext *ctx, int32_t fn, int32_t np, opcodeRec *code1, opcodeRec *code2);
opcodeRec *nseel_createMemoryAccess(compileContext *ctx, opcodeRec *code1, opcodeRec *code2);
opcodeRec *nseel_createIfElse(compileContext *ctx, opcodeRec *code1, opcodeRec *code2, opcodeRec *code3);
opcodeRec *nseel_createFunctionByName(compileContext *ctx, const char *name, int32_t np, opcodeRec *code1, opcodeRec *code2, opcodeRec *code3);
// converts a generic identifier (VARPTR) opcode into either an actual variable reference (parmcnt = -1),
// or if parmcnt >= 0, to a function call (see nseel_setCompiledFunctionCallParameters())
opcodeRec *nseel_resolve_named_symbol(compileContext *ctx, opcodeRec *rec, int32_t parmcnt, int32_t *errOut); 
// sets parameters and calculates parameter count for opcode, and calls nseel_resolve_named_symbol() with the right
// parameter count
opcodeRec *nseel_setCompiledFunctionCallParameters(compileContext *ctx, opcodeRec *fn, opcodeRec *code1, opcodeRec *code2, opcodeRec *code3, opcodeRec *postCode, int32_t *errOut); 
// errOut will be set if return NULL:
// -1 if postCode set when not wanted (i.e. not while())
// 0 if func not found, 
// 1 if function requires 2+ parameters but was given more
// 2 if function needs more parameters
// 4 if function requires 1 parameter but was given more
eelStringSegmentRec *nseel_createStringSegmentRec(compileContext *ctx, const char *str, int32_t len);
opcodeRec *nseel_eelMakeOpcodeFromStringSegments(compileContext *ctx, eelStringSegmentRec *rec);
float *nseel_int_register_var(compileContext *ctx, const char *name, int32_t isReg, const char **namePtrOut);
_codeHandleFunctionRec *eel_createFunctionNamespacedInstance(compileContext *ctx, _codeHandleFunctionRec *fr, const char *nameptr);
// nseel_simple_tokenizer will return comments as tokens if state is non-NULL
const char *nseel_simple_tokenizer(const char **ptr, const char *endptr, int32_t *lenOut, int32_t *state);
int32_t nseel_filter_escaped_string(char *outbuf, int32_t outbuf_sz, const char *rdptr, size_t rdptr_size, char delim_char); // returns length used, minus NUL char
opcodeRec *nseel_translate(compileContext *ctx, const char *tmp, size_t tmplen); // tmplen=0 for nul-term
#define __NSEEL_RAMAlloc(pblocks, w) (pblocks + w)
float * NSEEL_CGEN_CALL __NSEEL_RAM_MemSet(float *blocks,float *dest, float *v, float *lenptr);
float * NSEEL_CGEN_CALL __NSEEL_RAM_MemCpy(float *blocks,float *dest, float *src, float *lenptr);
extern float NSEEL_CGEN_CALL nseel_int_rand(float amplitude);
#ifdef __cplusplus
}
#endif
#endif//__NS_EELINT_H__
