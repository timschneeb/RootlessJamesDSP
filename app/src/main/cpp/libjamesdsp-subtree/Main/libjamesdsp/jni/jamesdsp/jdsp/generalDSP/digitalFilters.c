#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <float.h>
#include "digitalFilters.h"
void myButter(DF1P *iir, double fc, double q)
{
	double Gcsq = q * q;
	double alph = tan((M_PI*fc) / 2.0) * sqrt(Gcsq) / sqrt(1.0 - Gcsq);
	iir->b = alph / (1.0 + alph);
	iir->bp = 1.0 - iir->b;
	iir->a = -(1.0 - alph) / (1.0 + alph);
}
double processLPF(DF1P *iir, double x)
{
	double out = x * iir->b + iir->zLPF;
	iir->zLPF = x * iir->b - iir->a * out;
	return out;
}
double processHPF(DF1P *iir, double x)
{
	double out = x * iir->bp + iir->zHPF;
	iir->zHPF = x * -iir->bp - iir->a * out;
	return out;
}
void processCrossover(DF1P *iir, double x, double *lowOut, double *highOut)
{
	double tmp = x * iir->b;
	*lowOut = tmp + iir->zLPF;
	iir->zLPF = tmp - iir->a * (*lowOut);
	*highOut = x * iir->bp + iir->zHPF;
	iir->zHPF = x * -iir->bp - iir->a * (*highOut);
}
void init10BandsCrossover(TenBandsCrossover *lr4, double fs, double lowBandHz, double midBand1Hz, double midBand2Hz, double midBand3Hz, double midBand4Hz, double midBand5Hz, double midBand6Hz, double midBand7Hz, double midBand8Hz)
{
	double q = 1.0 / sqrt(2.0);
	myButter(&lr4->sys[0], lowBandHz / fs * 2.0, q);
	myButter(&lr4->sys[1], midBand1Hz / fs * 2.0, q);
	myButter(&lr4->sys[2], midBand2Hz / fs * 2.0, q);
	myButter(&lr4->sys[3], midBand3Hz / fs * 2.0, q);
	myButter(&lr4->sys[4], midBand4Hz / fs * 2.0, q);
	myButter(&lr4->sys[5], midBand5Hz / fs * 2.0, q);
	myButter(&lr4->sys[6], midBand6Hz / fs * 2.0, q);
	myButter(&lr4->sys[7], midBand7Hz / fs * 2.0, q);
	myButter(&lr4->sys[8], midBand8Hz / fs * 2.0, q);
}
void process10BandsCrossover(TenBandsCrossover *lr4, double x, double *lowOut, double *midOut1, double *midOut2, double *midOut3, double *midOut4, double *midOut5, double *midOut6, double *midOut7, double *midOut8, double *highOut)
{
	double tmp;
	processCrossover(&lr4->sys[0], x, lowOut, &tmp);
	processCrossover(&lr4->sys[1], tmp, midOut1, &tmp);
	processCrossover(&lr4->sys[2], tmp, midOut2, &tmp);
	processCrossover(&lr4->sys[3], tmp, midOut3, &tmp);
	processCrossover(&lr4->sys[4], tmp, midOut4, &tmp);
	processCrossover(&lr4->sys[5], tmp, midOut5, &tmp);
	processCrossover(&lr4->sys[6], tmp, midOut6, &tmp);
	processCrossover(&lr4->sys[7], tmp, midOut7, &tmp);
	processCrossover(&lr4->sys[8], tmp, midOut8, highOut);
}
double iirSOSProcessorProcessBiquadSampleBySample(iirSOS *state, const float *input, const LPFCoeffs *coeffs)
{
	double *yn = &state->_yn[0];
	double *yn1 = &state->_yn1[0];
	double *yn2 = &state->_yn2[0];
	double xn = (double)*input;
	yn[0] = xn + 2.0 * state->_xn1 + state->_xn2 + coeffs[0].a1 * yn1[0] + coeffs[0].a2 * yn2[0];
	for (int i = 1; i < STAGE; i++)
		yn[i] = yn[i - 1] + 2.0 * yn1[i - 1] + yn2[i - 1] + coeffs[i].a1 * yn1[i] + coeffs[i].a2 * yn2[i];
	for (int i = 0; i < STAGE; i++)
	{
		yn2[i] = yn1[i];
		yn1[i] = yn[i];
	}
	state->_xn2 = state->_xn1;
	state->_xn1 = xn;
	return yn[M1STAGE];
}
double iirSOSProcessorProcessBiquadSampleBySampleD(iirSOS *state, const double *input, const LPFCoeffs *coeffs)
{
	double *yn = &state->_yn[0];
	double *yn1 = &state->_yn1[0];
	double *yn2 = &state->_yn2[0];
	double xn = (double)*input;
	yn[0] = xn + 2.0 * state->_xn1 + state->_xn2 + coeffs[0].a1 * yn1[0] + coeffs[0].a2 * yn2[0];
	for (int i = 1; i < STAGE; i++)
		yn[i] = yn[i - 1] + 2.0 * yn1[i - 1] + yn2[i - 1] + coeffs[i].a1 * yn1[i] + coeffs[i].a2 * yn2[i];
	for (int i = 0; i < STAGE; i++)
	{
		yn2[i] = yn1[i];
		yn1[i] = yn[i];
	}
	state->_xn2 = state->_xn1;
	state->_xn1 = xn;
	return yn[M1STAGE];
}
double iirSOSProcessorProcessBiquadSampleBySampleInterpolation(iirSOS *state, const LPFCoeffs *coeffs)
{
	double *yn = &state->_yn[0];
	double *yn1 = &state->_yn1[0];
	double *yn2 = &state->_yn2[0];
	yn[0] = 2.0 * state->_xn1 + state->_xn2 + coeffs[0].a1 * yn1[0] + coeffs[0].a2 * yn2[0];
	for (int i = 1; i < STAGE; i++)
		yn[i] = yn[i - 1] + 2.0 * yn1[i - 1] + yn2[i - 1] + coeffs[i].a1 * yn1[i] + coeffs[i].a2 * yn2[i];
	for (int i = 0; i < STAGE; i++)
	{
		yn2[i] = yn1[i];
		yn1[i] = yn[i];
	}
	state->_xn2 = state->_xn1;
	state->_xn1 = 0.0;
	return yn[M1STAGE];
}
typedef struct
{
	double re, im;
} jcomplex;
inline void complexMultiplicationj(jcomplex *x, jcomplex *h, jcomplex *y)
{
	double imag = x->re * h->im + x->im * h->re;
	y->re = x->re * h->re - x->im * h->im;
	y->im = imag;
}
inline void complexDivisionj(jcomplex *x, jcomplex *h, jcomplex *y)
{
	double c = h->re;
	double d = h->im;
	double zreal2 = c * c;
	double zimag2 = d * d;
	double imag = (x->im*c - x->re*d) / (zreal2 + zimag2);
	y->re = (x->re*c + x->im*d) / (zreal2 + zimag2);
	y->im = imag;
}
double blt(jcomplex *sz)
{
	jcomplex s, h;
	double sRe = sz->re;
	double sIm = sz->im;
	double real = 2.0 - sRe;
	double imag = -sIm;
	s.re = 2.0 + sRe;
	s.im = sIm;
	h.re = real;
	h.im = imag;
	complexDivisionj(&s, &h, sz);
	return sqrt(real * real + imag * imag);
}
#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif
void ButterworthCalcCoefficients(const double fs, const double freq1_cutoff, float *overallGain, LPFCoeffs coeffs[STAGE])
{
	jcomplex polesVec[ORDERMULT2];
	double Wc = 2 * tan(M_PI * freq1_cutoff / fs);
	int numPoles = (int)(((double)(ORDER + 1) / 2.0)) * 2;
	int index = 0;
	for (int k = 0; k < (ORDER + 1) / 2; k++)
	{
		double theta = (double)(2 * k + 1) * M_PI / (2 * ORDER);
		double real = -sin(theta);
		double imag = cos(theta);
		polesVec[index + k].re = real;
		polesVec[index + k].im = imag;
		polesVec[index + k + 1].re = real;
		polesVec[index + k + 1].im = -imag;
		index++;
	}
	jcomplex tmp = { Wc, 0.0 };
	for (int i = 0; i < numPoles; i++)
		complexMultiplicationj(&tmp, &polesVec[i], &polesVec[i]);
	double gain = pow(Wc, numPoles);
	double ba[ORDERPLUS1];
	double preBLTgain = gain;
	for (int i = 0; i < numPoles; i++)
		gain *= blt(&polesVec[i]);
	*overallGain = (float)(preBLTgain * (preBLTgain / gain));
	int numSOS = 0;
	for (int i = 0; i + 1 < numPoles; i += 2, numSOS++)
	{
		jcomplex tmp;
		ba[2 * numSOS] = -(polesVec[i].re + polesVec[i + 1].re);
		complexMultiplicationj(&polesVec[i], &polesVec[i + 1], &tmp);
		ba[2 * numSOS + 1] = tmp.re;
	}
	if (numPoles % 2 == 1)
		ba[2 * numSOS] = -polesVec[numPoles - 1].re;
	for (int i = 0; i < STAGE; i++)
	{
		coeffs[i].a1 = -ba[2 * i];
		coeffs[i].a2 = -ba[2 * i + 1];
	}
}
#ifndef M_2PI
#define M_2PI (M_PI * 2.0)
#endif
// oversample
void oversample_makeSmp(samplerateTool *oversample, int factor)
{
	oversample->factor = factor;
	ButterworthCalcCoefficients(2 * oversample->factor, 0.89, &oversample->gain, oversample->coeffs); // 0.89 Antialiasing
	memset(&oversample->lpfU, 0, sizeof(iirSOS));
	memset(&oversample->lpfD, 0, sizeof(iirSOS));
}
// output length must be oversample->factor
void oversample_stepupSmp(samplerateTool *oversample, float input, float *output)
{
	input *= oversample->factor;
	output[0] = (float)iirSOSProcessorProcessBiquadSampleBySample(&oversample->lpfU, &input, oversample->coeffs) * oversample->gain;
	for (int i = 1; i < oversample->factor; i++)
		output[i] = (float)iirSOSProcessorProcessBiquadSampleBySampleInterpolation(&oversample->lpfU, oversample->coeffs) * oversample->gain;
}
// input length must be oversample->factor
double oversample_stepdownSmpDouble(samplerateTool *oversample, double *input)
{
	double out;
	for (int i = 0; i < oversample->factor; i++)
		out = iirSOSProcessorProcessBiquadSampleBySampleD(&oversample->lpfD, &input[i], oversample->coeffs) * oversample->gain;
	return out;
}
// input length must be oversample->factor
float oversample_stepdownSmpFloat(samplerateTool *oversample, float *input)
{
	double out;
	for (int i = 0; i < oversample->factor; i++)
		out = iirSOSProcessorProcessBiquadSampleBySample(&oversample->lpfD, &input[i], oversample->coeffs) * oversample->gain;
	return (float)out;
}
