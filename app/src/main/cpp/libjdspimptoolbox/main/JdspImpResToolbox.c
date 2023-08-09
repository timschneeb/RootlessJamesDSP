#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

#include "interpolation.h"

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

void complexMultiplication(double xReal, double xImag, double yReal, double yImag, double *zReal, double *zImag)
{
    *zReal = xReal * yReal - xImag * yImag;
    *zImag = xReal * yImag + xImag * yReal;
}
void cdivid(const double ar, const double ai, const double br, const double bi, double *cr, double *ci)
{
    double r, d;
    if (br == 0.0 && bi == 0.0)
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

void HSHOResponse(double fs, double fc, unsigned int filterOrder, double gain, double overallGainDb, unsigned int queryPts, double *dispFreq, double *cplxRe, double *cplxIm)
{
    unsigned int L = filterOrder >> 1;
    double *sRe = (double *)malloc(queryPts * sizeof(double));
    double *sIm = (double *)malloc(queryPts * sizeof(double));
    double *s2Re = (double *)malloc(queryPts * sizeof(double));
    double *s2Im = (double *)malloc(queryPts * sizeof(double));
    for (unsigned int j = 0; j < queryPts; j++)
    {
        double digw = dispFreq[j] / fs * 2.0 * M_PI;
        sRe[j] = cos(digw);
        sIm[j] = sin(digw);
        s2Re[j] = sRe[j] * sRe[j] - sIm[j] * sIm[j];
        s2Im[j] = sRe[j] * sIm[j] + sIm[j] * sRe[j];
    }
    double Dw = M_PI * (0.5 - fc / fs);
    double GB = (1.0 / sqrt(2.0)) * gain;
    double G = pow(10.0, gain / 20.0);
    double overallGain = pow(10.0, overallGainDb / 20.0);
    if (G == 1.0)
        goto scalar_gain;
    GB = pow(10.0, GB / 20.0);
    double gR;
    if (fabs(GB * GB - 1) < DBL_EPSILON)
        gR = 0.0;
    else
        gR = (G * G - GB * GB) / (GB * GB - 1);
    double reci1Ord = 1.0 / filterOrder;
    double ratOrd = pow(gR, reci1Ord);
    double sDw = sin(Dw);
    double sDw2 = sDw * sDw;
    double cDw = cos(Dw);
    double cDw2 = cDw * cDw;
    double ratRO = pow(gR, 1.0 / (2 * filterOrder));
    double g2reO = pow(G, 2.0 * reci1Ord);
    double gP1 = pow(G, 1.0 / filterOrder);
    double t2 = 2 * g2reO * sDw2;
    double t4 = cDw2 * ratOrd;
    double t5 = 2 * cDw2 * ratOrd;
    double t8 = cDw2 * ratOrd;
    double t10 = g2reO * sDw2;
    double t11 = 2 * sDw2;
    double t12 = 2 * t8;
    double t13 = 2 * gP1 * cDw * sDw * ratRO;
    double t14 = g2reO * sDw2 + cDw2 * ratOrd;
    double t15 = 2 * cDw * sDw * ratRO;
    double t16 = sDw2 + cDw2 * ratOrd;
    for (unsigned int i = 0; i < L; i++)
    {
        double si = sin((2 * (i + 1) - 1.0) * M_PI / (2.0 * filterOrder));
        double t6 = t13 * si;
        double t1 = t14 - t6;
        double t9 = t15 * si;
        double t7 = t16 - t9;
        for (unsigned int j = 0; j < queryPts; j++)
        {
            double k1Re = (t1 - t2 * sRe[j] + t5 * sRe[j] + t10 * s2Re[j] + t4 * s2Re[j] + t6 * s2Re[j]);
            double k1Im = (-t2 * sIm[j] + t5 * sIm[j] + t10 * s2Im[j] + t4 * s2Im[j] + t6 * s2Im[j]);
            double k2Re = (t7 + s2Re[j] * sDw2 - t11 * sRe[j] + s2Re[j] * t8 + sRe[j] * t12 + s2Re[j] * t9);
            double k2Im = (s2Im[j] * sDw2 - t11 * sIm[j] + s2Im[j] * t8 + sIm[j] * t12 + s2Im[j] * t9);
            double cplx2Re, cplx2Im;
            cdivid(k1Re, k1Im, k2Re, k2Im, &cplx2Re, &cplx2Im);
            complexMultiplication(cplxRe[j], cplxIm[j], cplx2Re, cplx2Im, &cplxRe[j], &cplxIm[j]);
        }
    }
    scalar_gain:
    free(sRe);
    free(sIm);
    free(s2Re);
    free(s2Im);
    for (unsigned int j = 0; j < queryPts; j++)
    {
        cplxRe[j] *= overallGain;
        cplxIm[j] *= overallGain;
    }
}

unsigned int upper_power_of_two(unsigned int v)
{
    v--;
    v |= v >> 1;
    v |= v >> 2;
    v |= v >> 4;
    v |= v >> 8;
    v |= v >> 16;
    return ++v;
}

void channel_splitFloat(float *buffer, unsigned int num_frames, float **chan_buffers, unsigned int num_channels)
{
	unsigned int i, samples = num_frames * num_channels;
	for (i = 0; i < samples; i++)
		chan_buffers[i % num_channels][i / num_channels] = buffer[i];
}
void channel_joinFloat(float **chan_buffers, unsigned int num_channels, float *buffer, unsigned int num_frames)
{
	unsigned int i, samples = num_frames * num_channels;
	for (i = 0; i < samples; i++)
		buffer[i] = chan_buffers[i % num_channels][i / num_channels];
}

unsigned int LLIntegerLog2(unsigned int v)
{
	unsigned int i = 0;
	while (v > 1)
	{
		++i;
		v >>= 1;
	}
	return i;
}
unsigned LLRevBits(unsigned int x, unsigned int bits)
{
	unsigned int y = 0;
	while (bits--)
	{
		y = (y + y) + (x & 1);
		x >>= 1;
	}
	return y;
}
void LLbitReversalTbl(unsigned *dst, unsigned int n)
{
	unsigned int bits = LLIntegerLog2(n);
	for (unsigned int i = 0; i < n; ++i)
		dst[i] = LLRevBits(i, bits);
}
void LLsinHalfTblFloat(float *dst, unsigned int n)
{
	const double twopi_over_n = 6.283185307179586476925286766559 / n;
	for (unsigned int i = 0; i < n; ++i)
		dst[i] = (float)sin(twopi_over_n * i);
}
typedef struct
{
	unsigned int xLen;
	unsigned int fftLen;
	unsigned int halfLen;
	unsigned int halfLenWdc;
	unsigned int *mBitRev;
	float *mSineTab;
	float threshold, logThreshold, normalizeGain;
} fftData;
void freeMpsFFTData(fftData *fd)
{
	free(fd->mBitRev);
	free(fd->mSineTab);
}
void initMpsFFTData(fftData *fd, unsigned int xLen, float threshdB)
{
	fd->xLen = xLen;
	fd->fftLen = upper_power_of_two(xLen);
	fd->halfLen = fd->fftLen >> 1;
	fd->halfLenWdc = fd->halfLen + 1;
	fd->mBitRev = (unsigned int*)malloc(fd->fftLen * sizeof(unsigned int));
	fd->mSineTab = (float*)malloc(fd->fftLen * sizeof(float));
	LLbitReversalTbl(fd->mBitRev, fd->fftLen);
	LLsinHalfTblFloat(fd->mSineTab, fd->fftLen);
	fd->threshold = powf(10.0f, threshdB / 20.0f);
	fd->logThreshold = logf(fd->threshold);
	fd->normalizeGain = 1.0f / fd->fftLen;
}
void mps(fftData *fd, float *x, float *y)
{
	unsigned int i;
	float *padded = (float*)malloc(fd->fftLen * sizeof(float));
	float *ceptrum = (float*)malloc(fd->fftLen * sizeof(float));
	for (i = 0; i < fd->xLen; i++)
		padded[fd->mBitRev[i]] = x[i];
	for (; i < fd->fftLen; i++)
		padded[fd->mBitRev[i]] = 0.0f;
	LLdiscreteHartleyFloat(padded, fd->fftLen, fd->mSineTab);
	unsigned int symIdx;
	float magnitude = fabsf(padded[0]);
	ceptrum[0] = magnitude < fd->threshold ? fd->logThreshold : logf(magnitude);
	for (i = 1; i < fd->halfLenWdc; i++)
	{
		symIdx = fd->fftLen - i;
		float lR = (padded[i] + padded[symIdx]) * 0.5f;
		float lI = (padded[i] - padded[symIdx]) * 0.5f;
		magnitude = hypotf(lR, lI);
		if (magnitude < fd->threshold)
			ceptrum[fd->mBitRev[i]] = ceptrum[fd->mBitRev[symIdx]] = fd->logThreshold;
		else
		{
			magnitude = logf(magnitude);
			ceptrum[fd->mBitRev[i]] = magnitude;
			ceptrum[fd->mBitRev[symIdx]] = magnitude;
		}
	}
	LLdiscreteHartleyFloat(ceptrum, fd->fftLen, fd->mSineTab);
	padded[0] = ceptrum[0] * fd->normalizeGain;
	padded[fd->mBitRev[fd->halfLen]] = ceptrum[fd->halfLen] * fd->normalizeGain;
	for (i = 1; i < fd->halfLen; i++)
	{
		padded[fd->mBitRev[i]] = (ceptrum[i] + ceptrum[fd->fftLen - i]) * fd->normalizeGain;
		padded[fd->mBitRev[fd->fftLen - i]] = 0.0f;
	}
	LLdiscreteHartleyFloat(padded, fd->fftLen, fd->mSineTab);
	ceptrum[0] = expf(padded[0]);
	float eR;
	for (i = 1; i < fd->halfLenWdc; i++)
	{
		symIdx = fd->fftLen - i;
		float lR = (padded[i] + padded[symIdx]) * 0.5f;
		float lI = (padded[i] - padded[symIdx]) * 0.5f;
		eR = expf(lR);
		lR = eR * cosf(lI);
		lI = eR * sinf(lI);
		ceptrum[fd->mBitRev[i]] = lR + lI;
		ceptrum[fd->mBitRev[symIdx]] = lR - lI;
	}
	LLdiscreteHartleyFloat(ceptrum, fd->fftLen, fd->mSineTab);
	for (i = 0; i < fd->xLen; i++)
		y[i] = ceptrum[i] * fd->normalizeGain;
	free(padded);
	free(ceptrum);
}
#include "cpthread.h"
typedef struct
{
	int rangeMin, rangeMax;
	fftData *fd;
	float **x, **y;
	int sampleShift;
} mpsThread;
void* mpsMulticore(void *args)
{
	mpsThread *th = (mpsThread*)args;
	for (int i = th->rangeMin; i < th->rangeMax; i++)
		mps(th->fd, &th->x[i][th->sampleShift], th->y[i]);
	return 0;
}
void checkStartEnd(float **signal, int channels, int nsamples, float normalizedDbCutoff1, float normalizedDbCutoff2, int range[2])
{
	int i, j;
	float max = fabsf(signal[0][0]);
	for (i = 0; i < channels; i++)
	{
		for (j = 1; j < nsamples; j++)
		{
			if (fabsf(signal[i][j]) > max)
				max = signal[i][j];
		}
	}
	max = 1.0f / ((max < FLT_EPSILON) ? (max + FLT_EPSILON) : max);
	float linGain1 = powf(10.0f, normalizedDbCutoff1 / 20.0f);
	float linGain2 = powf(10.0f, normalizedDbCutoff2 / 20.0f);
	int firstSmps = 0;
	int lastSmps = 0;
	int firstSmpsPrevious = nsamples - 1;
	int lastSmpsPrevious = 0;
	float normalized;
	int found;
	for (i = 0; i < channels; i++)
	{
		found = 0;
		firstSmps = 0;
		for (j = 0; j < nsamples; j++)
		{
			normalized = fabsf(signal[i][j]) * max;
			if (!found)
			{
				if (normalized > linGain1)
				{
					if (!firstSmps)
					{
						firstSmps = j;
						found = 1;
					}
				}
			}
			if (normalized > linGain2)
				lastSmps = j;
		}
		firstSmpsPrevious = (firstSmpsPrevious < firstSmps) ? firstSmpsPrevious : firstSmps;
		lastSmpsPrevious = (lastSmpsPrevious > lastSmps) ? lastSmpsPrevious : lastSmps;
	}
	range[0] = firstSmpsPrevious != (nsamples - 1) ? firstSmpsPrevious : 0;
	range[1] = lastSmpsPrevious + 1;
}
#include "../libsamplerate/samplerate.h"
#define DRMP3_IMPLEMENTATION
#include "../dr_mp3.h"
#define DR_FLAC_IMPLEMENTATION
#include "../dr_flac.h"
#define DR_WAV_IMPLEMENTATION
#include "../dr_wav.h"
const char *get_filename_ext(const char *filename)
{
    const char *dot = strrchr(filename, '.');
    if (!dot || dot == filename) return "";
    return dot + 1;
}
void reverse(float *arr, int32_t start, int32_t end)
{
	while (start < end)
	{
		float tmp = arr[start];
		arr[start] = arr[end];
		arr[end] = tmp;
		start++;
		end--;
	}
}
void shift(float *arr, int32_t k, int32_t n)
{
	k = k % n;
	reverse(arr, 0, n - 1);
	reverse(arr, 0, n - k - 1);
	reverse(arr, n - k, n - 1);
}
void circshift(float *x, int n, int k)
{
	k < 0 ? shift(x, -k, n) : shift(x, n - k, n);
}
#define NUMPTS 15
#define NUMPTS_DRS (7)
ierper pch1, pch2, pch3;
__attribute__((constructor)) static void initialize(void)
{
    precompute_lpfcoeff_toolbox();
	initIerper(&pch1, NUMPTS + 2);
    initIerper(&pch2, NUMPTS + 2);
    initIerper(&pch3, NUMPTS_DRS + 2);
}
__attribute__((destructor)) static void destruction(void)
{
    clean_lpfcoeff_toolbox();
	freeIerper(&pch1);
	freeIerper(&pch2);
    freeIerper(&pch3);
}
void JamesDSPOfflineResamplingToolbox(float const *in, float *out, size_t lenIn, size_t lenOut, int channels, double src_ratio, int resampleQuality)
{
	if (lenOut == lenIn && lenIn == 1)
	{
		memcpy(out, in, channels * sizeof(float));
		return;
	}
	SRC_DATA src_data;
	memset(&src_data, 0, sizeof(src_data));
	src_data.data_in = in;
	src_data.data_out = out;
	src_data.input_frames = lenIn;
	src_data.output_frames = lenOut;
	src_data.src_ratio = src_ratio;
	int error;
	if ((error = src_simple(&src_data, resampleQuality, channels)))
	{
//		printf("\n%s\n\n", src_strerror(error));
	}
}
float* loadAudioFile(const char *filename, double targetFs, unsigned int *channels, drwav_uint64 *totalPCMFrameCount, int resampleQuality)
{
	unsigned int fs = 1;
    const char *ext = get_filename_ext(filename);
    float *pSampleData = 0;
    if (!strncmp(ext, "wav", 5) || !strncmp(ext, "irs", 5))
        pSampleData = drwav_open_file_and_read_pcm_frames_f32(filename, channels, &fs, totalPCMFrameCount, 0);
    if (!strncmp(ext, "flac", 5))
        pSampleData = drflac_open_file_and_read_pcm_frames_f32(filename, channels, &fs, totalPCMFrameCount, 0);
    if (!strncmp(ext, "mp3", 5))
    {
        drmp3_config mp3Conf;
        pSampleData = drmp3_open_file_and_read_pcm_frames_f32(filename, &mp3Conf, totalPCMFrameCount, 0);
        *channels = mp3Conf.channels;
        fs = mp3Conf.sampleRate;
    }
	if (pSampleData == NULL)
	{
		printf("Error opening and reading WAV file");
		return 0;
	}
	// Sanity check
	if (*channels < 1)
	{
		printf("Invalid audio channels count");
		free(pSampleData);
		return 0;
	}
	if ((*totalPCMFrameCount <= 0) || (*totalPCMFrameCount <= 0))
	{
		printf("Invalid audio sample rate / frame count");
		free(pSampleData);
		return 0;
	}
	double ratio = targetFs / (double)fs;
	if (ratio != 1.0)
	{
		int compressedLen = (int)ceil(*totalPCMFrameCount * ratio);
		float *tmpBuf = (float*)malloc(compressedLen * *channels * sizeof(float));
		memset(tmpBuf, 0, compressedLen * *channels * sizeof(float));
        JamesDSPOfflineResamplingToolbox(pSampleData, tmpBuf, *totalPCMFrameCount, compressedLen, *channels, ratio, resampleQuality);
		*totalPCMFrameCount = compressedLen;
		free(pSampleData);
		return tmpBuf;
	}
	return pSampleData;
}

int validateAdvImpParameter(int frameCount, int convMode, jint* advSetPtr, jsize advSetSize) {
	int frameCountGE8 = frameCount < 8 ? 8 : frameCount;
    int splittedBufferSize = convMode == 2 ? (2 * frameCountGE8) : frameCount;
    for(int i = 2; i < advSetSize; i++) {
        if(advSetPtr[i] >= splittedBufferSize || advSetPtr[i] < 0)
            return 0;
    }
    return 1;
}

JNIEXPORT jfloatArray JNICALL Java_me_timschneeberger_rootlessjamesdsp_interop_JdspImpResToolbox_ReadImpulseResponseToFloat
(JNIEnv *env, jobject obj, jstring path, jint targetSampleRate, jintArray jImpInfo, jint convMode, jintArray jadvParam)
{
	const char *mIRFileName = (*env)->GetStringUTFChars(env, path, 0);
	if (strlen(mIRFileName) <= 0) return 0;
	unsigned int channels;
	drwav_uint64 frameCount;
	float *pFrameBuffer = loadAudioFile(mIRFileName, targetSampleRate, &channels, &frameCount, SRC_SINC_BEST_QUALITY);
	if (channels == 0 || channels == 3 || channels > 4)
	{
		free(pFrameBuffer);
		return 0;
	}
	jint *javaAdvSetPtr = (jint*) (*env)->GetIntArrayElements(env, jadvParam, 0);
    jsize javaAdvSetSize = (*env)->GetArrayLength(env, jadvParam);

    if(javaAdvSetSize != 6) {
        return 0;
    }

    int isAdvSetValid = validateAdvImpParameter(frameCount, convMode, javaAdvSetPtr, javaAdvSetSize);
    if(!isAdvSetValid) {
        // Overwrite invalid advanced params
        javaAdvSetPtr[0] = -80;
        javaAdvSetPtr[1] = -100;
        javaAdvSetPtr[2] = 0;
        javaAdvSetPtr[3] = 0;
        javaAdvSetPtr[4] = 0;
        javaAdvSetPtr[5] = 0;
    }

	int i;
	float *splittedBuffer[4];
	int alloc = frameCount;
	if (alloc < 8)
		alloc = 8;
	for (i = 0; i < channels; i++)
	{
		if (convMode == 2)
		{
			splittedBuffer[i] = (float*)malloc(alloc * 2 * sizeof(float));
			memset(splittedBuffer[i], 0, alloc * 2 * sizeof(float));
		}
		else
			splittedBuffer[i] = (float*)malloc(frameCount * sizeof(float));
	}
	channel_splitFloat(pFrameBuffer, frameCount, splittedBuffer, channels);
	if (convMode > 0)
	{
		free(pFrameBuffer);
		int range[2];
		float startCutdB = javaAdvSetPtr[0];
		float endCutdB = javaAdvSetPtr[1];
		if (convMode == 1)
			checkStartEnd(splittedBuffer, channels, frameCount, startCutdB, endCutdB, range);
		else
		{
			range[0] = 0;
			range[1] = alloc * 2;
		}
		float *outPtr[4];
		int xLen = range[1] - range[0];
		if (convMode == 1)
		{
			checkStartEnd(splittedBuffer, channels, frameCount, startCutdB, endCutdB, range);
			for (i = 0; i < channels; i++)
			{
				outPtr[i] = &splittedBuffer[i][range[0]];
				circshift(outPtr[i], xLen, javaAdvSetPtr[i + 2]);
				for (int j = 0; j < javaAdvSetPtr[i + 2] - 1; j++)
					outPtr[i][j] = 0.0f;
			}
		}
		else
		{
			fftData fd;
			initMpsFFTData(&fd, xLen, -80.0f);
			int spawnNthread = channels - 1;
			if (spawnNthread + 1 > channels)
				spawnNthread = channels - 1;
			int taskPerThread = channels / (spawnNthread + 1);
			pthread_t *pthread = (pthread_t*)malloc(spawnNthread * sizeof(pthread_t));
			mpsThread *th = (mpsThread*)malloc(spawnNthread * sizeof(mpsThread));
			for (i = 0; i < spawnNthread; i++)
			{
				th[i].rangeMin = (i + 1) * taskPerThread;
				if (i < spawnNthread - 1)
					th[i].rangeMax = th[i].rangeMin + taskPerThread;
				th[i].fd = &fd;
				th[i].x = splittedBuffer;
				th[i].y = splittedBuffer;
				th[i].sampleShift = range[0];
			}
			th[spawnNthread - 1].rangeMax = channels;
			for (i = 0; i < spawnNthread; i++)
				pthread_create(&pthread[i], 0, mpsMulticore, &th[i]);
			for (i = 0; i < taskPerThread; i++)
				mps(&fd, &splittedBuffer[i][range[0]], splittedBuffer[i]);
			for (i = 0; i < spawnNthread; i++)
				pthread_join(pthread[i], 0);
			free(pthread);
			free(th);
			freeMpsFFTData(&fd);
			checkStartEnd(splittedBuffer, channels, alloc, startCutdB, endCutdB, range);
			xLen = range[1];
			for (i = 0; i < channels; i++)
			{
				outPtr[i] = &splittedBuffer[i][0];
				circshift(outPtr[i], xLen, javaAdvSetPtr[i + 2]);
				for (int j = 0; j < javaAdvSetPtr[i + 2] - 1; j++)
					outPtr[i][j] = 0.0f;
			}
		}
		unsigned int totalFrames = xLen * channels;
		frameCount = xLen;
		pFrameBuffer = (float*)malloc(totalFrames * sizeof(float));
		channel_joinFloat(outPtr, channels, pFrameBuffer, xLen);
	}
	else
	{
		for (i = 0; i < channels; i++)
		{
			circshift(splittedBuffer[i], frameCount, javaAdvSetPtr[i + 2]);
			for (int j = 0; j < javaAdvSetPtr[i + 2] - 1; j++)
				splittedBuffer[i][j] = 0.0f;
		}
		channel_joinFloat(splittedBuffer, channels, pFrameBuffer, frameCount);
	}
	for (i = 0; i < channels; i++)
		free(splittedBuffer[i]);
	(*env)->ReleaseIntArrayElements(env, jadvParam, javaAdvSetPtr, 0);
	jint *javaBasicInfoPtr = (jint*) (*env)->GetIntArrayElements(env, jImpInfo, 0);
	javaBasicInfoPtr[0] = (int)channels;
	javaBasicInfoPtr[1] = (int)frameCount;
	javaBasicInfoPtr[2] = (int)isAdvSetValid;
	(*env)->SetIntArrayRegion(env, jImpInfo, 0, 3, javaBasicInfoPtr);
	jfloatArray outbuf;
	int frameCountTotal = channels * frameCount;
	size_t bufferSize = frameCountTotal * sizeof(float);
	outbuf = (*env)->NewFloatArray(env, (jsize)frameCountTotal);
	(*env)->SetFloatArrayRegion(env, outbuf, 0, (jsize)frameCountTotal, pFrameBuffer);
	free(pFrameBuffer);
	return outbuf;
}
JNIEXPORT jstring JNICALL Java_me_timschneeberger_rootlessjamesdsp_interop_JdspImpResToolbox_OfflineAudioResample
(JNIEnv *env, jobject obj, jstring path, jstring filename, jint targetSampleRate)
{
	const char *jnipath = (*env)->GetStringUTFChars(env, path, 0);
	if (strlen(jnipath) <= 0) return 0;
	const char *mIRFileName = (*env)->GetStringUTFChars(env, filename, 0);
	if (strlen(mIRFileName) <= 0) return 0;
	size_t needed = snprintf(NULL, 0, "%s%s", jnipath, mIRFileName) + 1;
	char *filenameIR = malloc(needed);
	snprintf(filenameIR, needed, "%s%s", jnipath, mIRFileName);
	unsigned int channels;
	drwav_uint64 frameCount;
	float *pFrameBuffer = loadAudioFile(filenameIR, targetSampleRate, &channels, &frameCount, SRC_SINC_BEST_QUALITY);
	free(filenameIR);
	if (!pFrameBuffer)
	{
		needed = snprintf(NULL, 0, "Invalid") + 1;
		filenameIR = malloc(needed);
		snprintf(filenameIR, needed, "Invalid");
	}
	else
	{
		needed = snprintf(NULL, 0, "%s%d_%s", jnipath, targetSampleRate, mIRFileName) + 1;
		filenameIR = malloc(needed);
		snprintf(filenameIR, needed, "%s%d_%s", jnipath, targetSampleRate, mIRFileName);
		drwav pWav;
		drwav_data_format format;
		format.container = drwav_container_riff;
		format.format = DR_WAVE_FORMAT_IEEE_FLOAT;
		format.channels = channels;
		format.sampleRate = targetSampleRate;
		format.bitsPerSample = 32;
		unsigned int fail = drwav_init_file_write(&pWav, filenameIR, &format, 0);
		drwav_uint64 framesWritten = drwav_write_pcm_frames(&pWav, frameCount, pFrameBuffer);
		drwav_uninit(&pWav);
		free(pFrameBuffer);
	}
	(*env)->ReleaseStringUTFChars(env, path, jnipath);
	(*env)->ReleaseStringUTFChars(env, filename, mIRFileName);
	jstring finalName = (*env)->NewStringUTF(env, filenameIR);
	free(filenameIR);
	return finalName;
}
double freq[NUMPTS + 2];
double gain[NUMPTS + 2];
JNIEXPORT jint JNICALL Java_me_timschneeberger_rootlessjamesdsp_interop_JdspImpResToolbox_ComputeEqResponse
(JNIEnv *env, jobject obj, jint n, jdoubleArray jfreq, jdoubleArray jgain, jint interpolationMode, jint queryPts, jdoubleArray dispFreq, jfloatArray response)
{
	jdouble *javaFreqPtr = (jdouble*) (*env)->GetDoubleArrayElements(env, jfreq, 0);
	jdouble *javaGainPtr = (jdouble*) (*env)->GetDoubleArrayElements(env, jgain, 0);
	jdouble *javadispFreqPtr = (jdouble*) (*env)->GetDoubleArrayElements(env, dispFreq, 0);
	jfloat *javaResponsePtr = (jfloat*) (*env)->GetFloatArrayElements(env, response, 0);
	memcpy(freq + 1, javaFreqPtr, NUMPTS * sizeof(double));
	memcpy(gain + 1, javaGainPtr, NUMPTS * sizeof(double));
	(*env)->ReleaseDoubleArrayElements(env, jfreq, javaFreqPtr, 0);
	(*env)->ReleaseDoubleArrayElements(env, jgain, javaGainPtr, 0);
	freq[0] = 0.0;
	gain[0] = gain[1];
	freq[NUMPTS + 1] = 24000.0;
	gain[NUMPTS + 1] = gain[NUMPTS];
	ierper *lerpPtr;
	if (!interpolationMode)
	{
		pchip(&pch1, freq, gain, NUMPTS + 2, 1, 1);
		lerpPtr = &pch1;
	}
	else
	{
		makima(&pch2, freq, gain, NUMPTS + 2, 1, 1);
		lerpPtr = &pch2;
	}
	for (int i = 0; i < queryPts; i++)
	{
		javaResponsePtr[i] = (float)getValueAt(&lerpPtr->cb, javadispFreqPtr[i]);
	}
	(*env)->ReleaseDoubleArrayElements(env, dispFreq, javadispFreqPtr, 0);
	(*env)->SetFloatArrayRegion(env, response, 0, queryPts, javaResponsePtr);
	return 0;
}

JNIEXPORT void JNICALL Java_me_timschneeberger_rootlessjamesdsp_interop_JdspImpResToolbox_ComputeCompResponse(JNIEnv *env, jobject obj, jdoubleArray jfreq, jdoubleArray jgain, jint queryPts, jdoubleArray dispFreq, jfloatArray response)
{
    double freqComp[NUMPTS_DRS + 2];
    double gainComp[NUMPTS_DRS + 2];

    jdouble *javaFreqPtr = (jdouble*) (*env)->GetDoubleArrayElements(env, jfreq, 0);
    jdouble *javaGainPtr = (jdouble*) (*env)->GetDoubleArrayElements(env, jgain, 0);
    jdouble *javadispFreqPtr = (jdouble*) (*env)->GetDoubleArrayElements(env, dispFreq, 0);
    jfloat *javaResponsePtr = (jfloat*) (*env)->GetFloatArrayElements(env, response, 0);

    memcpy(freqComp + 1, javaFreqPtr, NUMPTS_DRS * sizeof(double));
    memcpy(gainComp + 1, javaGainPtr, NUMPTS_DRS * sizeof(double));
    (*env)->ReleaseDoubleArrayElements(env, jfreq, javaFreqPtr, 0);
    (*env)->ReleaseDoubleArrayElements(env, jgain, javaGainPtr, 0);

    freqComp[0] = 0.0;
    gainComp[0] = gainComp[1];
    freqComp[NUMPTS_DRS + 1] = 24000.0;
    gainComp[NUMPTS_DRS + 1] = gainComp[NUMPTS_DRS];
    makima(&pch3, freqComp, gainComp, NUMPTS_DRS + 2, 1, 1);
    ierper *lerpPtr = &pch3;
    for (int i = 0; i < queryPts; i++)
        javaResponsePtr[i] = (float)getValueAt(&lerpPtr->cb, javadispFreqPtr[i]);

    (*env)->ReleaseDoubleArrayElements(env, dispFreq, javadispFreqPtr, 0);
    (*env)->SetFloatArrayRegion(env, response, 0, queryPts, javaResponsePtr);
}

JNIEXPORT void JNICALL Java_me_timschneeberger_rootlessjamesdsp_interop_JdspImpResToolbox_ComputeIIREqualizerCplx(JNIEnv *env, jobject obj, jint srate, jint order, jdoubleArray jfreq, jdoubleArray jgain, jint nPts, jdoubleArray jdispFreq, jdoubleArray jcplxRe, jdoubleArray jcplxIm)
{
    jdouble *freqs = (jdouble*) (*env)->GetDoubleArrayElements(env, jfreq, 0);
    jdouble *gains = (jdouble*) (*env)->GetDoubleArrayElements(env, jgain, 0);
    jdouble *dispFreq = (jdouble*) (*env)->GetDoubleArrayElements(env, jdispFreq, 0);
    jdouble *cplxRe = (jdouble*) (*env)->GetDoubleArrayElements(env, jcplxRe, 0);
    jdouble *cplxIm = (jdouble*) (*env)->GetDoubleArrayElements(env, jcplxIm, 0);

    for (int i = 0; i < nPts; i++)
    {
        cplxRe[i] = 1;
        cplxIm[i] = 0;
    }

    for (int i = 0; i < NUMPTS - 1; i++)
    {
        double dB = gains[i + 1] - gains[i];
        double designFreq;
        if (i)
            designFreq = (freqs[i + 1] + freqs[i]) * 0.5;
        else
            designFreq = freqs[i];
        double overallGain = i == 0 ? gains[i] : 0.0;
        HSHOResponse(48000.0, designFreq, (unsigned int)order, dB, overallGain, nPts, dispFreq, cplxRe, cplxIm);
    }

    (*env)->SetDoubleArrayRegion(env, jcplxRe, 0, nPts, cplxRe);
    (*env)->SetDoubleArrayRegion(env, jcplxIm, 0, nPts, cplxIm);
    (*env)->ReleaseDoubleArrayElements(env, jfreq, freqs, 0);
    (*env)->ReleaseDoubleArrayElements(env, jgain, gains, 0);
    (*env)->ReleaseDoubleArrayElements(env, jdispFreq, dispFreq, 0);
}

JNIEXPORT void JNICALL Java_me_timschneeberger_rootlessjamesdsp_interop_JdspImpResToolbox_ComputeIIREqualizerResponse(JNIEnv *env, jobject obj, jint nPts, jdoubleArray jcplxRe, jdoubleArray jcplxIm, jfloatArray jresponse)
{
    jdouble *cplxRe = (jdouble*) (*env)->GetDoubleArrayElements(env, jcplxRe, 0);
    jdouble *cplxIm = (jdouble*) (*env)->GetDoubleArrayElements(env, jcplxIm, 0);
    jfloat *response = (jfloat*) (*env)->GetFloatArrayElements(env, jresponse, 0);
    for(int i = 0; i < nPts; i++) {
        response[i] = 20.0f * log10f(hypot(cplxRe[i], cplxIm[i]));
    }
    (*env)->SetFloatArrayRegion(env, jresponse, 0, nPts, response);
    (*env)->ReleaseDoubleArrayElements(env, jcplxRe, cplxRe, 0);
    (*env)->ReleaseDoubleArrayElements(env, jcplxIm, cplxIm, 0);
}
