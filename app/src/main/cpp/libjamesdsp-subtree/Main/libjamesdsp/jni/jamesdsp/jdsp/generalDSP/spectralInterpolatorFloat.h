size_t EstimateMemorySpectralInterpolator(unsigned int *fcLen, unsigned int arrayLen, double avgBW, unsigned int *smallGridSize);
void InitSpectralInterpolator(char *octaveSmooth, unsigned int fcLen, unsigned int arrayLen, double avgBW);
int ShrinkGridSpectralInterpolator(char *octaveSmooth, unsigned int inputSpecLen, float *spectrum, float *virtualSubbands);
int LerpSpectralInterpolator(char *octaveSmooth, unsigned int aryLen, float *x, unsigned int inputSpecLen, float *expandGrid);