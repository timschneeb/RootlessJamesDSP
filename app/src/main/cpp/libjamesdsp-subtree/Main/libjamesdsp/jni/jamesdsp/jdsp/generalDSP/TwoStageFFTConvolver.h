#ifndef _FFTCONVOLVER_TWOSTAGEFFTCONVOLVER_H
#define _FFTCONVOLVER_TWOSTAGEFFTCONVOLVER_H

#include "../Effects/eel2/numericSys/FFTConvolver.h"
#define THREAD
#ifdef THREAD
#include "../Effects/eel2/cpthread.h"
enum pt_state
{
	NOTHING,
	SETUP,
	IDLE,
	WORKING,
	GET_OFF_FROM_WORK
};
typedef struct
{
	enum pt_state state;
	pthread_cond_t work_cond;
	pthread_mutex_t work_mtx;
	pthread_cond_t boss_cond;
	pthread_mutex_t boss_mtx;
	FFTConvolver1x1 *_tailConvolver;
	float *_backgroundProcessingInput;
	float *_tailOutput;
	unsigned int _tailBlockSize;
} pt_info;
typedef struct
{
	enum pt_state state;
	pthread_cond_t work_cond;
	pthread_mutex_t work_mtx;
	pthread_cond_t boss_cond;
	pthread_mutex_t boss_mtx;
	FFTConvolver2x4x2 *_tailConvolver;
	float *_backgroundProcessingInput[2];
	float *_tailOutput[2];
	unsigned int _tailBlockSize;
} pt_info2x4x2;
typedef struct
{
	enum pt_state state;
	pthread_cond_t work_cond;
	pthread_mutex_t work_mtx;
	pthread_cond_t boss_cond;
	pthread_mutex_t boss_mtx;
	FFTConvolver2x2 *_tailConvolver;
	float *_backgroundProcessingInput[2];
	float *_tailOutput[2];
	unsigned int _tailBlockSize;
} pt_info2x2;
typedef struct
{
	enum pt_state state;
	pthread_cond_t work_cond;
	pthread_mutex_t work_mtx;
	pthread_cond_t boss_cond;
	pthread_mutex_t boss_mtx;
	FFTConvolver1x2 *_tailConvolver;
	float *_backgroundProcessingInput;
	float *_tailOutput[2];
	unsigned int _tailBlockSize;
} pt_info1x2;
#endif
typedef struct
{
	unsigned int _headBlockSize;
	unsigned int _tailBlockSize;
	FFTConvolver1x1 _headConvolver;
	FFTConvolver1x1 _tailConvolver0;
	FFTConvolver1x1 _tailConvolver;
	float *_tailOutput0;
	float *_tailPrecalculated0;
	float *_tailOutput;
	float *_tailPrecalculated;
	float *_tailInput;
	float *_backgroundProcessingInput;
	unsigned int _tailInputFill;
	unsigned int _precalculatedPos;
#ifdef THREAD
	// Thread
	pthread_t threads;
	pt_info shared_info;
#endif
} TwoStageFFTConvolver1x1;
typedef struct
{
	unsigned int _headBlockSize;
	unsigned int _tailBlockSize;
	FFTConvolver2x4x2 _headConvolver;
	FFTConvolver2x4x2 _tailConvolver0;
	FFTConvolver2x4x2 _tailConvolver;
	float *_tailOutput0[2];
	float *_tailPrecalculated0[2];
	float *_tailOutput[2];
	float *_tailPrecalculated[2];
	float *_tailInput[2];
	float *_backgroundProcessingInput[2];
	unsigned int _tailInputFill;
	unsigned int _precalculatedPos;
#ifdef THREAD
	// Thread
	pthread_t threads;
	pt_info2x4x2 shared_info;
#endif
} TwoStageFFTConvolver2x4x2;
typedef struct
{
	unsigned int _headBlockSize;
	unsigned int _tailBlockSize;
	FFTConvolver2x2 _headConvolver;
	FFTConvolver2x2 _tailConvolver0;
	FFTConvolver2x2 _tailConvolver;
	float *_tailOutput0[2];
	float *_tailPrecalculated0[2];
	float *_tailOutput[2];
	float *_tailPrecalculated[2];
	float *_tailInput[2];
	float *_backgroundProcessingInput[2];
	unsigned int _tailInputFill;
	unsigned int _precalculatedPos;
#ifdef THREAD
	// Thread
	pthread_t threads;
	pt_info2x2 shared_info;
#endif
} TwoStageFFTConvolver2x2;
typedef struct
{
	unsigned int _headBlockSize;
	unsigned int _tailBlockSize;
	FFTConvolver1x2 _headConvolver;
	FFTConvolver1x2 _tailConvolver0;
	FFTConvolver1x2 _tailConvolver;
	float *_tailOutput0[2];
	float *_tailPrecalculated0[2];
	float *_tailOutput[2];
	float *_tailPrecalculated[2];
	float *_tailInput;
	float *_backgroundProcessingInput;
	unsigned int _tailInputFill;
	unsigned int _precalculatedPos;
#ifdef THREAD
	// Thread
	pthread_t threads;
	pt_info1x2 shared_info;
#endif
} TwoStageFFTConvolver1x2;
/**
* @class TwoStageFFTConvolver1x1
* @brief FFT convolver using two different block sizes
*
* The 2-stage convolver consists internally of two convolvers:
*
* - A head convolver, which processes the only the begin of the impulse response.
*
* - A tail convolver, which processes the rest and major amount of the impulse response.
*
* Using a short block size for the head convolver and a long block size for
* the tail convolver results in much less CPU usage, while keeping the
* calculation time of each processing call short.
*
* Furthermore, this convolver class provides virtual methods which provide the
* possibility to move the tail convolution into the background (e.g. by using
* multithreading, see startBackgroundProcessing()/waitForBackgroundProcessing()).
*
* As well as the basic FFTConvolver1x1 class, the 2-stage convolver is suitable
* for real-time processing which means that no "unpredictable" operations like
* allocations, locking, API calls, etc. are performed during processing (all
* necessary allocations and preparations take place during initialization).
*/
extern void TwoStageFFTConvolver1x1Init(TwoStageFFTConvolver1x1 *conv);
extern void TwoStageFFTConvolver2x4x2Init(TwoStageFFTConvolver2x4x2 *conv);
extern void TwoStageFFTConvolver2x2Init(TwoStageFFTConvolver2x2 *conv);
extern void TwoStageFFTConvolver1x2Init(TwoStageFFTConvolver1x2 *conv);

/**
* @brief Initialization the convolver
* @param headBlockSize The head block size
* @param tailBlockSize the tail block size
* @param ir The impulse response
* @param irLen Length of the impulse response in samples
* @return 1: Success - 0: Failed
*/
int TwoStageFFTConvolver1x1LoadImpulseResponse(TwoStageFFTConvolver1x1 *conv, unsigned int headBlockSize, unsigned int tailBlockSize, const float* ir, unsigned int irLen);
int TwoStageFFTConvolver2x4x2LoadImpulseResponse(TwoStageFFTConvolver2x4x2 *conv, unsigned int headBlockSize, unsigned int tailBlockSize, const float* irLL, const float* irLR, const float* irRL, const float* irRR, unsigned int irLen);
int TwoStageFFTConvolver2x2LoadImpulseResponse(TwoStageFFTConvolver2x2 *conv, unsigned int headBlockSize, unsigned int tailBlockSize, const float* irL, const float* irR, unsigned int irLen);
int TwoStageFFTConvolver1x2LoadImpulseResponse(TwoStageFFTConvolver1x2 *conv, unsigned int headBlockSize, unsigned int tailBlockSize, const float* irL, const float* irR, unsigned int irLen);

/**
* @brief Convolves the the given input samples and immediately outputs the result
* @param input The input samples
* @param output The convolution result
* @param len Number of input/output samples
*/
extern void TwoStageFFTConvolver1x1Process(TwoStageFFTConvolver1x1 *conv, const float* input, float* output, unsigned int len);
extern void TwoStageFFTConvolver2x4x2Process(TwoStageFFTConvolver2x4x2 *conv, const float* x1, const float* x2, float* y1, float* y2, unsigned int len);
extern void TwoStageFFTConvolver2x2Process(TwoStageFFTConvolver2x2 *conv, const float* x1, const float* x2, float* y1, float* y2, unsigned int len);
extern void TwoStageFFTConvolver1x2Process(TwoStageFFTConvolver1x2 *conv, const float* x, float* y1, float* y2, unsigned int len);

/**
* @brief Resets the convolver and discards the set impulse response
*/
extern void TwoStageFFTConvolver1x1Free(TwoStageFFTConvolver1x1 *conv);
extern void TwoStageFFTConvolver2x4x2Free(TwoStageFFTConvolver2x4x2 *conv);
extern void TwoStageFFTConvolver2x2Free(TwoStageFFTConvolver2x2 *conv);
extern void TwoStageFFTConvolver1x2Free(TwoStageFFTConvolver1x2 *conv);
#endif
