#include <string.h>
#include <stdlib.h>
#include <math.h>
#include "../jdsp_header.h"
void initWarpedPFB(WarpedPFB *pfb, double fs, unsigned int N, unsigned int m);
void assignPtrWarpedPFB(WarpedPFB *pfb, unsigned int N, unsigned int m);
size_t getMemSizeWarpedPFB(unsigned int N, unsigned int m);
void analysisWarpedPFBStereo(WarpedPFB *pfb1, WarpedPFB *pfb2, float *x1, float *x2);
void synthesisWarpedPFBStereo(WarpedPFB *pfb1, WarpedPFB *pfb2, float *y1, float *y2);
void StereoEnhancementSetParam(JamesDSPLib *jdsp, float mix)
{
    jdsp_lock(jdsp);
	jdsp->sterEnh.mix = mix;
	jdsp->sterEnh.minusMix = 1.0f - jdsp->sterEnh.mix;
	if (jdsp->sterEnh.mix > 0.5f)
		jdsp->sterEnh.gain = 3.0f - jdsp->sterEnh.mix * 2.0f;
	else
		jdsp->sterEnh.gain = jdsp->sterEnh.mix * 2.0f + 1.0f;
	size_t memSize = getMemSizeWarpedPFB(5, 2);
	if (jdsp->sterEnh.subband[0])
	{
		free(jdsp->sterEnh.subband[0]);
		jdsp->sterEnh.subband[0] = 0;
	}
	if (jdsp->sterEnh.subband[1])
	{
		free(jdsp->sterEnh.subband[1]);
		jdsp->sterEnh.subband[1] = 0;
	}
	if (!jdsp->sterEnh.subband[0])
		jdsp->sterEnh.subband[0] = (char*)malloc(memSize);
	if (!jdsp->sterEnh.subband[1])
		jdsp->sterEnh.subband[1] = (char*)malloc(memSize);
	initWarpedPFB((WarpedPFB*)jdsp->sterEnh.subband[0], jdsp->fs, 5, 2);
	assignPtrWarpedPFB((WarpedPFB*)jdsp->sterEnh.subband[1], 5, 2);
	unsigned int *Sk = ((WarpedPFB*)jdsp->sterEnh.subband[0])->Sk;
	float ms = 0.75f; // 0.75 ms
	for (unsigned int i = 0; i < 5; i++)
		jdsp->sterEnh.emaAlpha[i] = 1.0f - powf(10.0f, (log10f(0.5f) / (ms / 1000.0f) / (jdsp->fs / (float)Sk[i])));
    jdsp_unlock(jdsp);
}
void StereoEnhancementConstructor(JamesDSPLib *jdsp)
{
    jdsp_lock(jdsp);
	jdsp->sterEnh.subband[1] = jdsp->sterEnh.subband[0] = 0;
    jdsp_unlock(jdsp);
}
void StereoEnhancementDestructor(JamesDSPLib *jdsp)
{
    jdsp_lock(jdsp);
	if (jdsp->sterEnh.subband[0])
		free(jdsp->sterEnh.subband[0]);
	if (jdsp->sterEnh.subband[1])
		free(jdsp->sterEnh.subband[1]);
    jdsp_unlock(jdsp);
}
void StereoEnhancementEnable(JamesDSPLib *jdsp)
{
	jdsp->sterEnhEnabled = 1;
}
void StereoEnhancementDisable(JamesDSPLib *jdsp)
{
	jdsp->sterEnhEnabled = 0;
}
void StereoEnhancementProcess(JamesDSPLib *jdsp, size_t n)
{
	stereoEnhancement *snh = &jdsp->sterEnh;
	unsigned int *samplingPeriod = ((WarpedPFB*)snh->subband[0])->decimationCounter;
	unsigned int *Sk = ((WarpedPFB*)snh->subband[0])->Sk;
	float *bandLeft = ((WarpedPFB*)snh->subband[0])->subbandData;
	float *bandRight = ((WarpedPFB*)snh->subband[1])->subbandData;
	float y1, y2;
	for (size_t i = 0; i < n; i++)
	{
		analysisWarpedPFBStereo((WarpedPFB*)snh->subband[0], (WarpedPFB*)snh->subband[1], &jdsp->tmpBuffer[0][i], &jdsp->tmpBuffer[1][i]);
		for (int j = 0; j < 5; j++)
		{
			if (samplingPeriod[j] == Sk[j])
			{
				float sum = bandLeft[j] + bandRight[j];
				float diff = bandLeft[j] - bandRight[j];
				float sumSq = sum * sum;
				float diffSq = diff * diff;
				snh->sumStates[j] = snh->sumStates[j] * (1.0f - snh->emaAlpha[j]) + sumSq * snh->emaAlpha[j];
				snh->diffStates[j] = snh->diffStates[j] * (1.0f - snh->emaAlpha[j]) + diffSq * snh->emaAlpha[j];
				float centre = 0.0f;
				if (sumSq > FLT_EPSILON)
					centre = (0.5f - sqrtf(snh->diffStates[j] / snh->sumStates[j]) * 0.5f) * sum;
				bandLeft[j] = (bandLeft[j] - centre) * snh->mix + centre * snh->minusMix;
				bandRight[j] = (bandRight[j] - centre) * snh->mix + centre * snh->minusMix;
			}
		}
		synthesisWarpedPFBStereo((WarpedPFB*)snh->subband[0], (WarpedPFB*)snh->subband[1], &y1, &y2);
		jdsp->tmpBuffer[0][i] = y1 * snh->gain;
		jdsp->tmpBuffer[1][i] = y2 * snh->gain;
	}
}
