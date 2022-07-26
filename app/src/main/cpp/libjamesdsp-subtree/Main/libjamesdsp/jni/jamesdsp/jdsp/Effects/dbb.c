#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <float.h>
#include "../jdsp_header.h"
static void fht16(float A[16])
{
	float alpha, beta, beta2, alpha1, alpha2, y1, y2, y3;
	alpha = A[0];
	beta = A[1];
	beta2 = A[2];
	alpha1 = A[3];
	alpha2 = alpha + beta;
	y1 = alpha - beta;
	y2 = beta2 + alpha1;
	y3 = beta2 - alpha1;
	A[0] = alpha2 + y2;
	A[2] = alpha2 - y2;
	A[1] = y1 + y3;
	A[3] = y1 - y3;
	alpha = A[4];
	beta = A[5];
	beta2 = A[6];
	alpha1 = A[7];
	alpha2 = alpha + beta;
	y1 = alpha - beta;
	y2 = beta2 + alpha1;
	y3 = beta2 - alpha1;
	A[4] = alpha2 + y2;
	A[6] = alpha2 - y2;
	A[5] = y1 + y3;
	A[7] = y1 - y3;
	alpha = A[8];
	beta = A[9];
	beta2 = A[10];
	alpha1 = A[11];
	alpha2 = alpha + beta;
	y1 = alpha - beta;
	y2 = beta2 + alpha1;
	y3 = beta2 - alpha1;
	A[8] = alpha2 + y2;
	A[10] = alpha2 - y2;
	A[9] = y1 + y3;
	A[11] = y1 - y3;
	alpha = A[12];
	beta = A[13];
	beta2 = A[14];
	alpha1 = A[15];
	alpha2 = alpha + beta;
	y1 = alpha - beta;
	y2 = beta2 + alpha1;
	y3 = beta2 - alpha1;
	A[12] = alpha2 + y2;
	A[14] = alpha2 - y2;
	A[13] = y1 + y3;
	A[15] = y1 - y3;
	alpha = A[0];
	beta = A[4];
	A[0] = alpha + beta;
	A[4] = alpha - beta;
	alpha = A[2];
	beta = A[6];
	A[2] = alpha + beta;
	A[6] = alpha - beta;
	alpha = A[1];
	beta = 0.707106769f*(A[5] + A[7]);
	beta2 = 0.707106769f*(A[5] - A[7]);
	A[1] = alpha + beta;
	A[5] = alpha - beta;
	alpha = A[3];
	A[3] = alpha + beta2;
	A[7] = alpha - beta2;
	alpha = A[8];
	beta = A[12];
	A[8] = alpha + beta;
	A[12] = alpha - beta;
	alpha = A[10];
	beta = A[14];
	A[10] = alpha + beta;
	A[14] = alpha - beta;
	alpha = A[9];
	beta = 0.707106769f*(A[13] + A[15]);
	beta2 = 0.707106769f*(A[13] - A[15]);
	A[9] = alpha + beta;
	A[13] = alpha - beta;
	alpha = A[11];
	A[11] = alpha + beta2;
	A[15] = alpha - beta2;
	alpha = A[0];
	beta = A[8];
	A[0] = alpha + beta;
	A[8] = alpha - beta;
	alpha = A[4];
	beta = A[12];
	A[4] = alpha + beta;
	A[12] = alpha - beta;
	alpha1 = A[1];
	alpha2 = A[7];
	beta = A[9] * 0.923879504f + A[15] * 0.382683426f;
	beta2 = A[9] * 0.382683426f - A[15] * 0.923879504f;
	A[1] = alpha1 + beta;
	A[9] = alpha1 - beta;
	A[7] = alpha2 + beta2;
	A[15] = alpha2 - beta2;
	alpha1 = A[10] * 0.707106769f;
	alpha2 = A[14] * 0.707106769f;
	beta = alpha1 + alpha2;
	beta2 = alpha1 - alpha2;
	alpha1 = A[2];
	alpha2 = A[6];
	A[2] = alpha1 + beta;
	A[10] = alpha1 - beta;
	A[6] = alpha2 + beta2;
	A[14] = alpha2 - beta2;
	alpha1 = A[3];
	alpha2 = A[5];
	beta = A[11] * 0.382683426f + A[13] * 0.923879504f;
	beta2 = A[11] * 0.923879504f - A[13] * 0.382683426f;
	A[3] = alpha1 + beta;
	A[11] = alpha1 - beta;
	A[5] = alpha2 + beta2;
	A[13] = alpha2 - beta2;
}
void integerDelayLineDoubleP_clear(integerDelayLine *delayLine)
{
	for (unsigned int i = 0; i < delayLine->allocateLen; i++)
		delayLine->inputs[i] = 0;
}
void integerDelayLineInit(integerDelayLine *delayLine, unsigned int allocateLen)
{
	if (allocateLen > 1024)
		allocateLen = 1024;
	delayLine->allocateLen = allocateLen;
	integerDelayLineDoubleP_clear(delayLine);
	delayLine->inPoint = 0;
	delayLine->outPoint = delayLine->allocateLen >> 1;
}
void integerDelayLine_setDelay(integerDelayLine *delayLine, unsigned int lag)
{
	if (lag > delayLine->allocateLen - 1)
		delayLine->outPoint = delayLine->inPoint + 1;
	else
		delayLine->outPoint = delayLine->inPoint - lag;
	while (delayLine->outPoint < 0)
		delayLine->outPoint += delayLine->allocateLen;
}
float integerDelayLineProcess(integerDelayLine *delayLine, float sample)
{
	delayLine->inputs[delayLine->inPoint++] = sample;
	if (delayLine->inPoint == delayLine->allocateLen)
		delayLine->inPoint -= delayLine->allocateLen;
	float lastOutput = delayLine->inputs[delayLine->outPoint++];
	if (delayLine->outPoint >= delayLine->allocateLen)
		delayLine->outPoint -= delayLine->allocateLen;
	return lastOutput;
}
//#define DEBUG_DBB
#ifdef DEBUG_DBB
extern FILE *tele;
extern FILE *tele2;
#endif
enum SVFType {
	SVFLowpass = 0,
	SVFBandpass,
	SVFHighpass,
	SVFUnitGainBandpass,
	SVFPeak,
	SVFNotch,
	SVFAllpass
};
void InitStateVariable2ndOrder(StateVariable2ndOrder *svf)
{
	svf->filterType = SVFPeak;
	svf->gCoeff = 1.0f;
	svf->RCoeff = 1.0f;
	svf->KCoeff = 0.0f;
	svf->precomputeCoeff1 = 0.0f;
	svf->precomputeCoeff2 = 0.0f;
	svf->precomputeCoeff3 = 0.0f;
	svf->precomputeCoeff4 = 0.0f;
	svf->z1_A = svf->z2_A = 0.0f;
}
const double resonanceToQ(const double resonance)
{
	return 1.0 / (2.0 * (1.0 - resonance));
}
void refreshStateVariable2ndOrder(StateVariable2ndOrder *svf, const double fs, const double cutoffFreq, const double Q, const double shelfGain)
{
	const double T = 1.0 / fs;
	const double wa = (2.0 / T) * tan((cutoffFreq * 2.0 * M_PI) * T / 2.0);
	const double gCoeff = wa * T / 2.0;
	const double RCoeff = 1.0 / (2.0 * Q);
	const double KCoeff = shelfGain - 1.0;
	svf->gCoeff = (float)gCoeff;
	svf->RCoeff = (float)RCoeff;
	svf->KCoeff = (float)KCoeff;
	svf->precomputeCoeff1 = (float)(2.0 * RCoeff + gCoeff);
	svf->precomputeCoeff2 = (float)(1.0 / (1.0 + (2.0 * RCoeff * gCoeff) + gCoeff * gCoeff));
	svf->precomputeCoeff3 = (float)(2.0 * RCoeff);
	svf->precomputeCoeff4 = (float)(4.0 * RCoeff);
}
float ProcessStateVariable2ndOrder(StateVariable2ndOrder *svf, const float x)
{
	const float HP = (x - svf->precomputeCoeff1 * svf->z1_A - svf->z2_A) * svf->precomputeCoeff2;
	const float BP = HP * svf->gCoeff + svf->z1_A;
	const float LP = BP * svf->gCoeff + svf->z2_A;
	const float UBP = svf->precomputeCoeff3 * BP;
	const float Peak = x + UBP * svf->KCoeff;
	const float Notch = x - UBP;
	const float AP = x - svf->precomputeCoeff4 * BP;
	svf->z1_A = svf->gCoeff * HP + BP;
	svf->z2_A = svf->gCoeff * BP + LP;
	switch (svf->filterType)
	{
	case SVFLowpass:
		return LP;
		break;
	case SVFBandpass:
		return BP;
		break;
	case SVFHighpass:
		return HP;
		break;
	case SVFUnitGainBandpass:
		return UBP;
		break;
	case SVFPeak:
		return Peak;
		break;
	case SVFNotch:
		return Notch;
		break;
	case SVFAllpass:
		return AP;
		break;
	default:
		return 0.0f;
		break;
	}
}
void ProcessStateVariable2ndOrderStereo(StateVariable2ndOrder *svf[2], const float x1, const float x2, float *y1, float *y2)
{
	const float HPL = (x1 - svf[0]->precomputeCoeff1 * svf[0]->z1_A - svf[0]->z2_A) * svf[0]->precomputeCoeff2;
	const float BPL = HPL * svf[0]->gCoeff + svf[0]->z1_A;
	const float LPL = BPL * svf[0]->gCoeff + svf[0]->z2_A;
	const float UBPL = svf[0]->precomputeCoeff3 * BPL;
	const float PeakL = x1 + UBPL * svf[0]->KCoeff;
	const float NotchL = x1 - UBPL;
	const float APL = x1 - svf[0]->precomputeCoeff4 * BPL;
	svf[0]->z1_A = svf[0]->gCoeff * HPL + BPL;
	svf[0]->z2_A = svf[0]->gCoeff * BPL + LPL;
	const float HPR = (x2 - svf[0]->precomputeCoeff1 * svf[1]->z1_A - svf[1]->z2_A) * svf[0]->precomputeCoeff2;
	const float BPR = HPR * svf[0]->gCoeff + svf[1]->z1_A;
	const float LPR = BPR * svf[0]->gCoeff + svf[1]->z2_A;
	const float UBPR = svf[0]->precomputeCoeff3 * BPR;
	const float PeakR = x2 + UBPR * svf[0]->KCoeff;
	const float NotchR = x2 - UBPR;
	const float APR = x2 - svf[0]->precomputeCoeff4 * BPR;
	svf[1]->z1_A = svf[0]->gCoeff * HPR + BPR;
	svf[1]->z2_A = svf[0]->gCoeff * BPR + LPR;
	switch (svf[0]->filterType)
	{
	case SVFLowpass:
		*y1 = LPL;
		*y2 = LPR;
		break;
	case SVFBandpass:
		*y1 = BPL;
		*y2 = BPR;
		break;
	case SVFHighpass:
		*y1 = HPL;
		*y2 = HPR;
		break;
	case SVFUnitGainBandpass:
		*y1 = UBPL;
		*y2 = UBPR;
		break;
	case SVFPeak:
		*y1 = PeakL;
		*y2 = PeakR;
		break;
	case SVFNotch:
		*y1 = NotchL;
		*y2 = NotchR;
		break;
	case SVFAllpass:
		*y1 = APL;
		*y2 = APR;
		break;
	default:
		*y1 = 0.0f;
		*y2 = 0.0f;
		break;
	}
}
static void DBBParam(DBB *dbb, double fs, float maxG)
{
	double maxDetectionSmoothing = 0.5; // 0.5 ms
	double gainSmoothing = 2.0; // 2.0 ms
	if (maxG < 0.0f)
		maxG = 0.0f;
	double targetFS = 500.0;
	dbb->maxGain = maxG;
	int factor = (int)round(fs / targetFS);
	oversample_makeSmp(&dbb->downsampler, factor);
	double trueTargetFs = fs / factor;
	dbb->fs = fs;
	for (int i = 0; i < 9; i++)
		dbb->freq[i] = (float)((((double)i * (trueTargetFs / (double)16)) + (i * (trueTargetFs / (double)16))) * 0.5f);
	dbb->maxSmoothingFactor = (float)(1.0 - exp(-1.0 / (maxDetectionSmoothing / 1000.0 * dbb->fs)));
	dbb->minusmaxSmoothingFactor = 1.0f - dbb->maxSmoothingFactor;
	dbb->gainSmoothingFactor = (float)(1.0 - exp(-1.0 / (gainSmoothing / 1000.0 * dbb->fs)));
	dbb->minusgainSmoothingFactor = 1.0f - dbb->gainSmoothingFactor;
	integerDelayLineInit(&dbb->dL[0], 1024);
	integerDelayLineInit(&dbb->dL[1], 1024);
	integerDelayLine_setDelay(&dbb->dL[0], dbb->downsampler.factor + gainSmoothing * (dbb->fs / 1000.0));
	integerDelayLine_setDelay(&dbb->dL[1], dbb->downsampler.factor + gainSmoothing * (dbb->fs / 1000.0));
}
static void DBBProcess(DBB *dbb, float *x1, float *x2, float *y1, float *y2, size_t n)
{
	int pos = dbb->downsamplerPos;
	for (size_t framePos = 0; framePos < n; framePos++)
	{
		dbb->originalBuf[pos] = (x1[framePos] + x2[framePos]) * 0.5f;
		pos++;
		if (pos == dbb->downsampler.factor)
		{
			dbb->delayLine[15] = dbb->delayLine[14];
			dbb->delayLine[14] = dbb->delayLine[13];
			dbb->delayLine[13] = dbb->delayLine[12];
			dbb->delayLine[12] = dbb->delayLine[11];
			dbb->delayLine[11] = dbb->delayLine[10];
			dbb->delayLine[10] = dbb->delayLine[9];
			dbb->delayLine[9] = dbb->delayLine[8];
			dbb->delayLine[8] = dbb->delayLine[7];
			dbb->delayLine[7] = dbb->delayLine[6];
			dbb->delayLine[6] = dbb->delayLine[5];
			dbb->delayLine[5] = dbb->delayLine[4];
			dbb->delayLine[4] = dbb->delayLine[3];
			dbb->delayLine[3] = dbb->delayLine[2];
			dbb->delayLine[2] = dbb->delayLine[1];
			dbb->delayLine[1] = dbb->delayLine[0];
			dbb->fftBuf[0] = dbb->delayLine[0] = oversample_stepdownSmpFloat(&dbb->downsampler, dbb->originalBuf);
			dbb->fftBuf[8] = dbb->delayLine[1];
			dbb->fftBuf[4] = dbb->delayLine[2];
			dbb->fftBuf[12] = dbb->delayLine[3];
			dbb->fftBuf[2] = dbb->delayLine[4];
			dbb->fftBuf[10] = dbb->delayLine[5];
			dbb->fftBuf[6] = dbb->delayLine[6];
			dbb->fftBuf[14] = dbb->delayLine[7];
			dbb->fftBuf[1] = dbb->delayLine[8];
			dbb->fftBuf[9] = dbb->delayLine[9];
			dbb->fftBuf[5] = dbb->delayLine[10];
			dbb->fftBuf[13] = dbb->delayLine[11];
			dbb->fftBuf[3] = dbb->delayLine[12];
			dbb->fftBuf[11] = dbb->delayLine[13];
			dbb->fftBuf[7] = dbb->delayLine[14];
			dbb->fftBuf[15] = dbb->delayLine[15];
			fht16(dbb->fftBuf);
			float peak1 = dbb->smoothFFTBuffer[0] = fabsf(dbb->fftBuf[0]);
			float peak2 = fabsf(dbb->fftBuf[0]);
			float currentMaxFreq = dbb->freq[0];
			float currentMinFreq = dbb->freq[0];
			float maxdB, mindB;
			mindB = maxdB = 20.0f * log10f(peak1 + FLT_EPSILON);
#ifdef DEBUG_DBB
			fprintf(tele2, "%1.7f,", peak1);
			for (int i = 1; i < 8; i++)
			{
				int symIdx = 16 - i;
				float lR = (dbb->fftBuf[i] + dbb->fftBuf[symIdx]) * 0.5f;
				float lI = (dbb->fftBuf[i] - dbb->fftBuf[symIdx]) * 0.5f;
				float magnitude = sqrtf(lR * lR + lI * lI);
				fprintf(tele2, "%1.7f,", magnitude);
			}
			int symIdx = 16 - 8;
			float lR = (dbb->fftBuf[8] + dbb->fftBuf[symIdx]) * 0.5f;
			float lI = (dbb->fftBuf[8] - dbb->fftBuf[symIdx]) * 0.5f;
			float magnitude = sqrtf(lR * lR + lI * lI);
			fprintf(tele2, "%1.7f\n", magnitude);
			int binNum = 0;
#endif
			for (int i = 1; i < 9; i++)
			{
				int symIdx = 16 - i;
				float lR = (dbb->fftBuf[i] + dbb->fftBuf[symIdx]) * 0.5f;
				float lI = (dbb->fftBuf[i] - dbb->fftBuf[symIdx]) * 0.5f;
				float magnitude = sqrtf(lR * lR + lI * lI);
				dbb->smoothFFTBuffer[i] = magnitude;
				if (dbb->smoothFFTBuffer[i] > peak1)
				{
					peak1 = dbb->smoothFFTBuffer[i];
					currentMaxFreq = dbb->freq[i];
					maxdB = 20.0f * log10f(dbb->smoothFFTBuffer[i] + FLT_EPSILON);
#ifdef DEBUG_DBB
					binNum = i;
#endif
				}
				if (dbb->smoothFFTBuffer[i] < peak2)
				{
					peak2 = dbb->smoothFFTBuffer[i];
					currentMinFreq = dbb->freq[i];
					mindB = 20.0f * log10f(dbb->smoothFFTBuffer[i] + FLT_EPSILON);
				}
			}
			float gainClamp = maxdB - mindB;
			if (gainClamp > dbb->maxGain)
				gainClamp = dbb->maxGain;
			dbb->boostdB = gainClamp * dbb->gainSmoothingFactor + dbb->boostdB * dbb->minusgainSmoothingFactor;
			dbb->smoothMaxFreq = currentMaxFreq * dbb->maxSmoothingFactor + dbb->smoothMaxFreq * dbb->minusmaxSmoothingFactor;
			refreshStateVariable2ndOrder(&dbb->svf[0], dbb->fs, dbb->smoothMaxFreq, resonanceToQ(0.75), db2mag(dbb->boostdB));
#ifdef DEBUG_DBB
			fprintf(tele, "gain: %1.7f fc: %1.7f binNum: %d\n", dbb->boostdB, dbb->smoothMaxFreq, binNum + 1);
#endif
			pos = 0;
		}
	}
	dbb->downsamplerPos = pos;
	const StateVariable2ndOrder *ptr[2] = { &dbb->svf[0], &dbb->svf[1] };
	for (size_t i = 0; i < n; i++)
		ProcessStateVariable2ndOrderStereo(ptr, integerDelayLineProcess(&dbb->dL[0], x1[i]), integerDelayLineProcess(&dbb->dL[1], x2[i]), &y1[i], &y2[i]);
}
// Bass boost
void BassBoostEnable(JamesDSPLib *jdsp)
{
	jdsp->bassBoostEnabled = 1;
}
void BassBoostDisable(JamesDSPLib *jdsp)
{
	jdsp->bassBoostEnabled = 0;
}
void BassBoostConstructor(JamesDSPLib *jdsp)
{
	InitStateVariable2ndOrder(&jdsp->dbb.svf[0]);
	InitStateVariable2ndOrder(&jdsp->dbb.svf[1]);
}
// BassBoostSetParam(context, dB [0 - 15])
void BassBoostSetParam(JamesDSPLib *jdsp, float maxG)
{
	DBBParam(&jdsp->dbb, jdsp->fs, maxG);
}
void BassBoostProcess(JamesDSPLib *jdsp, size_t n)
{
	DBBProcess(&jdsp->dbb, jdsp->tmpBuffer[0], jdsp->tmpBuffer[1], jdsp->tmpBuffer[0], jdsp->tmpBuffer[1], n);
}