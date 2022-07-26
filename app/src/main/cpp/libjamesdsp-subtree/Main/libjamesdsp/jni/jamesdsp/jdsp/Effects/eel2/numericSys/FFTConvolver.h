#ifndef _FFTCONVOLVER_FFTCONVOLVER_H
#define _FFTCONVOLVER_FFTCONVOLVER_H
extern unsigned int upper_power_of_two(unsigned int v);
/**
* @class FFTConvolver1x1
* @brief Implementation of a partitioned FFT convolution algorithm with uniform block size
*
* Some notes on how to use it:
*
* - After initialization with an impulse response, subsequent data portions of
*   arbitrary length can be convolved. The convolver internally can handle
*   this by using appropriate buffering.
*
* - The convolver works without "latency" (except for the required
*   processing time, of course), i.e. the output always is the convolved
*   input for each processing call.
*
* - The convolver is suitable for real-time processing which means that no
*   "unpredictable" operations like allocations, locking, API calls, etc. are
*   performed during processing (all necessary allocations and preparations take
*   place during initialization).
*/
typedef struct
{
	unsigned int _blockSize;
	unsigned int _segSize;
	unsigned int _segCount;
	unsigned int _segCountMinus1;
	unsigned int _fftComplexSize;
	float **_segmentsRe;
	float **_segmentsIm;
	float **_segmentsIRRe;
	float **_segmentsIRIm;
	float *_fftBuffer;
	unsigned int *bit;
	float *sine;
	float *_preMultiplied[2];
	float *_overlap;
	unsigned int _current;
	float *_inputBuffer;
	unsigned int _inputBufferFill;
	float gain; // float32, it's perfectly safe to have blockSize == 2097152, however, it's impractical to have such large block
	void(*fft)(float*, const float*);
} FFTConvolver1x1;
typedef struct
{
	unsigned int _blockSize;
	unsigned int _segSize;
	unsigned int _segCount;
	unsigned int _segCountMinus1;
	unsigned int _fftComplexSize;
	float **_segmentsReLeft;
	float **_segmentsImLeft;
	float **_segmentsReRight;
	float **_segmentsImRight;
	float **_segmentsLLIRRe;
	float **_segmentsLLIRIm;
	float **_segmentsLRIRRe;
	float **_segmentsLRIRIm;
	float **_segmentsRLIRRe;
	float **_segmentsRLIRIm;
	float **_segmentsRRIRRe;
	float **_segmentsRRIRIm;
	float *_fftBuffer[2];
	unsigned int *bit;
	float *sine;
	float *_preMultiplied[2][2];
	float *_overlap[2];
	unsigned int _current;
	float *_inputBuffer[2];
	unsigned int _inputBufferFill;
	float gain; // float32, it's perfectly safe to have blockSize == 2097152, however, it's impractical to have such large block
	void(*fft)(float*, const float*);
} FFTConvolver2x4x2;
typedef struct
{
	unsigned int _blockSize;
	unsigned int _segSize;
	unsigned int _segCount;
	unsigned int _segCountMinus1;
	unsigned int _fftComplexSize;
	float **_segmentsReLeft;
	float **_segmentsImLeft;
	float **_segmentsReRight;
	float **_segmentsImRight;
	float **_segmentsLLIRRe;
	float **_segmentsLLIRIm;
	float **_segmentsRRIRRe;
	float **_segmentsRRIRIm;
	float *_fftBuffer[2];
	unsigned int *bit;
	float *sine;
	float *_preMultiplied[2][2];
	float *_overlap[2];
	unsigned int _current;
	float *_inputBuffer[2];
	unsigned int _inputBufferFill;
	float gain; // float32, it's perfectly safe to have blockSize == 2097152, however, it's impractical to have such large block
	void(*fft)(float*, const float*);
} FFTConvolver2x2;
typedef struct
{
	unsigned int _blockSize;
	unsigned int _segSize;
	unsigned int _segCount;
	unsigned int _segCountMinus1;
	unsigned int _fftComplexSize;
	float **_segmentsRe;
	float **_segmentsIm;
	float **_segmentsLLIRRe;
	float **_segmentsLLIRIm;
	float **_segmentsRRIRRe;
	float **_segmentsRRIRIm;
	float *_fftBuffer[2];
	unsigned int *bit;
	float *sine;
	float *_preMultiplied[2][2];
	float *_overlap[2];
	unsigned int _current;
	float *_inputBuffer;
	unsigned int _inputBufferFill;
	float gain; // float32, it's perfectly safe to have blockSize == 2097152, however, it's impractical to have such large block
	void(*fft)(float*, const float*);
} FFTConvolver1x2;
extern void FFTConvolver1x1Init(FFTConvolver1x1 *conv);
extern void FFTConvolver2x4x2Init(FFTConvolver2x4x2 *conv);
extern void FFTConvolver2x2Init(FFTConvolver2x2 *conv);
extern void FFTConvolver1x2Init(FFTConvolver1x2 *conv);

/**
* @brief Initializes the convolver
* @param blockSize Block size internally used by the convolver (partition size)
* @param ir The impulse response
* @param irLen Length of the impulse response
* @return 1: Success - 0: Failed
*/
int FFTConvolver1x1LoadImpulseResponse(FFTConvolver1x1 *conv, unsigned int blockSize, const float* ir, unsigned int irLen);
int FFTConvolver2x4x2LoadImpulseResponse(FFTConvolver2x4x2 *conv, unsigned int blockSize, const float* irLL, const float* irLR, const float* irRL, const float* irRR, unsigned int irLen);
int FFTConvolver2x2LoadImpulseResponse(FFTConvolver2x2 *conv, unsigned int blockSize, const float* irL, const float* irR, unsigned int irLen);
int FFTConvolver1x2LoadImpulseResponse(FFTConvolver1x2 *conv, unsigned int blockSize, const float* irL, const float* irR, unsigned int irLen);

/**
* @brief Convolves the the given input samples and immediately outputs the result
* @param input The input samples
* @param output The convolution result
* @param len Number of input/output samples
*/
extern void FFTConvolver1x1Process(FFTConvolver1x1 *conv, const float* input, float* output, unsigned int len);
extern void FFTConvolver2x4x2Process(FFTConvolver2x4x2 *conv, const float* x1, const float* x2, float* y1, float* y2, unsigned int len);
extern void FFTConvolver2x2Process(FFTConvolver2x2 *conv, const float* x1, const float* x2, float* y1, float* y2, unsigned int len);
extern void FFTConvolver1x2Process(FFTConvolver1x2 *conv, const float* x, float* y1, float* y2, unsigned int len);

/**
* @brief Resets the convolver and discards the set impulse response
*/
extern void FFTConvolver1x1Free(FFTConvolver1x1 *conv);
extern void FFTConvolver2x4x2Free(FFTConvolver2x4x2 *conv);
extern void FFTConvolver2x2Free(FFTConvolver2x2 *conv);
extern void FFTConvolver1x2Free(FFTConvolver1x2 *conv);

/**
* @brief Refreshs the convolver without modify memory pointers
* blockSize, irLen must be the same as the one in initialization
*/
extern int FFTConvolver1x1RefreshImpulseResponse(FFTConvolver1x1 *conv, unsigned int blockSize, const float* ir, unsigned int irLen);
extern int FFTConvolver2x2RefreshImpulseResponse(FFTConvolver2x2 *conv, unsigned int blockSize, const float* irL, const float* irR, unsigned int irLen);
#endif