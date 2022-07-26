#include <android/log.h>

#define TAG "JamesDspWrapper_JNI"
#include <Log.h>

#include <string>
#include <jni.h>

#include "JamesDspWrapper.h"
#include "JArrayList.h"
#include "EelVariable.h"

extern "C" {
#include "../EELStdOutExtension.h"
#include <jdsp_header.h>
}

// C interop
inline JamesDSPLib* cast(void* raw){
    auto* ptr = static_cast<JamesDSPLib*>(raw);
    if(ptr == nullptr)
    {
        LOGE("JamesDspWrapper::cast: JamesDSPLib pointer is NULL")
    }
    return ptr;
}

#define THIS (reinterpret_cast<JamesDspWrapper*>(self))

inline int32_t arySearch(int32_t *array, int32_t N, int32_t x)
{
    for (int32_t i = 0; i < N; i++)
    {
        if (array[i] == x)
            return i;
    }
    return -1;
}

#define FLOIDX 20000
inline void* GetStringForIndex(eel_string_context_state *st, float val, int32_t write)
{
    auto castedValue = (int32_t)(val + 0.5f);
    if (castedValue < FLOIDX)
        return nullptr;
    int32_t idx = arySearch(st->map, st->slot, castedValue);
    if (idx < 0)
        return nullptr;
    if (!write)
    {
        s_str *tmp = &st->m_literal_strings[idx];
        const char *s = s_str_c_str(tmp);
        return (void*)s;
    }
    else
        return (void*)&st->m_literal_strings[idx];
}

extern "C" JNIEXPORT jlong JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_alloc(JNIEnv *env, jobject obj, jobject callback)
{
    auto* self = new JamesDspWrapper();
    self->callbackInterface = env->NewGlobalRef(callback);
    self->env = env;

    jclass callbackClass = env->GetObjectClass(callback);
    if (callbackClass == nullptr)
    {
        LOGE("JamesDspWrapper::ctor: Cannot find callback class");
        delete self;
        return 0;
    }
    else
    {
        self->callbackOnLiveprogOutput = env->GetMethodID(callbackClass, "onLiveprogOutput",
                                                      "(Ljava/lang/String;)V");
        self->callbackOnLiveprogExec = env->GetMethodID(callbackClass, "onLiveprogExec",
                                                    "(Ljava/lang/String;)V");
        self->callbackOnLiveprogResult = env->GetMethodID(callbackClass, "onLiveprogResult",
                                                          "(ILjava/lang/String;Ljava/lang/String;)V");
        self->callbackOnVdcParseError = env->GetMethodID(callbackClass, "onVdcParseError",
                                                          "()V");
        if (self->callbackOnLiveprogOutput == nullptr || self->callbackOnLiveprogExec == nullptr ||
            self->callbackOnLiveprogResult == nullptr || self->callbackOnVdcParseError == nullptr)
        {
            LOGE("JamesDspWrapper::ctor: Cannot find callback method");
            delete self;
            return 0;
        }
    }


    auto* _dsp = (JamesDSPLib*)malloc(sizeof(JamesDSPLib));
    memset(_dsp, 0, sizeof(JamesDSPLib));

    if(!_dsp)
    {
        LOGE("JamesDspWrapper::ctor: Failed to allocate memory for libjamesdsp class object");
        delete self;
        return 1;
    }

    JamesDSPGlobalMemoryAllocation();
    JamesDSPInit(_dsp, 128, 48000);

    if(!JamesDSPGetMutexStatus(_dsp))
    {
        LOGE("JamesDspWrapper::ctor: JamesDSPGetMutexStatus returned false. "
                    "Cannot run safely in multi-threaded environment.");
        JamesDSPFree(_dsp);
        JamesDSPGlobalMemoryDeallocation();
        delete self;
        return 2;
    }

    self->dsp = _dsp;

    LOGD("JamesDspWrapper::ctor: memory allocated at %ld", (long)self);
    return (long)self;
}

extern "C" JNIEXPORT void JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_free(JNIEnv *env, jobject obj, jlong self)
{
    setStdOutHandler(nullptr, nullptr);

    JamesDSPFree(cast(THIS->dsp));
    THIS->dsp = nullptr;

    JamesDSPGlobalMemoryDeallocation();

    env->DeleteGlobalRef(THIS->callbackInterface);
    delete THIS;

    LOGD("JamesDspWrapper::dtor: memory freed");
}

extern "C"
JNIEXPORT void JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_setSamplingRate(JNIEnv *env,
                                                                                jobject obj,
                                                                                jlong self,
                                                                                jfloat sample_rate,
                                                                                jboolean force_refresh)
{
    JamesDSPSetSampleRate(cast(THIS->dsp), sample_rate, force_refresh);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_isHandleValid(JNIEnv *env, jobject obj, jlong self)
{
    if(THIS == nullptr || cast(THIS->dsp) == nullptr)
    {
        return false;
    }
    return true;
}

extern "C"
JNIEXPORT jshortArray JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_processInt16(JNIEnv *env, jobject obj, jlong self, jshortArray inputObj)
{
    auto* dsp = cast(THIS->dsp);

    auto inputLength = env->GetArrayLength(inputObj);
    auto outputObj = env->NewShortArray(inputLength);

    auto input = env->GetShortArrayElements(inputObj, nullptr);
    auto output = env->GetShortArrayElements(outputObj, nullptr);
    dsp->processInt16Multiplexd(dsp, input, output, inputLength / 2);
    env->ReleaseShortArrayElements(inputObj, input, JNI_ABORT);
    env->ReleaseShortArrayElements(outputObj, output, 0);
    return outputObj;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_processInt32(JNIEnv *env, jobject obj,
                                                                        jlong self, jintArray inputObj)
{
    auto* dsp = cast(THIS->dsp);
    auto inputLength = env->GetArrayLength(inputObj);
    auto outputObj = env->NewIntArray(inputLength);

    auto input = env->GetIntArrayElements(inputObj, nullptr);
    auto output = env->GetIntArrayElements(outputObj, nullptr);
    dsp->processInt32Multiplexd(dsp, input, output, inputLength / 2);
    env->ReleaseIntArrayElements(inputObj, input, JNI_ABORT);
    env->ReleaseIntArrayElements(outputObj, output, 0);
    return outputObj;
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_processFloat(JNIEnv *env, jobject obj,
                                                                             jlong self, jfloatArray inputObj)
{
    auto* dsp = cast(THIS->dsp);

    auto inputLength = env->GetArrayLength(inputObj);
    auto outputObj = env->NewFloatArray(inputLength);

    auto input = env->GetFloatArrayElements(inputObj, nullptr);
    auto output = env->GetFloatArrayElements(outputObj, nullptr);

    dsp->processFloatMultiplexd(dsp, input, output, inputLength / 2);

    env->ReleaseFloatArrayElements(inputObj, input, JNI_ABORT);
    env->ReleaseFloatArrayElements(outputObj, output, 0);
    return outputObj;
}

extern "C" JNIEXPORT void JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_setLimiter(JNIEnv *env, jobject obj, jlong self, jfloat threshold, jfloat release)
{
    JLimiterSetCoefficients(cast(THIS->dsp), threshold, release);
}

extern "C" JNIEXPORT void JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_setPostGain(JNIEnv *env, jobject obj, jlong self, jfloat gain)
{
    JamesDSPSetPostGain(cast(THIS->dsp), gain);
}

extern "C" JNIEXPORT void JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_setFirEqualizer(JNIEnv *env, jobject obj, jlong self,
                                                                                jboolean enable, jint filterType, jint interpolationMode,
                                                                                jdoubleArray bands)
{
    auto* dsp = cast(THIS->dsp);
    if(env->GetArrayLength(bands) != 30)
    {
        LOGE("JamesDspWrapper::setFirEqualizer: Invalid EQ data. 30 semicolon-separated fields expected, "
                      "found %d fields instead.", env->GetArrayLength(bands));
        return;
    }

    if(bands == nullptr)
    {
        LOGW("JamesDspWrapper::setFirEqualizer: EQ band pointer is NULL. Disabling EQ");
        FIREqualizerDisable(dsp);
        return;
    }

    if(enable)
    {
        auto* nativeBands = (env->GetDoubleArrayElements(bands, nullptr));
        FIREqualizerAxisInterpolation(dsp, interpolationMode, filterType, nativeBands, nativeBands + 15);
        env->ReleaseDoubleArrayElements(bands, nativeBands, JNI_ABORT);
        FIREqualizerEnable(dsp);
    }
    else
    {
        FIREqualizerDisable(dsp);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_setVdc(JNIEnv *env, jobject obj, jlong self,
                                                                       jboolean enable, jstring vdcContents)
{
    auto* wrapper = THIS;
    auto* dsp = cast(wrapper->dsp);
    if(enable)
    {
        const char *nativeString = env->GetStringUTFChars(vdcContents, nullptr);
        DDCStringParser(dsp, (char*)nativeString);
        env->ReleaseStringUTFChars(vdcContents, nativeString);

        int ret = DDCEnable(dsp);
        if (ret <= 0)
        {
            LOGE("JamesDspWrapper::setVdc: Call to DDCEnable(wrapper->dsp) failed. Invalid DDC parameter?");
            LOGE("JamesDspWrapper::setVdc: Disabling DDC engine");
            env->CallVoidMethod(wrapper->callbackInterface, wrapper->callbackOnVdcParseError);

            DDCDisable(dsp);
            return;
        }
    }
    else
    {
        DDCDisable(dsp);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_setCompressor(JNIEnv *env, jobject obj, jlong self,
                                                                              jboolean enable, jfloat maxAttack, jfloat maxRelease, float adaptSpeed)
{
    auto* dsp = cast(THIS->dsp);
    if(enable)
    {
        CompressorSetParam(dsp, maxAttack, maxRelease, adaptSpeed);
        CompressorEnable(dsp);
    }
    else
    {
        CompressorDisable(dsp);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_setReverb(JNIEnv *env, jobject obj, jlong self,
                                                                          jboolean enable, jint preset)
{
    auto* dsp = cast(THIS->dsp);
    if(enable)
    {
        Reverb_SetParam(dsp, preset);
        ReverbEnable(dsp);
    }
    else
    {
        ReverbDisable(dsp);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_setConvolver(JNIEnv *env, jobject obj, jlong self,
                                                                             jboolean enable, jfloatArray impulseResponse,
                                                                             jint irChannels, jint irFrames)
{
    auto* dsp = cast(THIS->dsp);

    int success = 1;
    if(env->GetArrayLength(impulseResponse) <= 0)
    {
        LOGW("JamesDspWrapper::setConvolver: Impulse response array is empty. Disabling convolver");
        enable = false;
    }

    if(enable)
    {
        if(irFrames <= 0)
        {
            LOGW("JamesDspWrapper::setConvolver: Impulse response has zero frames");
        }

        LOGD("JamesDspWrapper::setConvolver: Impulse response loaded: channels=%d, frames=%d", irChannels, irFrames);

        Convolver1DDisable(dsp);

        auto* nativeImpulse = (env->GetFloatArrayElements(impulseResponse, nullptr));
        success = Convolver1DLoadImpulseResponse(dsp, nativeImpulse, irChannels, irFrames);
        env->ReleaseFloatArrayElements(impulseResponse, nativeImpulse, JNI_ABORT);
    }

    if(enable)
        Convolver1DEnable(dsp);
    else
        Convolver1DDisable(dsp);

    if(success <= 0)
    {
        LOGD("JamesDspWrapper::setConvolver: Failed to update convolver. Convolver1DLoadImpulseResponse returned an error.");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_setGraphicEq(JNIEnv *env, jobject obj, jlong self,
                                                                             jboolean enable, jstring graphicEq)
{
    auto* dsp = cast(THIS->dsp);
    if(graphicEq == nullptr || env->GetStringUTFLength(graphicEq) <= 0)
    {
        LOGE("JamesDspWrapper::setGraphicEq: graphicEq is empty or NULL. Disabling graphic eq.");
        enable = false;
    }

    if(enable)
    {
        const char *nativeString = env->GetStringUTFChars(graphicEq, nullptr);
        ArbitraryResponseEqualizerStringParser(dsp, (char*)nativeString);
        env->ReleaseStringUTFChars(graphicEq, nativeString);

        ArbitraryResponseEqualizerEnable(dsp);
    }
    else
        ArbitraryResponseEqualizerDisable(dsp);
}

extern "C" JNIEXPORT void JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_setCrossfeed(JNIEnv *env, jobject obj, jlong self,
                                                                             jboolean enable, jint mode, jint customFcut, jint customFeed)
{
    auto* dsp = cast(THIS->dsp);
    if(mode == 99)
    {
        memset(&dsp->advXF.bs2b, 0, sizeof(dsp->advXF.bs2b));
        BS2BInit(&dsp->advXF.bs2b[1], (unsigned int)dsp->fs, ((unsigned int)customFcut | ((unsigned int)customFeed << 16)));
        dsp->advXF.mode = 1;
    }
    else
    {
       CrossfeedChangeMode(dsp, mode);
    }

    if(enable)
        CrossfeedEnable(dsp);
    else
        CrossfeedDisable(dsp);
}

extern "C" JNIEXPORT void JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_setBassBoost(JNIEnv *env, jobject obj, jlong self,
                                                                             jboolean enable, jfloat maxGain)
{
    auto* wrapper = THIS;
    auto* dsp = cast(wrapper->dsp);
    if(enable)
    {
        BassBoostSetParam(dsp, maxGain);
        BassBoostEnable(dsp);
    }
    else
    {
        BassBoostDisable(dsp);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_setStereoEnhancement(JNIEnv *env, jobject obj, jlong self,
                                                                                     jboolean enable, jfloat level)
{
    auto* dsp = cast(THIS->dsp);
    StereoEnhancementDisable(dsp);
    StereoEnhancementSetParam(dsp, level / 100.0f);
    if(enable)
    {
        StereoEnhancementEnable(dsp);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_setVacuumTube(JNIEnv *env, jobject obj, jlong self,
                                                                              jboolean enable, jfloat level)
{
    auto* dsp = cast(THIS->dsp);
    if(enable)
    {
        VacuumTubeSetGain(dsp, level / 100.0f);
        VacuumTubeEnable(dsp);
    }
    else
    {
        VacuumTubeDisable(dsp);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_setLiveprog(JNIEnv *env, jobject obj, jlong self,
                                                                            jboolean enable, jstring id, jstring liveprogContent)
{
    auto* wrapper = THIS;

    // Attach log listener
    setStdOutHandler(receiveLiveprogStdOut, wrapper);

    auto* dsp = cast(wrapper->dsp);
    LiveProgDisable(dsp);

    env->CallVoidMethod(wrapper->callbackInterface, wrapper->callbackOnLiveprogExec, id);

    const char *nativeString = env->GetStringUTFChars(liveprogContent, nullptr);
    int ret = LiveProgStringParser(dsp, (char*)nativeString); // Ignore constness, libjamesdsp does not modify it
    env->ReleaseStringUTFChars(liveprogContent, nativeString);

    // Workaround due to library bug
    jdsp_unlock(dsp);

    const char* errorString = NSEEL_code_getcodeerror(dsp->eel.vm);
    if(errorString != nullptr)
    {
        LOGW("JamesDspWrapper::setLiveprog: NSEEL_code_getcodeerror: Syntax error in script file, cannot load. Reason: %s", errorString);
    }
    if(ret <= 0)
    {
        LOGW("JamesDspWrapper::setLiveprog: %s", checkErrorCode(ret));
    }

    jstring errorStringJni = env->NewStringUTF(errorString);
    env->CallVoidMethod(wrapper->callbackInterface, wrapper->callbackOnLiveprogResult, ret, id, errorStringJni);
    env->DeleteLocalRef(errorStringJni);

    if(enable)
        LiveProgEnable(dsp);
    else
        LiveProgDisable(dsp);
}


extern "C" JNIEXPORT jobject JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_enumerateEelVariables(JNIEnv *env, jobject obj, jlong self)
{
    auto array = JArrayList(env);

    auto *ctx = (compileContext*)cast(THIS->dsp)->eel.vm;
    for (int i = 0; i < ctx->varTable_numBlocks; i++)
    {
        for (int j = 0; j < NSEEL_VARS_PER_BLOCK; j++)
        {
            const char *valid = (char*)GetStringForIndex(ctx->m_string_context, ctx->varTable_Values[i][j], 0);
            bool isString = valid;

            if (ctx->varTable_Names[i][j])
            {
                const char* name = ctx->varTable_Names[i][j];
                const char* value;

                if(isString)
                    value = valid;
                else
                    value = std::to_string(ctx->varTable_Values[i][j]).c_str();

                auto var = EelVariable(env, name, value, isString);
                array.add(var.getJavaReference());
            }
        }
    }

    return array.getJavaReference();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_manipulateEelVariable(JNIEnv *env, jobject obj, jlong self,
                                                                                      jstring name, jfloat value)
{
    auto* dsp = cast(THIS->dsp);
    auto* ctx = (compileContext*)dsp->eel.vm;
    for (int i = 0; i < ctx->varTable_numBlocks; i++)
    {
        for (int j = 0; j < NSEEL_VARS_PER_BLOCK; j++)
        {
            const char *nativeName = env->GetStringUTFChars(name, nullptr);
            if(!ctx->varTable_Names[i][j] || std::strcmp(ctx->varTable_Names[i][j], nativeName) != 0)
            {
                env->ReleaseStringUTFChars(name, nativeName);
                continue;
            }


            char *validString = (char*)GetStringForIndex(ctx->m_string_context, ctx->varTable_Values[i][j], 0);
            if(validString)
            {
                LOGE("JamesDspWrapper::manipulateEelVariable: variable '%s' is a string; currently only numerical variables can be manipulated", nativeName);
                env->ReleaseStringUTFChars(name, nativeName);
                return false;
            }

            ctx->varTable_Values[i][j] = value;

            env->ReleaseStringUTFChars(name, nativeName);
            return true;
        }
    }

    const char *nativeName = env->GetStringUTFChars(name, nullptr);
    LOGE("JamesDspWrapper::manipulateEelVariable: variable '%s' not found", nativeName);
    env->ReleaseStringUTFChars(name, nativeName);
    return false;
}

extern "C" JNIEXPORT void JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_freezeLiveprogExecution(JNIEnv *env, jobject obj, jlong self,
                                                                                        jboolean freeze)
{
    cast(THIS->dsp)->eel.active = !freeze;
    LOGD("JamesDspWrapper::freezeLiveprogExecution: Liveprog execution has been %s", (freeze ? "frozen" : "resumed"));
}

extern "C" JNIEXPORT jstring JNICALL
Java_me_timschneeberger_rootlessjamesdsp_native_JamesDspWrapper_eelErrorCodeToString(JNIEnv *env,
                                                                                     jobject obj,
                                                                                     jint error_code)
{
    return env->NewStringUTF(checkErrorCode(error_code));
}

void receiveLiveprogStdOut(const char *buffer, void* userData)
{
    auto* self = static_cast<JamesDspWrapper*>(userData);
    if(self == nullptr)
    {
        LOGE("JamesDspWrapper::receiveLiveprogStdOut: Self reference is NULL");
        LOGE("JamesDspWrapper::receiveLiveprogStdOut: Unhandled output: %s", buffer);
        return;
    }

    self->env->CallVoidMethod(self->callbackInterface, self->callbackOnLiveprogOutput, self->env->NewStringUTF(buffer));
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *, void *)
{
    firebase::crashlytics::Initialize();
    LOGD("JNI_OnLoad called")
    return JNI_VERSION_1_6;
}
