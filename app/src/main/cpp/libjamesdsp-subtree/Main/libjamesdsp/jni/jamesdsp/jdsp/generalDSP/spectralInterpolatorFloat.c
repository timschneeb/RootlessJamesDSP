#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <float.h>
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
unsigned int getAuditoryBandLen(const unsigned int xLen, double avgBW)
{
	unsigned int a = 3;
	if (avgBW < FLT_EPSILON)
		return xLen;
	double step = avgBW;
	unsigned int cnt = 0;
	while (a + step - 1.0 < xLen)
	{
		a = a + (unsigned int)ceil(round(step) * 0.5);
		step = step * avgBW;
		cnt = cnt + 1;
	}
	return (cnt > (xLen >> 1)) ? xLen : cnt;
}
void initInterpolationList(unsigned int *indexList, double *levels, double avgBW, unsigned int fcLen, unsigned int xLim)
{
	unsigned int a = 3;
	double step = avgBW;
	levels[0] = (1.0 - 1.0) / (double)xLim;
	levels[1] = (2.0 - 1.0) / (double)xLim;
	for (unsigned int i = 0; i < fcLen; i++)
	{
		unsigned int stepp = (unsigned int)round(step);
		indexList[(i << 1) + 0] = a - 1;
		indexList[(i << 1) + 1] = a + stepp - 1;
		levels[i + 2] = ((double)a + ((double)stepp - 1.0) * 0.5 - 1.0) / (double)xLim;
		a = a + (unsigned int)ceil(((double)stepp) * 0.5);
		step = step * avgBW;
	}
	indexList[(fcLen << 1) + 0] = a - 1;
	indexList[(fcLen << 1) + 1] = xLim;
	levels[2 + fcLen] = ((a + xLim) * 0.5 - 1.0) / (double)xLim;
	levels[2 + fcLen + 1] = (xLim - 1.0) / (double)xLim;
}
size_t EstimateMemorySpectralInterpolator(unsigned int *fcLen, unsigned int arrayLen, double avgBW, unsigned int *smallGridSize)
{
	if (avgBW <= (1.0 + DBL_EPSILON))
	{
		printf("Invalid avgBW\n");
		*fcLen = 0;
		*smallGridSize = 0;
		return 0;
	}
	unsigned int shrinkedLen = getAuditoryBandLen(arrayLen, avgBW);
	if (shrinkedLen == arrayLen)
	{
		*fcLen = 0;
		*smallGridSize = 0;
		return sizeof(unsigned int);
	}
	else
		*fcLen = shrinkedLen;
	unsigned int idxLen = shrinkedLen + 1;
	*smallGridSize = idxLen + 3;
	return sizeof(unsigned int) + sizeof(float) + sizeof(unsigned int) + (idxLen << 1) * sizeof(unsigned int) + idxLen * sizeof(float) + (idxLen + 3) * 2 * sizeof(float);
}
void InitSpectralInterpolator(char *octaveSmooth, unsigned int fcLen, unsigned int arrayLen, double avgBW)
{
	unsigned int i;
	unsigned int idxLen = fcLen + 1;
	unsigned int *indexList = (unsigned int*)malloc((idxLen << 1) * sizeof(unsigned int));
	double *levels = (double*)malloc((idxLen + 3) * sizeof(double));
	float *multiplicationPrecompute = (float*)malloc(idxLen * sizeof(float));
	unsigned int *val = (unsigned int*)(octaveSmooth);
	*val = arrayLen;
	float *val2 = (float*)(octaveSmooth + sizeof(unsigned int));
	*val2 = (float)(1.0 / arrayLen);
	val = (unsigned int*)(octaveSmooth + sizeof(unsigned int) + sizeof(float));
	*val = idxLen;
	initInterpolationList(indexList, levels, avgBW, fcLen, arrayLen);
	for (i = 0; i < idxLen; i++)
		multiplicationPrecompute[i] = (float)(1.0 / (indexList[(i << 1) + 1] - indexList[(i << 1) + 0]));
	memcpy(octaveSmooth + sizeof(unsigned int) + sizeof(float) + sizeof(unsigned int), indexList, (idxLen << 1) * sizeof(unsigned int));
	memcpy(octaveSmooth + sizeof(unsigned int) + sizeof(float) + sizeof(unsigned int) + (idxLen << 1) * sizeof(unsigned int), multiplicationPrecompute, idxLen * sizeof(float));
	float *mLevel_1 = (float*)(octaveSmooth + sizeof(unsigned int) + sizeof(float) + sizeof(unsigned int) + (idxLen << 1) * sizeof(unsigned int) + idxLen * sizeof(float));
	float *mLevel_2 = (float*)(octaveSmooth + sizeof(unsigned int) + sizeof(float) + sizeof(unsigned int) + (idxLen << 1) * sizeof(unsigned int) + idxLen * sizeof(float) + (idxLen + 3) * sizeof(float));
	for (i = 0; i < idxLen + 3; i++)
	{
		mLevel_1[i] = (float)levels[i];
		mLevel_2[i] = (float)(1.0 / (levels[i + 1] - levels[i]));
	}
	free(levels);
	free(multiplicationPrecompute);
	free(indexList);
}
int ShrinkGridSpectralInterpolator(char *octaveSmooth, unsigned int inputSpecLen, float *spectrum, float *virtualSubbands)
{
	unsigned int i;
	unsigned int lpLen = *((unsigned int*)(octaveSmooth + sizeof(unsigned int) + sizeof(float)));
	unsigned int *idxLst = (unsigned int*)(octaveSmooth + sizeof(unsigned int) + sizeof(float) + sizeof(unsigned int));
	float *premult = (float*)(octaveSmooth + sizeof(unsigned int) + sizeof(float) + sizeof(unsigned int) + (lpLen << 1) * sizeof(unsigned int));
	virtualSubbands[0] = spectrum[0];
	virtualSubbands[1] = spectrum[1];
	float sum;
	for (i = 0; i < lpLen; i++)
	{
		sum = 0.0f;
		for (unsigned int j = idxLst[(i << 1) + 0]; j < idxLst[(i << 1) + 1]; j++)
			sum += spectrum[j];
		virtualSubbands[2 + i] = sum * premult[i];
	}
	virtualSubbands[(lpLen + 3) - 1] = virtualSubbands[(lpLen + 3) - 2];
	return 0;
}
int LerpSpectralInterpolator(char *octaveSmooth, unsigned int aryLen, float *x, unsigned int inputSpecLen, float *expandGrid)
{
	unsigned int i;
	unsigned int specLen = *((unsigned int*)(octaveSmooth));
	if (inputSpecLen != specLen)
		return -1;
	float reciprocal = *((float*)(octaveSmooth + sizeof(unsigned int)));
	unsigned int lpLen = *((unsigned int*)(octaveSmooth + sizeof(unsigned int) + sizeof(float)));
	unsigned int mySmGrid = lpLen + 3;
	if (aryLen != (lpLen + 3))
		return -2;
	unsigned int *idxLst = (unsigned int*)(octaveSmooth + sizeof(unsigned int) + sizeof(float) + sizeof(unsigned int));
	float *lv1 = (float*)(octaveSmooth + sizeof(unsigned int) + sizeof(float) + sizeof(unsigned int) + (lpLen << 1) * sizeof(unsigned int) + lpLen * sizeof(float));
	float *lv2 = (float*)(octaveSmooth + sizeof(unsigned int) + sizeof(float) + sizeof(unsigned int) + (lpLen << 1) * sizeof(unsigned int) + lpLen * sizeof(float) + (lpLen + 3) * sizeof(float));
	for (i = 0; i < inputSpecLen; i++)
	{
		float val = i * reciprocal;
		if (val <= lv1[0])
			expandGrid[i] = x[0];
		else if (val >= lv1[lpLen + 3 - 1])
			expandGrid[i] = x[lpLen + 3 - 1];
		else
		{
			size_t j = fast_upper_bound4(lv1, lpLen + 3, &val);
			expandGrid[i] = ((val - lv1[j - 1]) * lv2[j - 1]) * (x[j] - x[j - 1]) + x[j - 1];
		}
	}
	return 0;
}