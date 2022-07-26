#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <float.h>
#include <string.h>
#include <stdint.h>
#include "../../ns-eel.h"
#include "../quadprog.h"
extern double xnrm2(int n, const double x_data[], int ix0);
extern void xswap(int32_t rows1, double x_data[], int32_t ix0, int32_t iy0);
extern void xzlarf(int32_t cols1, int32_t rows1, int32_t iv0, double tau, double C_data[], int32_t ic0, int32_t ldc, double work_data[]);
extern double rt_hypotd(double u0, double u1);
void xgetrf(const int32_t m, double A[], int32_t ipiv_data[])
{
	int32_t yk, jA, u0, j, mmj, b_tmp, jp1j, ix, i2;
	double smax, s;
	ipiv_data[0] = 1;
	yk = 1;
	for (jA = 2; jA <= m; jA++)
		ipiv_data[jA - 1] = ++yk;
	for (j = 0; j < m - 1; j++)
	{
		mmj = m - j;
		b_tmp = j * (m + 1);
		jp1j = b_tmp + 2;
		yk = 0;
		ix = b_tmp;
		smax = fabs(A[b_tmp]);
		for (jA = 2; jA <= mmj; jA++)
		{
			s = fabs(A[++ix]);
			if (s > smax)
			{
				yk = jA - 1;
				smax = s;
			}
		}
		if (A[b_tmp + yk] != 0.0)
		{
			if (yk != 0)
			{
				yk += j;
				ipiv_data[j] = yk + 1;
				ix = j;
				for (jA = 0; jA < m; jA++)
				{
					smax = A[ix];
					A[ix] = A[yk];
					A[yk] = smax;
					ix += m;
					yk += m;
				}
			}
			for (yk = jp1j; yk <= b_tmp + mmj; yk++)
				A[yk - 1] /= A[b_tmp];
		}
		yk = b_tmp + m;
		jA = (b_tmp + m) + 1;
		for (jp1j = 0; jp1j <= m - j - 2; jp1j++)
		{
			smax = A[yk];
			if (A[yk] != 0.0)
			{
				ix = b_tmp + 1;
				i2 = mmj + jA;
				for (u0 = jA + 1; u0 < i2; u0++)
					A[u0 - 1] += A[ix++] * -smax;
			}
			yk += m;
			jA += m;
		}
	}
}
#ifndef min
#define min(a,b) (((a) < (b)) ? (a) : (b))
#endif
#ifndef max
#define max(a,b) (((a) > (b)) ? (a) : (b))
#endif
extern double rt_hypotd(double u0, double u1);
void mldivide(const double A[], const int32_t rows1, const int32_t cols1, const double b[], const int32_t rows2, const int32_t cols2, double Y[], int32_t Y_size[2])
{
	if (rows1 != rows2)
	{
		char *msg = "Matrix dimensions must agree.\n";
		EEL_STRING_STDOUT_WRITE(msg, strlen(msg));
		return;
	}
	int32_t minmn, i0, maxmn, m, b_nb, j, mn, k, i;
	int32_t b_n;
	int32_t yk;
	double smax;
	int32_t i_i;
	int32_t nmi;
	int32_t mmi;
	double s;
	double beta1;
	int32_t knt;
	double tol;
	size_t bufferSize = ((rows2 * cols2) + (rows1 * cols1) + (cols1 * cols2) + (cols1 * 3) + min(rows1, cols1)) * sizeof(double) + max(rows1, cols1) * sizeof(int32_t);
	char *workingBuffer = (char*)malloc(bufferSize * sizeof(char));
	double *B_tmp_data = (double*)workingBuffer;
	double *b_A_data = B_tmp_data + rows2 * cols2;
	double *b_Y_data = b_A_data + rows1 * cols1;
	double *qrWorkBuf = b_Y_data + cols1 * cols2;
	double *tau_data = qrWorkBuf + cols1 * 3;
	int32_t *jpvt_data = (int32_t*)(tau_data + min(rows1, cols1));
	if (rows1 == cols1)
	{
		for (i = 0; i < cols1; i++)
			for (j = 0; j < rows1; j++)
				b_A_data[j + rows1 * i] = A[i + cols1 * j];
		for (i = 0; i < cols2; i++)
			for (j = 0; j < rows2; j++)
				b_Y_data[j + rows2 * i] = b[i + cols2 * j];
		xgetrf(cols1, b_A_data, jpvt_data);
		for (maxmn = 0; maxmn <= cols1 - 2; maxmn++)
		{
			if (jpvt_data[maxmn] != maxmn + 1)
			{
				for (m = 0; m <= cols2 - 1; m++)
				{
					minmn = rows2 * m;
					b_nb = maxmn + minmn;
					tol = b_Y_data[b_nb];
					mn = jpvt_data[maxmn] + minmn - 1;
					b_Y_data[b_nb] = b_Y_data[mn];
					b_Y_data[mn] = tol;
				}
			}
		}
		for (j = 0; j <= cols2 - 1; j++)
		{
			minmn = cols1 * j - 1;
			for (k = 0; k < cols1; k++)
			{
				if (b_Y_data[k + minmn + 1] != 0.0)
					for (i = k + 2; i <= cols1; i++)
						b_Y_data[i + minmn] -= b_Y_data[k + minmn + 1] * b_A_data[(i + cols1 * k) - 1];
			}
		}
		for (j = 0; j <= cols2 - 1; j++)
		{
			minmn = cols1 * j - 1;
			for (k = cols1; k >= 1; k--)
			{
				maxmn = cols1 * (k - 1) - 1;
				i0 = k + minmn;
				if (b_Y_data[i0] != 0.0)
				{
					b_Y_data[i0] /= b_A_data[k + maxmn];
					for (i = 0; i <= k - 2; i++)
						b_Y_data[i + minmn + 1] -= b_Y_data[i0] * b_A_data[i + maxmn + 1];
				}
			}
		}
		Y_size[1] = cols2;
		Y_size[0] = rows2;
		for (i = 0; i < rows2; i++)
			for (j = 0; j < cols2; j++)
				Y[j + cols2 * i] = b_Y_data[i + rows2 * j];
	}
	else
	{
		for (i = 0; i < cols1; i++)
			for (j = 0; j < rows1; j++)
				b_A_data[j + rows1 * i] = A[i + cols1 * j];
		for (i = 0; i < cols2; i++)
			for (j = 0; j < rows2; j++)
				B_tmp_data[j + rows2 * i] = b[i + cols2 * j];
		double *vn1_data = qrWorkBuf + cols1;
		double *vn2_data = vn1_data + cols1;
		b_n = rows1;
		mn = cols1;
		if (b_n < mn)
			mn = b_n;
		jpvt_data[0] = 1;
		yk = 1;
		for (k = 2; k <= cols1; k++)
			jpvt_data[k - 1] = ++yk;
		if (0 <= cols1 - 1)
			memset(&qrWorkBuf[0], 0, cols1 * (int32_t)sizeof(double));
		k = 1;
		for (yk = 0; yk < cols1; yk++)
		{
			smax = xnrm2(rows1, b_A_data, k);
			vn1_data[yk] = smax;
			vn2_data[yk] = smax;
			k += rows1;
		}
		for (i = 0; i < mn; i++)
		{
			i_i = i + i * rows1;
			nmi = cols1 - i;
			mmi = (rows1 - i) - 1;
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
				xswap(rows1, b_A_data, 1 + rows1 * b_n, 1 + rows1 * i);
				yk = jpvt_data[b_n];
				jpvt_data[b_n] = jpvt_data[i];
				jpvt_data[i] = yk;
				vn1_data[b_n] = vn1_data[i];
				vn2_data[b_n] = vn2_data[i];
			}
			if (i + 1 < rows1)
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
			if (i + 1 < cols1)
			{
				s = b_A_data[i_i];
				b_A_data[i_i] = 1.0;
				xzlarf(1 + mmi, nmi - 1, i_i + 1, tau_data[i], b_A_data, (i + (i + 1) * rows1) + 1, rows1, qrWorkBuf);
				b_A_data[i_i] = s;
			}
			for (yk = i + 2; yk <= cols1; yk++)
			{
				smax = vn1_data[yk - 1];
				if (smax != 0.0)
				{
					b_n = i + rows1 * (yk - 1);
					s = fabs(b_A_data[b_n]) / smax;
					s = 1.0 - s * s;
					if (s < 0.0)
						s = 0.0;
					beta1 = smax / vn2_data[yk - 1];
					beta1 = s * (beta1 * beta1);
					if (beta1 <= DBL_EPSILON)
					{
						if (i + 1 < rows1)
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
		if (rows1 < cols1)
		{
			minmn = rows1;
			maxmn = cols1;
		}
		else
		{
			minmn = cols1;
			maxmn = rows1;
		}
		int32_t rankR = 0;
		if (minmn > 0)
		{
			tol = DBL_EPSILON * (double)maxmn * fabs(b_A_data[0]);
			while ((rankR < minmn) && (fabs(b_A_data[rankR + rows1 * rankR]) > tol))
				rankR++;
		}
		memset(b_Y_data, 0, cols1 * cols2 * sizeof(double));
		m = rows1;
		b_nb = cols2;
		minmn = rows1;
		mn = cols1;
		if (minmn < mn)
			mn = minmn;
		for (j = 0; j < mn; j++)
		{
			if (tau_data[j] != 0.0)
			{
				for (k = 0; k < b_nb; k++)
				{
					maxmn = rows2 * k;
					minmn = j + maxmn;
					tol = B_tmp_data[minmn];
					i0 = j + 2;
					for (i = i0; i <= m; i++)
						tol += b_A_data[(i + rows1 * j) - 1] * B_tmp_data[(i + maxmn) - 1];
					tol *= tau_data[j];
					if (tol != 0.0)
					{
						B_tmp_data[minmn] -= tol;
						for (i = i0; i <= m; i++)
							B_tmp_data[(i + maxmn) - 1] -= b_A_data[(i + rows1 * j) - 1] * tol;
					}
				}
			}
		}
		for (k = 0; k < cols2; k++)
		{
			for (i = 0; i < rankR; i++)
				b_Y_data[(jpvt_data[i] + cols1 * k) - 1] = B_tmp_data[i + rows2 * k];
			for (j = rankR; j >= 1; j--)
			{
				mn = (jpvt_data[j - 1] + cols1 * k) - 1;
				minmn = rows1 * (j - 1);
				b_Y_data[mn] /= b_A_data[(j + minmn) - 1];
				for (i = 0; i <= j - 2; i++)
				{
					maxmn = (jpvt_data[i] + cols1 * k) - 1;
					b_Y_data[maxmn] -= b_Y_data[mn] * b_A_data[i + minmn];
				}
			}
		}
		Y_size[1] = cols2;
		Y_size[0] = cols1;
		for (i = 0; i < cols1; i++)
			for (j = 0; j < cols2; j++)
				Y[j + cols2 * i] = b_Y_data[i + cols1 * j];
	}
	free(workingBuffer);
}
// Column major code, but symmetric matrix don't care
void NxN_mldivide_mat_vec(const double *A, const double f[], const int32_t n, double H[])
{
	int32_t i, j, k;
	double *A2 = (double*)malloc(n * n * sizeof(double));
	memcpy(A2, A, n * n * sizeof(double));
	int32_t *ipiv = (int32_t*)malloc(n * sizeof(int32_t));
	memcpy(H, f, n * sizeof(double));
	xgetrf(n, A2, ipiv);
	double temp;
	for (i = 0; i < n - 1; i++)
	{
		if (ipiv[i] != i + 1)
		{
			temp = H[i];
			H[i] = H[ipiv[i] - 1];
			H[ipiv[i] - 1] = temp;
		}
	}
	for (j = 0; j < n; j++)
	{
		k = n * j;
		if (H[j] != 0.0)
		{
			for (i = j + 2; i < n + 1; i++)
				H[i - 1] -= H[j] * A2[(i + k) - 1];
		}
	}
	for (j = n - 1; j >= 0; j--)
	{
		k = n * j;
		if (H[j] != 0.0)
		{
			H[j] /= A2[j + k];
			for (i = 0; i < j; i++)
				H[i] -= H[j] * A2[i + k];
		}
	}
	free(A2);
	free(ipiv);
}
