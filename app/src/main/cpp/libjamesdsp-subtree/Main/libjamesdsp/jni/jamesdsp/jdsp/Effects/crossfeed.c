#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <float.h>
#include "../jdsp_header.h"
void CrossfeedConstructor(JamesDSPLib *jdsp)
{
	memset(&jdsp->advXF, 0, sizeof(jdsp->advXF));
}
void CrossfeedDestructor(JamesDSPLib *jdsp)
{
	jdsp_lock(jdsp);
	if (jdsp->advXF.conv[0])
	{
		FFTConvolver2x4x2Free(jdsp->advXF.conv[0]);
		FFTConvolver2x4x2Free(jdsp->advXF.conv[1]);
		FFTConvolver2x4x2Free(jdsp->advXF.conv[2]);
		free(jdsp->advXF.conv[0]);
		free(jdsp->advXF.conv[1]);
		free(jdsp->advXF.conv[2]);
	}
	if (jdsp->advXF.convLong)
	{
		TwoStageFFTConvolver2x4x2Free(jdsp->advXF.convLong);
		free(jdsp->advXF.convLong);
	}
	jdsp_unlock(jdsp);
}
void CrossfeedRefreshConv(JamesDSPLib *jdsp)
{
	jdsp_lock(jdsp);
	if (jdsp->advXF.conv[0])
	{
		FFTConvolver2x4x2Free(jdsp->advXF.conv[0]);
		FFTConvolver2x4x2Free(jdsp->advXF.conv[1]);
		FFTConvolver2x4x2Free(jdsp->advXF.conv[2]);
		free(jdsp->advXF.conv[0]);
		free(jdsp->advXF.conv[1]);
		free(jdsp->advXF.conv[2]);
	}
	if (jdsp->advXF.convLong)
	{
		TwoStageFFTConvolver2x4x2Free(jdsp->advXF.convLong);
		free(jdsp->advXF.convLong);
	}
	jdsp->advXF.conv[0] = (FFTConvolver2x4x2*)malloc(sizeof(FFTConvolver2x4x2));
	FFTConvolver2x4x2Init(jdsp->advXF.conv[0]);
	FFTConvolver2x4x2LoadImpulseResponse(jdsp->advXF.conv[0], (unsigned int)jdsp->blockSize, jdsp->blobsCh1[0], jdsp->blobsCh2[0], jdsp->blobsCh3[0], jdsp->blobsCh4[0], jdsp->blobsResampledLen);
	jdsp->advXF.conv[1] = (FFTConvolver2x4x2*)malloc(sizeof(FFTConvolver2x4x2));
	FFTConvolver2x4x2Init(jdsp->advXF.conv[1]);
	FFTConvolver2x4x2LoadImpulseResponse(jdsp->advXF.conv[1], (unsigned int)jdsp->blockSize, jdsp->blobsCh1[1], jdsp->blobsCh2[1], jdsp->blobsCh3[1], jdsp->blobsCh4[1], jdsp->blobsResampledLen);
	jdsp->advXF.conv[2] = (FFTConvolver2x4x2*)malloc(sizeof(FFTConvolver2x4x2));
	FFTConvolver2x4x2Init(jdsp->advXF.conv[2]);
	FFTConvolver2x4x2LoadImpulseResponse(jdsp->advXF.conv[2], (unsigned int)jdsp->blockSize, jdsp->blobsCh1[2], jdsp->blobsCh2[2], jdsp->blobsCh3[2], jdsp->blobsCh4[2], jdsp->blobsResampledLen);
	jdsp->advXF.convLong = (TwoStageFFTConvolver2x4x2*)malloc(sizeof(TwoStageFFTConvolver2x4x2));
	TwoStageFFTConvolver2x4x2Init(jdsp->advXF.convLong);
	unsigned int seg1Len = (unsigned int)jdsp->blockSize;
	unsigned int seg2Len = (jdsp->frameLenSVirResampled - seg1Len) / 4;
	TwoStageFFTConvolver2x4x2LoadImpulseResponse(jdsp->advXF.convLong, seg1Len, seg2Len, jdsp->hrtfblobsResampled[0], jdsp->hrtfblobsResampled[1], jdsp->hrtfblobsResampled[2], jdsp->hrtfblobsResampled[3], jdsp->frameLenSVirResampled);
	jdsp_unlock(jdsp);
}
void CrossfeedEnable(JamesDSPLib *jdsp)
{
	if (jdsp->advXF.mode >= 2)
	{
		if (jdsp->crossfeedForceRefresh || !jdsp->advXF.conv[0] || !jdsp->advXF.conv[1] || !jdsp->advXF.conv[2] || !jdsp->advXF.convLong)
		{
			CrossfeedRefreshConv(jdsp);
			jdsp->crossfeedForceRefresh = 0;
		}
	}
	if (jdsp->advXF.mode < 2)
	{
		memset(&jdsp->advXF.bs2b, 0, sizeof(jdsp->advXF.bs2b));
		BS2BInit(&jdsp->advXF.bs2b[0], (unsigned int)jdsp->fs, BS2B_DEFAULT_CLEVEL);
		BS2BInit(&jdsp->advXF.bs2b[1], (unsigned int)jdsp->fs, BS2B_JMEIER_CLEVEL);
	}
	jdsp->crossfeedEnabled = 1;
}
void CrossfeedDisable(JamesDSPLib *jdsp)
{
	jdsp->crossfeedEnabled = 0;
}
void CrossfeedChangeMode(JamesDSPLib *jdsp, int nMode)
{
	if (nMode < 0)
		nMode = 0;
	if (nMode > 5)
		nMode = 5;
	if (nMode >= 2)
	{
		if (jdsp->crossfeedForceRefresh || !jdsp->advXF.conv[0] || !jdsp->advXF.conv[1] || !jdsp->advXF.conv[2] || !jdsp->advXF.convLong)
		{
			CrossfeedRefreshConv(jdsp);
			jdsp->crossfeedForceRefresh = 0;
		}
	}
	if (nMode < 2)
	{
		memset(&jdsp->advXF.bs2b, 0, sizeof(jdsp->advXF.bs2b));
		if (!nMode)
			BS2BInit(&jdsp->advXF.bs2b[1], (unsigned int)jdsp->fs, BS2B_CMOY_CLEVEL);
		else
			BS2BInit(&jdsp->advXF.bs2b[1], (unsigned int)jdsp->fs, BS2B_JMEIER_CLEVEL);
	}
	jdsp->advXF.mode = nMode;
}
void CrossfeedProcess(JamesDSPLib *jdsp, size_t n)
{
	if (jdsp->advXF.mode < 2)
	{
		double tmpL, tmpR;
		for (size_t i = 0; i < n; i++)
		{
			tmpL = (double)jdsp->tmpBuffer[0][i];
			tmpR = (double)jdsp->tmpBuffer[1][i];
			BS2BProcess(&jdsp->advXF.bs2b[jdsp->advXF.mode], &tmpL, &tmpR);
			jdsp->tmpBuffer[0][i] = (float)tmpL;
			jdsp->tmpBuffer[1][i] = (float)tmpR;
		}
	}
	else if (jdsp->advXF.mode < 5)
		FFTConvolver2x4x2Process(jdsp->advXF.conv[jdsp->advXF.mode - 2], jdsp->tmpBuffer[0], jdsp->tmpBuffer[1], jdsp->tmpBuffer[0], jdsp->tmpBuffer[1], (unsigned int)n);
	else
		TwoStageFFTConvolver2x4x2Process(jdsp->advXF.convLong, jdsp->tmpBuffer[0], jdsp->tmpBuffer[1], jdsp->tmpBuffer[0], jdsp->tmpBuffer[1], (unsigned int)n);
}