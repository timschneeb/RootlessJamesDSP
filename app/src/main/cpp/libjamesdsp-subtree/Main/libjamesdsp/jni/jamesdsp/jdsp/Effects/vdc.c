#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <math.h>
#include <float.h>
#include "../jdsp_header.h"
double SOS_DF2Process(DirectForm2 *df2, double x1)
{
	double w1 = x1 - df2->a1*df2->v1L - df2->a2*df2->v2L;
	double y1 = df2->b0*w1 + df2->b1*df2->v1L + df2->b2*df2->v2L;
	df2->v2L = df2->v1L;
	df2->v1L = w1;
	return y1;
}
void SOS_DF2_StereoProcess(DirectForm2 *df2, double x1, double x2, double *Out_y1, double *Out_y2)
{
	double w1 = x1 - df2->a1*df2->v1L - df2->a2*df2->v2L;
	double y1 = df2->b0*w1 + df2->b1*df2->v1L + df2->b2*df2->v2L;
	df2->v2L = df2->v1L;
	df2->v1L = w1;
	*Out_y1 = y1;
	double w2 = x2 - df2->a1*df2->v1R - df2->a2*df2->v2R;
	double y2 = df2->b0*w2 + df2->b1*df2->v1R + df2->b2*df2->v2R;
	df2->v2R = df2->v1R;
	df2->v1R = w2;
	*Out_y2 = y2;
}
int countChars(char* s, char c)
{
	return *s == '\0' ? 0 : countChars(s + 1, c) + (*s == c);
}
int get_doubleVDC(char *val, double *F)
{
	char *eptr;
	errno = 0;
	double f = strtod(val, &eptr);
	if (eptr != val && errno != ERANGE)
	{
		*F = f;
		return 1;
	}
	return 0;
}
int DDCParser(char *DDCString, DirectForm2 ***ptrdf441, DirectForm2 ***ptrdf48)
{
	char *fs44_1 = strstr(DDCString, "SR_44100");
	char *fs48 = strstr(DDCString, "SR_48000");
	if (!fs44_1 || !fs48)
		return 0;
	int numberCount = (countChars(fs48, ',') + 1);
	int sosCount = numberCount / 5;
	if (!sosCount)
		return 0;
	DirectForm2 **df441 = (DirectForm2**)malloc(sosCount * sizeof(DirectForm2*));
	DirectForm2 **df48 = (DirectForm2**)malloc(sosCount * sizeof(DirectForm2*));
	int i;
	for (i = 0; i < sosCount; i++)
	{
		df441[i] = (DirectForm2*)malloc(sizeof(DirectForm2));
		memset(df441[i], 0, sizeof(DirectForm2));
		df48[i] = (DirectForm2*)malloc(sizeof(DirectForm2));
		memset(df48[i], 0, sizeof(DirectForm2));
	}
	double number;
	i = 0;
	int counter = 0;
	int b0b1b2a1a2 = 0;
	char *startingPoint = fs44_1 + 9;
	while (counter < numberCount)
	{
		if (get_doubleVDC(startingPoint, &number))
		{
			double val = strtod(startingPoint, &startingPoint);
			counter++;
			if (!b0b1b2a1a2)
				df441[i]->b0 = val;
			else if (b0b1b2a1a2 == 1)
				df441[i]->b1 = val;
			else if (b0b1b2a1a2 == 2)
				df441[i]->b2 = val;
			else if (b0b1b2a1a2 == 3)
				df441[i]->a1 = -val;
			else if (b0b1b2a1a2 == 4)
			{
				df441[i]->a2 = -val;
				i++;
			}
			b0b1b2a1a2++;
			if (b0b1b2a1a2 == 5)
				b0b1b2a1a2 = 0;
		}
		else
			startingPoint++;
	}
	i = 0;
	counter = 0;
	b0b1b2a1a2 = 0;
	startingPoint = fs48 + 9;
	while (counter < numberCount)
	{
		if (get_doubleVDC(startingPoint, &number))
		{
			double val = strtod(startingPoint, &startingPoint);
			counter++;
			if (!b0b1b2a1a2)
				df48[i]->b0 = val;
			else if (b0b1b2a1a2 == 1)
				df48[i]->b1 = val;
			else if (b0b1b2a1a2 == 2)
				df48[i]->b2 = val;
			else if (b0b1b2a1a2 == 3)
				df48[i]->a1 = -val;
			else if (b0b1b2a1a2 == 4)
			{
				df48[i]->a2 = -val;
				i++;
			}
			b0b1b2a1a2++;
			if (b0b1b2a1a2 == 5)
				b0b1b2a1a2 = 0;
		}
		else
			startingPoint++;
	}
	*ptrdf441 = df441;
	*ptrdf48 = df48;
	return sosCount;
}
void complexMultiplicationRI(double *zReal, double *zImag, double xReal, double xImag, double yReal, double yImag)
{
	*zReal = xReal * yReal - xImag * yImag;
	*zImag = xReal * yImag + xImag * yReal;
}
void complexDivisionRI(double *zReal, double *zImag, double xReal, double xImag, double yReal, double yImag)
{
	*zReal = (xReal * yReal + xImag * yImag) / (yReal * yReal + yImag * yImag);
	*zImag = (xImag * yReal - xReal * yImag) / (yReal * yReal + yImag * yImag);
}
// Analytical magnitude response of digital filters
void DigitalFilterMagnitudeResponsedB(DirectForm2 **IIR, int section, double *magnitude, int NumPts)
{
	double Arg;
	double z1Real, z1Imag, z2Real, z2Imag, HofZReal, HofZImag, DenomReal, DenomImag, tmpReal, tmpImag;
	for (int j = 0; j < NumPts; j++)
	{
		Arg = M_PI * (double)j / (double)NumPts;
		z1Real = cos(Arg), z1Imag = -sin(Arg);
		complexMultiplicationRI(&z2Real, &z2Imag, z1Real, z1Imag, z1Real, z1Imag);
		HofZReal = 1.0, HofZImag = 0.0;
		tmpReal = IIR[section]->b0 + IIR[section]->b1 * z1Real + IIR[section]->b2 * z2Real;
		tmpImag = IIR[section]->b1 * z1Imag + IIR[section]->b2 * z2Imag;
		complexMultiplicationRI(&HofZReal, &HofZImag, HofZReal, HofZImag, tmpReal, tmpImag);
		DenomReal = 1.0 + IIR[section]->a1 * z1Real + IIR[section]->a2 * z2Real;
		DenomImag = IIR[section]->a1 * z1Imag + IIR[section]->a2 * z2Imag;
		if (sqrt(DenomReal * DenomReal + DenomImag * DenomImag) < DBL_EPSILON)
			magnitude[j] = 0.0;
		else
		{
			complexDivisionRI(&HofZReal, &HofZImag, HofZReal, HofZImag, DenomReal, DenomImag);
			magnitude[j] = 20.0 * log10(sqrt(HofZReal * HofZReal + HofZImag * HofZImag));
		}
	}
}
// DDC resampler
void designPeakingFilter(double dbGain, double centreFreq, double fs, double dBandwidthOrQOrS, double *b0, double *b1, double *b2, double *a1, double *a2)
{
	if (centreFreq <= DBL_EPSILON || fs <= DBL_EPSILON)
		return;
	const double at1d3 = atanh(1.0 / 3.0);
	double lingain = pow(10.0, dbGain / 40.0);
	double omega = (6.2831853071795862 * centreFreq) / fs;
	double num3 = sin(omega);
	double cs = cos(omega);
	double alpha = num3 * sinh((at1d3 * dBandwidthOrQOrS * omega) / num3);
	double B0 = 1.0 + (alpha * lingain);
	double B1 = -2.0 * cs;
	double B2 = 1.0 - (alpha * lingain);
	double A0 = 1.0 + (alpha / lingain);
	double A1 = -2.0 * cs;
	double A2 = 1.0 - (alpha / lingain);
	*b0 = B0 / A0;
	*b1 = B1 / A0;
	*b2 = B2 / A0;
	*a1 = A1 / A0;
	*a2 = A2 / A0;
}
int PeakingFilterResampler(DirectForm2 **inputIIR, double inFs, DirectForm2 ***resampledIIR, double outFs, int sosCount)
{
	const int filterTestLength = 32768;
	double *magnitude = (double*)malloc(filterTestLength * sizeof(double));
	int i;
	int outSOSCount = 0;
	double *cell[3];
	for (i = 0; i < 3; i++)
		cell[i] = (double*)malloc(sosCount * sizeof(double));
	for (i = 0; i < sosCount; i++)
	{
		DigitalFilterMagnitudeResponsedB(inputIIR, i, magnitude, filterTestLength);
		double absoluteMax = fabs(magnitude[0]);
		double centreFreq = 0.0;
		int index = 0;
		for (int c = 1; c < filterTestLength; c++)
		{
			if (fabs(magnitude[c]) > absoluteMax)
			{
				absoluteMax = fabs(magnitude[c]);
				index = c;
				centreFreq = round(c * (inFs / filterTestLength / 2.0));
			}
		}
		if (centreFreq < DBL_EPSILON)
			centreFreq = DBL_EPSILON;
		if (fabs(inputIIR[i]->b1) < DBL_EPSILON)
		{
			if (inputIIR[i]->b1 < 0.0)
				inputIIR[i]->b1 = -DBL_EPSILON;
			else
				inputIIR[i]->b1 = DBL_EPSILON;
		}
		double omega = (6.2831853071795862 * centreFreq) / inFs;
		double A0 = (-2.0 * cos(omega)) / inputIIR[i]->b1;
		double lingain = pow(10.0, magnitude[index] / 40.0);
		double num3 = sin(omega);
		double bandwidth = ((asinh(((A0 - 1.0) * lingain) / num3) * num3) / omega) / 0.34657359027997264;
		if (bandwidth > 98.8 - DBL_EPSILON)
			bandwidth = 98.8;
		if (centreFreq < outFs - DBL_EPSILON)
			outSOSCount++;
		cell[0][i] = magnitude[index];
		cell[1][i] = centreFreq;
		cell[2][i] = bandwidth;
	}
	free(magnitude);
	DirectForm2 **temp = (DirectForm2**)malloc(outSOSCount * sizeof(DirectForm2*));
	for (i = 0; i < outSOSCount; i++)
	{
		temp[i] = (DirectForm2*)malloc(sizeof(DirectForm2));
		memset(temp[i], 0, sizeof(DirectForm2));
		designPeakingFilter(cell[0][i], cell[1][i], outFs, cell[2][i], &temp[i]->b0, &temp[i]->b1, &temp[i]->b2, &temp[i]->a1, &temp[i]->a2);
	}
	for (i = 0; i < 3; i++)
		free(cell[i]);
	*resampledIIR = temp;
	return outSOSCount;
}
void DDCConstructor(JamesDSPLib *jdsp)
{
	jdsp->vdcFl.oldFile = 0;
	jdsp->vdcFl.sosPointer = 0;
	jdsp->vdcFl.usedSOSCount = 0;
}
void DDCDestructor(JamesDSPLib *jdsp)
{
	jdsp_lock(jdsp);
	if (jdsp->vdcFl.sosPointer)
	{
		for (int i = 0; i < jdsp->vdcFl.usedSOSCount; i++)
			free(jdsp->vdcFl.sosPointer[i]);
		free(jdsp->vdcFl.sosPointer);
		jdsp->vdcFl.sosPointer = 0;
	}
	if (jdsp->vdcFl.oldFile)
	{
		free(jdsp->vdcFl.oldFile);
		jdsp->vdcFl.oldFile = 0;
	}
	jdsp_unlock(jdsp);
}
int DDCRefresh(JamesDSPLib *jdsp, char *inStr)
{
	if (!inStr)
		return 0;
	DirectForm2 **df441, **df48;
	int sosCount = DDCParser(inStr, &df441, &df48);
	if (!sosCount)
		return 0;
	if (jdsp->fs == 44100.0f && df441)
	{
		jdsp->vdcFl.sosPointer = df441;
		jdsp->vdcFl.usedSOSCount = sosCount;
		for (int i = 0; i < sosCount; i++)
			free(df48[i]);
		free(df48);
	}
	else if (jdsp->fs == 48000.0f && df48)
	{
		jdsp->vdcFl.sosPointer = df48;
		jdsp->vdcFl.usedSOSCount = sosCount;
		for (int i = 0; i < sosCount; i++)
			free(df441[i]);
		free(df441);
	}
	else
	{
		DirectForm2 **dfResampled;
		int resampledSOSCount = PeakingFilterResampler(df48, 48000.0, &dfResampled, jdsp->fs, sosCount);
		jdsp->vdcFl.usedSOSCount = resampledSOSCount;
		jdsp->vdcFl.sosPointer = dfResampled;
		for (int i = 0; i < sosCount; i++)
		{
			free(df441[i]);
			free(df48[i]);
		}
		free(df441);
		free(df48);
	}
	return 1;
}
int DDCEnable(JamesDSPLib *jdsp)
{
	int success = 1;
	if (jdsp->ddcForceRefresh)
	{
		success = DDCRefresh(jdsp, jdsp->vdcFl.oldFile);
		jdsp->ddcForceRefresh = 1;
	}
	if (!success || !jdsp->vdcFl.usedSOSCount || !jdsp->vdcFl.sosPointer)
	{
		jdsp->ddcEnabled = 0;
		return -1;
	}
	else
	{
		jdsp->ddcEnabled = 1;
		return 1;
	}
}
void DDCDisable(JamesDSPLib *jdsp)
{
	jdsp->ddcEnabled = 0;
}
int DDCStringParser(JamesDSPLib *jdsp, char *newStr)
{
	jdsp_lock(jdsp);
	if (jdsp->vdcFl.oldFile)
	{
		if (!strcmp(jdsp->vdcFl.oldFile, newStr) && !jdsp->ddcForceRefresh)
		{
			jdsp_unlock(jdsp);
			return 0;
		}
	}
	if (jdsp->vdcFl.sosPointer)
	{
		for (int i = 0; i < jdsp->vdcFl.usedSOSCount; i++)
			free(jdsp->vdcFl.sosPointer[i]);
		free(jdsp->vdcFl.sosPointer);
		jdsp->vdcFl.usedSOSCount = 0;
		jdsp->vdcFl.sosPointer = 0;
	}
	int success = DDCRefresh(jdsp, newStr);
	if (!success)
	{
		if (jdsp->vdcFl.oldFile)
		{
			free(jdsp->vdcFl.oldFile);
			jdsp->vdcFl.oldFile = 0;
		}
		jdsp_unlock(jdsp);
		return -1;
	}
	if (jdsp->vdcFl.oldFile)
		free(jdsp->vdcFl.oldFile);
	size_t stLe = strlen(newStr);
	jdsp->vdcFl.oldFile = (char*)malloc(stLe + 1);
	strncpy(jdsp->vdcFl.oldFile, newStr, stLe);
	jdsp->vdcFl.oldFile[stLe] = '\0';
	jdsp->ddcForceRefresh = 0;
	jdsp_unlock(jdsp);
	return 1;
}
void DDCProcess(JamesDSPLib *jdsp, size_t n)
{
	if (jdsp->vdcFl.usedSOSCount)
	{
		for (size_t i = 0; i < n; i++)
		{
			double sampleOutL = (double)jdsp->tmpBuffer[0][i], sampleOutR = (double)jdsp->tmpBuffer[1][i];
			for (int j = 0; j < jdsp->vdcFl.usedSOSCount; j++)
				SOS_DF2_StereoProcess(jdsp->vdcFl.sosPointer[j], sampleOutL, sampleOutR, &sampleOutL, &sampleOutR);
			jdsp->tmpBuffer[0][i] = (float)sampleOutL;
			jdsp->tmpBuffer[1][i] = (float)sampleOutR;
		}
	}
}
