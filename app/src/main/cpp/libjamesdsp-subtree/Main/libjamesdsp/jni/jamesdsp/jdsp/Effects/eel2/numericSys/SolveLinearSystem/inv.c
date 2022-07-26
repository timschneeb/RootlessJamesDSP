#include <math.h>
#include <stdlib.h>
#include <string.h>
#include "../quadprog.h"
void inv(const double *A, const int32_t n, double *Y)
{
	if (n == 1)
	{
		Y[0] = 1.0 / A[0];
		return;
	}
	int32_t i, j, k, yk, i2, jA, ldap1, mmj_tmp, jp1j, jj, ix;
	double smax, s;
	char *workingBuffer = (char*)malloc(((n * n * sizeof(double)) + (n * 2 * sizeof(int32_t))) * sizeof(char));
	double *b_x_data = (double*)workingBuffer;
	int32_t *perm1 = (int32_t*)(b_x_data + n * n);
	int32_t *perm2 = perm1 + n;
	memset(Y, 0, n * n * sizeof(double));
	memcpy(b_x_data, A, n * n * sizeof(double));
	perm1[0] = 1;
	yk = 1;
	for (k = 2; k <= n; k++)
		perm1[k - 1] = ++yk;
	ldap1 = n + 1;
	for (j = 0; j < n - 1; j++)
	{
		mmj_tmp = n - j;
		jA = j * ldap1;
		jj = j * ldap1;
		jp1j = jA + 2;
		yk = 0;
		ix = jA;
		smax = fabs(b_x_data[jA]);
		for (k = 2; k <= mmj_tmp; k++)
		{
			ix++;
			s = fabs(b_x_data[ix]);
			if (s > smax)
			{
				yk = k - 1;
				smax = s;
			}
		}
		if (b_x_data[jj + yk] != 0.0)
		{
			if (yk != 0)
			{
				yk += j;
				perm1[j] = yk + 1;
				ix = j;
				for (k = 0; k < n; k++)
				{
					smax = b_x_data[ix];
					b_x_data[ix] = b_x_data[yk];
					b_x_data[yk] = smax;
					ix += n;
					yk += n;
				}
			}
			for (i = jp1j; i <= jj + mmj_tmp; i++)
				b_x_data[i - 1] /= b_x_data[jj];
		}
		yk = jA + n;
		jA = jj + ldap1;
		for (jp1j = 0; jp1j <= mmj_tmp - 2; jp1j++)
		{
			smax = b_x_data[yk];
			if (b_x_data[yk] != 0.0)
			{
				ix = jj + 1;
				i2 = jA + 1;
				k = mmj_tmp + jA;
				for (i = i2; i < k; i++)
				{
					b_x_data[i - 1] += b_x_data[ix] * -smax;
					ix++;
				}
			}
			yk += n;
			jA += n;
		}
	}
	perm2[0] = 1;
	yk = 1;
	for (k = 2; k <= n; k++)
		perm2[k - 1] = ++yk;
	for (k = 0; k < n; k++)
	{
		if (perm1[k] > 1 + k)
		{
			yk = perm2[perm1[k] - 1];
			perm2[perm1[k] - 1] = perm2[k];
			perm2[k] = yk;
		}
	}
	for (k = 0; k < n; k++)
	{
		jp1j = n * (perm2[k] - 1);
		Y[k + jp1j] = 1.0;
		for (j = k + 1; j <= n; j++)
			if (Y[(j + jp1j) - 1] != 0.0)
				for (i = j + 1; i <= n; i++)
					Y[(i + jp1j) - 1] -= Y[(j + n * (perm2[k] - 1)) - 1] * b_x_data[(i + n * (j - 1)) - 1];
	}
	for (j = 0; j < n; j++)
	{
		yk = n * j - 1;
		for (k = n; k >= 1; k--)
		{
			jA = n * (k - 1) - 1;
			i2 = k + yk;
			if (Y[i2] != 0.0)
			{
				Y[i2] /= b_x_data[k + jA];
				for (i = 0; i <= k - 2; i++)
					Y[(i + yk) + 1] -= Y[i2] * b_x_data[(i + jA) + 1];
			}
		}
	}
	free(workingBuffer);
}