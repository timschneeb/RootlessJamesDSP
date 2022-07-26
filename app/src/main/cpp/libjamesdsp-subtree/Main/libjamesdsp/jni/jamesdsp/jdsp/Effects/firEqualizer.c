#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <float.h>
#include "../jdsp_header.h"
void FIREqualizerConstructor(JamesDSPLib *jdsp)
{
	initIerper(&jdsp->fireq.pch1, NUMPTS + 2);
	initIerper(&jdsp->fireq.pch2, NUMPTS + 2);
	jdsp->fireq.instance.filterLen = InitArbitraryEq(&jdsp->fireq.instance.coeffGen, 0);
	FFTConvolver2x2Init(&jdsp->fireq.instance.convState);
	float *kDelta = (float*)malloc(jdsp->fireq.instance.filterLen * sizeof(float));
	memset(kDelta, 0, jdsp->fireq.instance.filterLen * sizeof(float));
	kDelta[0] = 1.0f;
	FFTConvolver2x2LoadImpulseResponse(&jdsp->fireq.instance.convState, (unsigned int)jdsp->blockSize, kDelta, kDelta, jdsp->fireq.instance.filterLen);
	free(kDelta);
}
void FIREqualizerDestructor(JamesDSPLib *jdsp)
{
	jdsp_lock(jdsp);
	freeIerper(&jdsp->fireq.pch1);
	freeIerper(&jdsp->fireq.pch2);
	EqNodesFree(&jdsp->fireq.instance.coeffGen);
	FFTConvolver2x2Free(&jdsp->fireq.instance.convState);
	jdsp_unlock(jdsp);
}
void FIREqualizerAxisInterpolation(JamesDSPLib *jdsp, int interpolationMode, int phaseMode, double *freqAx, double *gaindB)
{
	jdsp_lock(jdsp);
	memcpy(jdsp->fireq.freq + 1, freqAx, NUMPTS * sizeof(double));
	memcpy(jdsp->fireq.gain + 1, gaindB, NUMPTS * sizeof(double));
	jdsp->fireq.freq[0] = 0.0;
	jdsp->fireq.gain[0] = jdsp->fireq.gain[1];
	jdsp->fireq.freq[NUMPTS + 1] = 24000.0;
	jdsp->fireq.gain[NUMPTS + 1] = jdsp->fireq.gain[NUMPTS];
	float *eqFil;
	if (!phaseMode)
	{
		if (!interpolationMode)
		{
			pchip(&jdsp->fireq.pch1, jdsp->fireq.freq, jdsp->fireq.gain, NUMPTS + 2, 1, 1);
			eqFil = InterpolatingEqMinimumPhase(&jdsp->fireq.instance.coeffGen, (float)jdsp->fs, (void*)(&jdsp->fireq.pch1));
		}
		else
		{
			makima(&jdsp->fireq.pch2, jdsp->fireq.freq, jdsp->fireq.gain, NUMPTS + 2, 1, 1);
			eqFil = InterpolatingEqMinimumPhase(&jdsp->fireq.instance.coeffGen, (float)jdsp->fs, (void*)(&jdsp->fireq.pch2));
		}
	}
	else
	{
		if (!interpolationMode)
		{
			pchip(&jdsp->fireq.pch1, jdsp->fireq.freq, jdsp->fireq.gain, NUMPTS + 2, 1, 1);
			eqFil = InterpolatingEqLinearPhase(&jdsp->fireq.instance.coeffGen, (float)jdsp->fs, (void*)(&jdsp->fireq.pch1));
		}
		else
		{
			makima(&jdsp->fireq.pch2, jdsp->fireq.freq, jdsp->fireq.gain, NUMPTS + 2, 1, 1);
			eqFil = InterpolatingEqLinearPhase(&jdsp->fireq.instance.coeffGen, (float)jdsp->fs, (void*)(&jdsp->fireq.pch2));
		}
	}
	int filterLen;
	if (!phaseMode)
		filterLen = FILTERLEN;
	else
		filterLen = MUL2FILTERLEN - 1;
	if (jdsp->equalizerForceRefresh || jdsp->fireq.currentPhaseMode != phaseMode)
	{
		FFTConvolver2x2Free(&jdsp->fireq.instance.convState);
		FFTConvolver2x2LoadImpulseResponse(&jdsp->fireq.instance.convState, (unsigned int)jdsp->blockSize, eqFil, eqFil, filterLen);
		jdsp->equalizerForceRefresh = 0;
	}
	else
		FFTConvolver2x2RefreshImpulseResponse(&jdsp->fireq.instance.convState, (unsigned int)jdsp->blockSize, eqFil, eqFil, filterLen);
	jdsp->fireq.currentPhaseMode = phaseMode;
	jdsp->fireq.currentInterpolationMode = interpolationMode;
	jdsp_unlock(jdsp);
}
void FIREqualizerEnable(JamesDSPLib *jdsp)
{
	jdsp_lock(jdsp);
	if (jdsp->equalizerForceRefresh)
	{
		float *eqFil;
		if (!jdsp->fireq.currentPhaseMode)
		{
			if (!jdsp->fireq.currentInterpolationMode)
			{
				pchip(&jdsp->fireq.pch1, jdsp->fireq.freq, jdsp->fireq.gain, NUMPTS + 2, 1, 1);
				eqFil = InterpolatingEqMinimumPhase(&jdsp->fireq.instance.coeffGen, (float)jdsp->fs, (void*)(&jdsp->fireq.pch1));
			}
			else
			{
				makima(&jdsp->fireq.pch2, jdsp->fireq.freq, jdsp->fireq.gain, NUMPTS + 2, 1, 1);
				eqFil = InterpolatingEqMinimumPhase(&jdsp->fireq.instance.coeffGen, (float)jdsp->fs, (void*)(&jdsp->fireq.pch2));
			}
		}
		else
		{
			if (!jdsp->fireq.currentInterpolationMode)
			{
				pchip(&jdsp->fireq.pch1, jdsp->fireq.freq, jdsp->fireq.gain, NUMPTS + 2, 1, 1);
				eqFil = InterpolatingEqLinearPhase(&jdsp->fireq.instance.coeffGen, (float)jdsp->fs, (void*)(&jdsp->fireq.pch1));
			}
			else
			{
				makima(&jdsp->fireq.pch2, jdsp->fireq.freq, jdsp->fireq.gain, NUMPTS + 2, 1, 1);
				eqFil = InterpolatingEqLinearPhase(&jdsp->fireq.instance.coeffGen, (float)jdsp->fs, (void*)(&jdsp->fireq.pch2));
			}
		}
		int filterLen;
		if (!jdsp->fireq.currentPhaseMode)
			filterLen = FILTERLEN;
		else
			filterLen = MUL2FILTERLEN - 1;
		FFTConvolver2x2RefreshImpulseResponse(&jdsp->fireq.instance.convState, (unsigned int)jdsp->blockSize, eqFil, eqFil, filterLen);
		jdsp->equalizerForceRefresh = 0;
	}
	jdsp->equalizerEnabled = 1;
	jdsp_unlock(jdsp);
}
void FIREqualizerDisable(JamesDSPLib *jdsp)
{
	jdsp->equalizerEnabled = 0;
}
void FIREqualizerProcess(JamesDSPLib *jdsp, size_t n)
{
	FFTConvolver2x2Process(&jdsp->fireq.instance.convState, jdsp->tmpBuffer[0], jdsp->tmpBuffer[1], jdsp->tmpBuffer[0], jdsp->tmpBuffer[1], (unsigned int)n);
}