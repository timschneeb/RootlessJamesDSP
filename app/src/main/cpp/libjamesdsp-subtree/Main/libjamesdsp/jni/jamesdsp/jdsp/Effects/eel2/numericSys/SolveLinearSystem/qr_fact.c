#include <math.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include "qr_fact.h"
#ifndef min
#define min(a,b) (((a)<(b))?(a):(b))
#endif
#ifndef max
#define max(a,b) (((a)>(b))?(a):(b))
#endif
void xzlarf(int32_t cols1, int32_t rows1, int32_t iv0, double tau, double C_data[], int32_t ic0, int32_t ldc, double work_data[])
{
	int32_t lastv;
	int32_t lastc;
	int32_t i;
	char exitg2;
	int32_t jy;
	int32_t i3;
	int32_t ia;
	int32_t ix;
	int32_t exitg1;
	double c;
	int32_t i4;
	int32_t ijA;
	if (tau != 0.0)
	{
		lastv = cols1;
		i = iv0 + cols1;
		while ((lastv > 0) && (C_data[i - 2] == 0.0))
		{
			lastv--;
			i--;
		}
		lastc = rows1 - 1;
		exitg2 = 0;
		while ((!exitg2) && (lastc + 1 > 0))
		{
			i = ic0 + lastc * ldc;
			ia = i;
			do
			{
				exitg1 = 0;
				if (ia <= (i + lastv) - 1)
				{
					if (C_data[ia - 1] != 0.0)
						exitg1 = 1;
					else
						ia++;
				}
				else
				{
					lastc--;
					exitg1 = 2;
				}
			} while (exitg1 == 0);
			if (exitg1 == 1)
				exitg2 = 1;
		}
	}
	else
	{
		lastv = 0;
		lastc = -1;
	}
	if (lastv > 0)
	{
		if (lastc + 1 != 0)
		{
			if (0 <= lastc)
				memset(&work_data[0], 0, (lastc + 1) * sizeof(double));
			i = 0;
			i3 = ic0 + ldc * lastc;
			for (jy = ic0; ldc < 0 ? jy >= i3 : jy <= i3; jy += ldc)
			{
				ix = iv0;
				c = 0.0;
				i4 = (jy + lastv) - 1;
				for (ia = jy; ia <= i4; ia++)
				{
					c += C_data[ia - 1] * C_data[ix - 1];
					ix++;
				}
				work_data[i] += c;
				i++;
			}
		}
		if (-tau != 0.0)
		{
			i = ic0 - 1;
			jy = 0;
			for (ia = 0; ia <= lastc; ia++)
			{
				if (work_data[jy] != 0.0)
				{
					c = work_data[jy] * -tau;
					ix = iv0;
					i3 = i + 1;
					i4 = lastv + i;
					for (ijA = i3; ijA <= i4; ijA++)
					{
						C_data[ijA - 1] += C_data[ix - 1] * c;
						ix++;
					}
				}
				jy++;
				i += ldc;
			}
		}
	}
}
void xswap(int32_t rows1, double x_data[], int32_t ix0, int32_t iy0)
{
	int32_t ix;
	int32_t iy;
	int32_t k;
	double temp;
	ix = ix0 - 1;
	iy = iy0 - 1;
	for (k = 0; k < rows1; k++)
	{
		temp = x_data[ix];
		x_data[ix] = x_data[iy];
		x_data[iy] = temp;
		ix++;
		iy++;
	}
}
double rt_hypotd(double u0, double u1)
{
	double y;
	double a;
	double b;
	a = fabs(u0);
	b = fabs(u1);
	if (a < b)
	{
		a /= b;
		y = b * sqrt(a * a + 1.0);
	}
	else if (a > b)
	{
		b /= a;
		y = a * sqrt(b * b + 1.0);
	}
	else
		y = a * sqrt(2.0);
	return y;
}
void xgerc(int m, int n, double alpha1, int ix0, const double y_data[], double A_data[], int ia0, int lda)
{
	double temp;
	int i;
	int ijA;
	int ix;
	int j;
	int jA;
	int jy;
	if (alpha1 != 0.0) {
		jA = ia0;
		jy = 0;
		for (j = 0; j < n; j++) {
			if (y_data[jy] != 0.0) {
				temp = y_data[jy] * alpha1;
				ix = ix0;
				i = m + jA;
				for (ijA = jA; ijA < i; ijA++) {
					A_data[ijA - 1] += A_data[ix - 1] * temp;
					ix++;
				}
			}

			jy++;
			jA += lda;
		}
	}
}
double xnrm2(int n, const double x_data[], int ix0)
{
	double absxk;
	double scale;
	double t;
	double y;
	int k;
	int kend;
	y = 0.0;
	if (n >= 1) {
		if (n == 1) {
			y = fabs(x_data[ix0 - 1]);
		}
		else {
			scale = 3.3121686421112381E-170;
			kend = (ix0 + n) - 1;
			for (k = ix0; k <= kend; k++) {
				absxk = fabs(x_data[k - 1]);
				if (absxk > scale) {
					t = scale / absxk;
					y = y * t * t + 1.0;
					scale = absxk;
				}
				else {
					t = absxk / scale;
					y += t * t;
				}
			}

			y = scale * sqrt(y);
		}
	}

	return y;
}
void xorgqr(int n, int k, double A_data[], int lda, const double tau_data[], double *work_data)
{
	double c;
	int b_i;
	int c_i;
	int exitg1;
	int i;
	int i1;
	int ia;
	int iac;
	int iaii;
	int ic0;
	int itau;
	int ix;
	int lastc;
	int lastv;
	char exitg2;
	i = n - 1;
	for (b_i = k; b_i <= i; b_i++) {
		ia = b_i * lda;
		i1 = n - 1;
		memset(&A_data[ia], 0, (i1 + 1) * sizeof(double));
		A_data[ia + b_i] = 1.0;
	}

	itau = k - 1;

	for (c_i = k; c_i >= 1; c_i--) {
		iaii = c_i + (c_i - 1) * lda;
		if (c_i < n) {
			A_data[iaii - 1] = 1.0;
			ic0 = iaii + lda;
			if (tau_data[itau] != 0.0) {
				lastv = (n - c_i) + 1;
				b_i = (iaii + n) - c_i;
				while ((lastv > 0) && (A_data[b_i - 1] == 0.0)) {
					lastv--;
					b_i--;
				}

				lastc = n - c_i;
				exitg2 = 0;
				while ((!exitg2) && (lastc > 0)) {
					b_i = ic0 + (lastc - 1) * lda;
					ia = b_i;
					do {
						exitg1 = 0;
						if (ia <= (b_i + lastv) - 1) {
							if (A_data[ia - 1] != 0.0)
								exitg1 = 1;
							else
								ia++;
						}
						else {
							lastc--;
							exitg1 = 2;
						}
					} while (exitg1 == 0);

					if (exitg1 == 1)
						exitg2 = 1;
				}
			}
			else {
				lastv = 0;
				lastc = 0;
			}

			if (lastv > 0) {
				if (lastc != 0) {
					memset(&work_data[0], 0, lastc * sizeof(double));

					b_i = 0;
					i = ic0 + lda * (lastc - 1);
					for (iac = ic0; lda < 0 ? iac >= i : iac <= i; iac += lda)
					{
						ix = iaii;
						c = 0.0;
						i1 = (iac + lastv) - 1;
						for (ia = iac; ia <= i1; ia++) {
							c += A_data[ia - 1] * A_data[ix - 1];
							ix++;
						}

						work_data[b_i] += c;
						b_i++;
					}
				}

				xgerc(lastv, lastc, -tau_data[itau], iaii, work_data, A_data, ic0, lda);
			}
		}

		if (c_i < n) {
			b_i = iaii + 1;
			i = (iaii + n) - c_i;
			for (ix = b_i; ix <= i; ix++)
				A_data[ix - 1] *= -tau_data[itau];
		}

		A_data[iaii - 1] = 1.0 - tau_data[itau];
		for (b_i = 0; b_i <= c_i - 2; b_i++)
			A_data[(iaii - b_i) - 2] = 0.0;

		itau--;
	}
}
void xgeqp3(double A_data[], const int A_size[2], double tau_data[], int perm[], double *vn1_data, double *vn2_data, double *work_data)
{
	double s;
	double smax;
	double temp2;
	int b_i;
	int exitg1;
	int i;
	int ic0;
	int ii;
	int ip1;
	int ix;
	int iy;
	int k;
	int lastv;
	int ma;
	int minmana;
	int minmn;
	int mmi;
	int nmi;
	int pvt;
	char exitg2;

	ma = A_size[1];
	pvt = A_size[1];
	minmn = A_size[0];
	if (pvt < minmn) {
		minmn = pvt;
	}

	for (pvt = 0; pvt < A_size[0]; pvt++) {
		smax = xnrm2(A_size[1], A_data, pvt * ma + 1);
		vn1_data[pvt] = smax;
		vn2_data[pvt] = smax;
	}

	for (i = 0; i < minmn; i++) {
		ip1 = i + 2;
		iy = i * ma;
		ii = iy + i;
		nmi = A_size[0] - i;
		mmi = A_size[1] - i;
		if (nmi < 1) {
			minmana = -1;
		}
		else
		{
			minmana = 0;
			if (nmi > 1) {
				ix = i;
				smax = fabs(vn1_data[i]);
				for (k = 2; k <= nmi; k++) {
					ix++;
					s = fabs(vn1_data[ix]);
					if (s > smax) {
						minmana = k - 1;
						smax = s;
					}
				}
			}
		}

		pvt = i + minmana;
		if (pvt + 1 != i + 1) {
			ix = pvt * ma;
			for (k = 0; k < A_size[1]; k++) {
				smax = A_data[ix];
				A_data[ix] = A_data[iy];
				A_data[iy] = smax;
				ix++;
				iy++;
			}

			minmana = perm[pvt];
			perm[pvt] = perm[i];
			perm[i] = minmana;
			vn1_data[pvt] = vn1_data[i];
			vn2_data[pvt] = vn2_data[i];
		}

		if (i + 1 < A_size[1]) {
			temp2 = A_data[ii];
			minmana = ii + 2;
			tau_data[i] = 0.0;
			if (mmi > 0) {
				smax = xnrm2(mmi - 1, A_data, ii + 2);
				if (smax != 0.0) {
					s = hypot(A_data[ii], smax);
					if (A_data[ii] >= 0.0)
						s = -s;
					if (fabs(s) < 1.0020841800044864E-292)
					{
						pvt = -1;
						b_i = ii + mmi;
						do {
							pvt++;
							for (k = minmana; k <= b_i; k++)
								A_data[k - 1] *= 9.9792015476736E+291;
							s *= 9.9792015476736E+291;
							temp2 *= 9.9792015476736E+291;
						} while (!(fabs(s) >= 1.0020841800044864E-292));

						s = hypot(temp2, xnrm2(mmi - 1, A_data, ii + 2));
						if (temp2 >= 0.0)
							s = -s;
						tau_data[i] = (s - temp2) / s;
						smax = 1.0 / (temp2 - s);
						for (k = minmana; k <= b_i; k++)
							A_data[k - 1] *= smax;
						for (k = 0; k <= pvt; k++)
							s *= 1.0020841800044864E-292;
						temp2 = s;
					}
					else
					{
						tau_data[i] = (s - A_data[ii]) / s;
						smax = 1.0 / (A_data[ii] - s);
						b_i = ii + mmi;
						for (k = minmana; k <= b_i; k++)
							A_data[k - 1] *= smax;
						temp2 = s;
					}
				}
			}

			A_data[ii] = temp2;
		}
		else {
			tau_data[i] = 0.0;
		}

		if (i + 1 < A_size[0]) {
			temp2 = A_data[ii];
			A_data[ii] = 1.0;
			ic0 = (ii + ma) + 1;
			if (tau_data[i] != 0.0) {
				lastv = mmi;
				minmana = (ii + mmi) - 1;
				while ((lastv > 0) && (A_data[minmana] == 0.0)) {
					lastv--;
					minmana--;
				}

				pvt = nmi - 1;
				exitg2 = 0;
				while ((!exitg2) && (pvt > 0))
				{
					minmana = ic0 + (pvt - 1) * ma;
					nmi = minmana;
					do {
						exitg1 = 0;
						if (nmi <= (minmana + lastv) - 1)
						{
							if (A_data[nmi - 1] != 0.0)
								exitg1 = 1;
							else
								nmi++;
						}
						else
						{
							pvt--;
							exitg1 = 2;
						}
					} while (exitg1 == 0);
					if (exitg1 == 1)
						exitg2 = 1;
				}
			}
			else {
				lastv = 0;
				pvt = 0;
			}

			if (lastv > 0) {
				if (pvt != 0) {
					memset(&work_data[0], 0, pvt * sizeof(double));

					iy = 0;
					b_i = ic0 + ma * (pvt - 1);
					for (k = ic0; ma < 0 ? k >= b_i : k <= b_i; k += ma) {
						ix = ii;
						smax = 0.0;
						minmana = (k + lastv) - 1;
						for (nmi = k; nmi <= minmana; nmi++) {
							smax += A_data[nmi - 1] * A_data[ix];
							ix++;
						}

						work_data[iy] += smax;
						iy++;
					}
				}

				xgerc(lastv, pvt, -tau_data[i], ii + 1, work_data, A_data, ic0, ma);
			}

			A_data[ii] = temp2;
		}

		for (pvt = ip1; pvt <= A_size[0]; pvt++) {
			minmana = i + (pvt - 1) * ma;
			smax = vn1_data[pvt - 1];
			if (smax != 0.0) {
				s = fabs(A_data[minmana]) / smax;
				s = 1.0 - s * s;
				if (s < 0.0)
					s = 0.0;

				temp2 = smax / vn2_data[pvt - 1];
				temp2 = s * (temp2 * temp2);
				if (temp2 <= 1.4901161193847656E-8) {
					if (i + 1 < A_size[1]) {
						smax = xnrm2(mmi - 1, A_data, minmana + 2);
						vn1_data[pvt - 1] = smax;
						vn2_data[pvt - 1] = smax;
					}
					else {
						vn1_data[pvt - 1] = 0.0;
						vn2_data[pvt - 1] = 0.0;
					}
				}
				else
					vn1_data[pvt - 1] = smax * sqrt(s);
			}
		}
	}
}
void preallocateMemQR(QR_heap *qr, int m, int n)
{
	qr->tmp1 = (double*)malloc(m * n * sizeof(double));
	qr->tmp2 = (double*)malloc(m * m * sizeof(double));
	unsigned int maxMN = max(m, n);
	if (m > n)
		qr->tau_data = (double*)malloc(m * sizeof(double));
	else
		qr->tau_data = (double*)malloc(min(m, n) * sizeof(double));
	qr->vn1_data = (double*)malloc(maxMN * sizeof(double));
	qr->vn2_data = (double*)malloc(maxMN * sizeof(double));
	qr->work_data = (double*)malloc(maxMN * sizeof(double));
}
void freeMemQR(QR_heap *qr)
{
	free(qr->tmp1);
	free(qr->tmp2);
	free(qr->tau_data);
	free(qr->vn1_data);
	free(qr->vn2_data);
	free(qr->work_data);
}
void QR_pivoting(QR_heap *qr, double *Amat, int m, int n, double *Qmat, double *Rmat, int *perm)
{
	unsigned int maxMN = max(m, n);
	if (m > n)
		memset(&qr->tau_data[0], 0, m * sizeof(double));
	else
		memset(&qr->tau_data[0], 0, min(m, n) * sizeof(double));
	memset(&qr->work_data[0], 0, maxMN * sizeof(double));
	memset(&qr->vn1_data[0], 0, maxMN * sizeof(double));
	memset(&qr->vn2_data[0], 0, maxMN * sizeof(double));
	int b_A_size[2];
	int i, j;

	for (i = 0; i < n; i++)
		perm[i] = i;
	if (m > n)
	{
		for (i = 0; i < n; i++)
			for (j = 0; j < m; j++)
				qr->tmp2[j + m * i] = Amat[i + n * j];

		for (i = n + 1; i <= m; i++)
			for (j = 0; j < m; j++)
				qr->tmp2[j + m * (i - 1)] = 0.0;
		b_A_size[1] = m;
		b_A_size[0] = m;
		xgeqp3(qr->tmp2, b_A_size, qr->tau_data, perm, qr->vn1_data, qr->vn2_data, qr->work_data);
		for (int i = 0; i < n; i++)
		{
			for (int j = 0; j <= i; j++)
				Rmat[i + n * j] = qr->tmp2[j + m * i];
			for (int j = i + 2; j <= m; j++)
				Rmat[(i + n * (j - 1))] = 0.0;
		}
		memset(&qr->work_data[0], 0, maxMN * sizeof(double));
		xorgqr(m, n, qr->tmp2, m, qr->tau_data, qr->work_data);
	}
	else
	{
		for (i = 0; i < n; i++)
			for (j = 0; j < m; j++)
				qr->tmp1[j + m * i] = Amat[i + n * j];

		b_A_size[1] = m;
		b_A_size[0] = n;
		xgeqp3(qr->tmp1, b_A_size, qr->tau_data, perm, qr->vn1_data, qr->vn2_data, qr->work_data);
		for (i = 0; i < m; i++)
		{
			for (j = 0; j <= i; j++)
				Rmat[i + n * j] = qr->tmp1[j + m * i];
			for (j = i + 2; j <= m; j++)
				Rmat[(i + n * (j - 1))] = 0.0;
		}
		for (i = m + 1; i <= n; i++)
		{
			for (j = 0; j < m; j++)
				Rmat[(n + i + n * (j - 1)) - 1] = qr->tmp1[j + m * (i - 1)];
		}
		memset(&qr->work_data[0], 0, maxMN * sizeof(double));
		xorgqr(m, m, qr->tmp1, m, qr->tau_data, qr->work_data);
		for (i = 0; i < m; i++)
			for (j = 0; j < m; j++)
				qr->tmp2[j + m * i] = qr->tmp1[j + m * i];
	}
	for (i = 0; i < m; i++)
		for (j = 0; j < m; j++)
			Qmat[j + m * i] = qr->tmp2[i + m * j];
}