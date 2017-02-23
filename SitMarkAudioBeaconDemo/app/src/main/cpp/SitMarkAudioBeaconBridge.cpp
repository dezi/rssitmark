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
    SitMarkAudioBCDetectorAPI* detector = NULL;

    if (hfMode)
    {
        __android_log_write(ANDROID_LOG_INFO, "SitMarkAudioBeaconBridge: createDetector", "hf");

        const char *key = env->GetStringUTFChars(key_, 0);

        detector = SitMarkAudioBCDetectorLibrary::createInstance(
                SITMARKAUDIOBCDETECTORAPI_TYPE_WATERMARK,
                netMessLen, sampleFreq, freqMin, freqMax,
                useEcc, wmRedundancy, ringBufferLength,
                key, NULL, hfMode, playlength2BitSequence
        );
    }
    else
    {
        __android_log_write(ANDROID_LOG_INFO, "SitMarkAudioBeaconBridge: createDetector", "sw");

        detector = SitMarkAudioBCDetectorLibrary::createInstance(
                SITMARKAUDIOBCDETECTORAPI_TYPE_WATERMARK,
                netMessLen, sampleFreq, freqMin, freqMax,
                useEcc, wmRedundancy, ringBufferLength,
                NULL, NULL, hfMode, -1.0
        );
    }

    detector->initialize();
    detector->reset();

    return saveDetectorApi(detector);
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

    if (detector != NULL)
    {
        detector->reset();

        return 0;
    }

    return -1;
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

        return 0;
    }

    return -1;
}

extern "C" jint Java_de_kappa_1mm_sitmark_sitmarkaudiobeaconbridge_SitMarkAudioBeaconBridge_searchSync(
        JNIEnv *env,
        jclass type,
        jint detectorId,
        jbyteArray audioData)
{
    SitMarkAudioBCDetectorAPI* detector = findDetectorApi(detectorId);

    if (detector != NULL)
    {
        int iShift = 0;
        bool bUpdateScores = 0;
        double confidence = 0.0;

        jbyte* audioBytes = env->GetByteArrayElements(audioData, NULL);
        short int* frames = reinterpret_cast<short int*>(audioBytes);

        detector->feedDetector_energyEfficentHF(frames, confidence, iShift, bUpdateScores);

        env->ReleaseByteArrayElements(audioData, audioBytes, JNI_ABORT);

        return iShift;
    }

    return -1;
}

extern "C" jdouble Java_de_kappa_1mm_sitmark_sitmarkaudiobeaconbridge_SitMarkAudioBeaconBridge_detectBeacon(
        JNIEnv *env,
        jclass type,
        jint detectorId,
        jbyteArray audioData)
{
    SitMarkAudioBCDetectorAPI* detector = findDetectorApi(detectorId);

    if (detector != NULL)
    {
        jbyte* audioBytes = env->GetByteArrayElements(audioData, NULL);
        short int* frames = reinterpret_cast<short int*>(audioBytes);

        int iShift = 0;
        bool bUpdateScores = 1;
        double confidence = 0.0;

        detector->feedDetector_energyEfficentHF(frames, confidence, iShift, bUpdateScores);

        env->ReleaseByteArrayElements(audioData, audioBytes, JNI_ABORT);

        return confidence;
    }

    return -1.0;
}

extern "C" jdouble Java_de_kappa_1mm_sitmark_sitmarkaudiobeaconbridge_SitMarkAudioBeaconBridge_detectWatermark(
        JNIEnv *env,
        jclass type,
        jint detectorId,
        jbyteArray audioData)
{
    SitMarkAudioBCDetectorAPI* detector = findDetectorApi(detectorId);

    if (detector != NULL)
    {
        jbyte* audioBytes = env->GetByteArrayElements(audioData, NULL);
        short int* frames = reinterpret_cast<short int*>(audioBytes);

        double confidence = 0.0;

        detector->feedDetector(frames, confidence);

        env->ReleaseByteArrayElements(audioData, audioBytes, JNI_ABORT);

        return confidence;
    }

    return -1.0;
}

extern "C" jdouble Java_de_kappa_1mm_sitmark_sitmarkaudiobeaconbridge_SitMarkAudioBeaconBridge_getAccumulatedMessage(
        JNIEnv *env,
        jclass type,
        jint detectorId,
        jcharArray messageBuffer)
{
    SitMarkAudioBCDetectorAPI* detector = findDetectorApi(detectorId);

    if (detector != NULL)
    {
        jint messagePlusCrcLength = env->GetArrayLength(messageBuffer);
        jchar *messageChars = env->GetCharArrayElements(messageBuffer, NULL);

        char decodedMessage[ 128 ];

        bool correct = false; // android requires a reference, rvalues are not allowed
        double score = detector->getAccumulatedMessage(decodedMessage, correct);

        for (int i = 0; i < messagePlusCrcLength; i++)
        {
            messageChars[i] = decodedMessage[i];
        }

        env->ReleaseCharArrayElements(messageBuffer, messageChars, JNI_COMMIT);
        env->ReleaseCharArrayElements(messageBuffer, messageChars, JNI_ABORT);

        return score;
    }

    return -1.0;
}

//enregion Detector methods.

