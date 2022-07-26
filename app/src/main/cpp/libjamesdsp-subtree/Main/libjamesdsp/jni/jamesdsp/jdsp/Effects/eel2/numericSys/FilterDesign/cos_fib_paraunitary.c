#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <float.h>
#include <string.h>
#include <stdint.h>
#include "../SolveLinearSystem/qr_fact.h"
#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif
static int8_t sabs8(int8_t i)
{
	int8_t res;
	if (INT8_MIN == i)
		res = INT8_MAX;
	else
		res = i < 0 ? -i : i;
	return res;
}
static void matrix_transpose_matrix_self_product(const double *a, double *product, unsigned int cols1, unsigned int rows1)
{
	unsigned int i, j, k;
	for (i = 0; i < rows1; ++i)
		for (j = 0; j < rows1; ++j)
		{
			double res = 0.0;
			for (k = 0; k < cols1; ++k)
				res += a[k * rows1 + i] * a[k * rows1 + j];
			product[i * rows1 + j] = res;
		}
}
static void matrix_transpose_matrix_self_product_in_out_stride(const double *a, double *product, unsigned int cols1, unsigned int rows1, unsigned int inStride, unsigned int outStride)
{
	unsigned int i, j, k;
	for (i = 0; i < rows1; ++i)
		for (j = 0; j < rows1; ++j)
		{
			double res = 0.0;
			for (k = 0; k < cols1; ++k)
				res += a[k * inStride + i] * a[k * inStride + j];
			product[i * outStride + j] = res;
		}
}
static void matrix_matrix_product(const double *a, const double *b, double *product, int rows1, int cols1, int rows2, int cols2)
{
	int i, j, k;
	for (i = 0; i < rows1; ++i)
		for (j = 0; j < cols2; ++j)
		{
			double res = 0.0;
			for (k = 0; k < cols1; ++k)
				res += a[i * cols1 + k] * b[k * cols2 + j];
			product[i * cols2 + j] = res;
		}
}
static void matrix_transposedMatrix_product(const double *a, const double *b, double *product, unsigned int rows1, unsigned int cols1, unsigned int rows2, int unsigned cols2)
{
	unsigned int i, j, k;
	for (i = 0; i < rows1; ++i)
		for (j = 0; j < cols2; ++j)
		{
			double res = 0.0;
			for (k = 0; k < cols1; ++k)
				res += a[i * cols1 + k] * b[j * rows2 + k];
			product[i * cols2 + j] = res;
		}
}
static void matrix_matrix_transpose_self_product(const double *a, double *product, unsigned int rows1, unsigned int cols1)
{
	unsigned int i, j, k;
	for (i = 0; i < rows1; ++i)
	{
		for (j = 0; j < rows1; ++j)
		{
			double res = 0.0;
			for (k = 0; k < cols1; ++k)
				res += a[i * cols1 + k] * a[j * cols1 + k];
			product[i * rows1 + j] = res;
		}
	}
}
static void transpose(double *src, double *dst, unsigned int n, unsigned int m)
{
	for (unsigned int k = 0; k < n * m; k++)
		dst[k] = src[m * (k % n) + (k / n)];
}
static void matrix_vector_mult(const double *mat, const double *vec, double *c, unsigned int rows, unsigned int cols, unsigned int stepSize)
{
	for (unsigned int i = 0; i < rows; i++)
	{
		double res = 0.0;
		for (unsigned int j = 0; j < cols; j++)
			res += (mat[i * stepSize + j] * vec[j]);
		c[i] = res;
	}
}
unsigned int cholesky(double *A, double *L, unsigned int n)
{
	unsigned int i, k;
	unsigned int r = 0;
	double tol;
	double minV = A[0];
	for (i = 1; i < n; i++)
	{
		if (A[i * n + i] > 0.0)
		{
			if (A[i * n + i] < minV)
				minV = A[i * n + i];
		}
	}
	tol = minV * 1e-9;
	double *tmp = (double*)malloc(n * sizeof(double));
	for (k = 0; k < n; k++)
	{
		matrix_vector_mult(L + k * n, L + k * n, tmp, n - k, r, n);
		for (i = k; i < n; i++)
			L[i * n + r] = A[i * n + k] - tmp[i - k];
		if (L[k * n + r] > tol)
		{
			L[k * n + r] = sqrt(L[k * n + r]);
			if (k < n - 1)
			{
				for (i = k + 1; i < n; i++)
					L[i * n + r] = L[i * n + r] / L[k * n + r];
			}
			r++;
		}
	}
	free(tmp);
	return r;
}
void inv_chol(double *L, double *Y, unsigned int n)
{
	unsigned int i, j, k;
	double *S = (double*)malloc(n * n * sizeof(double));
	memset(S, 0, n * n * sizeof(double));
	for (i = 0; i < n; i++)
		S[i * n + i] = 1.0 / L[i * n + i];
	double res;
	for (i = n; i-- > 0;)
	{
		for (j = n; j-- > 0;)
		{
			res = 0.0;
			for (k = 0; k < n - i - 1; k++)
				res += (L[(i + 1) * n + k * n + i] * Y[(i + 1) * n + k * n + j]);
			Y[i * n + j] = (S[i * n + j] - res) / L[i * n + i];
			Y[j * n + i] = Y[i * n + j];
		}
	}
	free(S);
}
#define min(a,b) (((a)<(b))?(a):(b))
#define max(a,b) (((a)>(b))?(a):(b))
void geninv(double *G, unsigned int m1, unsigned int n1, double *Y, unsigned int size[2])
{
	unsigned int i, j, k;
	char tp;
	unsigned int n = min(m1, n1);
	double *A = (double*)malloc(n * n * sizeof(double));
	if (m1 < n1)
	{
		tp = 1;
		matrix_matrix_transpose_self_product(G, A, m1, n1);
	}
	else
	{
		tp = 0;
		matrix_transpose_matrix_self_product(G, A, m1, n1);
	}
	double *L = (double*)malloc(n * n * sizeof(double));
	memset(L, 0, n * n * sizeof(double));
	unsigned int r = cholesky(A, L, n);
	double *Ltmp = (double*)malloc(r * r * sizeof(double));
	matrix_transpose_matrix_self_product_in_out_stride(L, Ltmp, n, r, n, r);
	double *L2 = (double*)malloc(r * r * sizeof(double));
	memset(L2, 0, r * r * sizeof(double));
	cholesky(Ltmp, L2, r);
	double *M = (double*)malloc(r * r * sizeof(double));
	inv_chol(L2, M, r);
	double *tmpRes1 = (double*)malloc(r * sizeof(double));
	double *tmpRes2 = (double*)malloc(r * sizeof(double));
	double *tmpRes3;
	double *tmpRes4 = (double*)malloc(n1 * m1 * sizeof(double));
	if (tp)
	{
		tmpRes3 = (double*)malloc(r * sizeof(double));
		for (i = 0; i < n1; ++i)
		{
			// tmpRes1 = G' * L
			for (j = 0; j < r; ++j)
			{
				double res = 0.0;
				for (k = 0; k < m1; ++k)
					res += G[k * n1 + i] * L[k * n + j];
				tmpRes1[j] = res;
			}
			// tmpRes2 = tmpRes1 * M
			for (j = 0; j < r; ++j)
			{
				double res = 0.0;
				for (k = 0; k < r; ++k)
					res += tmpRes1[k] * M[k * r + j];
				tmpRes2[j] = res;
			}
			// tmpRes3 = tmpRes2 * M
			for (j = 0; j < r; ++j)
			{
				double res = 0.0;
				for (k = 0; k < r; ++k)
					res += tmpRes2[k] * M[k * r + j];
				tmpRes3[j] = res;
			}
			// Y = tmpRes3 * L'
			for (j = 0; j < m1; ++j)
			{
				double res = 0.0;
				for (k = 0; k < r; ++k)
					res += tmpRes3[k] * L[j * n + k];
				Y[i * m1 + j] = res;
			}
		}
	}
	else
	{
		tmpRes3 = (double*)malloc(n1 * sizeof(double));
		for (i = 0; i < n1; ++i)
		{
			for (j = 0; j < r; ++j)
			{
				double res = 0.0;
				for (k = 0; k < r; ++k)
					res += L[i * n + k] * M[k * r + j];
				tmpRes1[j] = res;
			}
			for (j = 0; j < r; ++j)
			{
				double res = 0.0;
				for (k = 0; k < r; ++k)
					res += tmpRes1[k] * M[k * r + j];
				tmpRes2[j] = res;
			}
			for (j = 0; j < n1; ++j)
			{
				double res = 0.0;
				for (k = 0; k < r; ++k)
					res += tmpRes2[k] * L[j * n1 + k];
				tmpRes3[j] = res;
			}
			for (j = 0; j < m1; ++j)
			{
				double res = 0.0;
				for (k = 0; k < n1; ++k)
					res += tmpRes3[k] * G[j * n1 + k];
				Y[i * m1 + j] = res;
			}
		}
	}
	size[0] = n1;
	size[1] = m1;
	free(A);
	free(L);
	free(Ltmp);
	free(L2);
	free(M);
	free(tmpRes1);
	free(tmpRes2);
	free(tmpRes3);
	free(tmpRes4);
}
void qr_getSize(unsigned int m, unsigned int n, unsigned int Q_size[2], unsigned int R_size[2], unsigned int *permSize)
{
	Q_size[0] = Q_size[1] = m;
	*permSize = n;
	R_size[0] = m;
	R_size[1] = n;
}
void backwardSubstitution(double *U, unsigned int n, double *b, double *x)
{
	unsigned int j, k;
	if (U[(n - 1) * n + (n - 1)] != 0.0)
		x[n - 1] = b[n - 1] / U[(n - 1) * n + (n - 1)];
	else
		x[n - 1] = 0.0;
	double x1;
	for (k = n - 1; k-- > 0;)
	{
		if (U[k * n + k] != 0.0)
		{
			double sum = 0.0;
			for (j = k + 1; j < n; j++)
				sum += U[k * n + j] * x[j];
			x1 = 1.0 / U[k * n + k] * (b[k] - sum);
		}
		else
			x1 = 0.0;
		x[k] = x1;
	}
}
void forwardSubstitution(double *L, unsigned int n, double *b, double *x)
{
	unsigned int j, k;
	if (L[0] != 0.0)
		x[0] = b[0] / L[0];
	else
		x[0] = 0.0;
	double x1;
	for (k = 1; k < n; k++)
	{
		if (L[k * n + k] != 0.0)
		{
			double sum = 0.0;
			for (j = k; j-- > 0;)
				sum += L[k * n + j] * x[j];
			x1 = 1.0 / L[k * n + k] * (b[k] - sum);
		}
		else
			x1 = 0.0;
		x[k] = x1;
	}
}
typedef struct
{
	double *Q, *R, *Q1, *preInv, *x1, *invPR;
	int *perm;
	QR_heap qr_mem;
} ulin_mem;
void preallocate_ulinsys_qr(ulin_mem *mem, unsigned int m, unsigned int n)
{
	unsigned int Q_size[2];
	unsigned int R_size[2];
	unsigned int permSize;
	qr_getSize(m, n, Q_size, R_size, &permSize);
	mem->Q = (double*)malloc(Q_size[0] * Q_size[1] * sizeof(double));
	mem->R = (double*)malloc(R_size[0] * R_size[1] * sizeof(double));
	mem->perm = (int*)malloc(permSize * sizeof(int));
	mem->Q1 = (double*)malloc(m * m * sizeof(double));
	mem->preInv = (double*)malloc(n * n * sizeof(double));
	mem->x1 = (double*)malloc(n * sizeof(double));
	mem->invPR = (double*)malloc(n * n * sizeof(double));
	preallocateMemQR(&mem->qr_mem, m, n);
}
void free_ulinsys_qr(ulin_mem *mem)
{
	free(mem->Q);
	free(mem->R);
	free(mem->perm);
	free(mem->Q1);
	free(mem->preInv);
	free(mem->x1);
	free(mem->invPR);
	freeMemQR(&mem->qr_mem);
}
unsigned int ulinsys_qr(ulin_mem *ulinM, double *A, unsigned int m, unsigned int n, double *b, double *Q2, double *ds)
{
	QR_pivoting(&ulinM->qr_mem, A, m, n, ulinM->Q, ulinM->R, ulinM->perm);
	unsigned int i, j, k;
	const double epsi = 5e-16;
	double tol = max(n, m);
	double mV = ulinM->R[0];
	for (unsigned int i = 1; i < m * n; i++)
	{
		double res = fabs(ulinM->R[i]);
		if (res > mV)
			mV = res;
	}
	tol = tol * mV * epsi;
	unsigned int i1 = 0;
	unsigned int cnt = 0;
	for (i = 0; i < m; i++)
	{
		for (j = 0; j < n; j++)
		{
			if (fabs(ulinM->R[i * n + j]) > tol)
			{
				i1 = i;
				for (k = 0; k < m; k++)
					ulinM->Q1[k * m + cnt] = ulinM->Q[k * m + i];
				if (i > 0)
					memcpy(ulinM->R + cnt * n, ulinM->R + i * n, n * sizeof(double));
				cnt++;
				break;
			}
		}
	}
	unsigned int Q2Len = m - i1 - 1;
	for (i = 0; i < m; i++)
		for (j = 0; j < Q2Len; j++)
			Q2[i * Q2Len + j] = ulinM->Q[i * m + i1 + j + 1];
	memset(ulinM->preInv, 0, n * cnt * sizeof(double));
	for (i = 0; i < cnt; i++)
		for (j = 0; j < cnt; j++)
			ulinM->preInv[ulinM->perm[i] * cnt + j] = ulinM->R[j * n + i];
	unsigned int oS[2];
	geninv(ulinM->preInv, n, cnt, ulinM->invPR, oS);
	matrix_vector_mult(ulinM->invPR, b, ulinM->x1, cnt, n, n);
	for (i = 0; i < m; i++)
	{
		double sum = 0.0;
		for (j = 0; j < cnt; j++)
			sum += ulinM->Q1[i * m + j] * ulinM->x1[j];
		ds[i] = sum;
	}
	return Q2Len;
}
typedef struct
{
	unsigned int mOpt, numCon, constraintMatSize;
	int *positionQ, *positionQQ;
	char *correspondingValueQ, *correspondingValueQQ;
} constraintsSparseMtx;
void eqconPrecompute(unsigned int L, char *C, constraintsSparseMtx *sparseMtx)
{
	unsigned int i, j;
	unsigned int Lh = L >> 1;
	unsigned int *activeConstraint = (unsigned int*)malloc(L * sizeof(unsigned int));
	unsigned int *opCnts = (unsigned int*)malloc(L * sizeof(unsigned int));
	unsigned int numCon = 0;
	char *U0l = (char*)malloc(L * Lh * sizeof(char));
	for (unsigned int actL = 0; actL < L; actL++)
	{
		unsigned char allzero = 1;
		memset(U0l, 0, L * Lh * sizeof(char));
		unsigned int mmax = ((L - 1) - actL) + 1;
		for (i = 0; i < mmax; i++)
		{
			unsigned int i1 = i + actL;
			if (i1 < Lh)
			{
				U0l[i * Lh + i1] = C[actL * L + i];
				if (sabs8(C[actL * L + i]) > 0)
					allzero = 0;
			}
			else
			{
				U0l[i * Lh + ((L - i1) - 1)] = C[actL * L + i];
				if (sabs8(C[actL * L + i]) > 0)
					allzero = 0;
			}
		}
		for (i = 0; i < Lh; i++)
		{
			for (j = 0; j < Lh; j++)
				U0l[i * Lh + j] += U0l[(L - i - 1) * Lh + j];
		}
		if (!allzero)
		{
			activeConstraint[numCon] = actL;
			unsigned int numOpt = 0;
			for (i = 0; i < Lh; i++)
				if (sabs8(U0l[i * Lh + 0]) > 0)
					numOpt++;
			unsigned int mO = numOpt;
			for (j = 1; j < Lh; j++)
			{
				numOpt = 0;
				for (i = 0; i < Lh; i++)
				{
					if (sabs8(U0l[i * Lh + j]) > 0)
						numOpt++;
				}
				if (numOpt > mO)
					mO = numOpt;
			}
			opCnts[numCon] = mO;
			//printf("%d %d %d\n", numCon + 1, actL, mO);
			numCon++;
		}
	}
	// Retrieve sparse matrix position
	unsigned int mOpt = opCnts[0];
	for (i = 1; i < numCon; i++)
	{
		if (opCnts[i] > mOpt)
			mOpt = opCnts[i];
	}
	unsigned int constraintMatSize = Lh * mOpt;
	sparseMtx->positionQ = (int*)malloc(numCon * constraintMatSize * sizeof(int));
	sparseMtx->positionQQ = (int*)malloc(numCon * constraintMatSize * sizeof(int));
	for (unsigned int ii = 0; ii < numCon * constraintMatSize; ii++)
	{
		sparseMtx->positionQ[ii] = -1;
		sparseMtx->positionQQ[ii] = -1;
	}
	sparseMtx->correspondingValueQ = (char*)malloc(numCon * constraintMatSize * sizeof(char));
	sparseMtx->correspondingValueQQ = (char*)malloc(numCon * constraintMatSize * sizeof(char));
	memset(sparseMtx->correspondingValueQ, 0, numCon * constraintMatSize * sizeof(char));
	memset(sparseMtx->correspondingValueQQ, 0, numCon * constraintMatSize * sizeof(char));
	char *QQ = (char*)malloc(Lh * Lh * sizeof(char));
	for (unsigned int ii = 1; ii < numCon; ii++)
	{
		unsigned int actL = activeConstraint[ii];
		memset(U0l, 0, L * Lh * sizeof(char));
		unsigned int mmax = ((L - 1) - actL) + 1;
		for (i = 0; i < mmax; i++)
		{
			unsigned int i1 = i + actL;
			if (i1 < Lh)
				U0l[i * Lh + i1] = C[actL * L + i];
			else
				U0l[i * Lh + ((L - i1) - 1)] = C[actL * L + i];
		}
		for (i = 0; i < Lh; i++)
		{
			for (j = 0; j < Lh; j++)
				U0l[i * Lh + j] += U0l[(L - i - 1) * Lh + j];
		}
		for (i = 0; i < Lh; i++)
		{
			for (j = 0; j < Lh; j++)
				QQ[i * Lh + j] = U0l[i * Lh + j] + U0l[j * Lh + i];
		}
		unsigned int n1, n2;
		for (i = 0; i < Lh; i++)
		{
			n1 = 0; n2 = 0;
			for (j = 0; j < Lh; j++)
			{
				if (sabs8(U0l[j * Lh + i]) > 0)
				{
					sparseMtx->positionQ[constraintMatSize * ii + i * mOpt + n1] = j;
					if (U0l[j * Lh + i] == -2)
						sparseMtx->correspondingValueQ[constraintMatSize * ii + i * mOpt + n1] = 0;
					else if (U0l[j * Lh + i] == -1)
						sparseMtx->correspondingValueQ[constraintMatSize * ii + i * mOpt + n1] = 1;
					else if (U0l[j * Lh + i] == 0)
						sparseMtx->correspondingValueQ[constraintMatSize * ii + i * mOpt + n1] = 2;
					else if (U0l[j * Lh + i] == 1)
						sparseMtx->correspondingValueQ[constraintMatSize * ii + i * mOpt + n1] = 3;
					else if (U0l[j * Lh + i] == 2)
						sparseMtx->correspondingValueQ[constraintMatSize * ii + i * mOpt + n1] = 4;
					else
						printf("Shit 1!\n");
					n1++;
				}
				if (sabs8(QQ[j * Lh + i]) > 0)
				{
					sparseMtx->positionQQ[constraintMatSize * ii + i * mOpt + n2] = j;
					if (QQ[j * Lh + i] == -2)
						sparseMtx->correspondingValueQQ[constraintMatSize * ii + i * mOpt + n2] = 0;
					else if (QQ[j * Lh + i] == -1)
						sparseMtx->correspondingValueQQ[constraintMatSize * ii + i * mOpt + n2] = 1;
					else if (QQ[j * Lh + i] == 0)
						sparseMtx->correspondingValueQQ[constraintMatSize * ii + i * mOpt + n2] = 2;
					else if (QQ[j * Lh + i] == 1)
						sparseMtx->correspondingValueQQ[constraintMatSize * ii + i * mOpt + n2] = 3;
					else if (QQ[j * Lh + i] == 2)
						sparseMtx->correspondingValueQQ[constraintMatSize * ii + i * mOpt + n2] = 4;
					else
						printf("Shit 2!\n");
					n2++;
				}
			}
		}
	}
	sparseMtx->numCon = numCon;
	sparseMtx->mOpt = mOpt;
	sparseMtx->constraintMatSize = constraintMatSize;
	free(QQ);
	free(opCnts);
	free(activeConstraint);
	free(U0l);
}
void genC(unsigned int N, unsigned int L, char *C)
{
	unsigned int i, j;
	unsigned int N2 = N << 1;
	memset(C, 0, L * L * sizeof(char));
	for (i = 0; i < L; i++)
	{
		if (i % N2 == 0)
		{
			if ((i / N2) % 2 == 0)
			{
				for (j = 0; j < L; j++)
					C[i * L + j] = 1;
			}
			else
			{
				for (j = 0; j < L; j++)
					C[i * L + j] = -1;
			}
		}
		for (j = 0; j < L; j++)
		{
			if ((2 * j) > (N + L - i))
			{
				if ((2 * j - N - L + 1 + i) % N2 == 0)
				{
					if (((2 * j - N - L + 1 + i) / N2) % 2 == 0)
						C[i * L + j] += 1;
					else
						C[i * L + j] -= 1;
				}
			}
			else
			{
				if ((N + L - 2 * j - 1 - i) % N2 == 0)
				{
					if (((N + L - 2 * j - 1 - i) / N2) % 2 == 0)
						C[i * L + j] += 1;
					else
						C[i * L + j] -= 1;
				}
			}
		}
	}
}
void genP(int L, double fc, double df, double *P)
{
	int ii, jj;
	int Lh = L >> 1;
	double theta_s = M_PI * (fc + df);
	double d = 2.0 * (M_PI - theta_s);
	const double delta_s = 1e-14;
	for (ii = 0; ii < Lh; ii++)
	{
		P[ii * Lh + ii] = d - 2.0 * sin(theta_s * (double)(2 * (ii + 1) - L - 1)) / (double)(2 * (ii + 1) - L - 1) + delta_s;
		for (jj = ii + 1; jj < Lh; jj++)
		{
			double idx1 = (ii + 1) - (jj + 1);
			double idx2 = (ii + 1) + (jj + 1);
			P[ii * Lh + jj] = -2.0 * (sin(theta_s * idx1) / idx1 + sin(theta_s * (double)(idx2 - L - 1)) / (double)(idx2 - L - 1));
			P[jj * Lh + ii] = P[ii * Lh + jj];
		}
	}
}
void eqconTransposeB(double *x, unsigned int N, unsigned int L, constraintsSparseMtx *mtx, double gain, double *A, double *b)
{
	unsigned int i, j;
	double N2 = (double)(N << 1);
	unsigned int Lh = L >> 1;
	for (i = 0; i < Lh; i++)
		A[i * mtx->numCon + 0] = x[i] * N2;
	const double lut[5] = { -2, -1, 0, 1, 2 };
	double res2, sumV1, sumV2;
	res2 = 0.0;
	for (i = 0; i < Lh; i++)
		res2 += x[i] * x[i];
	b[0] = (1.0 / N) - (res2 * N);
	for (unsigned int cont = 1; cont < mtx->numCon; cont++)
	{
		res2 = 0.0;
		for (i = 0; i < Lh; i++)
		{
			sumV1 = 0.0;
			sumV2 = 0.0;
			for (j = 0; j < mtx->mOpt; j++)
			{
				int pos = mtx->positionQQ[mtx->constraintMatSize * cont + i * mtx->mOpt + j];
				if (pos >= 0)
					sumV1 = sumV1 + lut[mtx->correspondingValueQQ[mtx->constraintMatSize * cont + i * mtx->mOpt + j]] * x[pos];
				pos = mtx->positionQ[mtx->constraintMatSize * cont + i * mtx->mOpt + j];
				if (pos >= 0)
					sumV2 = sumV2 + lut[mtx->correspondingValueQ[mtx->constraintMatSize * cont + i * mtx->mOpt + j]] * x[pos];
			}
			A[i * mtx->numCon + cont] = sumV1 * gain;
			res2 = res2 + sumV2 * x[i];
		}
		b[cont] = -res2 * gain;
	}
}
double bessi0(double x)
{
	double xh, sum, pow, ds;
	int k;
	xh = 0.5 * x;
	sum = 1.0;
	pow = 1.0;
	k = 0;
	ds = 1.0;
	while (ds > sum * DBL_EPSILON)
	{
		++k;
		pow = pow * (xh / k);
		ds = pow * pow;
		sum = sum + ds;
	}
	return sum;
}
double getKaiser(double x, double alpha)
{
	static double alpha_prev = 0.0;
	static double Ia = 1.0;
	double beta, win;
	if (alpha != alpha_prev)
	{
		Ia = bessi0(alpha);
		alpha_prev = alpha;
	}
	if (x <= -1.0 || x >= 1.0)
		win = 0.0;
	else
	{
		beta = alpha * sqrt(1.0 - x * x);
		win = bessi0(beta) / Ia;
	}
	return win;
}
extern double sinc(double x);
void halfSincKaiser(double *win, int L, double fc, double alpha)
{
	int i, minus1 = L - 1;
	for (i = 0; i < L >> 1; ++i)
	{
		double filter = fc * sinc(fc * ((0.5 + i) - L * 0.5));
		win[i] = filter * getKaiser((double)(2 * i - minus1) / minus1, alpha);
	}
}
void subsamplingCal(unsigned int M, double alpha, double *f_def, unsigned int *Sk)
{
	unsigned int i, k;
	for (i = 0; i < M + 1; i++)
	{
		double w = 2.0 * M_PI * (i / (double)(2 * M));
		f_def[i] = (w - 2.0 * atan((alpha * sin(w)) / (alpha * cos(w) - 1))) / (2.0 * M_PI);
	}
	double a_def = 2.0 * M_PI * (1.0 / (double)(2 * M)) + M_PI / (double)(2 * M);
	double fs_def = (a_def - 2.0 * atan(alpha * sin(a_def) / (alpha * cos(a_def) - 1.0))) / (2.0 * M_PI);
	double fU, fL;
	for (k = 0; k < M; k++)
	{
		Sk[k] = 1;
		if (k == 0)
		{
			fU = fs_def;
			fL = f_def[k];
		}
		else if (k == (M - 1))
		{
			fU = f_def[k + 1];
			fL = f_def[k - 1];
		}
		else
		{
			fU = f_def[k + 2];
			fL = f_def[k - 1];
		}
		double B = fU - fL;
		unsigned int nk_max = (unsigned int)(fU / B);
		for (unsigned int nk = 0; nk < nk_max; nk++)
		{
			unsigned int ub = (unsigned int)((nk + 1) / (2 * fU));
			unsigned int lb = (unsigned int)max(ceil(nk / (2 * fL)), 1.0) - 1;
			if (ub >= lb)
			{
				for (unsigned int s = lb; s < ub; s++)
				{
					if ((s + 1.0) * B <= 0.5)
						Sk[k] = max(Sk[k], (s + 1));
				}
			}
		}
	}
}
typedef struct
{
	unsigned long long inuse, capacity, grow_num;
	double *data;
} sample_vector;
void init_sample_vector(sample_vector *s, int init_capacity, int grow)
{
	s->data = (double*)malloc(init_capacity * sizeof(double));
	s->inuse = 0;
	s->capacity = init_capacity;
	s->grow_num = grow;
}
void push_back_sample_vector(sample_vector *s, double *x, int lenS)
{
	if ((s->inuse + lenS + (s->grow_num >> 1)) > s->capacity)
	{
		s->capacity += (s->grow_num + lenS);
		s->data = (double*)realloc(s->data, s->capacity * sizeof(double));
	}
	memcpy(s->data + s->inuse, x, lenS * sizeof(double));
	s->inuse += lenS;
}
void clear_sample_vector(sample_vector *s)
{
	s->inuse = 0;
}
void free_sample_vector(sample_vector *s)
{
	free(s->data);
}
float* allpass_char(double alpha, unsigned int L, unsigned int *CFiltLen)
{
	unsigned int i;
	double *allpass_delay_chain = (double*)malloc(L * sizeof(double));
	memset(allpass_delay_chain, 0, L * sizeof(double));
	double *APC_delay_1 = (double*)malloc(L * sizeof(double));
	double *APC_delay_2 = (double*)malloc(L * sizeof(double));
	memset(APC_delay_1, 0, L * sizeof(double));
	memset(APC_delay_2, 0, L * sizeof(double));
	unsigned int n = 0;
	sample_vector smpVec;
	init_sample_vector(&smpVec, 2048, 2048);
	double Threshold = 2e-3;
	unsigned int ResInd1 = 0, ResInd2 = 0;
	double pwr = 0.0f;
	while (1)
	{
		double R1_a;
		if (n == 0)
			R1_a = 1.0f;
		else
			R1_a = 0.0f;
		double R2_a = allpass_delay_chain[0];
		allpass_delay_chain[0] = R1_a;
		for (i = 1; i < L; i++)
		{
			double R3_a = R2_a;
			R2_a = allpass_delay_chain[i];
			R1_a = (R2_a - R1_a) * alpha + R3_a;
			allpass_delay_chain[i] = R1_a;
		}
		double R1_b = allpass_delay_chain[0];
		for (i = 0; i < L - 1; i++)
		{
			double R3_b = APC_delay_1[i];
			APC_delay_1[i] = R1_b;
			double R2_b = APC_delay_2[i];
			R1_b = (R2_b - R1_b) * alpha + R3_b;
			APC_delay_2[i] = R1_b;

			R1_b = R1_b + allpass_delay_chain[i + 1];
		}
		push_back_sample_vector(&smpVec, &R1_b, 1);
		double curPwr = fabs(R1_b);
		if (curPwr > Threshold && !ResInd1)
			ResInd1 = n;
		if (n == 0)
			pwr = curPwr;
		else
			pwr = curPwr * 0.5 + pwr * 0.5;
		if (curPwr < Threshold && (pwr < Threshold) && (ResInd1 > ResInd2))
		{
			ResInd2 = n;
			break;
		}
		n++;
	}
	if (ResInd2 > ResInd1)
		*CFiltLen = ResInd2 - ResInd1;
	else
	{
		*CFiltLen = 0;
		return 0;
	}
	float *C = (float*)malloc(*CFiltLen * sizeof(float));
	for (i = ResInd2; i-- > ResInd1; )
		C[ResInd2 - i - 1] = (float)(smpVec.data[i] / (double)L);
	free_sample_vector(&smpVec);
	free(allpass_delay_chain);
	free(APC_delay_1);
	free(APC_delay_2);
	return C;
}
void cos_fib_paraunitary1(unsigned int N, unsigned int m, unsigned int L, double df, double *h_opt)
{
	unsigned int i, k;
	unsigned int Lh = L >> 1;
	double fc = 1.21 / (double)(2 * N);
	double as = 7.95 + 7.18 * 2.0 * L * df;
	double beta;
	if (as < 21.0)
		beta = 0.0;
	else if (as < 50.0)
		beta = 0.58417 * pow(as - 20.96, 0.4) + 0.07886 * (as - 20.96);
	else
		beta = 0.1102 * (as - 8.7);
	double *h = (double*)malloc(Lh * sizeof(double));
	halfSincKaiser(h, L, fc, beta);
	double *P = (double*)malloc(Lh * Lh * sizeof(double));
	double *PT = (double*)malloc(Lh * Lh * sizeof(double));
	genP((int)L, fc, df, P);
	transpose(P, PT, Lh, Lh);
	free(P);
	double a = N * 0.5;
	char *C = (char*)malloc(L * L * sizeof(char));
	genC(N, L, C);
	constraintsSparseMtx sparseMtx;
	eqconPrecompute(L, C, &sparseMtx);
	unsigned int Kiter = 200;
	unsigned int Kiter1 = 40;
	double epsil = 1e-10;
	double epsil1 = 1e-7;
	char flag = 0;
	double previous2 = 0;
	double previous1 = 0;
	char problematic = 0;
	double *dt = (double*)malloc(Lh * sizeof(double));
	double *A = (double*)malloc(Lh * sparseMtx.numCon * sizeof(double));
	double *b = (double*)malloc(sparseMtx.numCon * sizeof(double));
	double *ds = (double*)malloc(Lh * sizeof(double));
	double *Q2 = (double*)malloc(Lh * Lh * sizeof(double));
	double *Q2T = (double*)malloc(Lh * Lh * sizeof(double));
	double *P1 = (double*)malloc(Lh * Lh * sizeof(double));
	double *P1Q2_mult = (double*)malloc(Lh * Lh * sizeof(double));
	double *R = (double*)malloc(Lh * Lh * sizeof(double));
	double *RT = (double*)malloc(Lh * Lh * sizeof(double));
	double *phi = (double*)malloc(Lh * sizeof(double));
	double *d = (double*)malloc(Lh * sizeof(double));
	ulin_mem ulinM;
	preallocate_ulinsys_qr(&ulinM, Lh, sparseMtx.numCon);
	for (k = 0; k < Kiter; k++)
	{
		double norm_ht = 0.0;
		for (i = 0; i < Lh; i++)
			norm_ht += h[i] * h[i];
		norm_ht = sqrt(norm_ht);
		eqconTransposeB(h, N, L, &sparseMtx, a, A, b);
		unsigned int dim2_Q2 = ulinsys_qr(&ulinM, A, Lh, sparseMtx.numCon, b, Q2, ds);
		if (k > 1 && flag == 0)
		{
			if (fabs(previous2 - previous1) < epsil1)
				flag = 1;
		}
		if (flag || k > Kiter1)
			memcpy(dt, ds, Lh * sizeof(double));
		else
		{
			for (i = 0; i < Lh; i++)
				dt[i] = ds[i] + h[i];
		}
		transpose(Q2, Q2T, Lh, dim2_Q2);
		matrix_transposedMatrix_product(Q2T, PT, P1, dim2_Q2, Lh, Lh, Lh);
		matrix_transposedMatrix_product(P1, Q2T, P1Q2_mult, dim2_Q2, Lh, Lh, dim2_Q2);
		memset(R, 0, dim2_Q2 * dim2_Q2 * sizeof(double));
		cholesky(P1Q2_mult, R, dim2_Q2);
		matrix_vector_mult(P1, dt, P1Q2_mult, dim2_Q2, Lh, Lh);
		forwardSubstitution(R, dim2_Q2, P1Q2_mult, d);
		transpose(R, RT, dim2_Q2, dim2_Q2);
		backwardSubstitution(RT, dim2_Q2, d, phi);
		matrix_vector_mult(Q2, phi, d, Lh, dim2_Q2, dim2_Q2);
		for (i = 0; i < Lh; i++)
			d[i] = ds[i] - d[i];
		double norm_d = 0.0, sum_d = 0.0;
		double max_d = d[0];
		for (i = 0; i < Lh; i++)
		{
			double abs_d = fabs(d[i]);
			sum_d += abs_d;
			norm_d += d[i] * d[i];
			if (abs_d > max_d)
				max_d = abs_d;
		}
		sum_d = sum_d / Lh;
		norm_d = sqrt(norm_d);
		if (sum_d > 1.0)
		{
			if (!problematic)
			{
				for (i = 0; i < Lh; i++)
					h[i] = d[i] / max_d * fc;
				problematic = 1;
				continue;
			}
			printf("Optimization go wrong, df parameter probably too large\n");
			memset(h_opt, 0, L * sizeof(double));
		}
		for (i = 0; i < Lh; i++)
			h[i] += d[i];
		previous2 = previous1;
		previous1 = norm_d / norm_ht;
		if (previous1 < epsil)
			break;
	}
	double totalPwr = 0.0;
	for (i = 0; i < Lh; i++)
	{
		h_opt[i] = h[i];
		h_opt[L - i - 1] = h[i];
		totalPwr += h_opt[i];
	}
	totalPwr = totalPwr * 2.0;
	double squareRt_N = sqrt(N);
	double norhpt = 0.0;
	if (fabs(totalPwr) > 0.0)
	{
		for (i = 0; i < L; i++)
		{
			h_opt[i] = squareRt_N * h_opt[i] / totalPwr;
			norhpt += h_opt[i] * h_opt[i];
		}
	}
	else
	{
		for (i = 0; i < L; i++)
		{
			h_opt[i] = squareRt_N * h_opt[i];
			norhpt += h_opt[i] * h_opt[i];
		}
	}
	norhpt = sqrt(norhpt);
	const double squareRt_2 = sqrt(2.0);
	if (norhpt > DBL_EPSILON)
	{
		for (i = 0; i < L; i++)
			h_opt[i] = squareRt_2 * h_opt[i] / norhpt;
	}
	else
	{
		for (i = 0; i < L; i++)
			h_opt[i] = squareRt_2 * h_opt[i];
	}
	free(h);
	free(PT);
	free(C);
	free(dt);
	free(A);
	free(b);
	free(ds);
	free(Q2);
	free(Q2T);
	free(P1);
	free(P1Q2_mult);
	free(R);
	free(RT);
	free(phi);
	free(d);
	free(sparseMtx.positionQ);
	free(sparseMtx.positionQQ);
	free(sparseMtx.correspondingValueQ);
	free(sparseMtx.correspondingValueQQ);
	free_ulinsys_qr(&ulinM);
}