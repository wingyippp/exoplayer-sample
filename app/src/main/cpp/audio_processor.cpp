#include <jni.h>
#include <android/log.h>
#include <cstring>
#include <malloc.h>
#include <__algorithm/max.h>
#include <__algorithm/min.h>
#include "bass_boost.h"

#define LOG_TAG "AudioProcessor"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Define encoding constants matching Media3's C class
#define ENCODING_INVALID 0
#define ENCODING_PCM_8BIT 3
#define ENCODING_PCM_16BIT 2
#define ENCODING_PCM_16BIT_BIG_ENDIAN 0x10000000
#define ENCODING_PCM_24BIT 21
#define ENCODING_PCM_24BIT_BIG_ENDIAN 0x50000000
#define ENCODING_PCM_32BIT 22
#define ENCODING_PCM_32BIT_BIG_ENDIAN 0x60000000
#define ENCODING_PCM_FLOAT 4

// Gain multiplier for 50% loudness reduction
static const float GAIN = 0.5f;

extern "C" {
/**
 * Process 16-bit PCM audio data with 50% loudness reduction.
 */
void processPcm16Bit(void *inst, const int16_t* input, int16_t* output, jint numSamples) {
//    for (jint i = 0; i < numSamples; i++) {
//        int16_t sample = input[i];
//
//        // Apply gain - convert to float for precision
//        float processed = static_cast<float>(sample) * GAIN;
//
//        // Round and clamp
//        auto result = static_cast<int32_t>(processed);
//        result = std::max(static_cast<int32_t>(INT16_MIN),
//                          std::min(static_cast<int32_t>(INT16_MAX), result));
//
//        output[i] = static_cast<int16_t>(result);
//    }
    processBassBoost(inst, (int16_t*)input, output, numSamples);
}

/**
 * Process 32-bit float PCM audio data with 50% loudness reduction.
 */
void processPcmFloat(void *inst, const float* input, float* output, jint numSamples) {
//    for (jint i = 0; i < numSamples; i++) {
//        float sample = input[i];
//        float processed = sample * GAIN;
//        processed = std::max(-1.0f, std::min(1.0f, processed));
//        output[i] = processed;
//    }
    processBassBoostFloat(inst, (float*)input, output, numSamples);
}

/**
 * JNI method to process PCM audio with 50% loudness reduction.
 */
extern "C" JNIEXPORT void JNICALL
Java_com_example_audioprocessorsample_LoudnessReducerAudioProcessor_processPcm(
        JNIEnv *env,
        jobject thisObject,
        jobject inputBuffer,
        jobject outputBuffer,
        jint position,
        jint limit,
        jint encoding,
        jlong instancePointer) {

    // Get direct buffer addresses
    auto* inputBase = static_cast<uint8_t*>(env->GetDirectBufferAddress(inputBuffer));
    auto* outputBase = static_cast<uint8_t*>(env->GetDirectBufferAddress(outputBuffer));

    // Validate buffers
    if (inputBase == nullptr || outputBase == nullptr) {
        jclass exceptionClass = env->FindClass("java/lang/IllegalArgumentException");
        env->ThrowNew(exceptionClass, "Input and output buffers must be direct ByteBuffers");
        return;
    }

    // Validate position and limit
    if (position < 0 || limit < position) {
        jclass exceptionClass = env->FindClass("java/lang/IllegalArgumentException");
        env->ThrowNew(exceptionClass, "Invalid position or limit");
        return;
    }

    // Calculate size
    jint size = limit - position;

    // Adjust pointers to account for position
    uint8_t* inputData = inputBase + position;
    uint8_t* outputData = outputBase;

    // Process based on encoding type
    switch (encoding) {
        case ENCODING_PCM_16BIT: {
            jint numSamples = size / 2;
            processPcm16Bit((void *)instancePointer, reinterpret_cast<int16_t*>(inputData),
                            reinterpret_cast<int16_t*>(outputData),
                            numSamples);
            break;
        }

        case ENCODING_PCM_FLOAT: {
            jint numSamples = size / 4;
            processPcmFloat((void *)instancePointer, reinterpret_cast<float*>(inputData),
                            reinterpret_cast<float*>(outputData),
                            numSamples);
            break;
        }

        default:
            jclass exceptionClass = env->FindClass("java/lang/IllegalArgumentException");
            env->ThrowNew(exceptionClass, "Unsupported audio encoding");
            break;
    }
}

JNIEXPORT jlong JNICALL
Java_com_example_audioprocessorsample_LoudnessReducerAudioProcessor_onConfigureNative(
        JNIEnv *env,
        jobject thisObject,
        jint sampleRate,
        jint channelCount,
        jint bytesPerFrame) {
    // Called when the processor is configured for a new input format.
    LOGD("onConfigure");
    long inst = 0;
    inst = (long)initBassBoost();
    return inst;
}

JNIEXPORT void JNICALL
Java_com_example_audioprocessorsample_LoudnessReducerAudioProcessor_onResetNative(
        JNIEnv *env,
        jobject thisObject,
        jlong instancePointer) {
    // Called when the processor is reset.
    LOGD("onReset %lld", instancePointer);
    closeBassBoost((void *)instancePointer);
}

JNIEXPORT void JNICALL
Java_com_example_audioprocessorsample_LoudnessReducerAudioProcessor_onFlushNative(
        JNIEnv *env,
        jobject thisObject) {
    // Called when the processor is flushed.
    LOGD("onFlush");
}

JNIEXPORT void JNICALL
Java_com_example_audioprocessorsample_LoudnessReducerAudioProcessor_onQueueEndOfStreamNative(
        JNIEnv *env,
        jobject thisObject) {
    // Called when the end-of-stream is queued to the processor.
    LOGD("onQueueEndOfStream");
}

JNIEXPORT void JNICALL
Java_com_example_audioprocessorsample_LoudnessReducerAudioProcessor_setParamsNative(
        JNIEnv *env,
        jobject thisObject,
        jint sampleRate,
        jfloat gain,
        jfloat frequency,
        jfloat qValue
        ) {
    // Called when the processor is flushed.
    LOGD("setParamNative gain:%f frequency:%f qValue:%f", gain, frequency, qValue);
}

} // extern "C"