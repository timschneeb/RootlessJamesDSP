#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <float.h>
#include "../../ns-eel.h"
#include "../quadprog.h"
#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif
#ifndef M_2PI
#define M_2PI 6.28318530717958647692
#endif
void complexMultiplication(double xReal, double xImag, double yReal, double yImag, double *zReal, double *zImag)
{
	*zReal = xReal * yReal - xImag * yImag;
	*zImag = xReal * yImag + xImag * yReal;
}
void cdivid(const double ar, const double ai, const double br, const double bi, double *cr, double *ci)
{
	double r, d;
	if (br == 0.0 && bi == 0.0)
	{
		// Division by zero, c = infinity
		*cr = DBL_MAX;
		*ci = DBL_MAX;
		return;
	}
	if (fabs(br) < fabs(bi))
	{
		r = br / bi;
		d = bi + r * br;
		*cr = (ar * r + ai) / d;
		*ci = (ai * r - ar) / d;
		return;
	}
	r = bi / br;
	d = br + r * bi;
	*cr = (ar + ai * r) / d;
	*ci = (ai - ar * r) / d;
}
double sinc(double x)
{
	if (x != 0.0)
		return sin(M_PI * x) / (M_PI * x);
	else
		return 1.0;
}
void cplxlog(double zRe, double zIm, double *yRe, double *yIm)
{
	double rr = hypot(zRe, zIm);
	double p = log(rr);
	rr = atan2(zIm, zRe);
	*yRe = p;
	*yIm = rr;
}
double eps(double re, double im)
{
	double y;
	int32_t exponent;
	double absxk = hypot(re, im);
	if (absxk <= 2.2250738585072014E-308)
		y = 4.94065645841247E-324;
	else {
		frexp(absxk, &exponent);
		y = ldexp(1.0, exponent - 53);
	}
	return y;
}
void cplxexp(double re, double im, double *yRe, double *yIm)
{
	*yRe = exp(re) * cos(im);
	*yIm = exp(re) * sin(im);
}
void cplxexpint(double re, double im, double *yRe, double *yIm)
{
	static const double p[9] = { -3.6026936263360228E-9, -4.81953845214096E-7, -2.5694983221159331E-5, -0.000697379085953419, -0.010195735298457921, -0.078118635592481972, -0.30124328927627148, -0.77738073257355289, 8.2676619523664776 };
	double polyv, previous = 0.0;
	if (re != 0.0)
	{
		for (int32_t k = 0; k < 9; k++)
		{
			polyv = previous + p[k];
			previous = polyv * re;
		}
	}
	else
		polyv = p[8];
	// series expansion
	double tmpRe, tmpIm;
	if (fabs(im) <= polyv)
	{
		double egamma = 0.57721566490153286061;
		double xkRe = re;
		double xkIm = im;
		cplxlog(re, im, &tmpRe, &tmpIm);
		double ykRe = -egamma - tmpRe;
		double ykIm = -tmpIm;
		double j = 1.0;
		double ptermRe = re;
		double ptermIm = im;
		double termRe = re;
		double termIm = im;
		while (hypot(termRe, termIm) > eps(ykRe, ykIm))
		{
			ykRe = ykRe + termRe;
			ykIm = ykIm + termIm;
			j = j + 1.0;
			complexMultiplication(-xkRe, -xkIm, ptermRe / j, ptermIm / j, &ptermRe, &ptermIm);
			termRe = ptermRe / j;
			termIm = ptermIm / j;
		}
		*yRe = ykRe;
		*yIm = ykIm;
	}
	else
	{
		double xkRe = re;
		double xkIm = im;
		double am2Re = 0.0;
		double am2Im = 0.0;
		double bm2Re = 1.0;
		double bm2Im = 0.0;
		double am1Re = 1.0;
		double am1Im = 0.0;
		double bm1Re = xkRe;
		double bm1Im = xkIm;
		double fRe, fIm;
		cdivid(am1Re, am1Im, bm1Re, bm1Im, &fRe, &fIm);
		double oldfRe = (double)FLT_MAX;
		double oldfIm = 0.0;
		int32_t j = 2;
		while (hypot(fRe - oldfRe, fIm - oldfIm) > (100.0 * DBL_EPSILON * hypot(fRe, fIm)))
		{
			// calculate the coefficients of the recursion formulas for j even
			double alpha = j * 0.5; // note: beta= 1
			//calculate A(j), B(j), and f(j)
			double aRe = am1Re + alpha * am2Re;
			double aIm = am1Im + alpha * am2Im;
			double bRe = bm1Re + alpha * bm2Re;
			double bIm = bm1Im + alpha * bm2Im;
			// save new normalized variables for next pass through the loop
			//  note: normalization to avoid overflow or underflow
			cdivid(am1Re, am1Im, bRe, bIm, &am2Re, &am2Im);
			cdivid(bm1Re, bm1Im, bRe, bIm, &bm2Re, &bm2Im);
			cdivid(aRe, aIm, bRe, bIm, &am1Re, &am1Im);
			bm1Re = 1.0;
			bm1Im = 0.0;
			fRe = am1Re;
			fIm = am1Im;
			j = j + 1;
			// calculate the coefficients for j odd
			alpha = (j - 1) * 0.5;
			double betaRe = xkRe;
			double betaIm = xkIm;
			complexMultiplication(betaRe, betaIm, am1Re, am1Im, &aRe, &aIm);
			aRe += alpha * am2Re;
			aIm += alpha * am2Im;
			complexMultiplication(betaRe, betaIm, bm1Re, bm1Im, &bRe, &bIm);
			bRe += alpha * bm2Re;
			bIm += alpha * bm2Im;
			cdivid(am1Re, am1Im, bRe, bIm, &am2Re, &am2Im);
			cdivid(bm1Re, bm1Im, bRe, bIm, &bm2Re, &bm2Im);
			cdivid(aRe, aIm, bRe, bIm, &am1Re, &am1Im);
			bm1Re = 1.0;
			bm1Im = 0.0;
			oldfRe = fRe;
			oldfIm = fIm;
			fRe = am1Re;
			fIm = am1Im;
			j = j + 1;
		}
		cplxexp(-xkRe, -xkIm, &tmpRe, &tmpIm);
		complexMultiplication(tmpRe, tmpIm, fRe, fIm, &tmpRe, &tmpIm);
		*yRe = tmpRe;
		*yIm = tmpIm - M_PI * ((xkRe < 0.0) && (xkIm == 0));
	}
}
double sineIntReal(double x)
{
	double sg = (x < 0.0) ? -1.0 : (x > 0.0);
	x = x * sg;
	double tmpRe, tmpIm;
	double yRe;
	double yIm;
	if (x < DBL_EPSILON)
		return 0.0;
	if (fabs(x) <= 8.2676619523664776)
	{
		double egamma = 0.57721566490153286061;
		double xkRe = 0.0;
		double xkIm = x;
		cplxlog(0.0, x, &tmpRe, &tmpIm);
		double ykRe = -egamma - tmpRe;
		double ykIm = -tmpIm;
		double j = 1.0;
		double ptermRe = 0.0;
		double ptermIm = x;
		double termRe = 0.0;
		double termIm = x;
		while (hypot(termRe, termIm) > eps(ykRe, ykIm))
		{
			ykRe = ykRe + termRe;
			ykIm = ykIm + termIm;
			j = j + 1.0;
			complexMultiplication(-xkRe, -xkIm, ptermRe / j, ptermIm / j, &ptermRe, &ptermIm);
			termRe = ptermRe / j;
			termIm = ptermIm / j;
		}
		yRe = ykRe;
		yIm = ykIm;
	}
	else
	{
		double xkRe = 0.0;
		double xkIm = x;
		double am2Re = 0.0;
		double am2Im = 0.0;
		double bm2Re = 1.0;
		double bm2Im = 0.0;
		double am1Re = 1.0;
		double am1Im = 0.0;
		double bm1Re = xkRe;
		double bm1Im = xkIm;
		double fRe, fIm;
		cdivid(am1Re, am1Im, bm1Re, bm1Im, &fRe, &fIm);
		double oldfRe = (double)FLT_MAX;
		double oldfIm = 0.0;
		int32_t j = 2;
		while (hypot(fRe - oldfRe, fIm - oldfIm) > (100.0 * DBL_EPSILON * hypot(fRe, fIm)))
		{
			// calculate the coefficients of the recursion formulas for j even
			double alpha = j * 0.5; // note: beta= 1
			//calculate A(j), B(j), and f(j)
			double aRe = am1Re + alpha * am2Re;
			double aIm = am1Im + alpha * am2Im;
			double bRe = bm1Re + alpha * bm2Re;
			double bIm = bm1Im + alpha * bm2Im;
			// save new normalized variables for next pass through the loop
			//  note: normalization to avoid overflow or underflow
			cdivid(am1Re, am1Im, bRe, bIm, &am2Re, &am2Im);
			cdivid(bm1Re, bm1Im, bRe, bIm, &bm2Re, &bm2Im);
			cdivid(aRe, aIm, bRe, bIm, &am1Re, &am1Im);
			bm1Re = 1.0;
			bm1Im = 0.0;
			fRe = am1Re;
			fIm = am1Im;
			j = j + 1;
			// calculate the coefficients for j odd
			alpha = (j - 1) * 0.5;
			double betaRe = xkRe;
			double betaIm = xkIm;
			complexMultiplication(betaRe, betaIm, am1Re, am1Im, &aRe, &aIm);
			aRe += alpha * am2Re;
			aIm += alpha * am2Im;
			complexMultiplication(betaRe, betaIm, bm1Re, bm1Im, &bRe, &bIm);
			bRe += alpha * bm2Re;
			bIm += alpha * bm2Im;
			cdivid(am1Re, am1Im, bRe, bIm, &am2Re, &am2Im);
			cdivid(bm1Re, bm1Im, bRe, bIm, &bm2Re, &bm2Im);
			cdivid(aRe, aIm, bRe, bIm, &am1Re, &am1Im);
			bm1Re = 1.0;
			bm1Im = 0.0;
			oldfRe = fRe;
			oldfIm = fIm;
			fRe = am1Re;
			fIm = am1Im;
			j = j + 1;
		}
		cplxexp(-xkRe, -xkIm, &tmpRe, &tmpIm);
		complexMultiplication(tmpRe, tmpIm, fRe, fIm, &tmpRe, &tmpIm);
		yRe = tmpRe;
		yIm = tmpIm - M_PI * ((xkRe < 0.0) && (xkIm == 0));
	}
	return (yIm + M_PI * 0.5) * sg;
}
int32_t firls(int32_t N, double *freq, double *M, double *weight, int32_t gridLen, int32_t filtertype, double *h)
{
	if (gridLen % 2)
	{
		char *msg = "Must have even length\n";
		EEL_STRING_STDOUT_WRITE(msg, strlen(msg));
		return -1;
	}
	int32_t i, j;
	double *W = (double*)malloc((gridLen >> 1) * sizeof(double));
	if (!weight)
	{
		for (i = 0; i < gridLen >> 1; i++)
			W[i] = 1.0;
	}
	else
		for (i = 0; i < gridLen >> 1; i++)
			W[i] = sqrt(weight[i]);
	int32_t ftype, differ;
	if (!filtertype)
	{
		ftype = 0;
		differ = 0;
	}
	else if (filtertype == 1)
	{
		ftype = 1;
		differ = 0;
	}
	else if (filtertype == 2)
	{
		ftype = 1;
		differ = 1;
	}
	N++;
	int32_t L = (N - 1) >> 1;
	int32_t Nodd = N % 2;
	double *F = (double*)malloc(gridLen * sizeof(double));
	double *m = (double*)malloc((L + 1) * sizeof(double));
	double *k = (double*)malloc((L + 1) * sizeof(double));
	double *I1 = (double*)malloc((L + 1) * (L + 1) * sizeof(double));
	double *I2 = (double*)malloc((L + 1) * (L + 1) * sizeof(double));
	double *G = (double*)malloc((L + 1) * (L + 1) * sizeof(double));
	double *b = (double*)malloc((L + 1) * sizeof(double));
	double *a = (double*)malloc((L + 1) * sizeof(double));
	for (i = 0; i < gridLen; i++)
		F[i] = freq[i] * 0.5;
	memset(G, 0, (L + 1) * (L + 1) * sizeof(double));
	memset(b, 0, (L + 1) * sizeof(double));
	int32_t size[2];
	int32_t LP1;
	if (!ftype)
	{
		LP1 = L + 1;
		if (!Nodd)
			for (i = 0; i < LP1; i++)
				m[i] = i + 0.5;
		else
			for (i = 0; i < LP1; i++)
				m[i] = i;
		memcpy(k, m, LP1 * sizeof(double));
		for (i = 0; i < LP1; i++)
		{
			for (j = 0; j < LP1; j++)
			{
				I1[i * LP1 + j] = k[i] + m[j];
				I2[i * LP1 + j] = k[i] - m[j];
			}
		}
		double b0;
		if (Nodd)
		{
			for (j = 0; j < L; j++)
				k[j] = k[j + 1];
			b0 = 0.0;
		}
		for (int32_t s = 0; s < gridLen; s += 2)
		{
			double m = (M[s + 1] - M[s]) / (F[s + 1] - F[s]); // slope
			double b1 = M[s] - m * F[s]; // y - intercept
			double tmp = fabs(W[(s + 1) / 2] * W[(s + 1) / 2]);
			if (Nodd)
				b0 = b0 + (b1*(F[s + 1] - F[s]) + m / 2.0 * (F[s + 1] * F[s + 1] - F[s] * F[s])) * tmp;
			for (i = 0; i < (Nodd == 1 ? L : LP1); i++)
			{
				b[i] += ((m / (4.0 * M_PI*M_PI)*(cos(M_2PI*k[i] * F[s + 1]) - cos(M_2PI*k[i] * F[s])) / (k[i] * k[i])) * tmp);
				b[i] += ((F[s + 1] * (m*F[s + 1] + b1)*sinc(2.0 * k[i] * F[s + 1]) - F[s] * (m*F[s] + b1)*sinc(2.0 * k[i] * F[s])) * tmp);
			}
			for (i = 0; i < LP1 * LP1; i++)
				G[i] += (0.5*F[s + 1] * (sinc(2.0 * I1[i] * F[s + 1]) + sinc(2.0 * I2[i] * F[s + 1])) - 0.5*F[s] * (sinc(2.0 * I1[i] * F[s]) + sinc(2.0 * I2[i] * F[s]))) *tmp;
		}
		if (Nodd)
		{
			for (j = L - 1; j >= 0; j--)
				b[j + 1] = b[j];
			b[0] = b0;
		}
		mldivide(G, LP1, LP1, b, LP1, 1, a, size);
		if (Nodd)
		{
			for (i = LP1 - 2; i >= 0; i--)
			{
				h[LP1 - 2 - i] = a[i + 1] * 0.5;
				h[i + LP1] = a[i + 1] * 0.5;
			}
			h[LP1 - 1] = a[0];
		}
		else
		{
			for (i = LP1 - 1; i >= 0; i--)
			{
				h[LP1 - 1 - i] = a[i] * 0.5;
				h[i + LP1] = a[i] * 0.5;
			}
		}
	}
	else
	{
		//  basis vectors are sin(2*M_PI*m*f) (see m below)
		if (Nodd)
		{
			LP1 = L;
			for (i = 0; i < LP1; i++)
				m[i] = i + 1.0;
		}
		else
		{
			LP1 = L + 1;
			for (i = 0; i < LP1; i++)
				m[i] = i + 0.5;
		}
		memset(G, 0, LP1 * LP1 * sizeof(double));
		memcpy(k, m, LP1 * sizeof(double));
		memset(b, 0, LP1 * sizeof(double));
		for (i = 0; i < LP1; i++)
		{
			for (j = 0; j < LP1; j++)
			{
				I1[i * LP1 + j] = k[i] + m[j];
				I2[i * LP1 + j] = k[i] - m[j];
			}
		}
		double tmpRe, tmpIm;
		for (int32_t s = 0; s < gridLen; s += 2)
		{
			double tmp = fabs(W[(s + 1) / 2] * W[(s + 1) / 2]);
			if (differ && (fabs(M[s]) + fabs(M[s + 1])) > 0)
			{
				if (F[s] == 0)
					F[s] = 1e-5; // avoid singularities
				double m = (M[s + 1] - M[s]) / (F[s + 1] - F[s]);
				double b1 = M[s] - m * F[s];
				for (i = 0; i < LP1; i++)
				{
					double snint1 = sineIntReal(M_2PI*k[i] * F[s + 1]) - sineIntReal(M_2PI*k[i] * F[s]);
					double csint1;
					cplxexpint(0.0, M_2PI*k[i] * F[s + 1], &csint1, &tmpIm);
					cplxexpint(0.0, M_2PI*k[i] * F[s], &tmpRe, &tmpIm);
					csint1 = -(csint1 - tmpRe);
					b[i] += (m*snint1 + b1 * M_2PI *k[i] * (-sinc(2.0 * k[i] * F[s + 1]) + sinc(2.0 * k[i] * F[s]) + csint1)) * tmp;
				}
				for (i = 0; i < LP1 * LP1; i++)
				{
					double snint1 = sineIntReal(M_2PI * F[s + 1] * -I2[i]);
					double snint2 = sineIntReal(M_2PI * F[s + 1] * I1[i]);
					double snint3 = sineIntReal(M_2PI * F[s] * -I2[i]);
					double snint4 = sineIntReal(M_2PI * F[s] * I1[i]);
					G[i] -= ((-0.5 * (cos(M_2PI*F[s + 1] * -I2[i]) / F[s + 1] - 2.0 * snint1*M_PI*I2[i] - cos(M_2PI*F[s + 1] * I1[i]) / F[s + 1] - 2.0 * snint2*M_PI*I1[i])) - (-0.5 * (cos(M_2PI*F[s] * -I2[i]) / F[s] - 2.0 * snint3*M_PI*I2[i] - cos(M_2PI*F[s] * I1[i]) / F[s] - 2.0 * snint4*M_PI*I1[i]))) * tmp;
				}
			}
			else
			{
				double m = (M[s + 1] - M[s]) / (F[s + 1] - F[s]);
				double b1 = M[s] - m * F[s];
				for (i = 0; i < LP1; i++)
				{
					b[i] += (m / (4 * M_PI*M_PI)*(sin(M_2PI*k[i] * F[s + 1]) - sin(M_2PI*k[i] * F[s])) / (k[i] * k[i])) * tmp;
					b[i] += (((m*F[s] + b1)*cos(M_2PI*k[i] * F[s]) - (m*F[s + 1] + b1)*cos(M_2PI*k[i] * F[s + 1])) / (M_2PI*k[i])) * tmp;
				}
				for (i = 0; i < LP1 * LP1; i++)
					G[i] += (0.5*F[s + 1] * (sinc(2.0 * I1[i] * F[s + 1]) - sinc(2.0 * I2[i] * F[s + 1])) - 0.5*F[s] * (sinc(2.0 * I1[i] * F[s]) - sinc(2.0 * I2[i] * F[s]))) * tmp;
			}
		}
		/*		for (i = 0; i < LP1; i++)
				{
					for (j = 0; j < LP1; j++)
						printf("%1.8lf ", G[i * LP1 + j]);
					printf("\n");
				}
				printf("\n");*/
		mldivide(G, LP1, LP1, b, LP1, 1, a, size);
		if (differ)
		{
			if (Nodd)
			{
				for (i = LP1 - 1; i >= 0; i--)
				{
					h[LP1 - 1 - i] = -a[i] * 0.5;
					h[i + LP1 + 1] = a[i] * 0.5;
				}
				h[LP1] = 0.0;
			}
			else
			{
				for (i = LP1 - 1; i >= 0; i--)
				{
					h[LP1 - 1 - i] = -a[i] * 0.5;
					h[i + LP1] = a[i] * 0.5;
				}
			}
		}
		else
		{
			if (Nodd)
			{
				for (i = LP1 - 1; i >= 0; i--)
				{
					h[LP1 - 1 - i] = a[i] * 0.5;
					h[i + LP1 + 1] = -a[i] * 0.5;
				}
				h[LP1] = 0.0;
			}
			else
			{
				for (i = LP1 - 1; i >= 0; i--)
				{
					h[LP1 - 1 - i] = a[i] * 0.5;
					h[i + LP1] = -a[i] * 0.5;
				}
			}
		}
	}
	free(W);
	free(F);
	free(m);
	free(k);
	free(I1);
	free(I2);
	free(G);
	free(b);
	free(a);
	return N;
}
