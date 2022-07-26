#include "../jdsp_header.h"

void LiveProgConstructor(JamesDSPLib *jdsp)
{
	LiveProg *pg = &jdsp->eel;
    pg->active = 1;
	pg->compileSucessfully = 0;
	pg->codehandleInit = 0;
	pg->codehandleProcess = 0;
	pg->vm = NSEEL_VM_alloc(); // create virtual machine
	pg->vmFs = NSEEL_VM_regvar(pg->vm, "srate");
	*pg->vmFs = jdsp->fs;
	pg->input1 = NSEEL_VM_regvar(pg->vm, "spl0");
	pg->input2 = NSEEL_VM_regvar(pg->vm, "spl1");
}
void LiveProgDestructor(JamesDSPLib *jdsp)
{
	jdsp_lock(jdsp);
	if (jdsp->eel.vm)
	{
		NSEEL_code_free(jdsp->eel.codehandleInit);
		NSEEL_code_free(jdsp->eel.codehandleProcess);
		NSEEL_VM_free(jdsp->eel.vm);
	}
	jdsp_unlock(jdsp);
}
void LiveProgEnable(JamesDSPLib *jdsp)
{
	*jdsp->eel.vmFs = jdsp->fs;
	jdsp->liveprogEnabled = 1;
}
void LiveProgDisable(JamesDSPLib *jdsp)
{
	jdsp->liveprogEnabled = 0;
}
int LiveProgLoadCode(JamesDSPLib *jdsp, char *codeTextInit, char *codeTextProcess)
{
	LiveProg *pg = &jdsp->eel;
	pg->compileSucessfully = 0;
	compileContext *ctx = (compileContext*)pg->vm;
	NSEEL_VM_freevars(pg->vm);
	NSEEL_init_string(pg->vm);
	pg->vmFs = NSEEL_VM_regvar(pg->vm, "srate");
	*pg->vmFs = jdsp->fs;
	pg->input1 = NSEEL_VM_regvar(pg->vm, "spl0");
	pg->input2 = NSEEL_VM_regvar(pg->vm, "spl1");
	if (pg->codehandleInit)
	{
		NSEEL_code_free(pg->codehandleInit);
		pg->codehandleInit = 0;
	}
	if (pg->codehandleProcess)
	{
		NSEEL_code_free(pg->codehandleProcess);
		pg->codehandleProcess = 0;
	}
	ctx->functions_common = 0;
	pg->codehandleInit = NSEEL_code_compile_ex(pg->vm, codeTextInit, 0, 1);
	if (!pg->codehandleInit)
	{
		pg->compileSucessfully = 0;
		return -1;
	}
	NSEEL_code_execute(pg->codehandleInit);
    pg->codehandleProcess = NSEEL_code_compile(pg->vm, codeTextProcess, 0);
	if (pg->codehandleInit && pg->codehandleProcess)
	{
		if (pg->codehandleProcess)
			pg->compileSucessfully = 1;
		else
			pg->compileSucessfully = 0;
	}
	else
	{
		pg->compileSucessfully = 0;
		return -3;
	}
	return 1;
}
const char* checkErrorCode(int errCode)
{
	switch (errCode)
	{
	case 0:
		return "@init section not found";
	case -1:
		return "Syntax error at @init section";
	case -2:
		return "@sample section not found";
	case -3:
		return "Syntax error at @sample section";
	default:
		return "No syntax errors detected";
	}
}
int LiveProgStringParser(JamesDSPLib *jdsp, char *eelCode)
{
    jdsp_lock(jdsp);
	long long strLen = (long long)strlen(eelCode);
	const char *initSegment = strstr(eelCode, "@init");
	if (!initSegment)
	{
        return 0;
	}
	else
		initSegment += 6;
	const char *processSegment = strstr(eelCode, "@sample");
	if (!processSegment)
	{
        return -2;
	}
	else
		processSegment += 8;
	char *codeTextInit = (char*)malloc(strLen * sizeof(char));
	char *codeTextProcess = (char*)malloc(strLen * sizeof(char));
	memset(codeTextInit, 0, strLen * sizeof(char));
	memset(codeTextProcess, 0, strLen * sizeof(char));
	int errorMsg = 1;
	if (initSegment && processSegment)
	{
		if (initSegment < processSegment)
		{
			long long cpyLen = processSegment - initSegment - (8 + 1);
			if (cpyLen > 0)
				strncpy(codeTextInit, initSegment, cpyLen);
			if (processSegment - eelCode < strLen)
			{
				strcpy(codeTextProcess, processSegment);
			}
			errorMsg = LiveProgLoadCode(jdsp, codeTextInit, codeTextProcess);
		}
		else
		{
			strcpy(codeTextInit, initSegment);
			long long cpyLen = initSegment - processSegment - (6 + 1);
			if (cpyLen > 0)
				strncpy(codeTextProcess, processSegment, cpyLen);
			errorMsg = LiveProgLoadCode(jdsp, codeTextInit, codeTextProcess);
		}
	}
	free(codeTextInit);
	free(codeTextProcess);
	jdsp_unlock(jdsp);
	return errorMsg;
}
void LiveProgProcess(JamesDSPLib *jdsp, size_t n)
{
	LiveProg *eel = &jdsp->eel;
    if (eel->compileSucessfully && eel->active)
	{
		for (size_t i = 0; i < n; i++)
		{
			*eel->input1 = jdsp->tmpBuffer[0][i];
			*eel->input2 = jdsp->tmpBuffer[1][i];
			NSEEL_code_execute(eel->codehandleProcess);
			jdsp->tmpBuffer[0][i] = (float)*eel->input1;
			jdsp->tmpBuffer[1][i] = (float)*eel->input2;
		}
	}
}
