#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <string.h>
#include "../../ns-eel.h"
#include "../quadprog.h"
extern double xnrm2(int n, const double x_data[], int ix0);
extern void xzlarf(int32_t cols1, int32_t rows1, int32_t iv0, double tau, double C_data[], int32_t ic0, int32_t ldc, double work_data[]);
extern double rt_hypotd(double u0, double u1);
extern void xswap(int32_t rows1, double x_data[], int32_t ix0, int32_t iy0);
void mrdivide(const double A[], const int32_t rows1, const int32_t cols1, const double b[], const int32_t rows2, const int32_t cols2, double Y[], int32_t Y_size[2])
{
	if (cols1 != cols2)
	{
		char *msg = "Matrix dimensions must agree.\n";
		EEL_STRING_STDOUT_WRITE(msg, strlen(msg));
		return;
	}
	int32_t yk, jA, i, k, j, nb, mmj_tmp, jj, jp1j, ix, minmn, maxmn, rankR;
	int32_t b_n;
	int32_t mn;
	int32_t i_i;
	int32_t nmi;
	int32_t mmi;
	double beta1;
	int32_t knt;
	double smax, s;
	size_t bufferSize = ((rows1 * cols1) + (rows2 * 3) + cols1 + (rows2 * cols2) + (rows1 * cols1)) * sizeof(double) + (rows2 * 2 + cols1) * sizeof(int32_t);
	char *workingBuffer = (char*)malloc(bufferSize * sizeof(char));
	double *b_B_data = (double*)workingBuffer;
	double *qrWorkBuf = b_B_data + rows1 * cols1;
	double *tau_data = qrWorkBuf + rows2 * 3;
	double *b_A_data = tau_data + cols1;
	double *B_data = b_A_data + rows2 * cols2;
	int32_t *y_data = (int32_t*)(B_data + rows1 * cols1);
	int32_t *jpvt_data = y_data + rows2;
	int32_t *ipiv_data = jpvt_data + rows2;
	if (rows2 == cols2)
	{
		for (i = 0; i < cols2; i++)
			for (j = 0; j < rows2; j++)
				b_A_data[j + rows2 * i] = b[i + cols2 * j];
		for (i = 0; i < cols1; i++)
			for (j = 0; j < rows1; j++)
				B_data[j + rows1 * i] = A[i + cols1 * j];
		y_data[0] = 1;
		yk = 1;
		for (k = 2; k <= cols2; k++)
		{
			yk++;
			y_data[k - 1] = yk;
		}
		if (0 <= cols2 - 1)
			memcpy(&ipiv_data[0], &y_data[0], cols2 * sizeof(int32_t));
		minmn = cols2 + 1;
		for (j = 0; j < cols2 - 1; j++)
		{
			mmj_tmp = cols2 - j;
			jA = j * (cols2 + 1);
			jj = j * minmn;
			jp1j = jA + 2;
			yk = 0;
			ix = jA;
			smax = fabs(b_A_data[jA]);
			for (k = 2; k <= mmj_tmp; k++)
			{
				ix++;
				s = fabs(b_A_data[ix]);
				if (s > smax)
				{
					yk = k - 1;
					smax = s;
				}
			}
			if (b_A_data[jj + yk] != 0.0)
			{
				if (yk != 0)
				{
					yk += j;
					ipiv_data[j] = yk + 1;
					ix = j;
					for (k = 0; k < cols2; k++)
					{
						smax = b_A_data[ix];
						b_A_data[ix] = b_A_data[yk];
						b_A_data[yk] = smax;
						ix += cols2;
						yk += cols2;
					}
				}
				for (i = jp1j; i <= jj + mmj_tmp; i++)
					b_A_data[i - 1] /= b_A_data[jj];
			}
			yk = jA + cols2;
			jA = jj + minmn;
			for (jp1j = 0; jp1j <= mmj_tmp - 2; jp1j++)
			{
				smax = b_A_data[yk];
				if (b_A_data[yk] != 0.0)
				{
					ix = jj + 1;
					i = mmj_tmp + jA;
					for (nb = jA + 1; nb < i; nb++)
					{
						b_A_data[nb - 1] += b_A_data[ix] * -smax;
						ix++;
					}
				}
				yk += cols2;
				jA += cols2;
			}
		}
		for (j = 0; j < cols2; j++)
		{
			jA = rows1 * j - 1;
			yk = cols2 * j;
			for (k = 0; k < j; k++)
			{
				jp1j = rows1 * k;
				smax = b_A_data[k + yk];
				if (smax != 0.0)
					for (i = 0; i < rows1; i++)
						B_data[(i + jA) + 1] -= smax * B_data[i + jp1j];
			}
			smax = 1.0 / b_A_data[j + yk];
			for (i = 0; i < rows1; i++)
				B_data[(i + jA) + 1] *= smax;
		}
		for (j = cols2; j >= 1; j--)
		{
			for (k = j + 1; k <= cols2; k++)
			{
				jp1j = rows1 * (k - 1);
				smax = b_A_data[k + (cols2 * (j - 1) - 1)];
				if (smax != 0.0)
					for (i = 0; i < rows1; i++)
						B_data[i + rows1 * (j - 1)] -= smax * B_data[i + jp1j];
			}
		}
		for (jA = cols2 - 1; jA >= 1; jA--)
		{
			i = ipiv_data[jA - 1];
			if (i != jA) {
				for (yk = 0; yk < rows1; yk++)
				{
					jp1j = yk + rows1 * (jA - 1);
					smax = B_data[jp1j];
					ix = yk + rows1 * (i - 1);
					B_data[jp1j] = B_data[ix];
					B_data[ix] = smax;
				}
			}
		}
		Y_size[1] = cols1;
		Y_size[0] = rows1;
		for (i = 0; i < rows1; i++)
			for (j = 0; j < cols1; j++)
				Y[j + cols1 * i] = B_data[i + rows1 * j];
	}
	else
	{
		// QR solve
		for (i = 0; i < rows2; i++)
			for (j = 0; j < cols2; j++)
				b_A_data[j + cols2 * i] = b[j + cols2 * i];
		double *vn1_data = qrWorkBuf + rows2;
		double *vn2_data = vn1_data + rows2;
		b_n = cols2;
		mn = rows2;
		if (b_n < mn)
			mn = b_n;
		jpvt_data[0] = 1;
		yk = 1;
		for (k = 2; k <= rows2; k++)
			jpvt_data[k - 1] = ++yk;
		if (0 <= rows2 - 1)
			memset(&qrWorkBuf[0], 0, rows2 * (int32_t)sizeof(double));
		k = 1;
		for (yk = 0; yk < rows2; yk++)
		{
			smax = xnrm2(cols2, b_A_data, k);
			vn1_data[yk] = smax;
			vn2_data[yk] = smax;
			k += cols2;
		}
		for (i = 0; i < mn; i++)
		{
			i_i = i + i * cols2;
			nmi = rows2 - i;
			mmi = (cols2 - i) - 1;
			if (nmi < 1)
				b_n = 0;
			else
			{
				b_n = 1;
				if (nmi > 1)
				{
					yk = i;
					smax = fabs(vn1_data[i]);
					for (k = 2; k <= nmi; k++)
					{
						yk++;
						s = fabs(vn1_data[yk]);
						if (s > smax)
						{
							b_n = k;
							smax = s;
						}
					}
				}
			}
			b_n = (i + b_n) - 1;
			if (b_n + 1 != i + 1)
			{
				xswap(cols2, b_A_data, 1 + cols2 * b_n, 1 + cols2 * i);
				yk = jpvt_data[b_n];
				jpvt_data[b_n] = jpvt_data[i];
				jpvt_data[i] = yk;
				vn1_data[b_n] = vn1_data[i];
				vn2_data[b_n] = vn2_data[i];
			}
			if (i + 1 < cols2)
			{
				s = b_A_data[i_i];
				b_n = i_i + 2;
				tau_data[i] = 0.0;
				if (1 + mmi > 0)
				{
					smax = xnrm2(mmi, b_A_data, i_i + 2);
					if (smax != 0.0)
					{
						beta1 = rt_hypotd(b_A_data[i_i], smax);
						if (b_A_data[i_i] >= 0.0)
							beta1 = -beta1;
						if (fabs(beta1) < DBL_MIN)
						{
							knt = -1;
							do
							{
								knt++;
								for (k = b_n; k <= i_i + mmi + 1; k++)
									b_A_data[k - 1] *= DBL_MAX;
								beta1 *= DBL_MAX;
								s *= DBL_MAX;
							} while (!(fabs(beta1) >= DBL_MIN));
							beta1 = rt_hypotd(s, xnrm2(mmi, b_A_data, i_i + 2));
							if (s >= 0.0)
								beta1 = -beta1;
							tau_data[i] = (beta1 - s) / beta1;
							smax = 1.0 / (s - beta1);
							for (k = b_n; k <= i_i + mmi + 1; k++)
								b_A_data[k - 1] *= smax;
							for (k = 0; k <= knt; k++)
								beta1 *= DBL_MIN;
							s = beta1;
						}
						else
						{
							tau_data[i] = (beta1 - b_A_data[i_i]) / beta1;
							smax = 1.0 / (b_A_data[i_i] - beta1);
							for (k = b_n; k <= i_i + mmi + 1; k++)
								b_A_data[k - 1] *= smax;
							s = beta1;
						}
					}
				}
				b_A_data[i_i] = s;
			}
			else
				tau_data[i] = 0.0;
			if (i + 1 < rows2)
			{
				s = b_A_data[i_i];
				b_A_data[i_i] = 1.0;
				xzlarf(1 + mmi, nmi - 1, i_i + 1, tau_data[i], b_A_data, (i + (i + 1) * cols2) + 1, cols2, qrWorkBuf);
				b_A_data[i_i] = s;
			}
			for (yk = i + 2; yk <= rows2; yk++)
			{
				smax = vn1_data[yk - 1];
				if (smax != 0.0)
				{
					b_n = i + cols2 * (yk - 1);
					s = fabs(b_A_data[b_n]) / smax;
					s = 1.0 - s * s;
					if (s < 0.0)
						s = 0.0;
					beta1 = smax / vn2_data[yk - 1];
					beta1 = s * (beta1 * beta1);
					if (beta1 <= DBL_EPSILON)
					{
						if (i + 1 < cols2)
						{
							smax = xnrm2(mmi, b_A_data, b_n + 2);
							vn1_data[yk - 1] = smax;
							vn2_data[yk - 1] = smax;
						}
						else
						{
							vn1_data[yk - 1] = 0.0;
							vn2_data[yk - 1] = 0.0;
						}
					}
					else
						vn1_data[yk - 1] = smax * sqrt(s);
				}
			}
		}
		rankR = 0;
		if (cols2 < rows2)
		{
			minmn = cols2;
			maxmn = rows2;
		}
		else
		{
			minmn = rows2;
			maxmn = cols2;
		}
		if (minmn > 0)
			while ((rankR < minmn) && (fabs(b_A_data[rankR + cols2 * rankR]) > (2.2204460492503131E-15 * (double)maxmn * fabs(b_A_data[0]))))
				rankR++;
		minmn = rows1;
		for (i = 0; i < minmn; i++) {
			maxmn = cols1;
			for (j = 0; j < maxmn; j++) {
				k = j + cols1 * i;
				b_B_data[k] = A[k];
			}
		}
		Y_size[1] = rows2;
		Y_size[0] = rows1;
		memset(Y, 0, rows1 * rows2 * sizeof(double));
		minmn = cols2;
		ix = rows2;
		if (minmn < ix)
			ix = minmn;
		for (j = 0; j < ix; j++)
		{
			if (tau_data[j] != 0.0)
			{
				for (k = 0; k < rows1; k++)
				{
					maxmn = cols1 * k;
					minmn = j + maxmn;
					s = b_B_data[minmn];
					jj = j + 2;
					for (i = jj; i <= cols2; i++)
						s += b_A_data[(i + cols2 * j) - 1] * b_B_data[(i + maxmn) - 1];
					s *= tau_data[j];
					if (s != 0.0)
					{
						b_B_data[minmn] -= s;
						for (i = jj; i <= cols2; i++)
							b_B_data[(i + maxmn) - 1] -= b_A_data[(i + cols2 * j) - 1] * s;
					}
				}
			}
		}
		for (k = 0; k < rows1; k++)
		{
			for (i = 0; i < rankR; i++)
				Y[(jpvt_data[i] + rows2 * k) - 1] = b_B_data[i + cols1 * k];
			for (j = rankR; j >= 1; j--) {
				minmn = (jpvt_data[j - 1] + rows2 * k) - 1;
				maxmn = cols2 * (j - 1);
				Y[minmn] /= b_A_data[(j + maxmn) - 1];
				for (i = 0; i <= j - 2; i++)
					Y[(jpvt_data[i] + rows2 * k) - 1] -= Y[minmn] * b_A_data[i + maxmn];
			}
		}
	}
	free(workingBuffer);
}