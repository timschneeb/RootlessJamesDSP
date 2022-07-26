#include <stdlib.h>
#include <math.h>
#include <float.h>
#include "../jdsp_header.h"
void cubic_hermite_init(cubic_hermite *cuher, double* x, double* y, double* dydx, int n)
{
	cuher->x_ = x;
	cuher->y_ = y;
	cuher->dydx_ = dydx;
	if (n < 4)
	{
		printf("Must be at least four data points.");
	}
	cuher->n = n;
	double x0 = cuher->x_[0];
	for (int i = 1; i < n; ++i)
	{
		double x1 = cuher->x_[i];
		if (x1 <= x0)
			printf("Abscissas must be listed in strictly increasing order x0 < x1 < ... < x_{n-1}");
		x0 = x1;
	}
}
double getValueAt(cubic_hermite *cuher, double x)
{
	// We need t := (x-x_k)/(x_{k+1}-x_k) \in [0,1) for this to work.
	// Sadly this neccessitates this loathesome check, otherwise we get t = 1 at x = xf.
	if (x == cuher->x_[cuher->n - 1])
		return cuher->y_[cuher->n - 1];
	int it = upper_bound(cuher->x_, cuher->n, x);
	if (it == 0)
		it = 1;
	if (it == cuher->n)
		it = cuher->n - 1;
	double x0 = cuher->x_[it - 1];
	double x1 = cuher->x_[it];
	double y0 = cuher->y_[it - 1];
	double y1 = cuher->y_[it];
	double s0 = cuher->dydx_[it - 1];
	double s1 = cuher->dydx_[it];
	double dx = (x1 - x0);
	double t = (x - x0) / dx;
	// See the section 'Representations' in the page
	// https://en.wikipedia.org/wiki/Cubic_Hermite_spline
	// This uses the factorized form:
	//double y = y0*(1+2*t)*(1-t)*(1-t) + dx*s0*t*(1-t)*(1-t)
	//       + y1*t*t*(3-2*t) + dx*s1*t*t*(t-1);
	// And then factorized further:
	return (1.0 - t)*(1.0 - t)*(y0*(1.0 + 2.0 * t) + s0 * (x - x0)) + t * t*(y1*(3.0 - 2.0 * t) + dx * s1*(t - 1.0));
}
double getDerivativeAt(cubic_hermite *cuher, double x)
{
	if (x == cuher->x_[cuher->n - 1])
		return cuher->dydx_[cuher->n - 1];
	int it = upper_bound(cuher->x_, cuher->n, x);
	if (it == 0)
		it = 1;
	if (it == cuher->n)
		it = cuher->n - 1;
	double x0 = cuher->x_[it - 1];
	double x1 = cuher->x_[it];
	double s0 = cuher->dydx_[it - 1];
	double s1 = cuher->dydx_[it];
	// Ridiculous linear interpolation. Fine for now:
	return (s0 * (x1 - x) + s1 * (x - x0)) / (x1 - x0);
}
void initIerper(ierper *intp, int n)
{
	intp->s = (double*)malloc(n * sizeof(double));
}
void freeIerper(ierper *intp)
{
	free(intp->s);
}
void pchip(ierper *intp, double * x, double * y, int n, int left_endpoint_derivative, int right_endpoint_derivative)
{
	if (n < 4)
	{
		printf("Must be at least four data points.");
	}
	if (left_endpoint_derivative)
	{
		// O(h) finite difference derivative:
		// This, I believe, is the only derivative guaranteed to be monotonic:
		intp->s[0] = (y[1] - y[0]) / (x[1] - x[0]);
	}
	else
		intp->s[0] = left_endpoint_derivative;
	for (int k = 1; k < n - 1; ++k)
	{
		double hkm1 = x[k] - x[k - 1];
		double dkm1 = (y[k] - y[k - 1]) / hkm1;
		double hk = x[k + 1] - x[k];
		double dk = (y[k + 1] - y[k]) / hk;
		double w1 = 2.0 * hk + hkm1;
		double w2 = hk + 2.0 * hkm1;
		if ((dk > 0 && dkm1 < 0.0) || (dk < 0.0 && dkm1 > 0.0) || dk == 0.0 || dkm1 == 0.0)
			intp->s[k] = 0.0;
		else
			intp->s[k] = (w1 + w2) / (w1 / dkm1 + w2 / dk);
	}
	// Quadratic extrapolation at the other end:
	if (right_endpoint_derivative)
		intp->s[n - 1] = (y[n - 1] - y[n - 2]) / (x[n - 1] - x[n - 2]);
	else
		intp->s[n - 1] = right_endpoint_derivative;
	cubic_hermite_init(&intp->cb, x, y, intp->s, n);
}
void makima(ierper *intp, double * x, double * y, int n, int left_endpoint_derivative, int right_endpoint_derivative)
{
	if (n < 4)
	{
		printf("Must be at least four data points.");
	}
	double m2 = (y[3] - y[2]) / (x[3] - x[2]);
	double m1 = (y[2] - y[1]) / (x[2] - x[1]);
	double m0 = (y[1] - y[0]) / (x[1] - x[0]);
	// Quadratic extrapolation: m_{-1} = 2m_0 - m_1:
	double mm1 = 2.0 * m0 - m1;
	// Quadratic extrapolation: m_{-2} = 2*m_{-1}-m_0:
	double mm2 = 2.0 * mm1 - m0;
	double w1 = fabs(m1 - m0) + fabs(m1 + m0) * 0.5;
	double w2 = fabs(mm1 - mm2) + fabs(mm1 + mm2) * 0.5;
	if (left_endpoint_derivative)
	{
		if ((w1 + w2) < DBL_EPSILON)
			intp->s[0] = 0.0;
		else
			intp->s[0] = (w1*mm1 + w2 * m0) / (w1 + w2);
	}
	else
		intp->s[0] = left_endpoint_derivative;

	w1 = fabs(m2 - m1) + fabs(m2 + m1) * 0.5;
	w2 = fabs(m0 - mm1) + fabs(m0 + mm1) * 0.5;
	if ((w1 + w2) < DBL_EPSILON)
		intp->s[1] = 0.0;
	else
		intp->s[1] = (w1*m0 + w2 * m1) / (w1 + w2);
	for (int i = 2; i < n - 2; ++i)
	{
		double mim2 = (y[i - 1] - y[i - 2]) / (x[i - 1] - x[i - 2]);
		double mim1 = (y[i] - y[i - 1]) / (x[i] - x[i - 1]);
		double mi = (y[i + 1] - y[i]) / (x[i + 1] - x[i]);
		double mip1 = (y[i + 2] - y[i + 1]) / (x[i + 2] - x[i + 1]);
		w1 = fabs(mip1 - mi) + fabs(mip1 + mi) * 0.5;
		w2 = fabs(mim1 - mim2) + fabs(mim1 + mim2) * 0.5;
		if ((w1 + w2) < DBL_EPSILON)
			intp->s[i] = 0.0;
		else
			intp->s[i] = (w1*mim1 + w2 * mi) / (w1 + w2);
	}
	// Quadratic extrapolation at the other end:
	double mnm4 = (y[n - 3] - y[n - 4]) / (x[n - 3] - x[n - 4]);
	double mnm3 = (y[n - 2] - y[n - 3]) / (x[n - 2] - x[n - 3]);
	double mnm2 = (y[n - 1] - y[n - 2]) / (x[n - 1] - x[n - 2]);
	double mnm1 = 2.0 * mnm2 - mnm3;
	double mn = 2.0 * mnm1 - mnm2;
	w1 = fabs(mnm1 - mnm2) + fabs(mnm1 + mnm2) * 0.5;
	w2 = fabs(mnm3 - mnm4) + fabs(mnm3 + mnm4) * 0.5;

	if ((w1 + w2) < DBL_EPSILON)
		intp->s[n - 2] = 0.0;
	else
		intp->s[n - 2] = (w1*mnm3 + w2 * mnm2) / (w1 + w2);

	w1 = fabs(mn - mnm1) + fabs(mn + mnm1) * 0.5;
	w2 = fabs(mnm2 - mnm3) + fabs(mnm2 + mnm3) * 0.5;
	if (right_endpoint_derivative)
	{
		if ((w1 + w2) < DBL_EPSILON)
			intp->s[n - 1] = 0.0;
		else
			intp->s[n - 1] = (w1*mnm2 + w2 * mnm1) / (w1 + w2);
	}
	else
		intp->s[n - 1] = right_endpoint_derivative;
	cubic_hermite_init(&intp->cb, x, y, intp->s, n);
}