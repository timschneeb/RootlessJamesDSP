#ifndef M_PI
#define M_PI 3.14159265358979323846264338327950288
#endif
#define ORDER 16 // Must be even
#define ORDERPLUS1 (ORDER + 1)
#define ORDERMULT2 (ORDER << 1)
#define STAGE (ORDER >> 1)
#define M1STAGE (STAGE - 1)
typedef struct
{
	double a1, a2;
} LPFCoeffs;
typedef struct
{
	double _xn1, _xn2;
	double _yn[STAGE], _yn1[STAGE], _yn2[STAGE];
} iirSOS;
typedef struct
{
	int factor;
	LPFCoeffs coeffs[STAGE];
	float gain;
	iirSOS lpfU, lpfD;
} samplerateTool;
typedef struct
{
	double b, bp, a;
	double zLPF, zHPF;
} DF1P;
typedef struct
{
	DF1P sys[9];
} TenBandsCrossover;
void init10BandsCrossover(TenBandsCrossover *lr4, double fs, double lowBandHz, double midBand1Hz, double midBand2Hz, double midBand3Hz, double midBand4Hz, double midBand5Hz, double midBand6Hz, double midBand7Hz, double midBand8Hz);
void process10BandsCrossover(TenBandsCrossover *lr4, double x, double *lowOut, double *midOut1, double *midOut2, double *midOut3, double *midOut4, double *midOut5, double *midOut6, double *midOut7, double *midOut8, double *highOut);
void oversample_makeSmp(samplerateTool *oversample, int factor);
void oversample_stepupSmp(samplerateTool *oversample, float input, float *output);
double oversample_stepdownSmpDouble(samplerateTool *oversample, double *input);
float oversample_stepdownSmpFloat(samplerateTool *oversample, float *input);
