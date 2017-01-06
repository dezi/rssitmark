// ************************************************************************************************
// SitMark Audio BC Detector by Fraunhofer SIT
// https://www.sit.fraunhofer.de/en/offers/projekte/digital-watermarking/audio-wasserzeichen/
// 
// Generic Broadcast Detector API
//
// 2014-05-15 Daniel Trick
// ************************************************************************************************

#ifndef INC_SITMARKAUDIOBCDETECTORAPI_H
#define INC_SITMARKAUDIOBCDETECTORAPI_H

// The following ifdef block is the standard way of creating macros which make exporting 
// from a DLL simpler. All files within this DLL are compiled with the MDSAUDIODETECTORAPI_EXPORTS
// symbol defined on the command line. this symbol should not be defined on any project
// that uses this DLL. This way any other project whose source files include this file see 
// MDSAUDIODETECTORAPI_API functions as being imported from a DLL, whereas this DLL sees symbols
// defined with this macro as being exported.
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

#include <cstdlib>
#include <cstdio>

// ================================================================================================
// Interface Version
// ================================================================================================

//Must be increased on any change that breaks binary compatibility !!!
#define SITMARKAUDIOBCDETECTORAPI_CORE 2

//Helper macros (required by the C pre-processor in order to glue two strings)
#define SITMARKAUDIOBCDETECTORAPI_GLUE_HELPER(X,Y) X##Y
#define SITMARKAUDIOBCDETECTORAPI_GLUE(X,Y) SITMARKAUDIOBCDETECTORAPI_GLUE_HELPER(X,Y)

//The following two lines define the actual class names
#define SitMarkAudioBCDetectorAPI     SITMARKAUDIOBCDETECTORAPI_GLUE(SitMarkAudioBCDetectorAPI_r,     SITMARKAUDIOBCDETECTORAPI_CORE)
#define SitMarkAudioBCDetectorLibrary SITMARKAUDIOBCDETECTORAPI_GLUE(SitMarkAudioBCDetectorLibrary_r, SITMARKAUDIOBCDETECTORAPI_CORE)

// ================================================================================================
// Constants
// ================================================================================================

typedef enum
{
	SITMARKAUDIOBCDETECTORAPI_TYPE_WATERMARK   = 0,	//Broadcast Watermark
	SITMARKAUDIOBCDETECTORAPI_TYPE_ROBUST_HASH = 1,	//RMAC
}
SitMarkAudioBCDetectorAPI_t;

// ================================================================================================
// Generic Detector Interface
// ================================================================================================

class SITMARKAUDIOBCDETECTORAPI_API SitMarkAudioBCDetectorAPI
{
public:
	SitMarkAudioBCDetectorAPI(void) {}
	virtual ~SitMarkAudioBCDetectorAPI(void) {}

	//Detector API
	virtual void initialize(void) = 0;
	virtual int feedDetector(const short *in_frame, double &dConfidenceLevelOut) = 0;
    virtual int feedDetector_energyEfficentHF(const short *in_frame, double &dConfidenceLevelOut, int &shift, bool &update_score) = 0;
	virtual double getAccumulatedMessage(char* out_pNetMessage, bool &bCorrect) = 0;
	virtual int getSubMessages(char* out_pSubMessages, int &out_iCount, const size_t uiMaxSize) const = 0;
	virtual int getFrameSize(void) const = 0;
	virtual int getDecodedMessageLen(void) const = 0; /*result does not(!) include the extra byte for the NULL-terminator*/
	virtual void reset(void) = 0;
};

// ================================================================================================
// Broadcast Detector Library
// ================================================================================================

class SITMARKAUDIOBCDETECTORAPI_API SitMarkAudioBCDetectorLibrary
{
public:
	//Check for available detector modes
	static bool queryDetectorType(const int &detectorType);
	
	//Creates new detector instance
    static SitMarkAudioBCDetectorAPI* createInstance(const int &detectorType, const int &iNetMessLen, const int &sample_freq, const int &freq_min, const int &freq_max, const int &use_ecc, const int &in_iWMRedundancy, const int &in_iRingBufferLength = -1, const char *const in_key = NULL, FILE *const out_log = NULL, bool hf_mode = false, double playlength2bit = -1.0);

	//Destroy existing detector instance
	static void destroyInstance(SitMarkAudioBCDetectorAPI *&embedderInstance, FILE *const out_log = NULL);

	// Get library informatiom
	// Minimum required array size for output: versionDateOut[4], buildInfoOut[32], demoVersionOut[1]
	static void getLibraryInfo(int *versionDateOut, char *buildInfoOut, bool *demoVersionOut);

	// Retruns the error string for the given error code
	// The error string will be truncated, if the output buffer is too small!
	static bool getErrorDescription(const int errorCode, char* outBuffer, const size_t buffSize);

private:
	SitMarkAudioBCDetectorLibrary(void)  {}
	~SitMarkAudioBCDetectorLibrary(void) {}
};

// ================================================================================================
// EOF
// ================================================================================================

#endif /*INC_SITMARKAUDIOBCDETECTORAPI_H*/
