#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <errno.h>
#include "../jdsp_header.h"
#include "ArbFIRGen.h"
#define PI 3.141592653589793
#define PI2 6.283185307179586
int get_floatArb(char *val, float *F)
{
	char *eptr;
	errno = 0;
	float f = strtof(val, &eptr);
	if (eptr != val && errno != ERANGE)
	{
		*F = f;
		return 1;
	}
	return 0;
}
int cmpfuncD(const void *a, const void *b)
{
	if (*(float*)a - *(float*)b < 0)
		return -1;
	if (*(float*)a - *(float*)b > 0)
		return 1;
	return 0;
}
size_t fast_lower_boundEqNode(EqNode **array, size_t n, float x)
{
	size_t low = 0;
	while (n > 0)
	{
		size_t half = n >> 1;
		size_t other_half = n - half;
		size_t probe = low + half;
		size_t other_low = low + other_half;
		float v = array[probe]->freq;
		n = half;
		low = x <= v ? low : other_low;
	}
	return low;
}
float gainAtLogGrid(ArbitraryEq *arbEq, float freq)
{
	float dbGain = 0.0, logLeft, logRightMinusLeft;
	if (!arbEq->nodesCount)
		return dbGain;
	if (arbEq->nodesCount == 1)
		return arbEq->nodes[0]->gain;
	else
	{
		if (freq > arbEq->nodes[arbEq->nodesCount - 1]->freq)
			return arbEq->nodes[arbEq->nodesCount - 1]->gain;
		else
		{
			size_t pos = fast_lower_boundEqNode(arbEq->nodes, arbEq->nodesCount, freq);
			if (pos < 1)
				return arbEq->nodes[0]->gain;
			float tmpfreq = arbEq->nodes[pos - 1]->freq;
			if (tmpfreq < 2.0)
				logLeft = tmpfreq;
			else
				logLeft = logf(tmpfreq);
			tmpfreq = arbEq->nodes[pos]->freq;
			if (tmpfreq < 2.0)
				logRightMinusLeft = tmpfreq - logLeft;
			else
				logRightMinusLeft = logf(tmpfreq) - logLeft;
			float t;
			if (freq < 2.0)
				t = (freq - logLeft) / logRightMinusLeft;
			else
				t = (logf(freq) - logLeft) / logRightMinusLeft;
			if (arbEq->nodes[pos - 1]->gain == arbEq->nodes[pos]->gain)
				dbGain = arbEq->nodes[pos]->gain;
			else
				dbGain = arbEq->nodes[pos - 1]->gain + t * (arbEq->nodes[pos]->gain - arbEq->nodes[pos - 1]->gain);
		}
	}
	return dbGain;
}
#define threshdB -100.0f
float *ArbitraryEqMinimumPhase(ArbitraryEq *arbEq, float fs)
{
	float* timeData = arbEq->timeData;
	float* freqData = arbEq->freqData;
	const float threshold = powf(10.0f, threshdB / 20.0f);
	float logThreshold = logf(threshold);
	// Log grid interpolation
	unsigned int i;
	float gain, re, im, eR;
	gain = powf(10.0f, gainAtLogGrid(arbEq, 0.0f) / 20.0f);
	if (gain < threshold)
		gain = logThreshold;
	else
		gain = logf(gain);
	freqData[0] = gain;
	for (i = 1; i < FILTERLEN + 1; i++)
	{
		float freq = (float)i * fs / (float)MUL2FILTERLEN;
		float dbGain = gainAtLogGrid(arbEq, freq);
		float gain = powf(10.0f, dbGain / 20.0f);
		if (gain < threshold)
			gain = logThreshold;
		else
			gain = logf(gain);
		freqData[arbEq->mBitRev[i]] = gain;
		freqData[arbEq->mBitRev[MUL2FILTERLEN - i]] = gain;
	}
	LLdiscreteHartleyFloat(freqData, MUL2FILTERLEN, arbEq->mSineTab);
	gain = 1.0f / ((float)MUL2FILTERLEN);
	timeData[0] = freqData[0] * gain;
	timeData[arbEq->mBitRev[FILTERLEN]] = freqData[FILTERLEN] * gain;
	for (i = 1; i < FILTERLEN; i++)
	{
		timeData[arbEq->mBitRev[i]] = (freqData[i] + freqData[MUL2FILTERLEN - i]) * gain;
		timeData[arbEq->mBitRev[MUL2FILTERLEN - i]] = 0.0f;
	}
	LLdiscreteHartleyFloat(timeData, MUL2FILTERLEN, arbEq->mSineTab);
	freqData[0] = expf(timeData[0]);
	for (i = 1; i < MUL2FILTERLEN; i++)
	{
		re = (timeData[i] + timeData[MUL2FILTERLEN - i]) * 0.5f;
		im = (timeData[i] - timeData[MUL2FILTERLEN - i]) * 0.5f;
		eR = expf(re);
		re = eR * cosf(im);
		im = eR * sinf(im);
		freqData[arbEq->mBitRev[i]] = re + im;
		freqData[arbEq->mBitRev[MUL2FILTERLEN - i]] = re - im;
	}
	LLdiscreteHartleyFloat(freqData, MUL2FILTERLEN, arbEq->mSineTab);
	float *finalImpulse = arbEq->impulseResponse;
	for (i = 0; i < FILTERLEN; i++)
		finalImpulse[i] = (1.0f / MUL2FILTERLEN) * freqData[i];
	return finalImpulse;
}
float *ArbitraryEqLinearPhase(ArbitraryEq *arbEq, float fs)
{
	float* freqData = arbEq->freqData;
	// Log grid interpolation
	unsigned int i;
	freqData[0] = powf(10.0f, gainAtLogGrid(arbEq, 0.0f) / 20.0f);
	for (i = 1; i < FILTERLEN + 1; i++)
	{
		float freq = (float)i * fs / (float)MUL2FILTERLEN;
		float dbGain = gainAtLogGrid(arbEq, freq);
		float gain = powf(10.0f, dbGain / 20.0f);
		freqData[arbEq->mBitRev[i]] = gain;
		freqData[arbEq->mBitRev[MUL2FILTERLEN - i]] = gain;
	}
	LLdiscreteHartleyFloat(freqData, MUL2FILTERLEN, arbEq->mSineTab);
	float *finalImpulse = arbEq->impulseResponse;
	for (i = 0; i < FILTERLEN; i++)
	{
		float timeIntermediate = freqData[i] * (1.0f / MUL2FILTERLEN);
		finalImpulse[FILTERLENMINUS1 - i] = finalImpulse[FILTERLENMINUS1 + i] = timeIntermediate;
	}
	return finalImpulse;
}
unsigned int InitArbitraryEq(ArbitraryEq* arbEq, int isLinearPhase)
{
	fhtbitReversalTbl(arbEq->mBitRev, MUL2FILTERLEN);
	fhtsinHalfTblFloat(arbEq->mSineTab, MUL2FILTERLEN);
	arbEq->nodesCount = 0;
	if (!isLinearPhase)
	{
		arbEq->GetFilter = &ArbitraryEqMinimumPhase;
		return FILTERLEN;
	}
	else
	{
		arbEq->GetFilter = &ArbitraryEqLinearPhase;
		return MUL2FILTERLEN - 1;
	}
}
void EqNodesFree(ArbitraryEq *arbEq)
{
	for (unsigned int i = 0; i < arbEq->nodesCount; i++)
		free(arbEq->nodes[i]);
	free(arbEq->nodes);
	arbEq->nodes = 0;
	arbEq->nodesCount = 0;
}
void NodesSorter(ArbitraryEq *arbEq)
{
	unsigned int i, numOfNodes = arbEq->nodesCount;
	float *freqArray = (float*)malloc(numOfNodes * sizeof(float));
	for (i = 0; i < numOfNodes; i++)
		freqArray[i] = arbEq->nodes[i]->freq;
	qsort(freqArray, numOfNodes, sizeof(float), cmpfuncD);
	for (unsigned int j = 0; j < numOfNodes; j++)
	{
		for (i = 0; i < numOfNodes; i++)
		{
			if (freqArray[j] == arbEq->nodes[i]->freq)
			{
				float tmpFreq1 = arbEq->nodes[j]->freq;
				float tmpGain1 = arbEq->nodes[j]->gain;
				float tmpFreq2 = arbEq->nodes[i]->freq;
				float tmpGain2 = arbEq->nodes[i]->gain;
				arbEq->nodes[i]->freq = tmpFreq1;
				arbEq->nodes[i]->gain = tmpGain1;
				arbEq->nodes[j]->freq = tmpFreq2;
				arbEq->nodes[j]->gain = tmpGain2;
			}
		}
	}
	free(freqArray);
}
void ArbitraryEqString2SortedNodes(ArbitraryEq *arbEq, char *frArbitraryEqString)
{
	if (!arbEq)
		return;
	unsigned int numOfNodes;
	char *symSt = strchr(frArbitraryEqString, ':');
	if (!symSt)
		symSt = frArbitraryEqString;
	char *p = symSt;
	char *counter = symSt;
	unsigned int i, count = 0;
	float number;
	while (*p)
	{
		if (get_floatArb(p, &number))
		{
			strtod(p, &p);
			count++;
		}
		else
			p++;
	}
	numOfNodes = count / 2;
	EqNode **nodes;
	if (numOfNodes != arbEq->nodesCount)
	{
		if (arbEq->nodes)
			EqNodesFree(arbEq);
		nodes = (EqNode**)malloc(numOfNodes * sizeof(EqNode*));
		for (i = 0; i < numOfNodes; i++)
		{
			nodes[i] = (EqNode*)malloc(sizeof(EqNode));
			memset(nodes[i], 0, sizeof(EqNode));
		}
	}
	else
		nodes = arbEq->nodes;
	i = 0;
	count = 1;
	while (*counter)
	{
		if (get_floatArb(counter, &number))
		{
			count++;
			float val = strtof(counter, &counter);
			if (count % 2)
			{
				nodes[i]->gain = val;
				i++;
			}
			else
			{
				nodes[i]->freq = val;
			}
		}
		else
			counter++;
	}
	arbEq->nodesCount = numOfNodes;
	arbEq->nodes = nodes;
	NodesSorter(arbEq);
}
float *InterpolatingEqMinimumPhase(ArbitraryEq *arbEq, float fs, void *lerper)
{
	ierper *ptr = (ierper*)lerper;
	float* timeData = arbEq->timeData;
	float* freqData = arbEq->freqData;
	const float threshold = powf(10.0f, threshdB / 20.0f);
	float logThreshold = logf(threshold);
	// Log grid interpolation
	unsigned int i;
	float gain, re, im, eR;
	gain = powf(10.0f, (float)getValueAt(&ptr->cb, 0.0) / 20.0f);
	if (gain < threshold)
		gain = logThreshold;
	else
		gain = logf(gain);
	freqData[0] = gain;
	for (i = 1; i < FILTERLEN + 1; i++)
	{
		float freq = (float)i * fs / (float)MUL2FILTERLEN;
		float dbGain = (float)getValueAt(&ptr->cb, freq);
		float gain = powf(10.0f, dbGain / 20.0f);
		if (gain < threshold)
			gain = logThreshold;
		else
			gain = logf(gain);
		freqData[arbEq->mBitRev[i]] = gain;
		freqData[arbEq->mBitRev[MUL2FILTERLEN - i]] = gain;
	}
	LLdiscreteHartleyFloat(freqData, MUL2FILTERLEN, arbEq->mSineTab);
	gain = 1.0f / ((float)MUL2FILTERLEN);
	timeData[0] = freqData[0] * gain;
	timeData[arbEq->mBitRev[FILTERLEN]] = freqData[FILTERLEN] * gain;
	for (i = 1; i < FILTERLEN; i++)
	{
		timeData[arbEq->mBitRev[i]] = (freqData[i] + freqData[MUL2FILTERLEN - i]) * gain;
		timeData[arbEq->mBitRev[MUL2FILTERLEN - i]] = 0.0f;
	}
	LLdiscreteHartleyFloat(timeData, MUL2FILTERLEN, arbEq->mSineTab);
	freqData[0] = expf(timeData[0]);
	for (i = 1; i < MUL2FILTERLEN; i++)
	{
		re = (timeData[i] + timeData[MUL2FILTERLEN - i]) * 0.5f;
		im = (timeData[i] - timeData[MUL2FILTERLEN - i]) * 0.5f;
		eR = expf(re);
		re = eR * cosf(im);
		im = eR * sinf(im);
		freqData[arbEq->mBitRev[i]] = re + im;
		freqData[arbEq->mBitRev[MUL2FILTERLEN - i]] = re - im;
	}
	LLdiscreteHartleyFloat(freqData, MUL2FILTERLEN, arbEq->mSineTab);
	float *finalImpulse = arbEq->impulseResponse;
	for (i = 0; i < FILTERLEN; i++)
		finalImpulse[i] = (1.0f / MUL2FILTERLEN) * freqData[i];
	return finalImpulse;
}
float *InterpolatingEqLinearPhase(ArbitraryEq *arbEq, float fs, void *lerper)
{
	ierper *ptr = (ierper*)lerper;
	float* freqData = arbEq->freqData;
	// Log grid interpolation
	unsigned int i;
	freqData[0] = powf(10.0f, (float)getValueAt(&ptr->cb, 0.0) / 20.0f);
	for (i = 1; i < FILTERLEN + 1; i++)
	{
		float freq = (float)i * fs / (float)MUL2FILTERLEN;
		float dbGain = (float)getValueAt(&ptr->cb, freq);
		float gain = powf(10.0f, dbGain / 20.0f);
		freqData[arbEq->mBitRev[i]] = gain;
		freqData[arbEq->mBitRev[MUL2FILTERLEN - i]] = gain;
	}
	LLdiscreteHartleyFloat(freqData, MUL2FILTERLEN, arbEq->mSineTab);
	float *finalImpulse = arbEq->impulseResponse;
	for (i = 0; i < FILTERLEN; i++)
	{
		float timeIntermediate = freqData[i] * (1.0f / MUL2FILTERLEN);
		finalImpulse[FILTERLENMINUS1 - i] = finalImpulse[FILTERLENMINUS1 + i] = timeIntermediate;
	}
	return finalImpulse;
}
#undef threshdB
