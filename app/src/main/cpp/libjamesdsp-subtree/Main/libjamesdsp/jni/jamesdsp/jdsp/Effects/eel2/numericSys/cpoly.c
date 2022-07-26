// Jenkins-Traub complex polynomial root finder.
#include <math.h>
#include <float.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif
// COMPLEX DIVISION C = A/B, AVOIDING OVERFLOW
static void cdivid(const double ar, const double ai, const double br, const double bi, double *cr, double *ci)
{
	double r, d;
	if (br == 0 && bi == 0)
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
// MODULUS OF A COMPLEX NUMBER AVOIDING OVERFLOW
static double cmod(const double r, const double i)
{
	double ar, ai;
	ar = fabs(r);
	ai = fabs(i);
	if (ar < ai)
		return ai * sqrt(1.0 + pow((ar / ai), 2.0));
	if (ar > ai)
		return ar * sqrt(1.0 + pow((ai / ar), 2.0));
	return ar * sqrt(2.0);
}
// EVALUATES A POLYNOMIAL  P  AT  S  BY THE HORNER RECURRENCE
// PLACING THE PARTIAL SUMS IN Q AND THE COMPUTED VALUE IN PV
static void polyev(const int32_t nn, const double sr, const double si, const double pr[], const double pi[], double qr[], double qi[], double *pvr, double *pvi)
{
	int32_t i;
	double t;

	qr[0] = pr[0];
	qi[0] = pi[0];
	*pvr = qr[0];
	*pvi = qi[0];

	for (i = 1; i <= nn; i++)
	{
		t = (*pvr) * sr - (*pvi) * si + pr[i];
		*pvi = (*pvr) * si + (*pvi) * sr + pi[i];
		*pvr = t;
		qr[i] = *pvr;
		qi[i] = *pvi;
	}
}
// BOUNDS THE ERROR IN EVALUATING THE POLYNOMIAL BY THE HORNER RECURRENCE.
// QR,QI - THE PARTIAL SUMS
// MS    -MODULUS OF THE POINT
// MP    -MODULUS OF POLYNOMIAL VALUE
static double errev(const int32_t nn, const double qr[], const double qi[], const double ms, const double mp, const double mre)
{
	double e = cmod(qr[0], qi[0]) * mre / (DBL_EPSILON + mre);
	for (int32_t i = 0; i <= nn; i++)
		e = e * ms + cmod(qr[i], qi[i]);
	return e * (DBL_EPSILON + mre) - mp * mre;
}
// CAUCHY COMPUTES A LOWER BOUND ON THE MODULI OF THE ZEROS OF A
// POLYNOMIAL - PT IS THE MODULUS OF THE COEFFICIENTS.
static void cauchy(const int32_t nn, double pt[], double q[], double *fn_val)
{
	int32_t i, n;
	double x, xm, f, dx, df;
	pt[nn] = -pt[nn];
	// Compute upper estimate bound
	n = nn;
	x = exp(log(-pt[nn]) - log(pt[0])) / n;
	if (pt[n - 1] != 0)
	{
		// Newton step at the origin is better, use it
		xm = -pt[nn] / pt[n - 1];
		if (xm < x) x = xm;
	}
	// Chop the interval (0,x) until f < 0
	while (1)
	{
		xm = x * 0.1;
		f = pt[0];
		for (i = 1; i <= nn; i++)
			f = f * xm + pt[i];
		if (f <= 0)
			break;
		x = xm;
	}
	dx = x;
	// Do Newton iteration until x converges to two decimal places
	while (fabs(dx / x) > 0.005)
	{
		q[0] = pt[0];
		for (i = 1; i <= nn; i++)
			q[i] = q[i - 1] * x + pt[i];
		f = q[nn];
		df = q[0];
		for (i = 1; i < n; i++)
			df = df * x + q[i];
		dx = f / df;
		x -= dx;
	}
	*fn_val = x;
}
// RETURNS A SCALE FACTOR TO MULTIPLY THE COEFFICIENTS OF THE POLYNOMIAL.
// THE SCALING IS DONE TO AVOID OVERFLOW AND TO AVOID UNDETECTED UNDERFLOW
// INTERFERING WITH THE CONVERGENCE CRITERION.  THE FACTOR IS A POWER OF THE
// BASE.
// PT - MODULUS OF COEFFICIENTS OF P
static double scale(const int32_t nn, const double pt[])
{
	int32_t i, pexponent;
	double hi, lo, max, min, x, sc;
	double fn_val;

	// Find largest and smallest moduli of coefficients
	hi = sqrt(DBL_MAX);
	lo = DBL_MIN / DBL_EPSILON;
	max = 0;
	min = DBL_MAX;

	for (i = 0; i <= nn; i++)
	{
		x = pt[i];
		if (x > max) max = x;
		if (x != 0 && x < min) min = x;
	}
	// Scale only if there are very large or very small components
	fn_val = 1;
	if (min >= lo && max <= hi) return fn_val;
	x = lo / min;
	if (x <= 1.0)
		sc = 1.0 / (sqrt(max)* sqrt(min));
	else
	{
		sc = x;
		if (DBL_MAX / sc > max) sc = 1.0;
	}
	pexponent = (int32_t)(log(sc) / log(2.0) + 0.5);
	fn_val = pow(2.0, pexponent);
	return fn_val;
}
typedef struct
{
	double sr, si, tr, ti, pvr, pvi, mre;
	int32_t nn;
	double *pr, *pi, *hr, *hi, *qpr, *qpi, *qhr, *qhi, *shr, *shi;
} polyDat;
// COMPUTES  T = -P(S)/H(S).
// BOOL   - LOGICAL, SET TRUE IF H(S) IS ESSENTIALLY ZERO.
static void calct(polyDat *poD, int32_t *bol)
{
	int32_t n;
	double hvr, hvi;
	n = poD->nn;
	// evaluate h(s)
	polyev(n - 1, poD->sr, poD->si, poD->hr, poD->hi, poD->qhr, poD->qhi, &hvr, &hvi);
	*bol = cmod(hvr, hvi) <= DBL_EPSILON * 10.0 * cmod(poD->hr[n - 1], poD->hi[n - 1]) ? 1 : 0;
	if (!*bol)
	{
		cdivid(-poD->pvr, -poD->pvi, hvr, hvi, &poD->tr, &poD->ti);
		return;
	}
	poD->tr = 0;
	poD->ti = 0;
}
// CALCULATES THE NEXT SHIFTED H POLYNOMIAL.
// BOOL   -  LOGICAL, IF .TRUE. H(S) IS ESSENTIALLY ZERO
static void nexth(polyDat *poD, const int32_t bol)
{
	int32_t j, n;
	double t1, t2;
	n = poD->nn;
	if (!bol)
	{
		for (j = 1; j < n; j++)
		{
			t1 = poD->qhr[j - 1];
			t2 = poD->qhi[j - 1];
			poD->hr[j] = poD->tr * t1 - poD->ti * t2 + poD->qpr[j];
			poD->hi[j] = poD->tr * t2 + poD->ti * t1 + poD->qpi[j];
		}
		poD->hr[0] = poD->qpr[0];
		poD->hi[0] = poD->qpi[0];
		return;
	}
	// If h[s] is zero replace H with qh
	for (j = 1; j < n; j++)
	{
		poD->hr[j] = poD->qhr[j - 1];
		poD->hi[j] = poD->qhi[j - 1];
	}
	poD->hr[0] = 0;
	poD->hi[0] = 0;
}
// CARRIES OUT THE THIRD STAGE ITERATION.
// L3 - LIMIT OF STEPS IN STAGE 3.
// ZR,ZI   - ON ENTRY CONTAINS THE INITIAL ITERATE, IF THE
//           ITERATION CONVERGES IT CONTAINS THE FINAL ITERATE ON EXIT.
// CONV    -  .TRUE. IF ITERATION CONVERGES
static void vrshft(polyDat *poD, const int32_t l3, double *zr, double *zi, int32_t *conv)
{
	int32_t b, bol;
	int32_t i, j;
	double mp, ms, omp, relstp, r1, r2, tp;

	*conv = 0;
	b = 0;
	poD->sr = *zr;
	poD->si = *zi;

	// Main loop for stage three
	for (i = 1; i <= l3; i++)
	{
		// Evaluate P at S and test for convergence
		polyev(poD->nn, poD->sr, poD->si, poD->pr, poD->pi, poD->qpr, poD->qpi, &poD->pvr, &poD->pvi);
		mp = cmod(poD->pvr, poD->pvi);
		ms = cmod(poD->sr, poD->si);
		if (mp <= 20.0 * errev(poD->nn, poD->qpr, poD->qpi, ms, mp, poD->mre))
		{
			// Polynomial value is smaller in value than a bound onthe error
			// in evaluationg P, terminate the ietartion
			*conv = 1;
			*zr = poD->sr;
			*zi = poD->si;
			return;
		}
		if (i != 1)
		{
			if (!(b || mp < omp || relstp >= 0.05))
			{
				// Iteration has stalled. Probably a cluster of zeros. Do 5 fixed 
				// shift steps into the cluster to force one zero to dominate
				tp = relstp;
				b = 1;
				if (relstp < DBL_EPSILON) tp = DBL_EPSILON;
				r1 = sqrt(tp);
				r2 = poD->sr * (1 + r1) - poD->si * r1;
				poD->si = poD->sr * r1 + poD->si * (1 + r1);
				poD->sr = r2;
				polyev(poD->nn, poD->sr, poD->si, poD->pr, poD->pi, poD->qpr, poD->qpi, &poD->pvr, &poD->pvi);
				for (j = 1; j <= 5; j++)
				{
					calct(poD, &bol);
					nexth(poD, bol);
				}
				omp = DBL_MAX;
				calct(poD, &bol);
				nexth(poD, bol);
				calct(poD, &bol);
				if (!bol)
				{
					relstp = cmod(poD->tr, poD->ti) / cmod(poD->sr, poD->si);
					poD->sr += poD->tr;
					poD->si += poD->ti;
				}
				continue;
			}
			// Exit if polynomial value increase significantly
			if (mp * 0.1 > omp) return;
		}
		omp = mp;

		// Calculate next iterate
		calct(poD, &bol);
		nexth(poD, bol);
		calct(poD, &bol);
		if (!bol)
		{
			relstp = cmod(poD->tr, poD->ti) / cmod(poD->sr, poD->si);
			poD->sr += poD->tr;
			poD->si += poD->ti;
		}
	}
}
// COMPUTES L2 FIXED-SHIFT H POLYNOMIALS AND TESTS FOR CONVERGENCE.
// INITIATES A VARIABLE-SHIFT ITERATION AND RETURNS WITH THE
// APPROXIMATE ZERO IF SUCCESSFUL.
// L2 - LIMIT OF FIXED SHIFT STEPS
// ZR,ZI - APPROXIMATE ZERO IF CONV IS .TRUE.
// CONV  - LOGICAL INDICATING CONVERGENCE OF STAGE 3 ITERATION
static void fxshft(polyDat *poD, const int32_t l2, double *zr, double *zi, int32_t *conv)
{
	int32_t i, j, n;
	int32_t test, pasd, bol;
	double otr, oti, svsr, svsi;

	n = poD->nn;
	polyev(poD->nn, poD->sr, poD->si, poD->pr, poD->pi, poD->qpr, poD->qpi, &poD->pvr, &poD->pvi);
	test = 1;
	pasd = 0;

	// Calculate first T = -P(S)/H(S)
	calct(poD, &bol);

	// Main loop for second stage
	for (j = 1; j <= l2; j++)
	{
		otr = poD->tr;
		oti = poD->ti;

		// Compute the next H Polynomial and new t
		nexth(poD, bol);
		calct(poD, &bol);
		*zr = poD->sr + poD->tr;
		*zi = poD->si + poD->ti;

		// Test for convergence unless stage 3 has failed once or this
		// is the last H Polynomial
		if (!(bol || !test || j == 12))
		{
			if (cmod(poD->tr - otr, poD->ti - oti) < 0.5 * cmod(*zr, *zi))
			{
				if (pasd)
				{
					// The weak convergence test has been passwed twice, start the third stage
					// Iteration, after saving the current H polynomial and shift
					for (i = 0; i < n; i++)
					{
						poD->shr[i] = poD->hr[i];
						poD->shi[i] = poD->hi[i];
					}
					svsr = poD->sr;
					svsi = poD->si;
					vrshft(poD, 10, zr, zi, conv);
					if (*conv)
						return;
					//The iteration failed to converge. Turn off testing and restore h,s,pv and T
					test = 0;
					for (i = 0; i < n; i++)
					{
						poD->hr[i] = poD->shr[i];
						poD->hi[i] = poD->shi[i];
					}
					poD->sr = svsr;
					poD->si = svsi;
					polyev(poD->nn, poD->sr, poD->si, poD->pr, poD->pi, poD->qpr, poD->qpi, &poD->pvr, &poD->pvi);
					calct(poD, &bol);
					continue;
				}
				pasd = 1;
			}
			else
			{
				pasd = 0;
			}
		}
	}
	// Attempt an iteration with final H polynomial from second stage
	vrshft(poD, 10, zr, zi, conv);
}
int32_t cpoly(double *polyRe, double *polyIm, int32_t degree, double *zeror, double *zeroi)
{
	int32_t cnt1, cnt2, idnn2, i, conv, j, jj, n, nm1;
	double xx, yy, cosr, sinr, xxx, zr, zi, bnd, xni, t1, t2;
	const double RADFAC = M_PI / 180.0; // Degrees-to-radians conversion factor = poD->pi/180
	polyDat poD;
	poD.mre = 2.0 * sqrt(2.0) * DBL_EPSILON;
	xx = sqrt(0.5);
	yy = -xx;
	cosr = cos(94.0*RADFAC);
	sinr = sin(94.0*RADFAC);
	if (degree <= 0)
		return -2;
	// Remove leading zeros
	int32_t counter = 0;
	double *opr = (double*)malloc(((degree + 1) << 1) * sizeof(double));
	double *opi = opr + (degree + 1);
	memcpy(opr, polyRe, (degree + 1) * sizeof(double));
	memcpy(opi, polyIm, (degree + 1) * sizeof(double));
	for (i = 0; i < degree; i++)
	{
		if (opr[i] == 0.0 && opi[i] == 0.0)
			counter++;
		else
			break;
	}
	for (int32_t idx = 0; idx < counter; idx++)
	{
		for (i = 0; i < degree; i++)
		{
			opr[i] = opr[i + 1];
			opi[i] = opi[i + 1];
		}
	}
	degree -= counter;
	poD.nn = degree;

	// Algorithm fails if the leading coefficient is zero, tell upper level we may need to remove leading zero?
	if (opr[0] == 0.0 && opi[0] == 0.0)
	{
		free(opr);
		return -1;
	}

	// Allocate arrays
	int32_t degreePlus1 = degree + 1;
	poD.pr = (double*)malloc(degreePlus1 * 10 * sizeof(double));
	poD.pi = poD.pr + degreePlus1;
	poD.hr = poD.pi + degreePlus1;
	poD.hi = poD.hr + degreePlus1;
	poD.qpr = poD.hi + degreePlus1;
	poD.qpi = poD.qpr + degreePlus1;
	poD.qhr = poD.qpi + degreePlus1;
	poD.qhi = poD.qhr + degreePlus1;
	poD.shr = poD.qhi + degreePlus1;
	poD.shi = poD.shr + degreePlus1;

	// Remove the zeros at the origin if any
	while (opr[poD.nn] == 0 && opi[poD.nn] == 0)
	{
		idnn2 = degree - poD.nn;
		zeror[idnn2] = 0;
		zeroi[idnn2] = 0;
		poD.nn--;
	}

	// Make a copy of the coefficients
	for (i = 0; i <= poD.nn; i++)
	{
		poD.pr[i] = opr[i];
		poD.pi[i] = opi[i];
		poD.shr[i] = cmod(poD.pr[i], poD.pi[i]);
	}

	// Scale the polynomial
	bnd = scale(poD.nn, poD.shr);
	if (bnd != 1)
		for (i = 0; i <= poD.nn; i++)
		{
			poD.pr[i] *= bnd;
			poD.pi[i] *= bnd;
		}
	while (poD.nn > 0)
	{
		int32_t stop = 0;
		if (poD.nn <= 1)
		{
			cdivid(-poD.pr[1], -poD.pi[1], poD.pr[0], poD.pi[0], &zeror[degree - 1], &zeroi[degree - 1]);
			// Deallocate arrays
			free(poD.pr);
			free(opr);
			return degree;
		}

		// Calculate bnd, alower bound on the modulus of the zeros
		for (i = 0; i <= poD.nn; i++)
			poD.shr[i] = cmod(poD.pr[i], poD.pi[i]);

		cauchy(poD.nn, poD.shr, poD.shi, &bnd);

		// Outer loop to control 2 Major passes with different sequences of shifts
		for (cnt1 = 1; (cnt1 <= 2) && !stop; cnt1++)
		{
			// First stage calculation, no shift
			// COMPUTES  THE DERIVATIVE  POLYNOMIAL AS THE INITIAL H
			// POLYNOMIAL AND COMPUTES L1 NO-SHIFT H POLYNOMIALS.
			const int32_t l1 = 5;
			n = poD.nn;
			nm1 = n - 1;
			for (i = 0; i < n; i++)
			{
				xni = poD.nn - i;
				poD.hr[i] = xni * poD.pr[i] / n;
				poD.hi[i] = xni * poD.pi[i] / n;
			}
			for (jj = 1; jj <= l1; jj++)
			{
				if (cmod(poD.hr[n - 1], poD.hi[n - 1]) > DBL_EPSILON * 10.0 * cmod(poD.pr[n - 1], poD.pi[n - 1]))
				{
					cdivid(-poD.pr[poD.nn], -poD.pi[poD.nn], poD.hr[n - 1], poD.hi[n - 1], &poD.tr, &poD.ti);
					for (i = 0; i < nm1; i++)
					{
						j = poD.nn - i - 1;
						t1 = poD.hr[j - 1];
						t2 = poD.hi[j - 1];
						poD.hr[j] = poD.tr * t1 - poD.ti * t2 + poD.pr[j];
						poD.hi[j] = poD.tr * t2 + poD.ti * t1 + poD.pi[j];
					}
					poD.hr[0] = poD.pr[0];
					poD.hi[0] = poD.pi[0];
				}
				else
				{
					// If the constant term is essentially zero, shift H coefficients
					for (i = 0; i < nm1; i++)
					{
						j = poD.nn - i - 1;
						poD.hr[j] = poD.hr[j - 1];
						poD.hi[j] = poD.hi[j - 1];
					}
					poD.hr[0] = 0.0;
					poD.hi[0] = 0.0;
				}
			}
			// Inner loop to select a shift
			for (cnt2 = 1; (cnt2 <= 9) && !stop; cnt2++)
			{
				// Shift is chosen with modulus bnd and amplitude rotated by 94 degree from the previous shif
				xxx = cosr * xx - sinr * yy;
				yy = sinr * xx + cosr * yy;
				xx = xxx;
				poD.sr = bnd * xx;
				poD.si = bnd * yy;

				// Second stage calculation, fixed shift
				fxshft(&poD, 10 * cnt2, &zr, &zi, &conv);
				if (conv)
				{
					// The second stage jumps directly to the third stage ieration
					// If successful the zero is stored and the polynomial deflated
					idnn2 = degree - poD.nn;
					zeror[idnn2] = zr;
					zeroi[idnn2] = zi;
					poD.nn--;
					for (i = 0; i <= poD.nn; i++)
					{
						poD.pr[i] = poD.qpr[i];
						poD.pi[i] = poD.qpi[i];
					}
					stop = 1;
					break;
				}
				// If the iteration is unsuccessful another shift is chosen
			}
			// if 9 shifts fail, the outer loop is repeated with another sequence of shifts
			if (stop)
				break;
		}
		// The zerofinder has failed on two major passes
		// return empty handed with the number of roots found (less than the original degree)
		if (!stop)
		{
			degree -= poD.nn;
			// Deallocate arrays
			free(poD.pr);
			free(opr);
			return degree;
		}
	}
	// Deallocate arrays
	free(poD.pr);
	free(opr);
	return degree;
}
