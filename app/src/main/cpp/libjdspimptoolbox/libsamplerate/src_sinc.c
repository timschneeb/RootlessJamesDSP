/*
** Copyright (c) 2002-2021, Erik de Castro Lopo <erikd@mega-nerd.com>
** All rights reserved.
**
** This code is released under 2-clause BSD license. Please see the
** file at : https://github.com/libsndfile/libsamplerate/blob/master/COPYING
*/

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <float.h>

#include "common.h"
#ifndef M_PI
#define M_PI 3.14159265358979323846264338328
#endif
// Filter generator start
double sinc(double x)
{
	if (x != 0.0)
		return sin(M_PI * x) / (M_PI * x);
	else
		return 1.0;
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
double chbevl(double x, double array[], int n)
{
	double b0, b1, b2, *p;
	int i;

	p = array;
	b0 = *p++;
	b1 = 0.0;
	i = n - 1;

	do {
		b2 = b1;
		b1 = b0;
		b0 = x * b1 - b2 + *p++;
	} while (--i);

	return(0.5 * (b0 - b2));
}
/* Chebyshev coefficients for exp(-x) I0(x)
 * in the interval [0,8].
 *
 * lim(x->0){ exp(-x) I0(x) } = 1.
 */
static double A[] = {
  -4.41534164647933937950E-18,
  3.33079451882223809783E-17,
  -2.43127984654795469359E-16,
  1.71539128555513303061E-15,
  -1.16853328779934516808E-14,
  7.67618549860493561688E-14,
  -4.85644678311192946090E-13,
  2.95505266312963983461E-12,
  -1.72682629144155570723E-11,
  9.67580903537323691224E-11,
  -5.18979560163526290666E-10,
  2.65982372468238665035E-9,
  -1.30002500998624804212E-8,
  6.04699502254191894932E-8,
  -2.67079385394061173391E-7,
  1.11738753912010371815E-6,
  -4.41673835845875056359E-6,
  1.64484480707288970893E-5,
  -5.75419501008210370398E-5,
  1.88502885095841655729E-4,
  -5.76375574538582365885E-4,
  1.63947561694133579842E-3,
  -4.32430999505057594430E-3,
  1.05464603945949983183E-2,
  -2.37374148058994688156E-2,
  4.93052842396707084878E-2,
  -9.49010970480476444210E-2,
  1.71620901522208775349E-1,
  -3.04682672343198398683E-1,
  6.76795274409476084995E-1
};
/* Chebyshev coefficients for exp(-x) sqrt(x) I0(x)
 * in the inverted interval [8,infinity].
 *
 * lim(x->inf){ exp(-x) sqrt(x) I0(x) } = 1/sqrt(2pi).
 */
static double B[] = {
  -7.23318048787475395456E-18,
  -4.83050448594418207126E-18,
  4.46562142029675999901E-17,
  3.46122286769746109310E-17,
  -2.82762398051658348494E-16,
  -3.42548561967721913462E-16,
  1.77256013305652638360E-15,
  3.81168066935262242075E-15,
  -9.55484669882830764870E-15,
  -4.15056934728722208663E-14,
  1.54008621752140982691E-14,
  3.85277838274214270114E-13,
  7.18012445138366623367E-13,
  -1.79417853150680611778E-12,
  -1.32158118404477131188E-11,
  -3.14991652796324136454E-11,
  1.18891471078464383424E-11,
  4.94060238822496958910E-10,
  3.39623202570838634515E-9,
  2.26666899049817806459E-8,
  2.04891858946906374183E-7,
  2.89137052083475648297E-6,
  6.88975834691682398426E-5,
  3.36911647825569408990E-3,
  8.04490411014108831608E-1
};
double i0(double x)
{
	double y;

	if (x < 0)
		x = -x;
	if (x <= 8.0) {
		y = (x / 2.0) - 2.0;
		return(exp(x) * chbevl(y, A, 30));
	}
	return(exp(x) * chbevl(32.0 / x - 2.0, B, 25) / sqrt(x));
}
unsigned int getcoeff(double ratio, double atten, unsigned long long N, unsigned int incre, float *y)
{
	unsigned long long m;
	double bta = (atten + 0.5) / 10;
	double bes = 1.0 / fabs(i0(bta));
	double xind = 1.0 / ((N - 1) * (N - 1));
	double sum = 0.0;
	y[0] = 1.0;
	for (m = 1; m < ((N - 1) >> 1) + 1; m++)
	{
		double w = i0(bta * sqrt(1.0 - (double)(4 * m * m) * xind)) * bes;
		double f = sinc(m / ratio) * w;
		y[m] = f;
		sum += f;
	}
	sum = 1.0 / (y[0] + sum * 2.0);
	for (m = 0; m < ((N - 1) >> 1) + 1; m++)
		y[m] = (y[m] * incre) * sum;
	return ((N - 1) >> 1) + 1;
}
// Filter generator end
#define	SINC_MAGIC_MARKER	MAKE_MAGIC (' ', 's', 'i', 'n', 'c', ' ')

/*========================================================================================
*/

#define MAKE_INCREMENT_T(x) 	((increment_t) (x))

#define	SHIFT_BITS				12
#define	FP_ONE					((double) (((increment_t) 1) << SHIFT_BITS))
#define	INV_FP_ONE				(1.0 / FP_ONE)

/* Customixe max channls from Kconfig. */
#ifndef CONFIG_CHAN_NR
#define MAX_CHANNELS			128
#else
#define MAX_CHANNELS 			CONFIG_CHAN_NR
#endif

/*========================================================================================
*/

typedef int32_t increment_t ;
typedef float	coeff_t ;
typedef int _CHECK_SHIFT_BITS[2 * (SHIFT_BITS < sizeof (increment_t) * 8 - 1) - 1]; /* sanity check. */

typedef struct
{	int		sinc_magic_marker ;

	long	in_count, in_used ;
	long	out_count, out_gen ;

	int		coeff_half_len, index_inc ;

	double	src_ratio, input_index ;

	coeff_t *coeffs ;

	int		b_current, b_end, b_real_end, b_len ;

	/* Sure hope noone does more than 128 channels at once. */
	double left_calc [MAX_CHANNELS], right_calc [MAX_CHANNELS] ;

	float	*buffer ;
} SINC_FILTER ;

static SRC_ERROR sinc_multichan_vari_process (SRC_STATE *state, SRC_DATA *data) ;
static SRC_ERROR sinc_hex_vari_process (SRC_STATE *state, SRC_DATA *data) ;
static SRC_ERROR sinc_quad_vari_process (SRC_STATE *state, SRC_DATA *data) ;
static SRC_ERROR sinc_stereo_vari_process (SRC_STATE *state, SRC_DATA *data) ;
static SRC_ERROR sinc_mono_vari_process (SRC_STATE *state, SRC_DATA *data) ;

static SRC_ERROR prepare_data (SINC_FILTER *filter, int channels, SRC_DATA *data, int half_filter_chan_len) WARN_UNUSED ;

static void sinc_reset (SRC_STATE *state) ;
static SRC_STATE *sinc_copy (SRC_STATE *state) ;
static void sinc_close (SRC_STATE *state) ;

static SRC_STATE_VT sinc_multichan_state_vt =
{
	sinc_multichan_vari_process,
	sinc_multichan_vari_process,
	sinc_reset,
	sinc_copy,
	sinc_close
} ;

static SRC_STATE_VT sinc_hex_state_vt =
{
	sinc_hex_vari_process,
	sinc_hex_vari_process,
	sinc_reset,
	sinc_copy,
	sinc_close
} ;

static SRC_STATE_VT sinc_quad_state_vt =
{
	sinc_quad_vari_process,
	sinc_quad_vari_process,
	sinc_reset,
	sinc_copy,
	sinc_close
} ;

static SRC_STATE_VT sinc_stereo_state_vt =
{
	sinc_stereo_vari_process,
	sinc_stereo_vari_process,
	sinc_reset,
	sinc_copy,
	sinc_close
} ;

static SRC_STATE_VT sinc_mono_state_vt =
{
	sinc_mono_vari_process,
	sinc_mono_vari_process,
	sinc_reset,
	sinc_copy,
	sinc_close
} ;

static inline increment_t
double_to_fp (double x)
{	return (increment_t) (psf_lrint ((x) * FP_ONE)) ;
} /* double_to_fp */

static inline increment_t
int_to_fp (int x)
{	return (((increment_t) (x)) << SHIFT_BITS) ;
} /* int_to_fp */

static inline int
fp_to_int (increment_t x)
{	return (((x) >> SHIFT_BITS)) ;
} /* fp_to_int */

static inline increment_t
fp_fraction_part (increment_t x)
{	return ((x) & ((((increment_t) 1) << SHIFT_BITS) - 1)) ;
} /* fp_fraction_part */

static inline double
fp_to_double (increment_t x)
{	return fp_fraction_part (x) * INV_FP_ONE ;
} /* fp_to_double */

static inline int
int_div_ceil (int divident, int divisor) /* == (int) ceil ((float) divident / divisor) */
{	assert (divident >= 0 && divisor > 0) ; /* For positive numbers only */
	return (divident + (divisor - 1)) / divisor ;
}

/*----------------------------------------------------------------------------------------
*/

LIBSAMPLERATE_DLL_PRIVATE const char*
sinc_get_name (int src_enum)
{
	switch (src_enum)
	{	case SRC_SINC_BEST_QUALITY :
			return "Best Sinc Interpolator" ;

		case SRC_SINC_MEDIUM_QUALITY :
			return "Medium Sinc Interpolator" ;

		case SRC_SINC_FASTEST :
			return "Fastest Sinc Interpolator" ;

		default: break ;
		} ;

	return NULL ;
} /* sinc_get_descrition */

LIBSAMPLERATE_DLL_PRIVATE const char*
sinc_get_description (int src_enum)
{
	switch (src_enum)
	{	case SRC_SINC_FASTEST :
			return "Band limited sinc interpolation, fastest, 97dB SNR, 80% BW." ;

		case SRC_SINC_MEDIUM_QUALITY :
			return "Band limited sinc interpolation, medium quality, 121dB SNR, 90% BW." ;

		case SRC_SINC_BEST_QUALITY :
			return "Band limited sinc interpolation, best quality, 144dB SNR, 96% BW." ;

		default :
			break ;
		} ;

	return NULL ;
} /* sinc_get_descrition */
float *gbl_coeff[3];
void clean_lpfcoeff()
{
	for (unsigned int i = 0; i < 3; i++)
	{
		if (gbl_coeff[i])
			free(gbl_coeff[i]);
		gbl_coeff[i] = 0;
	}
}
void precompute_lpfcoeff()
{
	clean_lpfcoeff();
	gbl_coeff[0] = (float *)malloc(2464 * sizeof(float));
	unsigned int halfLen = getcoeff(153.9, 100.3, 4925, 128, gbl_coeff[0]); //1.539440918821971e+02
	for (unsigned int i = halfLen; i < halfLen + 1; i++)
		gbl_coeff[0][i] = 0;
	gbl_coeff[1] = (float *)malloc(22438 * sizeof(float));
	halfLen = getcoeff(520.0, 122.0, 44873, 491, gbl_coeff[1]); //5.342397164871713e+02
	for (unsigned int i = halfLen; i < halfLen + 1; i++)
		gbl_coeff[1][i] = 0;
	gbl_coeff[2] = (float *)malloc(340239 * sizeof(float));
	halfLen = getcoeff(2430.0, 160.0, 680475, 2381, gbl_coeff[2]); //2.465496453815475e+03
	for (unsigned int i = halfLen; i < halfLen + 1; i++)
		gbl_coeff[2][i] = 0;
}
static SINC_FILTER *sinc_filter_new (int converter_type, int channels)
{
	assert (converter_type == SRC_SINC_FASTEST ||
		converter_type == SRC_SINC_MEDIUM_QUALITY ||
		converter_type == SRC_SINC_BEST_QUALITY) ;
	assert (channels > 0 && channels <= MAX_CHANNELS) ;

	SINC_FILTER *priv = (SINC_FILTER *) calloc (1, sizeof (SINC_FILTER)) ;
	if (priv)
	{
		priv->sinc_magic_marker = SINC_MAGIC_MARKER ;
		unsigned int halfLen;
		switch (converter_type)
		{
		case SRC_SINC_FASTEST:
			priv->coeffs = gbl_coeff[0];
			priv->index_inc = 128;
			priv->coeff_half_len = 2464 - 2;
			break;
		case SRC_SINC_MEDIUM_QUALITY:
			priv->coeffs = gbl_coeff[1];
			priv->index_inc = 491;
			priv->coeff_half_len = 22438 - 2;
			break;
		case SRC_SINC_BEST_QUALITY:
			priv->coeffs = gbl_coeff[2];
			priv->index_inc = 2381;
			priv->coeff_half_len = 340239 - 2;
			break;
		}

		priv->b_len = 3 * (int) psf_lrint ((priv->coeff_half_len + 2.0) / priv->index_inc * SRC_MAX_RATIO + 1) ;
		priv->b_len = MAX (priv->b_len, 4096) ;
		priv->b_len *= channels ;
		priv->b_len += 1 ; // There is a <= check against samples_in_hand requiring a buffer bigger than the calculation above


		priv->buffer = (float *) calloc (priv->b_len + channels, sizeof (float)) ;
		if (!priv->buffer)
		{
			free (priv) ;
			priv = NULL ;
		}
	}

	return priv ;
}

LIBSAMPLERATE_DLL_PRIVATE SRC_STATE *
sinc_state_new (int converter_type, int channels, SRC_ERROR *error)
{
	assert (converter_type == SRC_SINC_FASTEST ||
		converter_type == SRC_SINC_MEDIUM_QUALITY ||
		converter_type == SRC_SINC_BEST_QUALITY) ;
	assert (channels > 0) ;
	assert (error != NULL) ;

	if (channels > MAX_CHANNELS)
	{
		*error = SRC_ERR_BAD_CHANNEL_COUNT ;
		return NULL ;
	}

	SRC_STATE *state = (SRC_STATE *) calloc (1, sizeof (SRC_STATE)) ;
	if (!state)
	{
		*error = SRC_ERR_MALLOC_FAILED ;
		return NULL ;
	}

	state->channels = channels ;
	state->mode = SRC_MODE_PROCESS ;

	if (state->channels == 1)
		state->vt = &sinc_mono_state_vt ;
	else if (state->channels == 2)
		state->vt = &sinc_stereo_state_vt ;
	else if (state->channels == 4)
		state->vt = &sinc_quad_state_vt ;
	else if (state->channels == 6)
		state->vt = &sinc_hex_state_vt ;
	else
		state->vt = &sinc_multichan_state_vt ;

	state->private_data = sinc_filter_new (converter_type, state->channels) ;
	if (!state->private_data)
	{
		free (state) ;
		*error = SRC_ERR_MALLOC_FAILED ;
		return NULL ;
	}

	sinc_reset (state) ;

	*error = SRC_ERR_NO_ERROR ;

	return state ;
}

static void
sinc_reset (SRC_STATE *state)
{	SINC_FILTER *filter ;

	filter = (SINC_FILTER*) state->private_data ;
	if (filter == NULL)
		return ;

	filter->b_current = filter->b_end = 0 ;
	filter->b_real_end = -1 ;

	filter->src_ratio = filter->input_index = 0.0 ;

	memset (filter->buffer, 0, filter->b_len * sizeof (filter->buffer [0])) ;

	/* Set this for a sanity check */
	memset (filter->buffer + filter->b_len, 0xAA, state->channels * sizeof (filter->buffer [0])) ;
} /* sinc_reset */

static SRC_STATE *
sinc_copy (SRC_STATE *state)
{
	assert (state != NULL) ;

	if (state->private_data == NULL)
		return NULL ;

	SRC_STATE *to = (SRC_STATE *) calloc (1, sizeof (SRC_STATE)) ;
	if (!to)
		return NULL ;
	memcpy (to, state, sizeof (SRC_STATE)) ;


	SINC_FILTER* from_filter = (SINC_FILTER*) state->private_data ;
	SINC_FILTER *to_filter = (SINC_FILTER *) calloc (1, sizeof (SINC_FILTER)) ;
	if (!to_filter)
	{
		free (to) ;
		return NULL ;
	}
	memcpy (to_filter, from_filter, sizeof (SINC_FILTER)) ;
	to_filter->buffer = (float *) malloc (sizeof (float) * (from_filter->b_len + state->channels)) ;
	if (!to_filter->buffer)
	{
		free (to) ;
		free (to_filter) ;
		return NULL ;
	}
	memcpy (to_filter->buffer, from_filter->buffer, sizeof (float) * (from_filter->b_len + state->channels)) ;

	to->private_data = to_filter ;

	return to ;
} /* sinc_copy */

/*========================================================================================
**	Beware all ye who dare pass this point. There be dragons here.
*/

static inline double
calc_output_single (SINC_FILTER *filter, increment_t increment, increment_t start_filter_index)
{	double		fraction, left, right, icoeff ;
	increment_t	filter_index, max_filter_index ;
	int			data_index, coeff_count, indx ;

	/* Convert input parameters into fixed point. */
	max_filter_index = int_to_fp (filter->coeff_half_len) ;

	/* First apply the left half of the filter. */
	filter_index = start_filter_index ;
	coeff_count = (max_filter_index - filter_index) / increment ;
	filter_index = filter_index + coeff_count * increment ;
	data_index = filter->b_current - coeff_count ;

	if (data_index < 0) /* Avoid underflow access to filter->buffer. */
	{	int steps = -data_index ;
		/* If the assert triggers we would have to take care not to underflow/overflow */
		assert (steps <= int_div_ceil (filter_index, increment)) ;
		filter_index -= increment * steps ;
		data_index += steps ;
	}
	left = 0.0 ;
	while (filter_index >= MAKE_INCREMENT_T (0))
	{	fraction = fp_to_double (filter_index) ;
		indx = fp_to_int (filter_index) ;
		assert (indx >= 0 && indx + 1 < filter->coeff_half_len + 2) ;
		icoeff = filter->coeffs [indx] + fraction * (filter->coeffs [indx + 1] - filter->coeffs [indx]) ;
		assert (data_index >= 0 && data_index < filter->b_len) ;
		assert (data_index < filter->b_end) ;
		left += icoeff * filter->buffer [data_index] ;

		filter_index -= increment ;
		data_index = data_index + 1 ;
		} ;

	/* Now apply the right half of the filter. */
	filter_index = increment - start_filter_index ;
	coeff_count = (max_filter_index - filter_index) / increment ;
	filter_index = filter_index + coeff_count * increment ;
	data_index = filter->b_current + 1 + coeff_count ;

	right = 0.0 ;
	do
	{	fraction = fp_to_double (filter_index) ;
		indx = fp_to_int (filter_index) ;
		assert (indx < filter->coeff_half_len + 2) ;
		icoeff = filter->coeffs [indx] + fraction * (filter->coeffs [indx + 1] - filter->coeffs [indx]) ;
		assert (data_index >= 0 && data_index < filter->b_len) ;
		assert (data_index < filter->b_end) ;
		right += icoeff * filter->buffer [data_index] ;

		filter_index -= increment ;
		data_index = data_index - 1 ;
		}
	while (filter_index > MAKE_INCREMENT_T (0)) ;

	return (left + right) ;
} /* calc_output_single */

static SRC_ERROR
sinc_mono_vari_process (SRC_STATE *state, SRC_DATA *data)
{	SINC_FILTER *filter ;
	double		input_index, src_ratio, count, float_increment, terminate, rem ;
	increment_t	increment, start_filter_index ;
	int			half_filter_chan_len, samples_in_hand ;

	if (state->private_data == NULL)
		return SRC_ERR_NO_PRIVATE ;

	filter = (SINC_FILTER*) state->private_data ;

	/* If there is not a problem, this will be optimised out. */
	if (sizeof (filter->buffer [0]) != sizeof (data->data_in [0]))
		return SRC_ERR_SIZE_INCOMPATIBILITY ;

	filter->in_count = data->input_frames * state->channels ;
	filter->out_count = data->output_frames * state->channels ;
	filter->in_used = filter->out_gen = 0 ;

	src_ratio = state->last_ratio ;

	if (is_bad_src_ratio (src_ratio))
		return SRC_ERR_BAD_INTERNAL_STATE ;

	/* Check the sample rate ratio wrt the buffer len. */
	count = (filter->coeff_half_len + 2.0) / filter->index_inc ;
	if (MIN (state->last_ratio, data->src_ratio) < 1.0)
		count /= MIN (state->last_ratio, data->src_ratio) ;

	/* Maximum coefficientson either side of center point. */
	half_filter_chan_len = state->channels * (int) (psf_lrint (count) + 1) ;

	input_index = state->last_position ;

	rem = fmod_one (input_index) ;
	filter->b_current = (filter->b_current + state->channels * psf_lrint (input_index - rem)) % filter->b_len ;
	input_index = rem ;

	terminate = 1.0 / src_ratio + 1e-20 ;

	/* Main processing loop. */
	while (filter->out_gen < filter->out_count)
	{
		/* Need to reload buffer? */
		samples_in_hand = (filter->b_end - filter->b_current + filter->b_len) % filter->b_len ;

		if (samples_in_hand <= half_filter_chan_len)
		{	if ((state->error = prepare_data (filter, state->channels, data, half_filter_chan_len)) != 0)
				return state->error ;

			samples_in_hand = (filter->b_end - filter->b_current + filter->b_len) % filter->b_len ;
			if (samples_in_hand <= half_filter_chan_len)
				break ;
			} ;

		/* This is the termination condition. */
		if (filter->b_real_end >= 0)
		{	if (filter->b_current + input_index + terminate > filter->b_real_end)
				break ;
			} ;

		if (filter->out_count > 0 && fabs (state->last_ratio - data->src_ratio) > 1e-10)
			src_ratio = state->last_ratio + filter->out_gen * (data->src_ratio - state->last_ratio) / filter->out_count ;

		float_increment = filter->index_inc * (src_ratio < 1.0 ? src_ratio : 1.0) ;
		increment = double_to_fp (float_increment) ;

		start_filter_index = double_to_fp (input_index * float_increment) ;

		data->data_out [filter->out_gen] = (float) ((float_increment / filter->index_inc) *
										calc_output_single (filter, increment, start_filter_index)) ;
		filter->out_gen ++ ;

		/* Figure out the next index. */
		input_index += 1.0 / src_ratio ;
		rem = fmod_one (input_index) ;

		filter->b_current = (filter->b_current + state->channels * psf_lrint (input_index - rem)) % filter->b_len ;
		input_index = rem ;
		} ;

	state->last_position = input_index ;

	/* Save current ratio rather then target ratio. */
	state->last_ratio = src_ratio ;

	data->input_frames_used = filter->in_used / state->channels ;
	data->output_frames_gen = filter->out_gen / state->channels ;

	return SRC_ERR_NO_ERROR ;
} /* sinc_mono_vari_process */

static inline void
calc_output_stereo (SINC_FILTER *filter, int channels, increment_t increment, increment_t start_filter_index, double scale, float * output)
{	double		fraction, left [2], right [2], icoeff ;
	increment_t	filter_index, max_filter_index ;
	int			data_index, coeff_count, indx ;

	/* Convert input parameters into fixed point. */
	max_filter_index = int_to_fp (filter->coeff_half_len) ;

	/* First apply the left half of the filter. */
	filter_index = start_filter_index ;
	coeff_count = (max_filter_index - filter_index) / increment ;
	filter_index = filter_index + coeff_count * increment ;
	data_index = filter->b_current - channels * coeff_count ;

	if (data_index < 0) /* Avoid underflow access to filter->buffer. */
	{	int steps = int_div_ceil (-data_index, 2) ;
		/* If the assert triggers we would have to take care not to underflow/overflow */
		assert (steps <= int_div_ceil (filter_index, increment)) ;
		filter_index -= increment * steps ;
		data_index += steps * 2;
	}
	left [0] = left [1] = 0.0 ;
	while (filter_index >= MAKE_INCREMENT_T (0))
	{	fraction = fp_to_double (filter_index) ;
		indx = fp_to_int (filter_index) ;
		assert (indx >= 0 && indx + 1 < filter->coeff_half_len + 2) ;
		icoeff = filter->coeffs [indx] + fraction * (filter->coeffs [indx + 1] - filter->coeffs [indx]) ;
		assert (data_index >= 0 && data_index + 1 < filter->b_len) ;
		assert (data_index + 1 < filter->b_end) ;
		for (int ch = 0; ch < 2; ch++)
			left [ch] += icoeff * filter->buffer [data_index + ch] ;

		filter_index -= increment ;
		data_index = data_index + 2 ;
		} ;

	/* Now apply the right half of the filter. */
	filter_index = increment - start_filter_index ;
	coeff_count = (max_filter_index - filter_index) / increment ;
	filter_index = filter_index + coeff_count * increment ;
	data_index = filter->b_current + channels * (1 + coeff_count) ;

	right [0] = right [1] = 0.0 ;
	do
	{	fraction = fp_to_double (filter_index) ;
		indx = fp_to_int (filter_index) ;
		assert (indx >= 0 && indx + 1 < filter->coeff_half_len + 2) ;
		icoeff = filter->coeffs [indx] + fraction * (filter->coeffs [indx + 1] - filter->coeffs [indx]) ;
		assert (data_index >= 0 && data_index + 1 < filter->b_len) ;
		assert (data_index + 1 < filter->b_end) ;
		for (int ch = 0; ch < 2; ch++)
			right [ch] += icoeff * filter->buffer [data_index + ch] ;

		filter_index -= increment ;
		data_index = data_index - 2 ;
		}
	while (filter_index > MAKE_INCREMENT_T (0)) ;

	for (int ch = 0; ch < 2; ch++)
		output [ch] = (float) (scale * (left [ch] + right [ch])) ;
} /* calc_output_stereo */

SRC_ERROR
sinc_stereo_vari_process (SRC_STATE *state, SRC_DATA *data)
{	SINC_FILTER *filter ;
	double		input_index, src_ratio, count, float_increment, terminate, rem ;
	increment_t	increment, start_filter_index ;
	int			half_filter_chan_len, samples_in_hand ;

	if (state->private_data == NULL)
		return SRC_ERR_NO_PRIVATE ;

	filter = (SINC_FILTER*) state->private_data ;

	/* If there is not a problem, this will be optimised out. */
	if (sizeof (filter->buffer [0]) != sizeof (data->data_in [0]))
		return SRC_ERR_SIZE_INCOMPATIBILITY ;

	filter->in_count = data->input_frames * state->channels ;
	filter->out_count = data->output_frames * state->channels ;
	filter->in_used = filter->out_gen = 0 ;

	src_ratio = state->last_ratio ;

	if (is_bad_src_ratio (src_ratio))
		return SRC_ERR_BAD_INTERNAL_STATE ;

	/* Check the sample rate ratio wrt the buffer len. */
	count = (filter->coeff_half_len + 2.0) / filter->index_inc ;
	if (MIN (state->last_ratio, data->src_ratio) < 1.0)
		count /= MIN (state->last_ratio, data->src_ratio) ;

	/* Maximum coefficientson either side of center point. */
	half_filter_chan_len = state->channels * (int) (psf_lrint (count) + 1) ;

	input_index = state->last_position ;

	rem = fmod_one (input_index) ;
	filter->b_current = (filter->b_current + state->channels * psf_lrint (input_index - rem)) % filter->b_len ;
	input_index = rem ;

	terminate = 1.0 / src_ratio + 1e-20 ;

	/* Main processing loop. */
	while (filter->out_gen < filter->out_count)
	{
		/* Need to reload buffer? */
		samples_in_hand = (filter->b_end - filter->b_current + filter->b_len) % filter->b_len ;

		if (samples_in_hand <= half_filter_chan_len)
		{	if ((state->error = prepare_data (filter, state->channels, data, half_filter_chan_len)) != 0)
				return state->error ;

			samples_in_hand = (filter->b_end - filter->b_current + filter->b_len) % filter->b_len ;
			if (samples_in_hand <= half_filter_chan_len)
				break ;
			} ;

		/* This is the termination condition. */
		if (filter->b_real_end >= 0)
		{	if (filter->b_current + input_index + terminate >= filter->b_real_end)
				break ;
			} ;

		if (filter->out_count > 0 && fabs (state->last_ratio - data->src_ratio) > 1e-10)
			src_ratio = state->last_ratio + filter->out_gen * (data->src_ratio - state->last_ratio) / filter->out_count ;

		float_increment = filter->index_inc * (src_ratio < 1.0 ? src_ratio : 1.0) ;
		increment = double_to_fp (float_increment) ;

		start_filter_index = double_to_fp (input_index * float_increment) ;

		calc_output_stereo (filter, state->channels, increment, start_filter_index, float_increment / filter->index_inc, data->data_out + filter->out_gen) ;
		filter->out_gen += 2 ;

		/* Figure out the next index. */
		input_index += 1.0 / src_ratio ;
		rem = fmod_one (input_index) ;

		filter->b_current = (filter->b_current + state->channels * psf_lrint (input_index - rem)) % filter->b_len ;
		input_index = rem ;
		} ;

	state->last_position = input_index ;

	/* Save current ratio rather then target ratio. */
	state->last_ratio = src_ratio ;

	data->input_frames_used = filter->in_used / state->channels ;
	data->output_frames_gen = filter->out_gen / state->channels ;

	return SRC_ERR_NO_ERROR ;
} /* sinc_stereo_vari_process */

static inline void
calc_output_quad (SINC_FILTER *filter, int channels, increment_t increment, increment_t start_filter_index, double scale, float * output)
{	double		fraction, left [4], right [4], icoeff ;
	increment_t	filter_index, max_filter_index ;
	int			data_index, coeff_count, indx ;

	/* Convert input parameters into fixed point. */
	max_filter_index = int_to_fp (filter->coeff_half_len) ;

	/* First apply the left half of the filter. */
	filter_index = start_filter_index ;
	coeff_count = (max_filter_index - filter_index) / increment ;
	filter_index = filter_index + coeff_count * increment ;
	data_index = filter->b_current - channels * coeff_count ;

	if (data_index < 0) /* Avoid underflow access to filter->buffer. */
	{	int steps = int_div_ceil (-data_index, 4) ;
		/* If the assert triggers we would have to take care not to underflow/overflow */
		assert (steps <= int_div_ceil (filter_index, increment)) ;
		filter_index -= increment * steps ;
		data_index += steps * 4;
	}
	left [0] = left [1] = left [2] = left [3] = 0.0 ;
	while (filter_index >= MAKE_INCREMENT_T (0))
	{	fraction = fp_to_double (filter_index) ;
		indx = fp_to_int (filter_index) ;
		assert (indx >= 0 && indx + 1 < filter->coeff_half_len + 2) ;
		icoeff = filter->coeffs [indx] + fraction * (filter->coeffs [indx + 1] - filter->coeffs [indx]) ;
		assert (data_index >= 0 && data_index + 3 < filter->b_len) ;
		assert (data_index + 3 < filter->b_end) ;
		for (int ch = 0; ch < 4; ch++)
			left [ch] += icoeff * filter->buffer [data_index + ch] ;

		filter_index -= increment ;
		data_index = data_index + 4 ;
		} ;

	/* Now apply the right half of the filter. */
	filter_index = increment - start_filter_index ;
	coeff_count = (max_filter_index - filter_index) / increment ;
	filter_index = filter_index + coeff_count * increment ;
	data_index = filter->b_current + channels * (1 + coeff_count) ;

	right [0] = right [1] = right [2] = right [3] = 0.0 ;
	do
	{	fraction = fp_to_double (filter_index) ;
		indx = fp_to_int (filter_index) ;
		assert (indx >= 0 && indx + 1 < filter->coeff_half_len + 2) ;
		icoeff = filter->coeffs [indx] + fraction * (filter->coeffs [indx + 1] - filter->coeffs [indx]) ;
		assert (data_index >= 0 && data_index + 3 < filter->b_len) ;
		assert (data_index + 3 < filter->b_end) ;
		for (int ch = 0; ch < 4; ch++)
			right [ch] += icoeff * filter->buffer [data_index + ch] ;


		filter_index -= increment ;
		data_index = data_index - 4 ;
		}
	while (filter_index > MAKE_INCREMENT_T (0)) ;

	for (int ch = 0; ch < 4; ch++)
		output [ch] = (float) (scale * (left [ch] + right [ch])) ;
} /* calc_output_quad */

SRC_ERROR
sinc_quad_vari_process (SRC_STATE *state, SRC_DATA *data)
{	SINC_FILTER *filter ;
	double		input_index, src_ratio, count, float_increment, terminate, rem ;
	increment_t	increment, start_filter_index ;
	int			half_filter_chan_len, samples_in_hand ;

	if (state->private_data == NULL)
		return SRC_ERR_NO_PRIVATE ;

	filter = (SINC_FILTER*) state->private_data ;

	/* If there is not a problem, this will be optimised out. */
	if (sizeof (filter->buffer [0]) != sizeof (data->data_in [0]))
		return SRC_ERR_SIZE_INCOMPATIBILITY ;

	filter->in_count = data->input_frames * state->channels ;
	filter->out_count = data->output_frames * state->channels ;
	filter->in_used = filter->out_gen = 0 ;

	src_ratio = state->last_ratio ;

	if (is_bad_src_ratio (src_ratio))
		return SRC_ERR_BAD_INTERNAL_STATE ;

	/* Check the sample rate ratio wrt the buffer len. */
	count = (filter->coeff_half_len + 2.0) / filter->index_inc ;
	if (MIN (state->last_ratio, data->src_ratio) < 1.0)
		count /= MIN (state->last_ratio, data->src_ratio) ;

	/* Maximum coefficientson either side of center point. */
	half_filter_chan_len = state->channels * (int) (psf_lrint (count) + 1) ;

	input_index = state->last_position ;

	rem = fmod_one (input_index) ;
	filter->b_current = (filter->b_current + state->channels * psf_lrint (input_index - rem)) % filter->b_len ;
	input_index = rem ;

	terminate = 1.0 / src_ratio + 1e-20 ;

	/* Main processing loop. */
	while (filter->out_gen < filter->out_count)
	{
		/* Need to reload buffer? */
		samples_in_hand = (filter->b_end - filter->b_current + filter->b_len) % filter->b_len ;

		if (samples_in_hand <= half_filter_chan_len)
		{	if ((state->error = prepare_data (filter, state->channels, data, half_filter_chan_len)) != 0)
				return state->error ;

			samples_in_hand = (filter->b_end - filter->b_current + filter->b_len) % filter->b_len ;
			if (samples_in_hand <= half_filter_chan_len)
				break ;
			} ;

		/* This is the termination condition. */
		if (filter->b_real_end >= 0)
		{	if (filter->b_current + input_index + terminate >= filter->b_real_end)
				break ;
			} ;

		if (filter->out_count > 0 && fabs (state->last_ratio - data->src_ratio) > 1e-10)
			src_ratio = state->last_ratio + filter->out_gen * (data->src_ratio - state->last_ratio) / filter->out_count ;

		float_increment = filter->index_inc * (src_ratio < 1.0 ? src_ratio : 1.0) ;
		increment = double_to_fp (float_increment) ;

		start_filter_index = double_to_fp (input_index * float_increment) ;

		calc_output_quad (filter, state->channels, increment, start_filter_index, float_increment / filter->index_inc, data->data_out + filter->out_gen) ;
		filter->out_gen += 4 ;

		/* Figure out the next index. */
		input_index += 1.0 / src_ratio ;
		rem = fmod_one (input_index) ;

		filter->b_current = (filter->b_current + state->channels * psf_lrint (input_index - rem)) % filter->b_len ;
		input_index = rem ;
		} ;

	state->last_position = input_index ;

	/* Save current ratio rather then target ratio. */
	state->last_ratio = src_ratio ;

	data->input_frames_used = filter->in_used / state->channels ;
	data->output_frames_gen = filter->out_gen / state->channels ;

	return SRC_ERR_NO_ERROR ;
} /* sinc_quad_vari_process */

static inline void
calc_output_hex (SINC_FILTER *filter, int channels, increment_t increment, increment_t start_filter_index, double scale, float * output)
{	double		fraction, left [6], right [6], icoeff ;
	increment_t	filter_index, max_filter_index ;
	int			data_index, coeff_count, indx ;

	/* Convert input parameters into fixed point. */
	max_filter_index = int_to_fp (filter->coeff_half_len) ;

	/* First apply the left half of the filter. */
	filter_index = start_filter_index ;
	coeff_count = (max_filter_index - filter_index) / increment ;
	filter_index = filter_index + coeff_count * increment ;
	data_index = filter->b_current - channels * coeff_count ;

	if (data_index < 0) /* Avoid underflow access to filter->buffer. */
	{	int steps = int_div_ceil (-data_index, 6) ;
		/* If the assert triggers we would have to take care not to underflow/overflow */
		assert (steps <= int_div_ceil (filter_index, increment)) ;
		filter_index -= increment * steps ;
		data_index += steps * 6;
	}
	left [0] = left [1] = left [2] = left [3] = left [4] = left [5] = 0.0 ;
	while (filter_index >= MAKE_INCREMENT_T (0))
	{	fraction = fp_to_double (filter_index) ;
		indx = fp_to_int (filter_index) ;
		assert (indx >= 0 && indx + 1 < filter->coeff_half_len + 2) ;
		icoeff = filter->coeffs [indx] + fraction * (filter->coeffs [indx + 1] - filter->coeffs [indx]) ;
		assert (data_index >= 0 && data_index + 5 < filter->b_len) ;
		assert (data_index + 5 < filter->b_end) ;
		for (int ch = 0; ch < 6; ch++)
			left [ch] += icoeff * filter->buffer [data_index + ch] ;

		filter_index -= increment ;
		data_index = data_index + 6 ;
		} ;

	/* Now apply the right half of the filter. */
	filter_index = increment - start_filter_index ;
	coeff_count = (max_filter_index - filter_index) / increment ;
	filter_index = filter_index + coeff_count * increment ;
	data_index = filter->b_current + channels * (1 + coeff_count) ;

	right [0] = right [1] = right [2] = right [3] = right [4] = right [5] = 0.0 ;
	do
	{	fraction = fp_to_double (filter_index) ;
		indx = fp_to_int (filter_index) ;
		assert (indx >= 0 && indx + 1 < filter->coeff_half_len + 2) ;
		icoeff = filter->coeffs [indx] + fraction * (filter->coeffs [indx + 1] - filter->coeffs [indx]) ;
		assert (data_index >= 0 && data_index + 5 < filter->b_len) ;
		assert (data_index + 5 < filter->b_end) ;
		for (int ch = 0; ch < 6; ch++)
			right [ch] += icoeff * filter->buffer [data_index + ch] ;

		filter_index -= increment ;
		data_index = data_index - 6 ;
		}
	while (filter_index > MAKE_INCREMENT_T (0)) ;

	for (int ch = 0; ch < 6; ch++)
		output [ch] = (float) (scale * (left [ch] + right [ch])) ;
} /* calc_output_hex */

SRC_ERROR
sinc_hex_vari_process (SRC_STATE *state, SRC_DATA *data)
{	SINC_FILTER *filter ;
	double		input_index, src_ratio, count, float_increment, terminate, rem ;
	increment_t	increment, start_filter_index ;
	int			half_filter_chan_len, samples_in_hand ;

	if (state->private_data == NULL)
		return SRC_ERR_NO_PRIVATE ;

	filter = (SINC_FILTER*) state->private_data ;

	/* If there is not a problem, this will be optimised out. */
	if (sizeof (filter->buffer [0]) != sizeof (data->data_in [0]))
		return SRC_ERR_SIZE_INCOMPATIBILITY ;

	filter->in_count = data->input_frames * state->channels ;
	filter->out_count = data->output_frames * state->channels ;
	filter->in_used = filter->out_gen = 0 ;

	src_ratio = state->last_ratio ;

	if (is_bad_src_ratio (src_ratio))
		return SRC_ERR_BAD_INTERNAL_STATE ;

	/* Check the sample rate ratio wrt the buffer len. */
	count = (filter->coeff_half_len + 2.0) / filter->index_inc ;
	if (MIN (state->last_ratio, data->src_ratio) < 1.0)
		count /= MIN (state->last_ratio, data->src_ratio) ;

	/* Maximum coefficientson either side of center point. */
	half_filter_chan_len = state->channels * (int) (psf_lrint (count) + 1) ;

	input_index = state->last_position ;

	rem = fmod_one (input_index) ;
	filter->b_current = (filter->b_current + state->channels * psf_lrint (input_index - rem)) % filter->b_len ;
	input_index = rem ;

	terminate = 1.0 / src_ratio + 1e-20 ;

	/* Main processing loop. */
	while (filter->out_gen < filter->out_count)
	{
		/* Need to reload buffer? */
		samples_in_hand = (filter->b_end - filter->b_current + filter->b_len) % filter->b_len ;

		if (samples_in_hand <= half_filter_chan_len)
		{	if ((state->error = prepare_data (filter, state->channels, data, half_filter_chan_len)) != 0)
				return state->error ;

			samples_in_hand = (filter->b_end - filter->b_current + filter->b_len) % filter->b_len ;
			if (samples_in_hand <= half_filter_chan_len)
				break ;
			} ;

		/* This is the termination condition. */
		if (filter->b_real_end >= 0)
		{	if (filter->b_current + input_index + terminate >= filter->b_real_end)
				break ;
			} ;

		if (filter->out_count > 0 && fabs (state->last_ratio - data->src_ratio) > 1e-10)
			src_ratio = state->last_ratio + filter->out_gen * (data->src_ratio - state->last_ratio) / filter->out_count ;

		float_increment = filter->index_inc * (src_ratio < 1.0 ? src_ratio : 1.0) ;
		increment = double_to_fp (float_increment) ;

		start_filter_index = double_to_fp (input_index * float_increment) ;

		calc_output_hex (filter, state->channels, increment, start_filter_index, float_increment / filter->index_inc, data->data_out + filter->out_gen) ;
		filter->out_gen += 6 ;

		/* Figure out the next index. */
		input_index += 1.0 / src_ratio ;
		rem = fmod_one (input_index) ;

		filter->b_current = (filter->b_current + state->channels * psf_lrint (input_index - rem)) % filter->b_len ;
		input_index = rem ;
		} ;

	state->last_position = input_index ;

	/* Save current ratio rather then target ratio. */
	state->last_ratio = src_ratio ;

	data->input_frames_used = filter->in_used / state->channels ;
	data->output_frames_gen = filter->out_gen / state->channels ;

	return SRC_ERR_NO_ERROR ;
} /* sinc_hex_vari_process */

static inline void
calc_output_multi (SINC_FILTER *filter, increment_t increment, increment_t start_filter_index, int channels, double scale, float * output)
{	double		fraction, icoeff ;
	/* The following line is 1999 ISO Standard C. If your compiler complains, get a better compiler. */
	double		*left, *right ;
	increment_t	filter_index, max_filter_index ;
	int			data_index, coeff_count, indx ;

	left = filter->left_calc ;
	right = filter->right_calc ;

	/* Convert input parameters into fixed point. */
	max_filter_index = int_to_fp (filter->coeff_half_len) ;

	/* First apply the left half of the filter. */
	filter_index = start_filter_index ;
	coeff_count = (max_filter_index - filter_index) / increment ;
	filter_index = filter_index + coeff_count * increment ;
	data_index = filter->b_current - channels * coeff_count ;

	if (data_index < 0) /* Avoid underflow access to filter->buffer. */
	{	int steps = int_div_ceil (-data_index, channels) ;
		/* If the assert triggers we would have to take care not to underflow/overflow */
		assert (steps <= int_div_ceil (filter_index, increment)) ;
		filter_index -= increment * steps ;
		data_index += steps * channels ;
	}

	memset (left, 0, sizeof (left [0]) * channels) ;

	while (filter_index >= MAKE_INCREMENT_T (0))
	{	fraction = fp_to_double (filter_index) ;
		indx = fp_to_int (filter_index) ;
		assert (indx >= 0 && indx + 1 < filter->coeff_half_len + 2) ;
		icoeff = filter->coeffs [indx] + fraction * (filter->coeffs [indx + 1] - filter->coeffs [indx]) ;

		assert (data_index >= 0 && data_index + channels - 1 < filter->b_len) ;
		assert (data_index + channels - 1 < filter->b_end) ;
		for (int ch = 0; ch < channels; ch++)
			left [ch] += icoeff * filter->buffer [data_index + ch] ;

		filter_index -= increment ;
		data_index = data_index + channels ;
		} ;

	/* Now apply the right half of the filter. */
	filter_index = increment - start_filter_index ;
	coeff_count = (max_filter_index - filter_index) / increment ;
	filter_index = filter_index + coeff_count * increment ;
	data_index = filter->b_current + channels * (1 + coeff_count) ;

	memset (right, 0, sizeof (right [0]) * channels) ;
	do
	{	fraction = fp_to_double (filter_index) ;
		indx = fp_to_int (filter_index) ;
		assert (indx >= 0 && indx + 1 < filter->coeff_half_len + 2) ;
		icoeff = filter->coeffs [indx] + fraction * (filter->coeffs [indx + 1] - filter->coeffs [indx]) ;
		assert (data_index >= 0 && data_index + channels - 1 < filter->b_len) ;
		assert (data_index + channels - 1 < filter->b_end) ;
		for (int ch = 0; ch < channels; ch++)
			right [ch] += icoeff * filter->buffer [data_index + ch] ;

		filter_index -= increment ;
		data_index = data_index - channels ;
		}
	while (filter_index > MAKE_INCREMENT_T (0)) ;

	for(int ch = 0; ch < channels; ch++)
		output [ch] = (float) (scale * (left [ch] + right [ch])) ;

	return ;
} /* calc_output_multi */

static SRC_ERROR
sinc_multichan_vari_process (SRC_STATE *state, SRC_DATA *data)
{	SINC_FILTER *filter ;
	double		input_index, src_ratio, count, float_increment, terminate, rem ;
	increment_t	increment, start_filter_index ;
	int			half_filter_chan_len, samples_in_hand ;

	if (state->private_data == NULL)
		return SRC_ERR_NO_PRIVATE ;

	filter = (SINC_FILTER*) state->private_data ;

	/* If there is not a problem, this will be optimised out. */
	if (sizeof (filter->buffer [0]) != sizeof (data->data_in [0]))
		return SRC_ERR_SIZE_INCOMPATIBILITY ;

	filter->in_count = data->input_frames * state->channels ;
	filter->out_count = data->output_frames * state->channels ;
	filter->in_used = filter->out_gen = 0 ;

	src_ratio = state->last_ratio ;

	if (is_bad_src_ratio (src_ratio))
		return SRC_ERR_BAD_INTERNAL_STATE ;

	/* Check the sample rate ratio wrt the buffer len. */
	count = (filter->coeff_half_len + 2.0) / filter->index_inc ;
	if (MIN (state->last_ratio, data->src_ratio) < 1.0)
		count /= MIN (state->last_ratio, data->src_ratio) ;

	/* Maximum coefficientson either side of center point. */
	half_filter_chan_len = state->channels * (int) (psf_lrint (count) + 1) ;

	input_index = state->last_position ;

	rem = fmod_one (input_index) ;
	filter->b_current = (filter->b_current + state->channels * psf_lrint (input_index - rem)) % filter->b_len ;
	input_index = rem ;

	terminate = 1.0 / src_ratio + 1e-20 ;

	/* Main processing loop. */
	while (filter->out_gen < filter->out_count)
	{
		/* Need to reload buffer? */
		samples_in_hand = (filter->b_end - filter->b_current + filter->b_len) % filter->b_len ;

		if (samples_in_hand <= half_filter_chan_len)
		{	if ((state->error = prepare_data (filter, state->channels, data, half_filter_chan_len)) != 0)
				return state->error ;

			samples_in_hand = (filter->b_end - filter->b_current + filter->b_len) % filter->b_len ;
			if (samples_in_hand <= half_filter_chan_len)
				break ;
			} ;

		/* This is the termination condition. */
		if (filter->b_real_end >= 0)
		{	if (filter->b_current + input_index + terminate >= filter->b_real_end)
				break ;
			} ;

		if (filter->out_count > 0 && fabs (state->last_ratio - data->src_ratio) > 1e-10)
			src_ratio = state->last_ratio + filter->out_gen * (data->src_ratio - state->last_ratio) / filter->out_count ;

		float_increment = filter->index_inc * (src_ratio < 1.0 ? src_ratio : 1.0) ;
		increment = double_to_fp (float_increment) ;

		start_filter_index = double_to_fp (input_index * float_increment) ;

		calc_output_multi (filter, increment, start_filter_index, state->channels, float_increment / filter->index_inc, data->data_out + filter->out_gen) ;
		filter->out_gen += state->channels ;

		/* Figure out the next index. */
		input_index += 1.0 / src_ratio ;
		rem = fmod_one (input_index) ;

		filter->b_current = (filter->b_current + state->channels * psf_lrint (input_index - rem)) % filter->b_len ;
		input_index = rem ;
		} ;

	state->last_position = input_index ;

	/* Save current ratio rather then target ratio. */
	state->last_ratio = src_ratio ;

	data->input_frames_used = filter->in_used / state->channels ;
	data->output_frames_gen = filter->out_gen / state->channels ;

	return SRC_ERR_NO_ERROR ;
} /* sinc_multichan_vari_process */

/*----------------------------------------------------------------------------------------
*/

static SRC_ERROR
prepare_data (SINC_FILTER *filter, int channels, SRC_DATA *data, int half_filter_chan_len)
{	int len = 0 ;

	if (filter->b_real_end >= 0)
		return SRC_ERR_NO_ERROR ;	/* Should be terminating. Just return. */

	if (data->data_in == NULL)
		return SRC_ERR_NO_ERROR ;

	if (filter->b_current == 0)
	{	/* Initial state. Set up zeros at the start of the buffer and
		** then load new data after that.
		*/
		len = filter->b_len - 2 * half_filter_chan_len ;

		filter->b_current = filter->b_end = half_filter_chan_len ;
		}
	else if (filter->b_end + half_filter_chan_len + channels < filter->b_len)
	{	/*  Load data at current end position. */
		len = MAX (filter->b_len - filter->b_current - half_filter_chan_len, 0) ;
		}
	else
	{	/* Move data at end of buffer back to the start of the buffer. */
		len = filter->b_end - filter->b_current ;
		memmove (filter->buffer, filter->buffer + filter->b_current - half_filter_chan_len,
						(half_filter_chan_len + len) * sizeof (filter->buffer [0])) ;

		filter->b_current = half_filter_chan_len ;
		filter->b_end = filter->b_current + len ;

		/* Now load data at current end of buffer. */
		len = MAX (filter->b_len - filter->b_current - half_filter_chan_len, 0) ;
		} ;

	len = MIN ((int) (filter->in_count - filter->in_used), len) ;
	len -= (len % channels) ;

	if (len < 0 || filter->b_end + len > filter->b_len)
		return SRC_ERR_SINC_PREPARE_DATA_BAD_LEN ;

	memcpy (filter->buffer + filter->b_end, data->data_in + filter->in_used,
						len * sizeof (filter->buffer [0])) ;

	filter->b_end += len ;
	filter->in_used += len ;

	if (filter->in_used == filter->in_count &&
			filter->b_end - filter->b_current < 2 * half_filter_chan_len && data->end_of_input)
	{	/* Handle the case where all data in the current buffer has been
		** consumed and this is the last buffer.
		*/

		if (filter->b_len - filter->b_end < half_filter_chan_len + 5)
		{	/* If necessary, move data down to the start of the buffer. */
			len = filter->b_end - filter->b_current ;
			memmove (filter->buffer, filter->buffer + filter->b_current - half_filter_chan_len,
							(half_filter_chan_len + len) * sizeof (filter->buffer [0])) ;

			filter->b_current = half_filter_chan_len ;
			filter->b_end = filter->b_current + len ;
			} ;

		filter->b_real_end = filter->b_end ;
		len = half_filter_chan_len + 5 ;

		if (len < 0 || filter->b_end + len > filter->b_len)
			len = filter->b_len - filter->b_end ;

		memset (filter->buffer + filter->b_end, 0, len * sizeof (filter->buffer [0])) ;
		filter->b_end += len ;
		} ;

	return SRC_ERR_NO_ERROR ;
} /* prepare_data */

static void
sinc_close (SRC_STATE *state)
{
	if (state)
	{
		SINC_FILTER *sinc = (SINC_FILTER *) state->private_data ;
		if (sinc)
		{
			if (sinc->buffer)
			{
				free (sinc->buffer) ;
				sinc->buffer = NULL ;
			}
			free (sinc) ;
			sinc = NULL ;
		}
		free (state) ;
		state = NULL ;
	}
} /* sinc_close */
