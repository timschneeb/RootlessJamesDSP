#include "FFTConvolver.h"
#include <stdlib.h>
#include <string.h>
#include <float.h>
#include <math.h>
#include "codelet.h"
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
void FFTConvolver1x1Init(FFTConvolver1x1 *conv)
{
	conv->bit = 0;
	conv->_blockSize = 0;
	conv->_segSize = 0;
	conv->_segCount = 0;
	conv->_fftComplexSize = 0;
	conv->_segmentsRe = 0;
	conv->_segmentsIm = 0;
	conv->_segmentsIRRe = 0;
	conv->_segmentsIRIm = 0;
	conv->_current = 0;
	conv->_fftBuffer = 0;
	conv->_inputBuffer = 0;
	conv->_overlap = 0;
	conv->_inputBufferFill = 0;
	conv->_preMultiplied[0] = 0;
}
void FFTConvolver2x4x2Init(FFTConvolver2x4x2 *conv)
{
	conv->bit = 0;
	conv->_blockSize = 0;
	conv->_segSize = 0;
	conv->_segCount = 0;
	conv->_fftComplexSize = 0;
	conv->_segmentsReLeft = 0;
	conv->_segmentsImLeft = 0;
	conv->_segmentsReRight = 0;
	conv->_segmentsImRight = 0;
	conv->_segmentsLLIRRe = 0;
	conv->_segmentsLLIRIm = 0;
	conv->_segmentsLRIRRe = 0;
	conv->_segmentsLRIRIm = 0;
	conv->_segmentsRLIRRe = 0;
	conv->_segmentsRLIRIm = 0;
	conv->_segmentsRRIRRe = 0;
	conv->_segmentsRRIRIm = 0;
	conv->_current = 0;
	conv->_fftBuffer[0] = 0;
	conv->_inputBuffer[0] = 0;
	conv->_overlap[0] = 0;
	conv->_inputBufferFill = 0;
	conv->_preMultiplied[0][0] = 0;
}
void FFTConvolver2x2Init(FFTConvolver2x2 *conv)
{
	conv->bit = 0;
	conv->_blockSize = 0;
	conv->_segSize = 0;
	conv->_segCount = 0;
	conv->_fftComplexSize = 0;
	conv->_segmentsReLeft = 0;
	conv->_segmentsImLeft = 0;
	conv->_segmentsReRight = 0;
	conv->_segmentsImRight = 0;
	conv->_segmentsLLIRRe = 0;
	conv->_segmentsLLIRIm = 0;
	conv->_segmentsRRIRRe = 0;
	conv->_segmentsRRIRIm = 0;
	conv->_current = 0;
	conv->_fftBuffer[0] = 0;
	conv->_inputBuffer[0] = 0;
	conv->_overlap[0] = 0;
	conv->_inputBufferFill = 0;
	conv->_preMultiplied[0][0] = 0;
}
void FFTConvolver1x2Init(FFTConvolver1x2 *conv)
{
	conv->bit = 0;
	conv->_blockSize = 0;
	conv->_segSize = 0;
	conv->_segCount = 0;
	conv->_fftComplexSize = 0;
	conv->_segmentsRe = 0;
	conv->_segmentsLLIRRe = 0;
	conv->_segmentsLLIRIm = 0;
	conv->_segmentsRRIRRe = 0;
	conv->_segmentsRRIRIm = 0;
	conv->_current = 0;
	conv->_fftBuffer[0] = 0;
	conv->_inputBuffer = 0;
	conv->_overlap[0] = 0;
	conv->_inputBufferFill = 0;
	conv->_preMultiplied[0][0] = 0;
}
void FFTConvolver1x1Free(FFTConvolver1x1 *conv)
{
	if (conv->_segmentsRe)
	{
		for (unsigned int i = 0; i < conv->_segCount; ++i)
		{
			free(conv->_segmentsRe[i]);
			free(conv->_segmentsIm[i]);
			free(conv->_segmentsIRRe[i]);
			free(conv->_segmentsIRIm[i]);
		}
		free(conv->_segmentsRe);
		free(conv->_segmentsIm);
		free(conv->_segmentsIRRe);
		free(conv->_segmentsIRIm);
		conv->_segmentsRe = 0;
		conv->_segmentsIm = 0;
		conv->_segmentsIRRe = 0;
		conv->_segmentsIRIm = 0;
	}
	if (conv->_preMultiplied[0])
	{
		free(conv->_preMultiplied[0]);
		free(conv->_preMultiplied[1]);
		conv->_preMultiplied[0] = 0;
	}
	if (conv->bit)
	{
		free(conv->bit);
		free(conv->sine);
		conv->bit = 0;
	}
	conv->_blockSize = 0;
	conv->_segSize = 0;
	conv->_segCount = 0;
	conv->_fftComplexSize = 0;
	if (conv->_fftBuffer)
	{
		free(conv->_fftBuffer);
		conv->_fftBuffer = 0;
	}
	if (conv->_overlap)
	{
		free(conv->_overlap);
		conv->_overlap = 0;
	}
	if (conv->_inputBuffer)
	{
		free(conv->_inputBuffer);
		conv->_inputBuffer = 0;
	}
	conv->_current = 0;
	conv->_inputBufferFill = 0;
}
void FFTConvolver2x4x2Free(FFTConvolver2x4x2 *conv)
{
	if (conv->_segmentsReLeft)
	{
		for (unsigned int i = 0; i < conv->_segCount; ++i)
		{
			free(conv->_segmentsReLeft[i]);
			free(conv->_segmentsImLeft[i]);
			free(conv->_segmentsReRight[i]);
			free(conv->_segmentsImRight[i]);
			free(conv->_segmentsLLIRRe[i]);
			free(conv->_segmentsLLIRIm[i]);
			free(conv->_segmentsLRIRRe[i]);
			free(conv->_segmentsLRIRIm[i]);
			free(conv->_segmentsRLIRRe[i]);
			free(conv->_segmentsRLIRIm[i]);
			free(conv->_segmentsRRIRRe[i]);
			free(conv->_segmentsRRIRIm[i]);
		}
		free(conv->_segmentsReLeft);
		free(conv->_segmentsImLeft);
		free(conv->_segmentsReRight);
		free(conv->_segmentsImRight);
		free(conv->_segmentsLLIRRe);
		free(conv->_segmentsLLIRIm);
		free(conv->_segmentsLRIRRe);
		free(conv->_segmentsLRIRIm);
		free(conv->_segmentsRLIRRe);
		free(conv->_segmentsRLIRIm);
		free(conv->_segmentsRRIRRe);
		free(conv->_segmentsRRIRIm);
		conv->_segmentsReLeft = 0;
		conv->_segmentsImLeft = 0;
		conv->_segmentsReRight = 0;
		conv->_segmentsImRight = 0;
		conv->_segmentsLLIRRe = 0;
		conv->_segmentsLLIRIm = 0;
		conv->_segmentsLRIRRe = 0;
		conv->_segmentsLRIRIm = 0;
		conv->_segmentsRLIRRe = 0;
		conv->_segmentsRLIRIm = 0;
		conv->_segmentsRRIRRe = 0;
		conv->_segmentsRRIRIm = 0;
	}
	if (conv->_preMultiplied[0][0])
	{
		free(conv->_preMultiplied[0][0]);
		free(conv->_preMultiplied[0][1]);
		free(conv->_preMultiplied[1][0]);
		free(conv->_preMultiplied[1][1]);
		conv->_preMultiplied[0][0] = 0;
	}
	if (conv->bit)
	{
		free(conv->bit);
		free(conv->sine);
		conv->bit = 0;
	}
	conv->_blockSize = 0;
	conv->_segSize = 0;
	conv->_segCount = 0;
	conv->_fftComplexSize = 0;
	if (conv->_fftBuffer[0])
	{
		free(conv->_fftBuffer[0]);
		free(conv->_fftBuffer[1]);
		conv->_fftBuffer[0] = 0;
	}
	if (conv->_overlap[0])
	{
		free(conv->_overlap[0]);
		free(conv->_overlap[1]);
		conv->_overlap[0] = 0;
	}
	if (conv->_inputBuffer[0])
	{
		free(conv->_inputBuffer[0]);
		free(conv->_inputBuffer[1]);
		conv->_inputBuffer[0] = 0;
		conv->_inputBuffer[1] = 0;
	}
	conv->_current = 0;
	conv->_inputBufferFill = 0;
}
void FFTConvolver2x2Free(FFTConvolver2x2 *conv)
{
	if (conv->_segmentsReLeft)
	{
		for (unsigned int i = 0; i < conv->_segCount; ++i)
		{
			free(conv->_segmentsReLeft[i]);
			free(conv->_segmentsImLeft[i]);
			free(conv->_segmentsReRight[i]);
			free(conv->_segmentsImRight[i]);
			free(conv->_segmentsLLIRRe[i]);
			free(conv->_segmentsLLIRIm[i]);
			free(conv->_segmentsRRIRRe[i]);
			free(conv->_segmentsRRIRIm[i]);
		}
		free(conv->_segmentsReLeft);
		free(conv->_segmentsImLeft);
		free(conv->_segmentsReRight);
		free(conv->_segmentsImRight);
		free(conv->_segmentsLLIRRe);
		free(conv->_segmentsLLIRIm);
		free(conv->_segmentsRRIRRe);
		free(conv->_segmentsRRIRIm);
		conv->_segmentsReLeft = 0;
		conv->_segmentsImLeft = 0;
		conv->_segmentsReRight = 0;
		conv->_segmentsImRight = 0;
		conv->_segmentsLLIRRe = 0;
		conv->_segmentsLLIRIm = 0;
		conv->_segmentsRRIRRe = 0;
		conv->_segmentsRRIRIm = 0;
	}
	if (conv->_preMultiplied[0][0])
	{
		free(conv->_preMultiplied[0][0]);
		free(conv->_preMultiplied[0][1]);
		free(conv->_preMultiplied[1][0]);
		free(conv->_preMultiplied[1][1]);
		conv->_preMultiplied[0][0] = 0;
	}
	if (conv->bit)
	{
		free(conv->bit);
		free(conv->sine);
		conv->bit = 0;
	}
	conv->_blockSize = 0;
	conv->_segSize = 0;
	conv->_segCount = 0;
	conv->_fftComplexSize = 0;
	if (conv->_fftBuffer[0])
	{
		free(conv->_fftBuffer[0]);
		free(conv->_fftBuffer[1]);
		conv->_fftBuffer[0] = 0;
	}
	if (conv->_overlap[0])
	{
		free(conv->_overlap[0]);
		free(conv->_overlap[1]);
		conv->_overlap[0] = 0;
	}
	if (conv->_inputBuffer[0])
	{
		free(conv->_inputBuffer[0]);
		free(conv->_inputBuffer[1]);
		conv->_inputBuffer[0] = 0;
		conv->_inputBuffer[1] = 0;
	}
	conv->_current = 0;
	conv->_inputBufferFill = 0;
}
void FFTConvolver1x2Free(FFTConvolver1x2 *conv)
{
	if (conv->_segmentsRe)
	{
		for (unsigned int i = 0; i < conv->_segCount; ++i)
		{
			free(conv->_segmentsRe[i]);
			free(conv->_segmentsIm[i]);
			free(conv->_segmentsLLIRRe[i]);
			free(conv->_segmentsLLIRIm[i]);
			free(conv->_segmentsRRIRRe[i]);
			free(conv->_segmentsRRIRIm[i]);
		}
		free(conv->_segmentsRe);
		free(conv->_segmentsIm);
		free(conv->_segmentsLLIRRe);
		free(conv->_segmentsLLIRIm);
		free(conv->_segmentsRRIRRe);
		free(conv->_segmentsRRIRIm);
		conv->_segmentsRe = 0;
		conv->_segmentsIm = 0;
		conv->_segmentsLLIRRe = 0;
		conv->_segmentsLLIRIm = 0;
		conv->_segmentsRRIRRe = 0;
		conv->_segmentsRRIRIm = 0;
	}
	if (conv->_preMultiplied[0][0])
	{
		free(conv->_preMultiplied[0][0]);
		free(conv->_preMultiplied[0][1]);
		free(conv->_preMultiplied[1][0]);
		free(conv->_preMultiplied[1][1]);
		conv->_preMultiplied[0][0] = 0;
	}
	if (conv->bit)
	{
		free(conv->bit);
		free(conv->sine);
		conv->bit = 0;
	}
	conv->_blockSize = 0;
	conv->_segSize = 0;
	conv->_segCount = 0;
	conv->_fftComplexSize = 0;
	if (conv->_fftBuffer[0])
	{
		free(conv->_fftBuffer[0]);
		free(conv->_fftBuffer[1]);
		conv->_fftBuffer[0] = 0;
	}
	if (conv->_overlap[0])
	{
		free(conv->_overlap[0]);
		free(conv->_overlap[1]);
		conv->_overlap[0] = 0;
	}
	if (conv->_inputBuffer)
	{
		free(conv->_inputBuffer);
		conv->_inputBuffer = 0;
	}
	conv->_current = 0;
	conv->_inputBufferFill = 0;
}
extern void fhtbitReversalTbl(unsigned *dst, unsigned int n);
extern void fhtsinHalfTblFloat(float *dst, unsigned int n);
int FFTConvolver1x1LoadImpulseResponse(FFTConvolver1x1 *conv, unsigned int blockSize, const float* ir, unsigned int irLen)
{
	if (blockSize == 0)
		return 0;
	if (irLen == 0)
		return 0;

	if (conv->bit)
		FFTConvolver1x1Free(conv);
	conv->_blockSize = upper_power_of_two(blockSize);
	conv->_segSize = 2 * conv->_blockSize;
	conv->_segCount = (unsigned int)ceil((double)irLen / (double)conv->_blockSize);
	conv->_segCountMinus1 = conv->_segCount - 1;
	conv->_fftComplexSize = (conv->_segSize >> 1) + 1;

	// FFT
	if (conv->_segSize == 2)
		conv->fft = DFT2;
	else if (conv->_segSize == 4)
		conv->fft = DFT4;
	else if (conv->_segSize == 8)
		conv->fft = DFT8;
	else if (conv->_segSize == 16)
		conv->fft = DFT16;
	else if (conv->_segSize == 32)
		conv->fft = DFT32;
	else if (conv->_segSize == 64)
		conv->fft = DFT64;
	else if (conv->_segSize == 128)
		conv->fft = DFT128;
	else if (conv->_segSize == 256)
		conv->fft = DFT256;
	else if (conv->_segSize == 512)
		conv->fft = DFT512;
	else if (conv->_segSize == 1024)
		conv->fft = DFT1024;
	else if (conv->_segSize == 2048)
		conv->fft = DFT2048;
	else if (conv->_segSize == 4096)
		conv->fft = DFT4096;
	else if (conv->_segSize == 8192)
		conv->fft = DFT8192;
	else if (conv->_segSize == 16384)
		conv->fft = DFT16384;
	else if (conv->_segSize == 32768)
		conv->fft = DFT32768;
	else if (conv->_segSize == 65536)
		conv->fft = DFT65536;
	else if (conv->_segSize == 131072)
		conv->fft = DFT131072;
	else if (conv->_segSize == 262144)
		conv->fft = DFT262144;
	else if (conv->_segSize == 524288)
		conv->fft = DFT524288;
	else if (conv->_segSize == 1048576)
		conv->fft = DFT1048576;
	conv->bit = (unsigned int*)malloc(conv->_segSize * sizeof(unsigned int));
	conv->sine = (float*)malloc(conv->_segSize * sizeof(float));
	fhtbitReversalTbl(conv->bit, conv->_segSize);
	fhtsinHalfTblFloat(conv->sine, conv->_segSize);
	conv->_fftBuffer = (float*)malloc(conv->_segSize * sizeof(float));

	// Prepare segments
	conv->_segmentsRe = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsIm = (float**)malloc(conv->_segCount * sizeof(float*));
	for (unsigned int i = 0; i < conv->_segCount; ++i)
	{
		conv->_segmentsRe[i] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		conv->_segmentsIm[i] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		memset(conv->_segmentsRe[i], 0, conv->_fftComplexSize * sizeof(float));
		memset(conv->_segmentsIm[i], 0, conv->_fftComplexSize * sizeof(float));
	}

	// Prepare IR
	conv->_segmentsIRRe = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsIRIm = (float**)malloc(conv->_segCount * sizeof(float*));
	for (unsigned int i = 0; i < conv->_segCount; ++i)
	{
		float* segmentRe = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		float* segmentIm = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		const unsigned int remaining = irLen - (i * conv->_blockSize);
		const unsigned int sizeCopy = (remaining >= conv->_blockSize) ? conv->_blockSize : remaining;
		for (unsigned int j = 0; j < sizeCopy; j++)
			conv->_fftBuffer[conv->bit[j]] = ir[i*conv->_blockSize + j];
		for (unsigned int j = sizeCopy; j < conv->_segSize; j++)
			conv->_fftBuffer[conv->bit[j]] = 0.0f;
		conv->fft(conv->_fftBuffer, conv->sine);
		segmentRe[0] = conv->_fftBuffer[0] * 2.0f;
		segmentIm[0] = 0.0f;
		for (unsigned int j = 1; j < conv->_fftComplexSize; j++)
		{
			unsigned int symIdx = conv->_segSize - j;
			segmentRe[j] = conv->_fftBuffer[j] + conv->_fftBuffer[symIdx];
			segmentIm[j] = conv->_fftBuffer[j] - conv->_fftBuffer[symIdx];
		}
		conv->_segmentsIRRe[i] = segmentRe;
		conv->_segmentsIRIm[i] = segmentIm;
	}

	// Prepare convolution buffers
	conv->_preMultiplied[0] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
	conv->_preMultiplied[1] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
	memset(conv->_preMultiplied[0], 0, conv->_fftComplexSize * sizeof(float));
	memset(conv->_preMultiplied[1], 0, conv->_fftComplexSize * sizeof(float));
	conv->_overlap = (float*)malloc(conv->_blockSize * sizeof(float));
	memset(conv->_overlap, 0, conv->_blockSize * sizeof(float));

	// Prepare input buffer
	conv->_inputBuffer = (float*)malloc(conv->_blockSize * sizeof(float));
	memset(conv->_inputBuffer, 0, conv->_blockSize * sizeof(float));
	conv->_inputBufferFill = 0;

	// Reset current position
	conv->_current = 0;
	conv->gain = 1.0f / ((float)conv->_segSize * 2.0f);
	return 1;
}
int FFTConvolver1x1RefreshImpulseResponse(FFTConvolver1x1 *conv, unsigned int blockSize, const float* ir, unsigned int irLen)
{
	if (blockSize == 0)
		return 0;
	if (irLen == 0)
		return 0;
	// Prepare IR
	for (unsigned int i = 0; i < conv->_segCount; ++i)
	{
		float* segmentRe = conv->_segmentsIRRe[i];
		float* segmentIm = conv->_segmentsIRIm[i];
		const unsigned int remaining = irLen - (i * conv->_blockSize);
		const unsigned int sizeCopy = (remaining >= conv->_blockSize) ? conv->_blockSize : remaining;
		for (unsigned int j = 0; j < sizeCopy; j++)
			conv->_fftBuffer[conv->bit[j]] = ir[i*conv->_blockSize + j];
		for (unsigned int j = sizeCopy; j < conv->_segSize; j++)
			conv->_fftBuffer[conv->bit[j]] = 0.0f;
		conv->fft(conv->_fftBuffer, conv->sine);
		segmentRe[0] = conv->_fftBuffer[0] * 2.0f;
		segmentIm[0] = 0.0f;
		for (unsigned int j = 1; j < conv->_fftComplexSize; j++)
		{
			unsigned int symIdx = conv->_segSize - j;
			segmentRe[j] = conv->_fftBuffer[j] + conv->_fftBuffer[symIdx];
			segmentIm[j] = conv->_fftBuffer[j] - conv->_fftBuffer[symIdx];
		}
	}
	return 1;
}
int FFTConvolver2x4x2LoadImpulseResponse(FFTConvolver2x4x2 *conv, unsigned int blockSize, const float* irLL, const float* irLR, const float* irRL, const float* irRR, unsigned int irLen)
{
	if (blockSize == 0)
		return 0;
	if (irLen == 0)
		return 0;

	if (conv->bit)
		FFTConvolver2x4x2Free(conv);

	conv->_blockSize = upper_power_of_two(blockSize);
	conv->_segSize = 2 * conv->_blockSize;
	conv->_segCount = (unsigned int)ceil((double)irLen / (double)conv->_blockSize);
	conv->_segCountMinus1 = conv->_segCount - 1;
	conv->_fftComplexSize = (conv->_segSize >> 1) + 1;

	// FFT
	if (conv->_segSize == 2)
		conv->fft = DFT2;
	else if (conv->_segSize == 4)
		conv->fft = DFT4;
	else if (conv->_segSize == 8)
		conv->fft = DFT8;
	else if (conv->_segSize == 16)
		conv->fft = DFT16;
	else if (conv->_segSize == 32)
		conv->fft = DFT32;
	else if (conv->_segSize == 64)
		conv->fft = DFT64;
	else if (conv->_segSize == 128)
		conv->fft = DFT128;
	else if (conv->_segSize == 256)
		conv->fft = DFT256;
	else if (conv->_segSize == 512)
		conv->fft = DFT512;
	else if (conv->_segSize == 1024)
		conv->fft = DFT1024;
	else if (conv->_segSize == 2048)
		conv->fft = DFT2048;
	else if (conv->_segSize == 4096)
		conv->fft = DFT4096;
	else if (conv->_segSize == 8192)
		conv->fft = DFT8192;
	else if (conv->_segSize == 16384)
		conv->fft = DFT16384;
	else if (conv->_segSize == 32768)
		conv->fft = DFT32768;
	else if (conv->_segSize == 65536)
		conv->fft = DFT65536;
	else if (conv->_segSize == 131072)
		conv->fft = DFT131072;
	else if (conv->_segSize == 262144)
		conv->fft = DFT262144;
	else if (conv->_segSize == 524288)
		conv->fft = DFT524288;
	else if (conv->_segSize == 1048576)
		conv->fft = DFT1048576;
	conv->bit = (unsigned int*)malloc(conv->_segSize * sizeof(unsigned int));
	conv->sine = (float*)malloc(conv->_segSize * sizeof(float));
	fhtbitReversalTbl(conv->bit, conv->_segSize);
	fhtsinHalfTblFloat(conv->sine, conv->_segSize);
	conv->_fftBuffer[0] = (float*)malloc(conv->_segSize * sizeof(float));
	conv->_fftBuffer[1] = (float*)malloc(conv->_segSize * sizeof(float));

	// Prepare segments
	conv->_segmentsReLeft = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsImLeft = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsReRight = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsImRight = (float**)malloc(conv->_segCount * sizeof(float*));
	for (unsigned int i = 0; i < conv->_segCount; ++i)
	{
		conv->_segmentsReLeft[i] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		conv->_segmentsImLeft[i] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		memset(conv->_segmentsReLeft[i], 0, conv->_fftComplexSize * sizeof(float));
		memset(conv->_segmentsImLeft[i], 0, conv->_fftComplexSize * sizeof(float));
		conv->_segmentsReRight[i] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		conv->_segmentsImRight[i] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		memset(conv->_segmentsReRight[i], 0, conv->_fftComplexSize * sizeof(float));
		memset(conv->_segmentsImRight[i], 0, conv->_fftComplexSize * sizeof(float));
	}

	// Prepare IR
	conv->_segmentsLLIRRe = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsLLIRIm = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsLRIRRe = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsLRIRIm = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsRLIRRe = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsRLIRIm = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsRRIRRe = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsRRIRIm = (float**)malloc(conv->_segCount * sizeof(float*));
	for (unsigned int i = 0; i < conv->_segCount; ++i)
	{
		unsigned int j, symIdx;
		float* segmentLLRe = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		float* segmentLLIm = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		unsigned int remaining = irLen - (i * conv->_blockSize);
		unsigned int sizeCopy = (remaining >= conv->_blockSize) ? conv->_blockSize : remaining;
		for (j = 0; j < sizeCopy; j++)
			conv->_fftBuffer[0][conv->bit[j]] = irLL[i*conv->_blockSize + j];
		for (j = sizeCopy; j < conv->_segSize; j++)
			conv->_fftBuffer[0][conv->bit[j]] = 0.0f;
		conv->fft(conv->_fftBuffer[0], conv->sine);
		segmentLLRe[0] = conv->_fftBuffer[0][0] * 2.0f;
		segmentLLIm[0] = 0.0f;
		for (j = 1; j < conv->_fftComplexSize; j++)
		{
			symIdx = conv->_segSize - j;
			segmentLLRe[j] = conv->_fftBuffer[0][j] + conv->_fftBuffer[0][symIdx];
			segmentLLIm[j] = conv->_fftBuffer[0][j] - conv->_fftBuffer[0][symIdx];
		}
		conv->_segmentsLLIRRe[i] = segmentLLRe;
		conv->_segmentsLLIRIm[i] = segmentLLIm;
		//
		float* segmentLRRe = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		float* segmentLRIm = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		remaining = irLen - (i * conv->_blockSize);
		sizeCopy = (remaining >= conv->_blockSize) ? conv->_blockSize : remaining;
		for (j = 0; j < sizeCopy; j++)
			conv->_fftBuffer[0][conv->bit[j]] = irLR[i*conv->_blockSize + j];
		for (j = sizeCopy; j < conv->_segSize; j++)
			conv->_fftBuffer[0][conv->bit[j]] = 0.0f;
		conv->fft(conv->_fftBuffer[0], conv->sine);
		segmentLRRe[0] = conv->_fftBuffer[0][0] * 2.0f;
		segmentLRIm[0] = 0.0f;
		for (j = 1; j < conv->_fftComplexSize; j++)
		{
			symIdx = conv->_segSize - j;
			segmentLRRe[j] = conv->_fftBuffer[0][j] + conv->_fftBuffer[0][symIdx];
			segmentLRIm[j] = conv->_fftBuffer[0][j] - conv->_fftBuffer[0][symIdx];
		}
		conv->_segmentsLRIRRe[i] = segmentLRRe;
		conv->_segmentsLRIRIm[i] = segmentLRIm;
		//
		float* segmentRLRe = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		float* segmentRLIm = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		remaining = irLen - (i * conv->_blockSize);
		sizeCopy = (remaining >= conv->_blockSize) ? conv->_blockSize : remaining;
		for (j = 0; j < sizeCopy; j++)
			conv->_fftBuffer[0][conv->bit[j]] = irRL[i*conv->_blockSize + j];
		for (j = sizeCopy; j < conv->_segSize; j++)
			conv->_fftBuffer[0][conv->bit[j]] = 0.0f;
		conv->fft(conv->_fftBuffer[0], conv->sine);
		segmentRLRe[0] = conv->_fftBuffer[0][0] * 2.0f;
		segmentRLIm[0] = 0.0f;
		for (j = 1; j < conv->_fftComplexSize; j++)
		{
			symIdx = conv->_segSize - j;
			segmentRLRe[j] = conv->_fftBuffer[0][j] + conv->_fftBuffer[0][symIdx];
			segmentRLIm[j] = conv->_fftBuffer[0][j] - conv->_fftBuffer[0][symIdx];
		}
		conv->_segmentsRLIRRe[i] = segmentRLRe;
		conv->_segmentsRLIRIm[i] = segmentRLIm;
		//
		float* segmentRRRe = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		float* segmentRRIm = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		remaining = irLen - (i * conv->_blockSize);
		sizeCopy = (remaining >= conv->_blockSize) ? conv->_blockSize : remaining;
		for (j = 0; j < sizeCopy; j++)
			conv->_fftBuffer[0][conv->bit[j]] = irRR[i*conv->_blockSize + j];
		for (j = sizeCopy; j < conv->_segSize; j++)
			conv->_fftBuffer[0][conv->bit[j]] = 0.0f;
		conv->fft(conv->_fftBuffer[0], conv->sine);
		segmentRRRe[0] = conv->_fftBuffer[0][0] * 2.0f;
		segmentRRIm[0] = 0.0f;
		for (j = 1; j < conv->_fftComplexSize; j++)
		{
			symIdx = conv->_segSize - j;
			segmentRRRe[j] = conv->_fftBuffer[0][j] + conv->_fftBuffer[0][symIdx];
			segmentRRIm[j] = conv->_fftBuffer[0][j] - conv->_fftBuffer[0][symIdx];
		}
		conv->_segmentsRRIRRe[i] = segmentRRRe;
		conv->_segmentsRRIRIm[i] = segmentRRIm;
	}

	// Prepare convolution buffers
	conv->_preMultiplied[0][0] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
	conv->_preMultiplied[0][1] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
	conv->_preMultiplied[1][0] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
	conv->_preMultiplied[1][1] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
	memset(conv->_preMultiplied[0][0], 0, conv->_fftComplexSize * sizeof(float));
	memset(conv->_preMultiplied[0][1], 0, conv->_fftComplexSize * sizeof(float));
	memset(conv->_preMultiplied[1][0], 0, conv->_fftComplexSize * sizeof(float));
	memset(conv->_preMultiplied[1][1], 0, conv->_fftComplexSize * sizeof(float));
	conv->_overlap[0] = (float*)malloc(conv->_blockSize * sizeof(float));
	memset(conv->_overlap[0], 0, conv->_blockSize * sizeof(float));
	conv->_overlap[1] = (float*)malloc(conv->_blockSize * sizeof(float));
	memset(conv->_overlap[1], 0, conv->_blockSize * sizeof(float));

	// Prepare input buffer
	conv->_inputBuffer[0] = (float*)malloc(conv->_blockSize * sizeof(float));
	memset(conv->_inputBuffer[0], 0, conv->_blockSize * sizeof(float));
	conv->_inputBuffer[1] = (float*)malloc(conv->_blockSize * sizeof(float));
	memset(conv->_inputBuffer[1], 0, conv->_blockSize * sizeof(float));
	conv->_inputBufferFill = 0;

	// Reset current position
	conv->_current = 0;
	conv->gain = 1.0f / ((float)conv->_segSize * 2.0f);
	return 1;
}
int FFTConvolver2x2LoadImpulseResponse(FFTConvolver2x2 *conv, unsigned int blockSize, const float* irL, const float* irR, unsigned int irLen)
{
	if (blockSize == 0)
		return 0;
	if (irLen == 0)
		return 0;

	if (conv->bit)
		FFTConvolver2x2Free(conv);

	conv->_blockSize = upper_power_of_two(blockSize);
	conv->_segSize = 2 * conv->_blockSize;
	conv->_segCount = (unsigned int)ceil((double)irLen / (double)conv->_blockSize);
	conv->_segCountMinus1 = conv->_segCount - 1;
	conv->_fftComplexSize = (conv->_segSize >> 1) + 1;

	// FFT
	if (conv->_segSize == 2)
		conv->fft = DFT2;
	else if (conv->_segSize == 4)
		conv->fft = DFT4;
	else if (conv->_segSize == 8)
		conv->fft = DFT8;
	else if (conv->_segSize == 16)
		conv->fft = DFT16;
	else if (conv->_segSize == 32)
		conv->fft = DFT32;
	else if (conv->_segSize == 64)
		conv->fft = DFT64;
	else if (conv->_segSize == 128)
		conv->fft = DFT128;
	else if (conv->_segSize == 256)
		conv->fft = DFT256;
	else if (conv->_segSize == 512)
		conv->fft = DFT512;
	else if (conv->_segSize == 1024)
		conv->fft = DFT1024;
	else if (conv->_segSize == 2048)
		conv->fft = DFT2048;
	else if (conv->_segSize == 4096)
		conv->fft = DFT4096;
	else if (conv->_segSize == 8192)
		conv->fft = DFT8192;
	else if (conv->_segSize == 16384)
		conv->fft = DFT16384;
	else if (conv->_segSize == 32768)
		conv->fft = DFT32768;
	else if (conv->_segSize == 65536)
		conv->fft = DFT65536;
	else if (conv->_segSize == 131072)
		conv->fft = DFT131072;
	else if (conv->_segSize == 262144)
		conv->fft = DFT262144;
	else if (conv->_segSize == 524288)
		conv->fft = DFT524288;
	else if (conv->_segSize == 1048576)
		conv->fft = DFT1048576;
	conv->bit = (unsigned int*)malloc(conv->_segSize * sizeof(unsigned int));
	conv->sine = (float*)malloc(conv->_segSize * sizeof(float));
	fhtbitReversalTbl(conv->bit, conv->_segSize);
	fhtsinHalfTblFloat(conv->sine, conv->_segSize);
	conv->_fftBuffer[0] = (float*)malloc(conv->_segSize * sizeof(float));
	conv->_fftBuffer[1] = (float*)malloc(conv->_segSize * sizeof(float));

	// Prepare segments
	conv->_segmentsReLeft = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsImLeft = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsReRight = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsImRight = (float**)malloc(conv->_segCount * sizeof(float*));
	for (unsigned int i = 0; i < conv->_segCount; ++i)
	{
		conv->_segmentsReLeft[i] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		conv->_segmentsImLeft[i] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		memset(conv->_segmentsReLeft[i], 0, conv->_fftComplexSize * sizeof(float));
		memset(conv->_segmentsImLeft[i], 0, conv->_fftComplexSize * sizeof(float));
		conv->_segmentsReRight[i] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		conv->_segmentsImRight[i] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		memset(conv->_segmentsReRight[i], 0, conv->_fftComplexSize * sizeof(float));
		memset(conv->_segmentsImRight[i], 0, conv->_fftComplexSize * sizeof(float));
	}

	// Prepare IR
	conv->_segmentsLLIRRe = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsLLIRIm = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsRRIRRe = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsRRIRIm = (float**)malloc(conv->_segCount * sizeof(float*));
	for (unsigned int i = 0; i < conv->_segCount; ++i)
	{
		unsigned int j, symIdx;
		float* segmentLLRe = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		float* segmentLLIm = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		unsigned int remaining = irLen - (i * conv->_blockSize);
		unsigned int sizeCopy = (remaining >= conv->_blockSize) ? conv->_blockSize : remaining;
		for (j = 0; j < sizeCopy; j++)
			conv->_fftBuffer[0][conv->bit[j]] = irL[i*conv->_blockSize + j];
		for (j = sizeCopy; j < conv->_segSize; j++)
			conv->_fftBuffer[0][conv->bit[j]] = 0.0f;
		conv->fft(conv->_fftBuffer[0], conv->sine);
		segmentLLRe[0] = conv->_fftBuffer[0][0] * 2.0f;
		segmentLLIm[0] = 0.0f;
		for (j = 1; j < conv->_fftComplexSize; j++)
		{
			symIdx = conv->_segSize - j;
			segmentLLRe[j] = conv->_fftBuffer[0][j] + conv->_fftBuffer[0][symIdx];
			segmentLLIm[j] = conv->_fftBuffer[0][j] - conv->_fftBuffer[0][symIdx];
		}
		conv->_segmentsLLIRRe[i] = segmentLLRe;
		conv->_segmentsLLIRIm[i] = segmentLLIm;
		//
		float* segmentRRRe = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		float* segmentRRIm = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		remaining = irLen - (i * conv->_blockSize);
		sizeCopy = (remaining >= conv->_blockSize) ? conv->_blockSize : remaining;
		for (j = 0; j < sizeCopy; j++)
			conv->_fftBuffer[0][conv->bit[j]] = irR[i*conv->_blockSize + j];
		for (j = sizeCopy; j < conv->_segSize; j++)
			conv->_fftBuffer[0][conv->bit[j]] = 0.0f;
		conv->fft(conv->_fftBuffer[0], conv->sine);
		segmentRRRe[0] = conv->_fftBuffer[0][0] * 2.0f;
		segmentRRIm[0] = 0.0f;
		for (j = 1; j < conv->_fftComplexSize; j++)
		{
			symIdx = conv->_segSize - j;
			segmentRRRe[j] = conv->_fftBuffer[0][j] + conv->_fftBuffer[0][symIdx];
			segmentRRIm[j] = conv->_fftBuffer[0][j] - conv->_fftBuffer[0][symIdx];
		}
		conv->_segmentsRRIRRe[i] = segmentRRRe;
		conv->_segmentsRRIRIm[i] = segmentRRIm;
	}

	// Prepare convolution buffers
	conv->_preMultiplied[0][0] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
	conv->_preMultiplied[0][1] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
	conv->_preMultiplied[1][0] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
	conv->_preMultiplied[1][1] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
	memset(conv->_preMultiplied[0][0], 0, conv->_fftComplexSize * sizeof(float));
	memset(conv->_preMultiplied[0][1], 0, conv->_fftComplexSize * sizeof(float));
	memset(conv->_preMultiplied[1][0], 0, conv->_fftComplexSize * sizeof(float));
	memset(conv->_preMultiplied[1][1], 0, conv->_fftComplexSize * sizeof(float));
	conv->_overlap[0] = (float*)malloc(conv->_blockSize * sizeof(float));
	memset(conv->_overlap[0], 0, conv->_blockSize * sizeof(float));
	conv->_overlap[1] = (float*)malloc(conv->_blockSize * sizeof(float));
	memset(conv->_overlap[1], 0, conv->_blockSize * sizeof(float));

	// Prepare input buffer
	conv->_inputBuffer[0] = (float*)malloc(conv->_blockSize * sizeof(float));
	memset(conv->_inputBuffer[0], 0, conv->_blockSize * sizeof(float));
	conv->_inputBuffer[1] = (float*)malloc(conv->_blockSize * sizeof(float));
	memset(conv->_inputBuffer[1], 0, conv->_blockSize * sizeof(float));
	conv->_inputBufferFill = 0;

	// Reset current position
	conv->_current = 0;
	conv->gain = 1.0f / ((float)conv->_segSize * 2.0f);
	return 1;
}
int FFTConvolver2x2RefreshImpulseResponse(FFTConvolver2x2 *conv, unsigned int blockSize, const float* irL, const float* irR, unsigned int irLen)
{
	if (blockSize == 0)
		return 0;
	if (irLen == 0)
		return 0;
	for (unsigned int i = 0; i < conv->_segCount; ++i)
	{
		unsigned int j, symIdx;
		float* segmentLLRe = conv->_segmentsLLIRRe[i];
		float* segmentLLIm = conv->_segmentsLLIRIm[i];
		unsigned int remaining = irLen - (i * conv->_blockSize);
		unsigned int sizeCopy = (remaining >= conv->_blockSize) ? conv->_blockSize : remaining;
		for (j = 0; j < sizeCopy; j++)
			conv->_fftBuffer[0][conv->bit[j]] = irL[i*conv->_blockSize + j];
		for (j = sizeCopy; j < conv->_segSize; j++)
			conv->_fftBuffer[0][conv->bit[j]] = 0.0f;
		conv->fft(conv->_fftBuffer[0], conv->sine);
		segmentLLRe[0] = conv->_fftBuffer[0][0] * 2.0f;
		segmentLLIm[0] = 0.0f;
		for (j = 1; j < conv->_fftComplexSize; j++)
		{
			symIdx = conv->_segSize - j;
			segmentLLRe[j] = conv->_fftBuffer[0][j] + conv->_fftBuffer[0][symIdx];
			segmentLLIm[j] = conv->_fftBuffer[0][j] - conv->_fftBuffer[0][symIdx];
		}
		//
		float* segmentRRRe = conv->_segmentsRRIRRe[i];
		float* segmentRRIm = conv->_segmentsRRIRIm[i];
		remaining = irLen - (i * conv->_blockSize);
		sizeCopy = (remaining >= conv->_blockSize) ? conv->_blockSize : remaining;
		for (j = 0; j < sizeCopy; j++)
			conv->_fftBuffer[0][conv->bit[j]] = irR[i*conv->_blockSize + j];
		for (j = sizeCopy; j < conv->_segSize; j++)
			conv->_fftBuffer[0][conv->bit[j]] = 0.0f;
		conv->fft(conv->_fftBuffer[0], conv->sine);
		segmentRRRe[0] = conv->_fftBuffer[0][0] * 2.0f;
		segmentRRIm[0] = 0.0f;
		for (j = 1; j < conv->_fftComplexSize; j++)
		{
			symIdx = conv->_segSize - j;
			segmentRRRe[j] = conv->_fftBuffer[0][j] + conv->_fftBuffer[0][symIdx];
			segmentRRIm[j] = conv->_fftBuffer[0][j] - conv->_fftBuffer[0][symIdx];
		}
	}
	return 1;
}
int FFTConvolver1x2LoadImpulseResponse(FFTConvolver1x2 *conv, unsigned int blockSize, const float* irL, const float* irR, unsigned int irLen)
{
	if (blockSize == 0)
		return 0;
	if (irLen == 0)
		return 0;

	if (conv->bit)
		FFTConvolver1x2Free(conv);

	conv->_blockSize = upper_power_of_two(blockSize);
	conv->_segSize = 2 * conv->_blockSize;
	conv->_segCount = (unsigned int)ceil((double)irLen / (double)conv->_blockSize);
	conv->_segCountMinus1 = conv->_segCount - 1;
	conv->_fftComplexSize = (conv->_segSize >> 1) + 1;

	// FFT
	if (conv->_segSize == 2)
		conv->fft = DFT2;
	else if (conv->_segSize == 4)
		conv->fft = DFT4;
	else if (conv->_segSize == 8)
		conv->fft = DFT8;
	else if (conv->_segSize == 16)
		conv->fft = DFT16;
	else if (conv->_segSize == 32)
		conv->fft = DFT32;
	else if (conv->_segSize == 64)
		conv->fft = DFT64;
	else if (conv->_segSize == 128)
		conv->fft = DFT128;
	else if (conv->_segSize == 256)
		conv->fft = DFT256;
	else if (conv->_segSize == 512)
		conv->fft = DFT512;
	else if (conv->_segSize == 1024)
		conv->fft = DFT1024;
	else if (conv->_segSize == 2048)
		conv->fft = DFT2048;
	else if (conv->_segSize == 4096)
		conv->fft = DFT4096;
	else if (conv->_segSize == 8192)
		conv->fft = DFT8192;
	else if (conv->_segSize == 16384)
		conv->fft = DFT16384;
	else if (conv->_segSize == 32768)
		conv->fft = DFT32768;
	else if (conv->_segSize == 65536)
		conv->fft = DFT65536;
	else if (conv->_segSize == 131072)
		conv->fft = DFT131072;
	else if (conv->_segSize == 262144)
		conv->fft = DFT262144;
	else if (conv->_segSize == 524288)
		conv->fft = DFT524288;
	else if (conv->_segSize == 1048576)
		conv->fft = DFT1048576;
	conv->bit = (unsigned int*)malloc(conv->_segSize * sizeof(unsigned int));
	conv->sine = (float*)malloc(conv->_segSize * sizeof(float));
	fhtbitReversalTbl(conv->bit, conv->_segSize);
	fhtsinHalfTblFloat(conv->sine, conv->_segSize);
	conv->_fftBuffer[0] = (float*)malloc(conv->_segSize * sizeof(float));
	conv->_fftBuffer[1] = (float*)malloc(conv->_segSize * sizeof(float));

	// Prepare segments
	conv->_segmentsRe = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsIm = (float**)malloc(conv->_segCount * sizeof(float*));
	for (unsigned int i = 0; i < conv->_segCount; ++i)
	{
		conv->_segmentsRe[i] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		conv->_segmentsIm[i] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		memset(conv->_segmentsRe[i], 0, conv->_fftComplexSize * sizeof(float));
		memset(conv->_segmentsIm[i], 0, conv->_fftComplexSize * sizeof(float));
	}

	// Prepare IR
	conv->_segmentsLLIRRe = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsLLIRIm = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsRRIRRe = (float**)malloc(conv->_segCount * sizeof(float*));
	conv->_segmentsRRIRIm = (float**)malloc(conv->_segCount * sizeof(float*));
	for (unsigned int i = 0; i < conv->_segCount; ++i)
	{
		unsigned int j, symIdx;
		float* segmentLLRe = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		float* segmentLLIm = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		unsigned int remaining = irLen - (i * conv->_blockSize);
		unsigned int sizeCopy = (remaining >= conv->_blockSize) ? conv->_blockSize : remaining;
		for (j = 0; j < sizeCopy; j++)
			conv->_fftBuffer[0][conv->bit[j]] = irL[i*conv->_blockSize + j];
		for (j = sizeCopy; j < conv->_segSize; j++)
			conv->_fftBuffer[0][conv->bit[j]] = 0.0f;
		conv->fft(conv->_fftBuffer[0], conv->sine);
		segmentLLRe[0] = conv->_fftBuffer[0][0] * 2.0f;
		segmentLLIm[0] = 0.0f;
		for (j = 1; j < conv->_fftComplexSize; j++)
		{
			symIdx = conv->_segSize - j;
			segmentLLRe[j] = conv->_fftBuffer[0][j] + conv->_fftBuffer[0][symIdx];
			segmentLLIm[j] = conv->_fftBuffer[0][j] - conv->_fftBuffer[0][symIdx];
		}
		conv->_segmentsLLIRRe[i] = segmentLLRe;
		conv->_segmentsLLIRIm[i] = segmentLLIm;
		//
		float* segmentRRRe = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		float* segmentRRIm = (float*)malloc(conv->_fftComplexSize * sizeof(float));
		remaining = irLen - (i * conv->_blockSize);
		sizeCopy = (remaining >= conv->_blockSize) ? conv->_blockSize : remaining;
		for (j = 0; j < sizeCopy; j++)
			conv->_fftBuffer[0][conv->bit[j]] = irR[i*conv->_blockSize + j];
		for (j = sizeCopy; j < conv->_segSize; j++)
			conv->_fftBuffer[0][conv->bit[j]] = 0.0f;
		conv->fft(conv->_fftBuffer[0], conv->sine);
		segmentRRRe[0] = conv->_fftBuffer[0][0] * 2.0f;
		segmentRRIm[0] = 0.0f;
		for (j = 1; j < conv->_fftComplexSize; j++)
		{
			symIdx = conv->_segSize - j;
			segmentRRRe[j] = conv->_fftBuffer[0][j] + conv->_fftBuffer[0][symIdx];
			segmentRRIm[j] = conv->_fftBuffer[0][j] - conv->_fftBuffer[0][symIdx];
		}
		conv->_segmentsRRIRRe[i] = segmentRRRe;
		conv->_segmentsRRIRIm[i] = segmentRRIm;
	}

	// Prepare convolution buffers
	conv->_preMultiplied[0][0] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
	conv->_preMultiplied[0][1] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
	conv->_preMultiplied[1][0] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
	conv->_preMultiplied[1][1] = (float*)malloc(conv->_fftComplexSize * sizeof(float));
	memset(conv->_preMultiplied[0][0], 0, conv->_fftComplexSize * sizeof(float));
	memset(conv->_preMultiplied[0][1], 0, conv->_fftComplexSize * sizeof(float));
	memset(conv->_preMultiplied[1][0], 0, conv->_fftComplexSize * sizeof(float));
	memset(conv->_preMultiplied[1][1], 0, conv->_fftComplexSize * sizeof(float));
	conv->_overlap[0] = (float*)malloc(conv->_blockSize * sizeof(float));
	memset(conv->_overlap[0], 0, conv->_blockSize * sizeof(float));
	conv->_overlap[1] = (float*)malloc(conv->_blockSize * sizeof(float));
	memset(conv->_overlap[1], 0, conv->_blockSize * sizeof(float));

	// Prepare input buffer
	conv->_inputBuffer = (float*)malloc(conv->_blockSize * sizeof(float));
	memset(conv->_inputBuffer, 0, conv->_blockSize * sizeof(float));
	conv->_inputBufferFill = 0;

	// Reset current position
	conv->_current = 0;
	conv->gain = 1.0f / ((float)conv->_segSize * 2.0f);
	return 1;
}

#ifndef min
#define min(a,b) (((a)<(b))?(a):(b))
#endif
void FFTConvolver1x1Process(FFTConvolver1x1 *conv, const float* input, float* output, unsigned int len)
{
	unsigned int j, symIdx;
	unsigned int processed = 0;
	while (processed < len)
	{
		const int inputBufferWasEmpty = (conv->_inputBufferFill == 0);
		const unsigned int processing = min(len - processed, conv->_blockSize - conv->_inputBufferFill);
		const unsigned int inputBufferPos = conv->_inputBufferFill;
		memcpy(conv->_inputBuffer + inputBufferPos, input + processed, processing * sizeof(float));

		// Forward FFT
		for (j = 0; j < conv->_blockSize; j++)
			conv->_fftBuffer[conv->bit[j]] = conv->_inputBuffer[j];
		for (j = conv->_blockSize; j < conv->_segSize; j++)
			conv->_fftBuffer[conv->bit[j]] = 0.0f;
		conv->fft(conv->_fftBuffer, conv->sine);
		conv->_segmentsRe[conv->_current][0] = conv->_fftBuffer[0];
		for (j = 1; j < conv->_fftComplexSize; j++)
		{
			symIdx = conv->_segSize - j;
			conv->_segmentsRe[conv->_current][j] = conv->_fftBuffer[j] + conv->_fftBuffer[symIdx];
			conv->_segmentsIm[conv->_current][j] = conv->_fftBuffer[j] - conv->_fftBuffer[symIdx];
		}

		// Complex multiplication
		const float *reA;
		const float *imA;
		const float *reB;
		const float *imB;
		unsigned int end4 = conv->_fftComplexSize - 1;
		if (inputBufferWasEmpty)
		{
			unsigned int segFrameIndex = (conv->_current + 1) % conv->_segCount;
			if (conv->_segCount > 1)
			{
				float *re = conv->_preMultiplied[0];
				float *im = conv->_preMultiplied[1];
				reA = conv->_segmentsIRRe[1];
				imA = conv->_segmentsIRIm[1];
				reB = conv->_segmentsRe[segFrameIndex];
				imB = conv->_segmentsIm[segFrameIndex];
				for (j = 0; j < end4; j += 4)
				{
					re[j + 0] = reA[j + 0] * reB[j + 0] - imA[j + 0] * imB[j + 0];
					re[j + 1] = reA[j + 1] * reB[j + 1] - imA[j + 1] * imB[j + 1];
					re[j + 2] = reA[j + 2] * reB[j + 2] - imA[j + 2] * imB[j + 2];
					re[j + 3] = reA[j + 3] * reB[j + 3] - imA[j + 3] * imB[j + 3];
					im[j + 0] = reA[j + 0] * imB[j + 0] + imA[j + 0] * reB[j + 0];
					im[j + 1] = reA[j + 1] * imB[j + 1] + imA[j + 1] * reB[j + 1];
					im[j + 2] = reA[j + 2] * imB[j + 2] + imA[j + 2] * reB[j + 2];
					im[j + 3] = reA[j + 3] * imB[j + 3] + imA[j + 3] * reB[j + 3];
				}
				re[end4] = reA[end4] * reB[end4] - imA[end4] * imB[end4];
				im[end4] = reA[end4] * imB[end4] + imA[end4] * reB[end4];
				for (unsigned int i = 2; i < conv->_segCount; ++i)
				{
					segFrameIndex = (conv->_current + i) % conv->_segCount;
					re = conv->_preMultiplied[0];
					im = conv->_preMultiplied[1];
					reA = conv->_segmentsIRRe[i];
					imA = conv->_segmentsIRIm[i];
					reB = conv->_segmentsRe[segFrameIndex];
					imB = conv->_segmentsIm[segFrameIndex];
					for (j = 0; j < end4; j += 4)
					{
						re[j + 0] += reA[j + 0] * reB[j + 0] - imA[j + 0] * imB[j + 0];
						re[j + 1] += reA[j + 1] * reB[j + 1] - imA[j + 1] * imB[j + 1];
						re[j + 2] += reA[j + 2] * reB[j + 2] - imA[j + 2] * imB[j + 2];
						re[j + 3] += reA[j + 3] * reB[j + 3] - imA[j + 3] * imB[j + 3];
						im[j + 0] += reA[j + 0] * imB[j + 0] + imA[j + 0] * reB[j + 0];
						im[j + 1] += reA[j + 1] * imB[j + 1] + imA[j + 1] * reB[j + 1];
						im[j + 2] += reA[j + 2] * imB[j + 2] + imA[j + 2] * reB[j + 2];
						im[j + 3] += reA[j + 3] * imB[j + 3] + imA[j + 3] * reB[j + 3];
					}
					re[end4] += reA[end4] * reB[end4] - imA[end4] * imB[end4];
					im[end4] += reA[end4] * imB[end4] + imA[end4] * reB[end4];
				}
			}
		}
		reA = conv->_segmentsIRRe[0];
		imA = conv->_segmentsIRIm[0];
		reB = conv->_segmentsRe[conv->_current];
		imB = conv->_segmentsIm[conv->_current];
		const float *srcRe = conv->_preMultiplied[0];
		const float *srcIm = conv->_preMultiplied[1];
		float real, imag;
		conv->_fftBuffer[0] = reB[0] * reA[0] + srcRe[0];
		for (j = 1; j < conv->_fftComplexSize; ++j)
		{
			symIdx = conv->_segSize - j;
			real = reB[j] * reA[j] - imB[j] * imA[j] + srcRe[j];
			imag = reB[j] * imA[j] + imB[j] * reA[j] + srcIm[j];
			conv->_fftBuffer[conv->bit[j]] = (real + imag) * 0.5f;
			conv->_fftBuffer[conv->bit[symIdx]] = (real - imag) * 0.5f;
		}
		// Backward FFT
		conv->fft(conv->_fftBuffer, conv->sine);

		// Add overlap
		float *result = output + processed;
		const float *a = conv->_fftBuffer + inputBufferPos;
		const float *b = conv->_overlap + inputBufferPos;
		end4 = (processing >> 2) << 2;
		for (j = 0; j < end4; j += 4)
		{
			result[j + 0] = (a[j + 0] + b[j + 0]) * conv->gain;
			result[j + 1] = (a[j + 1] + b[j + 1]) * conv->gain;
			result[j + 2] = (a[j + 2] + b[j + 2]) * conv->gain;
			result[j + 3] = (a[j + 3] + b[j + 3]) * conv->gain;
		}
		for (j = end4; j < processing; ++j)
			result[j] = (a[j] + b[j]) * conv->gain;

		// Input buffer full => Next block
		conv->_inputBufferFill += processing;
		if (conv->_inputBufferFill == conv->_blockSize)
		{
			// Input buffer is empty again now
			memset(conv->_inputBuffer, 0, conv->_blockSize * sizeof(float));
			conv->_inputBufferFill = 0;
			// Save the overlap
			memcpy(conv->_overlap, conv->_fftBuffer + conv->_blockSize, conv->_blockSize * sizeof(float));
			// Update current segment
			conv->_current = (conv->_current > 0) ? (conv->_current - 1) : conv->_segCountMinus1;
		}
		processed += processing;
	}
}
void FFTConvolver2x4x2Process(FFTConvolver2x4x2 *conv, const float* x1, const float* x2, float* y1, float* y2, unsigned int len)
{
	unsigned int j, symIdx;
	unsigned int processed = 0;
	while (processed < len)
	{
		const int inputBufferWasEmpty = (conv->_inputBufferFill == 0);
		const unsigned int processing = min(len - processed, conv->_blockSize - conv->_inputBufferFill);
		const unsigned int inputBufferPos = conv->_inputBufferFill;
		memcpy(conv->_inputBuffer[0] + inputBufferPos, x1 + processed, processing * sizeof(float));
		memcpy(conv->_inputBuffer[1] + inputBufferPos, x2 + processed, processing * sizeof(float));

		// Forward FFT
		for (j = 0; j < conv->_blockSize; j++)
		{
			conv->_fftBuffer[0][conv->bit[j]] = conv->_inputBuffer[0][j];
			conv->_fftBuffer[1][conv->bit[j]] = conv->_inputBuffer[1][j];
		}
		for (j = conv->_blockSize; j < conv->_segSize; j++)
		{
			conv->_fftBuffer[0][conv->bit[j]] = 0.0f;
			conv->_fftBuffer[1][conv->bit[j]] = 0.0f;
		}
		conv->fft(conv->_fftBuffer[0], conv->sine);
		conv->_segmentsReLeft[conv->_current][0] = conv->_fftBuffer[0][0];
		conv->fft(conv->_fftBuffer[1], conv->sine);
		conv->_segmentsReRight[conv->_current][0] = conv->_fftBuffer[1][0];
		for (j = 1; j < conv->_fftComplexSize; j++)
		{
			symIdx = conv->_segSize - j;
			conv->_segmentsReLeft[conv->_current][j] = conv->_fftBuffer[0][j] + conv->_fftBuffer[0][symIdx];
			conv->_segmentsImLeft[conv->_current][j] = conv->_fftBuffer[0][j] - conv->_fftBuffer[0][symIdx];
			conv->_segmentsReRight[conv->_current][j] = conv->_fftBuffer[1][j] + conv->_fftBuffer[1][symIdx];
			conv->_segmentsImRight[conv->_current][j] = conv->_fftBuffer[1][j] - conv->_fftBuffer[1][symIdx];
		}

		// Complex multiplication
		const float *reALL, *imALL, *reALR, *imALR, *reARL, *imARL, *reARR, *imARR;
		const float *reBLeft;
		const float *imBLeft;
		const float *reBRight;
		const float *imBRight;
		unsigned int end4 = conv->_fftComplexSize - 1;
		if (inputBufferWasEmpty)
		{
			unsigned int segFrameIndex = (conv->_current + 1) % conv->_segCount;
			if (conv->_segCount > 1)
			{
				float *reLeft = conv->_preMultiplied[0][0];
				float *imLeft = conv->_preMultiplied[0][1];
				float *reRight = conv->_preMultiplied[1][0];
				float *imRight = conv->_preMultiplied[1][1];
				reALL = conv->_segmentsLLIRRe[1];
				imALL = conv->_segmentsLLIRIm[1];
				reALR = conv->_segmentsLRIRRe[1];
				imALR = conv->_segmentsLRIRIm[1];
				reARL = conv->_segmentsRLIRRe[1];
				imARL = conv->_segmentsRLIRIm[1];
				reARR = conv->_segmentsRRIRRe[1];
				imARR = conv->_segmentsRRIRIm[1];
				reBLeft = conv->_segmentsReLeft[segFrameIndex];
				imBLeft = conv->_segmentsImLeft[segFrameIndex];
				reBRight = conv->_segmentsReRight[segFrameIndex];
				imBRight = conv->_segmentsImRight[segFrameIndex];
				for (j = 0; j < end4; j += 4)
				{
					reLeft[j + 0] = (reARL[j + 0] * reBRight[j + 0] - imARL[j + 0] * imBRight[j + 0]) + (reALL[j + 0] * reBLeft[j + 0] - imALL[j + 0] * imBLeft[j + 0]);
					reLeft[j + 1] = (reARL[j + 1] * reBRight[j + 1] - imARL[j + 1] * imBRight[j + 1]) + (reALL[j + 1] * reBLeft[j + 1] - imALL[j + 1] * imBLeft[j + 1]);
					reLeft[j + 2] = (reARL[j + 2] * reBRight[j + 2] - imARL[j + 2] * imBRight[j + 2]) + (reALL[j + 2] * reBLeft[j + 2] - imALL[j + 2] * imBLeft[j + 2]);
					reLeft[j + 3] = (reARL[j + 3] * reBRight[j + 3] - imARL[j + 3] * imBRight[j + 3]) + (reALL[j + 3] * reBLeft[j + 3] - imALL[j + 3] * imBLeft[j + 3]);
					imLeft[j + 0] = (imARL[j + 0] * reBRight[j + 0] + reARL[j + 0] * imBRight[j + 0]) + (reALL[j + 0] * imBLeft[j + 0] + imALL[j + 0] * reBLeft[j + 0]);
					imLeft[j + 1] = (imARL[j + 1] * reBRight[j + 1] + reARL[j + 1] * imBRight[j + 1]) + (reALL[j + 1] * imBLeft[j + 1] + imALL[j + 1] * reBLeft[j + 1]);
					imLeft[j + 2] = (imARL[j + 2] * reBRight[j + 2] + reARL[j + 2] * imBRight[j + 2]) + (reALL[j + 2] * imBLeft[j + 2] + imALL[j + 2] * reBLeft[j + 2]);
					imLeft[j + 3] = (imARL[j + 3] * reBRight[j + 3] + reARL[j + 3] * imBRight[j + 3]) + (reALL[j + 3] * imBLeft[j + 3] + imALL[j + 3] * reBLeft[j + 3]);
					reRight[j + 0] = (reALR[j + 0] * reBLeft[j + 0] - imALR[j + 0] * imBLeft[j + 0]) + (reARR[j + 0] * reBRight[j + 0] - imARR[j + 0] * imBRight[j + 0]);
					reRight[j + 1] = (reALR[j + 1] * reBLeft[j + 1] - imALR[j + 1] * imBLeft[j + 1]) + (reARR[j + 1] * reBRight[j + 1] - imARR[j + 1] * imBRight[j + 1]);
					reRight[j + 2] = (reALR[j + 2] * reBLeft[j + 2] - imALR[j + 2] * imBLeft[j + 2]) + (reARR[j + 2] * reBRight[j + 2] - imARR[j + 2] * imBRight[j + 2]);
					reRight[j + 3] = (reALR[j + 3] * reBLeft[j + 3] - imALR[j + 3] * imBLeft[j + 3]) + (reARR[j + 3] * reBRight[j + 3] - imARR[j + 3] * imBRight[j + 3]);
					imRight[j + 0] = (imALR[j + 0] * reBLeft[j + 0] + reALR[j + 0] * imBLeft[j + 0]) + (reARR[j + 0] * imBRight[j + 0] + imARR[j + 0] * reBRight[j + 0]);
					imRight[j + 1] = (imALR[j + 1] * reBLeft[j + 1] + reALR[j + 1] * imBLeft[j + 1]) + (reARR[j + 1] * imBRight[j + 1] + imARR[j + 1] * reBRight[j + 1]);
					imRight[j + 2] = (imALR[j + 2] * reBLeft[j + 2] + reALR[j + 2] * imBLeft[j + 2]) + (reARR[j + 2] * imBRight[j + 2] + imARR[j + 2] * reBRight[j + 2]);
					imRight[j + 3] = (imALR[j + 3] * reBLeft[j + 3] + reALR[j + 3] * imBLeft[j + 3]) + (reARR[j + 3] * imBRight[j + 3] + imARR[j + 3] * reBRight[j + 3]);
				}
				reLeft[end4] = (reARL[end4] * reBRight[end4] - imARL[end4] * imBRight[end4]) + (reALL[end4] * reBLeft[end4] - imALL[end4] * imBLeft[end4]);
				imLeft[end4] = (imARL[end4] * reBRight[end4] + reARL[end4] * imBRight[end4]) + (reALL[end4] * imBLeft[end4] + imALL[end4] * reBLeft[end4]);
				reRight[end4] = (reALR[end4] * reBLeft[end4] - imALR[end4] * imBLeft[end4]) + (reARR[end4] * reBRight[end4] - imARR[end4] * imBRight[end4]);
				imRight[end4] = (imALR[end4] * reBLeft[end4] + reALR[end4] * imBLeft[end4]) + (reARR[end4] * imBRight[end4] + imARR[end4] * reBRight[end4]);
				for (unsigned int i = 2; i < conv->_segCount; ++i)
				{
					segFrameIndex = (conv->_current + i) % conv->_segCount;
					reLeft = conv->_preMultiplied[0][0];
					imLeft = conv->_preMultiplied[0][1];
					reRight = conv->_preMultiplied[1][0];
					imRight = conv->_preMultiplied[1][1];
					reALL = conv->_segmentsLLIRRe[i];
					imALL = conv->_segmentsLLIRIm[i];
					reALR = conv->_segmentsLRIRRe[i];
					imALR = conv->_segmentsLRIRIm[i];
					reARL = conv->_segmentsRLIRRe[i];
					imARL = conv->_segmentsRLIRIm[i];
					reARR = conv->_segmentsRRIRRe[i];
					imARR = conv->_segmentsRRIRIm[i];
					reBLeft = conv->_segmentsReLeft[segFrameIndex];
					imBLeft = conv->_segmentsImLeft[segFrameIndex];
					reBRight = conv->_segmentsReRight[segFrameIndex];
					imBRight = conv->_segmentsImRight[segFrameIndex];
					for (j = 0; j < end4; j += 4)
					{
						reLeft[j + 0] += (reARL[j + 0] * reBRight[j + 0] - imARL[j + 0] * imBRight[j + 0]) + (reALL[j + 0] * reBLeft[j + 0] - imALL[j + 0] * imBLeft[j + 0]);
						reLeft[j + 1] += (reARL[j + 1] * reBRight[j + 1] - imARL[j + 1] * imBRight[j + 1]) + (reALL[j + 1] * reBLeft[j + 1] - imALL[j + 1] * imBLeft[j + 1]);
						reLeft[j + 2] += (reARL[j + 2] * reBRight[j + 2] - imARL[j + 2] * imBRight[j + 2]) + (reALL[j + 2] * reBLeft[j + 2] - imALL[j + 2] * imBLeft[j + 2]);
						reLeft[j + 3] += (reARL[j + 3] * reBRight[j + 3] - imARL[j + 3] * imBRight[j + 3]) + (reALL[j + 3] * reBLeft[j + 3] - imALL[j + 3] * imBLeft[j + 3]);
						imLeft[j + 0] += (imARL[j + 0] * reBRight[j + 0] + reARL[j + 0] * imBRight[j + 0]) + (reALL[j + 0] * imBLeft[j + 0] + imALL[j + 0] * reBLeft[j + 0]);
						imLeft[j + 1] += (imARL[j + 1] * reBRight[j + 1] + reARL[j + 1] * imBRight[j + 1]) + (reALL[j + 1] * imBLeft[j + 1] + imALL[j + 1] * reBLeft[j + 1]);
						imLeft[j + 2] += (imARL[j + 2] * reBRight[j + 2] + reARL[j + 2] * imBRight[j + 2]) + (reALL[j + 2] * imBLeft[j + 2] + imALL[j + 2] * reBLeft[j + 2]);
						imLeft[j + 3] += (imARL[j + 3] * reBRight[j + 3] + reARL[j + 3] * imBRight[j + 3]) + (reALL[j + 3] * imBLeft[j + 3] + imALL[j + 3] * reBLeft[j + 3]);
						reRight[j + 0] += (reALR[j + 0] * reBLeft[j + 0] - imALR[j + 0] * imBLeft[j + 0]) + (reARR[j + 0] * reBRight[j + 0] - imARR[j + 0] * imBRight[j + 0]);
						reRight[j + 1] += (reALR[j + 1] * reBLeft[j + 1] - imALR[j + 1] * imBLeft[j + 1]) + (reARR[j + 1] * reBRight[j + 1] - imARR[j + 1] * imBRight[j + 1]);
						reRight[j + 2] += (reALR[j + 2] * reBLeft[j + 2] - imALR[j + 2] * imBLeft[j + 2]) + (reARR[j + 2] * reBRight[j + 2] - imARR[j + 2] * imBRight[j + 2]);
						reRight[j + 3] += (reALR[j + 3] * reBLeft[j + 3] - imALR[j + 3] * imBLeft[j + 3]) + (reARR[j + 3] * reBRight[j + 3] - imARR[j + 3] * imBRight[j + 3]);
						imRight[j + 0] += (imALR[j + 0] * reBLeft[j + 0] + reALR[j + 0] * imBLeft[j + 0]) + (reARR[j + 0] * imBRight[j + 0] + imARR[j + 0] * reBRight[j + 0]);
						imRight[j + 1] += (imALR[j + 1] * reBLeft[j + 1] + reALR[j + 1] * imBLeft[j + 1]) + (reARR[j + 1] * imBRight[j + 1] + imARR[j + 1] * reBRight[j + 1]);
						imRight[j + 2] += (imALR[j + 2] * reBLeft[j + 2] + reALR[j + 2] * imBLeft[j + 2]) + (reARR[j + 2] * imBRight[j + 2] + imARR[j + 2] * reBRight[j + 2]);
						imRight[j + 3] += (imALR[j + 3] * reBLeft[j + 3] + reALR[j + 3] * imBLeft[j + 3]) + (reARR[j + 3] * imBRight[j + 3] + imARR[j + 3] * reBRight[j + 3]);
					}
					reLeft[end4] += (reARL[end4] * reBRight[end4] - imARL[end4] * imBRight[end4]) + (reALL[end4] * reBLeft[end4] - imALL[end4] * imBLeft[end4]);
					imLeft[end4] += (imARL[end4] * reBRight[end4] + reARL[end4] * imBRight[end4]) + (reALL[end4] * imBLeft[end4] + imALL[end4] * reBLeft[end4]);
					reRight[end4] += (reALR[end4] * reBLeft[end4] - imALR[end4] * imBLeft[end4]) + (reARR[end4] * reBRight[end4] - imARR[end4] * imBRight[end4]);
					imRight[end4] += (imALR[end4] * reBLeft[end4] + reALR[end4] * imBLeft[end4]) + (reARR[end4] * imBRight[end4] + imARR[end4] * reBRight[end4]);
				}
			}
		}
		reALL = conv->_segmentsLLIRRe[0];
		imALL = conv->_segmentsLLIRIm[0];
		reALR = conv->_segmentsLRIRRe[0];
		imALR = conv->_segmentsLRIRIm[0];
		reARL = conv->_segmentsRLIRRe[0];
		imARL = conv->_segmentsRLIRIm[0];
		reARR = conv->_segmentsRRIRRe[0];
		imARR = conv->_segmentsRRIRIm[0];
		reBLeft = conv->_segmentsReLeft[conv->_current];
		imBLeft = conv->_segmentsImLeft[conv->_current];
		reBRight = conv->_segmentsReRight[conv->_current];
		imBRight = conv->_segmentsImRight[conv->_current];
		const float *src1Re = conv->_preMultiplied[0][0];
		const float *src1Im = conv->_preMultiplied[0][1];
		const float *src2Re = conv->_preMultiplied[1][0];
		const float *src2Im = conv->_preMultiplied[1][1];
		float realL, imagL, realR, imagR;
		conv->_fftBuffer[0][0] = (reARL[0] * reBRight[0]) + (reALL[0] * reBLeft[0]) + src1Re[0];
		conv->_fftBuffer[1][0] = (reALR[0] * reBLeft[0]) + (reARR[0] * reBRight[0]) + src2Re[0];
		for (j = 1; j < conv->_fftComplexSize; ++j)
		{
			symIdx = conv->_segSize - j;
			realL = (reARL[j] * reBRight[j] - imARL[j] * imBRight[j]) + (reALL[j] * reBLeft[j] - imALL[j] * imBLeft[j]) + src1Re[j];
			imagL = (imARL[j] * reBRight[j] + reARL[j] * imBRight[j]) + (reALL[j] * imBLeft[j] + imALL[j] * reBLeft[j]) + src1Im[j];
			realR = (reALR[j] * reBLeft[j] - imALR[j] * imBLeft[j]) + (reARR[j] * reBRight[j] - imARR[j] * imBRight[j]) + src2Re[j];
			imagR = (imALR[j] * reBLeft[j] + reALR[j] * imBLeft[j]) + (reARR[j] * imBRight[j] + imARR[j] * reBRight[j]) + src2Im[j];
			conv->_fftBuffer[0][conv->bit[j]] = (realL + imagL) * 0.5f;
			conv->_fftBuffer[0][conv->bit[symIdx]] = (realL - imagL) * 0.5f;
			conv->_fftBuffer[1][conv->bit[j]] = (realR + imagR) * 0.5f;
			conv->_fftBuffer[1][conv->bit[symIdx]] = (realR - imagR) * 0.5f;
		}
		// Backward FFT
		conv->fft(conv->_fftBuffer[0], conv->sine);
		conv->fft(conv->_fftBuffer[1], conv->sine);

		// Add overlap
		float *result1 = y1 + processed;
		const float *a1 = conv->_fftBuffer[0] + inputBufferPos;
		const float *b1 = conv->_overlap[0] + inputBufferPos;
		float *result2 = y2 + processed;
		const float *a2 = conv->_fftBuffer[1] + inputBufferPos;
		const float *b2 = conv->_overlap[1] + inputBufferPos;
		end4 = (processing >> 2) << 2;
		for (j = 0; j < end4; j += 4)
		{
			result1[j + 0] = (a1[j + 0] + b1[j + 0]) * conv->gain;
			result1[j + 1] = (a1[j + 1] + b1[j + 1]) * conv->gain;
			result1[j + 2] = (a1[j + 2] + b1[j + 2]) * conv->gain;
			result1[j + 3] = (a1[j + 3] + b1[j + 3]) * conv->gain;
			result2[j + 0] = (a2[j + 0] + b2[j + 0]) * conv->gain;
			result2[j + 1] = (a2[j + 1] + b2[j + 1]) * conv->gain;
			result2[j + 2] = (a2[j + 2] + b2[j + 2]) * conv->gain;
			result2[j + 3] = (a2[j + 3] + b2[j + 3]) * conv->gain;
		}
		for (j = end4; j < processing; ++j)
		{
			result1[j] = (a1[j] + b1[j]) * conv->gain;
			result2[j] = (a2[j] + b2[j]) * conv->gain;
		}

		// Input buffer full => Next block
		conv->_inputBufferFill += processing;
		if (conv->_inputBufferFill == conv->_blockSize)
		{
			// Input buffer is empty again now
			memset(conv->_inputBuffer[0], 0, conv->_blockSize * sizeof(float));
			memset(conv->_inputBuffer[1], 0, conv->_blockSize * sizeof(float));
			conv->_inputBufferFill = 0;
			// Save the overlap
			memcpy(conv->_overlap[0], conv->_fftBuffer[0] + conv->_blockSize, conv->_blockSize * sizeof(float));
			memcpy(conv->_overlap[1], conv->_fftBuffer[1] + conv->_blockSize, conv->_blockSize * sizeof(float));
			// Update current segment
			conv->_current = (conv->_current > 0) ? (conv->_current - 1) : conv->_segCountMinus1;
		}
		processed += processing;
	}
}
void FFTConvolver2x2Process(FFTConvolver2x2 *conv, const float* x1, const float* x2, float* y1, float* y2, unsigned int len)
{
	unsigned int j, symIdx;
	unsigned int processed = 0;
	while (processed < len)
	{
		const int inputBufferWasEmpty = (conv->_inputBufferFill == 0);
		const unsigned int processing = min(len - processed, conv->_blockSize - conv->_inputBufferFill);
		const unsigned int inputBufferPos = conv->_inputBufferFill;
		memcpy(conv->_inputBuffer[0] + inputBufferPos, x1 + processed, processing * sizeof(float));
		memcpy(conv->_inputBuffer[1] + inputBufferPos, x2 + processed, processing * sizeof(float));

		// Forward FFT
		for (j = 0; j < conv->_blockSize; j++)
		{
			conv->_fftBuffer[0][conv->bit[j]] = conv->_inputBuffer[0][j];
			conv->_fftBuffer[1][conv->bit[j]] = conv->_inputBuffer[1][j];
		}
		for (j = conv->_blockSize; j < conv->_segSize; j++)
		{
			conv->_fftBuffer[0][conv->bit[j]] = 0.0f;
			conv->_fftBuffer[1][conv->bit[j]] = 0.0f;
		}
		conv->fft(conv->_fftBuffer[0], conv->sine);
		conv->_segmentsReLeft[conv->_current][0] = conv->_fftBuffer[0][0];
		conv->fft(conv->_fftBuffer[1], conv->sine);
		conv->_segmentsReRight[conv->_current][0] = conv->_fftBuffer[1][0];
		for (j = 1; j < conv->_fftComplexSize; j++)
		{
			symIdx = conv->_segSize - j;
			conv->_segmentsReLeft[conv->_current][j] = conv->_fftBuffer[0][j] + conv->_fftBuffer[0][symIdx];
			conv->_segmentsImLeft[conv->_current][j] = conv->_fftBuffer[0][j] - conv->_fftBuffer[0][symIdx];
			conv->_segmentsReRight[conv->_current][j] = conv->_fftBuffer[1][j] + conv->_fftBuffer[1][symIdx];
			conv->_segmentsImRight[conv->_current][j] = conv->_fftBuffer[1][j] - conv->_fftBuffer[1][symIdx];
		}

		// Complex multiplication
		const float *reALL, *imALL, *reARR, *imARR;
		const float *reBLeft;
		const float *imBLeft;
		const float *reBRight;
		const float *imBRight;
		unsigned int end4 = conv->_fftComplexSize - 1;
		if (inputBufferWasEmpty)
		{
			unsigned int segFrameIndex = (conv->_current + 1) % conv->_segCount;
			if (conv->_segCount > 1)
			{
				float *reLeft = conv->_preMultiplied[0][0];
				float *imLeft = conv->_preMultiplied[0][1];
				float *reRight = conv->_preMultiplied[1][0];
				float *imRight = conv->_preMultiplied[1][1];
				reALL = conv->_segmentsLLIRRe[1];
				imALL = conv->_segmentsLLIRIm[1];
				reARR = conv->_segmentsRRIRRe[1];
				imARR = conv->_segmentsRRIRIm[1];
				reBLeft = conv->_segmentsReLeft[segFrameIndex];
				imBLeft = conv->_segmentsImLeft[segFrameIndex];
				reBRight = conv->_segmentsReRight[segFrameIndex];
				imBRight = conv->_segmentsImRight[segFrameIndex];
				for (j = 0; j < end4; j += 4)
				{
					reLeft[j + 0] = reALL[j + 0] * reBLeft[j + 0] - imALL[j + 0] * imBLeft[j + 0];
					reLeft[j + 1] = reALL[j + 1] * reBLeft[j + 1] - imALL[j + 1] * imBLeft[j + 1];
					reLeft[j + 2] = reALL[j + 2] * reBLeft[j + 2] - imALL[j + 2] * imBLeft[j + 2];
					reLeft[j + 3] = reALL[j + 3] * reBLeft[j + 3] - imALL[j + 3] * imBLeft[j + 3];
					imLeft[j + 0] = reALL[j + 0] * imBLeft[j + 0] + imALL[j + 0] * reBLeft[j + 0];
					imLeft[j + 1] = reALL[j + 1] * imBLeft[j + 1] + imALL[j + 1] * reBLeft[j + 1];
					imLeft[j + 2] = reALL[j + 2] * imBLeft[j + 2] + imALL[j + 2] * reBLeft[j + 2];
					imLeft[j + 3] = reALL[j + 3] * imBLeft[j + 3] + imALL[j + 3] * reBLeft[j + 3];
					reRight[j + 0] = reARR[j + 0] * reBRight[j + 0] - imARR[j + 0] * imBRight[j + 0];
					reRight[j + 1] = reARR[j + 1] * reBRight[j + 1] - imARR[j + 1] * imBRight[j + 1];
					reRight[j + 2] = reARR[j + 2] * reBRight[j + 2] - imARR[j + 2] * imBRight[j + 2];
					reRight[j + 3] = reARR[j + 3] * reBRight[j + 3] - imARR[j + 3] * imBRight[j + 3];
					imRight[j + 0] = reARR[j + 0] * imBRight[j + 0] + imARR[j + 0] * reBRight[j + 0];
					imRight[j + 1] = reARR[j + 1] * imBRight[j + 1] + imARR[j + 1] * reBRight[j + 1];
					imRight[j + 2] = reARR[j + 2] * imBRight[j + 2] + imARR[j + 2] * reBRight[j + 2];
					imRight[j + 3] = reARR[j + 3] * imBRight[j + 3] + imARR[j + 3] * reBRight[j + 3];
				}
				reLeft[end4] = reALL[end4] * reBLeft[end4] - imALL[end4] * imBLeft[end4];
				imLeft[end4] = reALL[end4] * imBLeft[end4] + imALL[end4] * reBLeft[end4];
				reRight[end4] = reARR[end4] * reBRight[end4] - imARR[end4] * imBRight[end4];
				imRight[end4] = reARR[end4] * imBRight[end4] + imARR[end4] * reBRight[end4];
				for (unsigned int i = 2; i < conv->_segCount; ++i)
				{
					segFrameIndex = (conv->_current + i) % conv->_segCount;
					reLeft = conv->_preMultiplied[0][0];
					imLeft = conv->_preMultiplied[0][1];
					reRight = conv->_preMultiplied[1][0];
					imRight = conv->_preMultiplied[1][1];
					reALL = conv->_segmentsLLIRRe[i];
					imALL = conv->_segmentsLLIRIm[i];
					reARR = conv->_segmentsRRIRRe[i];
					imARR = conv->_segmentsRRIRIm[i];
					reBLeft = conv->_segmentsReLeft[segFrameIndex];
					imBLeft = conv->_segmentsImLeft[segFrameIndex];
					reBRight = conv->_segmentsReRight[segFrameIndex];
					imBRight = conv->_segmentsImRight[segFrameIndex];
					for (j = 0; j < end4; j += 4)
					{
						reLeft[j + 0] += reALL[j + 0] * reBLeft[j + 0] - imALL[j + 0] * imBLeft[j + 0];
						reLeft[j + 1] += reALL[j + 1] * reBLeft[j + 1] - imALL[j + 1] * imBLeft[j + 1];
						reLeft[j + 2] += reALL[j + 2] * reBLeft[j + 2] - imALL[j + 2] * imBLeft[j + 2];
						reLeft[j + 3] += reALL[j + 3] * reBLeft[j + 3] - imALL[j + 3] * imBLeft[j + 3];
						imLeft[j + 0] += reALL[j + 0] * imBLeft[j + 0] + imALL[j + 0] * reBLeft[j + 0];
						imLeft[j + 1] += reALL[j + 1] * imBLeft[j + 1] + imALL[j + 1] * reBLeft[j + 1];
						imLeft[j + 2] += reALL[j + 2] * imBLeft[j + 2] + imALL[j + 2] * reBLeft[j + 2];
						imLeft[j + 3] += reALL[j + 3] * imBLeft[j + 3] + imALL[j + 3] * reBLeft[j + 3];
						reRight[j + 0] += reARR[j + 0] * reBRight[j + 0] - imARR[j + 0] * imBRight[j + 0];
						reRight[j + 1] += reARR[j + 1] * reBRight[j + 1] - imARR[j + 1] * imBRight[j + 1];
						reRight[j + 2] += reARR[j + 2] * reBRight[j + 2] - imARR[j + 2] * imBRight[j + 2];
						reRight[j + 3] += reARR[j + 3] * reBRight[j + 3] - imARR[j + 3] * imBRight[j + 3];
						imRight[j + 0] += reARR[j + 0] * imBRight[j + 0] + imARR[j + 0] * reBRight[j + 0];
						imRight[j + 1] += reARR[j + 1] * imBRight[j + 1] + imARR[j + 1] * reBRight[j + 1];
						imRight[j + 2] += reARR[j + 2] * imBRight[j + 2] + imARR[j + 2] * reBRight[j + 2];
						imRight[j + 3] += reARR[j + 3] * imBRight[j + 3] + imARR[j + 3] * reBRight[j + 3];
					}
					reLeft[end4] += reALL[end4] * reBLeft[end4] - imALL[end4] * imBLeft[end4];
					imLeft[end4] += reALL[end4] * imBLeft[end4] + imALL[end4] * reBLeft[end4];
					reRight[end4] += reARR[end4] * reBRight[end4] - imARR[end4] * imBRight[end4];
					imRight[end4] += reARR[end4] * imBRight[end4] + imARR[end4] * reBRight[end4];
				}
			}
		}
		reALL = conv->_segmentsLLIRRe[0];
		imALL = conv->_segmentsLLIRIm[0];
		reARR = conv->_segmentsRRIRRe[0];
		imARR = conv->_segmentsRRIRIm[0];
		reBLeft = conv->_segmentsReLeft[conv->_current];
		imBLeft = conv->_segmentsImLeft[conv->_current];
		reBRight = conv->_segmentsReRight[conv->_current];
		imBRight = conv->_segmentsImRight[conv->_current];
		const float *src1Re = conv->_preMultiplied[0][0];
		const float *src1Im = conv->_preMultiplied[0][1];
		const float *src2Re = conv->_preMultiplied[1][0];
		const float *src2Im = conv->_preMultiplied[1][1];
		float realL, imagL, realR, imagR;
		conv->_fftBuffer[0][0] = (reALL[0] * reBLeft[0]) + src1Re[0];
		conv->_fftBuffer[1][0] = (reARR[0] * reBRight[0]) + src2Re[0];
		for (j = 1; j < conv->_fftComplexSize; ++j)
		{
			symIdx = conv->_segSize - j;
			realL = (reALL[j] * reBLeft[j] - imALL[j] * imBLeft[j]) + src1Re[j];
			imagL = (reALL[j] * imBLeft[j] + imALL[j] * reBLeft[j]) + src1Im[j];
			realR = (reARR[j] * reBRight[j] - imARR[j] * imBRight[j]) + src2Re[j];
			imagR = (reARR[j] * imBRight[j] + imARR[j] * reBRight[j]) + src2Im[j];
			conv->_fftBuffer[0][conv->bit[j]] = (realL + imagL) * 0.5f;
			conv->_fftBuffer[0][conv->bit[symIdx]] = (realL - imagL) * 0.5f;
			conv->_fftBuffer[1][conv->bit[j]] = (realR + imagR) * 0.5f;
			conv->_fftBuffer[1][conv->bit[symIdx]] = (realR - imagR) * 0.5f;
		}
		// Backward FFT
		conv->fft(conv->_fftBuffer[0], conv->sine);
		conv->fft(conv->_fftBuffer[1], conv->sine);

		// Add overlap
		float *result1 = y1 + processed;
		const float *a1 = conv->_fftBuffer[0] + inputBufferPos;
		const float *b1 = conv->_overlap[0] + inputBufferPos;
		float *result2 = y2 + processed;
		const float *a2 = conv->_fftBuffer[1] + inputBufferPos;
		const float *b2 = conv->_overlap[1] + inputBufferPos;
		end4 = (processing >> 2) << 2;
		for (j = 0; j < end4; j += 4)
		{
			result1[j + 0] = (a1[j + 0] + b1[j + 0]) * conv->gain;
			result1[j + 1] = (a1[j + 1] + b1[j + 1]) * conv->gain;
			result1[j + 2] = (a1[j + 2] + b1[j + 2]) * conv->gain;
			result1[j + 3] = (a1[j + 3] + b1[j + 3]) * conv->gain;
			result2[j + 0] = (a2[j + 0] + b2[j + 0]) * conv->gain;
			result2[j + 1] = (a2[j + 1] + b2[j + 1]) * conv->gain;
			result2[j + 2] = (a2[j + 2] + b2[j + 2]) * conv->gain;
			result2[j + 3] = (a2[j + 3] + b2[j + 3]) * conv->gain;
		}
		for (j = end4; j < processing; ++j)
		{
			result1[j] = (a1[j] + b1[j]) * conv->gain;
			result2[j] = (a2[j] + b2[j]) * conv->gain;
		}

		// Input buffer full => Next block
		conv->_inputBufferFill += processing;
		if (conv->_inputBufferFill == conv->_blockSize)
		{
			// Input buffer is empty again now
			memset(conv->_inputBuffer[0], 0, conv->_blockSize * sizeof(float));
			memset(conv->_inputBuffer[1], 0, conv->_blockSize * sizeof(float));
			conv->_inputBufferFill = 0;
			// Save the overlap
			memcpy(conv->_overlap[0], conv->_fftBuffer[0] + conv->_blockSize, conv->_blockSize * sizeof(float));
			memcpy(conv->_overlap[1], conv->_fftBuffer[1] + conv->_blockSize, conv->_blockSize * sizeof(float));
			// Update current segment
			conv->_current = (conv->_current > 0) ? (conv->_current - 1) : conv->_segCountMinus1;
		}
		processed += processing;
	}
}
void FFTConvolver1x2Process(FFTConvolver1x2 *conv, const float* x, float* y1, float* y2, unsigned int len)
{
	unsigned int j, symIdx;
	unsigned int processed = 0;
	while (processed < len)
	{
		const int inputBufferWasEmpty = (conv->_inputBufferFill == 0);
		const unsigned int processing = min(len - processed, conv->_blockSize - conv->_inputBufferFill);
		const unsigned int inputBufferPos = conv->_inputBufferFill;
		memcpy(conv->_inputBuffer + inputBufferPos, x + processed, processing * sizeof(float));

		// Forward FFT
		for (j = 0; j < conv->_blockSize; j++)
			conv->_fftBuffer[0][conv->bit[j]] = conv->_inputBuffer[j];
		for (j = conv->_blockSize; j < conv->_segSize; j++)
			conv->_fftBuffer[0][conv->bit[j]] = 0.0f;
		conv->fft(conv->_fftBuffer[0], conv->sine);
		conv->_segmentsRe[conv->_current][0] = conv->_fftBuffer[0][0];
		for (j = 1; j < conv->_fftComplexSize; j++)
		{
			symIdx = conv->_segSize - j;
			conv->_segmentsRe[conv->_current][j] = conv->_fftBuffer[0][j] + conv->_fftBuffer[0][symIdx];
			conv->_segmentsIm[conv->_current][j] = conv->_fftBuffer[0][j] - conv->_fftBuffer[0][symIdx];
		}

		// Complex multiplication
		const float *reALL, *imALL, *reARR, *imARR;
		const float *reB;
		const float *imB;
		unsigned int end4 = conv->_fftComplexSize - 1;
		if (inputBufferWasEmpty)
		{
			unsigned int segFrameIndex = (conv->_current + 1) % conv->_segCount;
			if (conv->_segCount > 1)
			{
				float *reLeft = conv->_preMultiplied[0][0];
				float *imLeft = conv->_preMultiplied[0][1];
				float *reRight = conv->_preMultiplied[1][0];
				float *imRight = conv->_preMultiplied[1][1];
				reALL = conv->_segmentsLLIRRe[1];
				imALL = conv->_segmentsLLIRIm[1];
				reARR = conv->_segmentsRRIRRe[1];
				imARR = conv->_segmentsRRIRIm[1];
				reB = conv->_segmentsRe[segFrameIndex];
				imB = conv->_segmentsIm[segFrameIndex];
				for (j = 0; j < end4; j += 4)
				{
					reLeft[j + 0] = reALL[j + 0] * reB[j + 0] - imALL[j + 0] * imB[j + 0];
					reLeft[j + 1] = reALL[j + 1] * reB[j + 1] - imALL[j + 1] * imB[j + 1];
					reLeft[j + 2] = reALL[j + 2] * reB[j + 2] - imALL[j + 2] * imB[j + 2];
					reLeft[j + 3] = reALL[j + 3] * reB[j + 3] - imALL[j + 3] * imB[j + 3];
					imLeft[j + 0] = reALL[j + 0] * imB[j + 0] + imALL[j + 0] * reB[j + 0];
					imLeft[j + 1] = reALL[j + 1] * imB[j + 1] + imALL[j + 1] * reB[j + 1];
					imLeft[j + 2] = reALL[j + 2] * imB[j + 2] + imALL[j + 2] * reB[j + 2];
					imLeft[j + 3] = reALL[j + 3] * imB[j + 3] + imALL[j + 3] * reB[j + 3];
					reRight[j + 0] = reARR[j + 0] * reB[j + 0] - imARR[j + 0] * imB[j + 0];
					reRight[j + 1] = reARR[j + 1] * reB[j + 1] - imARR[j + 1] * imB[j + 1];
					reRight[j + 2] = reARR[j + 2] * reB[j + 2] - imARR[j + 2] * imB[j + 2];
					reRight[j + 3] = reARR[j + 3] * reB[j + 3] - imARR[j + 3] * imB[j + 3];
					imRight[j + 0] = reARR[j + 0] * imB[j + 0] + imARR[j + 0] * reB[j + 0];
					imRight[j + 1] = reARR[j + 1] * imB[j + 1] + imARR[j + 1] * reB[j + 1];
					imRight[j + 2] = reARR[j + 2] * imB[j + 2] + imARR[j + 2] * reB[j + 2];
					imRight[j + 3] = reARR[j + 3] * imB[j + 3] + imARR[j + 3] * reB[j + 3];
				}
				reLeft[end4] = reALL[end4] * reB[end4] - imALL[end4] * imB[end4];
				imLeft[end4] = reALL[end4] * imB[end4] + imALL[end4] * reB[end4];
				reRight[end4] = reARR[end4] * reB[end4] - imARR[end4] * imB[end4];
				imRight[end4] = reARR[end4] * imB[end4] + imARR[end4] * reB[end4];
				for (unsigned int i = 2; i < conv->_segCount; ++i)
				{
					segFrameIndex = (conv->_current + i) % conv->_segCount;
					reLeft = conv->_preMultiplied[0][0];
					imLeft = conv->_preMultiplied[0][1];
					reRight = conv->_preMultiplied[1][0];
					imRight = conv->_preMultiplied[1][1];
					reALL = conv->_segmentsLLIRRe[i];
					imALL = conv->_segmentsLLIRIm[i];
					reARR = conv->_segmentsRRIRRe[i];
					imARR = conv->_segmentsRRIRIm[i];
					reB = conv->_segmentsRe[segFrameIndex];
					imB = conv->_segmentsIm[segFrameIndex];
					for (j = 0; j < end4; j += 4)
					{
						reLeft[j + 0] += reALL[j + 0] * reB[j + 0] - imALL[j + 0] * imB[j + 0];
						reLeft[j + 1] += reALL[j + 1] * reB[j + 1] - imALL[j + 1] * imB[j + 1];
						reLeft[j + 2] += reALL[j + 2] * reB[j + 2] - imALL[j + 2] * imB[j + 2];
						reLeft[j + 3] += reALL[j + 3] * reB[j + 3] - imALL[j + 3] * imB[j + 3];
						imLeft[j + 0] += reALL[j + 0] * imB[j + 0] + imALL[j + 0] * reB[j + 0];
						imLeft[j + 1] += reALL[j + 1] * imB[j + 1] + imALL[j + 1] * reB[j + 1];
						imLeft[j + 2] += reALL[j + 2] * imB[j + 2] + imALL[j + 2] * reB[j + 2];
						imLeft[j + 3] += reALL[j + 3] * imB[j + 3] + imALL[j + 3] * reB[j + 3];
						reRight[j + 0] += reARR[j + 0] * reB[j + 0] - imARR[j + 0] * imB[j + 0];
						reRight[j + 1] += reARR[j + 1] * reB[j + 1] - imARR[j + 1] * imB[j + 1];
						reRight[j + 2] += reARR[j + 2] * reB[j + 2] - imARR[j + 2] * imB[j + 2];
						reRight[j + 3] += reARR[j + 3] * reB[j + 3] - imARR[j + 3] * imB[j + 3];
						imRight[j + 0] += reARR[j + 0] * imB[j + 0] + imARR[j + 0] * reB[j + 0];
						imRight[j + 1] += reARR[j + 1] * imB[j + 1] + imARR[j + 1] * reB[j + 1];
						imRight[j + 2] += reARR[j + 2] * imB[j + 2] + imARR[j + 2] * reB[j + 2];
						imRight[j + 3] += reARR[j + 3] * imB[j + 3] + imARR[j + 3] * reB[j + 3];
					}
					reLeft[end4] += reALL[end4] * reB[end4] - imALL[end4] * imB[end4];
					imLeft[end4] += reALL[end4] * imB[end4] + imALL[end4] * reB[end4];
					reRight[end4] += reARR[end4] * reB[end4] - imARR[end4] * imB[end4];
					imRight[end4] += reARR[end4] * imB[end4] + imARR[end4] * reB[end4];
				}
			}
		}
		reALL = conv->_segmentsLLIRRe[0];
		imALL = conv->_segmentsLLIRIm[0];
		reARR = conv->_segmentsRRIRRe[0];
		imARR = conv->_segmentsRRIRIm[0];
		reB = conv->_segmentsRe[conv->_current];
		imB = conv->_segmentsIm[conv->_current];
		const float *src1Re = conv->_preMultiplied[0][0];
		const float *src1Im = conv->_preMultiplied[0][1];
		const float *src2Re = conv->_preMultiplied[1][0];
		const float *src2Im = conv->_preMultiplied[1][1];
		float realL, imagL, realR, imagR;
		conv->_fftBuffer[0][0] = (reALL[0] * reB[0]) + src1Re[0];
		conv->_fftBuffer[1][0] = (reARR[0] * reB[0]) + src2Re[0];
		for (j = 1; j < conv->_fftComplexSize; ++j)
		{
			symIdx = conv->_segSize - j;
			realL = (reALL[j] * reB[j] - imALL[j] * imB[j]) + src1Re[j];
			imagL = (reALL[j] * imB[j] + imALL[j] * reB[j]) + src1Im[j];
			realR = (reARR[j] * reB[j] - imARR[j] * imB[j]) + src2Re[j];
			imagR = (reARR[j] * imB[j] + imARR[j] * reB[j]) + src2Im[j];
			conv->_fftBuffer[0][conv->bit[j]] = (realL + imagL) * 0.5f;
			conv->_fftBuffer[0][conv->bit[symIdx]] = (realL - imagL) * 0.5f;
			conv->_fftBuffer[1][conv->bit[j]] = (realR + imagR) * 0.5f;
			conv->_fftBuffer[1][conv->bit[symIdx]] = (realR - imagR) * 0.5f;
		}
		// Backward FFT
		conv->fft(conv->_fftBuffer[0], conv->sine);
		conv->fft(conv->_fftBuffer[1], conv->sine);

		// Add overlap
		float *result1 = y1 + processed;
		const float *a1 = conv->_fftBuffer[0] + inputBufferPos;
		const float *b1 = conv->_overlap[0] + inputBufferPos;
		float *result2 = y2 + processed;
		const float *a2 = conv->_fftBuffer[1] + inputBufferPos;
		const float *b2 = conv->_overlap[1] + inputBufferPos;
		end4 = (processing >> 2) << 2;
		for (j = 0; j < end4; j += 4)
		{
			result1[j + 0] = (a1[j + 0] + b1[j + 0]) * conv->gain;
			result1[j + 1] = (a1[j + 1] + b1[j + 1]) * conv->gain;
			result1[j + 2] = (a1[j + 2] + b1[j + 2]) * conv->gain;
			result1[j + 3] = (a1[j + 3] + b1[j + 3]) * conv->gain;
			result2[j + 0] = (a2[j + 0] + b2[j + 0]) * conv->gain;
			result2[j + 1] = (a2[j + 1] + b2[j + 1]) * conv->gain;
			result2[j + 2] = (a2[j + 2] + b2[j + 2]) * conv->gain;
			result2[j + 3] = (a2[j + 3] + b2[j + 3]) * conv->gain;
		}
		for (j = end4; j < processing; ++j)
		{
			result1[j] = (a1[j] + b1[j]) * conv->gain;
			result2[j] = (a2[j] + b2[j]) * conv->gain;
		}

		// Input buffer full => Next block
		conv->_inputBufferFill += processing;
		if (conv->_inputBufferFill == conv->_blockSize)
		{
			// Input buffer is empty again now
			memset(conv->_inputBuffer, 0, conv->_blockSize * sizeof(float));
			conv->_inputBufferFill = 0;
			// Save the overlap
			memcpy(conv->_overlap[0], conv->_fftBuffer[0] + conv->_blockSize, conv->_blockSize * sizeof(float));
			memcpy(conv->_overlap[1], conv->_fftBuffer[1] + conv->_blockSize, conv->_blockSize * sizeof(float));
			// Update current segment
			conv->_current = (conv->_current > 0) ? (conv->_current - 1) : conv->_segCountMinus1;
		}
		processed += processing;
	}
}