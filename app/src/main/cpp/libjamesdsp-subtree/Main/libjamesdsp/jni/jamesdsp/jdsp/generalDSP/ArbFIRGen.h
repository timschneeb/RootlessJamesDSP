#ifndef ARBFIRGEN_H
#define ARBFIRGEN_H
typedef struct
{
	float freq;
	float gain;
} EqNode;
#define FILTERLEN 8192
#define FILTERLENMINUS1 (FILTERLEN - 1)
#define MUL2FILTERLEN (FILTERLEN << 1)
typedef struct str_ArbitraryEq
{
	EqNode **nodes;
	unsigned int nodesCount;
	float timeData[MUL2FILTERLEN];
	float freqData[MUL2FILTERLEN];
	unsigned int mBitRev[MUL2FILTERLEN];
	float mSineTab[MUL2FILTERLEN];
	float impulseResponse[MUL2FILTERLEN];
	float* (*GetFilter)(struct str_ArbitraryEq*, float);
} ArbitraryEq;
extern void EqNodesFree(ArbitraryEq *arbEq);
extern unsigned int InitArbitraryEq(ArbitraryEq* arbEq, int isLinearPhase);
extern void ArbitraryEqString2SortedNodes(ArbitraryEq *arbEq, char *frArbitraryEqString);
extern float *InterpolatingEqMinimumPhase(ArbitraryEq *arbEq, float fs, void *lerper);
extern float *InterpolatingEqLinearPhase(ArbitraryEq *arbEq, float fs, void *lerper);
#endif