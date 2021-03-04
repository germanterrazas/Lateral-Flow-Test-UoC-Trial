package uoc.ifm.dial.lfd.vanilla.util

class Constants {

    companion object{
        internal const val PHOTO_TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss"

        // For MainActivity and Dashboard activities
        internal const val CAMERA_PERMISSION_CODE = 1
        internal const val REQUEST_IMAGE_CAPTURE = 1
        internal const val JPEG_COMPRESSION_FOR_EXTRA = 50
        // Used to message the user through showToast()
        internal const val CAMERA_PERMISSION_DENIED = "Permission for camera denied"
        // Used in dispatchTakePictureIntent()
        internal const val APP_URI_NAME = "uoc.ifm.dial.lfd.vanilla"

        internal const val INTENT_DATE = "dateValue"
        internal const val INTENT_TIME = "timeValue"
        internal const val INTENT_LFD_IMAGE_NAME = "lfdImageName"
        internal const val INTENT_LFD_IMAGE = "lfdImage"

        // For SharedPreferences activity
        internal const val SHARED_PREF_RANDOM_DEVICE_ID_LENGHT = 20
        internal const val SHARED_PREF_DEVICE_ID = "DEVICE_ID"
        internal const val SHARED_PREF_CONSENT = "CONSENT_GIVEN"
        internal const val SHARED_PREF_CONSENT_VALUE = "YES"

        // For NodeJS REST API POST in Azure
        internal const val REST_API_URL = "https://lfd-test-trial-be.xyz/lfdtests"
        internal const val REST_API_IMAGE_URL = "https://lfd-test-trial-be.xyz/upload"
        internal const val CONNECT_TIMEOUT: Long = 10
        internal const val WRITE_TIMEOUT: Long = 180
        internal const val READ_TIMEOUT: Long = 180
        internal const val SUCCESS_API_POST_CODE: Int = 200
        internal const val API_POST_METHOD = "POST"

        // For use when UPLOAD button is pressed
        internal const val REQUEST_BODY_IMAGE = "image/png"
        internal const val REQUEST_BODY_JSON = "application/json; charset=utf-8"
        internal const val IMAGE_UPLOAD_WAIT = "Sending device photo please wait..."
        internal const val CHOICE_UPLOAD_WAIT = "Sending response please wait..."
        internal const val IMAGE_UPLOAD_FAILED = "Device photo unable to be sent - try again"
        internal const val CHOICE_UPLOAD_FAILED = "Response unable to be sent - try again"
        internal const val IMAGE_UPLOAD_SUCCESSFUL = "Device photo sent."
        internal const val CHOICE_UPLOAD_SUCCESSFUL = "Response sent."
        internal const val IMAGE_FILE_EXTENSION = ".jpeg"
    }
}