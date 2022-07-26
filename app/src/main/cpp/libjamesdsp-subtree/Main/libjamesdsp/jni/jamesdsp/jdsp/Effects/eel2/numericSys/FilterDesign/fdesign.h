#ifndef __FDESIGN_H__
#define __FDESIGN_H__
typedef struct
{
	double b1, b2, a1_lp, a2_lp, a1_hp, a2_hp;
	double lp_xm0, lp_xm1, hp_xm0, hp_xm1;
} LinkwitzRileyCrossover;
typedef struct
{
	LinkwitzRileyCrossover sys[3];
} ThreeBandsCrossover;
typedef struct
{
	LinkwitzRileyCrossover sys[5];
} FourBandsCrossover;
typedef struct
{
	LinkwitzRileyCrossover sys[10];
} FiveBandsCrossover;
typedef struct
{
	LinkwitzRileyCrossover sys[15];
} SixBandsCrossover;
typedef struct
{
	LinkwitzRileyCrossover sys[21];
} SevenBandsCrossover;
typedef struct
{
	LinkwitzRileyCrossover sys[28];
} EightBandsCrossover;
extern void LWZRCalculateCoefficients(LinkwitzRileyCrossover *lrCO, double fs, double mCrossoverFreq, char apf);
extern void LWZRClearStateVariable(LinkwitzRileyCrossover *lrCO);
extern void LWZRProcessSample(LinkwitzRileyCrossover *lr2, double in, double *low, double *high);
extern void LWZRProcessSampleAPF(LinkwitzRileyCrossover *lr2, double in, double *y);
extern void init3BandsCrossover(ThreeBandsCrossover *lr3, double fs, double lowBandHz, double highBandHz);
extern void clearState3BandsCrossover(ThreeBandsCrossover *lr4);
extern void process3BandsCrossover(ThreeBandsCrossover *lr3, float x, double *lowOut, double *midOut, double *highOut);
extern void init4BandsCrossover(FourBandsCrossover *lr4, double fs, double lowBandHz, double midBand1Hz, double midBand2Hz);
extern void clearState4BandsCrossover(FourBandsCrossover *lr4);
extern void process4BandsCrossover(FourBandsCrossover *lr4, float x, double *lowOut, double *midOut1, double *midOut2, double *highOut);
extern void init5BandsCrossover(FiveBandsCrossover *lr4, double fs, double lowBandHz, double midBand1Hz, double midBand2Hz, double midBand3Hz);
extern void clearState5BandsCrossover(FiveBandsCrossover *lr4);
extern void process5BandsCrossover(FiveBandsCrossover *lr4, float x, double *lowOut, double *midOut1, double *midOut2, double *midOut3, double *highOut);
extern void init6BandsCrossover(SixBandsCrossover *lr4, double fs, double lowBandHz, double midBand1Hz, double midBand2Hz, double midBand3Hz, double midBand4Hz);
extern void clearState6BandsCrossover(SixBandsCrossover *lr4);
extern void process6BandsCrossover(SixBandsCrossover *lr4, float x, double *lowOut, double *midOut1, double *midOut2, double *midOut3, double *midOut4, double *highOut);
extern void init7BandsCrossover(SevenBandsCrossover *lr4, double fs, double lowBandHz, double midBand1Hz, double midBand2Hz, double midBand3Hz, double midBand4Hz, double midBand5Hz);
extern void clearState7BandsCrossover(SevenBandsCrossover *lr4);
extern void process7BandsCrossover(SevenBandsCrossover *lr4, float x, double *lowOut, double *midOut1, double *midOut2, double *midOut3, double *midOut4, double *midOut5, double *highOut);
extern void init8BandsCrossover(EightBandsCrossover *lr4, double fs, double lowBandHz, double midBand1Hz, double midBand2Hz, double midBand3Hz, double midBand4Hz, double midBand5Hz, double midBand6Hz);
extern void clearState8BandsCrossover(EightBandsCrossover *lr4);
extern void process8BandsCrossover(EightBandsCrossover *lr4, float x, double *lowOut, double *midOut1, double *midOut2, double *midOut3, double *midOut4, double *midOut5, double *midOut6, double *highOut);
extern void unwrap(float *p, int N, double tol);
extern void complexMultiplication(double xReal, double xImag, double yReal, double yImag, double *zReal, double *zImag);
extern void cdivid(const double ar, const double ai, const double br, const double bi, double *cr, double *ci);
extern int32_t cplxpair(double *xRe, double *xIm, uint32_t xLen, double *sortedRe, double *sortedIm);
extern int32_t zp2sos(double *zRe, double *zIm, uint32_t zLen, double *pRe, double *pIm, uint32_t pLen, double *sos);
extern int32_t tf2sos(double *b, uint32_t bLen, double *a, uint32_t aLen, double **sos);
extern double sinc(double x);
extern void cplxlog(double zRe, double zIm, double *yRe, double *yIm);
extern void cplxexp(double re, double im, double *yRe, double *yIm);
extern void cplxexpint(double re, double im, double *yRe, double *yIm);
extern void LLbitReversalTbl(unsigned *dst, uint32_t n);
extern void discreteHartleyTransform(double *A, const int32_t nPoints, const double *sinTab);
extern void LLsinHalfTbl(double *dst, uint32_t n);
typedef struct
{
	int32_t num, denom, reqLen, A0xLen, twoReqLen;
	size_t memSize;
	double *memoryBuffer;
	double *b, *a;
} EquationErrorIIR;
extern void InitEquationErrorIIR(EquationErrorIIR *iir, int32_t num, int32_t denom, int32_t reqLen);
extern void EquationErrorIIRFree(EquationErrorIIR *iir);
extern void eqnerror(EquationErrorIIR *iir, double *om, double *DReal, double *DImag, double *W, int32_t iter);
extern int32_t designMinimumPhaseArbIIR(int32_t gridLen, double *ff, double *aa, int32_t gVLen, double *b, int32_t M, double *a, int32_t N, int32_t iterEqnErr);
extern int32_t firls(int32_t N, double *freq, double *M, double *weight, int32_t gridLen, int32_t filtertype, double *h);
extern void subsamplingCal(unsigned int M, double alpha, double *f_def, unsigned int *Sk);
extern float* allpass_char(double alpha, unsigned int L, unsigned int *CFiltLen);
extern void cos_fib_paraunitary1(unsigned int N, unsigned int m, unsigned int L, double df, double *h_opt);
#define DENORMAL_BUFFER (128)
typedef struct
{
	unsigned int N, m, L, N2;
	float *subbandData, *freqLabel;
	float *channelMatrix, *h;
	float *allpass_delay_chain, *virtualHilbertTransformDelay;
	float *APC_delay_1, *APC_delay_2, *Xk2;
	unsigned int *Sk, *decimationCounter;
	float alpha, postGain;
	// Denormal buster
	float noiseBuffer[DENORMAL_BUFFER];
	unsigned int noiseLoop;
} WarpedPFB;
#include "polyphaseASRC.h"
#include <stdint.h>
typedef struct
{
	uint32_t in, out;
} RingBuffer;
typedef struct
{
	unsigned int calculatedLatencyWholeSystem, ringBufSize;
	SRCResampler polyphaseDecimator;
	SRCResampler polyphaseInterpolator;
	RingBuffer intermediateRing;
} IntegerASRCHandler;
#endif