#ifdef DEBUG
#define TAG "EffectDSPMain"
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG,__VA_ARGS__)
#include "MemoryUsage.h"
#endif
#include <string.h>
#include "essential.h"
// Effect section
#include "jdsp/jdsp_header.h"
typedef struct
{
	unsigned long long initializeForFirst;
	int mEnable;
	JamesDSPLib jdsp;
    float mSamplingRate;
    int formatFloatModeInt32Mode;
	char *stringEq;
	float *tempImpulseIncoming;
	float drcAtkRel[4];
	float boostingCon;
	int bbMaxGain;
	// Variables
	int numTime2Send, samplesInc, stringIndex;
	int16_t impChannels;
	int32_t impulseLengthActual, convolverNeedRefresh;
} EffectDSPMain;
typedef struct
{
	int32_t status;
	uint32_t psize;
	uint32_t vsize;
	int32_t cmd;
	int32_t data;
} reply1x4_1x4_t;
void EffectDSPMainConstructor(EffectDSPMain *dspmain)
{
	dspmain->initializeForFirst = 0;
	dspmain->tempImpulseIncoming = 0;
	dspmain->stringEq = 0;
	dspmain->boostingCon = 3;
	dspmain->bbMaxGain = 0;
	dspmain->numTime2Send = 0;
	dspmain->samplesInc = 0;
	dspmain->stringIndex = 0;
	JamesDSPInit(&dspmain->jdsp, 128, 48000.0f);
}
void EffectDSPMainDestructor(EffectDSPMain *dspmain)
{
	JamesDSPFree(&dspmain->jdsp);
	if (dspmain->tempImpulseIncoming)
		free(dspmain->tempImpulseIncoming);
	if (dspmain->stringEq)
		free(dspmain->stringEq);
#ifdef DEBUG
	LOGI("Buffer freed");
#endif
}
int32_t configure(EffectDSPMain *dspmain, void* pCmdData, effect_buffer_access_e* mAccessMode)
{
    effect_config_t *cfg = (effect_config_t*)pCmdData;
    buffer_config_t in = cfg->inputCfg;
    buffer_config_t out = cfg->outputCfg;
    /* Check that we aren't asked to do resampling. Note that audioflinger
     * always provides full setup info at initial configure time. */
#ifdef DEBUG
    	LOGI("Sample rate of In: %u and out: %u", in.samplingRate, out.samplingRate);
#endif
    if ((in.mask & EFFECT_CONFIG_SMP_RATE) && (out.mask & EFFECT_CONFIG_SMP_RATE))
    {
        if (out.samplingRate != in.samplingRate)
        {
#ifdef DEBUG
	LOGW("In/out sample rate doesn't match");
#endif
    return -EINVAL;
        }
        dspmain->mSamplingRate = (float)in.samplingRate;
    }
    if (in.mask & EFFECT_CONFIG_CHANNELS && out.mask & EFFECT_CONFIG_CHANNELS)
    {
        if (in.channels != AUDIO_CHANNEL_OUT_STEREO)
        {
#ifdef DEBUG
	LOGE("Input is non stereo signal. It's channel count is %u", in.channels);
#endif
            return -EINVAL;
        }
        if (out.channels != AUDIO_CHANNEL_OUT_STEREO)
        {
#ifdef DEBUG
	LOGE("Output is non stereo signal. It's channel count is %u", out.channels);
#endif
            return -EINVAL;
        }
    }
    else
    {
#ifdef DEBUG
	LOGE("In/out channel mask doesn't match");
#endif
    }
    if (in.mask & EFFECT_CONFIG_FORMAT)
    {
        if (in.format != AUDIO_FORMAT_PCM_16_BIT)
        {
        	if (in.format == AUDIO_FORMAT_PCM_FLOAT)
        		dspmain->formatFloatModeInt32Mode = 1;
        	else if (in.format == AUDIO_FORMAT_PCM_32_BIT)
        		dspmain->formatFloatModeInt32Mode = 2;
        }
    }
    if (out.mask & EFFECT_CONFIG_FORMAT)
    {
        if (out.format != AUDIO_FORMAT_PCM_16_BIT)
        {
        	if (out.format == AUDIO_FORMAT_PCM_FLOAT)
        		dspmain->formatFloatModeInt32Mode = 1;
        	else if (in.format == AUDIO_FORMAT_PCM_32_BIT)
        		dspmain->formatFloatModeInt32Mode = 2;
        }
    }
#ifdef DEBUG
	LOGW("I/O FMT = { %u, %u }", in.format, out.format);
#endif
    if (out.mask & EFFECT_CONFIG_ACC_MODE)
        *mAccessMode = (effect_buffer_access_e) out.accessMode;
    return 0;
}
int32_t EffectDSPMainCommand(EffectDSPMain *dspmain, uint32_t cmdCode, uint32_t cmdSize, void* pCmdData, uint32_t* replySize, void* pReplyData)
{
#ifdef DEBUG
//		LOGI("Memory used: %f Mb", (float)getCurrentRSS() / 1024.0 / 1024.0);
#endif
	if (cmdCode == EFFECT_CMD_SET_CONFIG)
	{
		effect_buffer_access_e mAccessMode;
		int32_t *replyData = (int32_t *)pReplyData;
		int32_t ret = configure(dspmain, pCmdData, &mAccessMode);
		if (ret != 0)
		{
			*replyData = ret;
			return 0;
		}
		// Set sample rate
		JamesDSPSetSampleRate(&dspmain->jdsp, dspmain->mSamplingRate, 0);
		*replyData = 0;
		return 0;
	}
	if (cmdCode == EFFECT_CMD_GET_PARAM)
	{
		effect_param_t *cep = (effect_param_t *)pCmdData;
		if (cep->psize == 4 && cep->vsize == 4)
		{
			int32_t cmd = ((int32_t *)cep)[3];
			if (cmd == 19998)
			{
				reply1x4_1x4_t *replyData = (reply1x4_1x4_t *)pReplyData;
				replyData->status = 0;
				replyData->psize = 4;
				replyData->vsize = 4;
				replyData->cmd = 19998;
				replyData->data = (int32_t)dspmain->initializeForFirst++;
				*replySize = sizeof(reply1x4_1x4_t);
				return 0;
			}
			if (cmd == 19999)
			{
				reply1x4_1x4_t *replyData = (reply1x4_1x4_t *)pReplyData;
				replyData->status = 0;
				replyData->psize = 4;
				replyData->vsize = 4;
				replyData->cmd = 19999;
				replyData->data = (int32_t)dspmain->jdsp.blockSize;
				*replySize = sizeof(reply1x4_1x4_t);
				return 0;
			}
			else if (cmd == 20000)
			{
				reply1x4_1x4_t *replyData = (reply1x4_1x4_t *)pReplyData;
				replyData->status = 0;
				replyData->psize = 4;
				replyData->vsize = 4;
				replyData->cmd = 20000;
				replyData->data = (int32_t)dspmain->jdsp.blockSizeMax;
				*replySize = sizeof(reply1x4_1x4_t);
				return 0;
			}
			else if (cmd == 20001)
			{
				reply1x4_1x4_t *replyData = (reply1x4_1x4_t *)pReplyData;
				replyData->status = 0;
				replyData->psize = 4;
				replyData->vsize = 4;
				replyData->cmd = 20001;
				replyData->data = (int32_t)dspmain->jdsp.fs;
				*replySize = sizeof(reply1x4_1x4_t);
				return 0;
			}
			else if (cmd == 20002)
			{
				reply1x4_1x4_t *replyData = (reply1x4_1x4_t *)pReplyData;
				replyData->status = 0;
				replyData->psize = 4;
				replyData->vsize = 4;
				replyData->cmd = 20002;
				replyData->data = (int32_t)getpid();
				*replySize = sizeof(reply1x4_1x4_t);
				return 0;
			}
		}
	}
	if (cmdCode == EFFECT_CMD_SET_PARAM)
	{
		effect_param_t *cep = (effect_param_t *)pCmdData;
		int32_t *replyData = (int32_t *)pReplyData;
		if (cep->psize == 4 && cep->vsize == 2)
		{
			int32_t cmd = ((int32_t *)cep)[3];
			if (cmd == 128)
			{
				int reverbMode = (int)((int16_t *)cep)[8];
				Reverb_SetParam(&dspmain->jdsp, reverbMode);
#ifdef DEBUG
				LOGI("Reverb mode: %d", reverbMode);
#endif
				*replyData = 0;
				return 0;
			}
			else if (cmd == 137)
			{
				float agress = ((int16_t *)cep)[8] / 100.0f;
				StereoEnhancementSetParam(&dspmain->jdsp, agress);
#ifdef DEBUG
				LOGE("Stereo widen ag: %1.7f", agress);
#endif
				*replyData = 0;
				return 0;
			}
			else if (cmd == 188)
			{
				int16_t nMode = ((int16_t *)cep)[8];
				if (nMode < 0)
					nMode = 0;
				if (nMode > 5)
					nMode = 5;
				CrossfeedChangeMode(&dspmain->jdsp, nMode);
#ifdef DEBUG
				LOGI("Crossfeed mode: %d", nMode);
#endif
				*replyData = 0;
				return 0;
			}
			else if (cmd == 150)
			{
				float tubedrive = ((int16_t *)cep)[8] / 1000.0f;
#ifdef DEBUG
				LOGI("Tube drive: %1.7f", tubedrive);
#endif
				VacuumTubeSetGain(&dspmain->jdsp, tubedrive);
				*replyData = 0;
				return 0;
			}
			else if (cmd == 1200)
			{
				int16_t compressorEnabled = ((int16_t *)cep)[8];
				if (!compressorEnabled)
					CompressorDisable(&dspmain->jdsp);
				else
					CompressorEnable(&dspmain->jdsp);
#ifdef DEBUG
				LOGE("Compressor enabled: %d", compressorEnabled);
#endif
				*replyData = 0;
				return 0;
			}
			else if (cmd == 1201)
			{
				int16_t bassBoostEnabled = ((int16_t *)cep)[8];
				if (!bassBoostEnabled)
					BassBoostDisable(&dspmain->jdsp);
				else
					BassBoostEnable(&dspmain->jdsp);
#ifdef DEBUG
				LOGE("Bass boost enabled: %d", bassBoostEnabled);
#endif
				*replyData = 0;
				return 0;
			}
			else if (cmd == 112)
			{
				float maxg = ((int16_t *)cep)[8];
#ifdef DEBUG
				LOGE("Bass boost max gain: %f", maxg);
#endif
				BassBoostSetParam(&dspmain->jdsp, maxg);
				*replyData = 0;
				return 0;
			}
			else if (cmd == 1202)
			{
				int16_t equalizerEnabled = ((int16_t *)cep)[8];
				if (!equalizerEnabled)
					FIREqualizerDisable(&dspmain->jdsp);
				else
					FIREqualizerEnable(&dspmain->jdsp);
#ifdef DEBUG
				LOGE("FIR equalizer enabled: %d", equalizerEnabled);
#endif
				// Enable EQ
				*replyData = 0;
				return 0;
			}
			else if (cmd == 1203)
			{
				int16_t reverbEnabled = ((int16_t *)cep)[8];
				if (!reverbEnabled)
					ReverbDisable(&dspmain->jdsp);
				else
					ReverbEnable(&dspmain->jdsp);
#ifdef DEBUG
				LOGE("Reverb enabled: %d", reverbEnabled);
#endif
				*replyData = 0;
				return 0;
			}
			else if (cmd == 1204)
			{
				int16_t stereoWidenEnabled = ((int16_t *)cep)[8];
				if (!stereoWidenEnabled)
					StereoEnhancementDisable(&dspmain->jdsp);
				else
					StereoEnhancementEnable(&dspmain->jdsp);
#ifdef DEBUG
				LOGE("Stereo widen enabled: %d", stereoWidenEnabled);
#endif
				*replyData = 0;
				return 0;
			}
			else if (cmd == 1205)
			{
				int16_t convolverEnabled = ((int16_t *)cep)[8];
#ifdef DEBUG
				LOGE("Convolver enabled: %d", convolverEnabled);
#endif
				if (!convolverEnabled)
					Convolver1DDisable(&dspmain->jdsp);
				else
					Convolver1DEnable(&dspmain->jdsp);
				*replyData = 0;
				return 0;
			}
			else if (cmd == 1206)
			{
				int16_t analogueModelEnable = ((int16_t *)cep)[8];
				if (!analogueModelEnable)
					VacuumTubeDisable(&dspmain->jdsp);
				else
					VacuumTubeEnable(&dspmain->jdsp);
#ifdef DEBUG
				LOGE("Analogue modelling enabled: %d", analogueModelEnable);
#endif
				*replyData = 0;
				return 0;
			}
			else if (cmd == 1208)
			{
				int16_t bs2bEnabled = ((int16_t *)cep)[8];
				if (!bs2bEnabled)
					CrossfeedDisable(&dspmain->jdsp);
				else
					CrossfeedEnable(&dspmain->jdsp);
#ifdef DEBUG
				LOGE("Crossfeed enabled: %d", bs2bEnabled);
#endif
				*replyData = 0;
				return 0;
			}
			else if (cmd == 1210)
			{
				int16_t arbitraryResponseEnabled = ((int16_t *)cep)[8];
				if (!arbitraryResponseEnabled)
					ArbitraryResponseEqualizerDisable(&dspmain->jdsp);
				else
					ArbitraryResponseEqualizerEnable(&dspmain->jdsp);
#ifdef DEBUG
				LOGE("Arbitrary response eq enabled: %d", arbitraryResponseEnabled);
#endif
				*replyData = 0;
				return 0;
			}
			else if (cmd == 1212)
			{
				int16_t viperddcEnabled = ((int16_t *)cep)[8];
				int errCode = 0;
				if (!viperddcEnabled)
					DDCDisable(&dspmain->jdsp);
				else
					errCode = DDCEnable(&dspmain->jdsp);
#ifdef DEBUG
				LOGE("viperddcEnabled: %d, success?: %d", viperddcEnabled, errCode);
#endif
				*replyData = 0;
				return 0;
			}
			else if (cmd == 1213)
			{
				int16_t EELEnabled = ((int16_t *)cep)[8];
				if (!EELEnabled)
					LiveProgDisable(&dspmain->jdsp);
				else
					LiveProgEnable(&dspmain->jdsp);
#ifdef DEBUG
				LOGE("Liveprog enabled: %d", EELEnabled);
#endif
				*replyData = 0;
				return 0;
			}
			else if (cmd == 10004)
			{
				// Load impulse
#ifdef DEBUG
				if (dspmain->samplesInc == dspmain->numTime2Send)
					LOGI("Buffer slices complete, chs: %d, impLen: %d", dspmain->impChannels, dspmain->impulseLengthActual);
				else
					LOGI("Ops! Buffer slices got %d missing holes", dspmain->numTime2Send - dspmain->samplesInc);
#endif
				dspmain->samplesInc = 0;
				int success = Convolver1DLoadImpulseResponse(&dspmain->jdsp, dspmain->tempImpulseIncoming, dspmain->impChannels, dspmain->impulseLengthActual);
				free(dspmain->tempImpulseIncoming);
				dspmain->tempImpulseIncoming = 0;
#ifdef DEBUG
				LOGI("FFT convolver errCode: %d", success);
#endif
				*replyData = 0;
				return 0;
			}
			else if (cmd == 10006)
			{
				dspmain->stringIndex = 0;
				if (dspmain->stringEq)
				{
#ifdef DEBUG
				LOGI("%s", dspmain->stringEq);
#endif
					ArbitraryResponseEqualizerStringParser(&dspmain->jdsp, dspmain->stringEq);
					free(dspmain->stringEq);
					dspmain->stringEq = 0;
#ifdef DEBUG
				LOGI("Arbitrary response initialized");
#endif
				}
				*replyData = 0;
				return 0;
			}
			else if (cmd == 10009)
			{
				dspmain->stringIndex = 0;
				if (dspmain->stringEq)
				{
#ifdef DEBUG
				LOGI("%s", dspmain->stringEq);
#endif
					// Initialize DDC
					DDCStringParser(&dspmain->jdsp, dspmain->stringEq);
					free(dspmain->stringEq);
					dspmain->stringEq = 0;
#ifdef DEBUG
				LOGI("DDC initialized");
#endif
				}
				*replyData = 0;
				return 0;
			}
			else if (cmd == 10010)
			{
				dspmain->stringIndex = 0;
				if (dspmain->stringEq)
				{
#ifdef DEBUG
				LOGI("%s", dspmain->stringEq);
#endif
				// Initialize EEL
				int errorCode = LiveProgStringParser(&dspmain->jdsp, dspmain->stringEq);
				free(dspmain->stringEq);
				dspmain->stringEq = 0;
#ifdef DEBUG
				LOGI("Live prog error message: %s", checkErrorCode(errorCode));
#endif
				}
				*replyData = 0;
				return 0;
			}
		}
		if (cep->psize == 4 && cep->vsize == 12)
		{
			int32_t cmd = ((int32_t *)cep)[3];
			if (cmd == 1500) // Implemented
			{
				double limThreshold = (double)((float*)cep)[4];
				double limRelease = (double)((float*)cep)[5];
				double postgain = (double)((float*)cep)[6];
				if (limThreshold > -0.09)
					limThreshold = -0.09;
				if (limRelease < 0.15)
					limRelease = 0.15;
				if (postgain > 15.0)
					postgain = 15.0;
				if (postgain < -15.0)
					postgain = -15.0;
				JLimiterSetCoefficients(&dspmain->jdsp, limThreshold, limRelease);
				JamesDSPSetPostGain(&dspmain->jdsp, postgain);
#ifdef DEBUG
				LOGE("limThreshold: %f, limRelease: %f, postgain: %f", limThreshold, limRelease, postgain);
#endif
				*replyData = 0;
				return 0;
			}
		}
		if (cep->psize == 4 && cep->vsize == (4 * 3))
		{
			int32_t cmd = ((int32_t *)cep)[3];
			if (cmd == 115)
			{
				float maxAttack = ((float*)cep)[4 + 0];
				float maxRelease = ((float*)cep)[4 + 1];
				float adaptSpeed = ((float*)cep)[4 + 2];
#ifdef DEBUG
				LOGE("maxAttack: %f, maxRelease: %f, adaptSpeed: %f", maxAttack, maxRelease, adaptSpeed);
#endif
				CompressorSetParam(&dspmain->jdsp, maxAttack, maxRelease, adaptSpeed);
				*replyData = 0;
				return 0;
			}
		}
		if (cep->psize == 4 && cep->vsize == 128)
		{
			int32_t cmd = ((int32_t *)cep)[3];
			if (cmd == 116)
			{
				int filtertype = (((float*)cep)[4 + 0]) < 0.0f ? 0 : 1;
				int interpolationMode = (((float*)cep)[4 + 1]) < 0.0f ? 0 : 1;
				float *ptrFreqAxis = &((float*)cep)[4 + 2];
				float *ptrGainAxis = ptrFreqAxis + 15;
				double param[30];
				for (int i = 0; i < 15; i++)
				{
					param[i] = (double)ptrFreqAxis[i];
					param[i + 15] = (double)ptrGainAxis[i];
				}
#ifdef DEBUG
				LOGI("Eq axis: ");
				for (int i = 0; i < 15; i++)
				{
					LOGI("%1.7lf %1.7lf; ", param[i], param[i + 15]);
				}
#endif
				FIREqualizerAxisInterpolation(&dspmain->jdsp, interpolationMode, filtertype, param, param + 15);
				*replyData = 0;
				return 0;
			}
		}
		if (cep->psize == 4 && cep->vsize == 8)
		{
			int32_t cmd = ((int32_t *)cep)[3];
			if (cmd == 8888) // Implemented
			{
				int32_t times = ((int32_t *)cep)[4];
				int32_t sizePerBuffer = ((int32_t *)cep)[5];
				int stringLength = times * sizePerBuffer;
#ifdef DEBUG
				LOGI("Allocate %d string length", stringLength);
#endif
				dspmain->stringEq = (char*)calloc(stringLength, sizeof(char));
				*replyData = 0;
				return 0;
			}
		}
		if (cep->psize == 4 && cep->vsize == 16)
		{
			int32_t cmd = ((int32_t *)cep)[3];
			if (cmd == 9999) // Implemented
			{
				dspmain->impChannels = ((int32_t *)cep)[5];
				dspmain->impulseLengthActual = ((int32_t *)cep)[4] / dspmain->impChannels;
				float convGaindB = (float)((int32_t *)cep)[6] / 262144.0f;
				if (convGaindB > 50.0f)
					convGaindB = 50.0f;
				dspmain->numTime2Send = ((int32_t *)cep)[7];
				dspmain->tempImpulseIncoming = (float*)calloc(4096 * dspmain->impChannels * dspmain->numTime2Send, sizeof(float));
				*replyData = 0;
				return 0;
			}
		}
		if (cep->psize == 4 && cep->vsize == 16384)
		{
			int32_t cmd = ((int32_t *)cep)[3];
			if (cmd == 12000) // Implemented
			{
				memcpy(dspmain->tempImpulseIncoming + (dspmain->samplesInc++ * 4096), ((float*)cep) + 4, 4096 * sizeof(float));
				*replyData = 0;
				return 0;
			}
		}
		if (cep->psize == 4 && cep->vsize == 256)
		{
			int32_t cmd = ((int32_t *)cep)[3];
			if (cmd == 12001) // Implemented
			{
				memcpy(dspmain->stringEq + (dspmain->stringIndex++ * 256), ((char*)cep) + 16, 256 * sizeof(char));
				*replyData = 0;
				return 0;
			}
		}
		return -1;
	}
    switch (cmdCode)
    {
    case EFFECT_CMD_ENABLE:
    case EFFECT_CMD_DISABLE:
    {
        dspmain->mEnable = cmdCode == EFFECT_CMD_ENABLE;
        int32_t *replyData = (int32_t *) pReplyData;
        *replyData = 0;
        break;
    }
    case EFFECT_CMD_INIT:
    case EFFECT_CMD_SET_CONFIG:
    case EFFECT_CMD_SET_PARAM:
    case EFFECT_CMD_SET_PARAM_COMMIT:
    {
        int32_t *replyData = (int32_t *) pReplyData;
        *replyData = 0;
        break;
    }
    case EFFECT_CMD_RESET:
    case EFFECT_CMD_SET_PARAM_DEFERRED:
    case EFFECT_CMD_SET_DEVICE:
    case EFFECT_CMD_SET_AUDIO_MODE:
        break;
    case EFFECT_CMD_GET_PARAM:
    {
        effect_param_t *rep = (effect_param_t *) pReplyData;
        rep->status = -EINVAL;
        rep->psize = 0;
        rep->vsize = 0;
        *replySize = 12;
        break;
    }
    }
    return 0;
}

int32_t EffectDSPMainProcess(EffectDSPMain *dspmain, audio_buffer_t *in, audio_buffer_t *out)
{
	size_t actualFrameCount = in->frameCount;
	switch (dspmain->formatFloatModeInt32Mode)
	{
	case 0:
		dspmain->jdsp.processInt16Multiplexd(&dspmain->jdsp, in->s16, out->s16, actualFrameCount);
		break;
	case 1:
		dspmain->jdsp.processFloatMultiplexd(&dspmain->jdsp, in->f32, out->f32, actualFrameCount);
		break;
	case 2:
		dspmain->jdsp.processInt32Multiplexd(&dspmain->jdsp, in->s32, out->s32, actualFrameCount);
		break;
	}
	return dspmain->mEnable ? 0 : -ENODATA;
}
// Effect section end
static effect_descriptor_t jamesdsp_descriptor =
{
	{ 0xf98765f4, 0xc321, 0x5de6, 0x9a45, { 0x12, 0x34, 0x59, 0x49, 0x5a, 0xb2 } },
	{ 0xf27317f4, 0xc984, 0x4de6, 0x9a90, { 0x54, 0x57, 0x59, 0x49, 0x5b, 0xf2 } }, // own UUID
	EFFECT_CONTROL_API_VERSION,
	EFFECT_FLAG_TYPE_INSERT | EFFECT_FLAG_INSERT_FIRST,
	10,
	1,
	"JamesDSP v3.05",
	"James Fung"
};
__attribute__((constructor)) static void initialize(void)
{
	JamesDSPGlobalMemoryAllocation();
#ifdef DEBUG
	LOGI("Initialization: DLL loaded");
#endif
}
__attribute__((destructor)) static void destruction(void)
{
	JamesDSPGlobalMemoryDeallocation();
#ifdef DEBUG
	LOGI("Initialization: DLL unloaded");
#endif
}
// Library mandatory methods
struct effect_module_s
{
	const struct effect_interface_s *itfe;
	EffectDSPMain effect;
	effect_descriptor_t *descriptor;
};
static int32_t generic_process(effect_handle_t self, audio_buffer_t *in, audio_buffer_t *out)
{
	struct effect_module_s *e = (struct effect_module_s *) self;
	return EffectDSPMainProcess(&e->effect, in, out);
}
static int32_t generic_command(effect_handle_t self, uint32_t cmdCode, uint32_t cmdSize, void *pCmdData, uint32_t *replySize, void *pReplyData)
{
	struct effect_module_s *e = (struct effect_module_s *) self;
	return EffectDSPMainCommand(&e->effect, cmdCode, cmdSize, pCmdData, replySize, pReplyData);
}
static int32_t generic_getDescriptor(effect_handle_t self, effect_descriptor_t *pDescriptor)
{
	struct effect_module_s *e = (struct effect_module_s *) self;
	memcpy(pDescriptor, e->descriptor, sizeof(effect_descriptor_t));
	return 0;
}
static const struct effect_interface_s generic_interface =
{
	generic_process,
	generic_command,
	generic_getDescriptor,
	NULL
};
int32_t EffectCreate(const effect_uuid_t *uuid, int32_t sessionId, int32_t ioId, effect_handle_t *pEffect)
{
	(void)uuid;
	(void)ioId;
	struct effect_module_s *e = (struct effect_module_s *) calloc(1, sizeof(struct effect_module_s));
	e->itfe = &generic_interface;
#ifdef DEBUG
	LOGI("JamesDSP: Creating effect with %d sessionId", sessionId);
#endif
	EffectDSPMainConstructor(&e->effect);
	e->descriptor = &jamesdsp_descriptor;
	*pEffect = (effect_handle_t)e;
#ifdef DEBUG
	LOGI("JamesDSP: Effect created");
#endif
	return 0;
}
int32_t EffectRelease(effect_handle_t ei)
{
	struct effect_module_s *e = (struct effect_module_s *) ei;
	EffectDSPMainDestructor(&e->effect);
	free(e);
	return 0;
}
int32_t EffectGetDescriptor(const effect_uuid_t *uuid, effect_descriptor_t *pDescriptor)
{
#ifdef DEBUG
	LOGI("JamesDSP: Copying descriptor info");
#endif
	if (pDescriptor == NULL || uuid == NULL)
	{
#ifdef DEBUG
		LOGI("JamesDSP: EffectGetDescriptor() called with NULL pointer");
#endif
		return -EINVAL;
	}
	*pDescriptor = jamesdsp_descriptor;
	return 0;
}
__attribute__((visibility("default"))) audio_effect_library_t AUDIO_EFFECT_LIBRARY_INFO_SYM =
{
	.tag = AUDIO_EFFECT_LIBRARY_TAG,
	.version = EFFECT_LIBRARY_API_VERSION,
#if __aarch64__ == 1
		.name = "James Audio DSP arm64",
#elif __ARM_ARCH_7A__ == 1
		.name = "James Audio DSP arm32",
#elif __i386__ == 1
		.name = "James Audio DSP x86",
#elif __x86_64__ == 1
		.name = "James Audio DSP x64",
#endif
		.implementor = "James Fung",
		.create_effect = EffectCreate,
		.release_effect = EffectRelease,
		.get_descriptor = EffectGetDescriptor,
};
