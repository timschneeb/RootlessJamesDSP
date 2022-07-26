#include <math.h>
#include "../jdsp_header.h"
#ifndef M_PI
#define M_PI  3.14159265358979323846
#endif
int BS2BCalculateflevel(unsigned int fcut, unsigned int gain)
{
	return (int)(fcut | (gain << 16));
}
void BS2BInit(t_bs2bdp *bs2bdp, unsigned int samplerate, int flevel)
{
	double Fc_lo; /* Lowpass filter cut frequency (Hz) */
	double Fc_hi; /* Highboost filter cut frequency (Hz) */
	double G_lo;  /* Lowpass filter gain (multiplier) */
	double G_hi;  /* Highboost filter gain (multiplier) */
	double GB_lo; /* Lowpass filter gain (dB) */
	double GB_hi; /* Highboost filter gain (dB) ( 0 dB is highs ) */
	double level; /* Feeding level (dB) ( level = GB_lo - GB_hi ) */
	double x;
	Fc_lo = (double)(flevel & 0xffff);
	level = (double)((flevel & 0xffff0000) >> 16);
	if((Fc_lo > BS2B_MAXFCUT) || (Fc_lo < BS2B_MINFCUT) || (level > BS2B_MAXFEED) || (level < BS2B_MINFEED))
	{
		flevel = BS2B_DEFAULT_CLEVEL;
		Fc_lo = (double)(flevel & 0xffff);
		level = (double)((flevel & 0xffff0000) >> 16);
	}
	level /= 10.0;
	GB_lo = level * -5.0 / 6.0 - 3.0;
	GB_hi = level / 6.0 - 3.0;
	G_lo  = pow( 10, GB_lo / 20.0 );
	G_hi  = 1.0 - pow( 10, GB_hi / 20.0 );
	Fc_hi = Fc_lo * pow( 2.0, ( GB_lo - 20.0 * log10( G_hi ) ) / 12.0 );
	x = exp(-2.0 * M_PI * Fc_lo / samplerate);
	bs2bdp->b1_lo = x;
	bs2bdp->a0_lo = G_lo * (1.0 - x);
	x = exp(-2.0 * M_PI * Fc_hi / samplerate);
	bs2bdp->b1_hi = x;
	bs2bdp->a0_hi = 1.0 - G_hi * (1.0 - x);
	bs2bdp->a1_hi = -x;
	bs2bdp->gain = 1.0 / (1.0 - G_hi + G_lo);
}
/* Single pole IIR filter.
 * O[n] = a0*I[n] + a1*I[n-1] + b1*O[n-1]
 */
/* Lowpass filter */
#define lo_filter( in, out_1 ) \
	( bs2bdp->a0_lo * in + bs2bdp->b1_lo * out_1 )
/* Highboost filter */
#define hi_filter( in, in_1, out_1 ) \
	( bs2bdp->a0_hi * in + bs2bdp->a1_hi * in_1 + bs2bdp->b1_hi * out_1 )
void BS2BProcess(t_bs2bdp *bs2bdp, double *sampleL, double *sampleR)
{
	/* Lowpass filter */
	bs2bdp->lfs.lo[0] = lo_filter(*sampleL, bs2bdp->lfs.lo[0]);
	bs2bdp->lfs.lo[1] = lo_filter(*sampleR, bs2bdp->lfs.lo[1]);
	/* Highboost filter */
	bs2bdp->lfs.hi[0] = hi_filter(*sampleL, bs2bdp->lfs.asis[0], bs2bdp->lfs.hi[0]);
	bs2bdp->lfs.hi[1] = hi_filter(*sampleR, bs2bdp->lfs.asis[1], bs2bdp->lfs.hi[1]);
	bs2bdp->lfs.asis[0] = *sampleL;
	bs2bdp->lfs.asis[1] = *sampleR;
	/* Crossfeed */
	*sampleL = bs2bdp->lfs.hi[0] + bs2bdp->lfs.lo[1];
	*sampleR = bs2bdp->lfs.hi[1] + bs2bdp->lfs.lo[0];
	/* Bass boost cause allpass attenuation */
	*sampleL *= bs2bdp->gain;
	*sampleR *= bs2bdp->gain;
}