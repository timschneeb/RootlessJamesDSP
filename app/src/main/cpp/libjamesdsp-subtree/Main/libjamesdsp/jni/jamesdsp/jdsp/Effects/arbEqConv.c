#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <float.h>
#include "../jdsp_header.h"
void ArbitraryResponseEqualizerConstructor(JamesDSPLib *jdsp)
{
	jdsp->arbMag.filterLen = InitArbitraryEq(&jdsp->arbMag.coeffGen, 0);
	FFTConvolver2x2Init(&jdsp->arbMag.convState);
	float *kDelta = (float*)malloc(jdsp->arbMag.filterLen * sizeof(float));
	memset(kDelta, 0, jdsp->arbMag.filterLen * sizeof(float));
	kDelta[0] = 1.0f;
	FFTConvolver2x2LoadImpulseResponse(&jdsp->arbMag.convState, (unsigned int)jdsp->blockSize, kDelta, kDelta, jdsp->arbMag.filterLen);
	free(kDelta);
}
void ArbitraryResponseEqualizerDestructor(JamesDSPLib *jdsp)
{
	jdsp_lock(jdsp);
	EqNodesFree(&jdsp->arbMag.coeffGen);
	FFTConvolver2x2Free(&jdsp->arbMag.convState);
	jdsp_unlock(jdsp);
}
void ArbitraryResponseEqualizerStringParser(JamesDSPLib *jdsp, char *stringEq)
{
	jdsp_lock(jdsp);
	ArbitraryEqString2SortedNodes(&jdsp->arbMag.coeffGen, stringEq);
	float *eqFil = jdsp->arbMag.coeffGen.GetFilter(&jdsp->arbMag.coeffGen, (float)jdsp->fs);
	if (jdsp->arbMagForceRefresh)
	{
		FFTConvolver2x2Free(&jdsp->arbMag.convState);
		FFTConvolver2x2LoadImpulseResponse(&jdsp->arbMag.convState, (unsigned int)jdsp->blockSize, eqFil, eqFil, jdsp->arbMag.filterLen);
		jdsp->arbMagForceRefresh = 0;
	}
	else
		FFTConvolver2x2RefreshImpulseResponse(&jdsp->arbMag.convState, (unsigned int)jdsp->blockSize, eqFil, eqFil, jdsp->arbMag.filterLen);
	jdsp_unlock(jdsp);
}
void ArbitraryResponseEqualizerEnable(JamesDSPLib *jdsp)
{
	if (jdsp->arbMagForceRefresh)
	{
		jdsp_lock(jdsp);
		float *eqFil = jdsp->arbMag.coeffGen.GetFilter(&jdsp->arbMag.coeffGen, (float)jdsp->fs);
		FFTConvolver2x2Free(&jdsp->arbMag.convState);
		FFTConvolver2x2LoadImpulseResponse(&jdsp->arbMag.convState, (unsigned int)jdsp->blockSize, eqFil, eqFil, jdsp->arbMag.filterLen);
		jdsp->arbMagForceRefresh = 0;
		jdsp_unlock(jdsp);
	}
	jdsp->arbitraryMagEnabled = 1;
}
void ArbitraryResponseEqualizerDisable(JamesDSPLib *jdsp)
{
	jdsp->arbitraryMagEnabled = 0;
}
void ArbitraryResponseEqualizerProcess(JamesDSPLib *jdsp, size_t n)
{
	FFTConvolver2x2Process(&jdsp->arbMag.convState, jdsp->tmpBuffer[0], jdsp->tmpBuffer[1], jdsp->tmpBuffer[0], jdsp->tmpBuffer[1], (unsigned int)n);
}