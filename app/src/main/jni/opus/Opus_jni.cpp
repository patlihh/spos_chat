#include <jni.h>
#include <android/log.h>
#include "include/opus.h"
#include <malloc.h>

/* Header for class net_abcdefgh_opustrial_codec_Opus */
#ifndef _Included_net_abcdefgh_opustrial_codec_Opus
#define _Included_net_abcdefgh_opustrial_codec_Opus

#define SAMPLE_RATE 16000
#define CHANNEL_NUM 1
#define BIT_RATE 16000
#define BIT_PER_SAMPLE 16
#define WB_FRAME_SIZE 320
#define DATA_SIZE 1024 * 1024 * 4

OpusEncoder* enc = nullptr;
OpusDecoder* dec = nullptr;
#define TAG "Opus_JNI"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG , TAG,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO  , TAG,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN  , TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR , TAG,__VA_ARGS__)
#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT jint JNICALL Java_com_uni_cloud_chat_audio_OpusEncoder_nativeInitEncoder (JNIEnv *env, jobject obj, jint samplingRate, jint numberOfChannels, jint application)
{
    int error;
    int size;

    LOGD("Java_com_uni_cloud_chat_audio_OpusEncoder_nativeInitEncoder samplingRate=%d, num_ch=%d, app=%d",samplingRate, numberOfChannels, application);
    size = opus_encoder_get_size(1);
    enc = (OpusEncoder*)malloc(size);
    error = opus_encoder_init(enc, samplingRate, numberOfChannels, application);

    LOGD("Java_com_uni_cloud_chat_audio_OpusEncoder_nativeInitEncoder error0=%d", error);

    if (error) {
        free(enc);
        enc = nullptr;
    } else {
//        jclass cls = env->GetObjectClass(obj);
//        jfieldID fid = env->GetFieldID(cls, "address", "J");
//        env->SetLongField(obj, fid, (jlong)enc);
    }

    LOGD("Java_com_uni_cloud_chat_audio_OpusEncoder_nativeInitEncoder error1=%d", error);
    return error;
}

JNIEXPORT jint JNICALL Java_com_uni_cloud_chat_audio_OpusEncoder_nativeSetBitrate(JNIEnv *env, jobject obj, jint bitrate) {
//    jclass cls = env->GetObjectClass(obj);
//    jfieldID fid = env->GetFieldID(cls, "address", "J");
//    OpusEncoder* enc = (OpusEncoder*)(env->GetLongField(obj, fid));
    return opus_encoder_ctl(enc, OPUS_SET_BITRATE(bitrate));
}

JNIEXPORT jint JNICALL Java_com_uni_cloud_chat_audio_OpusEncoder_nativeSetComplexity(JNIEnv *env, jobject obj, jint complexity) {
//    jclass cls = env->GetObjectClass(obj);
//    jfieldID fid = env->GetFieldID( cls, "address", "J");
//    OpusEncoder* enc = (OpusEncoder*)(env->GetLongField(obj, fid));
    return opus_encoder_ctl(enc, OPUS_SET_COMPLEXITY(complexity));
}

JNIEXPORT jint JNICALL Java_com_uni_cloud_chat_audio_OpusEncoder_nativeEncodeShorts (JNIEnv *env, jobject obj, jshortArray in, jint frames, jbyteArray out)
{
//    jclass cls = env->GetObjectClass( obj);
//    jfieldID fid = env->GetFieldID(cls, "address", "J");
//    OpusEncoder* enc = (OpusEncoder*)(env->GetLongField( obj, fid));

    jint outputArraySize = env->GetArrayLength( out);

    jshort* audioSignal = env->GetShortArrayElements(in, 0);
    jbyte* encodedSignal = env->GetByteArrayElements(out, 0);

    int dataArraySize = opus_encode(enc, audioSignal, frames,
                                    (unsigned char *) encodedSignal, outputArraySize);

    env->ReleaseShortArrayElements(in,audioSignal,JNI_ABORT);
    env->ReleaseByteArrayElements(out,encodedSignal,0);

    return dataArraySize;
}

JNIEXPORT jint JNICALL Java_com_uni_cloud_chat_audio_OpusEncoder_nativeEncodeBytes (JNIEnv *env, jobject obj, jbyteArray in, jint frames, jbyteArray out)
{
//    jclass cls = env->GetObjectClass(obj);
//    jfieldID fid = env->GetFieldID(cls, "address", "J");
//    OpusEncoder* enc = (OpusEncoder*)(env->GetLongField(obj, fid));

    jint outputArraySize = env->GetArrayLength( out);

    jbyte* audioSignal = env->GetByteArrayElements( in, 0);
    jbyte* encodedSignal = env->GetByteArrayElements( out, 0);

    if (((unsigned long)audioSignal) % 2) {
        // Unaligned...
        return OPUS_BAD_ARG;
    }

    int dataArraySize = opus_encode(enc, (const opus_int16 *) audioSignal, frames,
                                    (unsigned char *) encodedSignal, outputArraySize);

    env->ReleaseByteArrayElements(in,audioSignal,JNI_ABORT);
    env->ReleaseByteArrayElements(out,encodedSignal,0);

    return dataArraySize;
}

JNIEXPORT jboolean JNICALL Java_com_uni_cloud_chat_audio_OpusEncoder_nativeReleaseEncoder (JNIEnv *env, jobject obj)
{
//    jclass cls = env->GetObjectClass( obj);
//    jfieldID fid = env->GetFieldID( cls, "address", "J");
//    OpusEncoder* enc = (OpusEncoder*)(env->GetLongField( obj, fid));
    if(enc ){
        free(enc);
        enc = nullptr;
    }


//   env->SetLongField( obj, fid, (jlong)NULL);

    return 1;
}

JNIEXPORT jint JNICALL Java_com_uni_cloud_chat_audio_OpusDecoder_nativeInitDecoder (JNIEnv *env, jobject obj, jint samplingRate, jint numberOfChannels)
{
    int size;
    int error;

    size = opus_decoder_get_size(numberOfChannels);
    dec = (OpusDecoder*)malloc(size);
    error = opus_decoder_init(dec, samplingRate, numberOfChannels);

    if (error) {
        free(dec);
        dec = nullptr;
    } else {
//        jclass cls = env->GetObjectClass( obj);
//        jfieldID fid = env->GetFieldID( cls, "address", "J");
//        env->SetLongField( obj, fid, (jlong)dec);
    }

    return error;
}

JNIEXPORT jint JNICALL Java_com_uni_cloud_chat_audio_OpusDecoder_nativeDecodeShorts (JNIEnv *env, jobject obj, jbyteArray in, jshortArray out, jint frames)
{
//    jclass cls = env->GetObjectClass( obj);
//    jfieldID fid = env->GetFieldID( cls, "address", "J");
//    OpusDecoder* dec = (OpusDecoder*)(env->GetLongField( obj, fid));

    jint inputArraySize = env->GetArrayLength( in);

    jbyte* encodedData = env->GetByteArrayElements( in, 0);
    jshort* decodedData = env->GetShortArrayElements( out, 0);
    int samples = opus_decode(dec, (const unsigned char *) encodedData, inputArraySize,
                              decodedData, frames, 0);
    env->ReleaseByteArrayElements(in,encodedData,JNI_ABORT);
    env->ReleaseShortArrayElements(out,decodedData,0);

    return samples;
}

JNIEXPORT jint JNICALL Java_com_uni_cloud_chat_audio_OpusDecoder_nativeDecodeBytes (JNIEnv *env, jobject obj, jbyteArray in, jbyteArray out, jint frames)
{
//    jclass cls = env->GetObjectClass( obj);
//    jfieldID fid = env->GetFieldID( cls, "address", "J");
//    OpusDecoder* dec = (OpusDecoder*)(env->GetLongField( obj, fid));

    jint inputArraySize = env->GetArrayLength( in);

    jbyte* encodedData = env->GetByteArrayElements( in, 0);
    jbyte* decodedData = env->GetByteArrayElements( out, 0);
    int samples = opus_decode(dec, (const unsigned char *) encodedData, inputArraySize,
                              (opus_int16 *) decodedData, frames, 0);
    env->ReleaseByteArrayElements(in,encodedData,JNI_ABORT);
    env->ReleaseByteArrayElements(out,decodedData,0);

    return samples;
}

JNIEXPORT jboolean JNICALL Java_com_uni_cloud_chat_audio_OpusDecoder_nativeReleaseDecoder (JNIEnv *env, jobject obj)
{
//    jclass cls = env->GetObjectClass( obj);
//    jfieldID fid = env->GetFieldID( cls, "address", "J");
//    OpusDecoder* enc = (OpusDecoder*)(env->GetLongField( obj, fid));
    if(dec){
        free(dec);
        dec = nullptr;
    }

//    env->SetLongField( obj, fid, (jlong)NULL);
    return 1;
}

#ifdef __cplusplus
}
#endif
#endif