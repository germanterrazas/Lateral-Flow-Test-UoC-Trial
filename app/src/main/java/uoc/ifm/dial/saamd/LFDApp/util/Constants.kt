package uoc.ifm.dial.saamd.LFDApp.util

class Constants {

    companion object{
        internal const val PHOTO_TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss"

        // For referring to the intent content in MainActivity and Dashboard activities
        internal const val INTENT_DATE = "dateValue"
        internal const val INTENT_TIME = "timeValue"
        internal const val INTENT_LFD_IMAGE_NAME = "lfdImageName"
        internal const val INTENT_LFD_IMAGE = "lfdImage"

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

        // For use when UPLOAD button is pressed
        internal const val REQUEST_BODY_IMAGE = "image/png"
        internal const val REQUEST_BODY_JSON = "application/json; charset=utf-8"
        internal const val IMAGE_UPLOAD_WAIT = "Sending device photo please wait..."
        internal const val CHOICE_UPLOAD_WAIT = "Sending response please wait..."
        internal const val IMAGE_UPLOAD_FAILED = "Device photo unable to be sent"
        internal const val CHOICE_UPLOAD_FAILED = "Response unable to be sent"
        internal const val IMAGE_UPLOAD_SUCCESSFUL = "Device photo sent."
        internal const val CHOICE_UPLOAD_SUCCESSFUL = "Response sent."
    }
}