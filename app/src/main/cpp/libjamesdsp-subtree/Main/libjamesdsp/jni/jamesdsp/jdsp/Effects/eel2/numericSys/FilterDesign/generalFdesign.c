#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <float.h>
#include "../quadprog.h"
#include "fdesign.h"
#include "../../ns-eel.h"
#ifndef max
#define max(a,b) (((a) > (b)) ? (a) : (b))
#endif
void LLsinHalfTbl(double *dst, uint32_t n)
{
	const double twopi_over_n = 6.283185307179586476925286766559 / n;
	for (uint32_t i = 0; i < n; ++i)
		dst[i] = sin(twopi_over_n * i);
}
uint32_t LLIntegerLog2(uint32_t v)
{
	uint32_t i = 0;
	while (v > 1)
	{
		++i;
		v >>= 1;
	}
	return i;
}
unsigned LLRevBits(uint32_t x, uint32_t bits)
{
	uint32_t y = 0;
	while (bits--)
	{
		y = (y + y) + (x & 1);
		x >>= 1;
	}
	return y;
}
void LLbitReversalTbl(unsigned *dst, uint32_t n)
{
	uint32_t bits = LLIntegerLog2(n);
	for (uint32_t i = 0; i < n; ++i)
		dst[i] = LLRevBits(i, bits);
}
// zp2sos
void swap(double* a, double* b)
{
	double t = *a;
	*a = *b;
	*b = t;
}
void selectionSort(double arr[], int32_t n)
{
	int32_t i, j, min_idx;
	// One by one move boundary of unsorted subarray  
	for (i = 0; i < n - 1; i++)
	{
		// Find the minimum element in unsorted array  
		min_idx = i;
		for (j = i + 1; j < n; j++)
			if (arr[j] < arr[min_idx])
				min_idx = j;
		// Swap the found minimum element with the first element
		swap(&arr[min_idx], &arr[i]);
	}
}
void selectionSortAux(double arr[], double arr2[], int32_t n)
{
	int32_t i, j, min_idx;

	// One by one move boundary of unsorted subarray  
	for (i = 0; i < n - 1; i++)
	{
		// Find the minimum element in unsorted array  
		min_idx = i;
		for (j = i + 1; j < n; j++)
			if (arr[j] < arr[min_idx])
				min_idx = j;
		// Swap the found minimum element with the first element
		swap(&arr[min_idx], &arr[i]);
		swap(&arr2[min_idx], &arr2[i]);
	}
}
int32_t cplxpair(double *xRe, double *xIm, uint32_t xLen, double *sortedRe, double *sortedIm)
{
	uint32_t i, j;
	double tol = 100.0 * DBL_EPSILON;
	double *xcRe = (double*)malloc(xLen * sizeof(double));
	double *xcIm = (double*)malloc(xLen * sizeof(double));
	memcpy(xcRe, xRe, xLen * sizeof(double));
	memcpy(xcIm, xIm, xLen * sizeof(double));
	uint32_t *aryIdx = (uint32_t*)malloc(xLen * sizeof(uint32_t));
	double *tmp1 = (double*)malloc(xLen * sizeof(double));
	uint32_t index = 0;
	while (1) // Odd number of entries remaining
	{
		for (i = 0; i < xLen; i++)
		{
			if (fabs(xcIm[i]) <= tol * sqrt(xcRe[i] * xcRe[i] + xcIm[i] * xcIm[i]))
			{
				aryIdx[index] = i;
				tmp1[index++] = xcRe[i];
			}
		}
		if ((xLen - index) % 2 != 0)
		{
			index = 0;
			tol *= 10.0;
			continue;
		}
		else
			break;
	}
	index = 0;
	for (i = 0; i < xLen; i++)
	{
		if (fabs(xcIm[i]) <= tol * sqrt(xcRe[i] * xcRe[i] + xcIm[i] * xcIm[i]))
		{
			aryIdx[index] = i;
			tmp1[index++] = xcRe[i];
		}
	}
	selectionSort(tmp1, index);
	for (i = xLen - index; i < xLen; i++)
	{
		sortedRe[i] = tmp1[i - xLen + index];
		sortedIm[i] = 0.0f;
	}
	uint32_t loop = 0;
	for (i = 0; i < index; i++)
	{
		for (uint32_t idx = 0; idx < xLen + loop; idx++)
		{
			if (idx == aryIdx[i])
			{
				for (j = idx - loop; j < xLen - 1; j++)
				{
					xcRe[j] = xcRe[j + 1];
					xcIm[j] = xcIm[j + 1];
				}
				xLen--;
				loop++;
			}
		}
	}
	if (!xLen)
	{
		free(xcRe);
		free(xcIm);
		free(aryIdx);
		free(tmp1);
		return -2;
	}
	if (xLen % 2 != 0) // Odd number of entries remaining
	{
		char *msg = "Complex numbers can't be paired.";
		EEL_STRING_STDOUT_WRITE(msg, strlen(msg));
		free(xcRe);
		free(xcIm);
		free(aryIdx);
		free(tmp1);
		return -1;
	}
	// Sort complex column-vector xc, based on its real part
	selectionSortAux(xcRe, xcIm, xLen);
	// Check real part pairs to see if imag parts are conjugates
	uint32_t nxt_row = 0; // next row in y for results
	double tmp2[2];
	tol = 100.0 * DBL_EPSILON;
	int32_t previousFail = 0;
	while (xLen)
	{
		uint32_t nn = 0;
		for (i = 0; i < xLen; i++)
			if (fabs(xcRe[i] - xcRe[0]) <= tol * sqrt(xcRe[i] * xcRe[i] + xcIm[i] * xcIm[i]))
				aryIdx[nn++] = i;
		if (nn <= 1 || nn > 2)
		{
			if (tol > 1e-5)
				break; // Simply no complex numbers pair
			tol *= 10.0;
			char *msg = "Complex numbers can't be paired, continue with larger tolerance\n";
			EEL_STRING_STDOUT_WRITE(msg, strlen(msg));
			previousFail = 1;
			continue;
		}
		else
		{
			tol = 100.0 * DBL_EPSILON;
			previousFail = 0;
		}
		for (i = 0; i < nn; i++)
		{
			tmp1[i] = xcIm[i];
			tmp2[i] = xcRe[i];
		}
		selectionSortAux(tmp1, tmp2, nn);
		sortedRe[nxt_row] = tmp2[1];
		sortedIm[nxt_row] = -tmp1[1];
		sortedRe[nxt_row + 1] = tmp2[1];
		sortedIm[nxt_row + 1] = tmp1[1];
		nxt_row += nn;
		loop = 0;
		for (i = 0; i < nn; i++)
		{
			for (uint32_t idx = 0; idx < xLen + loop; idx++)
			{
				if (idx == aryIdx[i])
				{
					for (j = idx - loop; j < xLen - 1; j++)
					{
						xcRe[j] = xcRe[j + 1];
						xcIm[j] = xcIm[j + 1];
					}
					xLen--;
					loop++;
				}
			}
		}
	}
	free(xcRe);
	free(xcIm);
	free(aryIdx);
	free(tmp1);
	return 0;
}
int32_t zp2sos(double *zRe, double *zIm, uint32_t zLen, double *pRe, double *pIm, uint32_t pLen, double *sos)
{
	uint32_t i;
	const double thresh = 100 * DBL_EPSILON;
	uint32_t nzc = 0, nzr = 0, npc = 0, npr = 0;
	double *zcpRe = (double*)malloc(zLen * sizeof(double));
	double *zcpIm = (double*)malloc(zLen * sizeof(double));
	uint32_t nzrsec = 0, idx;
	if (zLen)
	{
		cplxpair(zRe, zIm, zLen, zcpRe, zcpIm); // sort complex pairs, real roots at end
		idx = zLen - 1;
		while ((idx + 1) && fabs(zcpIm[idx]) < thresh) // determine no.of real values
		{
			nzrsec = nzrsec + 1;
			idx = idx - 1;
		}
	}
	uint32_t nzsect2 = zLen - nzrsec;
	if (nzsect2 % 2 != 0)
	{
		char *msg = "Odd number of zeros!";
		EEL_STRING_STDOUT_WRITE(msg, strlen(msg));
		free(zcpRe);
		free(zcpIm);
		return -1;
	}
	nzc = nzsect2 >> 1;
	double *zcRe = (double*)malloc(nzc * sizeof(double));
	double *zcIm = (double*)malloc(nzc * sizeof(double));
	idx = 0;
	for (i = 0; i < nzsect2; i++)
	{
		if ((i + 1) % 2 == 0)
		{
			zcRe[idx] = zcpRe[i];
			zcIm[idx++] = zcpIm[i];
		}
	}
	nzr = zLen - nzsect2;
	double *zr = (double*)malloc((nzr + 1) * sizeof(double));
	for (i = nzsect2; i < zLen; i++)
		zr[i - nzsect2] = zcpRe[i];
	free(zcpRe);
	free(zcpIm);
	zcpRe = (double*)malloc(pLen * sizeof(double));
	zcpIm = (double*)malloc(pLen * sizeof(double));
	nzrsec = 0;
	if (pLen)
	{
		cplxpair(pRe, pIm, pLen, zcpRe, zcpIm); // sort complex pairs, real roots at end
		idx = pLen - 1;
		while ((idx + 1) && fabs(zcpIm[idx]) < thresh) // determine no.of real values
		{
			nzrsec = nzrsec + 1;
			idx = idx - 1;
		}
	}
	nzsect2 = pLen - nzrsec;
	if (nzsect2 % 2 != 0)
	{
		char *msg = "Odd number of zeros!";
		EEL_STRING_STDOUT_WRITE(msg, strlen(msg));
		free(zcpRe);
		free(zcpIm);
		free(zcRe);
		free(zcIm);
		free(zr);
		return -1;
	}
	npc = nzsect2 >> 1;
	double *pcRe = (double*)malloc(npc * sizeof(double));
	double *pcIm = (double*)malloc(npc * sizeof(double));
	idx = 0;
	for (i = 0; i < nzsect2; i++)
	{
		if ((i + 1) % 2 == 0)
		{
			pcRe[idx] = zcpRe[i];
			pcIm[idx++] = zcpIm[i];
		}
	}
	npr = pLen - nzsect2;
	double *pr = (double*)malloc((npr + 1) * sizeof(double));
	for (i = nzsect2; i < pLen; i++)
		pr[i - nzsect2] = zcpRe[i];
	free(zcpRe);
	free(zcpIm);

	// Pair up real zeros:
	double *zrms = 0, *zrp = 0;
	if (nzr)
	{
		if (nzr % 2 != 0)
		{
			nzr++;
			zr[nzr - 1] = 0.0f;
		}
		nzrsec = nzr >> 1;
		zrms = (double*)malloc(nzrsec * sizeof(double));
		zrp = (double*)malloc(nzrsec * sizeof(double));
		idx = 0;
		for (i = 0; i < nzr; i++)
		{
			if ((i + 1) % 2 != 0)
			{
				zrms[idx] = -zr[i] - zr[i + 1];
				zrp[idx++] = zr[i] * zr[i + 1];
			}
		}
	}
	else
		nzrsec = 0;

	// Pair up real poles:
	uint32_t nprsec;
	double *prms = 0, *prp = 0;
	if (npr)
	{
		if (npr % 2 != 0)
		{
			npr++;
			pr[npr - 1] = 0.0f;
		}
		nprsec = npr >> 1;
		prms = (double*)malloc(nprsec * sizeof(double));
		prp = (double*)malloc(nprsec * sizeof(double));
		idx = 0;
		for (i = 0; i < npr; i++)
		{
			if ((i + 1) % 2 != 0)
			{
				prms[idx] = -pr[i] - pr[i + 1];
				prp[idx++] = pr[i] * pr[i + 1];
			}
		}
	}
	else
		nprsec = 0;
	uint32_t nzrl = nzc + nzrsec; // index of last real zero section
	uint32_t nprl = npc + nprsec; // index of last real pole section
	uint32_t nsecs = max(nzrl, nprl);
	// Convert complex zeros and poles to real 2nd-order section form:
	for (i = 0; i < nsecs; i++)
	{
		sos[i * 6] = sos[i * 6 + 3] = 1.0;
		if (i < nzc) // lay down a complex zero pair:
		{
			sos[i * 6 + 1] = -2.0 * zcRe[i];
			sos[i * 6 + 2] = zcRe[i] * zcRe[i] + zcIm[i] * zcIm[i];
		}
		else if (i < nzrl) // lay down a pair of real zeros:
		{
			sos[i * 6 + 1] = zrms[i - nzc];
			sos[i * 6 + 2] = zrp[i - nzc];
		}
		if (i < npc) // lay down a complex pole pair:
		{
			sos[i * 6 + 4] = -2.0 * pcRe[i];
			sos[i * 6 + 5] = pcRe[i] * pcRe[i] + pcIm[i] * pcIm[i];
		}
		else if (i < nprl) // lay down a pair of real poles:
		{
			sos[i * 6 + 4] = prms[i - npc];
			sos[i * 6 + 5] = prp[i - npc];
		}
	}
	free(zcRe);
	free(zcIm);
	free(zr);
	free(pcRe);
	free(pcIm);
	free(pr);
	if (zrms)
		free(zrms);
	if (zrp)
		free(zrp);
	if (prms)
		free(prms);
	if (prp)
		free(prp);
	return nsecs;
}
int32_t tf2sos(double *b, uint32_t bLen, double *a, uint32_t aLen, double **sos)
{
	// % Find Poles and Zeros
	if (!aLen)
	{
		char *msg = "Denominator cannot be empty\n";
		EEL_STRING_STDOUT_WRITE(msg, strlen(msg));
		return -1;
	}
	if (a[0] == 0.0f)
	{
		char *msg = "Denominator cannot be zero\n";
		EEL_STRING_STDOUT_WRITE(msg, strlen(msg));
		return -1;
	}
	uint32_t i;
	double *bpolyRe = (double*)malloc(bLen * sizeof(double));
	memcpy(bpolyRe, b, bLen * sizeof(double));
	double *bpolyIm = (double*)malloc(bLen * sizeof(double));
	memset(bpolyIm, 0, bLen * sizeof(double));
	double *zRe = (double*)malloc(bLen * sizeof(double));
	double *zIm = (double*)malloc(bLen * sizeof(double));
	int32_t zeroNumRoots = cpoly(bpolyRe, bpolyIm, bLen - 1, zRe, zIm);
	free(bpolyRe);
	free(bpolyIm);
	if (zeroNumRoots < 0)
		zeroNumRoots = 0;
	double *apolyRe = (double*)malloc(aLen * sizeof(double));
	memcpy(apolyRe, a, aLen * sizeof(double));
	double *apolyIm = (double*)malloc(aLen * sizeof(double));
	memset(apolyIm, 0, aLen * sizeof(double));
	double *pRe = (double*)malloc(aLen * sizeof(double));
	double *pIm = (double*)malloc(aLen * sizeof(double));
	int32_t poleNumRoots = cpoly(apolyRe, apolyIm, aLen - 1, pRe, pIm);
	free(apolyRe);
	free(apolyIm);
	if (poleNumRoots < 0)
		poleNumRoots = 0;
	uint32_t myNSec = (uint32_t)ceil(max(zeroNumRoots, poleNumRoots) * 0.5);
	*(sos) = (double*)malloc(myNSec * 6 * sizeof(double));
	memset(*(sos), 0, myNSec * 6 * sizeof(double));
	double firstNonZero = 0.0f;
	for (i = 0; i < bLen; i++)
	{
		if (b[i] != 0.0f)
		{
			firstNonZero = b[i];
			break;
		}
	}
	double k = firstNonZero / a[0];
	int32_t numSections = zp2sos(zRe, zIm, zeroNumRoots, pRe, pIm, poleNumRoots, *(sos));
	free(zRe);
	free(zIm);
	free(pRe);
	free(pIm);
	(*sos)[0] *= k;
	(*sos)[1] *= k;
	(*sos)[2] *= k;
	return numSections;
}
#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif
#ifndef M_2PI
#define M_2PI (M_PI * 2.0)
#endif
void LWZRCalculateCoefficients(LinkwitzRileyCrossover *lr2, double fs, double fc, char apf)
{
	double fpi = M_PI * fc;
	double wc = 2 * fpi;
	double wc2 = wc * wc;
	double wc22 = 2 * wc2;
	double k = wc / tan(fpi / fs);
	double k2 = k * k;
	double k22 = 2 * k2;
	double wck2 = 2 * wc*k;
	double tmpk = (k2 + wc2 + wck2);
	//b shared
	lr2->b1 = (-k22 + wc22) / tmpk;
	lr2->b2 = (-wck2 + k2 + wc2) / tmpk;
	if (!apf)
	{
		//---------------
		// low-pass
		//---------------
		lr2->a1_lp = wc22 / tmpk;
		lr2->a2_lp = wc2 / tmpk;
	}
	else
	{
		//---------------
		// all-pass
		//---------------
		lr2->a1_lp = (wc22 + k22) / tmpk;
		lr2->a2_lp = (wc2 - k2) / tmpk;
	}
	//----------------
	// high-pass
	//----------------
	lr2->a1_hp = (-k22) / tmpk;
	lr2->a2_hp = (k2) / tmpk;
}
void LWZRClearStateVariable(LinkwitzRileyCrossover *lr2)
{
	lr2->lp_xm0 = 0.0; lr2->lp_xm1 = 0.0; lr2->hp_xm0 = 0.0; lr2->hp_xm1 = 0.0;
}
void LWZRProcessSample(LinkwitzRileyCrossover *lr2, double in, double *low, double *high)
{
	*low = lr2->a2_lp * in + lr2->lp_xm0;
	lr2->lp_xm0 = lr2->a1_lp * in - lr2->b1 * *low + lr2->lp_xm1;
	lr2->lp_xm1 = lr2->a2_lp * in - lr2->b2 * *low;
	*high = lr2->a2_hp * in + lr2->hp_xm0;
	lr2->hp_xm0 = lr2->a1_hp * in - lr2->b1 * *high + lr2->hp_xm1;
	lr2->hp_xm1 = lr2->a2_hp * in - lr2->b2 * *high;
}
void LWZRProcessSampleAPF(LinkwitzRileyCrossover *lr2, double in, double *y)
{
	*y = lr2->a2_lp * in + lr2->lp_xm0;
	lr2->lp_xm0 = lr2->a1_lp * in - lr2->b1 * *y + lr2->lp_xm1;
	lr2->lp_xm1 = lr2->a2_lp * in - lr2->b2 * *y;
}
void init3BandsCrossover(ThreeBandsCrossover *lr3, double fs, double lowBandHz, double highBandHz)
{
	LWZRCalculateCoefficients(&lr3->sys[0], fs, lowBandHz, 0);
	LWZRCalculateCoefficients(&lr3->sys[1], fs, highBandHz, 0);
	LWZRCalculateCoefficients(&lr3->sys[2], fs, highBandHz, 1);
}
void clearState3BandsCrossover(ThreeBandsCrossover *lr3)
{
	LWZRClearStateVariable(&lr3->sys[0]);
	LWZRClearStateVariable(&lr3->sys[1]);
	LWZRClearStateVariable(&lr3->sys[2]);
}
void process3BandsCrossover(ThreeBandsCrossover *lr3, float x, double *lowOut, double *midOut, double *highOut)
{
	double highBand, lowBand;
	LWZRProcessSample(&lr3->sys[0], x, &lowBand, &highBand);
	LWZRProcessSample(&lr3->sys[1], highBand, midOut, highOut);
	LWZRProcessSampleAPF(&lr3->sys[2], lowBand, lowOut);
}
void init4BandsCrossover(FourBandsCrossover *lr4, double fs, double lowBandHz, double midBand1Hz, double midBand2Hz)
{
	LWZRCalculateCoefficients(&lr4->sys[0], fs, lowBandHz, 0);
	LWZRCalculateCoefficients(&lr4->sys[1], fs, midBand1Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[2], fs, midBand1Hz, 0);
	LWZRCalculateCoefficients(&lr4->sys[3], fs, midBand2Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[4], fs, midBand2Hz, 0);
}
void clearState4BandsCrossover(FourBandsCrossover *lr4)
{
	LWZRClearStateVariable(&lr4->sys[0]);
	LWZRClearStateVariable(&lr4->sys[1]);
	LWZRClearStateVariable(&lr4->sys[2]);
	LWZRClearStateVariable(&lr4->sys[3]);
	LWZRClearStateVariable(&lr4->sys[4]);
}
void process4BandsCrossover(FourBandsCrossover *lr4, float x, double *lowOut, double *midOut1, double *midOut2, double *highOut)
{
	double band0, band1, band2, apf;
	LWZRProcessSample(&lr4->sys[0], x, &band0, &band1);
	LWZRProcessSampleAPF(&lr4->sys[1], band0, lowOut); //APF
	LWZRProcessSample(&lr4->sys[2], band1, &apf, &band2);
	LWZRProcessSampleAPF(&lr4->sys[3], apf, midOut1); //APF
	LWZRProcessSample(&lr4->sys[4], band2, midOut2, highOut);
}
void init5BandsCrossover(FiveBandsCrossover *lr4, double fs, double lowBandHz, double midBand1Hz, double midBand2Hz, double midBand3Hz)
{
	LWZRCalculateCoefficients(&lr4->sys[0], fs, lowBandHz, 0);
	LWZRCalculateCoefficients(&lr4->sys[1], fs, midBand1Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[2], fs, midBand1Hz, 0);
	LWZRCalculateCoefficients(&lr4->sys[3], fs, midBand2Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[4], fs, midBand2Hz, 0);
	LWZRCalculateCoefficients(&lr4->sys[5], fs, midBand2Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[6], fs, midBand3Hz, 0);
	LWZRCalculateCoefficients(&lr4->sys[7], fs, midBand3Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[8], fs, midBand3Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[9], fs, midBand3Hz, 1); //APF
}
void clearState5BandsCrossover(FiveBandsCrossover *lr4)
{
	LWZRClearStateVariable(&lr4->sys[0]);
	LWZRClearStateVariable(&lr4->sys[1]);
	LWZRClearStateVariable(&lr4->sys[2]);
	LWZRClearStateVariable(&lr4->sys[3]);
	LWZRClearStateVariable(&lr4->sys[4]);
	LWZRClearStateVariable(&lr4->sys[5]);
	LWZRClearStateVariable(&lr4->sys[6]);
	LWZRClearStateVariable(&lr4->sys[7]);
	LWZRClearStateVariable(&lr4->sys[8]);
	LWZRClearStateVariable(&lr4->sys[9]);
}
void process5BandsCrossover(FiveBandsCrossover *lr4, float x, double *lowOut, double *midOut1, double *midOut2, double *midOut3, double *highOut)
{
	double band0, band1, band2, band3, apf;
	LWZRProcessSample(&lr4->sys[0], x, &band0, &band1);
	LWZRProcessSampleAPF(&lr4->sys[1], band0, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[5], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[7], apf, lowOut); //APF
	LWZRProcessSample(&lr4->sys[2], band1, &apf, &band2);
	LWZRProcessSampleAPF(&lr4->sys[3], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[8], apf, midOut1); //APF
	LWZRProcessSample(&lr4->sys[4], band2, &apf, &band3);
	LWZRProcessSampleAPF(&lr4->sys[9], apf, midOut2); //APF
	LWZRProcessSample(&lr4->sys[6], band3, midOut3, highOut);
}
void init6BandsCrossover(SixBandsCrossover *lr4, double fs, double lowBandHz, double midBand1Hz, double midBand2Hz, double midBand3Hz, double midBand4Hz)
{
	LWZRCalculateCoefficients(&lr4->sys[0], fs, lowBandHz, 0);
	LWZRCalculateCoefficients(&lr4->sys[1], fs, midBand1Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[2], fs, midBand1Hz, 0);
	LWZRCalculateCoefficients(&lr4->sys[3], fs, midBand2Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[4], fs, midBand2Hz, 0);
	LWZRCalculateCoefficients(&lr4->sys[5], fs, midBand2Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[6], fs, midBand3Hz, 0);
	LWZRCalculateCoefficients(&lr4->sys[7], fs, midBand3Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[8], fs, midBand3Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[9], fs, midBand3Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[10], fs, midBand4Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[11], fs, midBand4Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[12], fs, midBand4Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[13], fs, midBand4Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[14], fs, midBand4Hz, 0);
}
void clearState6BandsCrossover(SixBandsCrossover *lr4)
{
	LWZRClearStateVariable(&lr4->sys[0]);
	LWZRClearStateVariable(&lr4->sys[1]);
	LWZRClearStateVariable(&lr4->sys[2]);
	LWZRClearStateVariable(&lr4->sys[3]);
	LWZRClearStateVariable(&lr4->sys[4]);
	LWZRClearStateVariable(&lr4->sys[5]);
	LWZRClearStateVariable(&lr4->sys[6]);
	LWZRClearStateVariable(&lr4->sys[7]);
	LWZRClearStateVariable(&lr4->sys[8]);
	LWZRClearStateVariable(&lr4->sys[9]);
	LWZRClearStateVariable(&lr4->sys[10]);
	LWZRClearStateVariable(&lr4->sys[11]);
	LWZRClearStateVariable(&lr4->sys[12]);
	LWZRClearStateVariable(&lr4->sys[13]);
	LWZRClearStateVariable(&lr4->sys[14]);
}
void process6BandsCrossover(SixBandsCrossover *lr4, float x, double *lowOut, double *midOut1, double *midOut2, double *midOut3, double *midOut4, double *highOut)
{
	double band0, band1, band2, band3, band4, apf;
	LWZRProcessSample(&lr4->sys[0], x, &band0, &band1);
	LWZRProcessSampleAPF(&lr4->sys[1], band0, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[5], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[7], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[10], apf, lowOut);
	LWZRProcessSample(&lr4->sys[2], band1, &apf, &band2);
	LWZRProcessSampleAPF(&lr4->sys[3], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[8], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[12], apf, midOut1);
	LWZRProcessSample(&lr4->sys[4], band2, &apf, &band3);
	LWZRProcessSampleAPF(&lr4->sys[9], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[13], apf, midOut2);
	LWZRProcessSample(&lr4->sys[6], band3, &apf, &band4);
	LWZRProcessSampleAPF(&lr4->sys[11], apf, midOut3); //APF
	LWZRProcessSample(&lr4->sys[14], band4, midOut4, highOut);
}
void init7BandsCrossover(SevenBandsCrossover *lr4, double fs, double lowBandHz, double midBand1Hz, double midBand2Hz, double midBand3Hz, double midBand4Hz, double midBand5Hz)
{
	LWZRCalculateCoefficients(&lr4->sys[0], fs, lowBandHz, 0);
	LWZRCalculateCoefficients(&lr4->sys[1], fs, midBand1Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[2], fs, midBand1Hz, 0);
	LWZRCalculateCoefficients(&lr4->sys[3], fs, midBand2Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[4], fs, midBand2Hz, 0);
	LWZRCalculateCoefficients(&lr4->sys[5], fs, midBand2Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[6], fs, midBand3Hz, 0);
	LWZRCalculateCoefficients(&lr4->sys[7], fs, midBand3Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[8], fs, midBand3Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[9], fs, midBand3Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[10], fs, midBand4Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[11], fs, midBand4Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[12], fs, midBand4Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[13], fs, midBand4Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[14], fs, midBand4Hz, 0);
	LWZRCalculateCoefficients(&lr4->sys[15], fs, midBand5Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[16], fs, midBand5Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[17], fs, midBand5Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[18], fs, midBand5Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[19], fs, midBand5Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[20], fs, midBand5Hz, 0);
}
void clearState7BandsCrossover(SevenBandsCrossover *lr4)
{
	LWZRClearStateVariable(&lr4->sys[0]);
	LWZRClearStateVariable(&lr4->sys[1]);
	LWZRClearStateVariable(&lr4->sys[2]);
	LWZRClearStateVariable(&lr4->sys[3]);
	LWZRClearStateVariable(&lr4->sys[4]);
	LWZRClearStateVariable(&lr4->sys[5]);
	LWZRClearStateVariable(&lr4->sys[6]);
	LWZRClearStateVariable(&lr4->sys[7]);
	LWZRClearStateVariable(&lr4->sys[8]);
	LWZRClearStateVariable(&lr4->sys[9]);
	LWZRClearStateVariable(&lr4->sys[10]);
	LWZRClearStateVariable(&lr4->sys[11]);
	LWZRClearStateVariable(&lr4->sys[12]);
	LWZRClearStateVariable(&lr4->sys[13]);
	LWZRClearStateVariable(&lr4->sys[14]);
	LWZRClearStateVariable(&lr4->sys[15]);
	LWZRClearStateVariable(&lr4->sys[16]);
	LWZRClearStateVariable(&lr4->sys[17]);
	LWZRClearStateVariable(&lr4->sys[18]);
	LWZRClearStateVariable(&lr4->sys[19]);
	LWZRClearStateVariable(&lr4->sys[20]);
}
void process7BandsCrossover(SevenBandsCrossover *lr4, float x, double *lowOut, double *midOut1, double *midOut2, double *midOut3, double *midOut4, double *midOut5, double *highOut)
{
	double band0, band1, band2, band3, band4, band5, apf;
	LWZRProcessSample(&lr4->sys[0], x, &band0, &band1);
	LWZRProcessSampleAPF(&lr4->sys[1], band0, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[5], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[7], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[10], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[15], apf, lowOut); //APF
	LWZRProcessSample(&lr4->sys[2], band1, &apf, &band2);
	LWZRProcessSampleAPF(&lr4->sys[3], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[8], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[12], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[16], apf, midOut1); //APF
	LWZRProcessSample(&lr4->sys[4], band2, &apf, &band3);
	LWZRProcessSampleAPF(&lr4->sys[9], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[13], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[17], apf, midOut2); //APF
	LWZRProcessSample(&lr4->sys[6], band3, &apf, &band4);
	LWZRProcessSampleAPF(&lr4->sys[11], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[18], apf, midOut3); //APF
	LWZRProcessSample(&lr4->sys[14], band4, &apf, &band5);
	LWZRProcessSampleAPF(&lr4->sys[19], apf, midOut4); //APF
	LWZRProcessSample(&lr4->sys[20], band5, midOut5, highOut);
}
void init8BandsCrossover(EightBandsCrossover *lr4, double fs, double lowBandHz, double midBand1Hz, double midBand2Hz, double midBand3Hz, double midBand4Hz, double midBand5Hz, double midBand6Hz)
{
	LWZRCalculateCoefficients(&lr4->sys[0], fs, lowBandHz, 0);
	LWZRCalculateCoefficients(&lr4->sys[1], fs, midBand1Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[2], fs, midBand1Hz, 0);
	LWZRCalculateCoefficients(&lr4->sys[3], fs, midBand2Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[4], fs, midBand2Hz, 0);
	LWZRCalculateCoefficients(&lr4->sys[5], fs, midBand2Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[6], fs, midBand3Hz, 0);
	LWZRCalculateCoefficients(&lr4->sys[7], fs, midBand3Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[8], fs, midBand3Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[9], fs, midBand3Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[10], fs, midBand4Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[11], fs, midBand4Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[12], fs, midBand4Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[13], fs, midBand4Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[14], fs, midBand4Hz, 0);
	LWZRCalculateCoefficients(&lr4->sys[15], fs, midBand5Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[16], fs, midBand5Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[17], fs, midBand5Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[18], fs, midBand5Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[19], fs, midBand5Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[20], fs, midBand5Hz, 0);
	LWZRCalculateCoefficients(&lr4->sys[21], fs, midBand6Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[22], fs, midBand6Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[23], fs, midBand6Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[24], fs, midBand6Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[25], fs, midBand6Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[26], fs, midBand6Hz, 1); //APF
	LWZRCalculateCoefficients(&lr4->sys[27], fs, midBand6Hz, 0);
}
void clearState8BandsCrossover(EightBandsCrossover *lr4)
{
	LWZRClearStateVariable(&lr4->sys[0]);
	LWZRClearStateVariable(&lr4->sys[1]);
	LWZRClearStateVariable(&lr4->sys[2]);
	LWZRClearStateVariable(&lr4->sys[3]);
	LWZRClearStateVariable(&lr4->sys[4]);
	LWZRClearStateVariable(&lr4->sys[5]);
	LWZRClearStateVariable(&lr4->sys[6]);
	LWZRClearStateVariable(&lr4->sys[7]);
	LWZRClearStateVariable(&lr4->sys[8]);
	LWZRClearStateVariable(&lr4->sys[9]);
	LWZRClearStateVariable(&lr4->sys[10]);
	LWZRClearStateVariable(&lr4->sys[11]);
	LWZRClearStateVariable(&lr4->sys[12]);
	LWZRClearStateVariable(&lr4->sys[13]);
	LWZRClearStateVariable(&lr4->sys[14]);
	LWZRClearStateVariable(&lr4->sys[15]);
	LWZRClearStateVariable(&lr4->sys[16]);
	LWZRClearStateVariable(&lr4->sys[17]);
	LWZRClearStateVariable(&lr4->sys[18]);
	LWZRClearStateVariable(&lr4->sys[19]);
	LWZRClearStateVariable(&lr4->sys[20]);
	LWZRClearStateVariable(&lr4->sys[21]);
	LWZRClearStateVariable(&lr4->sys[22]);
	LWZRClearStateVariable(&lr4->sys[23]);
	LWZRClearStateVariable(&lr4->sys[24]);
	LWZRClearStateVariable(&lr4->sys[25]);
	LWZRClearStateVariable(&lr4->sys[26]);
	LWZRClearStateVariable(&lr4->sys[27]);
}
void process8BandsCrossover(EightBandsCrossover *lr4, float x, double *lowOut, double *midOut1, double *midOut2, double *midOut3, double *midOut4, double *midOut5, double *midOut6, double *highOut)
{
	double band0, band1, band2, band3, band4, band5, band6, apf;
	LWZRProcessSample(&lr4->sys[0], x, &band0, &band1);
	LWZRProcessSampleAPF(&lr4->sys[1], band0, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[5], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[7], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[10], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[15], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[21], apf, lowOut); //APF
	LWZRProcessSample(&lr4->sys[2], band1, &apf, &band2);
	LWZRProcessSampleAPF(&lr4->sys[3], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[8], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[12], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[16], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[22], apf, midOut1); //APF
	LWZRProcessSample(&lr4->sys[4], band2, &apf, &band3);
	LWZRProcessSampleAPF(&lr4->sys[9], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[13], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[17], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[23], apf, midOut2); //APF
	LWZRProcessSample(&lr4->sys[6], band3, &apf, &band4);
	LWZRProcessSampleAPF(&lr4->sys[11], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[18], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[24], apf, midOut3); //APF
	LWZRProcessSample(&lr4->sys[14], band4, &apf, &band5);
	LWZRProcessSampleAPF(&lr4->sys[19], apf, &apf); //APF
	LWZRProcessSampleAPF(&lr4->sys[25], apf, midOut4); //APF
	LWZRProcessSample(&lr4->sys[20], band5, &apf, &band6);
	LWZRProcessSampleAPF(&lr4->sys[26], apf, midOut5); //APF
	LWZRProcessSample(&lr4->sys[27], band6, midOut6, highOut);
}
void unwrap(float *p, int N, double tol)
{
	if (N < 2)
		return;
	if (tol <= M_PI)
		tol = M_PI;
	// incremental phase variation 
	// MATLAB: dp = diff(p, 1, 1);
	double dp = p[0 + 1] - p[0];
	// equivalent phase variation in [-pi, pi]
	// MATLAB: dps = mod(dp+dp,2*pi) - pi;
	double dps = (dp + M_PI) - floor((dp + M_PI) / (2 * M_PI))*(2 * M_PI) - M_PI;
	// preserve variation sign for +pi vs. -pi
	// MATLAB: dps(dps==pi & dp>0,:) = pi;
	if ((dps == -M_PI) && (dp > 0.0))
		dps = M_PI;
	// incremental phase correction
	// MATLAB: dp_corr = dps - dp;
	double dp_corr = dps - dp;
	// Ignore correction when incremental variation is smaller than tol
	// MATLAB: dp_corr(abs(dp)<tol,:) = 0;
	if (fabs(dp) < tol)
		dp_corr = 0.0;
	// Find cumulative sum of deltas
	// MATLAB: cumsum = cumsum(dp_corr, 1);
	double cumsum;
	double cumsumOld = dp_corr;
	int j;
	for (j = 1; j < N - 1; j++)
	{
		// incremental phase variation 
		// MATLAB: dp = diff(p, 1, 1);
		dp = p[j + 1] - p[j];
		// equivalent phase variation in [-pi, pi]
		// MATLAB: dps = mod(dp+dp,2*pi) - pi;
		dps = (dp + M_PI) - floor((dp + M_PI) / (2 * M_PI))*(2 * M_PI) - M_PI;
		// preserve variation sign for +pi vs. -pi
		// MATLAB: dps(dps==pi & dp>0,:) = pi;
		if ((dps == -M_PI) && (dp > 0.0))
			dps = M_PI;
		// incremental phase correction
		// MATLAB: dp_corr = dps - dp;
		dp_corr = dps - dp;
		// Ignore correction when incremental variation is smaller than tol
		// MATLAB: dp_corr(abs(dp)<tol,:) = 0;
		if (fabs(dp) < tol)
			dp_corr = 0.0;
		// Find cumulative sum of deltas
		// MATLAB: cumsum = cumsum(dp_corr, 1);
		cumsum = cumsumOld + dp_corr;
		// Integrate corrections and add to P to produce smoothed phase values
		// MATLAB: p(2:m,:) = p(2:m,:) + cumsum(dp_corr,1);
		p[j] += (float)cumsumOld;
		cumsumOld = cumsum;
	}
	// Integrate corrections and add to P to produce smoothed phase values
	// MATLAB: p(2:m,:) = p(2:m,:) + cumsum(dp_corr,1);
	p[N - 1] += (float)cumsumOld;
}
unsigned int fhtIntegerLog2(unsigned int v)
{
	unsigned int i = 0;
	while (v > 1)
	{
		++i;
		v >>= 1;
	}
	return i;
}
unsigned fhtRevBits(unsigned int x, unsigned int bits)
{
	unsigned int y = 0;
	while (bits--)
	{
		y = (y + y) + (x & 1);
		x >>= 1;
	}
	return y;
}
void fhtbitReversalTbl(unsigned *dst, unsigned int n)
{
	unsigned int bits = fhtIntegerLog2(n);
	for (unsigned int i = 0; i < n; ++i)
		dst[i] = fhtRevBits(i, bits);
}
void fhtsinHalfTblFloat(float *dst, unsigned int n)
{
	const double twopi_over_n = 6.283185307179586476925286766559 / n;
	for (unsigned int i = 0; i < n; ++i)
		dst[i] = (float)sin(twopi_over_n * i);
}