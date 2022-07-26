#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <stdarg.h>
#include "s_str.h"
#include "eelCommon.h"
#ifdef _WIN32
#include <vld.h>
#endif
void NSEEL_HOSTSTUB_EnterMutex() { }
void NSEEL_HOSTSTUB_LeaveMutex() { }
int32_t runcode(NSEEL_VMCTX m_vm, const char *codeptr, int32_t showerr, const char *showerrfn, int32_t canfree, int32_t ignoreEndOfInputChk, int32_t doExec)
{
	NSEEL_CODEHANDLE code = NSEEL_code_compile_ex(m_vm, codeptr, 0, canfree ? 0 : NSEEL_CODE_COMPILE_FLAG_COMMONFUNCS);
	char *err;
	if (!code && (err = NSEEL_code_getcodeerror(m_vm)))
	{
		if (!ignoreEndOfInputChk && (NSEEL_code_geterror_flag(m_vm) & 1)) return 1;
		if (showerr)
		{
			if (showerr == 2)
				printf("Warning: %s:%s\n", showerrfn, err);
			else
				printf("%s:%s\n", showerrfn, err);
		}
		return -1;
	}
	else
	{
		if (code)
		{
			if (doExec)
				NSEEL_code_execute(code);
			if (canfree)
				NSEEL_code_free(code);
		}
		return 0;
	}
	return -1;
}
NSEEL_CODEHANDLE loadfileAndCompile(NSEEL_VMCTX m_vm, const char *fn)
{
	FILE *fp = fopen(fn, "r");
	if (!fp)
	{
		printf("Error opening script file %s", fn);
		return 0;
	}
	s_str code = s_str_create();
	s_str_clear(&code);
	char line[4096];
	for (;;)
	{
		line[0] = 0;
		fgets(line, sizeof(line), fp);
		if (!line[0]) break;
		s_str_append_c_str(&code, line);
	}
	fclose(fp);
	const char *codetext = s_str_c_str(&code);
	NSEEL_CODEHANDLE compiledCode = NSEEL_code_compile_ex(m_vm, codetext, 0, 1);
	char *err;
	if (!compiledCode && (err = NSEEL_code_getcodeerror(m_vm)))
	{
		printf("%s\n", err);
		return 0;
	}
	s_str_destroy(&code);
	return compiledCode;
}
int32_t loadfile(NSEEL_VMCTX m_vm, const char *fn, const char *callerfn, int32_t allowstdin)
{
	FILE *fp = fopen(fn, "r");
	if (!fp)
	{
		if (callerfn)
			printf("Warning: @import could not open '%s'", fn);
		else
			printf("Error opening script file %s", fn);
		return -1;
	}
	s_str code = s_str_create();
	s_str_clear(&code);
	char line[4096];
	for (;;)
	{
		line[0] = 0;
		fgets(line, sizeof(line), fp);
		if (!line[0]) break;
		if (!strnicmp(line, "@import", 7) && isspace(line[7]))
		{
			char *p = line + 7;
			while (isspace(*p)) p++;
			char *ep = p;
			while (*ep) ep++;
			while (ep > p && isspace(ep[-1])) ep--;
			*ep = 0;
			if (*p)
				loadfile(m_vm, p, fn, 0);
		}
		else
			s_str_append_c_str(&code, line);
	}
	fclose(fp);
	const char *codetext = s_str_c_str(&code);
	int32_t ret = runcode(m_vm, codetext, callerfn ? 2 : 1, fn, 1, 1, !callerfn);
	s_str_destroy(&code);
	return ret;
}
void* thread1Spawns(void *args)
{
	NSEEL_code_execute(args);
	return 0;
}
extern void printFunctions();
int32_t main(int32_t argc, char **argv)
{
	int32_t argpos = 1;
	char *scriptfn = argv[0];
	int32_t g_interactive = 0;
	while (argpos < argc && argv[argpos][0] == '-' && argv[argpos][1])
	{
		if (!strcmp(argv[argpos], "-i")) g_interactive++;
		else
		{
			fprintf(stderr, "Usage: %s [scriptfile | -]\n", argv[0]);
			return -1;
		}
		argpos++;
	}
	if (argpos < argc && !g_interactive)
		scriptfn = argv[argpos++];
	else
		g_interactive = 1;
	// EEL global data
	NSEEL_start();
	s_str code, t;
	code = s_str_create();
	t = s_str_create();
	NSEEL_VMCTX m_vm = NSEEL_VM_alloc();
	if (g_interactive)
	{
		printf("EEL interactive mode, type @quit() to quit, @abort() to abort multiline entry, @help() for printing built-in function\n");
		float *resultVar = NSEEL_VM_regvar(m_vm, "__result");
		s_str_clear(&code);
		s_str_clear(&t);
		char line[4096];
		for (;;)
		{
			const char *fi = s_str_c_str(&code);
			if (!fi[0])
				printf("EEL> ");
			else
				printf("> ");
			fflush(stdout);
			line[0] = 0;
			fgets(line, sizeof(line), stdin);
			if (!line[0])
				break;
			while (line[0] && (line[strlen(line) - 1] == '\r' || line[strlen(line) - 1] == '\n' || line[strlen(line) - 1] == '\t' || line[strlen(line) - 1] == ' '))
				line[strlen(line) - 1] = 0;
			s_str_append_c_str(&code, line);
			if (!strcmp(line, "@quit()"))
				break;
			if (!strcmp(line, "@abort()"))
				s_str_assign_c_str(&code, "");
			if (!strcmp(line, "@help()"))
			{
				printFunctions();
				s_str_assign_c_str(&code, "");
			}
			s_str_assign_c_str(&t, "__result = (");
			const char *codetext = s_str_c_str(&code);
			s_str_append_c_str(&t, codetext);
			s_str_append_c_str(&t, ");");
			const char *tget = s_str_c_str(&t);
			int32_t res = runcode(m_vm, tget, 0, "", 1, 1, 1); // allow free, since functions can't be defined locally
			if (!res)
			{
				if (resultVar) printf("=%.8g\n", *resultVar);
				s_str_assign_c_str(&code, "");
			}
			else // try compiling again allowing function definitions (and not allowing free), but show errors if not continuation 
			{
				res = runcode(m_vm, codetext, 1, "(stdin)", 0, 0, 1);
				if (res <= 0)
					s_str_assign_c_str(&code, "");
				// res>0 means need more lines
			}
		}
	}
	else
		loadfile(m_vm, scriptfn, NULL, 1);
	s_str_destroy(&code);
	s_str_destroy(&t);
	NSEEL_VM_free(m_vm);
	NSEEL_quit();
	return 0;
}