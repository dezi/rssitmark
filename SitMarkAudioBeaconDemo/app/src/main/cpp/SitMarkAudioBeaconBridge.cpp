#include <jni.h>
#include <string>

// ================================================================================================
// Interface Version
// ================================================================================================

#ifdef _WIN32
#ifdef SITMARKAUDIOBCDETECTORAPI_EXPORTS
#define SITMARKAUDIOBCDETECTORAPI_API __declspec(dllexport)
#else
#if !defined(_LIB) && !defined(_WINRT_DLL)
#define SITMARKAUDIOBCDETECTORAPI_API __declspec(dllimport)
#else
#define SITMARKAUDIOBCDETECTORAPI_API /*static lib*/
#endif
#endif
#else
#if __GNUC__ >= 4
#define SITMARKAUDIOBCDETECTORAPI_API __attribute__ ((visibility ("default")))
#else
#define SITMARKAUDIOBCDETECTORAPI_API
#endif
#endif

//Must be increased on any change that breaks binary compatibility !!!
#define SITMARKAUDIOBCDETECTORAPI_CORE 2

//Helper macros (required by the C pre-processor in order to glue two strings)
#define SITMARKAUDIOBCDETECTORAPI_GLUE_HELPER(X, Y) X##Y
#define SITMARKAUDIOBCDETECTORAPI_GLUE(X, Y) SITMARKAUDIOBCDETECTORAPI_GLUE_HELPER(X,Y)

//The following two lines define the actual class names
#define SitMarkAudioBCDetectorAPI     SITMARKAUDIOBCDETECTORAPI_GLUE(SitMarkAudioBCDetectorAPI_r,     SITMARKAUDIOBCDETECTORAPI_CORE)
#define SitMarkAudioBCDetectorLibrary SITMARKAUDIOBCDETECTORAPI_GLUE(SitMarkAudioBCDetectorLibrary_r, SITMARKAUDIOBCDETECTORAPI_CORE)

// ================================================================================================
// Constants
// ================================================================================================

typedef enum
{
    SITMARKAUDIOBCDETECTORAPI_TYPE_WATERMARK = 0,    //Broadcast Watermark
    SITMARKAUDIOBCDETECTORAPI_TYPE_ROBUST_HASH = 1,    //RMAC

} SitMarkAudioBCDetectorAPI_t;

// ================================================================================================
// Generic Detector Interface
// ================================================================================================

class SITMARKAUDIOBCDETECTORAPI_API SitMarkAudioBCDetectorAPI
{
public:
    SitMarkAudioBCDetectorAPI(void)
    { }
    //void ~SitMarkAudioBCDetectorAPI(void) {}

    //Detector API
    virtual void initialize(void) = 0;

    virtual int feedDetector(const short *in_frame, double &dConfidenceLevelOut) = 0;

    virtual int feedDetector_HF(const short *in_frame, double &dConfidenceLevelOut, int &shift,
                                bool &update_score) = 0;

    virtual double getAccumulatedMessage(char *out_pNetMessage, bool &bCorrect) = 0;

    virtual int getSubMessages(char *out_pSubMessages, int &out_iCount,
                               const size_t uiMaxSize) const = 0;

    virtual int getFrameSize(void) const = 0;

    virtual int getDecodedMessageLen(void) const = 0;

    /*result does not(!) include the extra byte for the NULL-terminator*/
    virtual void reset(void) = 0;
};

class SITMARKAUDIOBCDETECTORAPI_API SitMarkAudioBCDetectorLibrary
{
public:
    //Check for available detector modes
    static bool queryDetectorType(const int &detectorType);

    //Creates new detector instance
    static SitMarkAudioBCDetectorAPI *createInstance(const int &detectorType,
                                                     const int &iNetMessLen, const int &sample_freq,
                                                     const int &freq_min, const int &freq_max,
                                                     const int &use_ecc,
                                                     const int &in_iWMRedundancy,
                                                     const int &in_iRingBufferLength = -1,
                                                     const char *const in_key = NULL,
                                                     FILE *const out_log = NULL,
                                                     bool hf_mode = false,
                                                     double playlength2bit = -1.0);

    //Destroy existing detector instance
    static void destroyInstance(SitMarkAudioBCDetectorAPI *&embedderInstance,
                                FILE *const out_log = NULL);

    // Get library informatiom
    // Minimum required array size for output: versionDateOut[4], buildInfoOut[32], demoVersionOut[1]
    static void getLibraryInfo(int *versionDateOut, char *buildInfoOut, bool *demoVersionOut);

    // Retruns the error string for the given error code
    // The error string will be truncated, if the output buffer is too small!
    static bool getErrorDescription(const int errorCode, char *outBuffer, const size_t buffSize);

private:
    SitMarkAudioBCDetectorLibrary(void)
    { }
    //~SitMarkAudioBCDetectorLibrary(void) {}
};

extern "C"
jstring
Java_de_kappa_1mm_sitmark_sitmarkaudiobeacondemo_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */)
{
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

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
