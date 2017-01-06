#include <jni.h>
#include <string>
#include <android/log.h>

#include "SitMarkAudioBCDetectorAPI.h"

extern "C" jstring Java_de_kappa_1mm_sitmark_sitmarkaudiobeaconbridge_SitMarkAudioBeaconBridge_initializeDecoder(
        JNIEnv *env,
        jobject /* this */)
{
    std::string hello = "Hello from C++ (initializeDecoder)";
    return env->NewStringUTF(hello.c_str());
}

extern "C" jstring Java_de_kappa_1mm_sitmark_sitmarkaudiobeaconbridge_SitMarkAudioBeaconBridge_getVersionString(
        JNIEnv *env,
        jclass type)
{
    int version[256];
    char build[256];
    bool demo;

    SitMarkAudioBCDetectorLibrary::getLibraryInfo(version, build, &demo);

    char versionStr[256];
    sprintf(versionStr, "%02d.%02d.%04d:%d", version[2], version[1], version[0], version[3]);

    std::string resultStr = "";

    resultStr.append(build);
    resultStr.append(" - ");
    resultStr.append(versionStr);
    resultStr.append(" - ");
    resultStr.append(demo ? "Demo" : "Licensed");

    return env->NewStringUTF(resultStr.c_str());
}

extern "C" jint Java_de_kappa_1mm_sitmark_sitmarkaudiobeaconbridge_SitMarkAudioBeaconBridge_initializeHF(
        JNIEnv *env,
        jclass type,
        jint netMessLen,
        jint freqMin,
        jint freqMax,
        jint useEcc,
        jint wmRedundancy,
        jint sampleFreq,
        jint ringBufferLength,
        jstring key_,
        jboolean hfMode,
        jdouble playlength2BitSequence)
{
    const char *key = env->GetStringUTFChars(key_, 0);

    SitMarkAudioBCDetectorAPI* detector = SitMarkAudioBCDetectorLibrary::createInstance(
            SITMARKAUDIOBCDETECTORAPI_TYPE_WATERMARK,
            netMessLen, sampleFreq, freqMin, freqMax,
            useEcc, wmRedundancy, ringBufferLength,
            key, NULL, hfMode, playlength2BitSequence
    );

    __android_log_print(ANDROID_LOG_DEBUG, "KappaSitMarkAudioDetector: createInstance=", "%ld", (long) detector);

    detector->initialize();

    __android_log_print(ANDROID_LOG_DEBUG, "KappaSitMarkAudioDetector: initialize=", "%ld", (long) detector);

    detector->reset();

    __android_log_print(ANDROID_LOG_DEBUG, "KappaSitMarkAudioDetector: reset=", "%ld", (long) detector);

    int frameSize = detector->getFrameSize();

    __android_log_print(ANDROID_LOG_DEBUG, "KappaSitMarkAudioDetector: getFrameSize=", "%d", frameSize);

    return 0;

    double confidence;
    short *values = (short *) malloc(256 * 1024);

    int ml = detector->feedDetector(values, confidence);

    int detectorId = 0;

    //detectorId = saveDetectorApi(detector);

    __android_log_print(ANDROID_LOG_DEBUG, "KappaSitMarkAudioDetector: initializeHF=", "%d => %lx ML=%d", detectorId, (long) detector, ml);

    env->ReleaseStringUTFChars(key_, key);

    return detectorId;
}
