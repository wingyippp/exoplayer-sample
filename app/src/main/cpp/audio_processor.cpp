#include <jni.h>
#include <android/log.h>
#include <cstring>
#include <malloc.h>

#define LOG_TAG "AudioProcessor"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C" {

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