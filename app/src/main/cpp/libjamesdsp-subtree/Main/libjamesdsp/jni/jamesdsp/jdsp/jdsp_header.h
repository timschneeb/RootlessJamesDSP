#ifndef _EEL_JDSP_HD_H_
#define _EEL_JDSP_HD_H_
#include <stdint.h>
#include "../cpthread.h"
#include "generalDSP/interpolation.h"
#include "Effects/eel2/numericSys/libsamplerate/samplerate.h"
#include "generalDSP/TwoStageFFTConvolver.h"
#include "generalDSP/digitalFilters.h"
#include "Effects/eel2/numericSys/FilterDesign/fdesign.h"
#include "Effects/eel2/eelCommon.h"
#include "generalDSP/ArbFIRGen.h"
// Misc
extern double mapVal(double x, double in_min, double in_max, double out_min, double out_max);
extern double mag2dB(double lin);
extern float db2magf(double dB);
extern double db2mag(double dB);
extern void linspace(double *x, int n, double a, double b);
extern void channel_joinFloat(float **chan_buffers, unsigned int num_channels, float *buffer, unsigned int num_frames);
extern void channel_join(double **chan_buffers, unsigned int num_channels, double *buffer, unsigned int num_frames);
extern void channel_split(double *buffer, unsigned int num_frames, double **chan_buffers, unsigned int num_channels);
extern void channel_splitFloat(float *buffer, unsigned int num_frames, float **chan_buffers, unsigned int num_channels);
extern void normalise(float *buffer, int num_samps);
extern unsigned int crc32c(const unsigned char *buf, size_t len);
extern int upper_bound(double *a, int n, double x);
extern int lower_bound(double *a, int n, double x);
extern size_t fast_upper_bound(double *a, size_t n, double x);
extern size_t fast_lower_bound(double *a, size_t n, double x);
extern void fhtbitReversalTbl(unsigned *dst, unsigned int n);
extern void fhtsinHalfTblFloat(float *dst, unsigned int n);
extern void LLdiscreteHartleyFloat(float *A, const int nPoints, const float *sinTab);
extern double randXorshift(uint64_t s[2]);
// Misc end
typedef struct
{
	float threshold;
	float relCoef;
	float envOverThreshold;
} JLimiter;
typedef double EnvelopeDetector;
typedef struct
{
	EnvelopeDetector att_;
	EnvelopeDetector rel_;
} AttRelEnvelope;
typedef struct
{
	float *fs;
	float rampCoeff;
	float cvRelease;
	float cvAttack;
	float cvSmooth;
	float logThreshold[2];
	float crestPeak;
	float crestRms;
	float metaKneeMult;
	float metaMaxAttackTime;
	float metaMaxReleaseTime;
	float metaCrestTime;
	float metaCrestCoeff;
	float metaAdaptTime;
	float metaAdaptCoeff;
} AutoComp;
#define FFTSIZE_DRS (8192)
#define ANALYSIS_OVERLAP_DRS 4
#define OVPSIZE_DRS (FFTSIZE_DRS / ANALYSIS_OVERLAP_DRS)
#define HALFWNDLEN_DRS ((FFTSIZE_DRS >> 1) + 1)
#define MAX_OUTPUT_BUFFERS_DRS 2
typedef struct
{
	// Constant
	unsigned int fftLen, minus_fftLen, ovpLen, halfLen, smpShift, procUpTo;
	void(*fft)(float*, const float*);
	unsigned int mBitRev[FFTSIZE_DRS];
	float 	mSineTab[FFTSIZE_DRS >> 1];
	float analysisWnd[FFTSIZE_DRS];
	float synthesisWnd[FFTSIZE_DRS];
	// Shared variable between all FFT length and modes and channel config
	int  mOutputReadSampleOffset;
	int  mOutputBufferCount; // How many buffers are actually in use
	unsigned int mInputSamplesNeeded;
	float 	*mOutputBuffer[MAX_OUTPUT_BUFFERS_DRS];
	float buffer[MAX_OUTPUT_BUFFERS_DRS][OVPSIZE_DRS * 2];
	unsigned int mInputPos;
	float 	mInput[2][FFTSIZE_DRS];
	float 	mOverlapStage2dash[2][OVPSIZE_DRS];
	float 	mTempLBuffer[FFTSIZE_DRS];
	float 	mTempRBuffer[FFTSIZE_DRS];
	float timeDomainOut[2][FFTSIZE_DRS];
	float mag[HALFWNDLEN_DRS];
	float aheight[HALFWNDLEN_DRS];
	char noGridDownsampling;
	unsigned int smallGridSize;
	char octaveSmooth[sizeof(unsigned int) + sizeof(float) + sizeof(unsigned int) + ((HALFWNDLEN_DRS + 1) << 1) * sizeof(unsigned int) + (HALFWNDLEN_DRS + 1) * sizeof(float) + ((HALFWNDLEN_DRS + 1) + 3) * 2 * sizeof(float)];
	float finalGain[HALFWNDLEN_DRS];
	// Global parameter
	float fgt_fac, spectralRate, metaMaxAttackTime, metaMaxReleaseTime;
	// Subband parameter
	float metaAdaptCoeff[HALFWNDLEN_DRS];
	// Subband states
	float adaptiveRelease[HALFWNDLEN_DRS], adaptiveAttack[HALFWNDLEN_DRS], smoothLogGain[HALFWNDLEN_DRS], logThreshold[HALFWNDLEN_DRS];
} FFTDynamicRangeSquasher;
void FFTDynamicRangeSquasherSetavgBW(FFTDynamicRangeSquasher *cm, double avgBW);
void FFTDynamicRangeSquasherInit(FFTDynamicRangeSquasher *msr, float fs);
int FFTDynamicRangeSquasherProcessSamples(FFTDynamicRangeSquasher *msr, const float *inLeft, const float *inRight, unsigned int inSampleCount, float *outL, float *outR);
typedef struct
{
	int needOversample;
	samplerateTool smp[2];
	SixBandsCrossover subband[2];
	float pregain, postgain;
} VacuumTube;
typedef struct
{
	float inputs[1024];
	int inPoint;
	int outPoint;
	int allocateLen;
} integerDelayLine;
typedef struct
{
	int filterType;
	float gCoeff; // gain element 
	float RCoeff; // feedback damping element
	float KCoeff; // shelf gain element
	float precomputeCoeff1, precomputeCoeff2, precomputeCoeff3, precomputeCoeff4;
	float z1_A, z2_A; // state variables (z^-1)
} StateVariable2ndOrder;
typedef struct
{
	float maxGain;
	float originalBuf[960];
	int downsamplerPos;
	samplerateTool downsampler;
	float delayLine[16];
	float fftBuf[16];
	float smoothFFTBuffer[9];
	float freq[9];
	float maxSmoothingFactor, minusmaxSmoothingFactor;
	float smoothMaxFreq;
	float boostdB;
	double fs;
	float gainSmoothingFactor, minusgainSmoothingFactor;
	StateVariable2ndOrder svf[2];
	integerDelayLine dL[2];
} DBB;
//   sf_reverb_state_st rv;
//   sf_presetreverb(&rv, 44100, SF_REVERB_PRESET_DEFAULT);
//
//   for each abitrary length sample:
//   float outputL, outputR;
//   sf_reverb_process(&rv, inputL, inputR, &outputL, &outputR);
#define SF_REVERB_DS        3000
typedef struct
{
	int pos;                 // current write position
	int size;                // delay size
	float buf[SF_REVERB_DS]; // delay buffer
} sf_rv_delay_st;
// 1st order IIR filter
typedef struct
{
	float a2; // coefficients
	float b1;
	float b2;
	float y1; // state
} sf_rv_iir1_st;
// biquad
// note: we don't use biquad.c because we want to step through the sound one sample at a time, one
//       channel at a time
typedef struct
{
	float b0; // biquad coefficients
	float b1;
	float b2;
	float a1;
	float a2;
	float xn1; // input[n - 1]
	float xn2; // input[n - 2]
	float yn1; // output[n - 1]
	float yn2; // output[n - 2]
} sf_rv_biquad_st;
// early reflection
typedef struct
{
	int             delaytblL[18], delaytblR[18];
	sf_rv_delay_st  delayPWL, delayPWR;
	sf_rv_delay_st  delayRL, delayLR;
	sf_rv_biquad_st allpassXL, allpassXR;
	sf_rv_biquad_st allpassL, allpassR;
	sf_rv_iir1_st   lpfL, lpfR;
	sf_rv_iir1_st   hpfL, hpfR;
	float wet1, wet2;
} sf_rv_earlyref_st;
// oversampling
// maximum oversampling factor
#define SF_REVERB_OF        2
typedef struct
{
	int factor;           // oversampling factor [1 to SF_REVERB_OF]
	sf_rv_biquad_st lpfU; // lowpass filter used for upsampling
	sf_rv_biquad_st lpfD; // lowpass filter used for downsampling
} sf_rv_oversample_st;
// dc cut
typedef struct
{
	float gain;
	float y1;
	float y2;
} sf_rv_dccut_st;
// fractal noise cache
// noise buffer size; must be a power of 2 because it's generated via fractal generator
#define SF_REVERB_NS        (1<<11)
typedef struct
{
	int pos;                 // current read position in the buffer
	float buf[SF_REVERB_NS]; // buffer filled with noise
} sf_rv_noise_st;
// low-frequency oscilator (LFO)
typedef struct
{
	float re;  // real part
	float im;  // imaginary part
	float sn;  // sin of angle increment per sample
	float co;  // cos of angle increment per sample
	int count; // number of samples generated so far (used to apply small corrections over time)
} sf_rv_lfo_st;
// all-pass filter
// maximum size
#define SF_REVERB_APS       3400
typedef struct
{
	int pos;
	int size;
	float feedback;
	float decay;
	float buf[SF_REVERB_APS];
} sf_rv_allpass_st;
// 2nd order all-pass filter
// maximum sizes of the two buffers
#define SF_REVERB_AP2S1     4200
#define SF_REVERB_AP2S2     3000
typedef struct
{
	//    line 1                 line 2
	int   pos1, pos2;
	int   size1, size2;
	float feedback1, feedback2;
	float decay1, decay2;
	float buf1[SF_REVERB_AP2S1], buf2[SF_REVERB_AP2S2];
} sf_rv_allpass2_st;
// 3rd order all-pass filter with modulation
// maximum sizes of the three buffers and maximum mod size of the first line
#define SF_REVERB_AP3S1     4000
#define SF_REVERB_AP3M1     600
#define SF_REVERB_AP3S2     2000
#define SF_REVERB_AP3S3     3000
typedef struct
{
	//    line 1 (with modulation)                 line 2                 line 3
	int   rpos1, wpos1, pos2, pos3;
	int   size1, msize1, size2, size3;
	float feedback1, feedback2, feedback3;
	float decay1, decay2, decay3;
	float buf1[SF_REVERB_AP3S1 + SF_REVERB_AP3M1], buf2[SF_REVERB_AP3S2], buf3[SF_REVERB_AP3S3];
} sf_rv_allpass3_st;
// modulated all-pass filter
// maximum size and maximum mod size
#define SF_REVERB_APMS      3600
#define SF_REVERB_APMM      137
typedef struct
{
	int rpos, wpos;
	int size, msize;
	float feedback;
	float decay;
	float z1;
	float buf[SF_REVERB_APMS + SF_REVERB_APMM];
} sf_rv_allpassm_st;
// comb filter
// maximum size of the buffer
#define SF_REVERB_CS        1500
typedef struct
{
	int pos;
	int size;
	float buf[SF_REVERB_CS];
} sf_rv_comb_st;
//
// the final reverb state structure
//
// note: this struct is about 1Mb
typedef struct
{
	sf_rv_earlyref_st   earlyref;
	sf_rv_oversample_st oversampleL, oversampleR;
	sf_rv_dccut_st      dccutL, dccutR;
	sf_rv_noise_st      noise;
	sf_rv_lfo_st        lfo1;
	sf_rv_iir1_st       lfo1_lpf;
	sf_rv_allpassm_st   diffL[10], diffR[10];
	sf_rv_allpass_st    crossL[4], crossR[4];
	sf_rv_iir1_st       clpfL, clpfR; // cross LPF
	sf_rv_delay_st      cdelayL, cdelayR; // cross delay
	sf_rv_biquad_st     bassapL, bassapR; // bass all-pass
	sf_rv_biquad_st     basslpL, basslpR; // bass lowpass
	sf_rv_iir1_st       damplpL, damplpR; // dampening lowpass
	sf_rv_allpassm_st   dampap1L, dampap1R; // dampening all-pass (1)
	sf_rv_delay_st      dampdL, dampdR; // dampening delay
	sf_rv_allpassm_st   dampap2L, dampap2R; // dampening all-pass (2)
	sf_rv_delay_st      cbassd1L, cbassd1R; // cross-fade bass delay (1)
	sf_rv_allpass2_st   cbassap1L, cbassap1R; // cross-fade bass allpass (1)
	sf_rv_delay_st      cbassd2L, cbassd2R; // cross-fade bass delay (2)
	sf_rv_allpass3_st   cbassap2L, cbassap2R; // cross-fade bass allpass (2)
	sf_rv_lfo_st        lfo2;
	sf_rv_iir1_st       lfo2_lpf;
	sf_rv_comb_st       combL, combR;
	sf_rv_biquad_st     lastlpfL, lastlpfR;
	sf_rv_delay_st      lastdelayL, lastdelayR;
	sf_rv_delay_st      inpdelayL, inpdelayR;
	int outco[32];
	float loopdecay;
	float wet1, wet2;
	float wander;
	float bassb;
	float ertolate; // early reflection mix parameters
	float erefwet;
	float dry;
} sf_reverb_state_st;
typedef enum
{
	SF_REVERB_PRESET_DEFAULT,
	SF_REVERB_PRESET_SMALLHALL1,
	SF_REVERB_PRESET_SMALLHALL2,
	SF_REVERB_PRESET_MEDIUMHALL1,
	SF_REVERB_PRESET_MEDIUMHALL2,
	SF_REVERB_PRESET_LARGEHALL1,
	SF_REVERB_PRESET_LARGEHALL2,
	SF_REVERB_PRESET_SMALLROOM1,
	SF_REVERB_PRESET_SMALLROOM2,
	SF_REVERB_PRESET_MEDIUMROOM1,
	SF_REVERB_PRESET_MEDIUMROOM2,
	SF_REVERB_PRESET_LARGEROOM1,
	SF_REVERB_PRESET_LARGEROOM2,
	SF_REVERB_PRESET_MEDIUMER1,
	SF_REVERB_PRESET_MEDIUMER2,
	SF_REVERB_PRESET_PLATEHIGH,
	SF_REVERB_PRESET_PLATELOW,
	SF_REVERB_PRESET_LONGREVERB1,
	SF_REVERB_PRESET_LONGREVERB2
} sf_reverb_preset;
extern void sf_advancereverb(sf_reverb_state_st *rv, int rate, int oversamplefactor, float ertolate, float erefwet, float dry, float ereffactor, float erefwidth, float width, float wet, float wander, float bassb, float spin, float inputlpf, float basslpf, float damplpf, float outputlpf, float rt60, float delay);

typedef struct
{
	char *subband[2];
	float emaAlpha[5];
	float sumStates[5];
	float diffStates[5];
	float mix, minusMix, gain;
} stereoEnhancement;
typedef struct
{
	NSEEL_VMCTX vm;
	NSEEL_CODEHANDLE codehandleInit, codehandleProcess;
	float *vmFs, *input1, *input2;
	int compileSucessfully;
    int active;
} LiveProg;
typedef struct
{
	double b0, b1, b2, a1, a2;
	double v1L, v2L, v1R, v2R; // State
} DirectForm2;
typedef struct
{
	char *oldFile;
	int usedSOSCount;
	DirectForm2 **sosPointer;
} DDC;
/* Minimum/maximum cut frequency (Hz) */
/* bs2b_set_level_fcut() */
#define BS2B_MINFCUT 300
#define BS2B_MAXFCUT 2000
/* Minimum/maximum feed level (dB * 10 @ low frequencies) */
/* bs2b_set_level_feed() */
#define BS2B_MINFEED 10   /* 1 dB */
#define BS2B_MAXFEED 150  /* 15 dB */
/* Default crossfeed levels */
/* Sets a new coefficients by new crossfeed value.
 * level = ( ( uint32_t )fcut | ( ( uint32_t )feed << 16 ) )
 * where 'feed' is crossfeeding level at low frequencies (dB * 10)
 * and 'fcut' is cut frecuency (Hz)
 */
#define BS2B_DEFAULT_CLEVEL  ((unsigned int)700 | ((unsigned int)45 << 16))
#define BS2B_CMOY_CLEVEL     ((unsigned int)700 | ((unsigned int)60 << 16))
#define BS2B_JMEIER_CLEVEL   ((unsigned int)650 | ((unsigned int)95 << 16))
typedef struct str_t_bs2bd
{
	double a0_lo, b1_lo;         /* Lowpass IIR filter coefficients */
	double a0_hi, a1_hi, b1_hi;  /* Highboost IIR filter coefficients */
	double gain;                 /* Global gain against overloading */
	/* Buffer of last filtered sample: [0] 1-st channel, [1] 2-d channel */
	struct { double asis[2], lo[2], hi[2]; } lfs;
} t_bs2bdp;
/* Get flevel value that used in function BS2BInit */
int BS2BCalculateflevel(unsigned int fcut, unsigned int gain);
void BS2BInit(t_bs2bdp *bs2bdp, unsigned int samplerate, int flevel);
/* sample poits to double floats native endians */
void BS2BProcess(t_bs2bdp *bs2bdp, double *sampleL, double *sampleR);
typedef struct
{
	int mode; // 0: BS2B Lv 1, 1: BS2B Lv 2, 2: HRTF crossfeed, 2: HRTF surround 1, 2: HRTF surround 2, 2: HRTF surround 3
	t_bs2bdp bs2b[2];
	FFTConvolver2x4x2 *conv[3];
	TwoStageFFTConvolver2x4x2 *convLong;
} Crossfeed;
typedef struct dspsys dspsys;
typedef struct
{
	FFTConvolver2x2 *conv1d2x2_S_S;
	TwoStageFFTConvolver2x2 *conv1d2x2_T_S;
	FFTConvolver2x4x2 *conv1d2x4x2_S_S;
	TwoStageFFTConvolver2x4x2 *conv1d2x4x2_T_S;
	void(*process)(struct dspsys*, size_t);
} Convolver1D;
typedef struct
{
	ArbitraryEq coeffGen;
	unsigned int filterLen;
	FFTConvolver2x2 convState;
} ArbEqConv;
#define NUMPTS 15
typedef struct
{
	int currentInterpolationMode, currentPhaseMode;
	ierper pch1, pch2;
	ArbEqConv instance;
	double freq[NUMPTS + 2];
	double gain[NUMPTS + 2];
} FIREqualizer;
typedef struct dspsys
{
	// Sys var
	char enableASRC;
	IntegerASRCHandler asrc[2];
	float trueSampleRate, fs;
	// Effect
	// Compressor
	int compEnabled;
	FFTDynamicRangeSquasher comp;
	// Bass boost
	int bassBoostEnabled;
	DBB dbb;
	// Equalizer
	int equalizerEnabled, equalizerForceRefresh;
	FIREqualizer fireq;
	// Reverb
	int reverbEnabled;
	sf_reverb_state_st reverb;
	// Stereo enhancement
	int sterEnhEnabled;
	stereoEnhancement sterEnh;
	// Vacuum tube
	int tubeEnabled;
	VacuumTube tube;
	// Crossfeed
	int crossfeedEnabled, crossfeedForceRefresh;
	Crossfeed advXF;
	// DDC
	int ddcEnabled, ddcForceRefresh;
	DDC vdcFl;
	// Convolver
	int convolverEnabled;
	Convolver1D conv;
	// Live programmable effect
	int liveprogEnabled;
	LiveProg eel;
	// Arbitrary magnitude response
	int arbitraryMagEnabled, arbMagForceRefresh;
	ArbEqConv arbMag;
	// Output limiter
	float postGain;
	JLimiter limiter;
	size_t blockSize, blockSizeMax, pw2BlockMemSize;
	float *tmpBuffer[6];
	// I/O function pointer
	void(*processInt16Deinterleaved)(struct dspsys*, int16_t*, int16_t*, int16_t*, int16_t*, size_t);
	void(*processInt32Deinterleaved)(struct dspsys*, int32_t*, int32_t*, int32_t*, int32_t*, size_t);
	void(*processFloatDeinterleaved)(struct dspsys*, float*, float*, float*, float*, size_t);
	void(*processInt16Multiplexd)(struct dspsys*, int16_t*, int16_t*, size_t);
	void(*processInt32Multiplexd)(struct dspsys*, int32_t*, int32_t*, size_t);
	void(*processFloatMultiplexd)(struct dspsys*, float*, float*, size_t);
	// Blobs(resampled)
	int blobsResampledLen;
	float *blobsCh1[3];
	float *blobsCh2[3];
	float *blobsCh3[3];
	float *blobsCh4[3];
	int frameLenSVirResampled;
	float *hrtfblobsResampled[4];
	// Mutex lock(pthread)
	int isMutexSuccess;
	pthread_mutex_t m_in_processing;
	// Random number and related
	uint64_t rndstate[2];
} JamesDSPLib;
// JamesDSP controller
extern void JamesDSPGlobalMemoryAllocation();
extern void JamesDSPGlobalMemoryDeallocation();
extern void JamesDSPReallocateBlock(JamesDSPLib *jdsp, size_t blockSizeMax);
extern void jdsp_lock(JamesDSPLib *jdsp);
extern void jdsp_unlock(JamesDSPLib *jdsp);
extern void JamesDSPFree(JamesDSPLib *jdsp);
extern void JamesDSPInit(JamesDSPLib *jdsp, int blockSizeMax, float sample_rate);
extern void JamesDSPSetPostGain(JamesDSPLib *jdsp, double pGaindB);
extern int JamesDSPGetMutexStatus(JamesDSPLib *jdsp);
extern void JamesDSPSetSampleRate(JamesDSPLib *jdsp, float new_sample_rate, int forceRefresh);
// Limiter
extern void JLimiterSetCoefficients(JamesDSPLib *jdsp, double thresholddB, double msRelease);
extern void JLimiterInit(JamesDSPLib *jdsp);
// Compressor
extern void CompressorReset(JamesDSPLib *jdsp);
extern void CompressorSetParam(JamesDSPLib *jdsp, float maxAtk, float maxRel, float adapt);
extern void CompressorEnable(JamesDSPLib *jdsp);
extern void CompressorDisable(JamesDSPLib *jdsp);
extern void CompressorProcess(JamesDSPLib *jdsp, size_t n);
// Bass boost
extern void BassBoostEnable(JamesDSPLib *jdsp);
extern void BassBoostDisable(JamesDSPLib *jdsp);
extern void BassBoostConstructor(JamesDSPLib *jdsp);
extern void BassBoostSetParam(JamesDSPLib *jdsp, float maxG);
extern void BassBoostProcess(JamesDSPLib *jdsp, size_t n);
// Reverb
extern void Reverb_SetParam(JamesDSPLib *jdsp, int presets);
extern void ReverbEnable(JamesDSPLib *jdsp);
extern void ReverbDisable(JamesDSPLib *jdsp);
extern void ReverbProcess(JamesDSPLib *jdsp, size_t n);
// Stereo enhancement
void StereoEnhancementDestructor(JamesDSPLib *jdsp);
void StereoEnhancementConstructor(JamesDSPLib *jdsp);
extern void StereoEnhancementSetParam(JamesDSPLib *jdsp, float mix);
extern void StereoEnhancementEnable(JamesDSPLib *jdsp);
extern void StereoEnhancementDisable(JamesDSPLib *jdsp);
extern void StereoEnhancementProcess(JamesDSPLib *jdsp, size_t n);
// Vacuum tube
extern void VacuumTubeEnable(JamesDSPLib *jdsp);
extern void VacuumTubeDisable(JamesDSPLib *jdsp);
extern void VacuumTubeSetGain(JamesDSPLib *jdsp, double dbGain);
extern void VacuumTubeProcess(JamesDSPLib *jdsp, size_t n);
// Live programmable effect
extern const char* checkErrorCode(int errCode);
extern void LiveProgConstructor(JamesDSPLib *jdsp);
extern void LiveProgDestructor(JamesDSPLib *jdsp);
extern int LiveProgStringParser(JamesDSPLib *jdsp, char *eelCode);
extern void LiveProgEnable(JamesDSPLib *jdsp);
extern void LiveProgDisable(JamesDSPLib *jdsp);
extern void LiveProgProcess(JamesDSPLib *jdsp, size_t n);
// DDC
extern void DDCConstructor(JamesDSPLib *jdsp);
extern void DDCDestructor(JamesDSPLib *jdsp);
extern int DDCEnable(JamesDSPLib *jdsp);
extern void DDCDisable(JamesDSPLib *jdsp);
extern int DDCStringParser(JamesDSPLib *jdsp, char *newStr);
extern void DDCProcess(JamesDSPLib *jdsp, size_t n);
// Crossfeed
extern void CrossfeedConstructor(JamesDSPLib *jdsp);
extern void CrossfeedDestructor(JamesDSPLib *jdsp);
extern void CrossfeedEnable(JamesDSPLib *jdsp);
extern void CrossfeedDisable(JamesDSPLib *jdsp);
extern void CrossfeedChangeMode(JamesDSPLib *jdsp, int nMode);
extern void CrossfeedProcess(JamesDSPLib *jdsp, size_t n);
// Convolver
extern void Convolver1DEnable(JamesDSPLib *jdsp);
extern void Convolver1DDisable(JamesDSPLib *jdsp);
extern void Convolver1DConstructor(JamesDSPLib *jdsp);
extern void Convolver1DDestructor(JamesDSPLib *jdsp, int reqUnlock);
extern int Convolver1DLoadImpulseResponse(JamesDSPLib *jdsp, float *imp, unsigned int channels, unsigned int frameCount);
// Arbitrary magnitude response
extern void ArbitraryResponseEqualizerConstructor(JamesDSPLib *jdsp);
extern void ArbitraryResponseEqualizerDestructor(JamesDSPLib *jdsp);
extern void ArbitraryResponseEqualizerStringParser(JamesDSPLib *jdsp, char *stringEq);
extern void ArbitraryResponseEqualizerEnable(JamesDSPLib *jdsp);
extern void ArbitraryResponseEqualizerDisable(JamesDSPLib *jdsp);
extern void ArbitraryResponseEqualizerProcess(JamesDSPLib *jdsp, size_t n);
// FIR Equalizer
extern void FIREqualizerConstructor(JamesDSPLib *jdsp);
extern void FIREqualizerDestructor(JamesDSPLib *jdsp);
extern void FIREqualizerAxisInterpolation(JamesDSPLib *jdsp, int interpolationMode, int phaseMode, double *freqAx, double *gaindB);
extern void FIREqualizerEnable(JamesDSPLib *jdsp);
extern void FIREqualizerDisable(JamesDSPLib *jdsp);
extern void FIREqualizerProcess(JamesDSPLib *jdsp, size_t n);
#endif
