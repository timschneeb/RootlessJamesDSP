#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <float.h>
#include "../../ns-eel.h"
#include "../quadprog.h"
#include "fdesign.h"
#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif
#ifndef M_2PI
#define M_2PI 6.28318530717958647692
#endif
void InitEquationErrorIIR(EquationErrorIIR *iir, int32_t num, int32_t denom, int32_t reqLen)
{
	iir->num = num;
	iir->denom = denom;
	iir->reqLen = reqLen;
	iir->A0xLen = num + denom + 1;
	iir->twoReqLen = reqLen << 1;
	int32_t length = ((reqLen * iir->A0xLen) << 1) + (reqLen << 7) + iir->twoReqLen + iir->A0xLen + iir->twoReqLen * iir->A0xLen + iir->A0xLen;
	iir->memSize = (length + (num + 1) + (denom + 1)) * sizeof(double);
	iir->memoryBuffer = (double*)malloc(iir->memSize);
	memset(iir->memoryBuffer, 0, iir->memSize);
	iir->b = iir->memoryBuffer + length;
	iir->a = iir->b + (num + 1);
}
void EquationErrorIIRFree(EquationErrorIIR *iir)
{
	free(iir->memoryBuffer);
}
void eqnerror(EquationErrorIIR *iir, double *om, double *DReal, double *DImag, double *W, int32_t iter)
{
	if (iter < 1)
		iter = 1;
	int32_t i, j;
	int32_t num = iir->num, denom = iir->denom, reqLen = iir->reqLen, A0xLen = iir->A0xLen, twoReqLen = iir->twoReqLen;
	memset(iir->memoryBuffer, 0, iir->memSize);
	double *A0Re = iir->memoryBuffer;
	double *A0Im = A0Re + (reqLen * A0xLen);
	double *W0 = A0Im + (reqLen * A0xLen);
	memcpy(W0, W, reqLen * sizeof(double));
	double *D0Re = W0 + reqLen;
	double *D0Im = D0Re + reqLen;
	double *pvRe = D0Im + reqLen;
	double *pvIm = pvRe + reqLen;
	double *sRe = pvIm + reqLen;
	double *sIm = sRe + reqLen;
	for (i = 0; i < reqLen; i++)
	{
		sRe[i] = cos(om[i]);
		sIm[i] = sin(om[i]);
		D0Re[i] = DReal[i];
		D0Im[i] = DImag[i];
	}
	double *den = sIm + reqLen;
	double *vectorWork = den + reqLen;
	double *xVec = vectorWork + twoReqLen;
	double *xMat = xVec + A0xLen;
	double *x = xMat + iir->twoReqLen * iir->A0xLen;
	double *b = iir->b;
	double *a = iir->a;
	a[0] = 1.0;
	for (i = 0; i < reqLen; i++)
	{
		den[i] = 1.0;
		for (j = 0; j < denom; j++)
		{
			double real = cos(om[i] * (double)(j + 1));
			double imag = -sin(om[i] * (double)(j + 1));
			A0Re[i * A0xLen + j] = -(DReal[i] * real - DImag[i] * imag);
			A0Im[i * A0xLen + j] = -(DReal[i] * imag + DImag[i] * real);
		}
		for (j = 0; j < num + 1; j++)
		{
			A0Re[i * A0xLen + denom + j] = cos(om[i] * (double)j);
			A0Im[i * A0xLen + denom + j] = -sin(om[i] * (double)j);
		}
	}
	for (int32_t it = 0; it < iter; it++)
	{
		for (i = 0; i < reqLen; i++)
		{
			W0[i] /= den[i];
			for (j = 0; j < A0xLen; j++)
			{
				xMat[i * A0xLen + j] = A0Re[i * A0xLen + j] * W0[i];
				xMat[(i + reqLen) * A0xLen + j] = A0Im[i * A0xLen + j] * W0[i];
			}
			vectorWork[i] = D0Re[i] * W0[i];
			vectorWork[i + reqLen] = D0Im[i] * W0[i];
		}
		int32_t matSize[2];
		mldivide(xMat, iir->twoReqLen, A0xLen, vectorWork, iir->twoReqLen, 1, x, matSize);
		for (i = 0; i < A0xLen; i++)
			xVec[i] = x[i];
		for (i = denom; i < A0xLen; i++)
			b[i - denom] = xVec[i];
		for (i = 1; i < denom + 1; i++)
			a[i] = xVec[i - 1];
		memset(pvIm, 0, reqLen * sizeof(double));
		for (i = 0; i < reqLen; i++)
			pvRe[i] = 1.0;
		for (i = 0; i < denom; i++)
			for (j = 0; j < reqLen; j++)
			{
				double s_im = sRe[j] * pvIm[j] + sIm[j] * pvRe[j];
				pvRe[j] = sRe[j] * pvRe[j] - sIm[j] * pvIm[j] + a[i + 1];
				pvIm[j] = s_im;
			}
		for (i = 0; i < reqLen; i++)
		{
			double omexpdenomRe = cos(om[i] * (double)denom);
			double imag = sin(om[i] * (double)denom);
			double real = (pvRe[i] * omexpdenomRe + pvIm[i] * imag) / (omexpdenomRe * omexpdenomRe + imag * imag);
			imag = (pvIm[i] * omexpdenomRe - pvRe[i] * imag) / (omexpdenomRe * omexpdenomRe + imag * imag);
			den[i] = hypot(real, imag);
		}
	}
}
void diff(double *y, double *f, int32_t sz)
{
	--sz;
	for (int32_t i = 0; i < sz; i++)
		f[i] = y[i + 1] - y[i];
}
int32_t isneg(double *y, int32_t sz)
{
	for (int32_t i = 0; i < sz; i++)
		if (y[i] < 0) return 1;
	return 0;
}
// Design arbitrary response IIR filter
// Function interpolate user provided frequency and gain vector into larger grid
// function perform minimum phase transform to interpolated grid
// arbitrary response filter designing algorithm will be applied
// int32_t gridLen: Interpolation grid points
// double *ff: Frequency vector [0 ... 1]
// double *aa: Gain vector in dB
// int32_t gVLen: Frequency or Gain vector length, length(ff) must == length(aa), unexpected error otherwise
// double *b: Returning array that contain numerator
// int32_t M: Numerator orders, recommended range [2 - 23]
// double *a: Returning array that contain denominator 
// int32_t N: Denominator orders, recommended range [2 - 23]
// int32_t iterEqnErr: Minimization iteration counts, recommended range[1 - 2]
// Returning error code: All error code smaller than 1 mean error detected, no valuable filter have been generated
int32_t designMinimumPhaseArbIIR(int32_t gridLen, double *ff, double *aa, int32_t gVLen, double *b, int32_t M, double *a, int32_t N, int32_t iterEqnErr)
{
	int32_t npt, lap, npt2;
	// Convert gain to linear scale
	for (lap = 0; lap < gVLen; lap++)
		aa[lap] = pow(10.0, aa[lap] / 20.0);
	if (gridLen < 1024)
		npt = 512;
	else
		npt = (int32_t)pow(2.0, ceil(log((double)gridLen) / log(2.0)));
	lap = (int32_t)(npt / 25);
	if (ff[0] != 0.0 || ff[gVLen - 1] != 1.0)
	{
		char *msg = "The first frequency must be 0 and the last 1";
		EEL_STRING_STDOUT_WRITE(msg, strlen(msg));
		return 0;
	}
	// Interpolate breakpoints onto large grid
	int32_t nint = gVLen - 1;
	double *df = (double*)malloc(sizeof(double)*(gVLen - 1));
	diff(ff, df, gVLen);
	if (isneg(df, gVLen - 1))
	{
		char *msg = "Frequencies must be non-decreasing";
		EEL_STRING_STDOUT_WRITE(msg, strlen(msg));
		return -1;
	}
	npt = npt; // Length of [dc 1 2 ... nyquist] frequencies.
	npt2 = npt * 2;
	int32_t nb = 0;
	int32_t ne = 0;
	double inc;
	double *H = (double*)malloc(sizeof(double)*npt);
	H[0] = aa[0];
	int32_t i;
	for (i = 0; i < nint; i++)
	{
		if (df[i] == 0)
		{
			nb = nb - lap / 2;
			ne = nb + lap;
		}
		else
			ne = (int32_t)(ff[i + 1] * npt) - 1;
		if (nb < 0 || ne > npt)
		{
			char *msg = "Too abrupt an amplitude change near end of frequency interval";
			EEL_STRING_STDOUT_WRITE(msg, strlen(msg));
			return -2;
		}
		int32_t j;
		for (j = nb; j <= ne; j++)
		{
			if (nb == ne)
				inc = 0;
			else
				inc = (double)(j - nb) / (double)(ne - nb);
			H[j] = inc * aa[i + 1] + (1.0 - inc)*aa[i];
		}
		nb = ne + 1;
	}
	free(df);
	const double threshdB = -140.0;
	const double threshold = pow(10.0, threshdB / 20.0);
	double logThreshold = log(threshold);
	uint32_t *mBitRev = (uint32_t*)malloc(npt2 * sizeof(uint32_t));
	double *mSineTab = (double*)malloc(npt2 * sizeof(double));
	double *timeData = (double*)malloc(npt2 * sizeof(double));
	double *freqData = (double*)malloc(npt2 * sizeof(double));
	LLbitReversalTbl(mBitRev, npt2);
	LLsinHalfTbl(mSineTab, npt2);
	for (i = 0; i < npt; i++)
	{
		double gain = H[i];
		// Part of minimum phase spectrum --- cepstrum calculation
		if (gain < threshold)
			gain = logThreshold;
		else
			gain = log(gain);
		freqData[mBitRev[i]] = gain;
		freqData[mBitRev[npt2 - 1 - i]] = gain;
	}
	free(H);
	discreteHartleyTransform(freqData, npt2, mSineTab);
	double gain = 1.0 / ((double)npt2);
	timeData[0] = freqData[0] * gain;
	timeData[mBitRev[npt]] = freqData[npt] * gain;
	for (i = 1; i < npt; i++)
	{
		timeData[mBitRev[i]] = (freqData[i] + freqData[npt2 - i]) * gain;
		timeData[mBitRev[npt2 - i]] = 0.0f;
	}
	discreteHartleyTransform(timeData, npt2, mSineTab);
	int32_t gridLength = npt + 1;
	double *om = (double*)malloc(gridLength * sizeof(double));
	double *DRe = (double*)malloc(gridLength * sizeof(double));
	double *DIm = (double*)malloc(gridLength * sizeof(double));
	double *W = (double*)malloc(gridLength * sizeof(double));
	DRe[0] = exp(timeData[0]);
	DIm[0] = 0.0;
	om[0] = 0.0;
	W[0] = 1.0;
	for (i = 1; i < npt + 1; i++)
	{
		om[i] = ((double)i * (1.0 / (double)npt)) * M_PI;
		W[i] = 1.0;
		double re = (timeData[i] + timeData[npt2 - i]) * 0.5;
		double im = (timeData[i] - timeData[npt2 - i]) * 0.5;
		double eR = exp(re);
		DRe[i] = eR * cos(im);
		DIm[i] = -eR * sin(im);
	}
	EquationErrorIIR initSolution;
	InitEquationErrorIIR(&initSolution, M, N, gridLength);
	eqnerror(&initSolution, om, DRe, DIm, W, iterEqnErr);
	memcpy(b, initSolution.b, (M + 1) * sizeof(double));
	memcpy(a, initSolution.a, (N + 1) * sizeof(double));
	EquationErrorIIRFree(&initSolution);
	free(mBitRev);
	free(mSineTab);
	free(timeData);
	free(freqData);
	free(om);
	free(DRe);
	free(DIm);
	free(W);
	return 1;
}