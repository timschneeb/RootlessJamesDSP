#include <math.h>
#include <float.h>
#include "../jdsp_header.h"
void JLimiterSetCoefficients(JamesDSPLib *jdsp, double thresholddB, double msRelease)
{
	if (msRelease < 1.5)
		msRelease = 1.5;
	jdsp->limiter.relCoef = (float)exp(-1000.0 / (msRelease * round((double)jdsp->fs)));
	jdsp->limiter.threshold = (float)pow(10.0, thresholddB / 20.0);
}
void JLimiterInit(JamesDSPLib *jdsp)
{
	jdsp->limiter.envOverThreshold = 0.0f;
}
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <float.h>
#include "eel2/numericSys/codelet.h"
#ifndef min
#define min(a,b) (((a)<(b))?(a):(b))
#endif
#ifndef max
#define max(a,b) (((a)>(b))?(a):(b))
#endif
#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif
float map(float x, float in_min, float in_max, float out_min, float out_max)
{
	return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
}
static void getAsymmetricWindow(float *analysisWnd, float *synthesisWnd, int k, int m, int p, double freq_temporal)
{
	int i;
	if ((k / m) < 4)
		freq_temporal = 1.0f;
	if (freq_temporal > 9.0f)
		freq_temporal = 9.0f;
	memset(synthesisWnd, 0, k * sizeof(float));
	int n = ((k - m) << 1) + 2;
	for (i = 0; i < k - m; ++i)
		analysisWnd[i] = (float)pow(0.5 * (1.0 - cos(2.0 * M_PI * (i + 1.0) / (double)n)), freq_temporal);
	n = (m << 1) + 2;
	if (freq_temporal > 1.02)
		freq_temporal = 1.02;
	for (i = k - m; i < k; ++i)
		analysisWnd[i] = (float)pow(sqrt(0.5 * (1.0 - cos(2.0 * M_PI * ((m + i - (k - m)) + 1.0) / (double)n))), freq_temporal);
	n = m << 1;
	for (i = k - (m << 1); i < k; ++i)
		synthesisWnd[i] = (float)(0.5 * (1.0 - cos(2.0 * M_PI * (double)(i - (k - (m << 1))) / (double)n))) / analysisWnd[i];
	// Pre-shift window function
	for (i = 0; i < k - p; i++)
		synthesisWnd[i] = synthesisWnd[i + p];
}
unsigned int LLIntegerLog2M(unsigned int v)
{
	unsigned int i = 0;
	while (v > 1)
	{
		++i;
		v >>= 1;
	}
	return i;
}
unsigned LLRevBitsM(unsigned int x, unsigned int bits)
{
	unsigned int y = 0;
	while (bits--)
	{
		y = (y + y) + (x & 1);
		x >>= 1;
	}
	return y;
}
void LLbitReversalTblM(unsigned *dst, unsigned int fftLen)
{
	unsigned int bits = LLIntegerLog2M(fftLen);
	for (int i = 0; i < fftLen; ++i)
		dst[i] = LLRevBitsM(i, bits);
}
void LLsinHalfTblFloatM(float *dst, unsigned int fftLen)
{
	const double twopi_over_n = 6.283185307179586476925286766559 / fftLen;
	for (int i = 0; i < fftLen >> 1; ++i)
		dst[i] = (float)sin(twopi_over_n * i);
}
#include "../generalDSP/spectralInterpolatorFloat.h"
static size_t choose(float *a, float *b, size_t src1, size_t src2)
{
	return (*b >= *a) ? src2 : src1;
}
static size_t fast_upper_bound4(float *vec, size_t n, float *value)
{
	size_t size = n;
	size_t low = 0;
	while (size >= 8)
	{
		size_t half = size / 2;
		size_t other_half = size - half;
		size_t probe = low + half;
		size_t other_low = low + other_half;
		size = half;
		low = choose(&vec[probe], value, low, other_low);

		half = size / 2;
		other_half = size - half;
		probe = low + half;
		other_low = low + other_half;
		size = half;
		low = choose(&vec[probe], value, low, other_low);

		half = size / 2;
		other_half = size - half;
		probe = low + half;
		other_low = low + other_half;
		size = half;
		low = choose(&vec[probe], value, low, other_low);
	}
	while (size > 0) {
		size_t half = size / 2;
		size_t other_half = size - half;
		size_t probe = low + half;
		size_t other_low = low + other_half;
		size = half;
		low = choose(&vec[probe], value, low, other_low);
	}
	return low;
}
static float lerp1DNoExtrapo(float val, float *x, float *y, int n)
{
	if (val <= x[0])
		return y[0];
	if (val >= x[n - 1])
		return y[n - 1];
	size_t j = fast_upper_bound4(x, n, &val);
	return ((val - x[j - 1]) / (x[j] - x[j - 1])) * (y[j] - y[j - 1]) + y[j - 1]; // Interpolation
}
#define MIN(a, b) (((a)<(b))?(a):(b))
#define MAX(a, b) (((a)>(b))?(a):(b))
#define CLAMP(x, min, max) ((x)>(max)?(max):(((x)<(min))?(min):(x)))
#define ABS(x) ((x)<0?(-(x)):(x))
#define SQUARE(x) ((x)*(x))
#define MIX(x0, y1, coeff) (((x0)-(y1))*(coeff)+(y1))
// Constants
#define MINATTACKTIME 0.1
#define MAXATTACKTIME 200.0
#define MINRELEASETIME 5.0
#define MAXRELEASETIME 800.0
#define MINADAPTTIME (MAXRELEASETIME / 2.0)
#define MAXADAPTTIME (MAXRELEASETIME * 2.0)
#include <stdint.h>
void FFTCompressorSetSpectralFollowingRate(FFTDynamicRangeSquasher *comp, float fs, float fgt_facT)
{
	comp->spectralRate = fs / (float)comp->fftLen * (float)ANALYSIS_OVERLAP_DRS;
	comp->fgt_fac = 1.0 - exp(-1.0 / (fgt_facT / 1000.0 * comp->spectralRate));
}
void FFTCompressorSetParam(FFTDynamicRangeSquasher *comp, float fs, float maxAtk, float maxRel, float adapt)
{
	comp->spectralRate = fs / (float)comp->fftLen * (float)ANALYSIS_OVERLAP_DRS;
	comp->metaMaxAttackTime = 2.0f * ((float)maxAtk / 1000.0f);
	comp->metaMaxReleaseTime = 2.0f * ((float)maxRel / 1000.0f);
	float metaAdaptTime = (float)adapt / 1000.0f;// 电平回复系数，回复对数电平到0dB
	for (unsigned int i = 0; i < comp->procUpTo; i++)
		comp->metaAdaptCoeff[i] = (float)(1.0 - exp(-1.0 / (metaAdaptTime * comp->spectralRate)));
}
static float my_logf(float a)
{
	float m, r, s, t, i, f;
	int32_t e;

	if ((a > 0.0f) && (a <= 3.40e+38f)) { // 0x1.fffffep+127
		m = frexpf(a, &e);
		if (m < 0.666666667f) {
			m = m + m;
			e = e - 1;
		}
		i = (float)e;
		/* m in [2/3, 4/3] */
		f = m - 1.0f;
		s = f * f;
		/* Compute log1p(f) for f in [-1/3, 1/3] */
		r = fmaf(-0.130187988f, f, 0.140889585f); // -0x1.0aa000p-3, 0x1.208ab8p-3
		t = fmaf(-0.121489584f, f, 0.139809534f); // -0x1.f19f10p-4, 0x1.1e5476p-3
		r = fmaf(r, s, t);
		r = fmaf(r, f, -0.166845024f); // -0x1.55b2d8p-3
		r = fmaf(r, f, 0.200121149f); //  0x1.99d91ep-3
		r = fmaf(r, f, -0.249996364f); // -0x1.fffe18p-3
		r = fmaf(r, f, 0.333331943f); //  0x1.5554f8p-2
		r = fmaf(r, f, -0.500000000f); // -0x1.000000p-1
		r = fmaf(r, s, f);
		r = fmaf(i, 0.693147182f, r); //   0x1.62e430p-1 // log(2) 
		return r;
	}
	return 0.0f;
}
static float processfftComp(FFTDynamicRangeSquasher *comp, unsigned int idx, float logIn)
{
	// Attack and release coefficients
	float myAttackCoeff;
	if (comp->metaMaxAttackTime < FLT_EPSILON)
		myAttackCoeff = 1.0f;
	else
		myAttackCoeff = 1.0f - expf(-1.0f / (comp->metaMaxAttackTime * comp->spectralRate));
	float trRel = comp->metaMaxReleaseTime - comp->metaMaxAttackTime;
	float myReleaseCoeff;
	if (trRel < FLT_EPSILON)
		myReleaseCoeff = 1.0f;
	else
		myReleaseCoeff = 1.0f - expf(-1.0f / (trRel * comp->spectralRate));
	float logOvershoot = logIn - comp->logThreshold[idx];
	// Set estimate for average log gain
	float logGainEstimate = comp->logThreshold[idx] * 0.5f;
	// Set knee width
	float myLogWidth = MAX(-(comp->smoothLogGain[idx] + logGainEstimate), 0.0f);
	// Soft knee rectification
	float logGain = 0.0f;
	if (logOvershoot >= myLogWidth * 0.5f)
		logGain = logOvershoot;
	else if (logOvershoot > (-myLogWidth * 0.5f) && logOvershoot < (myLogWidth * 0.5f))
		logGain = 1.0f / (2.0f * myLogWidth) * SQUARE(logOvershoot + (myLogWidth * 0.5f));
	// 更新功率包络的上升沿、下降沿
	comp->adaptiveRelease[idx] = MAX(logGain, MIX(logGain, comp->adaptiveRelease[idx], myReleaseCoeff));
	comp->adaptiveAttack[idx] = MIX(comp->adaptiveRelease[idx], comp->adaptiveAttack[idx], myAttackCoeff);
	// 反增益
	logGain = -comp->adaptiveAttack[idx];
	// 平滑增益
	comp->smoothLogGain[idx] = MIX(logGain - logGainEstimate, comp->smoothLogGain[idx], comp->metaAdaptCoeff[idx]);
	// 防止超越0dB
	if (logIn + logGain - (comp->smoothLogGain[idx] + logGainEstimate) > 0.0f)
		comp->smoothLogGain[idx] = logIn + logGain - logGainEstimate;
	// Apply automatic gain
	logGain -= comp->smoothLogGain[idx] + logGainEstimate;
	// Update threshold to recent average
	comp->logThreshold[idx] = comp->logThreshold[idx] + comp->fgt_fac * (logIn - comp->logThreshold[idx]);
	return expf(logGain); // Convert to linear
}
int FFTDynamicRangeSquasherProcessSamples(FFTDynamicRangeSquasher *cm, const float *inLeft, const float *inRight, unsigned int inSampleCount, float *outL, float *outR)
{
	unsigned int outSampleCount, maxOutSampleCount, copyCount;
	outSampleCount = 0;
	maxOutSampleCount = inSampleCount;

	while (inSampleCount > 0) {
		copyCount = min(cm->mInputSamplesNeeded, inSampleCount);
		float *sampDL = &cm->mInput[0][cm->mInputPos];
		float *sampDR = &cm->mInput[1][cm->mInputPos];
		const float *max = inLeft + copyCount;
		while (inLeft < max)
		{
			*sampDL = *inLeft;
			*sampDR = *inRight;
			inLeft += 1;
			inRight += 1;
			sampDL += 1;
			sampDR += 1;
		}
		inSampleCount -= copyCount;
		cm->mInputPos = (cm->mInputPos + copyCount) & cm->minus_fftLen;
		cm->mInputSamplesNeeded -= copyCount;
		if (cm->mInputSamplesNeeded == 0)
		{
			unsigned int i;
			// copy to temporary buffer and FHT
			for (i = 0; i < cm->fftLen; ++i)
			{
				const unsigned int k = (i + cm->mInputPos) & cm->minus_fftLen;
				const float w = cm->analysisWnd[i];
				cm->mTempLBuffer[cm->mBitRev[i]] = (cm->mInput[0][k] * w);
				cm->mTempRBuffer[cm->mBitRev[i]] = (cm->mInput[1][k] * w);
			}
			cm->fft(cm->mTempLBuffer, cm->mSineTab);
			cm->fft(cm->mTempRBuffer, cm->mSineTab);
			// Spectral analysis
			int symIdx;
			float lR = cm->mTempLBuffer[0] * 2.0f;
			float rR = cm->mTempRBuffer[0] * 2.0f;
			float leftMag, rightMag, currentMagnitude, mask;
			cm->timeDomainOut[0][0] = lR;
			cm->timeDomainOut[1][0] = rR;
			unsigned int bitRevFwd, bitRevSym;
			//idxFrame++;
			unsigned int specLen = *((unsigned int*)(cm->octaveSmooth));
			float reciprocal = *((float*)(cm->octaveSmooth + sizeof(unsigned int)));
			unsigned int lpLen = *((unsigned int*)(cm->octaveSmooth + sizeof(unsigned int) + sizeof(float)));
			float *lv1 = (float*)(cm->octaveSmooth + sizeof(unsigned int) + sizeof(float) + sizeof(unsigned int) + (lpLen << 1) * sizeof(unsigned int) + lpLen * sizeof(float));
			float *lv2 = (float*)(cm->octaveSmooth + sizeof(unsigned int) + sizeof(float) + sizeof(unsigned int) + (lpLen << 1) * sizeof(unsigned int) + lpLen * sizeof(float) + (lpLen + 3) * sizeof(float));
			if (!cm->noGridDownsampling)
			{
				leftMag = fabsf(lR);
				rightMag = fabsf(rR);
				cm->mag[0] = leftMag > rightMag ? leftMag : rightMag;
				for (i = 1; i < cm->procUpTo; i++)
				{
					symIdx = cm->fftLen - i;
					bitRevFwd = cm->mBitRev[i];
					bitRevSym = cm->mBitRev[symIdx];
					lR = cm->mTempLBuffer[i] + cm->mTempLBuffer[symIdx];
					float lI = cm->mTempLBuffer[i] - cm->mTempLBuffer[symIdx];
					rR = cm->mTempRBuffer[i] + cm->mTempRBuffer[symIdx];
					float rI = cm->mTempRBuffer[i] - cm->mTempRBuffer[symIdx];
					float absV1 = fabsf(lR);
					float absV2 = fabsf(lI);
					leftMag = max((127.0f / 128.0f) * max(absV1, absV2) + (3.0f / 16.0f) * min(absV1, absV2), (27.0f / 32.0f) * max(absV1, absV2) + (71.0f / 128.0f) * min(absV1, absV2));
					absV1 = fabsf(rR);
					absV2 = fabsf(rI);
					rightMag = max((127.0f / 128.0f) * max(absV1, absV2) + (3.0f / 16.0f) * min(absV1, absV2), (27.0f / 32.0f) * max(absV1, absV2) + (71.0f / 128.0f) * min(absV1, absV2));
					currentMagnitude = leftMag > rightMag ? leftMag : rightMag;
					cm->mag[i] = currentMagnitude;
				}
				ShrinkGridSpectralInterpolator(cm->octaveSmooth, cm->procUpTo, cm->mag, cm->aheight);
				for (i = 0; i < cm->smallGridSize; i++)
				{
					float magNormalized = cm->aheight[i];
					// Log conversion
					float logIn;
					if (magNormalized <= FLT_EPSILON)
						logIn = -15.942385152f;
					else
						logIn = my_logf(magNormalized);
					float mask = processfftComp(cm, i, logIn);
					cm->finalGain[i] = mask;
				}
				for (i = 1; i < cm->procUpTo; i++)
				{
					symIdx = cm->fftLen - i;
					bitRevFwd = cm->mBitRev[i];
					bitRevSym = cm->mBitRev[symIdx];
					float val = i * reciprocal;
					if (val <= lv1[0])
						mask = cm->finalGain[0];
					else if (val >= lv1[lpLen + 3 - 1])
						mask = cm->finalGain[lpLen + 3 - 1];
					else
					{
						size_t j = fast_upper_bound4(lv1, lpLen + 3, &val);
						mask = ((val - lv1[j - 1]) * lv2[j - 1]) * (cm->finalGain[j] - cm->finalGain[j - 1]) + cm->finalGain[j - 1];
					}
					lR = cm->mTempLBuffer[i] + cm->mTempLBuffer[symIdx];
					float lI = cm->mTempLBuffer[i] - cm->mTempLBuffer[symIdx];
					rR = cm->mTempRBuffer[i] + cm->mTempRBuffer[symIdx];
					float rI = cm->mTempRBuffer[i] - cm->mTempRBuffer[symIdx];
					cm->timeDomainOut[0][bitRevFwd] = (lR + lI) * mask;
					cm->timeDomainOut[0][bitRevSym] = (lR - lI) * mask;
					cm->timeDomainOut[1][bitRevFwd] = (rR + rI) * mask;
					cm->timeDomainOut[1][bitRevSym] = (rR - rI) * mask;
				}
				//printf("\n");
			}
			else
			{
				leftMag = fabsf(cm->mTempLBuffer[0]);
				rightMag = fabsf(cm->mTempRBuffer[0]);
				currentMagnitude = leftMag > rightMag ? leftMag : rightMag;
				float magNormalized = currentMagnitude;
				// Log conversion
				float logIn;
				if (magNormalized <= FLT_EPSILON)
					logIn = -15.942385152f;
				else
					logIn = my_logf(magNormalized);
				float mask = processfftComp(cm, 0, logIn);
				cm->timeDomainOut[0][0] = cm->mTempLBuffer[0] * mask;
				cm->timeDomainOut[1][0] = cm->mTempRBuffer[0] * mask;
				for (i = 1; i < cm->procUpTo; i++)
				{
					symIdx = cm->fftLen - i;
					bitRevFwd = cm->mBitRev[i];
					bitRevSym = cm->mBitRev[symIdx];
					lR = cm->mTempLBuffer[i] + cm->mTempLBuffer[symIdx];
					float lI = cm->mTempLBuffer[i] - cm->mTempLBuffer[symIdx];
					rR = cm->mTempRBuffer[i] + cm->mTempRBuffer[symIdx];
					float rI = cm->mTempRBuffer[i] - cm->mTempRBuffer[symIdx];
					float absV1 = fabsf(lR);
					float absV2 = fabsf(lI);
					leftMag = max((127.0f / 128.0f) * max(absV1, absV2) + (3.0f / 16.0f) * min(absV1, absV2), (27.0f / 32.0f) * max(absV1, absV2) + (71.0f / 128.0f) * min(absV1, absV2));
					absV1 = fabsf(rR);
					absV2 = fabsf(rI);
					rightMag = max((127.0f / 128.0f) * max(absV1, absV2) + (3.0f / 16.0f) * min(absV1, absV2), (27.0f / 32.0f) * max(absV1, absV2) + (71.0f / 128.0f) * min(absV1, absV2));
					currentMagnitude = leftMag > rightMag ? leftMag : rightMag;
					magNormalized = currentMagnitude;
					// Log conversion
					if (magNormalized <= FLT_EPSILON)
						logIn = -15.942385152f;
					else
						logIn = my_logf(magNormalized);
					mask = processfftComp(cm, i, logIn);
					cm->timeDomainOut[0][bitRevFwd] = (lR + lI) * mask;
					cm->timeDomainOut[0][bitRevSym] = (lR - lI) * mask;
					cm->timeDomainOut[1][bitRevFwd] = (rR + rI) * mask;
					cm->timeDomainOut[1][bitRevSym] = (rR - rI) * mask;
				}
			}
			for (i = cm->procUpTo; i < cm->halfLen; i++)
			{
				symIdx = cm->fftLen - i;
				bitRevFwd = cm->mBitRev[i];
				bitRevSym = cm->mBitRev[symIdx];
				lR = cm->mTempLBuffer[i] + cm->mTempLBuffer[symIdx];
				float lI = cm->mTempLBuffer[i] - cm->mTempLBuffer[symIdx];
				rR = cm->mTempRBuffer[i] + cm->mTempRBuffer[symIdx];
				float rI = cm->mTempRBuffer[i] - cm->mTempRBuffer[symIdx];
				cm->timeDomainOut[0][bitRevFwd] = lR + lI;
				cm->timeDomainOut[0][bitRevSym] = lR - lI;
				cm->timeDomainOut[1][bitRevFwd] = rR + rI;
				cm->timeDomainOut[1][bitRevSym] = rR - rI;
			}
			// reconstitute left/right channels
			cm->fft(cm->timeDomainOut[0], cm->mSineTab);
			cm->fft(cm->timeDomainOut[1], cm->mSineTab);
			cm->mOutputBufferCount++;
			if (cm->mOutputBufferCount > 2)
				continue;
			float *outBuffer = cm->mOutputBuffer[cm->mOutputBufferCount - 1];
			for (i = 0; i < cm->ovpLen; ++i)
			{
				outBuffer[0] = cm->mOverlapStage2dash[0][i] + (cm->timeDomainOut[0][i + cm->smpShift] * cm->synthesisWnd[i]);
				outBuffer[1] = cm->mOverlapStage2dash[1][i] + (cm->timeDomainOut[1][i + cm->smpShift] * cm->synthesisWnd[i]);
				outBuffer += 2;
				// overlapping
				cm->mOverlapStage2dash[0][i] = (cm->timeDomainOut[0][cm->smpShift + cm->ovpLen + i] * cm->synthesisWnd[i + cm->ovpLen]);
				cm->mOverlapStage2dash[1][i] = (cm->timeDomainOut[1][cm->smpShift + cm->ovpLen + i] * cm->synthesisWnd[i + cm->ovpLen]);
			}
			cm->mInputSamplesNeeded = cm->ovpLen;
		}
	}
	while ((cm->mOutputBufferCount > 0) && (outSampleCount < maxOutSampleCount)) {
		float *sampD = cm->mOutputBuffer[0];
		copyCount = min(cm->ovpLen - cm->mOutputReadSampleOffset, maxOutSampleCount - outSampleCount);
		float *out = sampD + (cm->mOutputReadSampleOffset * 2);
		float *max = outL + copyCount;
		while (outL < max)
		{
			*outL = *out;
			out += 1;
			*outR = *out;
			out += 1;
			outL += 1;
			outR += 1;
		}
		outSampleCount += copyCount;
		cm->mOutputReadSampleOffset += copyCount;
		if (cm->mOutputReadSampleOffset >= cm->ovpLen)
		{
			cm->mOutputBufferCount--;
			cm->mOutputReadSampleOffset = 0;
			if (cm->mOutputBufferCount > 0) {
				unsigned int i;
				float *moveToEnd = cm->mOutputBuffer[0];
				// Shift the buffers so that the current one for reading is at index 0
				for (i = 1; i < MAX_OUTPUT_BUFFERS_DRS; i++)
					cm->mOutputBuffer[i - 1] = cm->mOutputBuffer[i];
				cm->mOutputBuffer[MAX_OUTPUT_BUFFERS_DRS - 1] = 0;
				// Move the previous first buffer to the end (first null pointer)
				for (i = 0; i < MAX_OUTPUT_BUFFERS_DRS; i++)
				{
					if (!cm->mOutputBuffer[i])
					{
						cm->mOutputBuffer[i] = moveToEnd;
						break;
					}
				}
			}
		}
	}
	return outSampleCount;
}
void FFTDynamicRangeSquasherSetavgBW(FFTDynamicRangeSquasher *cm, double avgBW)
{
	unsigned int fcLen;
	size_t virtualStructSize = EstimateMemorySpectralInterpolator(&fcLen, cm->procUpTo, avgBW, &cm->smallGridSize);
	if (fcLen)
	{
		cm->noGridDownsampling = 0;
		InitSpectralInterpolator(cm->octaveSmooth, fcLen, cm->procUpTo, avgBW);
		//for (unsigned int i = 0; i < cm->smallGridSize; i++)
			//cm->ratioOld[i] = cm->ratio[i] = cm->ratio2[i] = 1.0f;
	}
	else
	{
		cm->smallGridSize = 0;
		cm->noGridDownsampling = 1;
		//for (unsigned int i = 0; i < cm->procUpTo; i++)
			//cm->ratioOld[i] = cm->ratio[i] = cm->ratio2[i] = 1.0f;
	}
}
void FFTDynamicRangeSquasherInit(FFTDynamicRangeSquasher *cm, float fs)
{
	memset(cm, 0, sizeof(FFTDynamicRangeSquasher));
	unsigned int i;
	const float oX[10] = { 750, 1500, 3000, 6000, 12000, 24000, 48000, 96000, 192000, 256000 };
	const float oY[10] = { 24, 48, 96, 192, 384, 768, 1536, 3072, 6144, 8192 };
	const float oX2[6] = { 0.0f, 0.25f, 0.4f, 0.5f, 0.75f, 1.0f };
	const float oY2[6] = { 9.0f, 7.0f, 4.5f, 4.0f, 2.0f, 1.0f };
	float frameLen = lerp1DNoExtrapo(fs, oX, oY, 10);
	float nextPwr2 = powf(2.0f, ceilf(logf(frameLen) / logf(2.0f)));
	float paddingRatio = map(frameLen / nextPwr2, 0.5f, 1.0f, 0.0f, 1.0f);
	float wndBeta = lerp1DNoExtrapo(paddingRatio, oX2, oY2, 6);
	cm->fftLen = (unsigned int)nextPwr2;
	if (cm->fftLen == 32)
		cm->fft = DFT32;
	else if (cm->fftLen == 64)
		cm->fft = DFT64;
	else if (cm->fftLen == 128)
		cm->fft = DFT128;
	else if (cm->fftLen == 256)
		cm->fft = DFT256;
	else if (cm->fftLen == 512)
		cm->fft = DFT512;
	else if (cm->fftLen == 1024)
		cm->fft = DFT1024;
	else if (cm->fftLen == 2048)
		cm->fft = DFT2048;
	else if (cm->fftLen == 4096)
		cm->fft = DFT4096;
	else
		cm->fft = DFT8192;
	cm->minus_fftLen = cm->fftLen - 1;
	cm->ovpLen = cm->fftLen / ANALYSIS_OVERLAP_DRS;
	cm->halfLen = (cm->fftLen >> 1) + 1;
	cm->smpShift = (cm->fftLen - (cm->ovpLen << 1));
	const float desiredProcessFreq = 24000.0f;
	unsigned int idx = (unsigned int)(desiredProcessFreq / (fs / (float)cm->fftLen)) + 1UL;
	if (idx > cm->halfLen)
		cm->procUpTo = cm->halfLen;
	else
		cm->procUpTo = idx;
	LLbitReversalTblM(cm->mBitRev, cm->fftLen);
	LLsinHalfTblFloatM(cm->mSineTab, cm->fftLen);
	for (i = 0; i < MAX_OUTPUT_BUFFERS_DRS; i++)
		cm->mOutputBuffer[i] = cm->buffer[i];
	cm->mInputSamplesNeeded = cm->ovpLen;
	cm->mInputPos = 0;
	cm->mOutputBufferCount = 0;
	cm->mOutputReadSampleOffset = 0;
	getAsymmetricWindow(cm->analysisWnd, cm->synthesisWnd, cm->fftLen, cm->ovpLen, cm->smpShift, wndBeta);
	for (i = 0; i < cm->fftLen; i++)
		cm->analysisWnd[i] *= (1.0f / cm->fftLen) * 0.5f;
	float sum = 0.0f;
	for (i = 0; i < cm->fftLen; i++)
		sum += cm->analysisWnd[i];
	cm->spectralRate = fs / (float)cm->fftLen * (float)ANALYSIS_OVERLAP_DRS;
	float fgt_facT = 30.0f; // Adaptive threshold
	double avgBW = 1.00005;
	for (unsigned int i = 0; i < cm->procUpTo; i++)
	{
		cm->metaAdaptCoeff[i] = 0.0f;
		cm->adaptiveRelease[i] = 1.2f;
		cm->adaptiveAttack[i] = 1.2f;
		cm->smoothLogGain[i] = 2.0f;
		cm->logThreshold[i] = -7.0f;
	}
	FFTDynamicRangeSquasherSetavgBW(cm, avgBW);
	FFTCompressorSetSpectralFollowingRate(cm, fs, fgt_facT);
	FFTCompressorSetParam(cm, fs, 100.0, 500.0, 800.0);
}
void CompressorEnable(JamesDSPLib *jdsp)
{
	jdsp->compEnabled = 1;
}
void CompressorDisable(JamesDSPLib *jdsp)
{
	jdsp->compEnabled = 0;
}
void CompressorReset(JamesDSPLib *jdsp)
{
	FFTDynamicRangeSquasherInit(&jdsp->comp, jdsp->fs);
}
void CompressorSetParam(JamesDSPLib *jdsp, float maxAtk, float maxRel, float adapt)
{
	FFTCompressorSetParam(&jdsp->comp, jdsp->fs, maxAtk, maxRel, adapt);
}
void CompressorProcess(JamesDSPLib *jdsp, size_t n)
{
	unsigned int offset = 0;
	while (offset < n)
	{
		const unsigned int processing = min(n - offset, jdsp->comp.ovpLen);
		FFTDynamicRangeSquasherProcessSamples(&jdsp->comp, jdsp->tmpBuffer[0] + offset, jdsp->tmpBuffer[1] + offset, processing, jdsp->tmpBuffer[0] + offset, jdsp->tmpBuffer[1] + offset);
		offset += processing;
	}
}