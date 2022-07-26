#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "polyphaseASRC.h"
#include "../codelet.h"
#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif
// n/d rounding up
static unsigned int iceil(unsigned int n, unsigned int d)
{
	return n / d + ((n % d != 0) ? 1ul : 0ul);
}
// Dot product of two vectors
static float dot(float* a, unsigned int a_length, float* history, float* b, unsigned int b_last_index)
{
	float dotprod = 0.0f;
	unsigned int i = 0;
	if (a_length > b_last_index)
	{
		for (; i < (a_length - b_last_index - 1); i++)
			dotprod += a[i] * history[b_last_index + i];
	}
	for (; i < a_length; i++)
		dotprod += a[i] * b[(i + 1UL + b_last_index) - a_length];
	return dotprod;
}
static void dotStereo(float* a, unsigned int a_length, float* history1, float* history2, float* b1, float* b2, unsigned int b_last_index, float *dotprod1, float *dotprod2)
{
	*dotprod1 = 0.0f;
	*dotprod2 = 0.0f;
	unsigned int i = 0;
	if (a_length > b_last_index)
	{
		for (; i < (a_length - b_last_index - 1); i++)
		{
			*dotprod1 += a[i] * history1[b_last_index + i];
			*dotprod2 += a[i] * history2[b_last_index + i];
		}
	}
	for (; i < a_length; i++)
	{
		*dotprod1 += a[i] * b1[(i + 1UL + b_last_index) - a_length];
		*dotprod2 += a[i] * b2[(i + 1UL + b_last_index) - a_length];
	}
}
// Shift b into a
static void src_shiftin(float* a, int a_length, float* b, int b_length)
{
	if (b_length > a_length)
		memcpy(a, &b[b_length - a_length], a_length * sizeof(float));
	else
	{
		for (int i = 0; i < (a_length - b_length); i++)
			a[i] = a[i + b_length];
		for (int i = 0; i < b_length; i++)
			a[i + a_length - b_length] = b[i];
	}
}
static void src_shiftinStereo(float* a1, float* a2, int a_length, float* b1, float* b2, int b_length)
{
	if (b_length > a_length)
	{
		memcpy(a1, &b1[b_length - a_length], a_length * sizeof(float));
		memcpy(a2, &b2[b_length - a_length], a_length * sizeof(float));
	}
	else
	{
		for (int i = 0; i < (a_length - b_length); i++)
		{
			a1[i] = a1[i + b_length];
			a2[i] = a2[i + b_length];
		}
		for (int i = 0; i < b_length; i++)
		{
			a1[i + a_length - b_length] = b1[i];
			a2[i + a_length - b_length] = b2[i];
		}
	}
}
static double sincFx(double x)
{
	if (x == 0.0)
		return 1.0;
	else
		return sin(x * M_PI) / (x * M_PI);
}
#include <float.h>
#define uptopow2_0(v) ((v) - 1)
#define uptopow2_1(v) (uptopow2_0(v) | uptopow2_0(v) >> 1)
#define uptopow2_2(v) (uptopow2_1(v) | uptopow2_1(v) >> 2)
#define uptopow2_3(v) (uptopow2_2(v) | uptopow2_2(v) >> 4)
#define uptopow2_4(v) (uptopow2_3(v) | uptopow2_3(v) >> 8)
#define uptopow2_5(v) (uptopow2_4(v) | uptopow2_4(v) >> 16)

#define uptopow2(v) (uptopow2_5(v) + 1)
extern void fhtbitReversalTbl(unsigned *dst, unsigned int n);
static void LLdiscreteHartley(double *A, const unsigned int nPoints, const double *sinTab)
{
	unsigned int i, j, n, n2, theta_inc, nptDiv2;
	double alpha, beta;
	// FHT - stage 1 and 2 (2 and 4 points)
	for (i = 0; i < nPoints; i += 4)
	{
		const double	x0 = A[i];
		const double	x1 = A[i + 1];
		const double	x2 = A[i + 2];
		const double	x3 = A[i + 3];
		const double	y0 = x0 + x1;
		const double	y1 = x0 - x1;
		const double	y2 = x2 + x3;
		const double	y3 = x2 - x3;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	// FHT - stage 3 (8 points)
	for (i = 0; i < nPoints; i += 8)
	{
		alpha = A[i];
		beta = A[i + 4];
		A[i] = alpha + beta;
		A[i + 4] = alpha - beta;
		alpha = A[i + 2];
		beta = A[i + 6];
		A[i + 2] = alpha + beta;
		A[i + 6] = alpha - beta;
		alpha = A[i + 1];
		const double beta1 = 0.70710678118654752440084436210485*(A[i + 5] + A[i + 7]);
		const double beta2 = 0.70710678118654752440084436210485*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	n = 16;
	n2 = 8;
	theta_inc = nPoints >> 4;
	nptDiv2 = nPoints >> 2;
	while (n <= nPoints)
	{
		for (i = 0; i < nPoints; i += n)
		{
			unsigned int theta = theta_inc;
			const unsigned int n4 = n2 >> 1;
			alpha = A[i];
			beta = A[i + n2];
			A[i] = alpha + beta;
			A[i + n2] = alpha - beta;
			alpha = A[i + n4];
			beta = A[i + n2 + n4];
			A[i + n4] = alpha + beta;
			A[i + n2 + n4] = alpha - beta;
			for (j = 1; j < n4; j++)
			{
				double	sinval = sinTab[theta];
				double	cosval = sinTab[theta + nptDiv2];
				double	alpha1 = A[i + j];
				double	alpha2 = A[i - j + n2];
				double	beta1 = A[i + j + n2] * cosval + A[i - j + n] * sinval;
				double	beta2 = A[i + j + n2] * sinval - A[i - j + n] * cosval;
				theta += theta_inc;
				A[i + j] = alpha1 + beta1;
				A[i + j + n2] = alpha1 - beta1;
				A[i - j + n2] = alpha2 + beta2;
				A[i - j + n] = alpha2 - beta2;
			}
		}
		n <<= 1;
		n2 <<= 1;
		theta_inc >>= 1;
	}
}
static void fhtsinHalfTbl(double *dst, unsigned int n)
{
	const double twopi_over_n = (2.0 * M_PI) / n;
	for (unsigned int i = 0; i < n >> 1; ++i)
		dst[i] = sin(twopi_over_n * i);
}
void psrc_generate(SRCResampler* filter, unsigned int interpolation, unsigned int decimation, int tap, double cutoff_freq, char minphase)
{
	unsigned int num_taps = tap * interpolation;
	unsigned int tapDiv2 = tap / 2;
	double ratio = (double)tapDiv2 / (double)num_taps;
	ratio = cutoff_freq * ratio;
	if (interpolation < decimation)
		ratio *= ((double)interpolation / (double)decimation);
	if (minphase)
	{
		unsigned int m = num_taps - 1;
		filter->taps_per_phase = iceil(num_taps, interpolation); // iceil(a/b)
		unsigned int hLen = interpolation * filter->taps_per_phase;
		double g = 1.0;
		if (interpolation < decimation)
			g = (double)interpolation / (double)decimation;
		unsigned int filterLength = uptopow2(num_taps * 100);
		double gain, re, im, eR;
		const double threshdB = -200.0;
		const double clipThresh = pow(10.0, threshdB / 20.0);
		double logThreshold = log(clipThresh);
		unsigned int *mBitRev = (unsigned int*)malloc(filterLength * sizeof(unsigned int));
		double* mSineTab = (double*)malloc((filterLength >> 1) * sizeof(double));
		double *timeData = (double*)malloc(filterLength * sizeof(double));
		double* freqData = (double*)malloc(filterLength * sizeof(double));
		fhtbitReversalTbl(mBitRev, filterLength);
		fhtsinHalfTbl(mSineTab, filterLength);
		int i, symIdx;
		for (i = 0; i < hLen; i++)
			timeData[mBitRev[i]] = cutoff_freq * g * ((sincFx(2.0*ratio*(i - m / 2.0))) * (0.5 - 0.5 * cos(2.0 * M_PI * (double)i / (double)(num_taps - 1))));
		for (i = hLen; i < filterLength; i++)
			timeData[mBitRev[i]] = 0;
		LLdiscreteHartley(timeData, filterLength, mSineTab);
		if (fabs(timeData[0]) < clipThresh)
			freqData[0] = logThreshold;
		else
			freqData[0] = log(fabs(timeData[0]));
		for (int i = 1; i < filterLength; i++)
		{
			symIdx = filterLength - i;
			double lR = timeData[i] + timeData[symIdx];
			double lI = timeData[i] - timeData[symIdx];
			double mag = hypot(lR, lI) * 0.5;
			double logV;
			if (mag < clipThresh)
				logV = logThreshold;
			else
				logV = log(mag);
			freqData[mBitRev[i]] = logV;
			freqData[mBitRev[symIdx]] = logV;
		}
		LLdiscreteHartley(freqData, filterLength, mSineTab);
		gain = 1.0 / ((double)filterLength);
		/*FILE *fp = fopen("a.txt", "wb");
		for (int i = 0; i < filterLength; i++)
			fprintf(fp, "%1.16lf\n", freqData[i] * gain);
		fclose(fp);*/
		timeData[0] = freqData[0] * gain;
		unsigned int halfLen = filterLength >> 1;
		timeData[mBitRev[halfLen]] = freqData[halfLen] * gain;
		for (i = 1; i < halfLen; i++)
		{
			timeData[mBitRev[i]] = (freqData[i] + freqData[filterLength - i]) * gain;
			timeData[mBitRev[filterLength - i]] = 0.0;
		}
		LLdiscreteHartley(timeData, filterLength, mSineTab);
		freqData[0] = exp(timeData[0]);
		for (i = 1; i < filterLength; i++)
		{
			re = (timeData[i] + timeData[filterLength - i]) * 0.5;
			im = (timeData[i] - timeData[filterLength - i]) * 0.5;
			eR = exp(re);
			re = eR * cos(im);
			im = eR * sin(im);
			freqData[mBitRev[i]] = re + im;
			freqData[mBitRev[filterLength - i]] = re - im;
		}
		LLdiscreteHartley(freqData, filterLength, mSineTab);
		/*FILE *fp = fopen("a.txt", "wb");
		for (int i = 0; i < filterLength; i++)
			fprintf(fp, "%1.16lf\n", freqData[i] * gain);
		fclose(fp);*/
		free(timeData);
		free(mBitRev);
		free(mSineTab);
		filter->num_phases = interpolation;
		unsigned int pfb_size = filter->taps_per_phase * interpolation;
		filter->pfb = (float*)malloc(pfb_size * sizeof(float));
		for (unsigned int phase = 0; phase < interpolation; phase++)
			for (unsigned int tap = 0; tap < filter->taps_per_phase; tap++)
				filter->pfb[phase * filter->taps_per_phase + filter->taps_per_phase - 1 - tap] = freqData[tap * interpolation + phase] * gain;
		free(freqData);
	}
	else
	{
		unsigned int m = num_taps - 1;
		filter->num_phases = interpolation;
		filter->taps_per_phase = iceil(num_taps, interpolation); // iceil(a/b)
		unsigned int pfb_size = filter->taps_per_phase * interpolation;
		filter->pfb = (float*)malloc(pfb_size * sizeof(float));
		double g = 1.0;
		if (interpolation < decimation)
			g = (double)interpolation / (double)decimation;
		/*FILE *fp = fopen("a.txt", "wb");
		for (int idx = 0; idx < interpolation * filter->taps_per_phase; idx++)
			fprintf(fp, "%1.14lf,", ((sincFx(2.0*ratio*(idx - m / 2.0))) * (0.5 - 0.5 * cos(2.0 * M_PI * (double)idx / (double)(num_taps - 1)))) / 4.0);
		fclose(fp);*/
		for (unsigned int phase = 0; phase < interpolation; phase++)
			for (unsigned int tap = 0; tap < filter->taps_per_phase; tap++)
			{
				unsigned int idx = tap * interpolation + phase;
				filter->pfb[phase * filter->taps_per_phase + filter->taps_per_phase - 1 - tap] = (float)(cutoff_freq * g * ((sincFx(2.0*ratio*(idx - m / 2.0))) * (0.5 - 0.5 * cos(2.0 * M_PI * (double)idx / (double)(num_taps - 1)))));
			}
	}
	filter->interpolation = interpolation;
	filter->decimation = decimation;
	filter->phase_index = 0;
	filter->input_deficit = 0;
	filter->history_length = filter->taps_per_phase - 1;
	filter->phase_index_step = filter->decimation % filter->interpolation;
	filter->history = malloc(filter->history_length * sizeof(float));
	memset(filter->history, 0, filter->history_length * sizeof(float));
}
void psrc_clone(SRCResampler* filterDest, SRCResampler* filterSrc)
{
	filterDest->num_phases = filterSrc->num_phases;
	filterDest->taps_per_phase = filterSrc->taps_per_phase;
	filterDest->interpolation = filterSrc->interpolation;
	filterDest->decimation = filterSrc->decimation;
	filterDest->input_deficit = filterSrc->input_deficit;
	filterDest->history_length = filterSrc->history_length;
	filterDest->phase_index = filterSrc->phase_index;
	filterDest->phase_index_step = filterSrc->phase_index_step;
	filterDest->history = (float*)malloc(filterSrc->history_length * sizeof(float));
	memcpy(filterDest->history, filterSrc->history, filterSrc->history_length * sizeof(float));
	filterDest->pfb = (float*)malloc(filterSrc->taps_per_phase * filterSrc->interpolation * sizeof(float));
	memcpy(filterDest->pfb, filterSrc->pfb, filterSrc->taps_per_phase * filterSrc->interpolation * sizeof(float));
}
unsigned int psrc_filt(SRCResampler* filter, float *x, unsigned int count, float *y)
{
	unsigned int phase = filter->phase_index;
	unsigned int i = filter->input_deficit;
	unsigned int output_length = 0;
	while (i < count)
	{
		y[output_length++] = dot(&filter->pfb[phase * filter->taps_per_phase], filter->taps_per_phase, filter->history, x, i);
		i += (phase + filter->decimation) / filter->interpolation;
		phase = (phase + filter->phase_index_step) % filter->interpolation;
	}
	filter->input_deficit = i - count;
	filter->phase_index = phase;
	src_shiftin(filter->history, filter->history_length, x, count);
	return output_length;
}
unsigned int psrc_filt_stereo(SRCResampler *filter[2], float *x1, float *x2, unsigned int count, float *y1, float *y2)
{
	unsigned int phase = filter[0]->phase_index;
	unsigned int i = filter[0]->input_deficit;
	unsigned int output_length = 0;
	while (i < count)
	{
		dotStereo(&filter[0]->pfb[phase * filter[0]->taps_per_phase], filter[0]->taps_per_phase, filter[0]->history, filter[1]->history, x1, x2, i, &y1[output_length], &y2[output_length]);
		output_length++;
		i += (phase + filter[0]->decimation) / filter[0]->interpolation;
		phase = (phase + filter[0]->phase_index_step) % filter[0]->interpolation;
	}
	filter[0]->input_deficit = i - count;
	filter[0]->phase_index = phase;
	src_shiftinStereo(filter[0]->history, filter[1]->history, filter[0]->history_length, x1, x2, count);
	return output_length;
}
void psrc_free(SRCResampler* filter)
{
	free(filter->pfb);
	free(filter->history);
}