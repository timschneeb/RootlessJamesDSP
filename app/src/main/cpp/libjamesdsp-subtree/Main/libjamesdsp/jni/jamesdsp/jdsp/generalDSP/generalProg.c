#include <stddef.h>
#include <stdint.h>
#include <math.h>
#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif
double mapVal(double x, double in_min, double in_max, double out_min, double out_max)
{
	return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
}
// linear -> dB conversion
double mag2dB(double lin)
{
	const double LOG_2_DB = 8.6858896380650365530225783783321;	// 20 / ln( 10 )
	return log(lin) * LOG_2_DB;
}
// dB -> linear conversion
float db2magf(double dB)
{
	const double DB_2_LOG = 0.11512925464970228420089957273422;	// ln( 10 ) / 20
	return (float)exp(dB * DB_2_LOG);
}
// dB -> linear conversion
double db2mag(double dB)
{
	const double DB_2_LOG = 0.11512925464970228420089957273422;	// ln( 10 ) / 20
	return exp(dB * DB_2_LOG);
}
void linspace(double *x, int n, double a, double b)
{
	int i;
	double d = (b - a) / (double)(n - 1);
	for (i = 0; i < n; i++)
		x[i] = a + i * d;
}
void channel_joinFloat(float **chan_buffers, unsigned int num_channels, float *buffer, unsigned int num_frames)
{
	unsigned int i, samples = num_frames * num_channels;
	for (i = 0; i < samples; i++)
		buffer[i] = chan_buffers[i % num_channels][i / num_channels];
}
void channel_join(double **chan_buffers, unsigned int num_channels, double *buffer, unsigned int num_frames)
{
	unsigned int i, samples = num_frames * num_channels;
	for (i = 0; i < samples; i++)
		buffer[i] = chan_buffers[i % num_channels][i / num_channels];
}
void channel_split(double *buffer, unsigned int num_frames, double **chan_buffers, unsigned int num_channels)
{
	unsigned int i, samples = num_frames * num_channels;
	for (i = 0; i < samples; i++)
		chan_buffers[i % num_channels][i / num_channels] = buffer[i];
}
void channel_splitFloat(float *buffer, unsigned int num_frames, float **chan_buffers, unsigned int num_channels)
{
	unsigned int i, samples = num_frames * num_channels;
	for (i = 0; i < samples; i++)
		chan_buffers[i % num_channels][i / num_channels] = buffer[i];
}
void normalise(float *buffer, int num_samps)
{
	int i;
	float max = 0.0f;
	for (i = 0; i < num_samps; i++)
		max = fabsf(buffer[i]) < max ? max : fabsf(buffer[i]);
	max = 1.0f / max;
	for (i = 0; i < num_samps; i++)
		buffer[i] *= max;
}
/* CRC-32C (iSCSI) polynomial in reversed bit order. */
#define POLY 0x82f63b78
/* CRC-32 (Ethernet, ZIP, etc.) polynomial in reversed bit order. */
/* #define POLY 0xedb88320 */
unsigned int crc32c(const unsigned char *buf, size_t len)
{
	int k;
	unsigned int crc = 0;
	crc = ~crc;
	while (len--) {
		crc ^= *buf++;
		for (k = 0; k < 8; k++)
			crc = crc & 1 ? (crc >> 1) ^ POLY : crc >> 1;
	}
	return ~crc;
}
int upper_bound(double *a, int n, double x)
{
	int l = 0;
	int h = n;
	while (l < h)
	{
		int mid = l + (h - l) / 2;
		if (x >= a[mid])
			l = mid + 1;
		else
			h = mid;
	}
	return l;
}
int lower_bound(double *a, int n, double x)
{
	int l = 0;
	int h = n;
	while (l < h)
	{
		int mid = l + (h - l) / 2;
		if (x <= a[mid])
			h = mid;
		else
			l = mid + 1;
	}
	return l;
}
size_t fast_upper_bound(double *a, size_t n, double x)
{
	size_t low = 0;
	while (n > 0)
	{
		size_t half = n >> 1;
		size_t other_half = n - half;
		size_t probe = low + half;
		size_t other_low = low + other_half;
		double v = a[probe];
		n = half;
		low = x >= v ? other_low : low;
	}
	return low;
}
size_t fast_lower_bound(double *a, size_t n, double x)
{
	size_t low = 0;
	while (n > 0)
	{
		size_t half = n >> 1;
		size_t other_half = n - half;
		size_t probe = low + half;
		size_t other_low = low + other_half;
		double v = a[probe];
		n = half;
		low = x <= v ? low : other_low;
	}
	return low;
}
void LLdiscreteHartleyFloat(float *A, const int nPoints, const float *sinTab)
{
	int i, j, n, n2, theta_inc, nptDiv2;
	float alpha, beta;
	// FHT - stage 1 and 2 (2 and 4 points)
	for (i = 0; i < nPoints; i += 4)
	{
		const float	x0 = A[i];
		const float	x1 = A[i + 1];
		const float	x2 = A[i + 2];
		const float	x3 = A[i + 3];
		const float	y0 = x0 + x1;
		const float	y1 = x0 - x1;
		const float	y2 = x2 + x3;
		const float	y3 = x2 - x3;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	// FHT - stage 3 (8 points)
	for (i = 0; i < nPoints; i += 8)
	{
		alpha = A[i];
		beta = A[i + 4];
		A[i] = alpha + beta;
		A[i + 4] = alpha - beta;
		alpha = A[i + 2];
		beta = A[i + 6];
		A[i + 2] = alpha + beta;
		A[i + 6] = alpha - beta;
		alpha = A[i + 1];
		const float beta1 = 0.70710678118654752440084436210485f*(A[i + 5] + A[i + 7]);
		const float beta2 = 0.70710678118654752440084436210485f*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	n = 16;
	n2 = 8;
	theta_inc = nPoints >> 4;
	nptDiv2 = nPoints >> 2;
	while (n <= nPoints)
	{
		for (i = 0; i < nPoints; i += n)
		{
			int theta = theta_inc;
			const int n4 = n2 >> 1;
			alpha = A[i];
			beta = A[i + n2];
			A[i] = alpha + beta;
			A[i + n2] = alpha - beta;
			alpha = A[i + n4];
			beta = A[i + n2 + n4];
			A[i + n4] = alpha + beta;
			A[i + n2 + n4] = alpha - beta;
			for (j = 1; j < n4; j++)
			{
				float	sinval = sinTab[theta];
				float	cosval = sinTab[theta + nptDiv2];
				float	alpha1 = A[i + j];
				float	alpha2 = A[i - j + n2];
				float	beta1 = A[i + j + n2] * cosval + A[i - j + n] * sinval;
				float	beta2 = A[i + j + n2] * sinval - A[i - j + n] * cosval;
				theta += theta_inc;
				A[i + j] = alpha1 + beta1;
				A[i + j + n2] = alpha1 - beta1;
				A[i - j + n2] = alpha2 + beta2;
				A[i - j + n] = alpha2 - beta2;
			}
		}
		n <<= 1;
		n2 <<= 1;
		theta_inc >>= 1;
	}
}
static inline uint64_t rotl(const uint64_t x, int k) {
	return (x << k) | (x >> (64 - k));
}
uint64_t next(uint64_t s[2])
{
	const uint64_t s0 = s[0];
	uint64_t s1 = s[1];
	const uint64_t result = rotl(s0 * 5, 7) * 9;
	s1 ^= s0;
	s[0] = rotl(s0, 24) ^ s1 ^ (s1 << 16); // a, b
	s[1] = rotl(s1, 37); // c
	return result;
}
double randXorshift(uint64_t s[2])
{
	const union { uint64_t i; double d; } u = { .i = UINT64_C(0x3FF) << 52 | next(s) >> 12 };
	return 2.0 * (u.d - 1.5);
}
unsigned int updateSplane(unsigned int order, double *QValsList)
{
	unsigned int pairs = order >> 1;
	unsigned int oddPoles = order & 1;
	double poleInc = M_PI / (double)order;

	// show coefficients
	double firstAngle = poleInc;
	unsigned int offset;
	if (!oddPoles)
	{
		firstAngle = firstAngle / 2.0;
		offset = 0;
	}
	else
	{
		QValsList[0] = 0.5;
		offset = 1;
	}
	for (unsigned int idx = 0; idx < pairs; idx++)
		QValsList[offset + idx] = 1.0 / (2.0 * cos(firstAngle + idx * poleInc));
	return pairs + offset;
}