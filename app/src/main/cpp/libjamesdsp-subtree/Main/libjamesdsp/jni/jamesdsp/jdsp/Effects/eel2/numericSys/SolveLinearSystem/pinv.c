#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <float.h>
#include "../quadprog.h"
void ppp(int32_t m, int32_t n, double *a, double *v, double *s, double *e)
{
	int32_t i, j;
	double d;
	if (m >= n)
		i = n;
	else
		i = m;
	for (j = 1; j <= i - 1; j++)
	{
		a[(j - 1) * n + j - 1] = s[j - 1];
		a[(j - 1) * n + j] = e[j - 1];
	}
	a[(i - 1) * n + i - 1] = s[i - 1];
	if (m < n)
		a[(i - 1) * n + i] = e[i - 1];
	for (i = 1; i <= n - 1; i++)
		for (j = i + 1; j <= n; j++)
		{
			d = v[(i - 1) * n + j - 1];
			v[(i - 1) * n + j - 1] = v[(j - 1) * n + i - 1];
			v[(j - 1) * n + i - 1] = d;
		}
}
void sss(double *fg, double *cs)
{
	double r, d;
	if ((fabs(fg[0]) + fabs(fg[1])) == 0.0)
	{
		cs[0] = 1.0;
		cs[1] = 0.0;
		d = 0.0;
	}
	else
	{
		d = sqrt(fg[0] * fg[0] + fg[1] * fg[1]);
		if (fabs(fg[0]) > fabs(fg[1]))
		{
			d = fabs(d);
			if (fg[0] < 0.0)
				d = -d;
		}
		if (fabs(fg[1]) >= fabs(fg[0]))
		{
			d = fabs(d);
			if (fg[1] < 0.0)
				d = -d;
		}
		cs[0] = fg[0] / d;
		cs[1] = fg[1] / d;
	}
	r = 1.0;
	if (fabs(fg[0]) > fabs(fg[1]))
		r = cs[1];
	else
		if (cs[0] != 0.0)
			r = 1.0 / cs[0];
	fg[0] = d;
	fg[1] = r;
}
void uav(int32_t m, int32_t n, double *u, double *a, double *v, double *s, double *e, double *w, int32_t iteration)
{
	int32_t i, j, k, l, it, ll, kk, mm, nn, m1, ks;
	double d, dd, t, sm, sm1, em1, sk, ek, b, c, shh;
	it = iteration;
	k = n;
	if (m - 1 < n)
		k = m - 1;
	l = m;
	if (n - 2 < m)
		l = n - 2;
	if (l < 0)
		l = 0;
	ll = k;
	if (l > k)
		ll = l;
	if (ll >= 1)
	{
		for (kk = 1; kk <= ll; kk++)
		{
			if (kk <= k)
			{
				d = 0.0;
				for (i = kk; i <= m; i++)
					d = d + a[(i - 1) * n + kk - 1] * a[(i - 1) * n + kk - 1];
				s[kk - 1] = sqrt(d);
				if (s[kk - 1] != 0.0)
				{
					if (a[(kk - 1) * n + kk - 1] != 0.0)
					{
						s[kk - 1] = fabs(s[kk - 1]);
						if (a[(kk - 1) * n + kk - 1] < 0.0)
							s[kk - 1] = -s[kk - 1];
					}
					for (i = kk; i <= m; i++)
						a[(i - 1) * n + kk - 1] = a[(i - 1) * n + kk - 1] / s[kk - 1];
					a[(kk - 1) * n + kk - 1] = 1.0 + a[(kk - 1) * n + kk - 1];
				}
				s[kk - 1] = -s[kk - 1];
			}
			if (n >= kk + 1)
			{
				for (j = kk + 1; j <= n; j++)
				{
					if ((kk <= k) && (s[kk - 1] != 0.0))
					{
						d = 0.0;
						for (i = kk; i <= m; i++)
							d = d + a[(i - 1) * n + kk - 1] * a[(i - 1) * n + j - 1];
						d = -d / a[(kk - 1) * n + kk - 1];
						for (i = kk; i <= m; i++)
							a[(i - 1) * n + j - 1] = a[(i - 1) * n + j - 1] + d * a[(i - 1) * n + kk - 1];
					}
					e[j - 1] = a[(kk - 1) * n + j - 1];
				}
			}
			if (kk <= k)
			{
				for (i = kk; i <= m; i++)
					u[(i - 1) * m + kk - 1] = a[(i - 1) * n + kk - 1];
			}
			if (kk <= l)
			{
				d = 0.0;
				for (i = kk + 1; i <= n; i++)
					d = d + e[i - 1] * e[i - 1];
				e[kk - 1] = sqrt(d);
				if (e[kk - 1] != 0.0)
				{
					if (e[kk] != 0.0)
					{
						e[kk - 1] = fabs(e[kk - 1]);
						if (e[kk] < 0.0) e[kk - 1] = -e[kk - 1];
					}
					for (i = kk + 1; i <= n; i++)
						e[i - 1] = e[i - 1] / e[kk - 1];
					e[kk] = 1.0 + e[kk];
				}
				e[kk - 1] = -e[kk - 1];
				if ((kk + 1 <= m) && (e[kk - 1] != 0.0))
				{
					for (i = kk + 1; i <= m; i++)
						w[i - 1] = 0.0;
					for (j = kk + 1; j <= n; j++)
						for (i = kk + 1; i <= m; i++)
							w[i - 1] = w[i - 1] + e[j - 1] * a[(i - 1) * n + j - 1];
					for (j = kk + 1; j <= n; j++)
						for (i = kk + 1; i <= m; i++)
							a[(i - 1) * n + j - 1] = a[(i - 1) * n + j - 1] - w[i - 1] * e[j - 1] / e[kk];
				}
				for (i = kk + 1; i <= n; i++)
					v[(i - 1) * n + kk - 1] = e[i - 1];
			}
		}
	}
	mm = n;
	if (m + 1 < n)
		mm = m + 1;
	if (k < n)
		s[k] = a[k * n + k];
	if (m < mm)
		s[mm - 1] = 0.0;
	if (l + 1 < mm)
		e[l] = a[l * n + mm - 1];
	e[mm - 1] = 0.0;
	nn = m;
	if (m > n)
		nn = n;
	if (nn >= k + 1)
	{
		for (j = k + 1; j <= nn; j++)
		{
			for (i = 1; i <= m; i++)
				u[(i - 1) * m + j - 1] = 0.0;
			u[(j - 1) * m + j - 1] = 1.0;
		}
	}
	if (k >= 1)
	{
		for (ll = 1; ll <= k; ll++)
		{
			kk = k - ll + 1;
			if (s[kk - 1] != 0.0)
			{
				if (nn >= kk + 1)
					for (j = kk + 1; j <= nn; j++)
					{
						d = 0.0;
						for (i = kk; i <= m; i++)
							d = d + u[(i - 1) * m + kk - 1] * u[(i - 1) * m + j - 1] / u[(kk - 1) * m + kk - 1];
						d = -d;
						for (i = kk; i <= m; i++)
							u[(i - 1) * m + j - 1] = u[(i - 1) * m + j - 1] + d * u[(i - 1) * m + kk - 1];
					}
				for (i = kk; i <= m; i++)
					u[(i - 1) * m + kk - 1] = -u[(i - 1) * m + kk - 1];
				u[(kk - 1) * m + kk - 1] = 1.0 + u[(kk - 1) * m + kk - 1];
				if (kk - 1 >= 1)
					for (i = 1; i <= kk - 1; i++)
						u[(i - 1) * m + kk - 1] = 0.0;
			}
			else
			{
				for (i = 1; i <= m; i++)
					u[(i - 1) * m + kk - 1] = 0.0;
				u[(kk - 1) * m + kk - 1] = 1.0;
			}
		}
	}
	for (ll = 1; ll <= n; ll++)
	{
		kk = n - ll + 1;
		if ((kk <= l) && (e[kk - 1] != 0.0))
		{
			for (j = kk + 1; j <= n; j++)
			{
				d = 0.0;
				for (i = kk + 1; i <= n; i++)
					d = d + v[(i - 1) * n + kk - 1] * v[(i - 1) * n + j - 1] / v[kk * n + kk - 1];
				d = -d;
				for (i = kk + 1; i <= n; i++)
					v[(i - 1) * n + j - 1] = v[(i - 1) * n + j - 1] + d * v[(i - 1) * n + kk - 1];
			}
		}
		for (i = 1; i <= n; i++)
			v[(i - 1) * n + kk - 1] = 0.0;
		v[(kk - 1) * n + kk - 1] = 1.0;
	}
	for (i = 1; i <= m; i++)
		for (j = 1; j <= n; j++)
			a[(i - 1) * n + j - 1] = 0.0;
	m1 = mm;
	it = iteration;
	while (1)
	{
		if (!mm)
		{
			ppp(m, n, a, v, s, e);
			return;
		}
		if (!it)
		{
			ppp(m, n, a, v, s, e);
			return;
		}
		kk = mm - 1;
		while ((kk != 0) && (fabs(e[kk - 1]) != 0.0))
		{
			d = fabs(s[kk - 1]) + fabs(s[kk]);
			dd = fabs(e[kk - 1]);
			if (dd > DBL_EPSILON * d)
				kk = kk - 1;
			else
				e[kk - 1] = 0.0;
		}
		if (kk == mm - 1)
		{
			kk = kk + 1;
			if (s[kk - 1] < 0.0)
			{
				s[kk - 1] = -s[kk - 1];
				for (i = 1; i <= n; i++)
					v[(i - 1) * n + kk - 1] = -v[(i - 1) * n + kk - 1];
			}
			while ((kk != m1) && (s[kk - 1] < s[kk]))
			{
				d = s[kk - 1]; s[kk - 1] = s[kk]; s[kk] = d;
				if (kk < n)
					for (i = 1; i <= n; i++)
					{
						d = v[(i - 1) * n + kk - 1];
						v[(i - 1) * n + kk - 1] = v[(i - 1) * n + kk];
						v[(i - 1) * n + kk] = d;
					}
				if (kk < m)
					for (i = 1; i <= m; i++)
					{
						d = u[(i - 1) * m + kk - 1];
						u[(i - 1) * m + kk - 1] = u[(i - 1) * m + kk];
						u[(i - 1) * m + kk] = d;
					}
				kk = kk + 1;
			}
			it = iteration;
			mm = mm - 1;
		}
		else
		{
			ks = mm;
			while ((ks > kk) && (fabs(s[ks - 1]) != 0.0))
			{
				d = 0.0;
				if (ks != mm)
					d = d + fabs(e[ks - 1]);
				if (ks != kk + 1)
					d = d + fabs(e[ks - 2]);
				dd = fabs(s[ks - 1]);
				if (dd > DBL_EPSILON * d)
					ks = ks - 1;
				else
					s[ks - 1] = 0.0;
			}
			double fg[2];
			double cs[2];
			if (ks == kk)
			{
				kk = kk + 1;
				d = fabs(s[mm - 1]);
				t = fabs(s[mm - 2]);
				if (t > d)
					d = t;
				t = fabs(e[mm - 2]);
				if (t > d)
					d = t;
				t = fabs(s[kk - 1]);
				if (t > d)
					d = t;
				t = fabs(e[kk - 1]);
				if (t > d)
					d = t;
				sm = s[mm - 1] / d; sm1 = s[mm - 2] / d;
				em1 = e[mm - 2] / d;
				sk = s[kk - 1] / d; ek = e[kk - 1] / d;
				b = ((sm1 + sm)*(sm1 - sm) + em1 * em1) / 2.0;
				c = sm * em1; c = c * c; shh = 0.0;
				if ((b != 0.0) || (c != 0.0))
				{
					shh = sqrt(b*b + c);
					if (b < 0.0)
						shh = -shh;
					shh = c / (b + shh);
				}
				fg[0] = (sk + sm)*(sk - sm) - shh;
				fg[1] = sk * ek;
				for (i = kk; i <= mm - 1; i++)
				{
					sss(fg, cs);
					if (i != kk)
						e[i - 2] = fg[0];
					fg[0] = cs[0] * s[i - 1] + cs[1] * e[i - 1];
					e[i - 1] = cs[0] * e[i - 1] - cs[1] * s[i - 1];
					fg[1] = cs[1] * s[i];
					s[i] = cs[0] * s[i];
					if ((cs[0] != 1.0) || (cs[1] != 0.0))
						for (j = 1; j <= n; j++)
						{
							d = cs[0] * v[(j - 1) * n + i - 1] + cs[1] * v[(j - 1) * n + i];
							v[(j - 1) * n + i] = -cs[1] * v[(j - 1) * n + i - 1] + cs[0] * v[(j - 1) * n + i];
							v[(j - 1) * n + i - 1] = d;
						}
					sss(fg, cs);
					s[i - 1] = fg[0];
					fg[0] = cs[0] * e[i - 1] + cs[1] * s[i];
					s[i] = -cs[1] * e[i - 1] + cs[0] * s[i];
					fg[1] = cs[1] * e[i];
					e[i] = cs[0] * e[i];
					if (i < m)
						if ((cs[0] != 1.0) || (cs[1] != 0.0))
							for (j = 1; j <= m; j++)
							{
								d = cs[0] * u[(j - 1) * m + i - 1] + cs[1] * u[(j - 1) * m + i];
								u[(j - 1) * m + i] = -cs[1] * u[(j - 1) * m + i - 1] + cs[0] * u[(j - 1) * m + i];
								u[(j - 1) * m + i - 1] = d;
							}
				}
				e[mm - 2] = fg[0];
				it = it - 1;
			}
			else
			{
				if (ks == mm)
				{
					kk = kk + 1;
					fg[1] = e[mm - 2]; e[mm - 2] = 0.0;
					for (ll = kk; ll <= mm - 1; ll++)
					{
						i = mm + kk - ll - 1;
						fg[0] = s[i - 1];
						sss(fg, cs);
						s[i - 1] = fg[0];
						if (i != kk)
						{
							fg[1] = -cs[1] * e[i - 2];
							e[i - 2] = cs[0] * e[i - 2];
						}
						if ((cs[0] != 1.0) || (cs[1] != 0.0))
							for (j = 1; j <= n; j++)
							{
								d = cs[0] * v[(j - 1) * n + i - 1] + cs[1] * v[(j - 1) * n + mm - 1];
								v[(j - 1) * n + mm - 1] = -cs[1] * v[(j - 1) * n + i - 1] + cs[0] * v[(j - 1) * n + mm - 1];
								v[(j - 1) * n + i - 1] = d;
							}
					}
				}
				else
				{
					kk = ks + 1;
					fg[1] = e[kk - 2];
					e[kk - 2] = 0.0;
					for (i = kk; i <= mm; i++)
					{
						fg[0] = s[i - 1];
						sss(fg, cs);
						s[i - 1] = fg[0];
						fg[1] = -cs[1] * e[i - 1];
						e[i - 1] = cs[0] * e[i - 1];
						if ((cs[0] != 1.0) || (cs[1] != 0.0))
							for (j = 1; j <= m; j++)
							{
								d = cs[0] * u[(j - 1) * m + i - 1] + cs[1] * u[(j - 1) * m + kk - 2];
								u[(j - 1) * m + kk - 2] = -cs[1] * u[(j - 1) * m + i - 1] + cs[0] * u[(j - 1) * m + kk - 2];
								u[(j - 1) * m + i - 1] = d;
							}
					}
				}
			}
		}
	}
}
#ifndef max
#define max(a,b) (((a) > (b)) ? (a) : (b))
#endif
void pinv(double *A, int32_t m, int32_t n, double *Y, int32_t *size)
{
	int32_t ka = (max(m, n) + 1);
	double *u = (double*)malloc(((m * m) + (m * n) + (n * n) + ka * 3) * sizeof(double));
	double *a = u + m * m;
	double *v = a + m * n;
	double *s = v + n * n;
	double *e = s + ka;
	double *w = e + ka;
	size[0] = n;
	size[1] = m;
	int32_t i, j, k, l;
	memcpy(a, A, m * n * sizeof(double)); // Out-of-place
	uav(m, n, u, a, v, s, e, w, 100);
	j = n;
	if (m < n)
		j = m;
	j = j - 1;
	k = 0;
	while ((k <= j) && (a[k * n + k] != 0.0))
		k = k + 1;
	k = k - 1;
	double sum;
	for (i = 0; i <= n - 1; i++)
		for (j = 0; j <= m - 1; j++)
		{
			sum = 0.0;
			for (l = 0; l <= k; l++)
				sum += v[l * n + i] * u[j * m + l] / a[l * n + l];
			Y[i * m + j] = sum;
		}
	free(u);
}
