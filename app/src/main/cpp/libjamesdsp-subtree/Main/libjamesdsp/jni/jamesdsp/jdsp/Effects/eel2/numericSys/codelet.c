#include "codelet.h"
void DFT1024(float *A, const float *sinTab)
{
	int i, j, theta;
	float alpha, beta, beta1, beta2, y0, y1, y2, y3;
	for (i = 0; i < 1024; i += 4)
	{
		alpha = A[i];
		beta = A[i + 1];
		beta1 = A[i + 2];
		beta2 = A[i + 3];
		y0 = alpha + beta;
		y1 = alpha - beta;
		y2 = beta1 + beta2;
		y3 = beta1 - beta2;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	for (i = 0; i < 1024; i += 8)
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
		beta1 = 0.70710678118654752440084436210485f*(A[i + 5] + A[i + 7]);
		beta2 = 0.70710678118654752440084436210485f*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	for (i = 0; i < 1024; i += 16)
	{
		theta = 64;
		alpha = A[i];
		beta = A[i + 8];
		A[i] = alpha + beta;
		A[i + 8] = alpha - beta;
		alpha = A[i + 4];
		beta = A[i + 12];
		A[i + 4] = alpha + beta;
		A[i + 12] = alpha - beta;
		for (j = 1; j < 4; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 256];
			alpha = A[i + j];
			beta = A[i - j + 8];
			beta1 = A[i + j + 8] * y1 + A[i - j + 16] * y0;
			beta2 = A[i + j + 8] * y0 - A[i - j + 16] * y1;
			theta += 64;
			A[i + j] = alpha + beta1;
			A[i + j + 8] = alpha - beta1;
			A[i - j + 8] = beta + beta2;
			A[i - j + 16] = beta - beta2;
		}
	}
	for (i = 0; i < 1024; i += 32)
	{
		theta = 32;
		alpha = A[i];
		beta = A[i + 16];
		A[i] = alpha + beta;
		A[i + 16] = alpha - beta;
		alpha = A[i + 8];
		beta = A[i + 24];
		A[i + 8] = alpha + beta;
		A[i + 24] = alpha - beta;
		for (j = 1; j < 8; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 256];
			alpha = A[i + j];
			beta = A[i - j + 16];
			beta1 = A[i + j + 16] * y1 + A[i - j + 32] * y0;
			beta2 = A[i + j + 16] * y0 - A[i - j + 32] * y1;
			theta += 32;
			A[i + j] = alpha + beta1;
			A[i + j + 16] = alpha - beta1;
			A[i - j + 16] = beta + beta2;
			A[i - j + 32] = beta - beta2;
		}
	}
	for (i = 0; i < 1024; i += 64)
	{
		theta = 16;
		alpha = A[i];
		beta = A[i + 32];
		A[i] = alpha + beta;
		A[i + 32] = alpha - beta;
		alpha = A[i + 16];
		beta = A[i + 48];
		A[i + 16] = alpha + beta;
		A[i + 48] = alpha - beta;
		for (j = 1; j < 16; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 256];
			alpha = A[i + j];
			beta = A[i - j + 32];
			beta1 = A[i + j + 32] * y1 + A[i - j + 64] * y0;
			beta2 = A[i + j + 32] * y0 - A[i - j + 64] * y1;
			theta += 16;
			A[i + j] = alpha + beta1;
			A[i + j + 32] = alpha - beta1;
			A[i - j + 32] = beta + beta2;
			A[i - j + 64] = beta - beta2;
		}
	}
	for (i = 0; i < 1024; i += 128)
	{
		theta = 8;
		alpha = A[i];
		beta = A[i + 64];
		A[i] = alpha + beta;
		A[i + 64] = alpha - beta;
		alpha = A[i + 32];
		beta = A[i + 96];
		A[i + 32] = alpha + beta;
		A[i + 96] = alpha - beta;
		for (j = 1; j < 32; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 256];
			alpha = A[i + j];
			beta = A[i - j + 64];
			beta1 = A[i + j + 64] * y1 + A[i - j + 128] * y0;
			beta2 = A[i + j + 64] * y0 - A[i - j + 128] * y1;
			theta += 8;
			A[i + j] = alpha + beta1;
			A[i + j + 64] = alpha - beta1;
			A[i - j + 64] = beta + beta2;
			A[i - j + 128] = beta - beta2;
		}
	}
	for (i = 0; i < 1024; i += 256)
	{
		theta = 4;
		alpha = A[i];
		beta = A[i + 128];
		A[i] = alpha + beta;
		A[i + 128] = alpha - beta;
		alpha = A[i + 64];
		beta = A[i + 192];
		A[i + 64] = alpha + beta;
		A[i + 192] = alpha - beta;
		for (j = 1; j < 64; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 256];
			alpha = A[i + j];
			beta = A[i - j + 128];
			beta1 = A[i + j + 128] * y1 + A[i - j + 256] * y0;
			beta2 = A[i + j + 128] * y0 - A[i - j + 256] * y1;
			theta += 4;
			A[i + j] = alpha + beta1;
			A[i + j + 128] = alpha - beta1;
			A[i - j + 128] = beta + beta2;
			A[i - j + 256] = beta - beta2;
		}
	}
	for (i = 0; i < 1024; i += 512)
	{
		theta = 2;
		alpha = A[i];
		beta = A[i + 256];
		A[i] = alpha + beta;
		A[i + 256] = alpha - beta;
		alpha = A[i + 128];
		beta = A[i + 384];
		A[i + 128] = alpha + beta;
		A[i + 384] = alpha - beta;
		for (j = 1; j < 128; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 256];
			alpha = A[i + j];
			beta = A[i - j + 256];
			beta1 = A[i + j + 256] * y1 + A[i - j + 512] * y0;
			beta2 = A[i + j + 256] * y0 - A[i - j + 512] * y1;
			theta += 2;
			A[i + j] = alpha + beta1;
			A[i + j + 256] = alpha - beta1;
			A[i - j + 256] = beta + beta2;
			A[i - j + 512] = beta - beta2;
		}
	}
	theta = 1;
	alpha = A[0];
	beta = A[512];
	A[0] = alpha + beta;
	A[512] = alpha - beta;
	alpha = A[256];
	beta = A[768];
	A[256] = alpha + beta;
	A[768] = alpha - beta;
	for (j = 1; j < 256; j++)
	{
		y0 = sinTab[theta];
		y1 = sinTab[theta + 256];
		alpha = A[j];
		beta = A[-j + 512];
		beta1 = A[j + 512] * y1 + A[-j + 1024] * y0;
		beta2 = A[j + 512] * y0 - A[-j + 1024] * y1;
		theta += 1;
		A[j] = alpha + beta1;
		A[j + 512] = alpha - beta1;
		A[-j + 512] = beta + beta2;
		A[-j + 1024] = beta - beta2;
	}
}
void DFT1048576(float *A, const float *sinTab)
{
	int i, j, theta;
	float alpha, beta, beta1, beta2, y0, y1, y2, y3;
	for (i = 0; i < 1048576; i += 4)
	{
		alpha = A[i];
		beta = A[i + 1];
		beta1 = A[i + 2];
		beta2 = A[i + 3];
		y0 = alpha + beta;
		y1 = alpha - beta;
		y2 = beta1 + beta2;
		y3 = beta1 - beta2;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	for (i = 0; i < 1048576; i += 8)
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
		beta1 = 0.70710678118654752440084436210485f*(A[i + 5] + A[i + 7]);
		beta2 = 0.70710678118654752440084436210485f*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	for (i = 0; i < 1048576; i += 16)
	{
		theta = 65536;
		alpha = A[i];
		beta = A[i + 8];
		A[i] = alpha + beta;
		A[i + 8] = alpha - beta;
		alpha = A[i + 4];
		beta = A[i + 12];
		A[i + 4] = alpha + beta;
		A[i + 12] = alpha - beta;
		for (j = 1; j < 4; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 262144];
			alpha = A[i + j];
			beta = A[i - j + 8];
			beta1 = A[i + j + 8] * y1 + A[i - j + 16] * y0;
			beta2 = A[i + j + 8] * y0 - A[i - j + 16] * y1;
			theta += 65536;
			A[i + j] = alpha + beta1;
			A[i + j + 8] = alpha - beta1;
			A[i - j + 8] = beta + beta2;
			A[i - j + 16] = beta - beta2;
		}
	}
	for (i = 0; i < 1048576; i += 32)
	{
		theta = 32768;
		alpha = A[i];
		beta = A[i + 16];
		A[i] = alpha + beta;
		A[i + 16] = alpha - beta;
		alpha = A[i + 8];
		beta = A[i + 24];
		A[i + 8] = alpha + beta;
		A[i + 24] = alpha - beta;
		for (j = 1; j < 8; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 262144];
			alpha = A[i + j];
			beta = A[i - j + 16];
			beta1 = A[i + j + 16] * y1 + A[i - j + 32] * y0;
			beta2 = A[i + j + 16] * y0 - A[i - j + 32] * y1;
			theta += 32768;
			A[i + j] = alpha + beta1;
			A[i + j + 16] = alpha - beta1;
			A[i - j + 16] = beta + beta2;
			A[i - j + 32] = beta - beta2;
		}
	}
	for (i = 0; i < 1048576; i += 64)
	{
		theta = 16384;
		alpha = A[i];
		beta = A[i + 32];
		A[i] = alpha + beta;
		A[i + 32] = alpha - beta;
		alpha = A[i + 16];
		beta = A[i + 48];
		A[i + 16] = alpha + beta;
		A[i + 48] = alpha - beta;
		for (j = 1; j < 16; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 262144];
			alpha = A[i + j];
			beta = A[i - j + 32];
			beta1 = A[i + j + 32] * y1 + A[i - j + 64] * y0;
			beta2 = A[i + j + 32] * y0 - A[i - j + 64] * y1;
			theta += 16384;
			A[i + j] = alpha + beta1;
			A[i + j + 32] = alpha - beta1;
			A[i - j + 32] = beta + beta2;
			A[i - j + 64] = beta - beta2;
		}
	}
	for (i = 0; i < 1048576; i += 128)
	{
		theta = 8192;
		alpha = A[i];
		beta = A[i + 64];
		A[i] = alpha + beta;
		A[i + 64] = alpha - beta;
		alpha = A[i + 32];
		beta = A[i + 96];
		A[i + 32] = alpha + beta;
		A[i + 96] = alpha - beta;
		for (j = 1; j < 32; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 262144];
			alpha = A[i + j];
			beta = A[i - j + 64];
			beta1 = A[i + j + 64] * y1 + A[i - j + 128] * y0;
			beta2 = A[i + j + 64] * y0 - A[i - j + 128] * y1;
			theta += 8192;
			A[i + j] = alpha + beta1;
			A[i + j + 64] = alpha - beta1;
			A[i - j + 64] = beta + beta2;
			A[i - j + 128] = beta - beta2;
		}
	}
	for (i = 0; i < 1048576; i += 256)
	{
		theta = 4096;
		alpha = A[i];
		beta = A[i + 128];
		A[i] = alpha + beta;
		A[i + 128] = alpha - beta;
		alpha = A[i + 64];
		beta = A[i + 192];
		A[i + 64] = alpha + beta;
		A[i + 192] = alpha - beta;
		for (j = 1; j < 64; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 262144];
			alpha = A[i + j];
			beta = A[i - j + 128];
			beta1 = A[i + j + 128] * y1 + A[i - j + 256] * y0;
			beta2 = A[i + j + 128] * y0 - A[i - j + 256] * y1;
			theta += 4096;
			A[i + j] = alpha + beta1;
			A[i + j + 128] = alpha - beta1;
			A[i - j + 128] = beta + beta2;
			A[i - j + 256] = beta - beta2;
		}
	}
	for (i = 0; i < 1048576; i += 512)
	{
		theta = 2048;
		alpha = A[i];
		beta = A[i + 256];
		A[i] = alpha + beta;
		A[i + 256] = alpha - beta;
		alpha = A[i + 128];
		beta = A[i + 384];
		A[i + 128] = alpha + beta;
		A[i + 384] = alpha - beta;
		for (j = 1; j < 128; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 262144];
			alpha = A[i + j];
			beta = A[i - j + 256];
			beta1 = A[i + j + 256] * y1 + A[i - j + 512] * y0;
			beta2 = A[i + j + 256] * y0 - A[i - j + 512] * y1;
			theta += 2048;
			A[i + j] = alpha + beta1;
			A[i + j + 256] = alpha - beta1;
			A[i - j + 256] = beta + beta2;
			A[i - j + 512] = beta - beta2;
		}
	}
	for (i = 0; i < 1048576; i += 1024)
	{
		theta = 1024;
		alpha = A[i];
		beta = A[i + 512];
		A[i] = alpha + beta;
		A[i + 512] = alpha - beta;
		alpha = A[i + 256];
		beta = A[i + 768];
		A[i + 256] = alpha + beta;
		A[i + 768] = alpha - beta;
		for (j = 1; j < 256; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 262144];
			alpha = A[i + j];
			beta = A[i - j + 512];
			beta1 = A[i + j + 512] * y1 + A[i - j + 1024] * y0;
			beta2 = A[i + j + 512] * y0 - A[i - j + 1024] * y1;
			theta += 1024;
			A[i + j] = alpha + beta1;
			A[i + j + 512] = alpha - beta1;
			A[i - j + 512] = beta + beta2;
			A[i - j + 1024] = beta - beta2;
		}
	}
	for (i = 0; i < 1048576; i += 2048)
	{
		theta = 512;
		alpha = A[i];
		beta = A[i + 1024];
		A[i] = alpha + beta;
		A[i + 1024] = alpha - beta;
		alpha = A[i + 512];
		beta = A[i + 1536];
		A[i + 512] = alpha + beta;
		A[i + 1536] = alpha - beta;
		for (j = 1; j < 512; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 262144];
			alpha = A[i + j];
			beta = A[i - j + 1024];
			beta1 = A[i + j + 1024] * y1 + A[i - j + 2048] * y0;
			beta2 = A[i + j + 1024] * y0 - A[i - j + 2048] * y1;
			theta += 512;
			A[i + j] = alpha + beta1;
			A[i + j + 1024] = alpha - beta1;
			A[i - j + 1024] = beta + beta2;
			A[i - j + 2048] = beta - beta2;
		}
	}
	for (i = 0; i < 1048576; i += 4096)
	{
		theta = 256;
		alpha = A[i];
		beta = A[i + 2048];
		A[i] = alpha + beta;
		A[i + 2048] = alpha - beta;
		alpha = A[i + 1024];
		beta = A[i + 3072];
		A[i + 1024] = alpha + beta;
		A[i + 3072] = alpha - beta;
		for (j = 1; j < 1024; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 262144];
			alpha = A[i + j];
			beta = A[i - j + 2048];
			beta1 = A[i + j + 2048] * y1 + A[i - j + 4096] * y0;
			beta2 = A[i + j + 2048] * y0 - A[i - j + 4096] * y1;
			theta += 256;
			A[i + j] = alpha + beta1;
			A[i + j + 2048] = alpha - beta1;
			A[i - j + 2048] = beta + beta2;
			A[i - j + 4096] = beta - beta2;
		}
	}
	for (i = 0; i < 1048576; i += 8192)
	{
		theta = 128;
		alpha = A[i];
		beta = A[i + 4096];
		A[i] = alpha + beta;
		A[i + 4096] = alpha - beta;
		alpha = A[i + 2048];
		beta = A[i + 6144];
		A[i + 2048] = alpha + beta;
		A[i + 6144] = alpha - beta;
		for (j = 1; j < 2048; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 262144];
			alpha = A[i + j];
			beta = A[i - j + 4096];
			beta1 = A[i + j + 4096] * y1 + A[i - j + 8192] * y0;
			beta2 = A[i + j + 4096] * y0 - A[i - j + 8192] * y1;
			theta += 128;
			A[i + j] = alpha + beta1;
			A[i + j + 4096] = alpha - beta1;
			A[i - j + 4096] = beta + beta2;
			A[i - j + 8192] = beta - beta2;
		}
	}
	for (i = 0; i < 1048576; i += 16384)
	{
		theta = 64;
		alpha = A[i];
		beta = A[i + 8192];
		A[i] = alpha + beta;
		A[i + 8192] = alpha - beta;
		alpha = A[i + 4096];
		beta = A[i + 12288];
		A[i + 4096] = alpha + beta;
		A[i + 12288] = alpha - beta;
		for (j = 1; j < 4096; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 262144];
			alpha = A[i + j];
			beta = A[i - j + 8192];
			beta1 = A[i + j + 8192] * y1 + A[i - j + 16384] * y0;
			beta2 = A[i + j + 8192] * y0 - A[i - j + 16384] * y1;
			theta += 64;
			A[i + j] = alpha + beta1;
			A[i + j + 8192] = alpha - beta1;
			A[i - j + 8192] = beta + beta2;
			A[i - j + 16384] = beta - beta2;
		}
	}
	for (i = 0; i < 1048576; i += 32768)
	{
		theta = 32;
		alpha = A[i];
		beta = A[i + 16384];
		A[i] = alpha + beta;
		A[i + 16384] = alpha - beta;
		alpha = A[i + 8192];
		beta = A[i + 24576];
		A[i + 8192] = alpha + beta;
		A[i + 24576] = alpha - beta;
		for (j = 1; j < 8192; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 262144];
			alpha = A[i + j];
			beta = A[i - j + 16384];
			beta1 = A[i + j + 16384] * y1 + A[i - j + 32768] * y0;
			beta2 = A[i + j + 16384] * y0 - A[i - j + 32768] * y1;
			theta += 32;
			A[i + j] = alpha + beta1;
			A[i + j + 16384] = alpha - beta1;
			A[i - j + 16384] = beta + beta2;
			A[i - j + 32768] = beta - beta2;
		}
	}
	for (i = 0; i < 1048576; i += 65536)
	{
		theta = 16;
		alpha = A[i];
		beta = A[i + 32768];
		A[i] = alpha + beta;
		A[i + 32768] = alpha - beta;
		alpha = A[i + 16384];
		beta = A[i + 49152];
		A[i + 16384] = alpha + beta;
		A[i + 49152] = alpha - beta;
		for (j = 1; j < 16384; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 262144];
			alpha = A[i + j];
			beta = A[i - j + 32768];
			beta1 = A[i + j + 32768] * y1 + A[i - j + 65536] * y0;
			beta2 = A[i + j + 32768] * y0 - A[i - j + 65536] * y1;
			theta += 16;
			A[i + j] = alpha + beta1;
			A[i + j + 32768] = alpha - beta1;
			A[i - j + 32768] = beta + beta2;
			A[i - j + 65536] = beta - beta2;
		}
	}
	for (i = 0; i < 1048576; i += 131072)
	{
		theta = 8;
		alpha = A[i];
		beta = A[i + 65536];
		A[i] = alpha + beta;
		A[i + 65536] = alpha - beta;
		alpha = A[i + 32768];
		beta = A[i + 98304];
		A[i + 32768] = alpha + beta;
		A[i + 98304] = alpha - beta;
		for (j = 1; j < 32768; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 262144];
			alpha = A[i + j];
			beta = A[i - j + 65536];
			beta1 = A[i + j + 65536] * y1 + A[i - j + 131072] * y0;
			beta2 = A[i + j + 65536] * y0 - A[i - j + 131072] * y1;
			theta += 8;
			A[i + j] = alpha + beta1;
			A[i + j + 65536] = alpha - beta1;
			A[i - j + 65536] = beta + beta2;
			A[i - j + 131072] = beta - beta2;
		}
	}
	for (i = 0; i < 1048576; i += 262144)
	{
		theta = 4;
		alpha = A[i];
		beta = A[i + 131072];
		A[i] = alpha + beta;
		A[i + 131072] = alpha - beta;
		alpha = A[i + 65536];
		beta = A[i + 196608];
		A[i + 65536] = alpha + beta;
		A[i + 196608] = alpha - beta;
		for (j = 1; j < 65536; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 262144];
			alpha = A[i + j];
			beta = A[i - j + 131072];
			beta1 = A[i + j + 131072] * y1 + A[i - j + 262144] * y0;
			beta2 = A[i + j + 131072] * y0 - A[i - j + 262144] * y1;
			theta += 4;
			A[i + j] = alpha + beta1;
			A[i + j + 131072] = alpha - beta1;
			A[i - j + 131072] = beta + beta2;
			A[i - j + 262144] = beta - beta2;
		}
	}
	for (i = 0; i < 1048576; i += 524288)
	{
		theta = 2;
		alpha = A[i];
		beta = A[i + 262144];
		A[i] = alpha + beta;
		A[i + 262144] = alpha - beta;
		alpha = A[i + 131072];
		beta = A[i + 393216];
		A[i + 131072] = alpha + beta;
		A[i + 393216] = alpha - beta;
		for (j = 1; j < 131072; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 262144];
			alpha = A[i + j];
			beta = A[i - j + 262144];
			beta1 = A[i + j + 262144] * y1 + A[i - j + 524288] * y0;
			beta2 = A[i + j + 262144] * y0 - A[i - j + 524288] * y1;
			theta += 2;
			A[i + j] = alpha + beta1;
			A[i + j + 262144] = alpha - beta1;
			A[i - j + 262144] = beta + beta2;
			A[i - j + 524288] = beta - beta2;
		}
	}
	theta = 1;
	alpha = A[0];
	beta = A[524288];
	A[0] = alpha + beta;
	A[524288] = alpha - beta;
	alpha = A[262144];
	beta = A[786432];
	A[262144] = alpha + beta;
	A[786432] = alpha - beta;
	for (j = 1; j < 262144; j++)
	{
		y0 = sinTab[theta];
		y1 = sinTab[theta + 262144];
		alpha = A[j];
		beta = A[-j + 524288];
		beta1 = A[j + 524288] * y1 + A[-j + 1048576] * y0;
		beta2 = A[j + 524288] * y0 - A[-j + 1048576] * y1;
		theta += 1;
		A[j] = alpha + beta1;
		A[j + 524288] = alpha - beta1;
		A[-j + 524288] = beta + beta2;
		A[-j + 1048576] = beta - beta2;
	}
}
void DFT128(float *A, const float *sinTab)
{
	int i, j, theta;
	float alpha, beta, beta1, beta2, y0, y1, y2, y3;
	for (i = 0; i < 128; i += 4)
	{
		alpha = A[i];
		beta = A[i + 1];
		beta1 = A[i + 2];
		beta2 = A[i + 3];
		y0 = alpha + beta;
		y1 = alpha - beta;
		y2 = beta1 + beta2;
		y3 = beta1 - beta2;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	for (i = 0; i < 128; i += 8)
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
		beta1 = 0.70710678118654752440084436210485f*(A[i + 5] + A[i + 7]);
		beta2 = 0.70710678118654752440084436210485f*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	for (i = 0; i < 128; i += 16)
	{
		theta = 8;
		alpha = A[i];
		beta = A[i + 8];
		A[i] = alpha + beta;
		A[i + 8] = alpha - beta;
		alpha = A[i + 4];
		beta = A[i + 12];
		A[i + 4] = alpha + beta;
		A[i + 12] = alpha - beta;
		for (j = 1; j < 4; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 32];
			alpha = A[i + j];
			beta = A[i - j + 8];
			beta1 = A[i + j + 8] * y1 + A[i - j + 16] * y0;
			beta2 = A[i + j + 8] * y0 - A[i - j + 16] * y1;
			theta += 8;
			A[i + j] = alpha + beta1;
			A[i + j + 8] = alpha - beta1;
			A[i - j + 8] = beta + beta2;
			A[i - j + 16] = beta - beta2;
		}
	}
	for (i = 0; i < 128; i += 32)
	{
		theta = 4;
		alpha = A[i];
		beta = A[i + 16];
		A[i] = alpha + beta;
		A[i + 16] = alpha - beta;
		alpha = A[i + 8];
		beta = A[i + 24];
		A[i + 8] = alpha + beta;
		A[i + 24] = alpha - beta;
		for (j = 1; j < 8; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 32];
			alpha = A[i + j];
			beta = A[i - j + 16];
			beta1 = A[i + j + 16] * y1 + A[i - j + 32] * y0;
			beta2 = A[i + j + 16] * y0 - A[i - j + 32] * y1;
			theta += 4;
			A[i + j] = alpha + beta1;
			A[i + j + 16] = alpha - beta1;
			A[i - j + 16] = beta + beta2;
			A[i - j + 32] = beta - beta2;
		}
	}
	for (i = 0; i < 128; i += 64)
	{
		theta = 2;
		alpha = A[i];
		beta = A[i + 32];
		A[i] = alpha + beta;
		A[i + 32] = alpha - beta;
		alpha = A[i + 16];
		beta = A[i + 48];
		A[i + 16] = alpha + beta;
		A[i + 48] = alpha - beta;
		for (j = 1; j < 16; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 32];
			alpha = A[i + j];
			beta = A[i - j + 32];
			beta1 = A[i + j + 32] * y1 + A[i - j + 64] * y0;
			beta2 = A[i + j + 32] * y0 - A[i - j + 64] * y1;
			theta += 2;
			A[i + j] = alpha + beta1;
			A[i + j + 32] = alpha - beta1;
			A[i - j + 32] = beta + beta2;
			A[i - j + 64] = beta - beta2;
		}
	}
	theta = 1;
	alpha = A[0];
	beta = A[64];
	A[0] = alpha + beta;
	A[64] = alpha - beta;
	alpha = A[32];
	beta = A[96];
	A[32] = alpha + beta;
	A[96] = alpha - beta;
	for (j = 1; j < 32; j++)
	{
		y0 = sinTab[theta];
		y1 = sinTab[theta + 32];
		alpha = A[j];
		beta = A[-j + 64];
		beta1 = A[j + 64] * y1 + A[-j + 128] * y0;
		beta2 = A[j + 64] * y0 - A[-j + 128] * y1;
		theta += 1;
		A[j] = alpha + beta1;
		A[j + 64] = alpha - beta1;
		A[-j + 64] = beta + beta2;
		A[-j + 128] = beta - beta2;
	}
}
void DFT131072(float *A, const float *sinTab)
{
	int i, j, theta;
	float alpha, beta, beta1, beta2, y0, y1, y2, y3;
	for (i = 0; i < 131072; i += 4)
	{
		alpha = A[i];
		beta = A[i + 1];
		beta1 = A[i + 2];
		beta2 = A[i + 3];
		y0 = alpha + beta;
		y1 = alpha - beta;
		y2 = beta1 + beta2;
		y3 = beta1 - beta2;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	for (i = 0; i < 131072; i += 8)
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
		beta1 = 0.70710678118654752440084436210485f*(A[i + 5] + A[i + 7]);
		beta2 = 0.70710678118654752440084436210485f*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	for (i = 0; i < 131072; i += 16)
	{
		theta = 8192;
		alpha = A[i];
		beta = A[i + 8];
		A[i] = alpha + beta;
		A[i + 8] = alpha - beta;
		alpha = A[i + 4];
		beta = A[i + 12];
		A[i + 4] = alpha + beta;
		A[i + 12] = alpha - beta;
		for (j = 1; j < 4; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 32768];
			alpha = A[i + j];
			beta = A[i - j + 8];
			beta1 = A[i + j + 8] * y1 + A[i - j + 16] * y0;
			beta2 = A[i + j + 8] * y0 - A[i - j + 16] * y1;
			theta += 8192;
			A[i + j] = alpha + beta1;
			A[i + j + 8] = alpha - beta1;
			A[i - j + 8] = beta + beta2;
			A[i - j + 16] = beta - beta2;
		}
	}
	for (i = 0; i < 131072; i += 32)
	{
		theta = 4096;
		alpha = A[i];
		beta = A[i + 16];
		A[i] = alpha + beta;
		A[i + 16] = alpha - beta;
		alpha = A[i + 8];
		beta = A[i + 24];
		A[i + 8] = alpha + beta;
		A[i + 24] = alpha - beta;
		for (j = 1; j < 8; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 32768];
			alpha = A[i + j];
			beta = A[i - j + 16];
			beta1 = A[i + j + 16] * y1 + A[i - j + 32] * y0;
			beta2 = A[i + j + 16] * y0 - A[i - j + 32] * y1;
			theta += 4096;
			A[i + j] = alpha + beta1;
			A[i + j + 16] = alpha - beta1;
			A[i - j + 16] = beta + beta2;
			A[i - j + 32] = beta - beta2;
		}
	}
	for (i = 0; i < 131072; i += 64)
	{
		theta = 2048;
		alpha = A[i];
		beta = A[i + 32];
		A[i] = alpha + beta;
		A[i + 32] = alpha - beta;
		alpha = A[i + 16];
		beta = A[i + 48];
		A[i + 16] = alpha + beta;
		A[i + 48] = alpha - beta;
		for (j = 1; j < 16; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 32768];
			alpha = A[i + j];
			beta = A[i - j + 32];
			beta1 = A[i + j + 32] * y1 + A[i - j + 64] * y0;
			beta2 = A[i + j + 32] * y0 - A[i - j + 64] * y1;
			theta += 2048;
			A[i + j] = alpha + beta1;
			A[i + j + 32] = alpha - beta1;
			A[i - j + 32] = beta + beta2;
			A[i - j + 64] = beta - beta2;
		}
	}
	for (i = 0; i < 131072; i += 128)
	{
		theta = 1024;
		alpha = A[i];
		beta = A[i + 64];
		A[i] = alpha + beta;
		A[i + 64] = alpha - beta;
		alpha = A[i + 32];
		beta = A[i + 96];
		A[i + 32] = alpha + beta;
		A[i + 96] = alpha - beta;
		for (j = 1; j < 32; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 32768];
			alpha = A[i + j];
			beta = A[i - j + 64];
			beta1 = A[i + j + 64] * y1 + A[i - j + 128] * y0;
			beta2 = A[i + j + 64] * y0 - A[i - j + 128] * y1;
			theta += 1024;
			A[i + j] = alpha + beta1;
			A[i + j + 64] = alpha - beta1;
			A[i - j + 64] = beta + beta2;
			A[i - j + 128] = beta - beta2;
		}
	}
	for (i = 0; i < 131072; i += 256)
	{
		theta = 512;
		alpha = A[i];
		beta = A[i + 128];
		A[i] = alpha + beta;
		A[i + 128] = alpha - beta;
		alpha = A[i + 64];
		beta = A[i + 192];
		A[i + 64] = alpha + beta;
		A[i + 192] = alpha - beta;
		for (j = 1; j < 64; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 32768];
			alpha = A[i + j];
			beta = A[i - j + 128];
			beta1 = A[i + j + 128] * y1 + A[i - j + 256] * y0;
			beta2 = A[i + j + 128] * y0 - A[i - j + 256] * y1;
			theta += 512;
			A[i + j] = alpha + beta1;
			A[i + j + 128] = alpha - beta1;
			A[i - j + 128] = beta + beta2;
			A[i - j + 256] = beta - beta2;
		}
	}
	for (i = 0; i < 131072; i += 512)
	{
		theta = 256;
		alpha = A[i];
		beta = A[i + 256];
		A[i] = alpha + beta;
		A[i + 256] = alpha - beta;
		alpha = A[i + 128];
		beta = A[i + 384];
		A[i + 128] = alpha + beta;
		A[i + 384] = alpha - beta;
		for (j = 1; j < 128; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 32768];
			alpha = A[i + j];
			beta = A[i - j + 256];
			beta1 = A[i + j + 256] * y1 + A[i - j + 512] * y0;
			beta2 = A[i + j + 256] * y0 - A[i - j + 512] * y1;
			theta += 256;
			A[i + j] = alpha + beta1;
			A[i + j + 256] = alpha - beta1;
			A[i - j + 256] = beta + beta2;
			A[i - j + 512] = beta - beta2;
		}
	}
	for (i = 0; i < 131072; i += 1024)
	{
		theta = 128;
		alpha = A[i];
		beta = A[i + 512];
		A[i] = alpha + beta;
		A[i + 512] = alpha - beta;
		alpha = A[i + 256];
		beta = A[i + 768];
		A[i + 256] = alpha + beta;
		A[i + 768] = alpha - beta;
		for (j = 1; j < 256; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 32768];
			alpha = A[i + j];
			beta = A[i - j + 512];
			beta1 = A[i + j + 512] * y1 + A[i - j + 1024] * y0;
			beta2 = A[i + j + 512] * y0 - A[i - j + 1024] * y1;
			theta += 128;
			A[i + j] = alpha + beta1;
			A[i + j + 512] = alpha - beta1;
			A[i - j + 512] = beta + beta2;
			A[i - j + 1024] = beta - beta2;
		}
	}
	for (i = 0; i < 131072; i += 2048)
	{
		theta = 64;
		alpha = A[i];
		beta = A[i + 1024];
		A[i] = alpha + beta;
		A[i + 1024] = alpha - beta;
		alpha = A[i + 512];
		beta = A[i + 1536];
		A[i + 512] = alpha + beta;
		A[i + 1536] = alpha - beta;
		for (j = 1; j < 512; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 32768];
			alpha = A[i + j];
			beta = A[i - j + 1024];
			beta1 = A[i + j + 1024] * y1 + A[i - j + 2048] * y0;
			beta2 = A[i + j + 1024] * y0 - A[i - j + 2048] * y1;
			theta += 64;
			A[i + j] = alpha + beta1;
			A[i + j + 1024] = alpha - beta1;
			A[i - j + 1024] = beta + beta2;
			A[i - j + 2048] = beta - beta2;
		}
	}
	for (i = 0; i < 131072; i += 4096)
	{
		theta = 32;
		alpha = A[i];
		beta = A[i + 2048];
		A[i] = alpha + beta;
		A[i + 2048] = alpha - beta;
		alpha = A[i + 1024];
		beta = A[i + 3072];
		A[i + 1024] = alpha + beta;
		A[i + 3072] = alpha - beta;
		for (j = 1; j < 1024; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 32768];
			alpha = A[i + j];
			beta = A[i - j + 2048];
			beta1 = A[i + j + 2048] * y1 + A[i - j + 4096] * y0;
			beta2 = A[i + j + 2048] * y0 - A[i - j + 4096] * y1;
			theta += 32;
			A[i + j] = alpha + beta1;
			A[i + j + 2048] = alpha - beta1;
			A[i - j + 2048] = beta + beta2;
			A[i - j + 4096] = beta - beta2;
		}
	}
	for (i = 0; i < 131072; i += 8192)
	{
		theta = 16;
		alpha = A[i];
		beta = A[i + 4096];
		A[i] = alpha + beta;
		A[i + 4096] = alpha - beta;
		alpha = A[i + 2048];
		beta = A[i + 6144];
		A[i + 2048] = alpha + beta;
		A[i + 6144] = alpha - beta;
		for (j = 1; j < 2048; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 32768];
			alpha = A[i + j];
			beta = A[i - j + 4096];
			beta1 = A[i + j + 4096] * y1 + A[i - j + 8192] * y0;
			beta2 = A[i + j + 4096] * y0 - A[i - j + 8192] * y1;
			theta += 16;
			A[i + j] = alpha + beta1;
			A[i + j + 4096] = alpha - beta1;
			A[i - j + 4096] = beta + beta2;
			A[i - j + 8192] = beta - beta2;
		}
	}
	for (i = 0; i < 131072; i += 16384)
	{
		theta = 8;
		alpha = A[i];
		beta = A[i + 8192];
		A[i] = alpha + beta;
		A[i + 8192] = alpha - beta;
		alpha = A[i + 4096];
		beta = A[i + 12288];
		A[i + 4096] = alpha + beta;
		A[i + 12288] = alpha - beta;
		for (j = 1; j < 4096; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 32768];
			alpha = A[i + j];
			beta = A[i - j + 8192];
			beta1 = A[i + j + 8192] * y1 + A[i - j + 16384] * y0;
			beta2 = A[i + j + 8192] * y0 - A[i - j + 16384] * y1;
			theta += 8;
			A[i + j] = alpha + beta1;
			A[i + j + 8192] = alpha - beta1;
			A[i - j + 8192] = beta + beta2;
			A[i - j + 16384] = beta - beta2;
		}
	}
	for (i = 0; i < 131072; i += 32768)
	{
		theta = 4;
		alpha = A[i];
		beta = A[i + 16384];
		A[i] = alpha + beta;
		A[i + 16384] = alpha - beta;
		alpha = A[i + 8192];
		beta = A[i + 24576];
		A[i + 8192] = alpha + beta;
		A[i + 24576] = alpha - beta;
		for (j = 1; j < 8192; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 32768];
			alpha = A[i + j];
			beta = A[i - j + 16384];
			beta1 = A[i + j + 16384] * y1 + A[i - j + 32768] * y0;
			beta2 = A[i + j + 16384] * y0 - A[i - j + 32768] * y1;
			theta += 4;
			A[i + j] = alpha + beta1;
			A[i + j + 16384] = alpha - beta1;
			A[i - j + 16384] = beta + beta2;
			A[i - j + 32768] = beta - beta2;
		}
	}
	for (i = 0; i < 131072; i += 65536)
	{
		theta = 2;
		alpha = A[i];
		beta = A[i + 32768];
		A[i] = alpha + beta;
		A[i + 32768] = alpha - beta;
		alpha = A[i + 16384];
		beta = A[i + 49152];
		A[i + 16384] = alpha + beta;
		A[i + 49152] = alpha - beta;
		for (j = 1; j < 16384; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 32768];
			alpha = A[i + j];
			beta = A[i - j + 32768];
			beta1 = A[i + j + 32768] * y1 + A[i - j + 65536] * y0;
			beta2 = A[i + j + 32768] * y0 - A[i - j + 65536] * y1;
			theta += 2;
			A[i + j] = alpha + beta1;
			A[i + j + 32768] = alpha - beta1;
			A[i - j + 32768] = beta + beta2;
			A[i - j + 65536] = beta - beta2;
		}
	}
	theta = 1;
	alpha = A[0];
	beta = A[65536];
	A[0] = alpha + beta;
	A[65536] = alpha - beta;
	alpha = A[32768];
	beta = A[98304];
	A[32768] = alpha + beta;
	A[98304] = alpha - beta;
	for (j = 1; j < 32768; j++)
	{
		y0 = sinTab[theta];
		y1 = sinTab[theta + 32768];
		alpha = A[j];
		beta = A[-j + 65536];
		beta1 = A[j + 65536] * y1 + A[-j + 131072] * y0;
		beta2 = A[j + 65536] * y0 - A[-j + 131072] * y1;
		theta += 1;
		A[j] = alpha + beta1;
		A[j + 65536] = alpha - beta1;
		A[-j + 65536] = beta + beta2;
		A[-j + 131072] = beta - beta2;
	}
}
void DFT16(float *A, const float *sinTab)
{
	int i, j, theta;
	float alpha, beta, beta1, beta2, y0, y1, y2, y3;
	for (i = 0; i < 16; i += 4)
	{
		alpha = A[i];
		beta = A[i + 1];
		beta1 = A[i + 2];
		beta2 = A[i + 3];
		y0 = alpha + beta;
		y1 = alpha - beta;
		y2 = beta1 + beta2;
		y3 = beta1 - beta2;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	for (i = 0; i < 16; i += 8)
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
		beta1 = 0.70710678118654752440084436210485f*(A[i + 5] + A[i + 7]);
		beta2 = 0.70710678118654752440084436210485f*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	theta = 1;
	alpha = A[0];
	beta = A[8];
	A[0] = alpha + beta;
	A[8] = alpha - beta;
	alpha = A[4];
	beta = A[12];
	A[4] = alpha + beta;
	A[12] = alpha - beta;
	for (j = 1; j < 4; j++)
	{
		y0 = sinTab[theta];
		y1 = sinTab[theta + 4];
		alpha = A[j];
		beta = A[-j + 8];
		beta1 = A[j + 8] * y1 + A[-j + 16] * y0;
		beta2 = A[j + 8] * y0 - A[-j + 16] * y1;
		theta += 1;
		A[j] = alpha + beta1;
		A[j + 8] = alpha - beta1;
		A[-j + 8] = beta + beta2;
		A[-j + 16] = beta - beta2;
	}
}
void DFT16384(float *A, const float *sinTab)
{
	int i, j, theta;
	float alpha, beta, beta1, beta2, y0, y1, y2, y3;
	for (i = 0; i < 16384; i += 4)
	{
		alpha = A[i];
		beta = A[i + 1];
		beta1 = A[i + 2];
		beta2 = A[i + 3];
		y0 = alpha + beta;
		y1 = alpha - beta;
		y2 = beta1 + beta2;
		y3 = beta1 - beta2;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	for (i = 0; i < 16384; i += 8)
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
		beta1 = 0.70710678118654752440084436210485f*(A[i + 5] + A[i + 7]);
		beta2 = 0.70710678118654752440084436210485f*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	for (i = 0; i < 16384; i += 16)
	{
		theta = 1024;
		alpha = A[i];
		beta = A[i + 8];
		A[i] = alpha + beta;
		A[i + 8] = alpha - beta;
		alpha = A[i + 4];
		beta = A[i + 12];
		A[i + 4] = alpha + beta;
		A[i + 12] = alpha - beta;
		for (j = 1; j < 4; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 4096];
			alpha = A[i + j];
			beta = A[i - j + 8];
			beta1 = A[i + j + 8] * y1 + A[i - j + 16] * y0;
			beta2 = A[i + j + 8] * y0 - A[i - j + 16] * y1;
			theta += 1024;
			A[i + j] = alpha + beta1;
			A[i + j + 8] = alpha - beta1;
			A[i - j + 8] = beta + beta2;
			A[i - j + 16] = beta - beta2;
		}
	}
	for (i = 0; i < 16384; i += 32)
	{
		theta = 512;
		alpha = A[i];
		beta = A[i + 16];
		A[i] = alpha + beta;
		A[i + 16] = alpha - beta;
		alpha = A[i + 8];
		beta = A[i + 24];
		A[i + 8] = alpha + beta;
		A[i + 24] = alpha - beta;
		for (j = 1; j < 8; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 4096];
			alpha = A[i + j];
			beta = A[i - j + 16];
			beta1 = A[i + j + 16] * y1 + A[i - j + 32] * y0;
			beta2 = A[i + j + 16] * y0 - A[i - j + 32] * y1;
			theta += 512;
			A[i + j] = alpha + beta1;
			A[i + j + 16] = alpha - beta1;
			A[i - j + 16] = beta + beta2;
			A[i - j + 32] = beta - beta2;
		}
	}
	for (i = 0; i < 16384; i += 64)
	{
		theta = 256;
		alpha = A[i];
		beta = A[i + 32];
		A[i] = alpha + beta;
		A[i + 32] = alpha - beta;
		alpha = A[i + 16];
		beta = A[i + 48];
		A[i + 16] = alpha + beta;
		A[i + 48] = alpha - beta;
		for (j = 1; j < 16; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 4096];
			alpha = A[i + j];
			beta = A[i - j + 32];
			beta1 = A[i + j + 32] * y1 + A[i - j + 64] * y0;
			beta2 = A[i + j + 32] * y0 - A[i - j + 64] * y1;
			theta += 256;
			A[i + j] = alpha + beta1;
			A[i + j + 32] = alpha - beta1;
			A[i - j + 32] = beta + beta2;
			A[i - j + 64] = beta - beta2;
		}
	}
	for (i = 0; i < 16384; i += 128)
	{
		theta = 128;
		alpha = A[i];
		beta = A[i + 64];
		A[i] = alpha + beta;
		A[i + 64] = alpha - beta;
		alpha = A[i + 32];
		beta = A[i + 96];
		A[i + 32] = alpha + beta;
		A[i + 96] = alpha - beta;
		for (j = 1; j < 32; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 4096];
			alpha = A[i + j];
			beta = A[i - j + 64];
			beta1 = A[i + j + 64] * y1 + A[i - j + 128] * y0;
			beta2 = A[i + j + 64] * y0 - A[i - j + 128] * y1;
			theta += 128;
			A[i + j] = alpha + beta1;
			A[i + j + 64] = alpha - beta1;
			A[i - j + 64] = beta + beta2;
			A[i - j + 128] = beta - beta2;
		}
	}
	for (i = 0; i < 16384; i += 256)
	{
		theta = 64;
		alpha = A[i];
		beta = A[i + 128];
		A[i] = alpha + beta;
		A[i + 128] = alpha - beta;
		alpha = A[i + 64];
		beta = A[i + 192];
		A[i + 64] = alpha + beta;
		A[i + 192] = alpha - beta;
		for (j = 1; j < 64; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 4096];
			alpha = A[i + j];
			beta = A[i - j + 128];
			beta1 = A[i + j + 128] * y1 + A[i - j + 256] * y0;
			beta2 = A[i + j + 128] * y0 - A[i - j + 256] * y1;
			theta += 64;
			A[i + j] = alpha + beta1;
			A[i + j + 128] = alpha - beta1;
			A[i - j + 128] = beta + beta2;
			A[i - j + 256] = beta - beta2;
		}
	}
	for (i = 0; i < 16384; i += 512)
	{
		theta = 32;
		alpha = A[i];
		beta = A[i + 256];
		A[i] = alpha + beta;
		A[i + 256] = alpha - beta;
		alpha = A[i + 128];
		beta = A[i + 384];
		A[i + 128] = alpha + beta;
		A[i + 384] = alpha - beta;
		for (j = 1; j < 128; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 4096];
			alpha = A[i + j];
			beta = A[i - j + 256];
			beta1 = A[i + j + 256] * y1 + A[i - j + 512] * y0;
			beta2 = A[i + j + 256] * y0 - A[i - j + 512] * y1;
			theta += 32;
			A[i + j] = alpha + beta1;
			A[i + j + 256] = alpha - beta1;
			A[i - j + 256] = beta + beta2;
			A[i - j + 512] = beta - beta2;
		}
	}
	for (i = 0; i < 16384; i += 1024)
	{
		theta = 16;
		alpha = A[i];
		beta = A[i + 512];
		A[i] = alpha + beta;
		A[i + 512] = alpha - beta;
		alpha = A[i + 256];
		beta = A[i + 768];
		A[i + 256] = alpha + beta;
		A[i + 768] = alpha - beta;
		for (j = 1; j < 256; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 4096];
			alpha = A[i + j];
			beta = A[i - j + 512];
			beta1 = A[i + j + 512] * y1 + A[i - j + 1024] * y0;
			beta2 = A[i + j + 512] * y0 - A[i - j + 1024] * y1;
			theta += 16;
			A[i + j] = alpha + beta1;
			A[i + j + 512] = alpha - beta1;
			A[i - j + 512] = beta + beta2;
			A[i - j + 1024] = beta - beta2;
		}
	}
	for (i = 0; i < 16384; i += 2048)
	{
		theta = 8;
		alpha = A[i];
		beta = A[i + 1024];
		A[i] = alpha + beta;
		A[i + 1024] = alpha - beta;
		alpha = A[i + 512];
		beta = A[i + 1536];
		A[i + 512] = alpha + beta;
		A[i + 1536] = alpha - beta;
		for (j = 1; j < 512; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 4096];
			alpha = A[i + j];
			beta = A[i - j + 1024];
			beta1 = A[i + j + 1024] * y1 + A[i - j + 2048] * y0;
			beta2 = A[i + j + 1024] * y0 - A[i - j + 2048] * y1;
			theta += 8;
			A[i + j] = alpha + beta1;
			A[i + j + 1024] = alpha - beta1;
			A[i - j + 1024] = beta + beta2;
			A[i - j + 2048] = beta - beta2;
		}
	}
	for (i = 0; i < 16384; i += 4096)
	{
		theta = 4;
		alpha = A[i];
		beta = A[i + 2048];
		A[i] = alpha + beta;
		A[i + 2048] = alpha - beta;
		alpha = A[i + 1024];
		beta = A[i + 3072];
		A[i + 1024] = alpha + beta;
		A[i + 3072] = alpha - beta;
		for (j = 1; j < 1024; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 4096];
			alpha = A[i + j];
			beta = A[i - j + 2048];
			beta1 = A[i + j + 2048] * y1 + A[i - j + 4096] * y0;
			beta2 = A[i + j + 2048] * y0 - A[i - j + 4096] * y1;
			theta += 4;
			A[i + j] = alpha + beta1;
			A[i + j + 2048] = alpha - beta1;
			A[i - j + 2048] = beta + beta2;
			A[i - j + 4096] = beta - beta2;
		}
	}
	for (i = 0; i < 16384; i += 8192)
	{
		theta = 2;
		alpha = A[i];
		beta = A[i + 4096];
		A[i] = alpha + beta;
		A[i + 4096] = alpha - beta;
		alpha = A[i + 2048];
		beta = A[i + 6144];
		A[i + 2048] = alpha + beta;
		A[i + 6144] = alpha - beta;
		for (j = 1; j < 2048; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 4096];
			alpha = A[i + j];
			beta = A[i - j + 4096];
			beta1 = A[i + j + 4096] * y1 + A[i - j + 8192] * y0;
			beta2 = A[i + j + 4096] * y0 - A[i - j + 8192] * y1;
			theta += 2;
			A[i + j] = alpha + beta1;
			A[i + j + 4096] = alpha - beta1;
			A[i - j + 4096] = beta + beta2;
			A[i - j + 8192] = beta - beta2;
		}
	}
	theta = 1;
	alpha = A[0];
	beta = A[8192];
	A[0] = alpha + beta;
	A[8192] = alpha - beta;
	alpha = A[4096];
	beta = A[12288];
	A[4096] = alpha + beta;
	A[12288] = alpha - beta;
	for (j = 1; j < 4096; j++)
	{
		y0 = sinTab[theta];
		y1 = sinTab[theta + 4096];
		alpha = A[j];
		beta = A[-j + 8192];
		beta1 = A[j + 8192] * y1 + A[-j + 16384] * y0;
		beta2 = A[j + 8192] * y0 - A[-j + 16384] * y1;
		theta += 1;
		A[j] = alpha + beta1;
		A[j + 8192] = alpha - beta1;
		A[-j + 8192] = beta + beta2;
		A[-j + 16384] = beta - beta2;
	}
}
void DFT2(float *A, const float *sinTab)
{
	float y0 = A[0] + A[1];
	A[1] = A[0] - A[1];
	A[0] = y0;
}
void DFT2048(float *A, const float *sinTab)
{
	int i, j, theta;
	float alpha, beta, beta1, beta2, y0, y1, y2, y3;
	for (i = 0; i < 2048; i += 4)
	{
		alpha = A[i];
		beta = A[i + 1];
		beta1 = A[i + 2];
		beta2 = A[i + 3];
		y0 = alpha + beta;
		y1 = alpha - beta;
		y2 = beta1 + beta2;
		y3 = beta1 - beta2;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	for (i = 0; i < 2048; i += 8)
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
		beta1 = 0.70710678118654752440084436210485f*(A[i + 5] + A[i + 7]);
		beta2 = 0.70710678118654752440084436210485f*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	for (i = 0; i < 2048; i += 16)
	{
		theta = 128;
		alpha = A[i];
		beta = A[i + 8];
		A[i] = alpha + beta;
		A[i + 8] = alpha - beta;
		alpha = A[i + 4];
		beta = A[i + 12];
		A[i + 4] = alpha + beta;
		A[i + 12] = alpha - beta;
		for (j = 1; j < 4; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 512];
			alpha = A[i + j];
			beta = A[i - j + 8];
			beta1 = A[i + j + 8] * y1 + A[i - j + 16] * y0;
			beta2 = A[i + j + 8] * y0 - A[i - j + 16] * y1;
			theta += 128;
			A[i + j] = alpha + beta1;
			A[i + j + 8] = alpha - beta1;
			A[i - j + 8] = beta + beta2;
			A[i - j + 16] = beta - beta2;
		}
	}
	for (i = 0; i < 2048; i += 32)
	{
		theta = 64;
		alpha = A[i];
		beta = A[i + 16];
		A[i] = alpha + beta;
		A[i + 16] = alpha - beta;
		alpha = A[i + 8];
		beta = A[i + 24];
		A[i + 8] = alpha + beta;
		A[i + 24] = alpha - beta;
		for (j = 1; j < 8; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 512];
			alpha = A[i + j];
			beta = A[i - j + 16];
			beta1 = A[i + j + 16] * y1 + A[i - j + 32] * y0;
			beta2 = A[i + j + 16] * y0 - A[i - j + 32] * y1;
			theta += 64;
			A[i + j] = alpha + beta1;
			A[i + j + 16] = alpha - beta1;
			A[i - j + 16] = beta + beta2;
			A[i - j + 32] = beta - beta2;
		}
	}
	for (i = 0; i < 2048; i += 64)
	{
		theta = 32;
		alpha = A[i];
		beta = A[i + 32];
		A[i] = alpha + beta;
		A[i + 32] = alpha - beta;
		alpha = A[i + 16];
		beta = A[i + 48];
		A[i + 16] = alpha + beta;
		A[i + 48] = alpha - beta;
		for (j = 1; j < 16; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 512];
			alpha = A[i + j];
			beta = A[i - j + 32];
			beta1 = A[i + j + 32] * y1 + A[i - j + 64] * y0;
			beta2 = A[i + j + 32] * y0 - A[i - j + 64] * y1;
			theta += 32;
			A[i + j] = alpha + beta1;
			A[i + j + 32] = alpha - beta1;
			A[i - j + 32] = beta + beta2;
			A[i - j + 64] = beta - beta2;
		}
	}
	for (i = 0; i < 2048; i += 128)
	{
		theta = 16;
		alpha = A[i];
		beta = A[i + 64];
		A[i] = alpha + beta;
		A[i + 64] = alpha - beta;
		alpha = A[i + 32];
		beta = A[i + 96];
		A[i + 32] = alpha + beta;
		A[i + 96] = alpha - beta;
		for (j = 1; j < 32; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 512];
			alpha = A[i + j];
			beta = A[i - j + 64];
			beta1 = A[i + j + 64] * y1 + A[i - j + 128] * y0;
			beta2 = A[i + j + 64] * y0 - A[i - j + 128] * y1;
			theta += 16;
			A[i + j] = alpha + beta1;
			A[i + j + 64] = alpha - beta1;
			A[i - j + 64] = beta + beta2;
			A[i - j + 128] = beta - beta2;
		}
	}
	for (i = 0; i < 2048; i += 256)
	{
		theta = 8;
		alpha = A[i];
		beta = A[i + 128];
		A[i] = alpha + beta;
		A[i + 128] = alpha - beta;
		alpha = A[i + 64];
		beta = A[i + 192];
		A[i + 64] = alpha + beta;
		A[i + 192] = alpha - beta;
		for (j = 1; j < 64; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 512];
			alpha = A[i + j];
			beta = A[i - j + 128];
			beta1 = A[i + j + 128] * y1 + A[i - j + 256] * y0;
			beta2 = A[i + j + 128] * y0 - A[i - j + 256] * y1;
			theta += 8;
			A[i + j] = alpha + beta1;
			A[i + j + 128] = alpha - beta1;
			A[i - j + 128] = beta + beta2;
			A[i - j + 256] = beta - beta2;
		}
	}
	for (i = 0; i < 2048; i += 512)
	{
		theta = 4;
		alpha = A[i];
		beta = A[i + 256];
		A[i] = alpha + beta;
		A[i + 256] = alpha - beta;
		alpha = A[i + 128];
		beta = A[i + 384];
		A[i + 128] = alpha + beta;
		A[i + 384] = alpha - beta;
		for (j = 1; j < 128; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 512];
			alpha = A[i + j];
			beta = A[i - j + 256];
			beta1 = A[i + j + 256] * y1 + A[i - j + 512] * y0;
			beta2 = A[i + j + 256] * y0 - A[i - j + 512] * y1;
			theta += 4;
			A[i + j] = alpha + beta1;
			A[i + j + 256] = alpha - beta1;
			A[i - j + 256] = beta + beta2;
			A[i - j + 512] = beta - beta2;
		}
	}
	for (i = 0; i < 2048; i += 1024)
	{
		theta = 2;
		alpha = A[i];
		beta = A[i + 512];
		A[i] = alpha + beta;
		A[i + 512] = alpha - beta;
		alpha = A[i + 256];
		beta = A[i + 768];
		A[i + 256] = alpha + beta;
		A[i + 768] = alpha - beta;
		for (j = 1; j < 256; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 512];
			alpha = A[i + j];
			beta = A[i - j + 512];
			beta1 = A[i + j + 512] * y1 + A[i - j + 1024] * y0;
			beta2 = A[i + j + 512] * y0 - A[i - j + 1024] * y1;
			theta += 2;
			A[i + j] = alpha + beta1;
			A[i + j + 512] = alpha - beta1;
			A[i - j + 512] = beta + beta2;
			A[i - j + 1024] = beta - beta2;
		}
	}
	theta = 1;
	alpha = A[0];
	beta = A[1024];
	A[0] = alpha + beta;
	A[1024] = alpha - beta;
	alpha = A[512];
	beta = A[1536];
	A[512] = alpha + beta;
	A[1536] = alpha - beta;
	for (j = 1; j < 512; j++)
	{
		y0 = sinTab[theta];
		y1 = sinTab[theta + 512];
		alpha = A[j];
		beta = A[-j + 1024];
		beta1 = A[j + 1024] * y1 + A[-j + 2048] * y0;
		beta2 = A[j + 1024] * y0 - A[-j + 2048] * y1;
		theta += 1;
		A[j] = alpha + beta1;
		A[j + 1024] = alpha - beta1;
		A[-j + 1024] = beta + beta2;
		A[-j + 2048] = beta - beta2;
	}
}
void DFT256(float *A, const float *sinTab)
{
	int i, j, theta;
	float alpha, beta, beta1, beta2, y0, y1, y2, y3;
	for (i = 0; i < 256; i += 4)
	{
		alpha = A[i];
		beta = A[i + 1];
		beta1 = A[i + 2];
		beta2 = A[i + 3];
		y0 = alpha + beta;
		y1 = alpha - beta;
		y2 = beta1 + beta2;
		y3 = beta1 - beta2;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	for (i = 0; i < 256; i += 8)
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
		beta1 = 0.70710678118654752440084436210485f*(A[i + 5] + A[i + 7]);
		beta2 = 0.70710678118654752440084436210485f*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	for (i = 0; i < 256; i += 16)
	{
		theta = 16;
		alpha = A[i];
		beta = A[i + 8];
		A[i] = alpha + beta;
		A[i + 8] = alpha - beta;
		alpha = A[i + 4];
		beta = A[i + 12];
		A[i + 4] = alpha + beta;
		A[i + 12] = alpha - beta;
		for (j = 1; j < 4; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 64];
			alpha = A[i + j];
			beta = A[i - j + 8];
			beta1 = A[i + j + 8] * y1 + A[i - j + 16] * y0;
			beta2 = A[i + j + 8] * y0 - A[i - j + 16] * y1;
			theta += 16;
			A[i + j] = alpha + beta1;
			A[i + j + 8] = alpha - beta1;
			A[i - j + 8] = beta + beta2;
			A[i - j + 16] = beta - beta2;
		}
	}
	for (i = 0; i < 256; i += 32)
	{
		theta = 8;
		alpha = A[i];
		beta = A[i + 16];
		A[i] = alpha + beta;
		A[i + 16] = alpha - beta;
		alpha = A[i + 8];
		beta = A[i + 24];
		A[i + 8] = alpha + beta;
		A[i + 24] = alpha - beta;
		for (j = 1; j < 8; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 64];
			alpha = A[i + j];
			beta = A[i - j + 16];
			beta1 = A[i + j + 16] * y1 + A[i - j + 32] * y0;
			beta2 = A[i + j + 16] * y0 - A[i - j + 32] * y1;
			theta += 8;
			A[i + j] = alpha + beta1;
			A[i + j + 16] = alpha - beta1;
			A[i - j + 16] = beta + beta2;
			A[i - j + 32] = beta - beta2;
		}
	}
	for (i = 0; i < 256; i += 64)
	{
		theta = 4;
		alpha = A[i];
		beta = A[i + 32];
		A[i] = alpha + beta;
		A[i + 32] = alpha - beta;
		alpha = A[i + 16];
		beta = A[i + 48];
		A[i + 16] = alpha + beta;
		A[i + 48] = alpha - beta;
		for (j = 1; j < 16; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 64];
			alpha = A[i + j];
			beta = A[i - j + 32];
			beta1 = A[i + j + 32] * y1 + A[i - j + 64] * y0;
			beta2 = A[i + j + 32] * y0 - A[i - j + 64] * y1;
			theta += 4;
			A[i + j] = alpha + beta1;
			A[i + j + 32] = alpha - beta1;
			A[i - j + 32] = beta + beta2;
			A[i - j + 64] = beta - beta2;
		}
	}
	for (i = 0; i < 256; i += 128)
	{
		theta = 2;
		alpha = A[i];
		beta = A[i + 64];
		A[i] = alpha + beta;
		A[i + 64] = alpha - beta;
		alpha = A[i + 32];
		beta = A[i + 96];
		A[i + 32] = alpha + beta;
		A[i + 96] = alpha - beta;
		for (j = 1; j < 32; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 64];
			alpha = A[i + j];
			beta = A[i - j + 64];
			beta1 = A[i + j + 64] * y1 + A[i - j + 128] * y0;
			beta2 = A[i + j + 64] * y0 - A[i - j + 128] * y1;
			theta += 2;
			A[i + j] = alpha + beta1;
			A[i + j + 64] = alpha - beta1;
			A[i - j + 64] = beta + beta2;
			A[i - j + 128] = beta - beta2;
		}
	}
	theta = 1;
	alpha = A[0];
	beta = A[128];
	A[0] = alpha + beta;
	A[128] = alpha - beta;
	alpha = A[64];
	beta = A[192];
	A[64] = alpha + beta;
	A[192] = alpha - beta;
	for (j = 1; j < 64; j++)
	{
		y0 = sinTab[theta];
		y1 = sinTab[theta + 64];
		alpha = A[j];
		beta = A[-j + 128];
		beta1 = A[j + 128] * y1 + A[-j + 256] * y0;
		beta2 = A[j + 128] * y0 - A[-j + 256] * y1;
		theta += 1;
		A[j] = alpha + beta1;
		A[j + 128] = alpha - beta1;
		A[-j + 128] = beta + beta2;
		A[-j + 256] = beta - beta2;
	}
}
void DFT262144(float *A, const float *sinTab)
{
	int i, j, theta;
	float alpha, beta, beta1, beta2, y0, y1, y2, y3;
	for (i = 0; i < 262144; i += 4)
	{
		alpha = A[i];
		beta = A[i + 1];
		beta1 = A[i + 2];
		beta2 = A[i + 3];
		y0 = alpha + beta;
		y1 = alpha - beta;
		y2 = beta1 + beta2;
		y3 = beta1 - beta2;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	for (i = 0; i < 262144; i += 8)
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
		beta1 = 0.70710678118654752440084436210485f*(A[i + 5] + A[i + 7]);
		beta2 = 0.70710678118654752440084436210485f*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	for (i = 0; i < 262144; i += 16)
	{
		theta = 16384;
		alpha = A[i];
		beta = A[i + 8];
		A[i] = alpha + beta;
		A[i + 8] = alpha - beta;
		alpha = A[i + 4];
		beta = A[i + 12];
		A[i + 4] = alpha + beta;
		A[i + 12] = alpha - beta;
		for (j = 1; j < 4; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 65536];
			alpha = A[i + j];
			beta = A[i - j + 8];
			beta1 = A[i + j + 8] * y1 + A[i - j + 16] * y0;
			beta2 = A[i + j + 8] * y0 - A[i - j + 16] * y1;
			theta += 16384;
			A[i + j] = alpha + beta1;
			A[i + j + 8] = alpha - beta1;
			A[i - j + 8] = beta + beta2;
			A[i - j + 16] = beta - beta2;
		}
	}
	for (i = 0; i < 262144; i += 32)
	{
		theta = 8192;
		alpha = A[i];
		beta = A[i + 16];
		A[i] = alpha + beta;
		A[i + 16] = alpha - beta;
		alpha = A[i + 8];
		beta = A[i + 24];
		A[i + 8] = alpha + beta;
		A[i + 24] = alpha - beta;
		for (j = 1; j < 8; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 65536];
			alpha = A[i + j];
			beta = A[i - j + 16];
			beta1 = A[i + j + 16] * y1 + A[i - j + 32] * y0;
			beta2 = A[i + j + 16] * y0 - A[i - j + 32] * y1;
			theta += 8192;
			A[i + j] = alpha + beta1;
			A[i + j + 16] = alpha - beta1;
			A[i - j + 16] = beta + beta2;
			A[i - j + 32] = beta - beta2;
		}
	}
	for (i = 0; i < 262144; i += 64)
	{
		theta = 4096;
		alpha = A[i];
		beta = A[i + 32];
		A[i] = alpha + beta;
		A[i + 32] = alpha - beta;
		alpha = A[i + 16];
		beta = A[i + 48];
		A[i + 16] = alpha + beta;
		A[i + 48] = alpha - beta;
		for (j = 1; j < 16; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 65536];
			alpha = A[i + j];
			beta = A[i - j + 32];
			beta1 = A[i + j + 32] * y1 + A[i - j + 64] * y0;
			beta2 = A[i + j + 32] * y0 - A[i - j + 64] * y1;
			theta += 4096;
			A[i + j] = alpha + beta1;
			A[i + j + 32] = alpha - beta1;
			A[i - j + 32] = beta + beta2;
			A[i - j + 64] = beta - beta2;
		}
	}
	for (i = 0; i < 262144; i += 128)
	{
		theta = 2048;
		alpha = A[i];
		beta = A[i + 64];
		A[i] = alpha + beta;
		A[i + 64] = alpha - beta;
		alpha = A[i + 32];
		beta = A[i + 96];
		A[i + 32] = alpha + beta;
		A[i + 96] = alpha - beta;
		for (j = 1; j < 32; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 65536];
			alpha = A[i + j];
			beta = A[i - j + 64];
			beta1 = A[i + j + 64] * y1 + A[i - j + 128] * y0;
			beta2 = A[i + j + 64] * y0 - A[i - j + 128] * y1;
			theta += 2048;
			A[i + j] = alpha + beta1;
			A[i + j + 64] = alpha - beta1;
			A[i - j + 64] = beta + beta2;
			A[i - j + 128] = beta - beta2;
		}
	}
	for (i = 0; i < 262144; i += 256)
	{
		theta = 1024;
		alpha = A[i];
		beta = A[i + 128];
		A[i] = alpha + beta;
		A[i + 128] = alpha - beta;
		alpha = A[i + 64];
		beta = A[i + 192];
		A[i + 64] = alpha + beta;
		A[i + 192] = alpha - beta;
		for (j = 1; j < 64; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 65536];
			alpha = A[i + j];
			beta = A[i - j + 128];
			beta1 = A[i + j + 128] * y1 + A[i - j + 256] * y0;
			beta2 = A[i + j + 128] * y0 - A[i - j + 256] * y1;
			theta += 1024;
			A[i + j] = alpha + beta1;
			A[i + j + 128] = alpha - beta1;
			A[i - j + 128] = beta + beta2;
			A[i - j + 256] = beta - beta2;
		}
	}
	for (i = 0; i < 262144; i += 512)
	{
		theta = 512;
		alpha = A[i];
		beta = A[i + 256];
		A[i] = alpha + beta;
		A[i + 256] = alpha - beta;
		alpha = A[i + 128];
		beta = A[i + 384];
		A[i + 128] = alpha + beta;
		A[i + 384] = alpha - beta;
		for (j = 1; j < 128; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 65536];
			alpha = A[i + j];
			beta = A[i - j + 256];
			beta1 = A[i + j + 256] * y1 + A[i - j + 512] * y0;
			beta2 = A[i + j + 256] * y0 - A[i - j + 512] * y1;
			theta += 512;
			A[i + j] = alpha + beta1;
			A[i + j + 256] = alpha - beta1;
			A[i - j + 256] = beta + beta2;
			A[i - j + 512] = beta - beta2;
		}
	}
	for (i = 0; i < 262144; i += 1024)
	{
		theta = 256;
		alpha = A[i];
		beta = A[i + 512];
		A[i] = alpha + beta;
		A[i + 512] = alpha - beta;
		alpha = A[i + 256];
		beta = A[i + 768];
		A[i + 256] = alpha + beta;
		A[i + 768] = alpha - beta;
		for (j = 1; j < 256; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 65536];
			alpha = A[i + j];
			beta = A[i - j + 512];
			beta1 = A[i + j + 512] * y1 + A[i - j + 1024] * y0;
			beta2 = A[i + j + 512] * y0 - A[i - j + 1024] * y1;
			theta += 256;
			A[i + j] = alpha + beta1;
			A[i + j + 512] = alpha - beta1;
			A[i - j + 512] = beta + beta2;
			A[i - j + 1024] = beta - beta2;
		}
	}
	for (i = 0; i < 262144; i += 2048)
	{
		theta = 128;
		alpha = A[i];
		beta = A[i + 1024];
		A[i] = alpha + beta;
		A[i + 1024] = alpha - beta;
		alpha = A[i + 512];
		beta = A[i + 1536];
		A[i + 512] = alpha + beta;
		A[i + 1536] = alpha - beta;
		for (j = 1; j < 512; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 65536];
			alpha = A[i + j];
			beta = A[i - j + 1024];
			beta1 = A[i + j + 1024] * y1 + A[i - j + 2048] * y0;
			beta2 = A[i + j + 1024] * y0 - A[i - j + 2048] * y1;
			theta += 128;
			A[i + j] = alpha + beta1;
			A[i + j + 1024] = alpha - beta1;
			A[i - j + 1024] = beta + beta2;
			A[i - j + 2048] = beta - beta2;
		}
	}
	for (i = 0; i < 262144; i += 4096)
	{
		theta = 64;
		alpha = A[i];
		beta = A[i + 2048];
		A[i] = alpha + beta;
		A[i + 2048] = alpha - beta;
		alpha = A[i + 1024];
		beta = A[i + 3072];
		A[i + 1024] = alpha + beta;
		A[i + 3072] = alpha - beta;
		for (j = 1; j < 1024; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 65536];
			alpha = A[i + j];
			beta = A[i - j + 2048];
			beta1 = A[i + j + 2048] * y1 + A[i - j + 4096] * y0;
			beta2 = A[i + j + 2048] * y0 - A[i - j + 4096] * y1;
			theta += 64;
			A[i + j] = alpha + beta1;
			A[i + j + 2048] = alpha - beta1;
			A[i - j + 2048] = beta + beta2;
			A[i - j + 4096] = beta - beta2;
		}
	}
	for (i = 0; i < 262144; i += 8192)
	{
		theta = 32;
		alpha = A[i];
		beta = A[i + 4096];
		A[i] = alpha + beta;
		A[i + 4096] = alpha - beta;
		alpha = A[i + 2048];
		beta = A[i + 6144];
		A[i + 2048] = alpha + beta;
		A[i + 6144] = alpha - beta;
		for (j = 1; j < 2048; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 65536];
			alpha = A[i + j];
			beta = A[i - j + 4096];
			beta1 = A[i + j + 4096] * y1 + A[i - j + 8192] * y0;
			beta2 = A[i + j + 4096] * y0 - A[i - j + 8192] * y1;
			theta += 32;
			A[i + j] = alpha + beta1;
			A[i + j + 4096] = alpha - beta1;
			A[i - j + 4096] = beta + beta2;
			A[i - j + 8192] = beta - beta2;
		}
	}
	for (i = 0; i < 262144; i += 16384)
	{
		theta = 16;
		alpha = A[i];
		beta = A[i + 8192];
		A[i] = alpha + beta;
		A[i + 8192] = alpha - beta;
		alpha = A[i + 4096];
		beta = A[i + 12288];
		A[i + 4096] = alpha + beta;
		A[i + 12288] = alpha - beta;
		for (j = 1; j < 4096; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 65536];
			alpha = A[i + j];
			beta = A[i - j + 8192];
			beta1 = A[i + j + 8192] * y1 + A[i - j + 16384] * y0;
			beta2 = A[i + j + 8192] * y0 - A[i - j + 16384] * y1;
			theta += 16;
			A[i + j] = alpha + beta1;
			A[i + j + 8192] = alpha - beta1;
			A[i - j + 8192] = beta + beta2;
			A[i - j + 16384] = beta - beta2;
		}
	}
	for (i = 0; i < 262144; i += 32768)
	{
		theta = 8;
		alpha = A[i];
		beta = A[i + 16384];
		A[i] = alpha + beta;
		A[i + 16384] = alpha - beta;
		alpha = A[i + 8192];
		beta = A[i + 24576];
		A[i + 8192] = alpha + beta;
		A[i + 24576] = alpha - beta;
		for (j = 1; j < 8192; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 65536];
			alpha = A[i + j];
			beta = A[i - j + 16384];
			beta1 = A[i + j + 16384] * y1 + A[i - j + 32768] * y0;
			beta2 = A[i + j + 16384] * y0 - A[i - j + 32768] * y1;
			theta += 8;
			A[i + j] = alpha + beta1;
			A[i + j + 16384] = alpha - beta1;
			A[i - j + 16384] = beta + beta2;
			A[i - j + 32768] = beta - beta2;
		}
	}
	for (i = 0; i < 262144; i += 65536)
	{
		theta = 4;
		alpha = A[i];
		beta = A[i + 32768];
		A[i] = alpha + beta;
		A[i + 32768] = alpha - beta;
		alpha = A[i + 16384];
		beta = A[i + 49152];
		A[i + 16384] = alpha + beta;
		A[i + 49152] = alpha - beta;
		for (j = 1; j < 16384; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 65536];
			alpha = A[i + j];
			beta = A[i - j + 32768];
			beta1 = A[i + j + 32768] * y1 + A[i - j + 65536] * y0;
			beta2 = A[i + j + 32768] * y0 - A[i - j + 65536] * y1;
			theta += 4;
			A[i + j] = alpha + beta1;
			A[i + j + 32768] = alpha - beta1;
			A[i - j + 32768] = beta + beta2;
			A[i - j + 65536] = beta - beta2;
		}
	}
	for (i = 0; i < 262144; i += 131072)
	{
		theta = 2;
		alpha = A[i];
		beta = A[i + 65536];
		A[i] = alpha + beta;
		A[i + 65536] = alpha - beta;
		alpha = A[i + 32768];
		beta = A[i + 98304];
		A[i + 32768] = alpha + beta;
		A[i + 98304] = alpha - beta;
		for (j = 1; j < 32768; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 65536];
			alpha = A[i + j];
			beta = A[i - j + 65536];
			beta1 = A[i + j + 65536] * y1 + A[i - j + 131072] * y0;
			beta2 = A[i + j + 65536] * y0 - A[i - j + 131072] * y1;
			theta += 2;
			A[i + j] = alpha + beta1;
			A[i + j + 65536] = alpha - beta1;
			A[i - j + 65536] = beta + beta2;
			A[i - j + 131072] = beta - beta2;
		}
	}
	theta = 1;
	alpha = A[0];
	beta = A[131072];
	A[0] = alpha + beta;
	A[131072] = alpha - beta;
	alpha = A[65536];
	beta = A[196608];
	A[65536] = alpha + beta;
	A[196608] = alpha - beta;
	for (j = 1; j < 65536; j++)
	{
		y0 = sinTab[theta];
		y1 = sinTab[theta + 65536];
		alpha = A[j];
		beta = A[-j + 131072];
		beta1 = A[j + 131072] * y1 + A[-j + 262144] * y0;
		beta2 = A[j + 131072] * y0 - A[-j + 262144] * y1;
		theta += 1;
		A[j] = alpha + beta1;
		A[j + 131072] = alpha - beta1;
		A[-j + 131072] = beta + beta2;
		A[-j + 262144] = beta - beta2;
	}
}
void DFT32(float *A, const float *sinTab)
{
	int i, j, theta;
	float alpha, beta, beta1, beta2, y0, y1, y2, y3;
	for (i = 0; i < 32; i += 4)
	{
		alpha = A[i];
		beta = A[i + 1];
		beta1 = A[i + 2];
		beta2 = A[i + 3];
		y0 = alpha + beta;
		y1 = alpha - beta;
		y2 = beta1 + beta2;
		y3 = beta1 - beta2;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	for (i = 0; i < 32; i += 8)
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
		beta1 = 0.70710678118654752440084436210485f*(A[i + 5] + A[i + 7]);
		beta2 = 0.70710678118654752440084436210485f*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	for (i = 0; i < 32; i += 16)
	{
		theta = 2;
		alpha = A[i];
		beta = A[i + 8];
		A[i] = alpha + beta;
		A[i + 8] = alpha - beta;
		alpha = A[i + 4];
		beta = A[i + 12];
		A[i + 4] = alpha + beta;
		A[i + 12] = alpha - beta;
		for (j = 1; j < 4; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 8];
			alpha = A[i + j];
			beta = A[i - j + 8];
			beta1 = A[i + j + 8] * y1 + A[i - j + 16] * y0;
			beta2 = A[i + j + 8] * y0 - A[i - j + 16] * y1;
			theta += 2;
			A[i + j] = alpha + beta1;
			A[i + j + 8] = alpha - beta1;
			A[i - j + 8] = beta + beta2;
			A[i - j + 16] = beta - beta2;
		}
	}
	theta = 1;
	alpha = A[0];
	beta = A[16];
	A[0] = alpha + beta;
	A[16] = alpha - beta;
	alpha = A[8];
	beta = A[24];
	A[8] = alpha + beta;
	A[24] = alpha - beta;
	for (j = 1; j < 8; j++)
	{
		y0 = sinTab[theta];
		y1 = sinTab[theta + 8];
		alpha = A[j];
		beta = A[-j + 16];
		beta1 = A[j + 16] * y1 + A[-j + 32] * y0;
		beta2 = A[j + 16] * y0 - A[-j + 32] * y1;
		theta += 1;
		A[j] = alpha + beta1;
		A[j + 16] = alpha - beta1;
		A[-j + 16] = beta + beta2;
		A[-j + 32] = beta - beta2;
	}
}
void DFT32768(float *A, const float *sinTab)
{
	int i, j, theta;
	float alpha, beta, beta1, beta2, y0, y1, y2, y3;
	for (i = 0; i < 32768; i += 4)
	{
		alpha = A[i];
		beta = A[i + 1];
		beta1 = A[i + 2];
		beta2 = A[i + 3];
		y0 = alpha + beta;
		y1 = alpha - beta;
		y2 = beta1 + beta2;
		y3 = beta1 - beta2;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	for (i = 0; i < 32768; i += 8)
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
		beta1 = 0.70710678118654752440084436210485f*(A[i + 5] + A[i + 7]);
		beta2 = 0.70710678118654752440084436210485f*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	for (i = 0; i < 32768; i += 16)
	{
		theta = 2048;
		alpha = A[i];
		beta = A[i + 8];
		A[i] = alpha + beta;
		A[i + 8] = alpha - beta;
		alpha = A[i + 4];
		beta = A[i + 12];
		A[i + 4] = alpha + beta;
		A[i + 12] = alpha - beta;
		for (j = 1; j < 4; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 8192];
			alpha = A[i + j];
			beta = A[i - j + 8];
			beta1 = A[i + j + 8] * y1 + A[i - j + 16] * y0;
			beta2 = A[i + j + 8] * y0 - A[i - j + 16] * y1;
			theta += 2048;
			A[i + j] = alpha + beta1;
			A[i + j + 8] = alpha - beta1;
			A[i - j + 8] = beta + beta2;
			A[i - j + 16] = beta - beta2;
		}
	}
	for (i = 0; i < 32768; i += 32)
	{
		theta = 1024;
		alpha = A[i];
		beta = A[i + 16];
		A[i] = alpha + beta;
		A[i + 16] = alpha - beta;
		alpha = A[i + 8];
		beta = A[i + 24];
		A[i + 8] = alpha + beta;
		A[i + 24] = alpha - beta;
		for (j = 1; j < 8; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 8192];
			alpha = A[i + j];
			beta = A[i - j + 16];
			beta1 = A[i + j + 16] * y1 + A[i - j + 32] * y0;
			beta2 = A[i + j + 16] * y0 - A[i - j + 32] * y1;
			theta += 1024;
			A[i + j] = alpha + beta1;
			A[i + j + 16] = alpha - beta1;
			A[i - j + 16] = beta + beta2;
			A[i - j + 32] = beta - beta2;
		}
	}
	for (i = 0; i < 32768; i += 64)
	{
		theta = 512;
		alpha = A[i];
		beta = A[i + 32];
		A[i] = alpha + beta;
		A[i + 32] = alpha - beta;
		alpha = A[i + 16];
		beta = A[i + 48];
		A[i + 16] = alpha + beta;
		A[i + 48] = alpha - beta;
		for (j = 1; j < 16; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 8192];
			alpha = A[i + j];
			beta = A[i - j + 32];
			beta1 = A[i + j + 32] * y1 + A[i - j + 64] * y0;
			beta2 = A[i + j + 32] * y0 - A[i - j + 64] * y1;
			theta += 512;
			A[i + j] = alpha + beta1;
			A[i + j + 32] = alpha - beta1;
			A[i - j + 32] = beta + beta2;
			A[i - j + 64] = beta - beta2;
		}
	}
	for (i = 0; i < 32768; i += 128)
	{
		theta = 256;
		alpha = A[i];
		beta = A[i + 64];
		A[i] = alpha + beta;
		A[i + 64] = alpha - beta;
		alpha = A[i + 32];
		beta = A[i + 96];
		A[i + 32] = alpha + beta;
		A[i + 96] = alpha - beta;
		for (j = 1; j < 32; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 8192];
			alpha = A[i + j];
			beta = A[i - j + 64];
			beta1 = A[i + j + 64] * y1 + A[i - j + 128] * y0;
			beta2 = A[i + j + 64] * y0 - A[i - j + 128] * y1;
			theta += 256;
			A[i + j] = alpha + beta1;
			A[i + j + 64] = alpha - beta1;
			A[i - j + 64] = beta + beta2;
			A[i - j + 128] = beta - beta2;
		}
	}
	for (i = 0; i < 32768; i += 256)
	{
		theta = 128;
		alpha = A[i];
		beta = A[i + 128];
		A[i] = alpha + beta;
		A[i + 128] = alpha - beta;
		alpha = A[i + 64];
		beta = A[i + 192];
		A[i + 64] = alpha + beta;
		A[i + 192] = alpha - beta;
		for (j = 1; j < 64; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 8192];
			alpha = A[i + j];
			beta = A[i - j + 128];
			beta1 = A[i + j + 128] * y1 + A[i - j + 256] * y0;
			beta2 = A[i + j + 128] * y0 - A[i - j + 256] * y1;
			theta += 128;
			A[i + j] = alpha + beta1;
			A[i + j + 128] = alpha - beta1;
			A[i - j + 128] = beta + beta2;
			A[i - j + 256] = beta - beta2;
		}
	}
	for (i = 0; i < 32768; i += 512)
	{
		theta = 64;
		alpha = A[i];
		beta = A[i + 256];
		A[i] = alpha + beta;
		A[i + 256] = alpha - beta;
		alpha = A[i + 128];
		beta = A[i + 384];
		A[i + 128] = alpha + beta;
		A[i + 384] = alpha - beta;
		for (j = 1; j < 128; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 8192];
			alpha = A[i + j];
			beta = A[i - j + 256];
			beta1 = A[i + j + 256] * y1 + A[i - j + 512] * y0;
			beta2 = A[i + j + 256] * y0 - A[i - j + 512] * y1;
			theta += 64;
			A[i + j] = alpha + beta1;
			A[i + j + 256] = alpha - beta1;
			A[i - j + 256] = beta + beta2;
			A[i - j + 512] = beta - beta2;
		}
	}
	for (i = 0; i < 32768; i += 1024)
	{
		theta = 32;
		alpha = A[i];
		beta = A[i + 512];
		A[i] = alpha + beta;
		A[i + 512] = alpha - beta;
		alpha = A[i + 256];
		beta = A[i + 768];
		A[i + 256] = alpha + beta;
		A[i + 768] = alpha - beta;
		for (j = 1; j < 256; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 8192];
			alpha = A[i + j];
			beta = A[i - j + 512];
			beta1 = A[i + j + 512] * y1 + A[i - j + 1024] * y0;
			beta2 = A[i + j + 512] * y0 - A[i - j + 1024] * y1;
			theta += 32;
			A[i + j] = alpha + beta1;
			A[i + j + 512] = alpha - beta1;
			A[i - j + 512] = beta + beta2;
			A[i - j + 1024] = beta - beta2;
		}
	}
	for (i = 0; i < 32768; i += 2048)
	{
		theta = 16;
		alpha = A[i];
		beta = A[i + 1024];
		A[i] = alpha + beta;
		A[i + 1024] = alpha - beta;
		alpha = A[i + 512];
		beta = A[i + 1536];
		A[i + 512] = alpha + beta;
		A[i + 1536] = alpha - beta;
		for (j = 1; j < 512; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 8192];
			alpha = A[i + j];
			beta = A[i - j + 1024];
			beta1 = A[i + j + 1024] * y1 + A[i - j + 2048] * y0;
			beta2 = A[i + j + 1024] * y0 - A[i - j + 2048] * y1;
			theta += 16;
			A[i + j] = alpha + beta1;
			A[i + j + 1024] = alpha - beta1;
			A[i - j + 1024] = beta + beta2;
			A[i - j + 2048] = beta - beta2;
		}
	}
	for (i = 0; i < 32768; i += 4096)
	{
		theta = 8;
		alpha = A[i];
		beta = A[i + 2048];
		A[i] = alpha + beta;
		A[i + 2048] = alpha - beta;
		alpha = A[i + 1024];
		beta = A[i + 3072];
		A[i + 1024] = alpha + beta;
		A[i + 3072] = alpha - beta;
		for (j = 1; j < 1024; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 8192];
			alpha = A[i + j];
			beta = A[i - j + 2048];
			beta1 = A[i + j + 2048] * y1 + A[i - j + 4096] * y0;
			beta2 = A[i + j + 2048] * y0 - A[i - j + 4096] * y1;
			theta += 8;
			A[i + j] = alpha + beta1;
			A[i + j + 2048] = alpha - beta1;
			A[i - j + 2048] = beta + beta2;
			A[i - j + 4096] = beta - beta2;
		}
	}
	for (i = 0; i < 32768; i += 8192)
	{
		theta = 4;
		alpha = A[i];
		beta = A[i + 4096];
		A[i] = alpha + beta;
		A[i + 4096] = alpha - beta;
		alpha = A[i + 2048];
		beta = A[i + 6144];
		A[i + 2048] = alpha + beta;
		A[i + 6144] = alpha - beta;
		for (j = 1; j < 2048; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 8192];
			alpha = A[i + j];
			beta = A[i - j + 4096];
			beta1 = A[i + j + 4096] * y1 + A[i - j + 8192] * y0;
			beta2 = A[i + j + 4096] * y0 - A[i - j + 8192] * y1;
			theta += 4;
			A[i + j] = alpha + beta1;
			A[i + j + 4096] = alpha - beta1;
			A[i - j + 4096] = beta + beta2;
			A[i - j + 8192] = beta - beta2;
		}
	}
	for (i = 0; i < 32768; i += 16384)
	{
		theta = 2;
		alpha = A[i];
		beta = A[i + 8192];
		A[i] = alpha + beta;
		A[i + 8192] = alpha - beta;
		alpha = A[i + 4096];
		beta = A[i + 12288];
		A[i + 4096] = alpha + beta;
		A[i + 12288] = alpha - beta;
		for (j = 1; j < 4096; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 8192];
			alpha = A[i + j];
			beta = A[i - j + 8192];
			beta1 = A[i + j + 8192] * y1 + A[i - j + 16384] * y0;
			beta2 = A[i + j + 8192] * y0 - A[i - j + 16384] * y1;
			theta += 2;
			A[i + j] = alpha + beta1;
			A[i + j + 8192] = alpha - beta1;
			A[i - j + 8192] = beta + beta2;
			A[i - j + 16384] = beta - beta2;
		}
	}
	theta = 1;
	alpha = A[0];
	beta = A[16384];
	A[0] = alpha + beta;
	A[16384] = alpha - beta;
	alpha = A[8192];
	beta = A[24576];
	A[8192] = alpha + beta;
	A[24576] = alpha - beta;
	for (j = 1; j < 8192; j++)
	{
		y0 = sinTab[theta];
		y1 = sinTab[theta + 8192];
		alpha = A[j];
		beta = A[-j + 16384];
		beta1 = A[j + 16384] * y1 + A[-j + 32768] * y0;
		beta2 = A[j + 16384] * y0 - A[-j + 32768] * y1;
		theta += 1;
		A[j] = alpha + beta1;
		A[j + 16384] = alpha - beta1;
		A[-j + 16384] = beta + beta2;
		A[-j + 32768] = beta - beta2;
	}
}
void DFT4(float *A, const float *sinTab)
{
	float y0, y1, y2, y3;
	y0 = A[0] + A[1];
	y1 = A[0] - A[1];
	y2 = A[2] + A[3];
	y3 = A[2] - A[3];
	A[0] = y0 + y2;
	A[2] = y0 - y2;
	A[1] = y1 + y3;
	A[3] = y1 - y3;
}
void DFT4096(float *A, const float *sinTab)
{
	int i, j, theta;
	float alpha, beta, beta1, beta2, y0, y1, y2, y3;
	for (i = 0; i < 4096; i += 4)
	{
		alpha = A[i];
		beta = A[i + 1];
		beta1 = A[i + 2];
		beta2 = A[i + 3];
		y0 = alpha + beta;
		y1 = alpha - beta;
		y2 = beta1 + beta2;
		y3 = beta1 - beta2;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	for (i = 0; i < 4096; i += 8)
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
		beta1 = 0.70710678118654752440084436210485f*(A[i + 5] + A[i + 7]);
		beta2 = 0.70710678118654752440084436210485f*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	for (i = 0; i < 4096; i += 16)
	{
		theta = 256;
		alpha = A[i];
		beta = A[i + 8];
		A[i] = alpha + beta;
		A[i + 8] = alpha - beta;
		alpha = A[i + 4];
		beta = A[i + 12];
		A[i + 4] = alpha + beta;
		A[i + 12] = alpha - beta;
		for (j = 1; j < 4; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 1024];
			alpha = A[i + j];
			beta = A[i - j + 8];
			beta1 = A[i + j + 8] * y1 + A[i - j + 16] * y0;
			beta2 = A[i + j + 8] * y0 - A[i - j + 16] * y1;
			theta += 256;
			A[i + j] = alpha + beta1;
			A[i + j + 8] = alpha - beta1;
			A[i - j + 8] = beta + beta2;
			A[i - j + 16] = beta - beta2;
		}
	}
	for (i = 0; i < 4096; i += 32)
	{
		theta = 128;
		alpha = A[i];
		beta = A[i + 16];
		A[i] = alpha + beta;
		A[i + 16] = alpha - beta;
		alpha = A[i + 8];
		beta = A[i + 24];
		A[i + 8] = alpha + beta;
		A[i + 24] = alpha - beta;
		for (j = 1; j < 8; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 1024];
			alpha = A[i + j];
			beta = A[i - j + 16];
			beta1 = A[i + j + 16] * y1 + A[i - j + 32] * y0;
			beta2 = A[i + j + 16] * y0 - A[i - j + 32] * y1;
			theta += 128;
			A[i + j] = alpha + beta1;
			A[i + j + 16] = alpha - beta1;
			A[i - j + 16] = beta + beta2;
			A[i - j + 32] = beta - beta2;
		}
	}
	for (i = 0; i < 4096; i += 64)
	{
		theta = 64;
		alpha = A[i];
		beta = A[i + 32];
		A[i] = alpha + beta;
		A[i + 32] = alpha - beta;
		alpha = A[i + 16];
		beta = A[i + 48];
		A[i + 16] = alpha + beta;
		A[i + 48] = alpha - beta;
		for (j = 1; j < 16; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 1024];
			alpha = A[i + j];
			beta = A[i - j + 32];
			beta1 = A[i + j + 32] * y1 + A[i - j + 64] * y0;
			beta2 = A[i + j + 32] * y0 - A[i - j + 64] * y1;
			theta += 64;
			A[i + j] = alpha + beta1;
			A[i + j + 32] = alpha - beta1;
			A[i - j + 32] = beta + beta2;
			A[i - j + 64] = beta - beta2;
		}
	}
	for (i = 0; i < 4096; i += 128)
	{
		theta = 32;
		alpha = A[i];
		beta = A[i + 64];
		A[i] = alpha + beta;
		A[i + 64] = alpha - beta;
		alpha = A[i + 32];
		beta = A[i + 96];
		A[i + 32] = alpha + beta;
		A[i + 96] = alpha - beta;
		for (j = 1; j < 32; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 1024];
			alpha = A[i + j];
			beta = A[i - j + 64];
			beta1 = A[i + j + 64] * y1 + A[i - j + 128] * y0;
			beta2 = A[i + j + 64] * y0 - A[i - j + 128] * y1;
			theta += 32;
			A[i + j] = alpha + beta1;
			A[i + j + 64] = alpha - beta1;
			A[i - j + 64] = beta + beta2;
			A[i - j + 128] = beta - beta2;
		}
	}
	for (i = 0; i < 4096; i += 256)
	{
		theta = 16;
		alpha = A[i];
		beta = A[i + 128];
		A[i] = alpha + beta;
		A[i + 128] = alpha - beta;
		alpha = A[i + 64];
		beta = A[i + 192];
		A[i + 64] = alpha + beta;
		A[i + 192] = alpha - beta;
		for (j = 1; j < 64; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 1024];
			alpha = A[i + j];
			beta = A[i - j + 128];
			beta1 = A[i + j + 128] * y1 + A[i - j + 256] * y0;
			beta2 = A[i + j + 128] * y0 - A[i - j + 256] * y1;
			theta += 16;
			A[i + j] = alpha + beta1;
			A[i + j + 128] = alpha - beta1;
			A[i - j + 128] = beta + beta2;
			A[i - j + 256] = beta - beta2;
		}
	}
	for (i = 0; i < 4096; i += 512)
	{
		theta = 8;
		alpha = A[i];
		beta = A[i + 256];
		A[i] = alpha + beta;
		A[i + 256] = alpha - beta;
		alpha = A[i + 128];
		beta = A[i + 384];
		A[i + 128] = alpha + beta;
		A[i + 384] = alpha - beta;
		for (j = 1; j < 128; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 1024];
			alpha = A[i + j];
			beta = A[i - j + 256];
			beta1 = A[i + j + 256] * y1 + A[i - j + 512] * y0;
			beta2 = A[i + j + 256] * y0 - A[i - j + 512] * y1;
			theta += 8;
			A[i + j] = alpha + beta1;
			A[i + j + 256] = alpha - beta1;
			A[i - j + 256] = beta + beta2;
			A[i - j + 512] = beta - beta2;
		}
	}
	for (i = 0; i < 4096; i += 1024)
	{
		theta = 4;
		alpha = A[i];
		beta = A[i + 512];
		A[i] = alpha + beta;
		A[i + 512] = alpha - beta;
		alpha = A[i + 256];
		beta = A[i + 768];
		A[i + 256] = alpha + beta;
		A[i + 768] = alpha - beta;
		for (j = 1; j < 256; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 1024];
			alpha = A[i + j];
			beta = A[i - j + 512];
			beta1 = A[i + j + 512] * y1 + A[i - j + 1024] * y0;
			beta2 = A[i + j + 512] * y0 - A[i - j + 1024] * y1;
			theta += 4;
			A[i + j] = alpha + beta1;
			A[i + j + 512] = alpha - beta1;
			A[i - j + 512] = beta + beta2;
			A[i - j + 1024] = beta - beta2;
		}
	}
	for (i = 0; i < 4096; i += 2048)
	{
		theta = 2;
		alpha = A[i];
		beta = A[i + 1024];
		A[i] = alpha + beta;
		A[i + 1024] = alpha - beta;
		alpha = A[i + 512];
		beta = A[i + 1536];
		A[i + 512] = alpha + beta;
		A[i + 1536] = alpha - beta;
		for (j = 1; j < 512; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 1024];
			alpha = A[i + j];
			beta = A[i - j + 1024];
			beta1 = A[i + j + 1024] * y1 + A[i - j + 2048] * y0;
			beta2 = A[i + j + 1024] * y0 - A[i - j + 2048] * y1;
			theta += 2;
			A[i + j] = alpha + beta1;
			A[i + j + 1024] = alpha - beta1;
			A[i - j + 1024] = beta + beta2;
			A[i - j + 2048] = beta - beta2;
		}
	}
	theta = 1;
	alpha = A[0];
	beta = A[2048];
	A[0] = alpha + beta;
	A[2048] = alpha - beta;
	alpha = A[1024];
	beta = A[3072];
	A[1024] = alpha + beta;
	A[3072] = alpha - beta;
	for (j = 1; j < 1024; j++)
	{
		y0 = sinTab[theta];
		y1 = sinTab[theta + 1024];
		alpha = A[j];
		beta = A[-j + 2048];
		beta1 = A[j + 2048] * y1 + A[-j + 4096] * y0;
		beta2 = A[j + 2048] * y0 - A[-j + 4096] * y1;
		theta += 1;
		A[j] = alpha + beta1;
		A[j + 2048] = alpha - beta1;
		A[-j + 2048] = beta + beta2;
		A[-j + 4096] = beta - beta2;
	}
}
void DFT512(float *A, const float *sinTab)
{
	int i, j, theta;
	float alpha, beta, beta1, beta2, y0, y1, y2, y3;
	for (i = 0; i < 512; i += 4)
	{
		alpha = A[i];
		beta = A[i + 1];
		beta1 = A[i + 2];
		beta2 = A[i + 3];
		y0 = alpha + beta;
		y1 = alpha - beta;
		y2 = beta1 + beta2;
		y3 = beta1 - beta2;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	for (i = 0; i < 512; i += 8)
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
		beta1 = 0.70710678118654752440084436210485f*(A[i + 5] + A[i + 7]);
		beta2 = 0.70710678118654752440084436210485f*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	for (i = 0; i < 512; i += 16)
	{
		theta = 32;
		alpha = A[i];
		beta = A[i + 8];
		A[i] = alpha + beta;
		A[i + 8] = alpha - beta;
		alpha = A[i + 4];
		beta = A[i + 12];
		A[i + 4] = alpha + beta;
		A[i + 12] = alpha - beta;
		for (j = 1; j < 4; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 128];
			alpha = A[i + j];
			beta = A[i - j + 8];
			beta1 = A[i + j + 8] * y1 + A[i - j + 16] * y0;
			beta2 = A[i + j + 8] * y0 - A[i - j + 16] * y1;
			theta += 32;
			A[i + j] = alpha + beta1;
			A[i + j + 8] = alpha - beta1;
			A[i - j + 8] = beta + beta2;
			A[i - j + 16] = beta - beta2;
		}
	}
	for (i = 0; i < 512; i += 32)
	{
		theta = 16;
		alpha = A[i];
		beta = A[i + 16];
		A[i] = alpha + beta;
		A[i + 16] = alpha - beta;
		alpha = A[i + 8];
		beta = A[i + 24];
		A[i + 8] = alpha + beta;
		A[i + 24] = alpha - beta;
		for (j = 1; j < 8; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 128];
			alpha = A[i + j];
			beta = A[i - j + 16];
			beta1 = A[i + j + 16] * y1 + A[i - j + 32] * y0;
			beta2 = A[i + j + 16] * y0 - A[i - j + 32] * y1;
			theta += 16;
			A[i + j] = alpha + beta1;
			A[i + j + 16] = alpha - beta1;
			A[i - j + 16] = beta + beta2;
			A[i - j + 32] = beta - beta2;
		}
	}
	for (i = 0; i < 512; i += 64)
	{
		theta = 8;
		alpha = A[i];
		beta = A[i + 32];
		A[i] = alpha + beta;
		A[i + 32] = alpha - beta;
		alpha = A[i + 16];
		beta = A[i + 48];
		A[i + 16] = alpha + beta;
		A[i + 48] = alpha - beta;
		for (j = 1; j < 16; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 128];
			alpha = A[i + j];
			beta = A[i - j + 32];
			beta1 = A[i + j + 32] * y1 + A[i - j + 64] * y0;
			beta2 = A[i + j + 32] * y0 - A[i - j + 64] * y1;
			theta += 8;
			A[i + j] = alpha + beta1;
			A[i + j + 32] = alpha - beta1;
			A[i - j + 32] = beta + beta2;
			A[i - j + 64] = beta - beta2;
		}
	}
	for (i = 0; i < 512; i += 128)
	{
		theta = 4;
		alpha = A[i];
		beta = A[i + 64];
		A[i] = alpha + beta;
		A[i + 64] = alpha - beta;
		alpha = A[i + 32];
		beta = A[i + 96];
		A[i + 32] = alpha + beta;
		A[i + 96] = alpha - beta;
		for (j = 1; j < 32; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 128];
			alpha = A[i + j];
			beta = A[i - j + 64];
			beta1 = A[i + j + 64] * y1 + A[i - j + 128] * y0;
			beta2 = A[i + j + 64] * y0 - A[i - j + 128] * y1;
			theta += 4;
			A[i + j] = alpha + beta1;
			A[i + j + 64] = alpha - beta1;
			A[i - j + 64] = beta + beta2;
			A[i - j + 128] = beta - beta2;
		}
	}
	for (i = 0; i < 512; i += 256)
	{
		theta = 2;
		alpha = A[i];
		beta = A[i + 128];
		A[i] = alpha + beta;
		A[i + 128] = alpha - beta;
		alpha = A[i + 64];
		beta = A[i + 192];
		A[i + 64] = alpha + beta;
		A[i + 192] = alpha - beta;
		for (j = 1; j < 64; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 128];
			alpha = A[i + j];
			beta = A[i - j + 128];
			beta1 = A[i + j + 128] * y1 + A[i - j + 256] * y0;
			beta2 = A[i + j + 128] * y0 - A[i - j + 256] * y1;
			theta += 2;
			A[i + j] = alpha + beta1;
			A[i + j + 128] = alpha - beta1;
			A[i - j + 128] = beta + beta2;
			A[i - j + 256] = beta - beta2;
		}
	}
	theta = 1;
	alpha = A[0];
	beta = A[256];
	A[0] = alpha + beta;
	A[256] = alpha - beta;
	alpha = A[128];
	beta = A[384];
	A[128] = alpha + beta;
	A[384] = alpha - beta;
	for (j = 1; j < 128; j++)
	{
		y0 = sinTab[theta];
		y1 = sinTab[theta + 128];
		alpha = A[j];
		beta = A[-j + 256];
		beta1 = A[j + 256] * y1 + A[-j + 512] * y0;
		beta2 = A[j + 256] * y0 - A[-j + 512] * y1;
		theta += 1;
		A[j] = alpha + beta1;
		A[j + 256] = alpha - beta1;
		A[-j + 256] = beta + beta2;
		A[-j + 512] = beta - beta2;
	}
}
void DFT524288(float *A, const float *sinTab)
{
	int i, j, theta;
	float alpha, beta, beta1, beta2, y0, y1, y2, y3;
	for (i = 0; i < 524288; i += 4)
	{
		alpha = A[i];
		beta = A[i + 1];
		beta1 = A[i + 2];
		beta2 = A[i + 3];
		y0 = alpha + beta;
		y1 = alpha - beta;
		y2 = beta1 + beta2;
		y3 = beta1 - beta2;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	for (i = 0; i < 524288; i += 8)
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
		beta1 = 0.70710678118654752440084436210485f*(A[i + 5] + A[i + 7]);
		beta2 = 0.70710678118654752440084436210485f*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	for (i = 0; i < 524288; i += 16)
	{
		theta = 32768;
		alpha = A[i];
		beta = A[i + 8];
		A[i] = alpha + beta;
		A[i + 8] = alpha - beta;
		alpha = A[i + 4];
		beta = A[i + 12];
		A[i + 4] = alpha + beta;
		A[i + 12] = alpha - beta;
		for (j = 1; j < 4; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 131072];
			alpha = A[i + j];
			beta = A[i - j + 8];
			beta1 = A[i + j + 8] * y1 + A[i - j + 16] * y0;
			beta2 = A[i + j + 8] * y0 - A[i - j + 16] * y1;
			theta += 32768;
			A[i + j] = alpha + beta1;
			A[i + j + 8] = alpha - beta1;
			A[i - j + 8] = beta + beta2;
			A[i - j + 16] = beta - beta2;
		}
	}
	for (i = 0; i < 524288; i += 32)
	{
		theta = 16384;
		alpha = A[i];
		beta = A[i + 16];
		A[i] = alpha + beta;
		A[i + 16] = alpha - beta;
		alpha = A[i + 8];
		beta = A[i + 24];
		A[i + 8] = alpha + beta;
		A[i + 24] = alpha - beta;
		for (j = 1; j < 8; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 131072];
			alpha = A[i + j];
			beta = A[i - j + 16];
			beta1 = A[i + j + 16] * y1 + A[i - j + 32] * y0;
			beta2 = A[i + j + 16] * y0 - A[i - j + 32] * y1;
			theta += 16384;
			A[i + j] = alpha + beta1;
			A[i + j + 16] = alpha - beta1;
			A[i - j + 16] = beta + beta2;
			A[i - j + 32] = beta - beta2;
		}
	}
	for (i = 0; i < 524288; i += 64)
	{
		theta = 8192;
		alpha = A[i];
		beta = A[i + 32];
		A[i] = alpha + beta;
		A[i + 32] = alpha - beta;
		alpha = A[i + 16];
		beta = A[i + 48];
		A[i + 16] = alpha + beta;
		A[i + 48] = alpha - beta;
		for (j = 1; j < 16; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 131072];
			alpha = A[i + j];
			beta = A[i - j + 32];
			beta1 = A[i + j + 32] * y1 + A[i - j + 64] * y0;
			beta2 = A[i + j + 32] * y0 - A[i - j + 64] * y1;
			theta += 8192;
			A[i + j] = alpha + beta1;
			A[i + j + 32] = alpha - beta1;
			A[i - j + 32] = beta + beta2;
			A[i - j + 64] = beta - beta2;
		}
	}
	for (i = 0; i < 524288; i += 128)
	{
		theta = 4096;
		alpha = A[i];
		beta = A[i + 64];
		A[i] = alpha + beta;
		A[i + 64] = alpha - beta;
		alpha = A[i + 32];
		beta = A[i + 96];
		A[i + 32] = alpha + beta;
		A[i + 96] = alpha - beta;
		for (j = 1; j < 32; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 131072];
			alpha = A[i + j];
			beta = A[i - j + 64];
			beta1 = A[i + j + 64] * y1 + A[i - j + 128] * y0;
			beta2 = A[i + j + 64] * y0 - A[i - j + 128] * y1;
			theta += 4096;
			A[i + j] = alpha + beta1;
			A[i + j + 64] = alpha - beta1;
			A[i - j + 64] = beta + beta2;
			A[i - j + 128] = beta - beta2;
		}
	}
	for (i = 0; i < 524288; i += 256)
	{
		theta = 2048;
		alpha = A[i];
		beta = A[i + 128];
		A[i] = alpha + beta;
		A[i + 128] = alpha - beta;
		alpha = A[i + 64];
		beta = A[i + 192];
		A[i + 64] = alpha + beta;
		A[i + 192] = alpha - beta;
		for (j = 1; j < 64; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 131072];
			alpha = A[i + j];
			beta = A[i - j + 128];
			beta1 = A[i + j + 128] * y1 + A[i - j + 256] * y0;
			beta2 = A[i + j + 128] * y0 - A[i - j + 256] * y1;
			theta += 2048;
			A[i + j] = alpha + beta1;
			A[i + j + 128] = alpha - beta1;
			A[i - j + 128] = beta + beta2;
			A[i - j + 256] = beta - beta2;
		}
	}
	for (i = 0; i < 524288; i += 512)
	{
		theta = 1024;
		alpha = A[i];
		beta = A[i + 256];
		A[i] = alpha + beta;
		A[i + 256] = alpha - beta;
		alpha = A[i + 128];
		beta = A[i + 384];
		A[i + 128] = alpha + beta;
		A[i + 384] = alpha - beta;
		for (j = 1; j < 128; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 131072];
			alpha = A[i + j];
			beta = A[i - j + 256];
			beta1 = A[i + j + 256] * y1 + A[i - j + 512] * y0;
			beta2 = A[i + j + 256] * y0 - A[i - j + 512] * y1;
			theta += 1024;
			A[i + j] = alpha + beta1;
			A[i + j + 256] = alpha - beta1;
			A[i - j + 256] = beta + beta2;
			A[i - j + 512] = beta - beta2;
		}
	}
	for (i = 0; i < 524288; i += 1024)
	{
		theta = 512;
		alpha = A[i];
		beta = A[i + 512];
		A[i] = alpha + beta;
		A[i + 512] = alpha - beta;
		alpha = A[i + 256];
		beta = A[i + 768];
		A[i + 256] = alpha + beta;
		A[i + 768] = alpha - beta;
		for (j = 1; j < 256; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 131072];
			alpha = A[i + j];
			beta = A[i - j + 512];
			beta1 = A[i + j + 512] * y1 + A[i - j + 1024] * y0;
			beta2 = A[i + j + 512] * y0 - A[i - j + 1024] * y1;
			theta += 512;
			A[i + j] = alpha + beta1;
			A[i + j + 512] = alpha - beta1;
			A[i - j + 512] = beta + beta2;
			A[i - j + 1024] = beta - beta2;
		}
	}
	for (i = 0; i < 524288; i += 2048)
	{
		theta = 256;
		alpha = A[i];
		beta = A[i + 1024];
		A[i] = alpha + beta;
		A[i + 1024] = alpha - beta;
		alpha = A[i + 512];
		beta = A[i + 1536];
		A[i + 512] = alpha + beta;
		A[i + 1536] = alpha - beta;
		for (j = 1; j < 512; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 131072];
			alpha = A[i + j];
			beta = A[i - j + 1024];
			beta1 = A[i + j + 1024] * y1 + A[i - j + 2048] * y0;
			beta2 = A[i + j + 1024] * y0 - A[i - j + 2048] * y1;
			theta += 256;
			A[i + j] = alpha + beta1;
			A[i + j + 1024] = alpha - beta1;
			A[i - j + 1024] = beta + beta2;
			A[i - j + 2048] = beta - beta2;
		}
	}
	for (i = 0; i < 524288; i += 4096)
	{
		theta = 128;
		alpha = A[i];
		beta = A[i + 2048];
		A[i] = alpha + beta;
		A[i + 2048] = alpha - beta;
		alpha = A[i + 1024];
		beta = A[i + 3072];
		A[i + 1024] = alpha + beta;
		A[i + 3072] = alpha - beta;
		for (j = 1; j < 1024; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 131072];
			alpha = A[i + j];
			beta = A[i - j + 2048];
			beta1 = A[i + j + 2048] * y1 + A[i - j + 4096] * y0;
			beta2 = A[i + j + 2048] * y0 - A[i - j + 4096] * y1;
			theta += 128;
			A[i + j] = alpha + beta1;
			A[i + j + 2048] = alpha - beta1;
			A[i - j + 2048] = beta + beta2;
			A[i - j + 4096] = beta - beta2;
		}
	}
	for (i = 0; i < 524288; i += 8192)
	{
		theta = 64;
		alpha = A[i];
		beta = A[i + 4096];
		A[i] = alpha + beta;
		A[i + 4096] = alpha - beta;
		alpha = A[i + 2048];
		beta = A[i + 6144];
		A[i + 2048] = alpha + beta;
		A[i + 6144] = alpha - beta;
		for (j = 1; j < 2048; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 131072];
			alpha = A[i + j];
			beta = A[i - j + 4096];
			beta1 = A[i + j + 4096] * y1 + A[i - j + 8192] * y0;
			beta2 = A[i + j + 4096] * y0 - A[i - j + 8192] * y1;
			theta += 64;
			A[i + j] = alpha + beta1;
			A[i + j + 4096] = alpha - beta1;
			A[i - j + 4096] = beta + beta2;
			A[i - j + 8192] = beta - beta2;
		}
	}
	for (i = 0; i < 524288; i += 16384)
	{
		theta = 32;
		alpha = A[i];
		beta = A[i + 8192];
		A[i] = alpha + beta;
		A[i + 8192] = alpha - beta;
		alpha = A[i + 4096];
		beta = A[i + 12288];
		A[i + 4096] = alpha + beta;
		A[i + 12288] = alpha - beta;
		for (j = 1; j < 4096; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 131072];
			alpha = A[i + j];
			beta = A[i - j + 8192];
			beta1 = A[i + j + 8192] * y1 + A[i - j + 16384] * y0;
			beta2 = A[i + j + 8192] * y0 - A[i - j + 16384] * y1;
			theta += 32;
			A[i + j] = alpha + beta1;
			A[i + j + 8192] = alpha - beta1;
			A[i - j + 8192] = beta + beta2;
			A[i - j + 16384] = beta - beta2;
		}
	}
	for (i = 0; i < 524288; i += 32768)
	{
		theta = 16;
		alpha = A[i];
		beta = A[i + 16384];
		A[i] = alpha + beta;
		A[i + 16384] = alpha - beta;
		alpha = A[i + 8192];
		beta = A[i + 24576];
		A[i + 8192] = alpha + beta;
		A[i + 24576] = alpha - beta;
		for (j = 1; j < 8192; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 131072];
			alpha = A[i + j];
			beta = A[i - j + 16384];
			beta1 = A[i + j + 16384] * y1 + A[i - j + 32768] * y0;
			beta2 = A[i + j + 16384] * y0 - A[i - j + 32768] * y1;
			theta += 16;
			A[i + j] = alpha + beta1;
			A[i + j + 16384] = alpha - beta1;
			A[i - j + 16384] = beta + beta2;
			A[i - j + 32768] = beta - beta2;
		}
	}
	for (i = 0; i < 524288; i += 65536)
	{
		theta = 8;
		alpha = A[i];
		beta = A[i + 32768];
		A[i] = alpha + beta;
		A[i + 32768] = alpha - beta;
		alpha = A[i + 16384];
		beta = A[i + 49152];
		A[i + 16384] = alpha + beta;
		A[i + 49152] = alpha - beta;
		for (j = 1; j < 16384; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 131072];
			alpha = A[i + j];
			beta = A[i - j + 32768];
			beta1 = A[i + j + 32768] * y1 + A[i - j + 65536] * y0;
			beta2 = A[i + j + 32768] * y0 - A[i - j + 65536] * y1;
			theta += 8;
			A[i + j] = alpha + beta1;
			A[i + j + 32768] = alpha - beta1;
			A[i - j + 32768] = beta + beta2;
			A[i - j + 65536] = beta - beta2;
		}
	}
	for (i = 0; i < 524288; i += 131072)
	{
		theta = 4;
		alpha = A[i];
		beta = A[i + 65536];
		A[i] = alpha + beta;
		A[i + 65536] = alpha - beta;
		alpha = A[i + 32768];
		beta = A[i + 98304];
		A[i + 32768] = alpha + beta;
		A[i + 98304] = alpha - beta;
		for (j = 1; j < 32768; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 131072];
			alpha = A[i + j];
			beta = A[i - j + 65536];
			beta1 = A[i + j + 65536] * y1 + A[i - j + 131072] * y0;
			beta2 = A[i + j + 65536] * y0 - A[i - j + 131072] * y1;
			theta += 4;
			A[i + j] = alpha + beta1;
			A[i + j + 65536] = alpha - beta1;
			A[i - j + 65536] = beta + beta2;
			A[i - j + 131072] = beta - beta2;
		}
	}
	for (i = 0; i < 524288; i += 262144)
	{
		theta = 2;
		alpha = A[i];
		beta = A[i + 131072];
		A[i] = alpha + beta;
		A[i + 131072] = alpha - beta;
		alpha = A[i + 65536];
		beta = A[i + 196608];
		A[i + 65536] = alpha + beta;
		A[i + 196608] = alpha - beta;
		for (j = 1; j < 65536; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 131072];
			alpha = A[i + j];
			beta = A[i - j + 131072];
			beta1 = A[i + j + 131072] * y1 + A[i - j + 262144] * y0;
			beta2 = A[i + j + 131072] * y0 - A[i - j + 262144] * y1;
			theta += 2;
			A[i + j] = alpha + beta1;
			A[i + j + 131072] = alpha - beta1;
			A[i - j + 131072] = beta + beta2;
			A[i - j + 262144] = beta - beta2;
		}
	}
	theta = 1;
	alpha = A[0];
	beta = A[262144];
	A[0] = alpha + beta;
	A[262144] = alpha - beta;
	alpha = A[131072];
	beta = A[393216];
	A[131072] = alpha + beta;
	A[393216] = alpha - beta;
	for (j = 1; j < 131072; j++)
	{
		y0 = sinTab[theta];
		y1 = sinTab[theta + 131072];
		alpha = A[j];
		beta = A[-j + 262144];
		beta1 = A[j + 262144] * y1 + A[-j + 524288] * y0;
		beta2 = A[j + 262144] * y0 - A[-j + 524288] * y1;
		theta += 1;
		A[j] = alpha + beta1;
		A[j + 262144] = alpha - beta1;
		A[-j + 262144] = beta + beta2;
		A[-j + 524288] = beta - beta2;
	}
}
void DFT64(float *A, const float *sinTab)
{
	int i, j, theta;
	float alpha, beta, beta1, beta2, y0, y1, y2, y3;
	for (i = 0; i < 64; i += 4)
	{
		alpha = A[i];
		beta = A[i + 1];
		beta1 = A[i + 2];
		beta2 = A[i + 3];
		y0 = alpha + beta;
		y1 = alpha - beta;
		y2 = beta1 + beta2;
		y3 = beta1 - beta2;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	for (i = 0; i < 64; i += 8)
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
		beta1 = 0.70710678118654752440084436210485f*(A[i + 5] + A[i + 7]);
		beta2 = 0.70710678118654752440084436210485f*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	for (i = 0; i < 64; i += 16)
	{
		theta = 4;
		alpha = A[i];
		beta = A[i + 8];
		A[i] = alpha + beta;
		A[i + 8] = alpha - beta;
		alpha = A[i + 4];
		beta = A[i + 12];
		A[i + 4] = alpha + beta;
		A[i + 12] = alpha - beta;
		for (j = 1; j < 4; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 16];
			alpha = A[i + j];
			beta = A[i - j + 8];
			beta1 = A[i + j + 8] * y1 + A[i - j + 16] * y0;
			beta2 = A[i + j + 8] * y0 - A[i - j + 16] * y1;
			theta += 4;
			A[i + j] = alpha + beta1;
			A[i + j + 8] = alpha - beta1;
			A[i - j + 8] = beta + beta2;
			A[i - j + 16] = beta - beta2;
		}
	}
	for (i = 0; i < 64; i += 32)
	{
		theta = 2;
		alpha = A[i];
		beta = A[i + 16];
		A[i] = alpha + beta;
		A[i + 16] = alpha - beta;
		alpha = A[i + 8];
		beta = A[i + 24];
		A[i + 8] = alpha + beta;
		A[i + 24] = alpha - beta;
		for (j = 1; j < 8; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 16];
			alpha = A[i + j];
			beta = A[i - j + 16];
			beta1 = A[i + j + 16] * y1 + A[i - j + 32] * y0;
			beta2 = A[i + j + 16] * y0 - A[i - j + 32] * y1;
			theta += 2;
			A[i + j] = alpha + beta1;
			A[i + j + 16] = alpha - beta1;
			A[i - j + 16] = beta + beta2;
			A[i - j + 32] = beta - beta2;
		}
	}
	theta = 1;
	alpha = A[0];
	beta = A[32];
	A[0] = alpha + beta;
	A[32] = alpha - beta;
	alpha = A[16];
	beta = A[48];
	A[16] = alpha + beta;
	A[48] = alpha - beta;
	for (j = 1; j < 16; j++)
	{
		y0 = sinTab[theta];
		y1 = sinTab[theta + 16];
		alpha = A[j];
		beta = A[-j + 32];
		beta1 = A[j + 32] * y1 + A[-j + 64] * y0;
		beta2 = A[j + 32] * y0 - A[-j + 64] * y1;
		theta += 1;
		A[j] = alpha + beta1;
		A[j + 32] = alpha - beta1;
		A[-j + 32] = beta + beta2;
		A[-j + 64] = beta - beta2;
	}
}
void DFT65536(float *A, const float *sinTab)
{
	int i, j, theta;
	float alpha, beta, beta1, beta2, y0, y1, y2, y3;
	for (i = 0; i < 65536; i += 4)
	{
		alpha = A[i];
		beta = A[i + 1];
		beta1 = A[i + 2];
		beta2 = A[i + 3];
		y0 = alpha + beta;
		y1 = alpha - beta;
		y2 = beta1 + beta2;
		y3 = beta1 - beta2;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	for (i = 0; i < 65536; i += 8)
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
		beta1 = 0.70710678118654752440084436210485f*(A[i + 5] + A[i + 7]);
		beta2 = 0.70710678118654752440084436210485f*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	for (i = 0; i < 65536; i += 16)
	{
		theta = 4096;
		alpha = A[i];
		beta = A[i + 8];
		A[i] = alpha + beta;
		A[i + 8] = alpha - beta;
		alpha = A[i + 4];
		beta = A[i + 12];
		A[i + 4] = alpha + beta;
		A[i + 12] = alpha - beta;
		for (j = 1; j < 4; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 16384];
			alpha = A[i + j];
			beta = A[i - j + 8];
			beta1 = A[i + j + 8] * y1 + A[i - j + 16] * y0;
			beta2 = A[i + j + 8] * y0 - A[i - j + 16] * y1;
			theta += 4096;
			A[i + j] = alpha + beta1;
			A[i + j + 8] = alpha - beta1;
			A[i - j + 8] = beta + beta2;
			A[i - j + 16] = beta - beta2;
		}
	}
	for (i = 0; i < 65536; i += 32)
	{
		theta = 2048;
		alpha = A[i];
		beta = A[i + 16];
		A[i] = alpha + beta;
		A[i + 16] = alpha - beta;
		alpha = A[i + 8];
		beta = A[i + 24];
		A[i + 8] = alpha + beta;
		A[i + 24] = alpha - beta;
		for (j = 1; j < 8; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 16384];
			alpha = A[i + j];
			beta = A[i - j + 16];
			beta1 = A[i + j + 16] * y1 + A[i - j + 32] * y0;
			beta2 = A[i + j + 16] * y0 - A[i - j + 32] * y1;
			theta += 2048;
			A[i + j] = alpha + beta1;
			A[i + j + 16] = alpha - beta1;
			A[i - j + 16] = beta + beta2;
			A[i - j + 32] = beta - beta2;
		}
	}
	for (i = 0; i < 65536; i += 64)
	{
		theta = 1024;
		alpha = A[i];
		beta = A[i + 32];
		A[i] = alpha + beta;
		A[i + 32] = alpha - beta;
		alpha = A[i + 16];
		beta = A[i + 48];
		A[i + 16] = alpha + beta;
		A[i + 48] = alpha - beta;
		for (j = 1; j < 16; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 16384];
			alpha = A[i + j];
			beta = A[i - j + 32];
			beta1 = A[i + j + 32] * y1 + A[i - j + 64] * y0;
			beta2 = A[i + j + 32] * y0 - A[i - j + 64] * y1;
			theta += 1024;
			A[i + j] = alpha + beta1;
			A[i + j + 32] = alpha - beta1;
			A[i - j + 32] = beta + beta2;
			A[i - j + 64] = beta - beta2;
		}
	}
	for (i = 0; i < 65536; i += 128)
	{
		theta = 512;
		alpha = A[i];
		beta = A[i + 64];
		A[i] = alpha + beta;
		A[i + 64] = alpha - beta;
		alpha = A[i + 32];
		beta = A[i + 96];
		A[i + 32] = alpha + beta;
		A[i + 96] = alpha - beta;
		for (j = 1; j < 32; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 16384];
			alpha = A[i + j];
			beta = A[i - j + 64];
			beta1 = A[i + j + 64] * y1 + A[i - j + 128] * y0;
			beta2 = A[i + j + 64] * y0 - A[i - j + 128] * y1;
			theta += 512;
			A[i + j] = alpha + beta1;
			A[i + j + 64] = alpha - beta1;
			A[i - j + 64] = beta + beta2;
			A[i - j + 128] = beta - beta2;
		}
	}
	for (i = 0; i < 65536; i += 256)
	{
		theta = 256;
		alpha = A[i];
		beta = A[i + 128];
		A[i] = alpha + beta;
		A[i + 128] = alpha - beta;
		alpha = A[i + 64];
		beta = A[i + 192];
		A[i + 64] = alpha + beta;
		A[i + 192] = alpha - beta;
		for (j = 1; j < 64; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 16384];
			alpha = A[i + j];
			beta = A[i - j + 128];
			beta1 = A[i + j + 128] * y1 + A[i - j + 256] * y0;
			beta2 = A[i + j + 128] * y0 - A[i - j + 256] * y1;
			theta += 256;
			A[i + j] = alpha + beta1;
			A[i + j + 128] = alpha - beta1;
			A[i - j + 128] = beta + beta2;
			A[i - j + 256] = beta - beta2;
		}
	}
	for (i = 0; i < 65536; i += 512)
	{
		theta = 128;
		alpha = A[i];
		beta = A[i + 256];
		A[i] = alpha + beta;
		A[i + 256] = alpha - beta;
		alpha = A[i + 128];
		beta = A[i + 384];
		A[i + 128] = alpha + beta;
		A[i + 384] = alpha - beta;
		for (j = 1; j < 128; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 16384];
			alpha = A[i + j];
			beta = A[i - j + 256];
			beta1 = A[i + j + 256] * y1 + A[i - j + 512] * y0;
			beta2 = A[i + j + 256] * y0 - A[i - j + 512] * y1;
			theta += 128;
			A[i + j] = alpha + beta1;
			A[i + j + 256] = alpha - beta1;
			A[i - j + 256] = beta + beta2;
			A[i - j + 512] = beta - beta2;
		}
	}
	for (i = 0; i < 65536; i += 1024)
	{
		theta = 64;
		alpha = A[i];
		beta = A[i + 512];
		A[i] = alpha + beta;
		A[i + 512] = alpha - beta;
		alpha = A[i + 256];
		beta = A[i + 768];
		A[i + 256] = alpha + beta;
		A[i + 768] = alpha - beta;
		for (j = 1; j < 256; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 16384];
			alpha = A[i + j];
			beta = A[i - j + 512];
			beta1 = A[i + j + 512] * y1 + A[i - j + 1024] * y0;
			beta2 = A[i + j + 512] * y0 - A[i - j + 1024] * y1;
			theta += 64;
			A[i + j] = alpha + beta1;
			A[i + j + 512] = alpha - beta1;
			A[i - j + 512] = beta + beta2;
			A[i - j + 1024] = beta - beta2;
		}
	}
	for (i = 0; i < 65536; i += 2048)
	{
		theta = 32;
		alpha = A[i];
		beta = A[i + 1024];
		A[i] = alpha + beta;
		A[i + 1024] = alpha - beta;
		alpha = A[i + 512];
		beta = A[i + 1536];
		A[i + 512] = alpha + beta;
		A[i + 1536] = alpha - beta;
		for (j = 1; j < 512; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 16384];
			alpha = A[i + j];
			beta = A[i - j + 1024];
			beta1 = A[i + j + 1024] * y1 + A[i - j + 2048] * y0;
			beta2 = A[i + j + 1024] * y0 - A[i - j + 2048] * y1;
			theta += 32;
			A[i + j] = alpha + beta1;
			A[i + j + 1024] = alpha - beta1;
			A[i - j + 1024] = beta + beta2;
			A[i - j + 2048] = beta - beta2;
		}
	}
	for (i = 0; i < 65536; i += 4096)
	{
		theta = 16;
		alpha = A[i];
		beta = A[i + 2048];
		A[i] = alpha + beta;
		A[i + 2048] = alpha - beta;
		alpha = A[i + 1024];
		beta = A[i + 3072];
		A[i + 1024] = alpha + beta;
		A[i + 3072] = alpha - beta;
		for (j = 1; j < 1024; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 16384];
			alpha = A[i + j];
			beta = A[i - j + 2048];
			beta1 = A[i + j + 2048] * y1 + A[i - j + 4096] * y0;
			beta2 = A[i + j + 2048] * y0 - A[i - j + 4096] * y1;
			theta += 16;
			A[i + j] = alpha + beta1;
			A[i + j + 2048] = alpha - beta1;
			A[i - j + 2048] = beta + beta2;
			A[i - j + 4096] = beta - beta2;
		}
	}
	for (i = 0; i < 65536; i += 8192)
	{
		theta = 8;
		alpha = A[i];
		beta = A[i + 4096];
		A[i] = alpha + beta;
		A[i + 4096] = alpha - beta;
		alpha = A[i + 2048];
		beta = A[i + 6144];
		A[i + 2048] = alpha + beta;
		A[i + 6144] = alpha - beta;
		for (j = 1; j < 2048; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 16384];
			alpha = A[i + j];
			beta = A[i - j + 4096];
			beta1 = A[i + j + 4096] * y1 + A[i - j + 8192] * y0;
			beta2 = A[i + j + 4096] * y0 - A[i - j + 8192] * y1;
			theta += 8;
			A[i + j] = alpha + beta1;
			A[i + j + 4096] = alpha - beta1;
			A[i - j + 4096] = beta + beta2;
			A[i - j + 8192] = beta - beta2;
		}
	}
	for (i = 0; i < 65536; i += 16384)
	{
		theta = 4;
		alpha = A[i];
		beta = A[i + 8192];
		A[i] = alpha + beta;
		A[i + 8192] = alpha - beta;
		alpha = A[i + 4096];
		beta = A[i + 12288];
		A[i + 4096] = alpha + beta;
		A[i + 12288] = alpha - beta;
		for (j = 1; j < 4096; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 16384];
			alpha = A[i + j];
			beta = A[i - j + 8192];
			beta1 = A[i + j + 8192] * y1 + A[i - j + 16384] * y0;
			beta2 = A[i + j + 8192] * y0 - A[i - j + 16384] * y1;
			theta += 4;
			A[i + j] = alpha + beta1;
			A[i + j + 8192] = alpha - beta1;
			A[i - j + 8192] = beta + beta2;
			A[i - j + 16384] = beta - beta2;
		}
	}
	for (i = 0; i < 65536; i += 32768)
	{
		theta = 2;
		alpha = A[i];
		beta = A[i + 16384];
		A[i] = alpha + beta;
		A[i + 16384] = alpha - beta;
		alpha = A[i + 8192];
		beta = A[i + 24576];
		A[i + 8192] = alpha + beta;
		A[i + 24576] = alpha - beta;
		for (j = 1; j < 8192; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 16384];
			alpha = A[i + j];
			beta = A[i - j + 16384];
			beta1 = A[i + j + 16384] * y1 + A[i - j + 32768] * y0;
			beta2 = A[i + j + 16384] * y0 - A[i - j + 32768] * y1;
			theta += 2;
			A[i + j] = alpha + beta1;
			A[i + j + 16384] = alpha - beta1;
			A[i - j + 16384] = beta + beta2;
			A[i - j + 32768] = beta - beta2;
		}
	}
	theta = 1;
	alpha = A[0];
	beta = A[32768];
	A[0] = alpha + beta;
	A[32768] = alpha - beta;
	alpha = A[16384];
	beta = A[49152];
	A[16384] = alpha + beta;
	A[49152] = alpha - beta;
	for (j = 1; j < 16384; j++)
	{
		y0 = sinTab[theta];
		y1 = sinTab[theta + 16384];
		alpha = A[j];
		beta = A[-j + 32768];
		beta1 = A[j + 32768] * y1 + A[-j + 65536] * y0;
		beta2 = A[j + 32768] * y0 - A[-j + 65536] * y1;
		theta += 1;
		A[j] = alpha + beta1;
		A[j + 32768] = alpha - beta1;
		A[-j + 32768] = beta + beta2;
		A[-j + 65536] = beta - beta2;
	}
}
void DFT8(float *A, const float *sinTab)
{
	int i;
	float alpha, beta, beta1, beta2, y0, y1, y2, y3;
	for (i = 0; i < 8; i += 4)
	{
		alpha = A[i];
		beta = A[i + 1];
		beta1 = A[i + 2];
		beta2 = A[i + 3];
		y0 = alpha + beta;
		y1 = alpha - beta;
		y2 = beta1 + beta2;
		y3 = beta1 - beta2;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	alpha = A[0];
	A[0] = alpha + A[4];
	A[4] = alpha - A[4];
	alpha = A[2];
	A[2] = alpha + A[6];
	A[6] = alpha - A[6];
	beta1 = 0.70710678118654752440084436210485f*(A[5] + A[7]);
	beta2 = 0.70710678118654752440084436210485f*(A[5] - A[7]);
	A[5] = A[1] - beta1;
	A[1] = A[1] + beta1;
	A[7] = A[3] - beta2;
	A[3] = A[3] + beta2;
}
void DFT8192(float *A, const float *sinTab)
{
	int i, j, theta;
	float alpha, beta, beta1, beta2, y0, y1, y2, y3;
	for (i = 0; i < 8192; i += 4)
	{
		alpha = A[i];
		beta = A[i + 1];
		beta1 = A[i + 2];
		beta2 = A[i + 3];
		y0 = alpha + beta;
		y1 = alpha - beta;
		y2 = beta1 + beta2;
		y3 = beta1 - beta2;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	for (i = 0; i < 8192; i += 8)
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
		beta1 = 0.70710678118654752440084436210485f*(A[i + 5] + A[i + 7]);
		beta2 = 0.70710678118654752440084436210485f*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	for (i = 0; i < 8192; i += 16)
	{
		theta = 512;
		alpha = A[i];
		beta = A[i + 8];
		A[i] = alpha + beta;
		A[i + 8] = alpha - beta;
		alpha = A[i + 4];
		beta = A[i + 12];
		A[i + 4] = alpha + beta;
		A[i + 12] = alpha - beta;
		for (j = 1; j < 4; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 2048];
			alpha = A[i + j];
			beta = A[i - j + 8];
			beta1 = A[i + j + 8] * y1 + A[i - j + 16] * y0;
			beta2 = A[i + j + 8] * y0 - A[i - j + 16] * y1;
			theta += 512;
			A[i + j] = alpha + beta1;
			A[i + j + 8] = alpha - beta1;
			A[i - j + 8] = beta + beta2;
			A[i - j + 16] = beta - beta2;
		}
	}
	for (i = 0; i < 8192; i += 32)
	{
		theta = 256;
		alpha = A[i];
		beta = A[i + 16];
		A[i] = alpha + beta;
		A[i + 16] = alpha - beta;
		alpha = A[i + 8];
		beta = A[i + 24];
		A[i + 8] = alpha + beta;
		A[i + 24] = alpha - beta;
		for (j = 1; j < 8; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 2048];
			alpha = A[i + j];
			beta = A[i - j + 16];
			beta1 = A[i + j + 16] * y1 + A[i - j + 32] * y0;
			beta2 = A[i + j + 16] * y0 - A[i - j + 32] * y1;
			theta += 256;
			A[i + j] = alpha + beta1;
			A[i + j + 16] = alpha - beta1;
			A[i - j + 16] = beta + beta2;
			A[i - j + 32] = beta - beta2;
		}
	}
	for (i = 0; i < 8192; i += 64)
	{
		theta = 128;
		alpha = A[i];
		beta = A[i + 32];
		A[i] = alpha + beta;
		A[i + 32] = alpha - beta;
		alpha = A[i + 16];
		beta = A[i + 48];
		A[i + 16] = alpha + beta;
		A[i + 48] = alpha - beta;
		for (j = 1; j < 16; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 2048];
			alpha = A[i + j];
			beta = A[i - j + 32];
			beta1 = A[i + j + 32] * y1 + A[i - j + 64] * y0;
			beta2 = A[i + j + 32] * y0 - A[i - j + 64] * y1;
			theta += 128;
			A[i + j] = alpha + beta1;
			A[i + j + 32] = alpha - beta1;
			A[i - j + 32] = beta + beta2;
			A[i - j + 64] = beta - beta2;
		}
	}
	for (i = 0; i < 8192; i += 128)
	{
		theta = 64;
		alpha = A[i];
		beta = A[i + 64];
		A[i] = alpha + beta;
		A[i + 64] = alpha - beta;
		alpha = A[i + 32];
		beta = A[i + 96];
		A[i + 32] = alpha + beta;
		A[i + 96] = alpha - beta;
		for (j = 1; j < 32; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 2048];
			alpha = A[i + j];
			beta = A[i - j + 64];
			beta1 = A[i + j + 64] * y1 + A[i - j + 128] * y0;
			beta2 = A[i + j + 64] * y0 - A[i - j + 128] * y1;
			theta += 64;
			A[i + j] = alpha + beta1;
			A[i + j + 64] = alpha - beta1;
			A[i - j + 64] = beta + beta2;
			A[i - j + 128] = beta - beta2;
		}
	}
	for (i = 0; i < 8192; i += 256)
	{
		theta = 32;
		alpha = A[i];
		beta = A[i + 128];
		A[i] = alpha + beta;
		A[i + 128] = alpha - beta;
		alpha = A[i + 64];
		beta = A[i + 192];
		A[i + 64] = alpha + beta;
		A[i + 192] = alpha - beta;
		for (j = 1; j < 64; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 2048];
			alpha = A[i + j];
			beta = A[i - j + 128];
			beta1 = A[i + j + 128] * y1 + A[i - j + 256] * y0;
			beta2 = A[i + j + 128] * y0 - A[i - j + 256] * y1;
			theta += 32;
			A[i + j] = alpha + beta1;
			A[i + j + 128] = alpha - beta1;
			A[i - j + 128] = beta + beta2;
			A[i - j + 256] = beta - beta2;
		}
	}
	for (i = 0; i < 8192; i += 512)
	{
		theta = 16;
		alpha = A[i];
		beta = A[i + 256];
		A[i] = alpha + beta;
		A[i + 256] = alpha - beta;
		alpha = A[i + 128];
		beta = A[i + 384];
		A[i + 128] = alpha + beta;
		A[i + 384] = alpha - beta;
		for (j = 1; j < 128; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 2048];
			alpha = A[i + j];
			beta = A[i - j + 256];
			beta1 = A[i + j + 256] * y1 + A[i - j + 512] * y0;
			beta2 = A[i + j + 256] * y0 - A[i - j + 512] * y1;
			theta += 16;
			A[i + j] = alpha + beta1;
			A[i + j + 256] = alpha - beta1;
			A[i - j + 256] = beta + beta2;
			A[i - j + 512] = beta - beta2;
		}
	}
	for (i = 0; i < 8192; i += 1024)
	{
		theta = 8;
		alpha = A[i];
		beta = A[i + 512];
		A[i] = alpha + beta;
		A[i + 512] = alpha - beta;
		alpha = A[i + 256];
		beta = A[i + 768];
		A[i + 256] = alpha + beta;
		A[i + 768] = alpha - beta;
		for (j = 1; j < 256; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 2048];
			alpha = A[i + j];
			beta = A[i - j + 512];
			beta1 = A[i + j + 512] * y1 + A[i - j + 1024] * y0;
			beta2 = A[i + j + 512] * y0 - A[i - j + 1024] * y1;
			theta += 8;
			A[i + j] = alpha + beta1;
			A[i + j + 512] = alpha - beta1;
			A[i - j + 512] = beta + beta2;
			A[i - j + 1024] = beta - beta2;
		}
	}
	for (i = 0; i < 8192; i += 2048)
	{
		theta = 4;
		alpha = A[i];
		beta = A[i + 1024];
		A[i] = alpha + beta;
		A[i + 1024] = alpha - beta;
		alpha = A[i + 512];
		beta = A[i + 1536];
		A[i + 512] = alpha + beta;
		A[i + 1536] = alpha - beta;
		for (j = 1; j < 512; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 2048];
			alpha = A[i + j];
			beta = A[i - j + 1024];
			beta1 = A[i + j + 1024] * y1 + A[i - j + 2048] * y0;
			beta2 = A[i + j + 1024] * y0 - A[i - j + 2048] * y1;
			theta += 4;
			A[i + j] = alpha + beta1;
			A[i + j + 1024] = alpha - beta1;
			A[i - j + 1024] = beta + beta2;
			A[i - j + 2048] = beta - beta2;
		}
	}
	for (i = 0; i < 8192; i += 4096)
	{
		theta = 2;
		alpha = A[i];
		beta = A[i + 2048];
		A[i] = alpha + beta;
		A[i + 2048] = alpha - beta;
		alpha = A[i + 1024];
		beta = A[i + 3072];
		A[i + 1024] = alpha + beta;
		A[i + 3072] = alpha - beta;
		for (j = 1; j < 1024; j++)
		{
			y0 = sinTab[theta];
			y1 = sinTab[theta + 2048];
			alpha = A[i + j];
			beta = A[i - j + 2048];
			beta1 = A[i + j + 2048] * y1 + A[i - j + 4096] * y0;
			beta2 = A[i + j + 2048] * y0 - A[i - j + 4096] * y1;
			theta += 2;
			A[i + j] = alpha + beta1;
			A[i + j + 2048] = alpha - beta1;
			A[i - j + 2048] = beta + beta2;
			A[i - j + 4096] = beta - beta2;
		}
	}
	theta = 1;
	alpha = A[0];
	beta = A[4096];
	A[0] = alpha + beta;
	A[4096] = alpha - beta;
	alpha = A[2048];
	beta = A[6144];
	A[2048] = alpha + beta;
	A[6144] = alpha - beta;
	for (j = 1; j < 2048; j++)
	{
		y0 = sinTab[theta];
		y1 = sinTab[theta + 2048];
		alpha = A[j];
		beta = A[-j + 4096];
		beta1 = A[j + 4096] * y1 + A[-j + 8192] * y0;
		beta2 = A[j + 4096] * y0 - A[-j + 8192] * y1;
		theta += 1;
		A[j] = alpha + beta1;
		A[j + 4096] = alpha - beta1;
		A[-j + 4096] = beta + beta2;
		A[-j + 8192] = beta - beta2;
	}
}
