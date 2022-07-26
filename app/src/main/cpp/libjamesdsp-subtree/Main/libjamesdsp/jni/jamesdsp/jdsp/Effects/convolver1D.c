#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <math.h>
#include <float.h>
#include "../jdsp_header.h"
void Convolver1DEnable(JamesDSPLib *jdsp)
{
	if (jdsp->conv.process)
		jdsp->convolverEnabled = 1;
}
void Convolver1DDisable(JamesDSPLib *jdsp)
{
	jdsp->convolverEnabled = 0;
}
void Convolver1DConstructor(JamesDSPLib *jdsp)
{
	memset(&jdsp->conv, 0, sizeof(jdsp->conv));
}
void Convolver1DDestructor(JamesDSPLib *jdsp, int reqUnlock)
{
	jdsp_lock(jdsp);
	if (jdsp->conv.conv1d2x2_S_S)
	{
		FFTConvolver2x2Free(jdsp->conv.conv1d2x2_S_S);
		free(jdsp->conv.conv1d2x2_S_S);
	}
	if (jdsp->conv.conv1d2x2_T_S)
	{
		TwoStageFFTConvolver2x2Free(jdsp->conv.conv1d2x2_T_S);
		free(jdsp->conv.conv1d2x2_T_S);
	}
	if (jdsp->conv.conv1d2x4x2_S_S)
	{
		FFTConvolver2x4x2Free(jdsp->conv.conv1d2x4x2_S_S);
		free(jdsp->conv.conv1d2x4x2_S_S);
	}
	if (jdsp->conv.conv1d2x4x2_T_S)
	{
		TwoStageFFTConvolver2x4x2Free(jdsp->conv.conv1d2x4x2_T_S);
		free(jdsp->conv.conv1d2x4x2_T_S);
	}
	if (reqUnlock)
		jdsp_unlock(jdsp);
}
void Convolver1DProcessFFTConvolver2x2(JamesDSPLib *jdsp, size_t n)
{
	FFTConvolver2x2Process(jdsp->conv.conv1d2x2_S_S, jdsp->tmpBuffer[0], jdsp->tmpBuffer[1], jdsp->tmpBuffer[0], jdsp->tmpBuffer[1], (unsigned int)n);
}
void Convolver1DProcessTwoStageFFTConvolver2x2(JamesDSPLib *jdsp, size_t n)
{
	TwoStageFFTConvolver2x2Process(jdsp->conv.conv1d2x2_T_S, jdsp->tmpBuffer[0], jdsp->tmpBuffer[1], jdsp->tmpBuffer[0], jdsp->tmpBuffer[1], (unsigned int)n);
}
void Convolver1DProcessFFTConvolver2x4x2(JamesDSPLib *jdsp, size_t n)
{
	FFTConvolver2x4x2Process(jdsp->conv.conv1d2x4x2_S_S, jdsp->tmpBuffer[0], jdsp->tmpBuffer[1], jdsp->tmpBuffer[0], jdsp->tmpBuffer[1], (unsigned int)n);
}
void Convolver1DProcessTwoStageFFTConvolver2x4x2(JamesDSPLib *jdsp, size_t n)
{
	TwoStageFFTConvolver2x4x2Process(jdsp->conv.conv1d2x4x2_T_S, jdsp->tmpBuffer[0], jdsp->tmpBuffer[1], jdsp->tmpBuffer[0], jdsp->tmpBuffer[1], (unsigned int)n);
}
int Convolver1DLoadImpulseResponse(JamesDSPLib *jdsp, float *tempImpulseFloat, unsigned int impChannels, unsigned int impulseLengthActual)
{
	jdsp->convolverEnabled = 0;
	Convolver1DDestructor(jdsp, 0);
	Convolver1DConstructor(jdsp);
	float **finalImpulse = (float**)malloc(impChannels * sizeof(float*));
	if (!finalImpulse)
	{
		jdsp_unlock(jdsp);
		return -1;
	}
	for (unsigned int i = 0; i < impChannels; i++)
	{
		float* channelbuf = (float*)malloc(impulseLengthActual * sizeof(float));
		if (!channelbuf)
		{
			free(finalImpulse);
			finalImpulse = 0;
			jdsp_unlock(jdsp);
			return -1;
		}
		float* p = tempImpulseFloat + i;
		for (unsigned int j = 0; j < impulseLengthActual; j++)
			channelbuf[j] = p[j * impChannels];
		finalImpulse[i] = channelbuf;
	}
	if (impulseLengthActual < 18000)
	{
		if (impChannels == 1)
		{
			jdsp->conv.conv1d2x2_S_S = (FFTConvolver2x2*)malloc(sizeof(FFTConvolver2x2));
			if (!jdsp->conv.conv1d2x2_S_S)
			{
				jdsp_unlock(jdsp);
				return -1;
			}
			FFTConvolver2x2Init(jdsp->conv.conv1d2x2_S_S);
			FFTConvolver2x2LoadImpulseResponse(jdsp->conv.conv1d2x2_S_S, (unsigned int)jdsp->blockSize, finalImpulse[0], finalImpulse[0], impulseLengthActual);
			jdsp->conv.process = Convolver1DProcessFFTConvolver2x2;
		}
		if (impChannels == 2)
		{
			jdsp->conv.conv1d2x2_S_S = (FFTConvolver2x2*)malloc(sizeof(FFTConvolver2x2));
			if (!jdsp->conv.conv1d2x2_S_S)
			{
				jdsp_unlock(jdsp);
				return -1;
			}
			FFTConvolver2x2Init(jdsp->conv.conv1d2x2_S_S);
			FFTConvolver2x2LoadImpulseResponse(jdsp->conv.conv1d2x2_S_S, (unsigned int)jdsp->blockSize, finalImpulse[0], finalImpulse[1], impulseLengthActual);
			jdsp->conv.process = Convolver1DProcessFFTConvolver2x2;
		}
		if (impChannels == 4)
		{
			jdsp->conv.conv1d2x4x2_S_S = (FFTConvolver2x4x2*)malloc(sizeof(FFTConvolver2x4x2));
			if (!jdsp->conv.conv1d2x4x2_S_S)
			{
				jdsp_unlock(jdsp);
				return -1;
			}
			FFTConvolver2x4x2Init(jdsp->conv.conv1d2x4x2_S_S);
			FFTConvolver2x4x2LoadImpulseResponse(jdsp->conv.conv1d2x4x2_S_S, (unsigned int)jdsp->blockSize, finalImpulse[0], finalImpulse[1], finalImpulse[2], finalImpulse[3], impulseLengthActual);
			jdsp->conv.process = Convolver1DProcessFFTConvolver2x4x2;
		}
	}
	else
	{
		unsigned int seg1Len, seg2Len;
		if (impulseLengthActual > (unsigned int)jdsp->blockSize)
		{
			// Fine tune performance
			seg1Len = (unsigned int)jdsp->blockSize;
			seg2Len = (impulseLengthActual - seg1Len) / 4;
			if (seg2Len > 32768)
				seg2Len = 32768;
		}
		else
		{
			seg1Len = impulseLengthActual * 3 / 4;
			seg2Len = (unsigned int)jdsp->blockSize - seg1Len;
		}
		if (seg1Len < 1)
			seg1Len = 1;
		if (seg2Len < 1)
			seg2Len = 1;
		if (impChannels == 1)
		{
			jdsp->conv.conv1d2x2_T_S = (TwoStageFFTConvolver2x2*)malloc(sizeof(TwoStageFFTConvolver2x2));
			if (!jdsp->conv.conv1d2x2_T_S)
			{
				jdsp_unlock(jdsp);
				return -1;
			}
			TwoStageFFTConvolver2x2Init(jdsp->conv.conv1d2x2_T_S);
			TwoStageFFTConvolver2x2LoadImpulseResponse(jdsp->conv.conv1d2x2_T_S, seg1Len, seg2Len, finalImpulse[0], finalImpulse[0], impulseLengthActual);
			jdsp->conv.process = Convolver1DProcessTwoStageFFTConvolver2x2;
		}
		if (impChannels == 2)
		{
			jdsp->conv.conv1d2x2_T_S = (TwoStageFFTConvolver2x2*)malloc(sizeof(TwoStageFFTConvolver2x2));
			if (!jdsp->conv.conv1d2x2_T_S)
			{
				jdsp_unlock(jdsp);
				return -1;
			}
			TwoStageFFTConvolver2x2Init(jdsp->conv.conv1d2x2_T_S);
			TwoStageFFTConvolver2x2LoadImpulseResponse(jdsp->conv.conv1d2x2_T_S, seg1Len, seg2Len, finalImpulse[0], finalImpulse[1], impulseLengthActual);
			jdsp->conv.process = Convolver1DProcessTwoStageFFTConvolver2x2;
		}
		if (impChannels == 4)
		{
			jdsp->conv.conv1d2x4x2_T_S = (TwoStageFFTConvolver2x4x2*)malloc(sizeof(TwoStageFFTConvolver2x4x2));
			if (!jdsp->conv.conv1d2x4x2_T_S)
			{
				jdsp_unlock(jdsp);
				return -1;
			}
			TwoStageFFTConvolver2x4x2Init(jdsp->conv.conv1d2x4x2_T_S);
			TwoStageFFTConvolver2x4x2LoadImpulseResponse(jdsp->conv.conv1d2x4x2_T_S, seg1Len, seg2Len, finalImpulse[0], finalImpulse[1], finalImpulse[2], finalImpulse[3], impulseLengthActual);
			jdsp->conv.process = Convolver1DProcessTwoStageFFTConvolver2x4x2;
		}
	}
	for (unsigned int i = 0; i < impChannels; i++)
		free(finalImpulse[i]);
	free(finalImpulse);
	jdsp_unlock(jdsp);
	return 1;
}
