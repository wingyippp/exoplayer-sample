#include <jni.h>
#include <android/log.h>
#include <cstring>
#include <malloc.h>
#include <__algorithm/max.h>
#include <__algorithm/min.h>

#define LOG_TAG "AudioProcessor"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// Define encoding constants matching Media3's C class
#define ENCODING_PCM_16BIT 2
#define ENCODING_PCM_FLOAT 4

// Gain multiplier for 50% loudness reduction
static const float GAIN = 0.5f;

extern "C" {
/**
 * Process 16-bit PCM audio data with 50% loudness reduction.
 */
void processPcm16Bit(uint8_t* input, uint8_t* output, jint size) {
    // Cast to int16_t pointers for easier processing
    int16_t* inputSamples = reinterpret_cast<int16_t*>(input);
    int16_t* outputSamples = reinterpret_cast<int16_t*>(output);

    // Calculate number of 16-bit samples
    jint numSamples = size / sizeof(int16_t);

    // Process each 16-bit sample
    for (jint i = 0; i < numSamples; i++) {
        // Read 16-bit signed integer sample
        int16_t sample = inputSamples[i];

        // Apply gain (reduce to 50%)
        int32_t processedSample = static_cast<int32_t>(sample * GAIN);

        // Clamp to 16-bit range
        processedSample = std::max(static_cast<int32_t>(INT16_MIN),
                                   std::min(static_cast<int32_t>(INT16_MAX), processedSample));

        // Write processed sample
        outputSamples[i] = static_cast<int16_t>(processedSample);
    }
}

/**
 * Process 32-bit float PCM audio data with 50% loudness reduction.
 */
void processPcmFloat(uint8_t* input, uint8_t* output, jint size) {
    // Cast to float pointers for easier processing
    float* inputFloats = reinterpret_cast<float*>(input);
    float* outputFloats = reinterpret_cast<float*>(output);

    // Calculate number of float samples
    jint numSamples = size / sizeof(float);

    // Process each 32-bit float sample
    for (jint i = 0; i < numSamples; i++) {
        // Read float sample (typically in range [-1.0, 1.0])
        float sample = inputFloats[i];

        // Apply gain (reduce to 50%)
        float processedSample = sample * GAIN;

        // Clamp to valid float range
        processedSample = std::max(-1.0f, std::min(1.0f, processedSample));

        // Write processed sample
        outputFloats[i] = processedSample;
    }
}

/**
 * JNI method to process PCM audio with 50% loudness reduction.
 */
extern "C" JNIEXPORT void JNICALL
Java_com_example_audioprocessorsample_LoudnessReducerAudioProcessor_processPcm(
        JNIEnv *env,
        jobject thiz,
        jobject inputBuffer,
        jobject outputBuffer,
        jint position,
        jint limit,
        jint encoding) {

    // Get direct buffer addresses
    uint8_t* inputBase = static_cast<uint8_t*>(env->GetDirectBufferAddress(inputBuffer));
    uint8_t* outputBase = static_cast<uint8_t*>(env->GetDirectBufferAddress(outputBuffer));

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

    // Calculate size and adjust pointers to account for position
    jint size = limit - position;
    uint8_t* inputData = inputBase + position;
    uint8_t* outputData = outputBase; // Output buffer position is already at 0

    // Process based on encoding type
    switch (encoding) {
        case ENCODING_PCM_16BIT:
            processPcm16Bit(inputData, outputData, size);
            break;

        case ENCODING_PCM_FLOAT:
            processPcmFloat(inputData, outputData, size);
            break;

        default:
            // Unsupported encoding
            jclass exceptionClass = env->FindClass("java/lang/IllegalArgumentException");
            env->ThrowNew(exceptionClass, "Unsupported audio encoding");
            break;
    }
}

JNIEXPORT jobject JNICALL
Java_com_example_audioprocessorsample_NoopAudioProcessor_processBufferNative(
        JNIEnv *env,
        jobject thiz,
        jobject inputBuffer,
        jint sampleRate,
        jint channelCount,
        jint bytesPerFrame,
        jlong instancePointer) {

    // 1. 获取 ByteBuffer 的直接缓冲区指针
    void *inputPtr = env->GetDirectBufferAddress(inputBuffer);
    jlong capacity = env->GetDirectBufferCapacity(inputBuffer);

    if (inputPtr == nullptr || capacity <= 0) {
        LOGD("Invalid ByteBuffer");
        return nullptr;
    }

    // 2. 创建输出 ByteBuffer（分配同样大小）
    jobject outputBuffer = env->NewDirectByteBuffer(malloc(capacity), capacity);
    void *outputPtr = env->GetDirectBufferAddress(outputBuffer);

    // 3. 处理音频数据（示例：音量减半）
    auto *inputAudio = static_cast<int16_t*>(inputPtr);
    auto *outputAudio = static_cast<int16_t*>(outputPtr);
    int sampleCount = capacity / sizeof(int16_t);

    for (int i = 0; i < sampleCount; i++) {
        // 简单的音频处理：音量降低为50%
        outputAudio[i] = inputAudio[i] / 2;
    }

    // 4. 返回处理后的 ByteBuffer
    return outputBuffer;
}

JNIEXPORT jlong JNICALL
Java_com_example_audioprocessorsample_NoopAudioProcessor_onConfigureNative(
        JNIEnv *env,
        jobject thiz,
        jint sampleRate,
        jint channelCount,
        jint bytesPerFrame) {
    // Called when the processor is configured for a new input format.
    LOGD("onConfigure");
    return 2;
}

JNIEXPORT void JNICALL
Java_com_example_audioprocessorsample_NoopAudioProcessor_onResetNative(
        JNIEnv *env,
        jobject thiz,
        jlong instancePointer) {
    // Called when the processor is reset.
    LOGD("onReset %lld", instancePointer);
}

JNIEXPORT void JNICALL
Java_com_example_audioprocessorsample_NoopAudioProcessor_onFlushNative(
        JNIEnv *env,
        jobject thiz) {
    // Called when the processor is flushed.
    LOGD("onFlush");
}

JNIEXPORT void JNICALL
Java_com_example_audioprocessorsample_NoopAudioProcessor_onQueueEndOfStreamNative(
        JNIEnv *env,
        jobject thiz) {
    // Called when the end-of-stream is queued to the processor.
    LOGD("onQueueEndOfStream");
}

} // extern "C"