#include <jni.h>
#include <string>
#include <android/log.h>

#include "SitMarkAudioBCDetectorAPI.h"

//region Helper methods.

#define MAXINSTANCES 10
bool initialized = false;
SitMarkAudioBCDetectorAPI *instances[ MAXINSTANCES ];

int saveDetectorApi(SitMarkAudioBCDetectorAPI* newDetector)
{
    if (! initialized)
    {
        for (int inx = 0; inx < MAXINSTANCES; ++inx)
        {
            instances[ inx ] = NULL;
        }

        initialized = true;
    }

    for (int inx = 0; inx < MAXINSTANCES; ++inx)
    {
        if (instances[ inx ] == NULL)
        {
            instances[ inx ] = newDetector;

            return inx;
        }
    }

    return -1;
}

SitMarkAudioBCDetectorAPI* findDetectorApi(int detectorId)
{
    if (! initialized) return NULL;

    return ((detectorId >= 0) && (detectorId < MAXINSTANCES)) ? instances[ detectorId ] : NULL;
}

void killDetectorApi(int detectorId)
{
    if (! initialized) return;

    if ((detectorId >= 0) && (detectorId < MAXINSTANCES)) instances[ detectorId ] = NULL;
}

//endregion Helper methods.

//region Static methods.

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

extern "C" jint Java_de_kappa_1mm_sitmark_sitmarkaudiobeaconbridge_SitMarkAudioBeaconBridge_createDetector(
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

    return saveDetectorApi(detector);

    double confidence;
    short *values = (short *) malloc(256 * 1024);

    int ml = detector->feedDetector(values, confidence);

    int detectorId = 0;

    //detectorId = saveDetectorApi(detector);

    __android_log_print(ANDROID_LOG_DEBUG, "KappaSitMarkAudioDetector: initializeHF=", "%d => %lx ML=%d", detectorId, (long) detector, ml);

    env->ReleaseStringUTFChars(key_, key);

    return detectorId;
}

//endregion Static methods.

//region Detector methods.

extern "C" jint Java_de_kappa_1mm_sitmark_sitmarkaudiobeaconbridge_SitMarkAudioBeaconBridge_getFrameSize(
        JNIEnv *env,
        jclass type,
        jint detectorId)
{
    SitMarkAudioBCDetectorAPI* detector = findDetectorApi(detectorId);

    return (detector != NULL) ? detector->getFrameSize() : -1;
}

extern "C" jint Java_de_kappa_1mm_sitmark_sitmarkaudiobeaconbridge_SitMarkAudioBeaconBridge_resetDetector(
        JNIEnv *env,
        jclass type,
        jint detectorId)
{
    SitMarkAudioBCDetectorAPI* detector = findDetectorApi(detectorId);

    if (detector != NULL) detector->reset();
}

extern "C" jint Java_de_kappa_1mm_sitmark_sitmarkaudiobeaconbridge_SitMarkAudioBeaconBridge_destroyDetector(
        JNIEnv *env,
        jclass type,
        jint detectorId)
{
    SitMarkAudioBCDetectorAPI* detector = findDetectorApi(detectorId);

    if (detector != NULL)
    {
        SitMarkAudioBCDetectorLibrary::destroyInstance(detector, NULL);
        killDetectorApi(detectorId);
    }
}

//enregion Detector methods.

