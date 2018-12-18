/*
 * Copyright (c) 2018 Serge Masse
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used
 * to endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * 4. This software, as well as products derived from it, must not be used for the purpose of
 * killing, harming, harassing, or capturing animals.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package sm.app.spectro;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import sm.lib.acoustic.Acoustic;
import sm.lib.acoustic.AcousticConfig;
import sm.lib.acoustic.AcousticDeviceCapabilities;
import sm.lib.acoustic.AcousticEvent;
import sm.lib.acoustic.AcousticLogConfig;
import sm.lib.acoustic.AcousticSettings;
import sm.lib.acoustic.AudioPlayer;
import sm.lib.acoustic.gui.SpectrogramView;
import sm.lib.acoustic.gui.TextDisplayWithEmailActivity;
import sm.lib.acoustic.util.AppContext;
import sm.lib.acoustic.util.AppPublisher;
import sm.lib.acoustic.util.DataFromIntent;
import sm.lib.acoustic.util.OnAnyThread;
import sm.lib.acoustic.util.Timestamp;


public final class SpectrogramActivity extends Activity implements Acoustic.Callback {

    private static final String TAG = SpectrogramActivity.class.getSimpleName();

    /**
     * only to be used for issues prior to AcousticLogConfig.DEBUG = INIT being set;
     * normally false in production
     */
    public static final boolean LOG_INIT_ENABLED = false;

    /**
     * only to be used for showing user some init events;
     * normally false in production
     * @deprecated maybe remove or replace with isDevMode()
     */
    public static final boolean SHOW_USER_INIT_EVENTS_ENABLED = false;

    /**
     * true means that the text for our apps is to be shown in a specific window;
     * false means that the text is to be shown with other texts such as in About.
     */
    public static final boolean SEPARATE_OUR_APPS_GUI_IS_ENABLED = false;

    public static final boolean SEPARATE_EMAIL_DEV_GUI_IS_ENABLED = false;

    /**
     * For user-entered url and for incoming url from any app.
     */
    public static final boolean SOUND_TO_PLAY_IS_ENABLED = true;

    /**
     * For saving and restoring the spectrogram bitmap between app restarts.
     * <p/>
     * When true, the app restores the image of the spectrogram when the pp is restarted.</P>
     * <p/>
     * <!-- do we restore all the time or only when the app had been pausedByHUser ???
     * idea: restore all the time but we don't restart listener when had been pausedByHUser
     * -->
     */
    public static final boolean SPECTROGRAM_IMAGE_PERSISTENCE_IS_ENABLED = false;

//    private final float ALPHA_DARK_DISABLED = 0.45f;
//    private final float ALPHA_NOT_SET = 0.55f;
//    private final float ALPHA_NEUTRAL_ENABLED = 0.65f;
//    private final float ALPHA_LIGHT_SET = 0.75f;
//    private final float ALPHA_LIGHT = 0.85f;

    //for dark device screens such as the samsung tablet sgh-i467m
    /**
     * to show a disable button
     */
    private final float ALPHA_DARK_DISABLED = 0.6f;
    /**
     * to show an active button which has not been recently selected
     */
    private final float ALPHA_NEUTRAL_ENABLED = 0.75f;
    /**
     * to show a button recently selected where the function is active
     */
    private final float ALPHA_LIGHT_SET = 0.9f;
    /* *
     * not used in this version
     */
    //private final float ALPHA_LIGHT = 0.95f;

    /*
        0% = #00
        10% = #16
        20% = #32
        30% = #48
        40% = #64
        50% = #80
        60% = #96
        70% = #112
        80% = #128
        90% = #144

        Opacity Values TODO constants in a util class

        100% — FF
        95% — F2
        90% — E6
        85% — D9
        80% — CC
        75% — BF
        70% — B3
        65% — A6
        60% — 99
        55% — 8C
        50% — 80
        45% — 73
        40% — 66
        35% — 59
        30% — 4D
        25% — 40
        20% — 33
        15% — 26
        10% — 1A
        5% — 0D
        0% — 00
     */

    public static final boolean TITLE_TEXTVIEW_ENABLED = false;

    public static final boolean ALWAYS_HIDE_BG_WHEN_TEXT = true;

    boolean deviceShown = false;
    boolean aboutShown = false;
    boolean ourAppsShown = false;
    /**
     * when ALWAYS_HIDE_BG_WHEN_TEXT is set, then hideBgIsSet should always be false
     * and the Hide-Xx button should always show "HIDE UI"
     * because when ALWAYS_HIDE_BG_WHEN_TEXT is set then the bg is controlled by the
     * device and about buttons.
     */
    boolean hideBgIsSet = false;
    boolean spectrogramShown = true;

    public static final String PREF_BITMAP_KEY = "bitmap";


    // ==== file-to-play data:

    /* *
     * @ deprecated replaced by file name and size
     */
//    public static final String PREF_URL_KEY = "url_to_play";

    // intent data not saved in preferences in this version
//    public static final String PREF_URL_FILE_SIZE_KEY = "url_to_play_file_size";
//    public static final String PREF_URL_FILE_NAME_KEY = "url_to_play_file_name";

//    String fileSizeToPlayFromPref = "";
//    String fileNameToPlayFromPref = "";

    /* *
     * decoded, used to display in edit text TODO display the user filename, not the internal doc id
     * @ deprecated not useful in this version
     */
//    volatile String urlToPlayString = "";

    /* *
     * @ deprecated ===TODO=== deprecated TBD
     */
//    String urlToPlayFromPref = "";

    boolean urlIsPlaying = false;
    boolean urlIsShown = true;
    boolean urlIsPaused = false;

    /**
     * from the inbound intent or from restored preferences;
     * to be displayed on UI.
     */
    volatile String fileNameToPlay = "";

    /**
     * from the inbound intent or from restored preferences;
     * to be displayed on UI.
     */
    volatile String fileSizeToPlay = "";

    volatile String fileSizeToPlayForDisplay = "";

    volatile String fileTextDisplayed = "";

    DataFromIntent dataFromIntent = null;
    Intent intentToPlay = null;
    boolean intentToPlayIsValid = false;

    /**
     * from external source, before decoding, possibly url-encoded
     * @deprecated
     */
    String urlToPlayRaw = "";
    /**
     * from external source, after decoding
     * @deprecated
     */
    String urlToPlayDecoded = "";

    /**
     * @deprecated
     */
    Uri urlToPlayUri = null;

    // use the color that comes with the edittext
    int urlColorForInactive = (Color.GRAY & 0x00FFFFFF) | 0x80000000;
    int urlColorForActive = (Color.GREEN & 0x00FFFFFF) | 0x80000000;
    int urlColorForPaused = (Color.YELLOW & 0x00FFFFFF) | 0x80000000;
    int urlColorForError = (Color.RED & 0x00FFFFFF) | 0x80000000;
    int editTextUrlToPlayTextInitialColor = urlColorForInactive;

    /**
     * not really editable in this version
     */
    EditText editTextUrlToPlay;
    /**
     * label is "Play" or "Cancel" ===TODO=== TBD
     */
    Button playUrlButton;
    Button hideUrlButton;
    LinearLayout urlLayout;


    /**
     * to disable an Intent type, the corresponding {@code *_ENABLED} flag is set to false and
     * the {@code <intent-filter>} in the manifest is commented out.
     */
    public static final String INBOUND_INTENT_PLAY_TYPE_SPECIAL = "application/vnd.sm.app.spectrogram";
    /**
     * ex.: http://url - from browser - disabled in this version
     */
    public static final String INBOUND_INTENT_PLAY_TYPE_TEXT = "text/plain";
    public static final String INBOUND_INTENT_PLAY_TYPE_AUDIO_WAV = "audio/wav";
    public static final String INBOUND_INTENT_PLAY_TYPE_AUDIO_PREFIX = "audio/";
    public static final String INBOUND_INTENT_PLAY_TYPE_AUDIO_ANY = "audio/*";

    // use these with SOUND_TO_PLAY_IS_ENABLED

    public static final boolean INBOUND_INTENT_PLAY_TYPE_SPECIAL_ENABLED = true;
    /**
     * ex.: http://url - from browser - disabled in this version
     */
    public static final boolean INBOUND_INTENT_PLAY_TYPE_TEXT_ENABLED = true;
    public static final boolean INBOUND_INTENT_PLAY_TYPE_AUDIO_WAV_ENABLED = true;
    public static final boolean INBOUND_INTENT_PLAY_TYPE_AUDIO_ANY_ENABLED = true;

    public static final boolean INBOUND_INTENT_PLAY_RAW_URL_ENABLED = true;

    //play sound file from intent from other app or from url entered by user;
    //an intent from other app causes this app to start or restart

    //volatile SmartPlayer player = null;

    /**
     * not empty if http link for wav, from clip from intent
     * <!-- maybe not needed -->
     *     @deprecated
     */
    private String clipDataItemText = "";

    // ==== end of file-to-play data


    boolean textColorBrighter = false;
    final int textColorTransparent = 0x8833b5e5;
    //designed to be bright blue on a black background
    final int textColorNotTransparent = 0xFF33b5e5;

    SpectrogramView spectrogramView;
    LinearLayout largeGuiLayout;

    /**
     * used for snackbar
     */
    CoordinatorLayout coordinatorLayout;//TODO washere bug 2018-4-17 does not seem to work: text not shown when exiting app

    LinearLayout buttonsLayout;
    TextView contentTextView;
    Button pause;
    Button hideGui;
    Button device;
    Button about;
    //Button ourApps;
    //Button emailDev;

    /*
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.WAKE_LOCK" /> not dangerous
    <uses-permission android:name="android.permission.INTERNET" /> not dangerous
    */
    private static final int PERMISSIONS_REQUEST_CODE_FOR_RECORD_AUDIO = 20180221;

    private static String[] PERMISSIONS_FOR_RECORD_AUDIO = {
            Manifest.permission.RECORD_AUDIO
        };

    // Storage Permissions
    private static final int PERMISSIONS_REQUEST_CODE_FOR_STORAGE_ACCESS = 20180223;

    /**
     * Used to move the value from method getPermissionForRecordAudio to onRequestPermissionsResult,
     * and to feed playLocalFileAfterStorageAccessGranted.
     */
    private volatile String filePathNeedingAccess = "";

    private static String[] PERMISSIONS_FOR_EXTERNAL_STORAGE_ACCESS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

    /**
     * true when storage access permission was asked to user; used to avoid asking permission more
     * than once.
     */
    private volatile boolean storageAccessPermissionWasAsked = false;

    /**
     * Used to move the value from method getPermissionForRecordAudio to onRequestPermissionsResult, and
     * to feed onCreateComplete, when there could be an intent to play by the caller of the app.
     * Needed in this version.
     */
    private volatile Bundle savedInstanceStateTemp = null;

    private volatile boolean isOnRealDevice = true;

    /**
     * device capability
     */
    private volatile boolean doesSoundInput = true;
    /**
     * device capability
     */
    private volatile boolean doesSoundOutput = true;

    /* *
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
//    private GoogleApiClient client;

    private final boolean APP_INDEXING_IS_ENABLED = false;

    /**
     * Production (prod): new AcousticLogConfig(AcousticLogConfig.OFF,AcousticLogConfig.OFF,AcousticLogConfig.OFF)
     * <br>error off
     * <br>warning off
     * <br>debug off
     */
    private final AcousticLogConfig LOG_CONFIG = new AcousticLogConfig(
            AcousticLogConfig.OFF,
            AcousticLogConfig.OFF,
            AcousticLogConfig.INIT); //LogConfig.DEVICE_SOUND_CAPABILITIES);

    /* TODO new pref
    - for mic, offer to user all choices supported by the device and give the app one of the options
    future:
    - for channel, mono or stereo, same as above
    - for float pcm encoding, same as above, float/int for input, float/int for output, depending on version of android
    - for sampling rate, offer to user the native rate and some other rates supported by device, and let user pick one
      and let user decide for same encoding for input and output

      TODO prio 2 keep list of urls to play, give them names, delete, move up/down, export list (share)
     */

    AcousticConfig soundPrefs = AcousticConfig.DEFAULT;

    private AcousticConfig getAcousticConfigFromClient(){
        if(soundPrefs!=null && ! AcousticConfig.DEFAULT.equals(soundPrefs)) return soundPrefs;
        soundPrefs = new AcousticConfig();
        soundPrefs.logConfig = LOG_CONFIG;
        soundPrefs.isMicPreferred = true;
        soundPrefs.isChannelMonoRequested = true;
        soundPrefs.isEncodingPcmFloatPreferred = false;//TODO future true with new code for float processing
        soundPrefs.isNativeSampleRateRequested = true;
        soundPrefs.isSameEncodingPcmForInputAndOutputRequested = false;
//        soundPrefs.isDevMode = false;
//        soundPrefs.isDbCapable = false;
//        soundPrefs.emissionIsEnabled = false;
        soundPrefs.mipmap_ic_launcher = R.mipmap.ic_launcher;

        //settings for textviews
        soundPrefs.textSizeSp = 14;
        soundPrefs.textStyleString = "bold";
        soundPrefs.textColorHexString = "white";
        soundPrefs.bgColorHexString = "#0099cc";

        //TODO washere textview container bg color

        if(LOG_CONFIG.DEBUG==AcousticLogConfig.ON){
            Log.d(TAG,".getAcousticConfigFromClient: isMicPreferred {"+soundPrefs.isMicPreferred
                    +"} isChannelMonoRequested {"+soundPrefs.isChannelMonoRequested
                    +"} isEncodingPcmFloatPreferred {"+soundPrefs.isEncodingPcmFloatPreferred
                    +"} isNativeSampleRateRequested {"+soundPrefs.isNativeSampleRateRequested
                    +"} isSameEncodingPcmForInputAndOutputRequested {"
                    +soundPrefs.isSameEncodingPcmForInputAndOutputRequested
                    +"}");
        }
        if(SHOW_USER_INIT_EVENTS_ENABLED){
            showStatusSnackbar("AcousticConfig were set");
        }
        return soundPrefs;
    }

    private void onError(String s) {
        if(Acoustic.getIt().isAnyLogEnabled() ){
            Log.d(TAG,"onError: "+s);
        }
    }


//    /**
//     * designed to be reset in children onCreate.
//     * Default = LogClient.APP_TYPE_FREE.
//     */
//    protected volatile int appType = LogClient.APP_TYPE_FREE;


//    /**
//     * default implementation, may be overwritten by client app
//     *
//     * @return int such as LogClient.APP_TYPE_FREE
//     */
//
//    public int getAppType() {
//        return appType;
//    }

//    /**
//     * Designed to be called by the onCreate method of the main Activity.
//     * Default: LogClient.APP_TYPE_FREE
//     * @param appType
//     */
//
//    public void setAppType(int appType) {
//        this.appType = appType;
//    }

//
//    public String getAppTypeString(final Object object, final int appTypeInt) {
//        final Context contextGiven = (Context)object;
//        switch (appTypeInt) {
//            case APP_TYPE_EDU:
//                return contextGiven.getString(R.string.app_type_edu);
//            case APP_TYPE_FREE:
//                return contextGiven.getString(R.string.app_type_free);
//            case APP_TYPE_PRICED:
//                return contextGiven.getString(R.string.app_type_priced);
//            case APP_TYPE_FREE_WITH_ADS:
//                return contextGiven.getString(R.string.app_type_free_with_ads);
//        }
//        return contextGiven.getString(R.string.app_type_unknown);
//    }
//
//
//    public String getAppTypeString(){
//        return getAppTypeString(AppContext.getIt().APP_CONTEXT,appType);
//    }


    public boolean isDbCapable() {
        return false;
    }

    public String getDbProviderAuthority() {
        return "N/A";
    }

    public String getDbProviderAuthorityFragment() {
        return "N/A";
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Acoustic acoustic = null;
        try {
            acoustic = Acoustic.firstCall(this, getAcousticConfigFromClient(), getApplicationContext());
        }catch(Throwable ex){
            if(LOG_INIT_ENABLED) Log.e(TAG,"onCreate: "+ex);
        }

        if( ! acoustic.isConfigOk() ){
            // error message: acoustic.acousticConfig.statusText
            if(LOG_INIT_ENABLED) {
                Log.w(TAG, ".onCreate: Acoustic lib reporting anomaly in config: "+
                        acoustic.getAcousticConfig().statusText);
            }

            //TODO show user downstream
        }

        super.onCreate(savedInstanceState);

        if (!isTaskRoot()) {
            // Android launched another instance of the root activity into an existing task
            //  so just quietly finish and go away, dropping the user back into the activity
            //  at the top of the stack (ie: the last state of this task)
            /* related to android b u g(s)
            http://code.google.com/p/android/issues/detail?id=2373
            http://code.google.com/p/android/issues/detail?id=26658
             */
            if(LOG_INIT_ENABLED)
                //Log.e(TAG, ".onCreate: finishing quietly because not TaskRoot");
                Log.w(TAG, ".onCreate: not TaskRoot and continuing...");
        }

//        tempHackForCpuAtFullSpeed();

        restorePreferences();

        if (Acoustic.getIt().isAnyLogEnabled()){
            Log.d(TAG, ".onCreate: entering..." +
                    "app name {" + AppContext.getAppName()
                    + "} version name {" + AppContext.getVersionName()
                    + "} AppPublisher.emailAddressForSupport {" + AppPublisher.emailAddressForSupport
                    +"}"//TODO prio 2 2017-7-1 and use the email support enabled flag in LogClient.Callback
                    + "\n" + getTextForDisplayFromClient()
                    + "\n" + Thread.currentThread()
            );
        }

        savedInstanceStateTemp = savedInstanceState;
        if(getPermissionForRecordAudio(permissions,PERMISSIONS_REQUEST_CODE_FOR_RECORD_AUDIO)){
            onCreateComplete(savedInstanceState);
        }
    }

    private static final String[] permissions = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_NETWORK_STATE};

    /**
     *
     * @param permissionsGiven
     * @param requestCode
     * @return true when apriori granted and caller may call onCreateComplete;
     * false when thread asking permission to system and possibly user and caller
     * must not call onCreateComplete as this will be done on another thread
     * if permissions get granted.
     */
    private boolean getPermissionForRecordAudio(final String[] permissionsGiven, final int requestCode) {
        //savedInstanceStateTemp = savedInstanceStateGiven;
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.INIT || LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_INPUT_INIT
                || LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS
                || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
            Log.d(TAG, ".getPermissionForRecordAudio entering...Build.VERSION.SDK_INT = "
                    + Build.VERSION.SDK_INT);

//        getPermissionForExternalStorageAccess();//TO DO was here integrate with the rest of this method
//        if(permissionGrantedForRecordAudio)return true;
        /*
        PackageManager.PERMISSION_GRANTED if you have the permission, or PackageManager.PERMISSION_DENIED if not.
         */
        boolean aprioriGranted = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aprioriGranted = checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            aprioriGranted = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        }
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.INIT || LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_INPUT_INIT
                || LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS
                || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
            Log.d(TAG, ".getPermissionForRecordAudio: aprioriGranted " + aprioriGranted);

        if (!aprioriGranted) {
            // we don't have permission yet, ask user;
            // No explanation needed to give user, we can request the permission.

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.INIT || LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_INPUT_INIT
                        || LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS
                        || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL )
                    Log.d(TAG, ".getPermissionForRecordAudio about to call requestPermissions(...)");
                // -----------------------------------------------------------------------
                requestPermissions(permissionsGiven, requestCode);
                // -----------------------------------------------------------------------
            } else {
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.INIT || LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_INPUT_INIT
                        || LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS
                        || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
                    Log.d(TAG, ".getPermissionForRecordAudio about to call ActivityCompat.requestPermissions(...)");

                ActivityCompat.requestPermissions(this, permissionsGiven, requestCode);
            }
            // PERMISSIONS_REQUEST_CODE_FOR_RECORD_AUDIO is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        } else {
            //granted apriori
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.INIT || LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_INPUT_INIT
                    || LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS
                    || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
                Log.d(TAG, ".getPermissionForRecordAudio: was granted apriori" +
                        "; calling onCreateComplete...");

//            permissionGrantedForRecordAudio = true;
//            onCreateComplete(savedInstanceStateGiven);
            return true;
        }
        //here when permission granted before running the app, e.g., older version of android

        if (LOG_CONFIG.DEBUG==AcousticLogConfig.INIT || LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_INPUT_INIT
                || LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS
                || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
            Log.d(TAG, ".getPermissionForRecordAudio exiting...");

        return false;
    }
    /**
     * Called by Android on the ui thread after the permissions statuses have been determined.
     *
     * @param requestCode  int
     * @param permissions  array of String instances
     * @param grantResults array of int primitives
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.INIT || LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_INPUT_INIT
                || LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS)
            Log.d(TAG, ".onRequestPermissionsResult entering with requestCode {" + requestCode
                    + "}; filePathNeedingAccess {" + filePathNeedingAccess + "}");
//        readFileAccepted = false;
//        writeFileAccepted = false;
        //permissionGrantedForRecordAudio = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE_FOR_RECORD_AUDIO: {
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.INIT
                        || LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_INPUT_INIT
                        || LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS)
                    Log.d(TAG, ".onRequestPermissionsResult: " +
                            "PERMISSIONS_REQUEST_CODE_FOR_RECORD_AUDIO; " +
                            "grantResults.length = " + grantResults.length);
                // If request is cancelled, the result arrays are empty.
                boolean permissionGrantedForRecordAudio = false;
                if (grantResults.length > 0) {
                    permissionGrantedForRecordAudio = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                } else {
                    //not granted
                }

                if (!permissionGrantedForRecordAudio) {
                    // not granted, cannot run the spectrogram, only the Device text is useful
                    if (LOG_CONFIG.DEBUG!=AcousticLogConfig.OFF)
//                    if (LOG_CONFIG.DEBUG==AcousticLogConfig.INIT || LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_INPUT_INIT
//                            || LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS)
                        Log.d(TAG, ".onRequestPermissionsResult: audio permission denied; calling shutdown withfinish()");

                    //TODO prio 2 2016-12 call an activity to display the status

                    shutdown(true);
                    return;
                }
                //here when record audio was granted
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.INIT || LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_INPUT_INIT
                        || LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS)
                    Log.d(TAG, ".onRequestPermissionsResult: audio permission granted");
                //complete create
                onCreateComplete(savedInstanceStateTemp);
                break;
            } // case

            case PERMISSIONS_REQUEST_CODE_FOR_STORAGE_ACCESS: {
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.INIT
                        || LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_INPUT_INIT
                        || LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS)
                    Log.d(TAG, ".onRequestPermissionsResult: " +
                            "PERMISSIONS_REQUEST_CODE_FOR_STORAGE_ACCESS; " +
                            "grantResults.length = " + grantResults.length);
                // If request is cancelled, the result arrays are empty.
                boolean readFileAccepted = false;
                boolean writeFileAccepted = false;
                if (grantResults.length > 0) {
                    readFileAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (grantResults.length > 1)
                        writeFileAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                }
                // when false, disable the play url functions for local files
//                permissionGrantedForStorageAccess = ;

                if (LOG_CONFIG.DEBUG==AcousticLogConfig.INIT
                        || LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_INPUT_INIT
                        || LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS)
                    Log.d(TAG, ".onRequestPermissionsResult: " +
                            "PERMISSIONS_REQUEST_CODE_FOR_STORAGE_ACCESS; " +
                            "readFileAccepted = " + readFileAccepted
                            + "; writeFileAccepted = " + writeFileAccepted);

                if (readFileAccepted || writeFileAccepted) {
                    // playLocalFileAfterStorageAccessGranted(filePathNeedingAccess);
                    // TODO 2017-7-26 does this break intent from external app?
                } else {
                    // disable the play url functions but only for local files
                }

                break;
            }

            // other 'case' lines to check for other
            // permissions this app might request
            default: {
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.INIT || LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_INPUT_INIT
                        || LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS)
                    Log.d(TAG, ".onRequestPermissionsResult: default; requestCode " + requestCode
                            //+"; permissionGrantedForRecordAudio "+ permissionGrantedForRecordAudio
                            //+"; calling onCreateComplete(savedInstanceStateTemp)..."
                    );
//                onCreateComplete(savedInstanceStateTemp);
            }
        }//switch

    }

    /**
     * Request permission to access device storage (aka. external storage, i.e., external to the app).
     * <p/>
     * If the app does not have permission yet, then the user will be prompted to grant permission.
     *
     * @return return true when permission granted and the caller can try to play,
     * false when previously denied (and not requested again)
     * or when permission being requested, i.e., the caller should not try to play.
     */
    @SuppressLint("WrongConstant")
    public boolean getPermissionForExternalStorageAccess(final String filePath) {
        requestFilesPermission(filePath);

        // Check if we have write permission
        int permission = 0;
        if (Build.VERSION.SDK_INT >= 23) {
            permission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else {
            permission = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (permission == PackageManager.PERMISSION_GRANTED) {
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS
                    || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL) {
                Log.d(TAG, ".getPermissionForExternalStorageAccess: " +
                        "checkSelfPermission returned positive; this method is returning true");
            }
            return true;
        }

        //no permission; did we asked the user before? if yes, then don't ask again
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS||LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL) {
            Log.d(TAG, ".getPermissionForExternalStorageAccess: " +
                    "checkSelfPermission returned negative; storageAccessPermissionWasAsked = "
                    + storageAccessPermissionWasAsked);
        }
        if (storageAccessPermissionWasAsked) {
            // previously denied by user
//            if (largeGuiLayout != null) {
//                Snackbar.make(findViewById(android.R.id.content), //largeGuiLayout,
//                        "The file cannot be played"
//                                + ": it is in external storage and access was denied by you",
//                        Snackbar.LENGTH_LONG).show();
                Toast.makeText(this,"The file cannot be played"
                        + ": it is in external storage and access was denied by you",
                        Toast.LENGTH_LONG).show();
//            }
            return false;
        }

        storageAccessPermissionWasAsked = true;

        // We don't have permission so prompt the user;
        // this function will be continued in onRequestPermissionsResult

        // to be used after permission is granted, if it is
        filePathNeedingAccess = filePath;

        ActivityCompat.requestPermissions(
                this,
                PERMISSIONS_FOR_EXTERNAL_STORAGE_ACCESS,
                PERMISSIONS_REQUEST_CODE_FOR_STORAGE_ACCESS
        );
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS||LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL) {
            Log.d(TAG, ".getPermissionForExternalStorageAccess: " +
                    "checkSelfPermission returned negative; storageAccessPermissionWasAsked = "
                    + storageAccessPermissionWasAsked
                    + "; user was asked; this method returning false");
        }
        return false;
    }

    private void requestFilesPermission(final String filePath) {
        filePathNeedingAccess = filePath;

        ActivityCompat.requestPermissions(
                this,
                PERMISSIONS_FOR_EXTERNAL_STORAGE_ACCESS,
                PERMISSIONS_REQUEST_CODE_FOR_STORAGE_ACCESS
        );
    }

    /**
     * Called on ui thread after initial permissions are processed by Android.
     *
     * @param savedInstanceStateGiven Bundle from onCreate; used for intent to play
     */
    protected void onCreateComplete(final Bundle savedInstanceStateGiven) {
        try {
            if (Acoustic.getIt().isLogDebugEnabled())
                Log.d(TAG, ".onCreateComplete: entering...");

            // 1. sound capabilities

            isOnRealDevice = isOnRealDeviceOrEmulator();

            doesSoundInput = setDeviceSoundCapabilities();

            doesSoundOutput = AcousticDeviceCapabilities.getIt().doesSoundOutput; //DeviceSoundCapabilities.isDeviceCapableOfSoundOutput();

            if (Acoustic.getIt().isLogDebugEnabled()) {
                String soundInputSupported = doesSoundInput?"supported":"_not_ supported";
                String soundOutputSupported = doesSoundOutput?"supported":"_not_ supported";;
                Log.d(TAG, ".onCreateComplete: isOnRealDevice = " +isOnRealDevice+
                        "; Sound input " + soundInputSupported
                        + ", output " + soundOutputSupported);
            }

            // 2. ui

            onCreateUI();

            // 3. status to user

            disableThePauseButton("Starting");

            if ( ! doesSoundInput) {
                // ========== device does not support sound input ==========
                disableThePauseButton("No audio");
                //TODO prio 2 do a better UI for this major issue
                Toast.makeText(this,
                        "This device does not support sound input; this app will not work properly.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            // here when can do sound input

            // ===== main process =====

            //=====================================
            boolean success = startAcoustic();
            //=====================================

            if ( ! success ) {
                // the listener failed to start
                if (Acoustic.getIt().isLogDebugEnabled()) {
                    Log.d(TAG,".onCreateComplete: exiting; the listener failed to start");
                }
                //TODO better info to user for this critical issue
                if(SHOW_USER_INIT_EVENTS_ENABLED){
                    showStatusSnackbar("Listener failed to start");
                }
                return;
            }

            // here when listener started ok; try playing a url if any from trigger app

            enableThePauseButton("Pause");

            //------------------------------------------
            initUrlToPlayLocal(savedInstanceStateGiven);
            //------------------------------------------

        } catch (Exception ex) {
            if(Acoustic.getIt().isAnyLogEnabled())Log.e(TAG,".onCreateComplete: "+ex);
            disableThePauseButton("Error");
            onExceptionAtInit(ex);//TODO prio 2 2016-11 does this work???
        } finally {
            if (Acoustic.getIt().isLogDebugEnabled())
                Log.d(TAG, ".onCreateComplete: exiting...");
        }
    }

    /**
     * called by onCreateComplete
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB) //api level 11
    protected void onCreateUI() {

        setContentView(R.layout.activity_spectrogram);

        coordinatorLayout = findViewById(R.id.coordinatorLayout);

        spectrogramView = (SpectrogramView) findViewById(R.id.spectrogram2);

        spectrogramView.setColorForSecMarker(SpectrogramView.COLOR_GREEN_TRANSPARENT);
        spectrogramView.setColorForFreqMarker(SpectrogramView.COLOR_GREEN_TRANSPARENT);

        //spectrogram2_textview_title is defined in the layout xml file
        if (!TITLE_TEXTVIEW_ENABLED) {
            TextView titleTextView = (TextView) findViewById(R.id.spectrogram2_textview_title);
            titleTextView.setVisibility(View.INVISIBLE);
        }

        largeGuiLayout = (LinearLayout) findViewById(R.id.spectrogram2_gui);
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
            Log.d(TAG, "onCreateUI: largeGuiLayout is spectrogram2_gui");
        largeGuiLayout.setOnClickListener(ON_CLICK_LISTENER);
        largeGuiLayout.setClickable(true);

        buttonsLayout = (LinearLayout) findViewById(R.id.spectrogram2_buttons);

        hideGui = findViewById(R.id.spectrogram2_hide);
        //hideGui.setBackgroundColor(Color.LTGRAY);
        hideGui.setOnClickListener(ON_CLICK_LISTENER);
        hideGui.setClickable(true);
        hideGui.setAlpha(ALPHA_NEUTRAL_ENABLED);
        hideBgIsSet = false;

        pause = findViewById(R.id.spectrogram2_pause);
        enableThePauseButton(null);
//        pause.setBackgroundColor(Color.LTGRAY);
//        pause.setOnClickListener(ON_CLICK_LISTENER);
//        pause.setClickable(true);
//        pause.setAlpha(ALPHA_NOT_SET);
//        pause.setText("Pause");

        device = findViewById(R.id.spectrogram2_device);
        //device.setBackgroundColor(Color.LTGRAY);
        device.setOnClickListener(ON_CLICK_LISTENER);
        device.setClickable(true);
        device.setAlpha(ALPHA_NEUTRAL_ENABLED);
        deviceShown = false;

        about = findViewById(R.id.spectrogram2_about);
        //about.setBackgroundColor(Color.LTGRAY);
        about.setOnClickListener(ON_CLICK_LISTENER);
        about.setClickable(true);
        about.setAlpha(ALPHA_NEUTRAL_ENABLED);
        aboutShown = false;

        contentTextView = findViewById(R.id.content_spectrogram2);
        contentTextView.setMovementMethod(new ScrollingMovementMethod());
        contentTextView.setGravity(Gravity.LEFT); // View.LAYOUT_DIRECTION_LOCALE);
        contentTextView.setVerticalScrollBarEnabled(true);
        contentTextView.setText("");
        contentTextView.setTextIsSelectable(true);
        contentTextView.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            contentTextView.setElegantTextHeight(true);
        }

        if (SOUND_TO_PLAY_IS_ENABLED && doesSoundOutput) {
            //to suppress the soft-keyboard until the user actually touches the editText
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
//            largeGuiLayout.setOnLongClickListener(ON_LONG_CLICK_LISTENER);
//            contentTextView.setOnLongClickListener(ON_LONG_CLICK_LISTENER);
            //shows layout *url_to_play_layout*
            //called *inflated_for_url_to_play* after this but the stub is not in hierarchy
            findViewById(R.id.stub_for_url_to_play).setVisibility(View.VISIBLE);
            editTextUrlToPlay = findViewById(R.id.url_to_play);
            editTextUrlToPlay.setFocusable(false);
            editTextUrlToPlay.setEnabled(false);
            editTextUrlToPlay.setClickable(false);
            editTextUrlToPlay.setLongClickable(false);
            editTextUrlToPlayTextInitialColor = (editTextUrlToPlay.getCurrentTextColor() & 0x00FFFFFF) | 0x80000000;
            urlColorForInactive = editTextUrlToPlayTextInitialColor;
            //editTextViewUrlToPlay.setFocusableInTouchMode(true);
//            editTextViewUrlToPlay.clearFocus();
//            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//            imm.hideSoftInputFromWindow(editTextViewUrlToPlay.getWindowToken(), 0); //InputMethodManager.HIDE_NOT_ALWAYS);
            //imm.showSoftInput(editTextViewUrlToPlay, InputMethodManager.SHOW_FORCED);
            //imm.showSoftInput(editTextViewUrlToPlay, InputMethodManager.SHOW_IMPLICIT);
            playUrlButton = findViewById(R.id.button_play);
            //playUrlButton.setBackgroundColor(Color.LTGRAY);
            hideUrlButton = findViewById(R.id.button_hide_url);
            //hideUrlButton.setBackgroundColor(Color.LTGRAY);
            //ViewStub:
            urlLayout = (LinearLayout) findViewById(R.id.inflated_for_url_to_play); //url_to_play_layout);
            if (urlLayout == null) {
                if(LOG_CONFIG.ERROR>=LOG_CONFIG.ON)
                    Log.e(TAG, ".onCreateUI: urlLayout is null");
            }
            playUrlButton.setOnClickListener(ON_CLICK_LISTENER);
            hideUrlButton.setOnClickListener(ON_CLICK_LISTENER);
            urlIsPlaying = false;
            urlIsPaused = false;
            playUrlButton.setAlpha(ALPHA_NEUTRAL_ENABLED);
            hideUrlButton.setAlpha(ALPHA_NEUTRAL_ENABLED);
        }
//        pause.setClickable(true);
//        pause.setFocusable(true);
//        pause.requestFocus();
        //Log.e(TAG,".onCreateUI: contentTextView is set");
    }

    private volatile String pauseButtonLabel = null;

    private Runnable RUNNABLE_TO_ENABLE_THE_PAUSE_BUTTON = new Runnable() {

        public void run() {
            if(pause==null)return;
            pause.setOnClickListener(ON_CLICK_LISTENER);
            pause.setAlpha(ALPHA_NEUTRAL_ENABLED);
            if (pauseButtonLabel != null) {
                pause.setText(pauseButtonLabel);
            }
        }
    };

    private Runnable RUNNABLE_TO_DISABLE_THE_PAUSE_BUTTON = new Runnable() {

        public void run() {
            if(pause==null)return;
            pause.setOnClickListener(null);
            pause.setClickable(false);
            pause.setAlpha(ALPHA_DARK_DISABLED);
            if (pauseButtonLabel != null) {
                pause.setText(pauseButtonLabel);
            }
        }
    };

    /**
     * It ensures that it is run on the ui thread.
     *
     * @param label not used if null
     */
    private void enableThePauseButton(final String label) {
        pauseButtonLabel = label;
        runOnUiThread(RUNNABLE_TO_ENABLE_THE_PAUSE_BUTTON);
    }

    /**
     * It ensures that it is run on the ui thread.
     *
     * @param label not change if param is null
     */
    private void disableThePauseButton(final String label) {
        pauseButtonLabel = label;
        runOnUiThread(RUNNABLE_TO_DISABLE_THE_PAUSE_BUTTON);
    }

    /**
     * for DEV mode only
     *
     * @return String, empty when not dev.
     */
    private String getTextForDisplayFromClient() {
        if (!isDevMode()) return getClass().getSimpleName();
        boolean isDbCapable = isDbCapable();
        return "isDevMode() " + isDevMode()
//                + ", isAdsCapable() " + isAdsCapable()
//                + ", isEduVersion() " + isEduVersion()
//                + ", isDonationsCapable() " + isDonationsCapable()
//                + ", isUseTestPurchase() " + isUseTestPurchase()
//                + ", isUseEmptyIabPayload() " + isUseEmptyIabPayload()
//                + ", isVerifyIabPayload() " + isVerifyIabPayload()
                + ", isSimulatingNoConnection() " + isSimulatingNoConnection()
//                + ", isSimulatingNoPurchase() " + isSimulatingNoPurchase()
                + ", isDbCapable " + isDbCapable
                + (isDbCapable ? ", getDbProviderAuthority() "+getDbProviderAuthority():"")
//                + ", isShowAdsWhenDonated() " + isShowAdsWhenDonated()
                + ", getDbProviderAuthorityFragment() " + getDbProviderAuthorityFragment()
                ;
    }

    /**
     * Init the sound to play from an intent from another app;
     * or a URL from previous session in urlString.
     *
     * <p/>Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     *
     * @param savedInstanceState Bundle or null when starting a new session with a url from incoming intent
     */
    private void initUrlToPlayLocal(final Bundle savedInstanceState) {

        if (!SOUND_TO_PLAY_IS_ENABLED) {
            if(LOG_CONFIG.DEBUG==AcousticLogConfig.INIT){
                Log.d(TAG,".initUrlToPlayLocal: exiting because SOUND_TO_PLAY_IS_ENABLED is false");
            }
            return;
        }

        if (!doesSoundOutput) {
            if(LOG_CONFIG.DEBUG==AcousticLogConfig.INIT){
                Log.d(TAG,".initUrlToPlayLocal: exiting because doesSoundOutput is false");
            }
            return;
        }

        if (savedInstanceState == null) {
            // starting new session (with an Intent), use url from intent (if any)
            // this method does nothing if the start intent has no valid url to play
            if(LOG_CONFIG.DEBUG==AcousticLogConfig.INIT){
                Log.d(TAG,".initUrlToPlayLocal: starting new session with an Intent");
            }
            // ======================================
            if (!handleIncomingIntent()) {
            // ======================================
                // no valid intent, then try url from previous session

                // in future maybe

            } else {
                // valid intent; was handled by handleIncomingIntent; nothing more to do here
                // TODO log
            }

        } else {
            // restoring session (not with intent), use url from previous session, if any
            if(LOG_CONFIG.DEBUG==AcousticLogConfig.INIT){
                Log.d(TAG,".initUrlToPlayLocal: restoring session without an Intent");
            }
            setUrlToPlayInUi();
        }
    }


//    final View.OnLongClickListener ON_LONG_CLICK_LISTENER = new View.OnLongClickListener() {
//
//        / **
//         * Called when a view has been clicked and held.
//         *
//         * <p>Long click shows all guis; hides nothing.</p>
//         *
//         * @param . The view that was clicked and held.
//         * @return true if the callback consumed the long click, false otherwise.
//         * /
//        @ Override
//        public boolean onLongClick(View v) {
//
//            return doShowUrlGui();
//        }
//    };

    private void disableTheHideGuiButton(){
        if(hideGui==null)return;
        hideGui.setAlpha(ALPHA_DARK_DISABLED);
    }

    private void enableTheHideGuiButton(){
        if(hideGui==null)return;
        hideGui.setAlpha(ALPHA_NEUTRAL_ENABLED);
    }

    /**
     * Designed to be used when the Hide-Xx button is "Hide Bg", and not "Hide Gui".
     */
    private void doHideOrShowBg() {
        // Hide Bg = toggle bg
        if (!spectrogramShown) {
            //spectro was not shown, then show it by disabling the color
            showBgAndHideText();
//            if (largeGuiLayout != null) largeGuiLayout.setBackgroundColor(Color.TRANSPARENT);
//            spectrogramShown = true;
//            textColorBrighter = false;
//            contentTextView.setTextColor(textColorTransparent);
        } else {
            //spectro was shown, then hide it by using all black guiLayout if we have text
            if(ALWAYS_HIDE_BG_WHEN_TEXT){
                //then we should have no text showing, so don't hide bg

            }else {
                hideBg();
            }
//            if (largeGuiLayout != null) largeGuiLayout.setBackgroundColor(Color.BLACK);
//            spectrogramShown = false;
//            textColorBrighter = true;
//            contentTextView.setTextColor(textColorNotTransparent);
        }
    }

    private void showBgAndHideText(){
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
            Log.d(TAG, "showBgAndHideText: " +
                    "aboutShown = " + aboutShown
                    +": deviceShown = "+deviceShown
                    + "; contentTextView = " + contentTextView
                    + "; largeGuiLayout = " + largeGuiLayout);
        //if (largeGuiLayout != null) largeGuiLayout.setBackgroundColor(Color.TRANSPARENT);
        spectrogramShown = true;
        textColorBrighter = false;
        aboutShown = false;
        deviceShown = false;
        //contentTextView.setTextColor(textColorTransparent);
        if (contentTextView != null) {
            contentTextView.setText("");
            contentTextView.setVisibility(View.GONE);
            contentTextView.setTextColor(textColorTransparent);
        }
        if (largeGuiLayout != null) {
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                Log.d(TAG, "deviceButtonSelected: " +
                        "largeGuiLayout sensitivity is enabled");
            largeGuiLayout.setOnClickListener(ON_CLICK_LISTENER);
            largeGuiLayout.setClickable(true);
            largeGuiLayout.setBackgroundColor(Color.TRANSPARENT);
        }
        enableThePauseButton(pauseButtonLabel);
        enableTheHideGuiButton();
    }

    private void hideBg(){
        if (largeGuiLayout != null) largeGuiLayout.setBackgroundColor(Color.BLACK);
        spectrogramShown = false;
        textColorBrighter = true;
        contentTextView.setTextColor(textColorNotTransparent);
    }

    private void doHideGui() {
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
            Log.d(TAG, "doHideGui: entering");
        if (buttonsLayout != null) {
            buttonsLayout.setVisibility(View.GONE);
        }
        if(!ALWAYS_HIDE_BG_WHEN_TEXT){
            if (contentTextView != null) {
                contentTextView.setVisibility(View.GONE);// TODO ok?
            }
        }
        doHideUrl(false);
        if (largeGuiLayout != null) {
            //enable screen sensitivity
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                Log.d(TAG, "doHideGui: largeGuiLayout sensitivity is enabled");
            largeGuiLayout.setOnClickListener(ON_CLICK_LISTENER);
            largeGuiLayout.setClickable(true);
//            Snackbar.make(findViewById(android.R.id.content), //coordinatorLayout,
//                    "To show GUI, tap near middle of screen.",
//                    Snackbar.LENGTH_LONG)
//                    .setAction("null", null).show();
            Toast.makeText(this,"To show the user interface, tap near middle of screen.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void doShowGui() {
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
            Log.d(TAG, "doShowGui: entering");
        if (buttonsLayout != null) buttonsLayout.setVisibility(View.VISIBLE);
        if (contentTextView != null) {
            if(contentTextView.getText().length()>0) {
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                    Log.d(TAG, "doShowGui: contentTextView made visible because it has some text");
                contentTextView.setVisibility(View.VISIBLE);
            }else{
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                    Log.d(TAG, "doShowGui: contentTextView made gone because it has no text");
                contentTextView.setVisibility(View.GONE);
            }
            if (textColorBrighter) {
                contentTextView.setTextColor(textColorNotTransparent);
            } else {
                contentTextView.setTextColor(textColorTransparent);
            }
        }

        doShowUrlGui();
    }

    private boolean doShowUrlGui() {
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
            Log.d(TAG, "doShowUrlGui: entering");

        if ( ! SOUND_TO_PLAY_IS_ENABLED) {
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                Log.d(TAG, "doShowUrlGui: exiting; SOUND_TO_PLAY_IS_ENABLED is false");
            return false;
        }

        if ( ! doesSoundOutput) {
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                Log.d(TAG, "doShowUrlGui: exiting; doesSoundOutput is false");
            return false;
        }

        if (urlLayout == null) {
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                Log.d(TAG, "doShowUrlGui: exiting; urlLayout is null");
            return false;
        }

        if (urlLayout.getVisibility() == View.VISIBLE) {
            //do nothing
//                urlLayout.setVisibility(View.GONE);
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                Log.d(TAG, "doShowUrlGui: urlLayout is already visible");
        } else {
            //show url gui
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                Log.d(TAG, "doShowUrlGui: urlLayout is being set visible");
            urlLayout.setVisibility(View.VISIBLE);
        }

        return true;
    }

    /**
     * <ul>
     * <li>pause: second choice is restart</li>
     * <li>hide: touch big screen to show buttons</li>
     * <li>device: second choice is hide text</li>
     * <li>about: second choice is hide text</li>
     * <!-- deprecated
     * <li>ourApps: optional.</li>
     * <li>emailDev: optional.</li>
     * -->
     * <!-- TODO add settings such as text color choice; text size choice -->
     * </ul>
     */
    final View.OnClickListener ON_CLICK_LISTENER = new View.OnClickListener() {
        /**
         * Performs the action after a gui has been selected (a button or the screen, for example).
         *
         * @param v The user-selected view (button or other gui)
         */

        public void onClick(View v) {
            try {
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                    Log.d(TAG, "ON_CLICK_LISTENER_FOR_SPECTROGRAM.onClick(v): entering");

                if (buttonsLayout.isShown()) {
                    //buttons are activated
                    if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                        Log.d(TAG, "ON_CLICK_LISTENER_FOR_SPECTROGRAM.onClick(v): " +
                                "buttonsLayout.isShown() returned true");
                    //do action
                    if (v instanceof Button) {
                        //for a button
                        if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                            Log.d(TAG, "ON_CLICK_LISTENER_FOR_SPECTROGRAM.onClick(v): " +
                                    ". is instance of button");

                        if (v.equals(hideGui)) {
                            //hide button selected, then toggle the bg
                            hideGuiButtonSelected();
                        } else {
                            // a button and not the hide gui button,
                            // guiLayout set downstream dependent on the selected button
                            if (v.equals(pause)) {
                                //pause spectro selected, so pause spectrogram, or restart if was paused
                                pauseToggle();
                                //the display will be taken care of downstream in postButtonSelected(v)
                            } else {
                                //not hide, not pause spectro, check others
                                if (v.equals(device)) {
                                    //the device button was selected
                                    deviceButtonSelected();
                                } else {
                                    //not pause button, not hide, not device, then check about
                                    if (v.equals(about)) {
                                        //about selected
                                        aboutButtonSelected();
                                    } else {
                                        //not about button or any other button upstream
                                        // check URL to play
                                        if (v.equals(playUrlButton)) {
                                            //play or pause url
                                            playUrlButtonSelected();
                                        } else {
                                            //not play/pause url, maybe hide url
                                            if (v.equals(hideUrlButton)) {
                                                // hide url gui or cancel url;
                                                // the space is made available for text
                                                hideOrCancelUrlButtonSelected();
                                            } else {
                                                // not hide url
                                                // do nothing here, go to afterButtonSelected downstream
                                                if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI || LOG_CONFIG.ERROR==AcousticLogConfig.ON)
                                                    Log.e(TAG,
                                                        "ON_CLICK_LISTENER_FOR_SPECTROGRAM.onClick(v): " +
                                                        ". is _not_ a known Button, do nothing: " + v);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        afterButtonSelected(v);
                    } else {
                        // not a button, and some gui shown, but maybe url gui not shown
                        if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                            Log.d(TAG, "ON_CLICK_LISTENER_FOR_SPECTROGRAM.onClick(v): " +
                                    ". is _not_ an instance of Button: " + v);
                        doShowGui();
                    }
                } else {
                    // layout not shown (buttons not show), then show it and show the buttons and url
                    if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                        Log.d(TAG, "ON_CLICK_LISTENER_FOR_SPECTROGRAM.onClick(v): " +
                                "buttonsLayout.isShown() returned false, then show GUI");
                    doShowGui();
                }
            } catch (Throwable ex) {
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI||LOG_CONFIG.ERROR==AcousticLogConfig.ON)
                    Log.e(TAG, "ON_CLICK_LISTENER_FOR_SPECTROGRAM.onClick(v): " + ex
                            + "\n" + Log.getStackTraceString(ex)
                        );
            }
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                Log.d(TAG, "ON_CLICK_LISTENER_FOR_SPECTROGRAM.onClick(v): exiting");
        }
    };

    /**
     * Sets the buttons look and flags after the actions for the given view have been done.
     *
     * @param v the button that has been selected
     */
    void afterButtonSelected(View v) {
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
            Log.d(TAG,"afterButtonSelected: entering");

        updateAboutButtonOnUIThread();

        if (v.equals(hideUrlButton) || v.equals(playUrlButton)) {
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                Log.d(TAG,"afterButtonSelected: exiting; button is hideUrlButton or playUrlButton");
            return;
        }

        //setting the pause button
        if (isPaused()) {
            pause.setAlpha(ALPHA_LIGHT_SET);
        } else {
            //is not paused
            if (pause != null) pause.setAlpha(ALPHA_NEUTRAL_ENABLED);
//                if(spectrogramShown){
//                    //not paused and spectro should be shown, then show it
//                    if(guiLayout!=null)guiLayout.setBackgroundColor(Color.TRANSPARENT);
//                }
        }

        // setting dependents on device text
        if (v.equals(device) && deviceShown) {
            // user just selected the device button
            device.setAlpha(ALPHA_LIGHT_SET);
            aboutShown = false;
            if (about != null) about.setAlpha(ALPHA_NEUTRAL_ENABLED);
            ourAppsShown = false;
        } else {
            if (!deviceShown && device != null) device.setAlpha(ALPHA_NEUTRAL_ENABLED);
        }

        //setting dependents on about text
        if (v.equals(about) && aboutShown) {
            about.setAlpha(ALPHA_LIGHT_SET);
            deviceShown = false;
            if (device != null) device.setAlpha(ALPHA_NEUTRAL_ENABLED);
            ourAppsShown = false;
        } else {
            if (!aboutShown && about != null) about.setAlpha(ALPHA_NEUTRAL_ENABLED);
        }

        //TODO deprecated
//        if (SEPARATE_OUR_APPS_GUI_IS_ENABLED) {
//            if (v.equals(ourApps) && ourAppsShown) {
//                ourApps.setAlpha(ALPHA_LIGHT_SET);
//                deviceShown = false;
//                aboutShown = false;
//                if (device != null) device.setAlpha(ALPHA_NOT_SET);
//                if (about != null) about.setAlpha(ALPHA_NOT_SET);
//
//            } else {
//                if (!ourAppsShown && ourApps != null) ourApps.setAlpha(ALPHA_NOT_SET);
//            }
//        }
        //TODO deprecated
//        if (SEPARATE_EMAIL_DEV_GUI_IS_ENABLED) {
//            if (v.equals(emailDev)) {
//                emailDev.setAlpha(ALPHA_LIGHT_SET);
//            } else {
//                emailDev.setAlpha(ALPHA_NOT_SET);
//            }
//        }

        //setting the hide xx button
        if (deviceShown || aboutShown) {
            //text shown
            if( !ALWAYS_HIDE_BG_WHEN_TEXT) {
                if (!hideBgIsSet) {
                    if (hideGui != null) hideGui.setText("Hide BG");
                    hideBgIsSet = true;
                }
                if (v.equals(hideGui)) {
                    hideGui.setAlpha(ALPHA_LIGHT_SET);
                } else {
                    if (hideGui != null) hideGui.setAlpha(ALPHA_NEUTRAL_ENABLED);
                }
            }else{
//                if (v.equals(hideGui)) {
//                    hideGui.setAlpha(ALPHA_LIGHT_SET);
//                } else {
//                    if (hideGui != null) hideGui.setAlpha(ALPHA_NEUTRAL_ENABLED);
//                }
            }
        } else {
            //no text shown
            if( !ALWAYS_HIDE_BG_WHEN_TEXT) {
                if (hideBgIsSet) {
                    if (hideGui != null) hideGui.setText("Hide UI");
                    hideBgIsSet = false;
                }
                if (v.equals(hideGui)) {
                    hideGui.setAlpha(ALPHA_LIGHT_SET);
                } else {
                    if (hideGui != null) hideGui.setAlpha(ALPHA_NEUTRAL_ENABLED);
                }
            }else{
//                if (v.equals(hideGui)) {
//                    hideGui.setAlpha(ALPHA_LIGHT_SET);
//                } else {
//                    if (hideGui != null) hideGui.setAlpha(ALPHA_NEUTRAL_ENABLED);
//                }
            }
        }
    }

    private void deviceButtonSelected(){
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
            Log.d(TAG, "ON_CLICK_LISTENER_FOR_SPECTROGRAM.onClick(v): " +
                    "DEVICE button selected");
        if (!deviceShown) {
            //the text is not shown, then show text of device sound capabilities
            if(ALWAYS_HIDE_BG_WHEN_TEXT){
                // hide bg option is enabled
                hideBg();
                disableTheHideGuiButton();
                disableThePauseButton(pauseButtonLabel);
            } else {
                //don't change bg here
            }
            deviceShown = true;
            aboutShown = false;
            if (contentTextView != null) {
                contentTextView.setVisibility(View.VISIBLE);
                contentTextView.setText(getDeviceText());
            }
            if (largeGuiLayout != null) {
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                    Log.d(TAG, "deviceButtonSelected: " +
                            "largeGuiLayout sensitivity is disabled");
                largeGuiLayout.setOnClickListener(null);
                largeGuiLayout.setClickable(false);
            }
        } else {
            //the text is shown, then hide it, and show bg if not shown
            showBgAndHideText();
        }
    }

    private void hideGuiButtonSelected() {
        if(ALWAYS_HIDE_BG_WHEN_TEXT){
            //if text showing, then don't do anything here
            if(aboutShown || deviceShown){
//                Snackbar.make(findViewById(android.R.id.content), //coordinatorLayout,
//                        "Nothing to do for this button in this situation",//TODO res value string
//                        Snackbar.LENGTH_LONG).setAction("null", null).show();
                Toast.makeText(this,"Nothing to do for this button in this situation",
                        Toast.LENGTH_LONG).show();
                return;
            }
            doHideGui();
        }else {
            if (hideBgIsSet) {
                // is "hide bg", not "hide ui", when Device or About texts are shown
                doHideOrShowBg();
            } else {
                // is normal hide UI, so hide widgets and enable screen sensitivity
                doHideGui();
            }
        }
    }

    private void aboutButtonSelected() {
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
            Log.d(TAG, "aboutButtonSelected: " +
                    "aboutShown = " + aboutShown
                    + "; contentTextView = " + contentTextView
                    + "; largeGuiLayout = " + largeGuiLayout);
        if (!aboutShown) {
            //hide bg if option is enabled, and show the about text
            if(ALWAYS_HIDE_BG_WHEN_TEXT){
                // hide bg option is enabled
                hideBg();
                disableTheHideGuiButton();
                disableThePauseButton(pauseButtonLabel);
                //doHideUrl();
            } else {
                //don't change bg here
            }
            aboutShown = true;
            deviceShown = false;
            if (contentTextView != null) {
                contentTextView.setText(getAboutText());
                contentTextView.setVisibility(View.VISIBLE);
            }
            if (largeGuiLayout != null) {
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                    Log.d(TAG, "aboutButtonSelected: largeGuiLayout sensitivity is disabled");
                largeGuiLayout.setOnClickListener(null);
                largeGuiLayout.setClickable(false);
            }
        } else {
            //hide the about text and show bg if not shown
            showBgAndHideText();
//            aboutShown = false;
//            spectrogramShown = true;
//            textColorBrighter = false;
//            if (contentTextView != null) {
//                contentTextView.setText("");
//                contentTextView.setVisibility(View.GONE);
//                contentTextView.setTextColor(textColorTransparent);
//            }
//            if (largeGuiLayout != null) {
//                if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
//                    Log.d(TAG, "aboutButtonSelected: largeGuiLayout sensitivity is enabled");
//                largeGuiLayout.setOnClickListener(ON_CLICK_LISTENER);
//                largeGuiLayout.setClickable(true);
//                largeGuiLayout.setBackgroundColor(Color.TRANSPARENT);
//            }
        }
    }

    /**
     * Called when the URL Play/Resume button is used.
     * There is another method for the Pause/Resume Spectrogram button.
     */
    private void playUrlButtonSelected(){
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
            Log.d(TAG, ".playUrlButtonSelected: " +
                    "urlIsPlaying = " + urlIsPlaying
                    + "; urlIsPaused = " + urlIsPaused);
        clearLastAnomaly();
        if (urlIsPlaying) {
            // url is playing, so do pause url, don't play it
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL){
                Log.d(TAG,".playUrlButtonSelected: url is playing, so do pause url");
            }
            // -----------------------------
            pauseSpectro();
            doPauseUrl();
            // -----------------------------
        } else {
            // url is not playing, then do play url or resume
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL){
                Log.d(TAG,".playUrlButtonSelected: url is not playing, play url or resume");
            }
            //TODO 2017-8-16 review with new resumeOrRestart version in lib
            if (urlIsPaused) {
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL){
                    Log.d(TAG,".playUrlButtonSelected: url is not playing and is paused, so restart spectro and doResumeUrl");
                }
                // ---------------
                restartSpectro();
                doResumeUrl();
                // ---------------
            } else {
                // url was not paused, so play, don't resume;
                // try to replay the previous intent;
                // if spectro paused, then resume spectro before playing the intent
                if (isPaused()) {
                    if (LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL){
                        Log.d(TAG,".playUrlButtonSelected: spectro is paused, so calling restartSpectro() and then play(dataFromIntent)...");
                    }
                    // --------------
                    restartSpectro();
                    // --------------
                }else{
                    if (LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL){
                        Log.d(TAG,".playUrlButtonSelected: spectro is not paused, so calling play(dataFromIntent)...");
                    }
                }
                // ------------------
                play(dataFromIntent);
                // ------------------
            }
        }
    }

    private void playUrlButtonSelected2() {
        if (LOG_CONFIG.DEBUG == AcousticLogConfig.PLAY_URL)
            Log.d(TAG, "playUrlButtonSelected2: button playUrlButton selected; " +
                    "urlIsPlaying = " + urlIsPlaying
                    + "; urlIsPaused = " + urlIsPaused);

        performFileSearch();

    }

    private static final int READ_REQUEST_CODE = 42;

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void performFileSearch() {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        }else{
            if (LOG_CONFIG.DEBUG == AcousticLogConfig.PLAY_URL)
                Log.d(TAG,"android version is too low");
            return;
        }

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("audio/*");

        if (LOG_CONFIG.DEBUG == AcousticLogConfig.PLAY_URL)
            Log.d(TAG,".performFileSearch: intent {"+intent+"}");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    /**
     * Designed to be used for the function of launching an external document (recording)
     * selection process that uses an external app, and this method is called when the
     * external app is returning with a selected recording in the Intent.
     *
     * @param requestCode
     * @param resultCode
     * @param resultData
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().

//            Uri uri = null;
//            if (resultData != null) {
//                uri = resultData.getData();
//                Log.i(TAG, "Uri: " + uri.toString());
//                //showImage(uri);
//            }

            if (LOG_CONFIG.DEBUG == AcousticLogConfig.PLAY_URL)
                Log.d(TAG,".onActivityResult: Intent resultData {"+resultData+"}");

            play(resultData);

        }//TODO message when not ok
    }


    private void hideOrCancelUrlButtonSelected(){
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
            Log.d(TAG, "hideUrlButton selected; urlIsPlaying = " + urlIsPlaying);
        if (urlIsPlaying) {
            // playing, so cancel play
            doCancelUrl();
        } else {
            // not playing, so hide url if not paused; if paused, then cancel
            if(urlIsPaused){
                doCancelUrl();
            }else {
                doHideUrl(true);
            }
        }
    }


    /**
     * Designed to be called by onCreate (or by methods called by onCreate)
     * and not the Play URL button.
     *
     * <P/>Starts a new thread to prepare and play the given URL, and shows a Snackbar.
     * Calls the AudioPlayer.
     *
     * <p/>Used in this version.
     *
     * <p/>Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     *
     * @param intent referencing a remote audio or media file
     * @throws IllegalArgumentException
     */
    private boolean play(final Intent intent) {
        if(LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL||LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT)
            Log.d(TAG, ".play(Intent) entering with {" + intent + "}");
        isCancelling = false;
        //if(AudioPlayer.getIt()!=null){
//            try{
//                AudioPlayer.getIt().shutdown();
//            }catch(Exception ex){
//                if(LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL||LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT
//                        || LOG_CONFIG.ERROR==AcousticLogConfig.ON)
//                    Log.e(TAG, ".play(Intent) player.shutdown() raised " + ex );
//            }
        //}
        //player = new SmartPlayer(this);
        try {
            showUrlPrepareSnackbar();
            // ==============================
            AudioPlayer.getIt().play(intent);
            // ==============================
        } catch (Throwable ex) {
            if(LOG_CONFIG.ERROR==AcousticLogConfig.ON)
                Log.e(TAG, ".play(Intent) " + ex + " "
                        + Log.getStackTraceString(ex));
            resetUrl(true);
//            showAnomalyText(ex, ex.getMessage()); //TO DO use snackbar maybe
            // invalidIntent(intent, null);
            return false;
        }
        //intentIncoming = intent; //TODO prio 2 2017-7-27 maybe smartplayer could do playLastIntent
        setUrlGuiWhenPlaying();
        return true;
    }

    /**
     *
     *
     * @param fileUrlString
     * @return boolean true when ok, false when failure
     */
    private boolean play(final String fileUrlString) {
        if(LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL||LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT)
            Log.d(TAG, ".play(fileUrlString) entering with {" + fileUrlString + "}");
        if(fileUrlString==null||fileUrlString.isEmpty()){
            return false;
        }
        isCancelling = false;
        //if(player!=null){
//            try{
//                AudioPlayer.getIt().shutdown();
//            }catch(Exception ex){
//                if(LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL||LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT
//                        || LOG_CONFIG.ERROR==AcousticLogConfig.ON)
//                    Log.e(TAG, ".play(fileUrlString) player.shutdown() raised " + ex );
//            }
        //}
        //player = new SmartPlayer(this);
        try {
            showUrlPrepareSnackbar();
            // =====================================
            AudioPlayer.getIt().play(fileUrlString);
            // =====================================
        } catch (Throwable ex) {
            if(LOG_CONFIG.ERROR==AcousticLogConfig.ON)
                Log.e(TAG, ".play(fileUrlString {"+fileUrlString+"}) " + ex + " "
                        + Log.getStackTraceString(ex));
            resetUrl(true);
            return false;
        }
        setUrlGuiWhenPlaying();
        return true;
    }

    private boolean play(DataFromIntent dfi){
        if(LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL||LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT)
            Log.d(TAG, ".play(DataFromIntent) entering with {" + dfi.toString() + "}");

        if(dfi==null)return false;

        if(dfi.uriIsContent()){
            return play(dfi.INTENT);
        }

        //if(dfi.uriIsHttp()){ //TODO prio 2 review for improvements for other cases of url's
            return play(dfi.URL_STRING);
        //}

        //return false;
    }

    private final Runnable RUNNABLE_FOR_URL_PREPARE = new Runnable() {

        public void run() {
//            if(coordinatorLayout==null)return;
//            urlPrepareSnackbar = Snackbar.make(findViewById(android.R.id.content), //coordinatorLayout,
//                    "Please wait, preparing media for playing...",//TODO res
//                    Snackbar.LENGTH_INDEFINITE).setAction("null", null);
//            urlPrepareSnackbar.show();
            Toast.makeText(SpectrogramActivity.this,
                    "Please wait, preparing media for playing...",
                    Toast.LENGTH_LONG).show();
        }
    };

    private void showUrlPrepareSnackbar() {
        runOnUiThread(RUNNABLE_FOR_URL_PREPARE);
    }

    private volatile String statusText = "";

    private final Runnable RUNNABLE_FOR_STATUS = new Runnable() {

        public void run() {
//            if(coordinatorLayout!=null) {
//                Snackbar statusSnackbar = Snackbar.make(findViewById(android.R.id.content), //coordinatorLayout,
//                        statusText,
//                        Snackbar.LENGTH_SHORT).setAction("null", null);
//                statusSnackbar.show();
                Toast.makeText(SpectrogramActivity.this,
                        statusText,
                        Toast.LENGTH_SHORT).show();
//            }
        }
    };


    /**
     * Designed to be called when urlIsPaused is true.
     *
     * <p/>Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     */
    private void doResumeUrl() {//TODO prio 2 2017-8-16 review with new resumeOrRestart version in lib
        if(LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL){
            Log.d(TAG,"doResumeUrl: entering");
        }
        isCancelling = false;
        //if url is not paused then exit
        if (!urlIsPaused) {
            if(LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL){
                Log.d(TAG,"doResumeUrl: exiting; urlIsPaused is false, so play is not paused, so exiting");
            }
            return;
        }
//        if (player == null) {
//            if(LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL){
//                Log.d(TAG,"doResumeUrl: player is null, so calling resetUrl and exiting");
//            }
//            resetUrl(true);
//            //TODO play(???
//            return;
//        }

        boolean resumeOrRestartOk = false;
        try {
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL){
                Log.d(TAG,".doResumeUrl: player is not null, so calling player.resumeOrRestart()...");
            }
            // -------------------------------------------------------
            resumeOrRestartOk = AudioPlayer.getIt().resumeOrRestart();
            // -------------------------------------------------------
            if(LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL){
                Log.d(TAG,"doResumeUrl: player.resumeOrRestart() returned "+resumeOrRestartOk);
            }
        } catch (Exception e) {
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL){
                Log.d(TAG,".doResumeUrl: player.resumeOrRestart() raised "+e);
            }
            notifyPlayAbnormalEnd(e);
        }

        if (resumeOrRestartOk) {
            setUrlGuiWhenPlaying();
        } else {
            resetUrl(true);
        }
    }

    /**
     * Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     *
     * <p/>Input:
     * <p/>fileNameToPlay
     * <p/>fileSizeToPlayForDisplay
     * <p/>fileTextDisplayed
     * <p/>editTextUrlToPlay
     *
     * <p/>Sets fileTextDisplayed, with filename and size, displayed in UI,
     * or empty when failed or no valid input.
     */
    private void setUrlToPlayInUi() {

        if (LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE
                || LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT
                || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
            Log.d(TAG, ".setUrlToPlayInUi: fileNameToPlay {" + fileNameToPlay + "}");

        if(editTextUrlToPlay==null){
            // ui widget is null
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE
                    || LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT
                    || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
                Log.e(TAG, ".setUrlToPlayInUi: editTextUrlToPlay widget is null");
            return;
        }

        if (fileNameToPlay != null && !fileNameToPlay.isEmpty()) {
            // some value to show

            if(fileSizeToPlayForDisplay!=null && ! fileSizeToPlayForDisplay.isEmpty()) {
                fileTextDisplayed = fileNameToPlay + " (" + fileSizeToPlayForDisplay + ")";
            }else{
                fileTextDisplayed = fileNameToPlay;
            }
            editTextUrlToPlay.setText(fileTextDisplayed);

        }else{
            // no value to show
            editTextUrlToPlay.setText("");
        }
    }

    /**
     * Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     */
    private void setUrlGuiWhenPlaying() {
        urlIsPlaying = true;
        urlIsPaused = false;
        if (playUrlButton != null) playUrlButton.setText("Pause");
        if (hideUrlButton != null) hideUrlButton.setText("Cancel");
        if (editTextUrlToPlay != null) editTextUrlToPlay.setTextColor(urlColorForActive);
    }

    /**
     * To be called when the Play-Url (pause) button selected while the url is playing.
     *
     * <p/>Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     */
    private void doPauseUrl() {
        // if the url is not playing then do nothing
        if ( ! urlIsPlaying) return;
        // here when url is playing, and player may be preparing to play or not playing due to invalid audio source
        // TODO if player is not prepared then reset
        if( AudioPlayer.getIt().isPreparing()){
            // player is preparing
            // ------------------------
            AudioPlayer.getIt().stop();
            // ------------------------
            resetUrl(false);
            return;
        }
        // player is not preparing, and it may not be playing
        if( ! AudioPlayer.getIt().isPlaying()){
            // player is not preparing and not playing
            resetUrl(false);
            return;
        }
        // player is playing (not preparing), then pause it
        // --------------------------------------------------
        boolean pauseSucceeded = AudioPlayer.getIt().pause();
        // --------------------------------------------------
        if(LOG_CONFIG.DEBUG==LOG_CONFIG.PLAY_URL)
            Log.d(TAG,".doPauseUrl: player.pause() returned "+pauseSucceeded);
        if (pauseSucceeded) {
            //player pause succeeded
//            if (urlPrepareSnackbar != null && urlPrepareSnackbar.isShown()) {
//                //urlPrepareSnackbar is not completed, then dismiss it
//                urlPrepareSnackbar.dismiss();
//                // TODO was player preparation completed ???
//                if(LOG_CONFIG.DEBUG==LOG_CONFIG.PLAY_URL)
//                    Log.w(TAG,".doPauseUrl: urlPrepareSnackbar was showing, was player preparation completed?");
//            }
            urlIsPlaying = false;
            urlIsPaused = true;
            if (playUrlButton != null) playUrlButton.setText("Resume");
            if (hideUrlButton != null) hideUrlButton.setText("Cancel");
            if (editTextUrlToPlay != null) editTextUrlToPlay.setTextColor(urlColorForPaused);
        } else {
            // player pause failed, then stop the player and reset the labels
            // urlIsPlaying = false;
            // urlIsPaused = false;
            resetUrl(false);
        }
    }

    /**
     * Stops the player when this is for an anomaly.
     * Includes resetting the recording file buttons labels to "Play" and "Hide URL".
     *
     * <p/>Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     *
     * @param isForAnomaly when true, the url color is set to the error color
     */
    private void resetUrl(boolean isForAnomaly) {
//        if (urlPrepareSnackbar != null) urlPrepareSnackbar.dismiss();
        resetUrlGui();//color is set to urlColorForInactive
        if (isForAnomaly) {
            AudioPlayer.getIt().stop();
            editTextUrlToPlay.setTextColor(urlColorForError);
        }
        //TODO prepare anomaly text for display; get text fragment from client method
    }

    /**
     * stop the play; for the hideUrlButton button when label is *Cancel* to stop the play and not hide gui
     */
    private void doCancelUrl() {
        isCancelling=true;
        resetUrlGui();
        AudioPlayer.getIt().stop(); //shutdown();
    }

    /**
     * @param percent the percentage (0-100) of the content
     *                that has been buffered or played thus far
     */
    @Override
    public void onPlayerBufferingUpdate(int percent){
        // do nothing in this version TODO prio 2 show percent to user in snackbar
    }

    /**
     * A zero or negative value will have the player use the default timeout (ex. 30 sec.).
     *
     * @return int seconds to trigger a timeout event for the preparation of a data source.
     */
    @Override
    public int getPlayerPreparationTimeoutSec(){
        return 15;
    }

    /**
     * Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     * <p/>
     * <p/>Also called when a file ends playing normally.
     */
    private void resetUrlGui() {
        urlIsPlaying = false;
        urlIsPaused = false;
        if (playUrlButton != null) playUrlButton.setText("Play");
        if (hideUrlButton != null) hideUrlButton.setText("Hide URL");
        if (editTextUrlToPlay != null) editTextUrlToPlay.setTextColor(urlColorForInactive);
    }

    /**
     * Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     *
     * @param withNotif true when caller wants the notification to be shown to user.
     */
    private void doHideUrl(final boolean withNotif) {
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
            Log.d(TAG, "doHideUrl: entering");

        doCancelUrl();

        if (urlLayout != null) urlLayout.setVisibility(View.GONE);

        if (largeGuiLayout != null) {
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                Log.d(TAG, "doHideUrl: largeGuiLayout sensitivity is enabled");
            largeGuiLayout.setOnClickListener(ON_CLICK_LISTENER);
        }

        if (withNotif) {
            String s = "Tap the screen to show the media GUI.";
            if(contentTextView.getVisibility()==View.VISIBLE){
                s = "To show the media GUI again, first hide the text by tapping the related text button and then tap the screen.";
            }
//            if (coordinatorLayout != null) {
//                Snackbar.make(findViewById(android.R.id.content), //coordinatorLayout,
//                        s,
//                        Snackbar.LENGTH_LONG)
//                        .setAction("null", null).show();
//            }
            Toast.makeText(this, s, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE || LOG_CONFIG.DEBUG==AcousticLogConfig.THREADS
                || LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE)
            Log.d(TAG, ".onSaveInstanceState " + Thread.currentThread());

        saveBitmap(outState);
    }

    /**
     * Called after onStart when the activity is being re-initialized from a previously saved state.
     * <p/>
     * This method is called between onStart and onPostCreate.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {//TODO future restore bitmap
        super.onSaveInstanceState(savedInstanceState);
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE || LOG_CONFIG.DEBUG==AcousticLogConfig.THREADS
                || LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE)
            Log.d(TAG, ".onRestoreInstanceState: calling restoreBitmap(savedInstanceState)..."
                    + Thread.currentThread());

        restoreBitmap(savedInstanceState);
    }


    private void restoreBitmap(final Bundle savedInstanceState) {
        if (!SPECTROGRAM_IMAGE_PERSISTENCE_IS_ENABLED) {
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE || LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE)
                Log.d(TAG, ".restoreBitmap: exiting because SPECTROGRAM_IMAGE_PERSISTENCE_IS_ENABLED is false");
            return;
        }
        if (savedInstanceState != null) {
            // not new session; restoring the app from Android savedInstanceState
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE || LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE)
                Log.d(TAG, ".restoreBitmap: restoring");
//            SpectrogramView.restoring = true; TODO future restoring bitmap function
            //------------------------------------------------------------
            Object ob = savedInstanceState.getParcelable(PREF_BITMAP_KEY);
            //------------------------------------------------------------
            if (ob != null) {
//                SpectrogramView.bitmap = (Bitmap) ob;
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE || LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE)
                    Log.d(TAG, ".restoreBitmap: restoring; saved bitmap not null");
            } else {
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE || LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE)
                    Log.d(TAG, ".restoreBitmap: restoring; saved bitmap is null");
            }

        } else {
            // not restoring; new session
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE || LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE)
                Log.d(TAG, ".restoreBitmap: not restoring");
//            SpectrogramView.restoring = false;
//            SpectrogramView.bitmap = null;
        }
    }

    private void saveBitmap(Bundle outState) {
        if (!SPECTROGRAM_IMAGE_PERSISTENCE_IS_ENABLED) {
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE || LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE)
                Log.d(TAG, ".saveBitmap: exiting because SPECTROGRAM_IMAGE_PERSISTENCE_IS_ENABLED is false");
            return;
        }

        if (SpectrogramView.spectrogramView != null) {
            Bitmap bitmap = SpectrogramView.spectrogramView.bitmap;
            if (bitmap != null) {
                //----------------------------------------------
                outState.putParcelable(PREF_BITMAP_KEY, bitmap);
                //----------------------------------------------
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE || LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE)
                    Log.d(TAG, ".saveBitmap: bitmap saved");
            } else {
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE || LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE)
                    Log.d(TAG, ".saveBitmap: bitmap is null");
            }
        }
    }

    /**
     * This may be preserved (as true) when using another app on top of this one, briefly, when
     * the user has paused it for some reason.
     * Currently it is reset to false in onStop but it may be kept as true
     * when the restore-bg-image-enabled is set
     * TODO future review usage for restoring the canvas/bitmap when restarting/restoring the app
     */
    private boolean isPausedByHUser = false;

    private void restartSpectro() {
        try {
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE
                    || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
                Log.d(TAG, ".restartSpectro: calling unpause()...");
            unpause();
            pause.setText(getString(R.string.pause_button));
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE
                    || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
                Log.d(TAG, ".restartSpectro: unpause() ok");
        } catch (Exception e) {
            //e.printStackTrace();
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_INPUT_INIT
                    || LOG_CONFIG.ERROR==AcousticLogConfig.ON
                    || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
                Log.e(TAG, ".restartSpectro: unpause() raised: " + e
                    +"\n"+Log.getStackTraceString(e));
            return;
        }
        if(LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
            Log.d(TAG, ".restartSpectro: exiting, isPausedByHUser set to false");
        isPausedByHUser = false;
    }

    private void pauseSpectro() {
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE)
            Log.d(TAG, ".pauseSpectro entering");
        closeListener();
        pause.setText(getString(R.string.pause_button_restart));
        isPausedByHUser = true;
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE)
            Log.d(TAG, ".pauseSpectro exiting");
    }

    /**
     * Called when the spectrogram Pause/Resume button is used (not the URL Play/Pause button).
     *
     * <p/>Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     */
    private void pauseToggle() {
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
            Log.d(TAG, ".pauseToggle: entering; isPausedByHUser " + isPausedByHUser
                            + "; isPaused() " + isPaused()
            );
        if (isPausedByHUser || isPaused()) {
            // is paused, then restart
            // --------------
            restartSpectro();
            // --------------
            // url is not playing, then do play url or resume
            //TODO prio 2 2017-8-16 review with new resumeOrRestart version in lib
            if (urlIsPaused) {
                // -----------
                doResumeUrl();
                // -----------
            } else {
                // was not paused, so play, don't resume
                // there is a time limit on attempt to play
                // play(editTextUrlToPlay.getText().toString());
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
                    Log.d(TAG, ".pauseToggle: entering; isPausedByHUser " + isPausedByHUser
                            + "; urlIsPaused " + urlIsPaused
                    );
            }
        } else {
            //is running, then pause
            // -----------------------------
            pauseSpectro();
            doPauseUrl();
            // -----------------------------
        }
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE)
            Log.d(TAG, ".pauseToggle: exiting; isPausedByHUser " + isPausedByHUser
                            + "; isPaused() " + isPaused()
            );
    }


    public boolean isPaused() {
        //return Acoustic.getIt().isRunning();
        return isPauseButtonChecked();
    }


    public void unpause() throws Exception {
        //getListener();
        Acoustic.getIt().restart();
    }


    public boolean isPauseSelected() {
        return false;
    }

    public boolean onPauseSoundInput(String s) {
        return false;
    }


    public boolean isPauseButtonChecked() {
        if(pause==null)return false;
        return pause.isPressed();
    }

    //AcousticEvent.GET_IS_SOUND_INPUT_PAUSED_BY_CLIENT

    /**
     * checks the pause button and maybe a status attribute in some cases
     *
     * @return true when paused
     */
    boolean isSoundInputPaused(){ //TODO washere 2018-7-14 called by onAcousticEvent ???
        return isPauseButtonChecked();
    }



//    @ Override
//    public boolean onCreateOptionsMenu(Menu menu) { TODO do we need an Options Menu???
//        // Inflate the menu; this adds items to the action bar if it is present.
////        getMenuInflater().inflate(R.menu.menu, menu);
////        return true;
//        return false;
//    }
//
//    @ Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
////        if (id == R.id.action_settings) {
////            SettingsTextActivity.show(this, //getSettingsText(),
////                    16f, //null);
////                    getResources().getString(R.string.app_name_short)); //MENU_SETTINGS_NAME);
////            return true;
////        }
//        if (id == R.id.action_device_sound_capabilities) {
//            //DeviceSoundCapabilitiesFullscreenActivity.show(this);
//            final String text = DeviceSoundCapabilities.getOptimalConfigsForDisplay(true);
//            TextDisplayActivity.show(this,text,AppContext.getAppName(),TextDisplayActivity.TYPE_MATERIAL_SM_1);
//            return true;
//        }
//
//        if (id == R.id.action_about) {
//            //FullscreenTextActivity.show(this, getAboutText(),false);
//            TextDisplayActivity.show(this,getAboutText(),AppContext.getAppName(),TextDisplayActivity.TYPE_MATERIAL_SM_1);
//            return true;
//        }
//
//        if (id == R.id.action_email) {
////            onUiThread.emailOption(new String[]{"info@simplocode.com"},
////                    "",getAppName(this),-1);
//            sendEmail(getString(R.string.email_address),
//                    "write your subject line here",
//                    "write your text here");
//            return true;
//        }
//
//        if (id == R.id.action_our_apps) {
//            //FullscreenTextActivity.showOurAppsText(this);
//            final String text = LeafyManagedTextView.getOurAppsText(this);
//            TextDisplayActivity.show(this,text,AppContext.getAppName(),TextDisplayActivity.TYPE_MATERIAL_SM_CENTERED);
//            return true;
//        }
//
//        if (id == R.id.action_pause) {
//            pause();
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    public void sendEmail(final String address, final String subject, final String text) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("mailto:" + address));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(intent);
    }

    public void sendEmailToSupport(final String text) {
        if (!isSupportEmailEnabled()) return;
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("mailto:" + AppPublisher.emailAddressForSupport));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Email to support");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(intent);
    }

//
//    public Runnable getRunnableForUiAfterDbLoaded() {
//        return null;
//    }
//
//
//    public Runnable getRunnableForMainProcess() {
//        return null;
//    }
//
//
//    public Runnable getRunnableToUnpauseOnUi() {
//        return null;
//    }
//
//
//    public Runnable getRunnableToEnableEmitButton() {
//        return null;
//    }

    /**
     * Set to null when cleared. May be null even when anomaly detected.
     */
    private volatile Throwable lastThrowable = null;

    /**
     * Cleared (set to null) by component that sets it and at app restart and when url starts playing.
     */
    private volatile String lastAnomalyText = null;

    /**
     * -1L when cleared.
     */
    private volatile long lastAnomalyTimeMillis = -1;

    /**
     * @return String or null when none;
     * when an anomaly text exists, then it is formatted with html tags for Html.fromHtml().
     */
    private String getLastAnomalyTextInHtml() {
        if (lastAnomalyText == null) return null;
        StringBuilder buf = new StringBuilder();
        buf.append("<h3>Last Anomaly:</h3><p/>");
        buf.append(lastAnomalyText);
        if (isDevMode()) {
            if (lastThrowable != null)
                buf.append("<p/>").append(Log.getStackTraceString(lastThrowable));
        } else {
            buf.append("<p/>For support, please contact ")
                    .append(AppPublisher.emailAddressForSupport);
        }
        buf.append("<p/>The anomaly was detected ").append(new Date(lastAnomalyTimeMillis));

        return buf.toString();
    }

    private void updateAboutButton() {
        if (lastAnomalyText == null || lastAnomalyText.length() == 0) {
            runOnUiThread(RUNNABLE_TO_CLEAR_ANOMALY_TEXT);
        }
    }

    @NonNull
    private String getDeviceText(){
//        StringBuilder buf = new StringBuilder();
//        buf.append( Html.fromHtml( getDeviceTextInHtml() ));
//        return buf.toString();
        return fromHtmlToString(getDeviceTextInHtml());
    }

    @NonNull
    private String getDeviceTextInHtml(){
        StringBuilder buf = new StringBuilder();

        buf.append(getContentSectionForDeviceTextInHtml())
                .append(AcousticDeviceCapabilities.getIt().getDeviceCapabilitiesInHtml())
                //.append(DeviceSoundCapabilities.getDeviceCapabilitiesInHtml(true, true, this))
                .append(AcousticSettings.getForAppTextInHtml()) //.getForAppTextInHtml(true))
                .append(getPerfMeasurementsInHtml())
        ;

        return buf.toString();
    }

    @NonNull
    private String getPerfMeasurementsInHtml(){
        StringBuilder buf = new StringBuilder();

        buf.append("<p/><h2>SOUND PROCESSING PERFORMANCE MEASUREMENTS</h2>")
        .append("<p/>Android devices these days have a delay of about half a second between the actual sound and the processing by the app, when non-native code is used")
        .append("; this app adds about 1/50 sec. of delay due to numerical analysis (e.g., FFT) and display; 1/50 sec. of delay is about 10% of the basic delay or latency for Android in 2016.")
        .append(" The time spent by Android and the app displaying the data as a spectrogram is much more than the time used for numerical analysis.")
        .append("<p/>").append(Acoustic.getIt().getProcessingPerfInHtml())  //BasicListener.processingPerfInHtml)
        .append("<p/>End of performance measurements results")
        ;
        return buf.toString();
    }

    @NonNull
    private String getContentSectionForDeviceTextInHtml(){
        StringBuilder buf = new StringBuilder();

        buf.append("<h4>Content:</h4>")
                .append("<p/>Sound Capabilities of Device")
                .append("<br>Sound Settings")
                .append("<br>Sound Performance Measurements")
                .append("<p/>")
        ;
        return buf.toString();
    }

    /**
     * TODO prio 2 move to library 2017-5
     * @param html String text including html tags
     * @return android.text.Spanned, implements CharSequence, formatted and without html tags
     */
    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(final String html){
        Spanned result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            result = Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY);
        } else {
            // deprecated
            result = Html.fromHtml(html);
        }
        return result;
    }

    public static String fromHtmlToString(final String html){
        return fromHtml(html).toString();
    }

    @NonNull
    private String getAboutText() {
        StringBuilder buf = new StringBuilder();

        buf.append(fromHtml( getAboutTextInHtml() ));

        if(LOG_CONFIG.DEBUG==AcousticLogConfig.UI){
            Log.d(TAG,".getAboutText: length = "+buf.length());
        }

        return buf.toString();
    }

    /**
     * Sometimes includes Help text and details about recent anomaly.
     *
     * @return String with html tags compatible with Html.fromHtml.
     */
    private String getAboutTextInHtml() {

        StringBuilder buf = new StringBuilder(); //.toUpperCase());

        String anomaliesText = getLastAnomalyTextInHtml();
        if( anomaliesText != null && ! anomaliesText.isEmpty()){
            buf.append(anomaliesText);
//            buf.append("<p/>Support email: ");
//            buf.append(AppPublisher.emailAddressForSupport);
            lastAnomalyTextIsShown = true;
            return buf.toString();
        }

        lastAnomalyTextIsShown = false;
        runOnUiThread(RUNNABLE_TO_CLEAR_ANOMALY_TEXT);

        buf.append("<h2>ABOUT THIS APP - ");
        buf.append(getString(R.string.app_name)).append("</h2>");

        buf.append("<h4>Content:</h4>")
        .append("<p/>Copyright and Credits")
        .append("<br>Version and Contact Info")
        .append("<br>Introduction")
        .append("<br>User Interface Guide")
        .append("<br>Privacy Policy")
        .append("<br>Terms of Use")
        .append("<p/>")
        ;

        buf.append("<p/>").append(AppPublisher.copyright);

        buf.append("<p/>")
        .append("Some links to cetacean vocalisation recordings are Copyright Aguasonic Acoustics.")
        .append(" And some icons were made using a fascinating recording by www.aguasonic.com " +
                "of a group of dolphins, probably Tursiops, where sometimes at least 5 individuals are whistling at the same time.");

        buf.append("<p/>").append("Application Version Code: ")
        .append(AppContext.getVersionCode())
        .append("<br>Version Name: ")
        .append(AppContext.getVersionName())
        .append("<p/>Questions, defects, suggestions, please contact ")
                .append(AppPublisher.emailAddressForSupport);

        // ##### intro #####

        buf.append("<p/><h2>INTRODUCTION</h2>");
        buf.append("<p/>This app displays a spectrogram to analyze live sounds in near real-time.");
        //buf.append(" ...we do offer other apps that do emit sounds.");

        buf.append("<p/>");
        buf.append("For best results, other apps running during a spectrogram display should be kept to a minimum.");

        buf.append("<p/>The spectrogram displays the intensity or energy of each frequency level and at each frequency sampling cycle, over time.")
                .append(" The frequency levels are displayed vertically, where the lower frequencies are at the bottom of the screen.")
                .append(" The frequency cycles are displayed from left to right, in a near real-time fashion.")
                .append(" The sound intensity is color-coded: the maximum intensity is red, ")
                .append("the lower intensities are yellow, green, and blue, from light to dark")
                .append("; very low intensity or lack of sound are black.");

        buf.append("<p/>The overall intensity of the sound is displayed in a small green band below the spectrogram.")
                .append(" Please note that on some devices and in certain orientation, ")
                .append("the green band may not have enough room to be shown, priority being given to the spectrogram.");

        // ~~~~~ device sound capabilities and audio config ~~~~~

        buf.append("<p/>You may use the <tt>DEVICE</tt> button to show details about the sound capabilities of your device.");

        // ##### FFT Info #####

        //buf.append("<p/>~~~~~~~~~~");
//        buf.append("<p/>") TODO prio 2 future confirm source of FFT
//                .append("The Fast Fourier Transform (FFT) calculations include parts of the NIST (National Institute of Standards) scimark2 program")
//                .append(", as well as calculations developed by Serge Masse. ")
//                .append("The original NIST Java code, not all used here, was written by Bruce R. Miller, and was inspired by the GSL"
//                .append(" (Gnu Scientific Library) FFT written in C by Brian Gough. ");

        //buf.append("The FFT use 'double' primitives."); TODO prio 2 future use option for dev for float vs double
        //buf.append("<p/>~~~~~~~~~~");

        // ##### UI Guide #####

        buf.append(getUIGuideInHtml());

        // ##### our apps text #####

        if (!SEPARATE_OUR_APPS_GUI_IS_ENABLED && AppPublisher.googlePlayPubAppsTextIsEnabled) {
            buf.append(getOurAppsText());
        }

        buf.append(getPrivacyPolicyInHtml());

        buf.append(getLicenseAndTermsInHtml());

        return buf.toString();
    }

    /* TODO prio 2 EULA example from https://www.makingmoneywithandroid.com/2011/05/how-to-eula-android-app/

    By downloading any application from <YOURCOMPANYNAME> or Google™ (here after referered to as “The Company”), installing or using this application or any portion thereof (“Application”), you agree to the following terms and conditions (the “Terms and Conditions”).

1. USE OF APPLICATION
a. The Company grants you the non-exclusive, non-transferable, limited right and license to install and use this Application solely and exclusively for your personal use.
b. You may not use the Application in any manner that could damage, disable, overburden, or impair the Application (or servers or networks connected to the Application), nor may you use the Application in any manner that could interfere with any other party’s use and enjoyment of the Application (or servers or networks connected to the Application).
c. You agree that you are solely responsible for (and that The Company has no responsibility to you or to any third party for) your use of the Application, any breach of your obligations under the Terms and Conditions, and for the consequences (including any loss or damage which The Company may suffer) of any such breach.

2. PROPRIETARY RIGHTS
You acknowledge that (a) the Application contains proprietary and confidential information that is protected by applicable intellectual property and other laws, and (b) The Company and/or third parties own all right, title and interest in and to the Application and content, excluding content provided by you, that may be presented or accessed through the Application, including without limitation all Intellectual Property Rights therein and thereto. “Intellectual Property Rights” means any and all rights existing from time to time under patent law, copyright law, trade secret law, trademark law, unfair competition law, and any and all other proprietary rights, and any and all applications, renewals, extensions and restorations thereof, now or hereafter in force and effect worldwide. You agree that you will not, and will not allow any third party to, (i) copy, sell, license, distribute, transfer, modify, adapt, translate, prepare derivative works from, decompile, reverse engineer, disassemble or otherwise attempt to derive source code from the Application or content that may be presented or accessed through the Application for any purpose, unless otherwise permitted, (ii) take any action to circumvent or defeat the security or content usage rules provided, deployed or enforced by any functionality (including without limitation digital rights management functionality) contained in the Application, (iii) use the Application to access, copy, transfer, transcode or retransmit content in violation of any law or third party rights, or (iv) remove, obscure, or alter The Company’s or any third partyâ€™s copyright notices, trademarks, or other proprietary rights notices affixed to or contained within or accessed in conjunction with or through the Application.

3. THE COMPANY TERMS OF SERVICE AND PRIVACY POLICY
a. The Company’s Privacy Policy (located at http://www.google.com/privacypolicy.html) explains how The Company treats your information and protects your privacy when you use the Application. You agree to the use of your data in accordance with The Company’s privacy policies.
b. The Application may contain features that are used in conjunction with The Company’s search and other services. Accordingly, your use of such features of the Application is also governed by The Company’s Terms of Service located at http://www.google.com/terms_of_service.html, The Company’s Privacy Policy located at http://www.google.com/privacypolicy.html, as well as any applicable The Company Service-specific Terms of Service and Privacy Policy, which may be updated from time to time and without notice.


4. U.S. GOVERNMENT RESTRICTED RIGHTS
This Application, related materials and documentation have been developed entirely with private funds. If the user of the Application is an agency, department, employee, or other entity of the United States Government, the use, duplication, reproduction, release, modification, disclosure, or transfer of the Application, including technical data or manuals, is restricted by the terms, conditions and covenants contained in these Terms and Conditions. In accordance with Federal Acquisition Regulation 12.212 for civilian agencies and Defense Federal Acquisition Regulation Supplement 227.7202 for military agencies, use of the Application is further restricted by these Terms and Conditions.

5. EXPORT RESTRICTIONS
The Application may be subject to export controls or restrictions by the United States or other countries or territories. You agree to comply with all applicable U.S. and international export laws and regulations. These laws include restrictions on destinations, end users, and end use.

6. TERMINATION
These Terms and Conditions will continue to apply until terminated by either you or The Company as set forth below. You may terminate these Terms and Conditions at any time by permanently deleting the Application from your mobile device in its entirety. Your rights automatically and immediately terminate without notice from The Company or any Third Party if you fail to comply with any provision of these Terms and Conditions. In such event, you must immediately delete the Application.

7. INDEMNITY
To the maximum extent permitted by law, you agree to defend, indemnify and hold harmless The Company, its affiliates and their respective directors, officers, employees and agents from and against any and all claims, actions, suits or proceedings, as well as any and all losses, liabilities, damages, costs and expenses (including reasonable attorneys fees) arising out of or accruing from your use of the Application, including your downloading, installation, or use of the Application, or your violation of these Terms and Conditions.

8. DISCLAIMER OF WARRANTIES
a. YOU EXPRESSLY UNDERSTAND AND AGREE THAT YOUR USE OF THE APPLICATION IS AT YOUR SOLE DISCRETION AND RISK AND THAT THE APPLICATION IS PROVIDED AS IS AND AS AVAILABLE WITHOUT WARRANTY OF ANY KIND.
b. YOU ARE SOLELY RESPONSIBLE FOR ANY DAMAGE TO YOUR MOBILE DEVICE, OR OTHER DEVICE, OR LOSS OF DATA THAT RESULTS FROM SUCH USE.
c. THE COMPANY FURTHER EXPRESSLY DISCLAIMS ALL WARRANTIES AND CONDITIONS OF ANY KIND, WHETHER EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO THE IMPLIED WARRANTIES AND CONDITIONS OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT, WITH RESPECT TO THE APPLICATION.
d. THE APPLICATION IS NOT INTENDED FOR USE IN THE OPERATION OF NUCLEAR FACILITIES, LIFE SUPPORT SYSTEMS, EMERGENCY COMMUNICATIONS, AIRCRAFT NAVIGATION OR COMMUNICATION SYSTEMS, AIR TRAFFIC CONTROL SYSTEMS, OR ANY OTHER ACTIVITIES IN WHICH THE FAILURE OF THE APPLICATION COULD LEAD TO DEATH, PERSONAL INJURY, OR SEVERE PHYSICAL OR ENVIRONMENTAL DAMAGE.

9. LIMITATION OF LIABILITY
YOU EXPRESSLY UNDERSTAND AND AGREE THAT THE COMPANY, ITS SUBSIDIARIES AND AFFILIATES, AND ITS LICENSORS ARE NOT LIABLE TO YOU UNDER ANY THEORY OF LIABILITY FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL CONSEQUENTIAL OR EXEMPLARY DAMAGES THAT MAY BE INCURRED BY YOU THROUGH YOUR USE OF THE APPLICATION, INCLUDING ANY LOSS OF DATA OR DAMAGE TO YOUR MOBILE DEVICE, WHETHER OR NOT THE COMPANY OR ITS REPRESENTATIVES HAVE BEEN ADVISED OF OR SHOULD HAVE BEEN AWARE OF THE POSSIBILITY OF ANY SUCH LOSSES ARISING.



10. MISCELLANEOUS
a. These Terms and Conditions constitute the entire Agreement between you and The Company relating to the Application and govern your use of the Application, and completely replace any prior or contemporaneous agreements between you and The Company regarding the Application.
b. The failure of The Company to exercise or enforce any right or provision of these Terms and Conditions does not constitute a waiver of such right or provision, which will still be available to The Company.
c. If any court of law, having the jurisdiction to decide on this matter, rules that any provision of these Terms and Conditions is invalid, then that provision will be removed from the Terms and Conditions without affecting the rest of the Terms and Conditions. The remaining provisions of these Terms and Conditions will continue to be valid and enforceable.
d. The rights granted in these Terms and Conditions may not be assigned or transferred by either you or The Company without the prior written approval of the other party. Neither you nor The Company are permitted to delegate their responsibilities or obligations under these Terms and Conditions without the prior written approval of the other party.
e. These Terms and Conditions and your relationship with The Company under these Terms and Conditions will be governed by the laws of the State of California without regard to its conflict of laws provisions. You and The Company agree to submit to the exclusive jurisdiction of the courts located within the county of Santa Clara, California to resolve any legal matter arising from these Terms and Conditions. Notwithstanding this, you agree that The Company will still be allowed to apply for injunctive remedies (or an equivalent type of urgent legal relief) in any jurisdiction.
     */

    /* TODO prio 2 from https://web.archive.org/web/20130205134238/http://www.developer-resource.com/sample-eula.htm

    END-USER LICENSE AGREEMENT FOR {INSERT PRODUCT NAME} IMPORTANT PLEASE READ THE TERMS AND CONDITIONS OF THIS LICENSE AGREEMENT CAREFULLY BEFORE CONTINUING WITH THIS PROGRAM INSTALL: {INSERT COMPANY NAME's } End-User License Agreement ("EULA") is a legal agreement between you (either an individual or a single entity) and {INSERT COMPANY NAME}. for the {INSERT COMPANY NAME} software product(s) identified above which may include associated software components, media, printed materials, and "online" or electronic documentation ("SOFTWARE PRODUCT"). By installing, copying, or otherwise using the SOFTWARE PRODUCT, you agree to be bound by the terms of this EULA. This license agreement represents the entire agreement concerning the program between you and {INSERT COMPANY NAME}, (referred to as "licenser"), and it supersedes any prior proposal, representation, or understanding between the parties. If you do not agree to the terms of this EULA, do not install or use the SOFTWARE PRODUCT.

The SOFTWARE PRODUCT is protected by copyright laws and international copyright treaties, as well as other intellectual property laws and treaties. The SOFTWARE PRODUCT is licensed, not sold.

1. GRANT OF LICENSE.
The SOFTWARE PRODUCT is licensed as follows:
(a) Installation and Use.
{INSERT COMPANY NAME} grants you the right to install and use copies of the SOFTWARE PRODUCT on your computer running a validly licensed copy of the operating system for which the SOFTWARE PRODUCT was designed [e.g., Windows 95, Windows NT, Windows 98, Windows 2000, Windows 2003, Windows XP, Windows ME, Windows Vista].
(b) Backup Copies.
You may also make copies of the SOFTWARE PRODUCT as may be necessary for backup and archival purposes.

2. DESCRIPTION OF OTHER RIGHTS AND LIMITATIONS.
(a) Maintenance of Copyright Notices.
You must not remove or alter any copyright notices on any and all copies of the SOFTWARE PRODUCT.
(b) Distribution.
You may not distribute registered copies of the SOFTWARE PRODUCT to third parties. Evaluation versions available for download from {INSERT COMPANY NAME}'s websites may be freely distributed.
(c) Prohibition on Reverse Engineering, Decompilation, and Disassembly.
You may not reverse engineer, decompile, or disassemble the SOFTWARE PRODUCT, except and only to the extent that such activity is expressly permitted by applicable law notwithstanding this limitation.
(d) Rental.
You may not rent, lease, or lend the SOFTWARE PRODUCT.
(e) Support Services.
{INSERT COMPANY NAME} may provide you with support services related to the SOFTWARE PRODUCT ("Support Services"). Any supplemental software code provided to you as part of the Support Services shall be considered part of the SOFTWARE PRODUCT and subject to the terms and conditions of this EULA.
(f) Compliance with Applicable Laws.
You must comply with all applicable laws regarding use of the SOFTWARE PRODUCT.

3. TERMINATION
Without prejudice to any other rights, {INSERT COMPANY NAME} may terminate this EULA if you fail to comply with the terms and conditions of this EULA. In such event, you must destroy all copies of the SOFTWARE PRODUCT in your possession.

4. COPYRIGHT
All title, including but not limited to copyrights, in and to the SOFTWARE PRODUCT and any copies thereof are owned by {INSERT COMPANY NAME} or its suppliers. All title and intellectual property rights in and to the content which may be accessed through use of the SOFTWARE PRODUCT is the property of the respective content owner and may be protected by applicable copyright or other intellectual property laws and treaties. This EULA grants you no rights to use such content. All rights not expressly granted are reserved by {INSERT COMPANY NAME}.

5. NO WARRANTIES
{INSERT COMPANY NAME} expressly disclaims any warranty for the SOFTWARE PRODUCT. The SOFTWARE PRODUCT is provided 'As Is' without any express or implied warranty of any kind, including but not limited to any warranties of merchantability, noninfringement, or fitness of a particular purpose. {INSERT COMPANY NAME} does not warrant or assume responsibility for the accuracy or completeness of any information, text, graphics, links or other items contained within the SOFTWARE PRODUCT. {INSERT COMPANY NAME} makes no warranties respecting any harm that may be caused by the transmission of a computer virus, worm, time bomb, logic bomb, or other such computer program. {INSERT COMPANY NAME} further expressly disclaims any warranty or representation to Authorized Users or to any third party.

6. LIMITATION OF LIABILITY
In no event shall {INSERT COMPANY NAME} be liable for any damages (including, without limitation, lost profits, business interruption, or lost information) rising out of 'Authorized Users' use of or inability to use the SOFTWARE PRODUCT, even if {INSERT COMPANY NAME} has been advised of the possibility of such damages. In no event will {INSERT COMPANY NAME} be liable for loss of data or for indirect, special, incidental, consequential (including lost profit), or other damages based in contract, tort or otherwise. {INSERT COMPANY NAME} shall have no liability with respect to the content of the SOFTWARE PRODUCT or any part thereof, including but not limited to errors or omissions contained therein, libel, infringements of rights of publicity, privacy, trademark rights, business interruption, personal injury, loss of privacy, moral rights or the disclosure of confidential information.
     */

    @NonNull
    private String getLicenseAndTermsInHtml() {
        StringBuilder buf = new StringBuilder();

//        buf.append("<p/><H3>License</H3><p/>");

//        buf.append("Licensed under the Apache License, Version 2.0 (the \"License\");");
//        buf.append(" you may not use this software except in compliance with the License.");
//        buf.append("<br>You may obtain a copy of the License at");
//
//        buf.append("<p/>http://www.apache.org/licenses/LICENSE-2.0");
//
//        buf.append("<p/>Unless required by applicable law or agreed to in writing, software");
//        buf.append(" distributed under the License is distributed on an \"AS IS\" BASIS,");

        buf.append("<p/><H2>TERMS OF USE</H2><p/>");

        //buf.append("Unless required by applicable law or agreed to in writing, this software");
        buf.append("This software is distributed on an \"AS IS\" BASIS,");
        buf.append(" WITHOUT WARRANTIES OF ANY KIND, either express or implied.");

        buf.append("<p/>This software is not to be used for the purpose of killing, harming, or capturing animals.");
        buf.append(" The use of this software with captive cetaceans is discouraged.");

        return buf.toString();
    }

    @NonNull
    private String getPrivacyPolicyInHtml() {
        StringBuilder buf = new StringBuilder();

        buf.append("<p/><H2>PRIVACY POLICY</H2><p/>");

        buf.append("This app does not send any information to anyone.");

        buf.append("<p/>The author/publisher would keep private any emails or other communications from users.");

        return buf.toString();
    }

    @NonNull
    private String getUIGuideInHtml() {
        StringBuilder buf = new StringBuilder();

        buf.append("<p/><H2>USER INTERFACE GUIDE</H2>");

        buf.append("<p/>There is only one window and all graphics, widgets, and texts,");
        buf.append(" are in layers in that single window. The layers are either visible, invisible, or transparent.");
        buf.append(" Using the back button at any time will shutdown the app so you must do it twice for an actual");
        buf.append(" shutdown.");
        buf.append("<p/>");
        buf.append("There are four main buttons: PAUSE, HIDE UI, DEVICE, and ABOUT.");
        buf.append(" There is also the optional URL for playing a media file from a remote location");
        buf.append(" on the Internet, with buttons PLAY and HIDE URL.");
        buf.append(" You can hide all UI widgets by tapging the HIDE UI button; you can show them again");
        buf.append(" by tapping the screen.");
        buf.append(" You can only hide the URL widgets by tapping the HIDE URL button, and you can show them");
        //buf.append(" again by using a long-press touch near the middle of the screen.");
        buf.append(" again by tapping near the middle of the screen.");

        buf.append("<p/>PAUSE: pauses and resumes the spectrogram");

        buf.append("<p/>DEVICE: shows the text with information on the audio capabilities of your device; if the");
        buf.append(" background graphics bother you, then you can hide it by using the HIDE BG button, which");
        buf.append(" is the HIDE UI button that has been re-purposed.");
        buf.append(" To hide the text, then tap the DEVICE button again.");

        buf.append("<p/>ABOUT: shows the text about the application. You can also use the HIDE BG button.");
        buf.append(" To hide the text, then tap the ABOUT button again.");

        buf.append("<p/><h3>URL TO PLAY A MEDIA FILE</h3>");
        buf.append("<p/>This app is great to analyse live sounds and recorded sound files.");
        buf.append("<p/>This app does not record sound but will play most types of recorded files and will display the histogram of the sound as it is played.");
        buf.append(" To do that, one way is to go to a download sound file and to share the file to this app.");
        buf.append(" To share the local sound file, you may long-press on the sound file and you select the share option, which has an icon that looks like three linked dots.");
        buf.append(" When the choices of ways to share comes up, you select the icon for this app.");

        buf.append("<p/>You can also use Chrome, browse the web, and share a remote file directly with this app, as long as the remote file is compatible, or download it to your device and share it.");

        buf.append(" You can also use other sound apps that can share recorded files.");

        //buf.append(" Some web sites do not let their sounds to be shared and many do.");

        buf.append("<p/>This site below contains high quality cetacean sounds that you can download to your device")
        .append(" and then share with this app, by using Android downloaded file access or a file management app from a third party.")
        .append(" You may need to create a free user account at freesound.org in order to enable downloads.")
        .append(" Also, these files can be somewhat large, a few megabytes in size, and therefore it is recommended to download them when on wifi.")

        .append("<p/>http://www.freesound.org/people/aguasonic/sounds/");

        buf.append("<p/>I recommend recordings from aguasonic.");

        //buf.append("<p/>You can also use other sound apps that can share recorded files with this app, often in a similar fashion to using Chrome for sharing.");

//        buf.append("<p/>The URL to play audio files can be used in two ways: you can type (or copy-paste) the URL in the widget or you can");
//        buf.append(" get another app to send an Intent with the appropriate characteristics, and the Spectrogram");
//        buf.append(" app will pick it up and play the URL sent by the third party app.");

//        buf.append("<p/>For copy-pasting a URL to the widget, ")
//        .append("here is an example with the new WHOI Watkins Marine Mammal Sound database at ")

        buf.append("<p/>You may also try the new WHOI Watkins Marine Mammal Sound database at")

        .append("<p/>http://cis.whoi.edu/science/B/whalesounds/bestOf.cfm?code=BD15F")

        .append("<p/>The above link is the page for the Stenella frontalis sounds.");

//        .append("<p/>Steps to play and view the spectrogram:")
//        .append("<br>- Ensure that the device output sound level is not zero and sufficient")
//        .append(", usually with the sound buttons on the side of the device.")
//        .append("<br>- You open the above page with your device Chrome browser,")
//        .append("<br>- you long-press the *Download* link for a recording,")
//        .append("<br>- you select the *Copy link address* option,")
//        .append("<br>- you open or go to the sm Spectrogram app,")
//        .append("<br>- if you are viewing this text, then hide it by tapping the ABOUT button,")
//        .append("<br>- you tap the URL widget and select *Paste*, ")
//        .append("and the link from the clipboard will be written by Android onto the URL widget,")
//        .append("<br>- if there was a previous link in the URL widget, ensure that it is entirely replaced by the new link,")
//        .append("<br>- you tap the *Play* button, wait a few seconds for the file to get accessed and played,")
//        .append(" and you should hear the sound and see the spectrogram.");
//        buf.append("<br>- You may want to pause the app in order to better observe the spectrogram being displayed.");
//        buf.append("<br>- You can replay the file as many times as needed.");

        buf.append("<p/>The images on this NOAA page below are linked to wav files that are played when you select one:")
//        .append(" Using a browser you may copy-paste the link to the wav files into the url text widget in the app.")
//        .append(" One way of doing this is to use Chrome to long-press an image and then select the *Copy link address* option,")
//        .append(" and then go to the app, touch the url text field and paste the link from the clipboard into the url text field.")
//
        .append("<p/>https://swfsc.noaa.gov/textblock.aspx?Division=PRD&ParentMenuId=148&id=5776")
        ;

//        buf.append("<p/>Here is a Google & Chrome-produced list of other sound sources on the web; some of these may not be compatible with this app.");
//
//        buf.append("<p/>https://www.google.ca/webhp?sourceid=chrome-instant&ion=1&espv=2&ie=UTF-8#q=cetacean%20sound%20library");

//        buf.append("<p/><p/>For sharing a file from another app, ")
//        .append("most apps sending Intent instances for audio file sharing should normally be compatible with this app")
//        .append(" and the user only needs to perform the ordinary sharing-file functions of the Android system.");
//
//        buf.append("<p/>For developing an app that can send an Intent for sharing with this app, a code example is:");
//        buf.append("<p/>");
//        buf.append("<tt>");
//        buf.append("// Example:");
//        buf.append("<br>String encoded = null;")
//        .append("<br>try {")
//        .append("<br>&nbsp;&nbsp;encoded = URLEncoder.encode(")
//        .append("<br>&nbsp;&nbsp;&nbsp;&nbsp;\"http://www.wavsource.com/snds_2016-02-14_1408938504723674/animals/dog_whine_duke.wav\",")
//        .append("<br>&nbsp;&nbsp;&nbsp;&nbsp;\"UTF-8\");")
//        .append("<br>} catch (UnsupportedEncodingException e) {")
//        .append("<br>&nbsp;&nbsp;// manage the error here")
//        .append("<br>}");
//        buf.append("<br><br>Intent intent = new Intent(Intent.ACTION_SEND);");
//        buf.append("<br><br>intent.setType(\"application/vnd.sm.app.spectrogram\");");
//        buf.append("<br><br>intent.putExtra(Intent.EXTRA_TEXT, encoded);");
//        buf.append("<br><br>// End of example");
//        buf.append("</tt>");
//        buf.append("<p/><p/>Important: The url in the Intent must be encoded with the URLEncoder in Android, and using the UTF-8 character set.");
        //buf.append("<p/>");
        //We published two simple apps to demonstrate this play-intent feature, TODO future demo apps text

        buf.append("<p/><p/><H3>KNOWN ISSUES</H3>");
        buf.append("<p/>1. After taking a screen shot, the app will restart and the previous image will be lost.")
        .append(" This will be improved in a future version.")
        .append("<p/>2. Re-orienting your device, e.g., from vertical to horizontal, will cause the app to restart the display in order to use the new image dimensions")
        .append("; this is unavoidable currently, therefore take care to keep the orientation stable if you need to avoid losing the current image.")
        ;

        return buf.toString();
    }

    private volatile String ourAppsText = "";

    private String getOurAppsText() {
        if (ourAppsText == null || ourAppsText.length() == 0)
            ourAppsText = "<p/><h2>OUR APPS</h2><p/>"
                    + getString(R.string.our_apps_short_part1)
                    + "<p/>"
                    + AppPublisher.googlePlayPub
                    + "<p/>"
                    + getString(R.string.our_apps_please_note)
                    + "<p/>";
        return ourAppsText;
    }

//    protected void onRestart(){
//        super.onRestart();
//
//        if (TOGGLE_ON_CLICK) {
//            mSystemUiHider.toggle();
//        } else {
//            mSystemUiHider.show();
//        }
//    }

    //Called by android after onCreate, onRestoreInstanceState, onRestart, or onPause.
    @Override
    protected void onResume() {
        super.onResume();
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.THREADS || LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE)
            Log.d(TAG, ".onResume: entering");
        if (isPausedByHUser) {
            //don't restart bg threads
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE || LOG_CONFIG.DEBUG==AcousticLogConfig.THREADS
                    || LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE)
                Log.d(TAG, ".onResume: isPausedByHUser, don't restart " + Thread.currentThread());
        } else {
            //isPausedByHUser = false
            //was not paused by H user, then restart if paused by system
            if (isPaused()) {
                // is paused, listener is null
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE || LOG_CONFIG.DEBUG==AcousticLogConfig.THREADS
                        || LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE)
                    Log.d(TAG, ".onResume: before unpause() "
                            + Thread.currentThread());
                try {
                    //========
                    unpause();
                    //========
//                    pauseButton.setText(getString(R.string.pause_button));
                } catch (Exception e) {
                    if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE || LOG_CONFIG.ERROR==AcousticLogConfig.ON
                            || LOG_CONFIG.DEBUG==AcousticLogConfig.THREADS || LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE)
                        Log.e(TAG, "onResume: unpause() raised " + e + " " + Thread.currentThread());
                }
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE || LOG_CONFIG.DEBUG==AcousticLogConfig.THREADS
                        || LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE)
                    Log.d(TAG, ".onResume: after unpause() "
                            + Thread.currentThread());
            } else {
                // is not paused
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE || LOG_CONFIG.DEBUG==AcousticLogConfig.THREADS
                        || LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE)
                    Log.d(TAG, ".onResume: not paused, do nothing " + Thread.currentThread());
            }
        }

        // restore url from pref done in onCreateComplete
    }

    /**
     * Designed to be done very early in the activity startup step.
     */
    private void restorePreferences() {
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.INIT)
            Log.d(TAG, ".restorePreferences: entering...");

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (LOG_CONFIG.DEBUG==AcousticLogConfig.INIT)
            Log.d(TAG, ".restorePreferences: prefs = " + prefs);

//        SettingsForSoundPreferences.restoreInputSettings(prefs);
          AcousticSettings.restoreFromPreferences(prefs);

//        restoreUrlToPlay(prefs);

        if (LOG_CONFIG.DEBUG==AcousticLogConfig.INIT)
            Log.d(TAG, ".restorePreferences: exiting...");
    }

//    / **
//     * disabled in this version
//     * @ param prefs
//     * /
//    private void restoreUrlToPlay(final SharedPreferences prefs) {
//        fileSizeToPlayFromPref = prefs.getString(PREF_URL_FILE_SIZE_KEY, "");
//
//        fileNameToPlayFromPref = prefs.getString(PREF_URL_FILE_NAME_KEY, "");
//
//        if (LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
//            Log.d(TAG, ".restoreUrlToPlay: fileNameToPlayFromPref {" + fileNameToPlayFromPref
//                    + "} fileSizeToPlayFromPref {"+ fileSizeToPlayFromPref +"}");
//    }

    private void onExceptionAtInit(Throwable ex) {
        String s = "" + ex; //ex.getLocalizedMessage() + "\n";
        if (LOG_CONFIG.ERROR==AcousticLogConfig.ON || LOG_CONFIG.DEBUG==AcousticLogConfig.THREADS) {
            s += "\n" + Log.getStackTraceString(ex);
            Log.e(TAG, ".onExceptionAtInit: " + s + " " + Thread.currentThread());
        }
//        if (urlPrepareSnackbar != null) urlPrepareSnackbar.dismiss();
        showAnomalyText(ex, "Error detected at startup");
    }

    /**
     *
     * @return true when on real device or false when on emulator.
     */
    private boolean isOnRealDeviceOrEmulator() {

        return Acoustic.getIt().isOnRealDevice();
    }

    /**
     * @return false when device does not support sound input
     * @throws Exception
     */
    private boolean setDeviceSoundCapabilities() throws Exception { //TODO washere 2018-5-27

        //includes sound configs kept in DeviceSoundCapabilities
        //============================================================
        //DeviceSoundCapabilities.initFundamentalsForDevice(this,false);
        AcousticDeviceCapabilities.getIt().init(false);
        //============================================================

        if( ! AcousticDeviceCapabilities.getIt().doesSoundInput){

        //if (!DeviceSoundCapabilities.isDeviceCapableOfSoundInput()) {
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.INIT
                    || LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_INPUT_INIT
                    || LOG_CONFIG.DEBUG==AcousticLogConfig.DEVICE_SOUND_CAPABILITIES
                    ) {
                Log.d(TAG, ".setDeviceSoundCapabilities: device cannot do sound input");
                boolean outputOk = AcousticDeviceCapabilities.getIt().doesSoundOutput; //DeviceSoundCapabilities.isDeviceCapableOfSoundOutput();
                Log.d(TAG, ".setDeviceSoundCapabilities: AcousticDeviceCapabilities.getIt().doesSoundOutput is {"+outputOk+"}");
            }
            return false;
        }
        if(LOG_CONFIG.DEBUG==AcousticLogConfig.DEVICE_SOUND_CAPABILITIES){
            Log.d(TAG, ".setDeviceSoundCapabilities: device can do sound input");
            boolean outputOk = AcousticDeviceCapabilities.getIt().doesSoundOutput;
            Log.d(TAG, ".setDeviceSoundCapabilities: AcousticDeviceCapabilities.getIt().doesSoundOutput is {"+outputOk+"}");
        }

        //VoltageSampling vs = DeviceSoundCapabilities.getSelectedVoltageSamplingForInput();

        AcousticSettings.dependentsOnVoltageSamplingRate();

//        SettingsForSoundInput.dependentsOnVoltageSamplingRate(
//                DeviceSoundCapabilities.getSelectedVoltageSamplingForInput());//does not depend on preferences in this version; depends on device capabilities

        //writeInMonitor(DeviceSoundCapabilities.getFundamentalsForDisplay());

        //writeInMonitor(DeviceSoundCapabilities.getForDisplay());

//			writeInMonitor(DeviceSoundCapabilities.getForDisplay(
//			    true,false,false)); //LeafyLog.SOUND_QUALITY_LOG_ENABLED)); //getDeviceAudioCapabilities();//2014-11

        if (LOG_CONFIG.DEBUG==AcousticLogConfig.INIT
                || LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_INPUT_INIT
                || LOG_CONFIG.DEBUG==AcousticLogConfig.DEVICE_SOUND_CAPABILITIES)
            Log.d(TAG, ".setDeviceSoundCapabilities: " + Timestamp.getNanosForDisplay()
                + "; first part of initFundamentalsForDevice completed ok;" +
                "\n xInputHzPerBinFloat = " + AcousticSettings.xInputHzPerBinFloat // SettingsForSoundInput.xInputHzPerBinFloat
                + "\n selectedVspsInputInt = " + AcousticDeviceCapabilities.getIt().selectedVspsOutputInt //DeviceSoundCapabilities.getSelectedVspsInputInt()
                //+ "\n cTimeIncSecDouble = " + Settings.cTimeIncSec
                + "\n MAX_PCM_ADJUSTED_FORMAT = " + AcousticSettings.MAX_PCM_ADJUSTED_FORMAT
                + " is close to the maximum value for a pcm value " +
                "(out of the A/D subsystem, or as input to the D/A subsystem)" +
                ", adjusted to a little below max to" +
                " avoid numerical side effects at, or near, the maximum values (peaks);" +
                " typical value: 0.9 * (Math.pow(2,PCM_BITS_PER_SAMPLE-1)-1) = (2^15 - 1) * 0.9"
//					+ "\n MAX_PCM_ADJUSTED_LONG = "
//					+ Settings.MAX_PCM_ADJUSTED_LONG
            );
        return true;
    }

    /**
     * This adds the prefix "A severe anomaly was detected " to the text param.
     * <p/>
     * The stack trace is not shown in the monitor text but it will be in the
     * details for the email and report file. The stack trace will be added to
     * the monitor text if the email and report file are disabled.
     *
     * @param text e.g., "in <method>: <details>"
     * @param ex TODO not needed? maybe only use in dev mode
     * @return String "A severe anomaly was detected " with text param
     */
    static String getMonitorTextForAnomalyNotif(final String text, final Throwable ex) {
        return "A severe anomaly was detected – " + text
//                + (text != null
//                && (!text.contains("main") && !text.contains("appParent"))
//                ? "; and attribute *appParent* (or *main*) is null"
//                : "")
                //+(ex==null?"":"; "+ex.toString()+" - "+ex.getLocalizedMessage());
                ;
    }

    static String getAnomalyDetailsForEmail(final Throwable ex) {

        final StringBuilder s = new StringBuilder();

        //stack
        if (ex != null) {
            s.append("\n\nStack:\n\n").append(Log.getStackTraceString(ex));
        }

        //device
        s.append("\n\n");
        s.append(OnAnyThread.IT.getDeviceInfoForDisplay());
        s.append("\n");
        s.append(OnAnyThread.IT.getAndroidInfoForDisplay());

        //session
//        s.append("\n\n");
//        s.append(getSessionInfoForAnomalyNotif());

        return s.toString();
    }

    /**
     * if dev email enabled and networked, then try to send email to dev;
     * if dev email not enabled or no network, then write Toast (?).
     * <p/>
     * This adds prefix "A severe anomaly was detected " to the text param.
     * <p/>
     * convenience method.
     *
     * @param text
     */
    public void notifyForAnomaly(final String text) {
        notifyForAnomaly(text, null);
    }

    /**
     * if dev email enabled and networked, then try to send email to dev and write in monitor and console (short msg);
     * <p/>if dev email not enabled or no network, then write in monitor and console (short msg) and to file.
     *
     * <p/>This adds prefix "A severe anomaly was detected " to the text param.
     *
     * @param text
     * @param ex   Throwable, may be null.
     */
    public void notifyForAnomaly(final String text, final Throwable ex) {

        if(LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_QUALITY
                || LOG_CONFIG.ERROR==AcousticLogConfig.ON) {
            Log.e(TAG, "notifyForAnomaly: isSupportEmailEnabled() "
                    + isSupportEmailEnabled()
                    //+"; Settings.anomalyReportToFileIsEnabled "+Settings.anomalyReportToFileIsEnabled
                    + "\n" + text);
        }

        notifyGenericAnomaly(ex);

//        if(SHOW_USER_INIT_EVENTS_ENABLED){
//            showStatusSnackbar(text);
//        }

        final String detailsForEmail = getAnomalyDetailsForEmail(ex);

        final String monitorText = getMonitorTextForAnomalyNotif(text, ex);

        //final String consoleMsg = "Anomaly detected, please consult app messages for details";

        final String all = monitorText + "\n\n" + detailsForEmail;



        TextDisplayWithEmailActivity.show(activity,
                all,
                16.0F,
                getResources().getString(R.string.app_name_short),
                "Severe Anomaly",
                "Severe Anomaly", //this.getResources().getString(R.string.device_sound_capabilities),
                OnAnyThread.IT.isConnected(isSimulatingNoConnection()),
                "" //this.getDeviceOwnerEmailAddress()
        );




        if (isSupportEmailEnabled() 
                && OnAnyThread.IT.isConnected(isSimulatingNoConnection())) {
            //try to send email to dev and write in monitor and console (short msg referring to monitor, app msgs);

            sendEmailToSupport(all);

            //rules for adding the stack trace to monitor
//            if (ex!=null && ! AppConfig.getIt().isSupportEmailEnabled()){ // && ! Settings.anomalyReportToFileIsEnabled) {
//                writeInMonitor(LeafyLog.getStack(ex));
//            }

            return;
        }

        //here no dev email or no network or appParent not set;
        //write in monitor and console (short msg) and to file.
//        if (appParent != null) {
//            appParent.writeInConsoleByApp(consoleMsg);
//            writeInMonitor(monitorText);
//        }
//        writeAnomalyReportToFileNoException(all);

        //rules for adding the stack trace to monitor
//        if (ex!=null && ! Settings.supportEmailEnabled && ! Settings.anomalyReportToFileIsEnabled) {
//            writeInMonitor(LeafyLog.getStack(ex));
//        }
    }



//    /**
//     * Shows anomaly text in text-view when detected, and in a toast.
//     *
//     * @return A BasicListener which is started, or null when failed.
//     */
//    private BasicListener startListener(){
//        BasicListener listener = null;
//        synchronized(LOCK_FOR_LISTENER) {
////            listener = getListener();
//            try {
////                listener.startSoundInput(this);
//                listener = BasicListener.getARunningListener(this);
//                if(LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE)
//                    Log.d(TAG, ".startListener(): *listener.startSoundInput(this)* completed ok.");
//            } catch (Exception e) {
//                if(LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_INPUT_INIT || LOG_CONFIG.ERROR==AcousticLogConfig.ON)
//                    Log.e(TAG,".startListener(): *BasicListener.getARunningListener(this)* raised "+e.getMessage()
//                            +"\n"+DeviceSoundCapabilities.getFundamentalsSummaryForDisplay());
//                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
//                // show error in text view
//                showFailureInMethod(e, "BasicListener.getARunningListener");
//            }
//        } //sync
//        return listener;
//    }


    /**
     * called by the overriding method in the child.
     *
     * @param ev AcousticEvent
     * @return success or failure; the ev object also contains the returnCode attribute.
     */
    public boolean onAcousticEvent(AcousticEvent ev){

        switch(ev.id){

            case AcousticEvent.UNDETERMINED:
                //TODO log or report bug to user and publisher, etc
                ev.returnCode = AcousticEvent.ON_SEVERE_ANOMALY_DETECTED_IN_LIB;
                return false;

            case AcousticEvent.UNKNOWN:
                // up to the child to decide if this situation is an error or not
                // here ev.returnCode == AcousticEvent.UNDETERMINED by default
                return false;

            // when the id is SEVERE_ANOMALY_DETECTED_IN_LIB
            // then the returnCode should be UNDETERMINED here


            case AcousticEvent.ON_NON_SEVERE_ANOMALY_DETECTED_IN_LIB:
                if(ev.ob!=null) {

                    showAnomalyText(null, ev.ob.toString());

                }
                ev.returnCode = AcousticEvent.OK;
                return true;

//            case AcousticEvent.TEXT_FOR_NORMAL_MONITORING:
//                if(ev.ob!=null) {
//                    writeInMonitor(ev.ob.toString());
//                }
//                ev.returnCode = AcousticEvent.OK;
//                return true;

            case AcousticEvent.ON_AUDIOTRACK_INIT_FAILED:
            case AcousticEvent.ON_SEVERE_ANOMALY_DETECTED_IN_LIB:
                if(ev.ob!=null) {
                    //writeInMonitorToShowWithEmailSupport(ev.ob.toString());
                    //TODO washere 2018-07-08
                }
                ev.returnCode = AcousticEvent.OK;
                return true;

            case AcousticEvent.GET_SYSTEM_SERVICE:
                ev.returnedObject = getSystemService(ev.genericString);
                ev.returnCode = AcousticEvent.OK;
                return true;

            case AcousticEvent.GET_TEXT_FOR_DISPLAY_FROM_CLIENT:
                ev.returnedObject = getTextForDisplayFromClient();
                ev.returnCode = AcousticEvent.OK;
                return true;

            case AcousticEvent.ON_NOTIFY_VOLUME:
                notifyVolume();
                ev.returnCode = AcousticEvent.OK;
                return true;

            case AcousticEvent.ON_ERROR:
                onError(ev.genericString);
                ev.returnCode = AcousticEvent.OK;
                return true;

            case AcousticEvent.ON_SHOW_STATUS:
                showStatus(ev.genericString);
                ev.returnCode = AcousticEvent.OK;
                return true;

            case AcousticEvent.GET_ACOUSTIC_CONFIG:
                ev.returnedObject = getAcousticConfigFromClient();
                ev.returnCode = AcousticEvent.OK;
                return true;

            case AcousticEvent.ON_CHANGE_PAUSE:
                if(ev.genericBoolean){
                    enableThePauseButton(null);
                }else{
                    disableThePauseButton(null);
                }
                ev.returnCode = AcousticEvent.OK;
                return true;

//            case AcousticEvent.GET_IS_PAUSE_BUTTON_CHECKED:
//                ev.returnedObject = isPauseButtonChecked();
//                ev.returnCode = AcousticEvent.OK;
//                return true;

            case AcousticEvent.GET_IS_SOUND_INPUT_PAUSED_BY_CLIENT:
                ev.returnedObject = isSoundInputPaused();
                ev.returnCode = AcousticEvent.OK;
                return true;

            case AcousticEvent.ON_PAUSE_SOUND_INPUT://TODO update acousticevent and callers
                ev.returnedObject = onPauseSoundInput(""+ev.ob);
                ev.returnCode = AcousticEvent.OK;
                return true;
        }

//        // cases from Command.executeCommand() etc
//        // ---------------------------------------
//
//        if(ev.id>= CommandEnum.ID_MIN && ev.id<=CommandEnum.ID_MAX){
//            if(ev.ob!=null) {
//                writeInMonitorAndShow(ev.ob.toString());
//            }
//            ev.returnCode = AcousticEvent.OK;
//            return true;
//        }

        //TODO log or report bug to user and publisher, etc
        // here ev.returnCode == AcousticEvent.UNDETERMINED by default
        // the calling child will do it's own ev.id matching

        ev.returnCode = AcousticEvent.ON_ERROR_RETURNED_BY_CALLBACK;
        return false;

    }

    /* *
     * 44,100 vsps may have less latency than 48,000 vsps on many devices
     *
     * @return boolean; when true, 48,000 vsps will be used only if it is the only supported rate
     * above 8000 and we are not on an emulator, i.e., if
     * 44,100 and 96,000 are not available.
     */
//    @ Override
//    public boolean isNativeSampleRateRequested() {
//        return true;
//    }

    /* *
     *
     * ENCODING_PCM_FLOAT: Introduced in API LOLLIPOP, this encoding specifies
     * that the audio sample is a 32 bit IEEE single precision float.
     * The sample can be manipulated as a Java float in a float array,
     * though within a ByteBuffer it is stored in native endian byte order.
     * The nominal range of ENCODING_PCM_FLOAT audio data is [-1.0, 1.0].
     * It is implementation dependent whether the positive maximum of 1.0 is included in the interval.
     * Values outside of the nominal range are clamped before sending to the endpoint device.
     * Beware that the handling of NaN is undefined; subnormals may be treated as zero;
     * and infinities are generally clamped just like other values for AudioTrack
     * – try to avoid infinities because they can easily generate a NaN.
     * To achieve higher audio bit depth than a signed 16 bit integer short
     * it is recommended to use ENCODING_PCM_FLOAT for audio capture, processing, and playback.
     * Floats are efficiently manipulated by modern CPUs,
     * have greater precision than 24 bit signed integers,
     * and have greater dynamic range than 32 bit signed integers.
     * AudioRecord as of API M and AudioTrack as of API LOLLIPOP support ENCODING_PCM_FLOAT.
     *
     * @return boolean; when true, the float encoding will be favored above the byte encoding for
     * a given rate; when false, the byte encoding is favored.
     */
//    @ Override
//    public boolean isEncodingPcmFloatPreferred() {
//        return false;
//    }

    /* *
     * Designed to be used when float encoding is preferred (and in LOLLIPOP+).
     *
     * <p/>AudioRecord as of API M and AudioTrack as of API LOLLIPOP support ENCODING_PCM_FLOAT.
     *
     * <p/>Therefore if the same encoding is requested for input and output, and if float is
     * preferred, then the lowest version of Android that can support that request is M,
     * and if the version is older than M, then the selected audio configs will be PCM 16-Bit
     * for input and output.
     *
     * <p/>Also, if the same encoding is <u>not</u> requested for input and output, and if float is
     * preferred, then the lowest version of Android that can partially support that is LOLLIPOP,
     * and if the version is older than M and at or after LOLLIPOP,
     * then the selected audio config will be PCM FLOAT for output and PCM 16-Bit for input,
     * and if the version is pre-LOLLIPOP, then the selected audio configs will be PCM 16-Bit
     * for input and output. If the version is at M or after, then float will be selected for both
     * inout and output.
     *
     * <p/>If float is <u>not</u> preferred, then 16-bit is selected, for both input and output,
     * irrespectively of the value returned by this method.
     *
     * <p/>This version will favor PCM 16-bit by default if the app does not prefer Float, even
     * if float is possible.
     * This could change in a future version and the default could be float in such a case.
     *
     * <p/><b>Dependencies:</b>
     * <p/>B=both I&O, I=Input, O=Output, Y=Yes, N=No, v=true, .=n/a
     * <pre>
     *     . . O . B B | encoding PCM Float
     *     B B I B . . | encoding PCM 16-Bit
     *     v . . . . . | Pre-Lollipop
     *     . v v v . . | Lollipop to pre-M
     *     . . . v v v | M+
     *     . Y Y N Y Y | encoding PCM Float is preferred (I&O)
     *     . Y N . Y N | same encoding for both I/O requested
     * </pre>
     *
     *
     * @return true when the same encoding is requested for input and output.
     */
//    public boolean isSameEncodingPcmForInputAndOutputRequested() { return false; }

    //AudioFormat.CHANNEL_IN_STEREO
    //AudioFormat.CHANNEL_IN_MONO

    /* *
     * for both channels, in and out.
     *
     * @return true when app is requesting an AudioConfig with mono channels, for input and output.
     */
//    @ Override
//    public boolean isChannelMonoRequested() {
//        return true;
//    }

//    public boolean isMicPreferred(){ return true; }


    /**
     * @return float volume percent
     */
    private float notifyVolume() {
        return 0;
    }


//    public SpectrogramView getSpectrogramView(){
//        return spectrogramView;
//    }

    private final Object LOCK_FOR_LISTENER = new Object();

    private boolean startAcoustic() throws Exception {
        synchronized (LOCK_FOR_LISTENER) {

            try {
                return Acoustic.getIt().start(true);

            } catch (Exception e) {
                disableThePauseButton("Failed");
                showProblemStartingListener();
                if (LOG_CONFIG.ERROR>=AcousticLogConfig.ON)
                    Log.e(TAG, ".startAcoustic: " + e + " " + Log.getStackTraceString(e));
                //throw e;
            }

            return false;
        }

    }

    private void showProblemStartingListener() {
//        if (coordinatorLayout != null) {
//            Snackbar.make(findViewById(android.R.id.content), //coordinatorLayout,
//                     "The sound listener cannot be started", Snackbar.LENGTH_LONG).show();
//        } else {
//            Toast.makeText(this, "The listener failed to start", Toast.LENGTH_LONG).show();
//        }
        Toast.makeText(this, "The sound listener cannot be started",
                Toast.LENGTH_LONG).show();
        // show error in text view
        showFailureInMethod(null, "BasicListener");
    }

    //private final ReentrantLock REL_FOR_LISTENER = new ReentrantLock();

    //private BasicListener listener = null;

    //TODO review washere 2018-6-1 do we need to always create a new listener in this method
    //TODO or can we store the listener in an attribute when it is already created??????
    //TODO this method is often used to _restart_ the listener, not to create a new one

//    /**
//     * Gets a new BasicListener from the Acoustic API.
//     * The new listener is started here.
//     *
//     * @return A new BasicListener which is started here. Is null when failure.
//     * @throws Exception
//     */
//    private BasicListener getListener() throws Exception {
//        synchronized (LOCK_FOR_LISTENER) {
//            try {
//                enableThePauseButton("Pause");
//                if(listener==null) {
//                    listener = Acoustic.getIt().getTheBasicListener(true); //BasicListener.getARunningListener(this);
//                    if (listener == null) {
//                        disableThePauseButton("Failed");
//                        showProblemStartingListener();
//                    }
//                }else {
//                    listener.startSoundInput(this);
//                }
//                return listener;
//            } catch (Exception e) {
//                disableThePauseButton(null);
//                showProblemStartingListener();
//                if (LOG_CONFIG.ERROR>=AcousticLogConfig.ON)
//                    Log.e(TAG, ".getListener: " + e + " " + Log.getStackTraceString(e));
//                throw e;
//            }
//        }//sync.
//    }

//    private final Object LOCK_FOR_LISTENER = new Object();
//    /**
//     * Shows anomaly text in text-view when detected, and in a toast.
//     *
//     * @return A BasicListener which is started, or null when failed.
//     */
//    private BasicListener startListener(){
//        BasicListener listener = null;
//        synchronized(LOCK_FOR_LISTENER) {
////            listener = getListener();
//            try {
////                listener.startSoundInput(this);
//                listener = BasicListener.getARunningListener(this);
//                if(LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE)
//                    Log.d(TAG, ".startListener(): *listener.startSoundInput(this)* completed ok.");
//            } catch (Exception e) {
//                if(LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_INPUT_INIT || LOG_CONFIG.ERROR==AcousticLogConfig.ON)
//                    Log.e(TAG,".startListener(): *BasicListener.getARunningListener(this)* raised "+e.getMessage()
//                            +"\n"+DeviceSoundCapabilities.getFundamentalsSummaryForDisplay());
//                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
//                // show error in text view
//                showFailureInMethod(e, "BasicListener.getARunningListener");
//            }
//        } //sync
//        return listener;
//    }

    private void closeListener() {
        //BasicListener.shutdownTheRunningListener();
        Acoustic.getIt().shutdown();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (LOG_CONFIG.DEBUG!=AcousticLogConfig.OFF) Log.d(TAG, ".onDestroy: entering");
        closeListener();
        AudioPlayer.getIt().onActivityStop();
        // remove notification if any showing
        notifyCancelAll();
        if (LOG_CONFIG.DEBUG!=AcousticLogConfig.OFF) Log.d(TAG, ".onDestroy: exiting");
    }

    @Override
    protected void onRestart() { //matched with onStop
        super.onRestart();
        // start listener if listener not running
        try {
            Acoustic.getIt().restart();
        } catch (Exception e) {
            //done upstream: Log.e(TAG,"Problem with the sound listener: "+e+" "+Log.getStackTraceString(e));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePrefs();
        isPausedByHUser = false;
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.PAUSE
                || LOG_CONFIG.DEBUG==AcousticLogConfig.THREADS
                || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL
                )
            Log.d(TAG, ".onPause: prefs saved; calling closeListener() " + Thread.currentThread());
        closeListener();

        AudioPlayer.getIt().pause(); // shutdown();

//        if (urlPrepareSnackbar != null) urlPrepareSnackbar.dismiss();
    }

    private void savePrefs() {

        // saveUrl(); disabled in this version

        //TODO future more saving e.g. audio settings when user will have some choices

    }

    //disabled in this version
//    private void saveUrl() {//TO DO future maybe also call this when url from intent at onCreate
//        //save url to pref
//        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        SharedPreferences.Editor editor = prefs.edit();
//        editor.putString(PREF_URL_FILE_NAME_KEY, fileNameToPlay);
//        editor.putString(PREF_URL_FILE_SIZE_KEY, fileSizeToPlay);
//        editor.apply();
//        if (LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE
//                || LOG_CONFIG.DEBUG==AcousticLogConfig.SAVE_PREF
//                || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL) {
//            Log.d(TAG, ".saveUrl: fileNameToPlay {" + fileNameToPlay
//                    + "} PREF_URL_FILE_NAME_KEY {" + PREF_URL_FILE_NAME_KEY + "}");
//        }
//    }

    private boolean isActionSendIntent(final Intent intent) {
        final String action = intent.getAction();
        return Intent.ACTION_SEND.equals(action);
    }

    private boolean isInboundIntentOkToPlay(final Intent intent){

        DataFromIntent si = new DataFromIntent(intent);

        if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
            Log.d(TAG,".isInboundIntentOkToPlay: "+si);

        return si.URI != null;
    }

    /*
    ex.: {Intent { act=android.intent.action.SEND
    typ=text/plain
    flg=0x1b080001
    cmp=sm.app.spectrogram/.SpectrogramActivity
    clip={text/plain T:http://sounds.aguasonic.com/files/ag25feb015-from-trk07.wav} (has extras) }}
     */
    private boolean isInboundIntentOkToPlayOld(final Intent intent) {
        clipDataItemText = "";
        final String action = intent.getAction();
        String type = intent.getType();
        if (!Intent.ACTION_SEND.equals(action) || type == null) {
            if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
                Log.d(TAG,".isInboundIntentOkToPlay: returning false; not SEND or type is null");
            return false;
        }
        type = type.toLowerCase();
        if (INBOUND_INTENT_PLAY_TYPE_AUDIO_ANY_ENABLED && type.startsWith(INBOUND_INTENT_PLAY_TYPE_AUDIO_PREFIX))
            return true;
        if (INBOUND_INTENT_PLAY_TYPE_AUDIO_ANY.equals(type) && INBOUND_INTENT_PLAY_TYPE_AUDIO_ANY_ENABLED)
            return true;
        if (INBOUND_INTENT_PLAY_TYPE_AUDIO_WAV.equals(type) && INBOUND_INTENT_PLAY_TYPE_AUDIO_WAV_ENABLED)
            return true;
        if (INBOUND_INTENT_PLAY_TYPE_TEXT.equals(type) && INBOUND_INTENT_PLAY_TYPE_TEXT_ENABLED) {
            // is text and text is enabled
            /*
            ex.: {Intent { act=android.intent.action.SEND
                    typ=text/plain
                    flg=0x1b080001
                    cmp=sm.app.spectrogram/.SpectrogramActivity
                    clip={text/plain
                    T:http://sounds.aguasonic.com/files/ag25feb015-from-trk07.wav} (has extras) }}
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                final ClipData clipData = intent.getClipData();
                ClipData.Item itemAt; String text = null; boolean ok = false; int j = 0;
                final int itemCount = clipData.getItemCount();
                for(int i=0; i<itemCount; i++){

                    itemAt = clipData.getItemAt(i);

                    text = itemAt.getText().toString();

                    j = i+1;

                    if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT
                            || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
                        Log.d(TAG,"isInboundIntentOkToPlay: clipData item "+j+" of "+itemCount+": "
                                +itemAt
                            +"\nText {"+text+"}"
                            +"\nIntent {"+itemAt.getIntent()+"}"
                            +"\nUri {"+itemAt.getUri()+"}"
                            );
                    if(isContainingWavUrl(text)){
                        // this will return the last link if more than one
                        if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT
                                || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
                            Log.d(TAG,"isInboundIntentOkToPlay: clipData item "+j+" of "+itemCount
                                +": clipDataItemText {"+clipDataItemText+"}");
                        clipDataItemText = text;
                        ok = true;
                        //return true; // this will return the first link
                    }
                }// for
                return ok;
            }else{
                // here when text enabled and version too old for clips

                // is there an http .wav link in the intent?

                return isContainingWavUrl(intent.toString());
            }
        }else{
            // here when not text or is text and text is not enabled
        }
        if (INBOUND_INTENT_PLAY_TYPE_SPECIAL.equals(type)
                && INBOUND_INTENT_PLAY_TYPE_SPECIAL_ENABLED)
            return true;

        return false;
    }

    private boolean isContainingWavUrl(String text){
        String s = text.toLowerCase();
        int pos1 = s.indexOf("http");
        int pos2 = s.indexOf(".wav");
        return (pos1>=0 && pos2 > pos1);
    }

//    /**
//     * Mostly immutable TODO move to lib
//     */
//    public final class DataFromIntent {
//
//        private final String TAG = DataFromIntent.class.getSimpleName();
//
//        public final Intent INTENT;
//
//        public final String TEXT;
//
//        public final String ACTION;
//
//        public final String TYPE;
//
//        public final String TYPE_IN_LOWERCASE;
//
//        /**
//         * can be null
//         */
//        public final Uri URI;
//
//        /**
//         * URI.toString() or http(s) url
//         */
//        public final String URL_STRING;
//
//        /**
//         * empty array when no clips (e.g., when older Android)
//         */
//        public final String[] CLIP_DATA_TEXTS;
//
//        public final Intent[] CLIP_DATA_INTENTS;
//
//        public final Uri[] CLIP_DATA_URIS;
//
//        public final String[] CLIP_DATA_HTML_TEXTS;
//
//        public final String[] EXTRA_BUNDLE_KEYS;
//
//        public DataFromIntent(final Intent intent){
//
//            if(intent==null)throw new IllegalArgumentException("param intent is null");
//
//            INTENT = intent;
//
//            TEXT = intent.toString();
//
//            ACTION = intent.getAction();
//
//            TYPE = intent.getType();
//
//            TYPE_IN_LOWERCASE = TYPE!=null?TYPE.toLowerCase():null;
//
//            extractData(intent);
//
//            URI = uriPrivate;
//
//            URL_STRING = uriString;
//
//            CLIP_DATA_TEXTS = clipTexts!=null?clipTexts.toArray(new String[clipTexts.size()]):new String[0];
//
//            CLIP_DATA_INTENTS = clipIntents!=null?clipIntents.toArray(new Intent[clipIntents.size()]):new Intent[0];
//
//            CLIP_DATA_URIS = clipUris!=null?clipUris.toArray(new Uri[clipUris.size()]):new Uri[0];
//
//            CLIP_DATA_HTML_TEXTS = clipHtmls!=null?clipHtmls.toArray(new String[clipHtmls.size()]):new String[0];
//
//            EXTRA_BUNDLE_KEYS = extraBundleKeys!=null?extraBundleKeys.toArray(new String[extraBundleKeys.size()]):new String[0];
//
//        }
//
//        public String toString(){
//            StringBuilder buf = new StringBuilder("Data extracted from Intent: ");
//            buf.append(TEXT);
//            buf.append("\n Action {"+ACTION+"}");
//            buf.append("\n Type {"+TYPE+"}");
//            buf.append("\n Uri {"+URI+"}");
//            buf.append("\n Nb clips = ").append(nbClips);
//            buf.append("\n Nb CLIP_DATA_TEXTS = ");
//            if(CLIP_DATA_TEXTS==null){
//                buf.append("0");
//            }else {
//                buf.append(CLIP_DATA_TEXTS.length);
//            }
//
//            if(EXTRA_BUNDLE_KEYS!=null && EXTRA_BUNDLE_KEYS.length>0) {
//                buf.append("\n "+EXTRA_BUNDLE_KEYS.length+" Extra Bundle keys:");
//                int i =0;
//                for(String s : EXTRA_BUNDLE_KEYS) {
//                    ++i;
//                    buf.append("\n  [").append(i).append("]: ").append(s);
//                    //TODO extra values...
//                }
//            } else {
//                buf.append("\n No Extra Bundle keys");
//            }
//
//            //TODO more ... isHttp...isContent ... protocol? ...
//            return buf.toString();
//        }
//
//        /**
//         * the first Uri in the clips, or null if null.
//         */
//        private Uri uriPrivate = null;
//
//        private String uriString = null;
//
//        private List<String> clipTexts = null;
//
//        private List<Intent> clipIntents = null;
//
//        private List<Uri> clipUris = null;
//
//        private List<String> clipHtmls = null;
//
//        private int nbClips = 0;
//
//        private Set<String> extraBundleKeys = null;
//
//        private void extractData(final Intent intent){
//
//            extractClips(intent);
//
//            uriPrivate = clipUris!=null && ! clipUris.isEmpty() ? clipUris.get(0) : null;
//
//            if(uriPrivate==null){
//                uriPrivate = intent.getData();
//            }
//
//            if(uriPrivate==null){
//                uriString = clipTexts!=null && !clipTexts.isEmpty() ? clipTexts.get(0) : null;
//            }else{
//                uriString = uriPrivate.toString();
//            }
//
//            extractExtras(intent);
//
////            intent.getCategories(); TODO
////
////            intent.getComponent();
////
////            intent.getDataString();
//
//        }
//
//        protected void extractClips(final Intent intent){
//            clipTexts = new LinkedList<>();
//            clipIntents = new LinkedList<>();
//            clipUris = new LinkedList<>();
//            clipHtmls = new LinkedList<>();
//            // this version supports jelly-bean and up
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//                final ClipData clipData = intent.getClipData();
//                if(clipData==null)return;
//                ClipData.Item item;
//                int j = 0;
//                nbClips = clipData.getItemCount();
//                for (int i = 0; i < nbClips; i++) {
//
//                    item = clipData.getItemAt(i);
//
//                    CharSequence cs = item.getText();
//
//                    if(cs!=null) clipTexts.add(cs.toString());
//
//                    if(item.getIntent()!=null)clipIntents.add(item.getIntent());
//
//                    if(item.getUri()!=null)clipUris.add(item.getUri());
//
//                    if(item.getHtmlText()!=null)clipHtmls.add(item.getHtmlText());
//
//                    j = i + 1;
//
//                    if (LOG_CONFIG.DEBUG == AcousticLogConfig.INTENT
//                            || LOG_CONFIG.DEBUG == AcousticLogConfig.PLAY_URL)
//                        Log.d(TAG, "extractClips: clipData item " + j
//                                + " of " + nbClips + ": "
//                                + item
//                                + "\nText {" + item.getText() + "}"
//                                + "\nIntent {" + item.getIntent() + "}"
//                                + "\nUri {" + item.getUri() + "}"
//                        );
////                    if (isContainingWavUrl(text)) {
////                        // this will return the last link if more than one
////                        if (LOG_CONFIG.DEBUG == AcousticLogConfig.INTENT
////                                || LOG_CONFIG.DEBUG == AcousticLogConfig.PLAY_URL)
////                            Log.d(TAG, "isInboundIntentOkToPlay: clipData item " + j + " of " + itemCount
////                                    + ": clipDataItemText {" + clipDataItemText + "}");
////                        clipDataItemText = text;
////                        ok = true;
////                        //return true; // this will return the first link
////                    }
//                }// for
//            }else{
//                // version < JellyBean TODO more prio 2 2017-12-22
//
//                //TODO prio 2 get Uri from extra Bundle or getData or ...
//            }
//        }// end of method extractClips
//
//        public void extractExtras(final Intent intent) {
//
//            Bundle bundle = intent.getExtras();
//
//            if(bundle==null) return;
//
//            extraBundleKeys = bundle.keySet();
//
//            //TODO prio 2 extract values
//
//        }
//
//        /**
//         * Extract the filename and size from the Uri from the Intent and from the content db.
//         *
//         * <p/>The intent contains a Uri for a media file in the content database
//         *
//         * @param context
//         *
//         * @return [0] = filename, [1] = file size in bytes
//         * (use formatShortFileSize(context,bytesString) to format the size bytes for a short display).
//         * Returns ["",""] when Uri from intent is not in content or not in Intent.
//         */
//        public String[] getFilenameAndSize(Context context) {
//            /* Use the file's content URI from the incoming Intent
//             * to query the server app to get the file's display name and size.
//             */
//            if (URI == null) return new String[]{"", ""};
//            Cursor cursor = context.getContentResolver().query(URI, null, null,
//                    null, null);
//            if (cursor == null) return new String[]{"", ""};
//            /*
//             * Get the column indexes of the data in the Cursor,
//             * move to the first row in the Cursor, get the data.
//             */
//            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
//            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
//            cursor.moveToFirst();
//            return new String[]{cursor.getString(nameIndex),
//                    Long.toString(cursor.getLong(sizeIndex))};
//        }
//
//        public boolean uriIsHttp(){
//            if(URI==null)return false;
//            String s = URI.getScheme();
//            return "http".equalsIgnoreCase(s) || "https".equalsIgnoreCase(s);
//        }
//
//        public boolean uriIsContent(){
//            if(URI==null)return false;
//            String s = URI.getScheme();
//            return "content".equalsIgnoreCase(s);
//        }
//
//    }// end of class DataFromIntent


    /**
     * Designed to be called from or during onCreate, which would be called when another app sends
     * an intent that matches the filter(s) defined in the manifest.
     * The listener is started upstream.
     * The player uses callbacks onNormalEndOfPlay and onAnomalyDetectedByPlayer
     *
     * <p/>Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     *
     * <p/>Input:
     * <p/>the Intent that launched the activity
     *
     * <p/>Output:
     * <p/>intentToPlay
     * <p/>fileNameToPlay
     * <p/>fileSizeToPlay
     * <p/>fileSizeToPlayForDisplay
     * <p/>fileTextDisplayed
     * <p/>intentToPlayIsValid
     *
     * @return boolean true when intent is valid; false when no valid intent present or when failure
     */
    private boolean handleIncomingIntent() {
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.INIT
                || LOG_CONFIG.DEBUG==AcousticLogConfig.SOUND_INPUT_INIT
                || LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT
                || LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE
                || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL) {
            Log.d(TAG, ".handleIncomingIntent: entering");
        }

        intentToPlayIsValid = false;
        fileNameToPlay = "";
        fileSizeToPlay = "";
        fileSizeToPlayForDisplay = "";
        fileTextDisplayed = "";

        try {
            // get the intent that started this activity, normally one containing a URL to play

            intentToPlay = getIntent();

            /*
            {Intent { act=android.intent.action.SEND cat=[android.intent.category.DEFAULT]
            typ=audio/x-wav
            flg=0x1b080001
            cmp=sm.app.spectro/.SpectrogramActivity
            clip={audio/x-wav
            U:content://com.android.providers.downloads.documents/document/6959} (has extras) }}
             */

            // ex.:
            // {Intent { act=android.intent.action.SEND typ=text/plain
            // flg=0x13080001
            // cmp=sm.app.spectrogram/.SpectrogramActivity
            // clip={text/uri-list
            // U:content://com.android.chrome.FileProvider/...} (has extras) }

            // ex.:
            // {Intent { act=android.intent.action.SEND typ=text/plain flg=0x1b080001
            // cmp=sm.app.spectrogram/.SpectrogramActivity
            // clip={text/plain
            // T:http://sounds.aguasonic.com/files/ag25feb015-from-trk07.wav} (has extras) }}

            dataFromIntent = new DataFromIntent(intentToPlay);

            if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
                Log.d(TAG,".handleIncomingIntent: "+dataFromIntent);

            if(dataFromIntent.URI == null && dataFromIntent.URL_STRING == null){

                // the intent does not contain a Uri;
                // could be normal start without an play intent or invalid send or play intent
                // so don't notify user

                if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT
                        || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
                    Log.d(TAG, ".handleIncomingIntent: not valid intent to play {"
                            + intentToPlay + "}");

                if (isActionSendIntent(intentToPlay)) {
                    // the start intent was sent by external app, and it's invalid
//                    if (coordinatorLayout != null)
//                        Snackbar.make(findViewById(android.R.id.content), //coordinatorLayout,
//                                "The URL from an external app is invalid and is ignored",
//                                Snackbar.LENGTH_LONG).show();
                    Toast.makeText(this,
                            "The URL from an external app is invalid and is ignored",
                            Toast.LENGTH_LONG).show();
                } else {
                    // the start intent is not a send intent, so ignore,
                    // probably normal start intent
                }

                return intentToPlayIsValid; // false here
            }

            // here when intent may be ok, the type is ok for media to play
            // and the function is enabled
            // ex.: "application/vnd.sm.app.spectrogram" or "text/plain"
            // extract data from intent

            if(dataFromIntent.uriIsContent()){

                //Uri {content://com.android.providers.downloads.documents/document/6959}

                String[] pair = dataFromIntent.getFilenameAndSize(this);

                if (LOG_CONFIG.DEBUG == AcousticLogConfig.INTENT
                        || LOG_CONFIG.DEBUG == AcousticLogConfig.PLAY_URL)
                    Log.d(TAG, ".handleIncomingIntent: OnAnyThread.getFileNameAndSizeFromContentDb returned "
                            + Arrays.toString(pair)
                    );

                fileNameToPlay = pair[0];
                fileSizeToPlay = pair[1];

                if (fileNameToPlay.isEmpty()) {
                    invalidIntent(intentToPlay, fileNameToPlay);
                    return intentToPlayIsValid; // is false
                }

                fileSizeToPlayForDisplay = OnAnyThread.formatShortFileSize(this, fileSizeToPlay);

                setUrlToPlayInUi();

                if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT
                        || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
                    Log.d(TAG, ".handleIncomingIntent: possibly valid intent to play;" +
                            " calling play(\n intent {" + intentToPlay
                            + "})\n fileTextDisplayed {"+fileTextDisplayed
                            + "}");

                // ======================================
                intentToPlayIsValid = play(intentToPlay);
                // ======================================

            } else {

                // URI is not content scheme, could be http scheme or file scheme or other
                // ex.: http.s file
                fileNameToPlay = dataFromIntent.URL_STRING;
                fileSizeToPlay = "";
                fileSizeToPlayForDisplay = "";
                fileTextDisplayed = fileNameToPlay;

                setUrlToPlayInUi();

                if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT
                        || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
                    Log.d(TAG, ".handleIncomingIntent: possibly valid intent to play;" +
                            " calling play(\n dfi.URL_STRING {" + dataFromIntent.URL_STRING
                            + "})");

                // ===================================================
                intentToPlayIsValid = play(dataFromIntent.URL_STRING);
                // ===================================================
            }

            if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT
                    || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
                Log.d(TAG,".handleIncomingIntent: play(intent or dfi.URL_STRING) returned "
                        +intentToPlayIsValid
                        +"; intent = "+intentToPlay);

            if (intentToPlayIsValid) {
                // ok

            } else {
                // failed;
                // the url from the intent is invalid

                // what about the previous intent that was valid? ignored in this version

                // this version does not keep intent data in preferences

                invalidIntent(intentToPlay, fileNameToPlay);

            }
            return intentToPlayIsValid;

        } catch (Throwable e) {
            intentToPlayIsValid = false;
            try {
                if (LOG_CONFIG.ERROR == AcousticLogConfig.ON)
                    Log.e(TAG, ".handleIncomingIntent: " + e
                            + "; " + Log.getStackTraceString(e));
                invalidIntent(intentToPlay, fileNameToPlay);
                setUrlToPlayInUi();
                notifyPlayAbnormalEnd(e);
            }catch (Throwable ignored){
            }
            return intentToPlayIsValid;
        }
    }

    /**
     * After calling this, and the Uri is not null,
     * then the client may get the url in String form by calling
     * {@code uri.toString()}
     *
     * @param intent Intent
     * @return A Uri from the intent; can be null.
     */
    private Uri getUri(final Intent intent) throws UnsupportedEncodingException {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (uri == null) {
            //extra-stream gives null, then try getData
            uri = intent.getData();
        }
        return uri;
    }


    /**
     * Informs the user of the invalidity of the intent.
     *
     * @param intent
     * @param filename
     */
    private void invalidIntent(final Intent intent, final String filename) {
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT
                || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
            Log.d(TAG, ".invalidIntent: invalid intent {" + intent + "}");
        String x = filename == null || filename.isEmpty() ? "" : ", filename {" + filename+"}";
        String s = "The Intent is invalid" + x;//from another app ???
        if (intent != null) {
            // url for a local file
            s = s+" Intent: "+intent;
        }
//        if (coordinatorLayout != null) {
//            Snackbar.make(findViewById(android.R.id.content), //coordinatorLayout,
//                    s, Snackbar.LENGTH_LONG).show();
//        }
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
        notifyPlayAbnormalEnd(new Exception(s));
    }

    private void showError(final String text) {
        if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT
                || LOG_CONFIG.DEBUG==AcousticLogConfig.ON
                || LOG_CONFIG.ERROR==AcousticLogConfig.ON)
            Log.e(TAG, ".showError {" + text + "}");
//        if (coordinatorLayout != null) {
//            Snackbar.make(findViewById(android.R.id.content), //coordinatorLayout,
//                    text, Snackbar.LENGTH_LONG).show();
//        }
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    /**
     * @param ex     Throwable
     * @param method a text fragment, example: "mediaPlayer.prepare"
     */
    private void showFailureInMethod(final Throwable ex, final String method) {
        //if (urlPrepareSnackbar != null) urlPrepareSnackbar.dismiss();
        showAnomalyText(ex, method + " raised " + ex);
    }

    //TO DO when is this reset to null after it is set to non-null?
    private volatile String previousAnomalyText = null;
    private volatile boolean lastAnomalyTextIsShown = false;

    /**
     * Designed to be used in runOnUiThread.
     */
    private final Runnable RUNNABLE_TO_SHOW_ANOMALY_TEXT = new Runnable() {

        public void run() {
            if (LOG_CONFIG.ERROR!=AcousticLogConfig.OFF)
                Log.e(SpectrogramActivity.TAG, fromHtmlToString(getLastAnomalyTextInHtml()));

            notifyForAnomaly("Anomaly detected",lastThrowable);

            if (about != null) {
//                if(lastAnomalyText==null || lastAnomalyText.isEmpty()){
//                    about. setText("About");//TO DO use res
//                }else {
//                    about. setText("Error");//TO DO use res
//                }
                aboutButtonSelected();
                afterButtonSelected(about);
//                if(!about.performClick()){
//                    Log.e(SpectrogramActivity.TAG,
//                            "secondary issue: *about.performClick()* returned false; " +
//                                    "the primary issue was: "+ getLastAnomalyTextForHtml());
//                }
            }
        }
    };


    /**
     * @param e    Throwable, may be null.
     * @param text String
     */
    private void showAnomalyText(final Throwable e, final String text) {
        lastThrowable = e;
        lastAnomalyText = text;
        lastAnomalyTimeMillis = System.currentTimeMillis();
        runOnUiThread(RUNNABLE_TO_SHOW_ANOMALY_TEXT);
    }

    private void clearLastAnomaly() {
        lastThrowable = null;
        lastAnomalyText = null;
        lastAnomalyTimeMillis = -1L;
    }

    private final Runnable RUNNABLE_TO_CLEAR_ANOMALY_TEXT = new Runnable() {

        public void run() {
            updateAboutButtonOnUIThread();
        }
    };

    private void updateAboutButtonOnUIThread() {
        if (lastAnomalyText == null || lastAnomalyText.length() == 0) {
            if (about != null) {
                about.setText("About");//TODO use res
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                    Log.d(TAG,"updateAboutButtonOnUIThread: about button label set to *About*");
            }
        } else {
            if (about != null) {
                about.setText("Error");//TODO use res
                if (LOG_CONFIG.DEBUG==AcousticLogConfig.UI)
                    Log.d(TAG,"updateAboutButtonOnUIThread: about button label set to *Error*");
            }
        }
    }

    private void clearAnomalyText() {
        previousAnomalyText = lastAnomalyText;
        lastAnomalyText = null;
        lastAnomalyTextIsShown = false;
        runOnUiThread(RUNNABLE_TO_CLEAR_ANOMALY_TEXT);
    }


    private final Runnable RUNNABLE_FOR_PLAYER_STARTING_TO_PLAY = new Runnable() {

        public void run() {

//            if (urlPrepareSnackbar != null) urlPrepareSnackbar.dismiss();

            notifyPlayStart();
        }
    };

    @Override
    public void onStartingToPlay() {
        runOnUiThread(RUNNABLE_FOR_PLAYER_STARTING_TO_PLAY);
    }

    volatile String textForEndOfPlay = "Normal end of play";

    private final Runnable RUNNABLE_FOR_PLAYER_NORMAL_END = new Runnable() {

        public void run() {
            notifyPlayNormalEnd();
            //if (urlPrepareSnackbar != null) urlPrepareSnackbar.dismiss();
//            if(coordinatorLayout==null)return;
//            Snackbar.make(findViewById(android.R.id.content), //coordinatorLayout,
//                    textForEndOfPlay,
//                    Snackbar.LENGTH_LONG).show();
            Toast.makeText(SpectrogramActivity.this, textForEndOfPlay, Toast.LENGTH_LONG).show();
            resetUrlGui();
            clearAnomalyText();
        }
    };

    @Override
    public void onNormalEndOfPlay( boolean audioFocusIsLost) {
        textForEndOfPlay = "The sound file has ended normally";
        if(audioFocusIsLost){
            textForEndOfPlay = "Playback ended due to loss of audio focus";
        }
        runOnUiThread(RUNNABLE_FOR_PLAYER_NORMAL_END);
    }

    private Activity activity = this;

    private final Runnable RUNNABLE_FOR_ANOMALY_IN_PLAYER = new Runnable() {

        public void run() {
            if(editTextUrlToPlay==null)return;
            notifyPlayAbnormalEnd(throwableFromPlayer);
            doCancelUrl();
//            if (urlPrepareSnackbar != null) urlPrepareSnackbar.dismiss();
            editTextUrlToPlay.setTextColor(Color.RED);

            showAnomalyText(throwableFromPlayer, errorMessageFromPlayer);

//            if (coordinatorLayout != null)
//                Snackbar.make(findViewById(android.R.id.content), //coordinatorLayout,
//                        errorMessageFromPlayer, Snackbar.LENGTH_LONG)
//                        .show();
            Toast.makeText(SpectrogramActivity.this, errorMessageFromPlayer,
                    Toast.LENGTH_LONG).show();
        }
    };

    private volatile Throwable throwableFromPlayer = null;
    private volatile String errorMessageFromPlayer = null;

    private volatile boolean isCancelling = false;

    /**
     * Called by the AudioPlayer.
     *
     * @param e            may be null
     * @param errorMessage String
     */
    @Override
    public void onAnomalyDetectedByPlayer(final Throwable e, final String errorMessage) {
        if(isCancelling){
            return;
        }
        throwableFromPlayer = e;
        errorMessageFromPlayer = errorMessage + (e != null ? ": " + e : "");
        runOnUiThread(RUNNABLE_FOR_ANOMALY_IN_PLAYER);
    }

    private final Runnable RUNNABLE_FOR_AUDIO_FOCUS_REFUSED = new Runnable() {

        public void run() {

            notifyPlayAbnormalEnd(new Exception("Audio focus was refused by the system"));

            doCancelUrl();
//            if (urlPrepareSnackbar != null) urlPrepareSnackbar.dismiss();
            if(editTextUrlToPlay!=null)
                editTextUrlToPlay.setTextColor(Color.GRAY);
            String s = "Audio focus refused by Android; try again later";
            //showAnomalyText(null, s);
//            if (coordinatorLayout != null)
//                Snackbar.make(findViewById(android.R.id.content), //coordinatorLayout,
//                        s, Snackbar.LENGTH_LONG)
//                        .show();
            Toast.makeText(SpectrogramActivity.this, s, Toast.LENGTH_LONG).show();
        }
    };

    @Override
    public void onAudioFocusRefused(){
        runOnUiThread(RUNNABLE_FOR_AUDIO_FOCUS_REFUSED);
    }

    @Override
    public Context getContextForSmartPlayer(){
        return this;
    }


    // generic notification =========================================

    /**
     * disabled until notifications are used better, based on G best practices,
     * such as with intents and action choices for user to see or do something with the
     * notifications.
     */
    public static final boolean NOTIF_ARE_ENABLED = false;

    /**
     * Removes all the notifications from the UI.
     *
     * Typical usage: called by onDestroy.
     */
    private void notifyCancelAll(){
        NotificationManager notificationManager = (NotificationManager)getSystemService(
                Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    /**
     * TODO future add intent for user to show the long anomaly text
     *
     * @param e
     */
    private void notifyGenericAnomaly(Throwable e){
        if(!NOTIF_ARE_ENABLED)return;
        notifySimple(
                e,
                "Anomaly detected",
                AppContext.getAppName(),
                R.drawable.ic_stat_error
        );
    }

    // url play notifications ============

    /**
     * disabled in this version, does nothing.
     */
    private void notifyPlayStart(){
        if(!NOTIF_ARE_ENABLED)return;
        notifySimple(
                null,
                "Sound file paying",
                AppContext.getAppName(),
                R.drawable.ic_stat_playing
        );
    }

    private void notifyPlayNormalEnd(){
        if(!NOTIF_ARE_ENABLED)return;
        notifySimple(
                null,
                "Sound file ended normally",
                AppContext.getAppName(),
                R.drawable.ic_stat_recording_ended_normally
        );
    }

    private void notifyPlayAbnormalEnd(Throwable e){
        if(!NOTIF_ARE_ENABLED)return;
        notifySimple(
                e,
                "Sound file ended abnormally",
                AppContext.getAppName(),
                R.drawable.ic_stat_error
        );
    }

    private void notifyPlayFailedToStart(Throwable e){
        if(!NOTIF_ARE_ENABLED)return;
        notifySimple(
                e,
                "Sound file failed to start playing",
                AppContext.getAppName(),
                R.drawable.ic_stat_error
        );
    }

    private static final String NOTIF_CHANNEL_SM_SPECTRO_NAME = "SM-SPECTRO-20171202";
    /**
     * An identifier for a notification unique within this application.
     * TODO do we need multiple IDs?
     */
    private static final int NOTIF_ID =20171202;
    private static final String NOTIF_CHANNEL_SM_SPECTRO_NAME_FOR_USER = "sm Spectro";

    /**
     * TODO could be moved to lib, with two new params: channel and id; and maybe also Context,
     * and maybe also importance attrib.
     *
     * @param e Throwable, may be null.
     * @param title shown as-is.
     * @param text the error message is appended to it when e is not null.
     * @param iconId not used when e is not null, then ndroid.R.drawable.stat_notify_error is used.
     */
    private void notifySimple(Throwable e, String title, String text, int iconId ) {
        if(!NOTIF_ARE_ENABLED)return;
        NotificationCompat.Builder b =
                new NotificationCompat.Builder(this, NOTIF_CHANNEL_SM_SPECTRO_NAME);

        b.setAutoCancel(true);//.setDefaults(Notification.DEFAULT_ALL);

        if (e == null) {
            b.setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(iconId)
            .setSound(null); // TODO does not seem to stop sound
        }
        else {
            b.setContentTitle(title)
                    .setContentText(text+" - "+e.getMessage())
                    .setSmallIcon(android.R.drawable.stat_notify_error);
        }

        NotificationManager mgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                mgr.getNotificationChannel(NOTIF_CHANNEL_SM_SPECTRO_NAME)==null) {
            // version Oreo+
            mgr.createNotificationChannel(new NotificationChannel(NOTIF_CHANNEL_SM_SPECTRO_NAME,
                    NOTIF_CHANNEL_SM_SPECTRO_NAME_FOR_USER,
                    NotificationManager.IMPORTANCE_LOW));//LOW may not use sound
        }

        mgr.notify(NOTIF_ID, b.build());
    }

    // ======================= end of notification methods =============================



    private long previousBackButtonMs = 0;
    public static final long BACK_BUTTON_TIMEOUT_MS = 5000;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            //Log.d(this.getClass().getName(), "back button pressed");
            String s = "";
            if (contentTextView.isShown()) {
                s = "; or tap the button to hide the text";
            }
            if (previousBackButtonMs > 0) {
                //there is a previous back button press time; this is the second back-key
                if (System.currentTimeMillis() - previousBackButtonMs > BACK_BUTTON_TIMEOUT_MS) {
                    // timeout exceeded, reset previous one; no exit
                    if(LOG_CONFIG.DEBUG!=AcousticLogConfig.OFF) Log.d(TAG,".onKeyDown: timeout exceeded, reset previous one; no exit");
                    previousBackButtonMs = System.currentTimeMillis();
                    //if a text is showing, add  s = "; to hide the text, touch the text button"
                    //if(coordinatorLayout!=null) {
//                        Snackbar.make(findViewById(android.R.id.content), //coordinatorLayout,
//                                "Please press the Back key again to exit" + s,
//                                Snackbar.LENGTH_LONG)
//                                .show();
                    //}
                    Toast.makeText(this, "Please press the Back key again to exit" + s,
                            Toast.LENGTH_LONG).show();
                    // eat the action and do not propagate it
                    return true;
                } else {
                    // timeout not exceeded, then exit; propagate the action
                    if (LOG_CONFIG.DEBUG!=AcousticLogConfig.OFF) Log.d(TAG,".onKeyDown: timeout not exceeded, then exit; propagate the action");
                    shutdown(true);
                }
            } else {
                // there is no previous back button press time; no exit TODO washere bug 2018-4-17 snackbar fails maybe internal error
                if(LOG_CONFIG.DEBUG!=AcousticLogConfig.OFF) Log.d(TAG,".onKeyDown: no previous back button press time; no exit");
                previousBackButtonMs = System.currentTimeMillis();
                //if(coordinatorLayout!=null) {
                /*
                E/InputEventSender: Exception dispatching finished signal.
E/MessageQueue-JNI: Exception in MessageQueue callback: handleReceiveCallback
E/MessageQueue-JNI: android.view.InflateException: Binary XML file line #41: Error inflating class <unknown>
                        at android.view.LayoutInflater.createView(LayoutInflater.java:626)
                 */
                s = "Please press the Back key again to exit" + s;

//                Snackbar.make(findViewById(android.R.id.content), //coordinatorLayout,
//                        "Please press the Back key again to exit" + s,
//                        Snackbar.LENGTH_LONG)
//                        .show();

                Toast.makeText(this,s,Toast.LENGTH_LONG).show();

                //}
                // eat the action and do not propagate it
                return true;
            }
        }
        if(LOG_CONFIG.DEBUG!=AcousticLogConfig.OFF) Log.d(TAG,".onKeyDown: calling *super.onKeyDown(keyCode, event)*");
        return super.onKeyDown(keyCode, event);
    }

//    /* *
//     * Disable app restart and the loss of data (url) when config changes,
//     * in coordination with THE manifest.xml file.
//     *
//     * @ param config
//     */
//    @ Override
//    public void onConfigurationChanged(Configuration config){
//        super.onConfigurationChanged(config);
//
////        restoreUrlToPlay();
////        if(editTextUrlToPlay!=null)
////            editTextUrlToPlay.setText(urlToPlayString);
////        if(LOG_CONFIG.DEBUG==AcousticLogConfig.RESTORE){
////            Log.d(TAG,".onConfigurationChanged: urlToPlayString {"+urlToPlayString+"}");
////        }
//    }


    private boolean isDevMode() {
        return getAcousticConfigFromClient().isDevMode;
    }


    private boolean isSupportEmailEnabled() {
        return getAcousticConfigFromClient().isSupportEmailEnabled;
    }

//
//    public boolean isAdsCapable() {
//        return false;
//    }

//
//    public boolean isShowAdsWhenDonated() {
//        return false;
//    }
//
//
//    public boolean isDonationsCapable() {
//        return false;
//    }
//
//
//    public boolean isUseTestPurchase() {
//        return false;
//    }
//
//
//    public boolean isUseEmptyIabPayload() {
//        return false;
//    }


    private boolean isSimulatingNoConnection() {

        return getAcousticConfigFromClient().isSimulatingNoConnection;

    }

    /**
     * Designed to show status updates from library logic.
     *
     * @param givenStatusText
     */
    public void showStatusSnackbar(final String givenStatusText) {//TODO 2017-7-1 add param isIndefinite and OK action with listener to dismiss it
        statusText = givenStatusText;
        runOnUiThread(RUNNABLE_FOR_STATUS);
    }

    private void showStatus(String s) {
//        Toast.makeText(this,s,
//                Toast.LENGTH_LONG).show();
        statusText = s;
        runOnUiThread(RUNNABLE_FOR_STATUS);
    }

//
//    public boolean isSimulatingNoPurchase() {
//        return false;
//    }

    @Override
    public void onStart() {
        super.onStart();

//        if(APP_INDEXING_IS_ENABLED) {//TO DO future enable
//
//            // ATTENTION: This was auto-generated to implement the App Indexing API.
//            // See https://g.co/AppIndexing/AndroidStudio for more information.
//            client.connect();
//            Action viewAction = Action.newAction(
//                    Action.TYPE_VIEW,
//                    "Spectrogram Page", // a title for the content shown.
//                    // If you have web page content that matches this app activity's content,
//                    // make sure this auto-generated web page URL is correct.
//                    // Otherwise, set the URL to null.
//                    //Uri.parse("http://host/path"),
//                    null,
//                    // TO DO: Make sure this auto-generated app URL is correct.
//                    Uri.parse("android-app://sm.app.spectrogram/http/host/path")
//            );
//            AppIndex.AppIndexApi.start(client, viewAction);
//        }
    }

    /**
     * called for finish, onStop
     */
    private void shutdown( final boolean withFinish ){
        if (LOG_CONFIG.DEBUG > AcousticLogConfig.OFF) Log.d(TAG, ".shutdown: entering withFinish = "+withFinish);
        //the player is also shutdown in onPause
        AudioPlayer.getIt().onActivityStop();
        if(withFinish){
            finish();
        }
        if (LOG_CONFIG.DEBUG > AcousticLogConfig.OFF) Log.d(TAG, ".shutdown: exiting");
    }

    @Override
    public void onStop() {
        if (LOG_CONFIG.DEBUG > AcousticLogConfig.OFF) Log.d(TAG,".onStop: entering");

        shutdown(false);

        super.onStop();

//        if(APP_INDEXING_IS_ENABLED) { // TO DO future enable
//
//            // ATTENTION: This was auto-generated to implement the App Indexing API.
//            // See https://g.co/AppIndexing/AndroidStudio for more information.
//            Action viewAction = Action.newAction(
//                    Action.TYPE_VIEW, // TO DO: choose an action type.
//                    "Spectrogram Page", // TO DO: Define a title for the content shown.
//                    // TO DO: If you have web page content that matches this app activity's content,
//                    // make sure this auto-generated web page URL is correct.
//                    // Otherwise, set the URL to null.
//                    null, // Uri.parse("http://host/path"),
//                    // TO DO: Make sure this auto-generated app URL is correct.
//                    Uri.parse("android-app://sm.app.spectrogram/http/host/path")
//            );
//            AppIndex.AppIndexApi.end(client, viewAction);
//            client.disconnect();
//        }c
    }



} // end of class

    //===================== archive ==================================


//    /**
//     * Not used in this version.
//     *
//     * @param urlString
//     */
//    private void invalidUrl(final String urlString) {
//        if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT
//                || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
//            Log.d(TAG, ".invalidUrl: invalid URL text {" + urlString + "}");
//        if (coordinatorLayout != null)
//            Snackbar.make(findViewById(android.R.id.content), //coordinatorLayout,
//                    "The URL is invalid and is ignored: " + urlString,
//                    Snackbar.LENGTH_LONG).show();
//        notifyPlayAbnormalEnd(new Exception("The URL is invalid {"+urlString+"}"));
//    }

//    /**
//     * Also sets attributes urlToPlayUri (can be null), urlToPlayRaw (can be null),
//     * and urlToPlayDecoded (can be empty).
//     *
//     * <p/>For an intent with a plain text URL, the returned value is not empty
//     * and urlToPlayUri is null.
//     *
//     * @param intent
//     * @return urlToPlayDecoded, can be empty
//     * @throws UnsupportedEncodingException
//     */
//    private String getUrlToPlay(final Intent intent) throws UnsupportedEncodingException {
//        urlToPlayRaw = null;
//        urlToPlayUri = null;
//        urlToPlayDecoded = "";
//        //TO DO was here was here spectro b u g this does not work for extra_stream for type AUDIO_*
//        urlToPlayRaw = intent.getStringExtra(Intent.EXTRA_TEXT);
//        if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT)
//            Log.d(TAG, ".handleIncomingIntent: urlToPlayRaw from Intent.EXTRA_TEXT {"
//                    + urlToPlayRaw + "}");
//        if (urlToPlayRaw == null) {
//            //no text url, try extra_stream
//            urlToPlayUri = getUri(intent);
//
//            if (urlToPlayUri != null) {
//                // url is uri, from extra stream or getData
//
//                urlToPlayRaw = urlToPlayUri.toString();
//            } else {
//                //urlToPlayRaw is null; urlToPlayUri is null
//            }
//        } else {
//            //urlToPlayRaw is not null; urlToPlayUri is null
//        }
//        if (urlToPlayRaw != null) {
//            urlToPlayDecoded = URLDecoder.decode(urlToPlayRaw, "UTF-8");
//        }
//        return urlToPlayDecoded;
//    }

    //=======================================================

//    /**
//     * Not used in this version.
//     *
//     * @param fd
//     * @param intent
//     * @return true when possible success, and false when definite failure.
//     *
//     * @deprecated in this version
//     */
//    private boolean play(final FileDescriptor fd, final Intent intent) {
//        player = new SmartPlayer();
//        try {
//            showUrlPrepareSnackbar();
//            player.play(fd, this);
//        } catch (Throwable ex) {
//            Log.e(TAG, ".play(FileDescriptor) " + ex + " " + Log.getStackTraceString(ex));
//            doResetUrl(true);
//            //showAnomalyText(ex, ex.getMessage());
//            invalidIntent(intent, null);// uses a snackbar
//            return false;
//        }
//        setUrlGuiWhenPlaying();
//        return true;
//    }

//    private boolean play(final Intent intent, final String urlToPlayGiven){
//        if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT)
//            Log.d(TAG, ".play: trying to play intent for remote file {" + intent
//                    + "} urlToPlayGiven {"+urlToPlayGiven+"}");
//        //urlToPlayString = urlToPlayDecoded;
//        editTextUrlToPlay.setText(urlToPlayGiven);
//        // ======================
//        return play(intent);
//        // ======================
//    }

//    /**
//     * Not used in this version.
//     *
//     * @param intent            an Intent referencing a remote file with a text URL
//     * @param urlToPlayRawGiven for a remote file
//     * @return true when successful, false otherwise
//     * @throws UnsupportedEncodingException
//     */
//    private boolean playRemoteFile(final Intent intent, final String urlToPlayRawGiven)
//            throws UnsupportedEncodingException {
//
//        urlToPlayDecoded = URLDecoder.decode(urlToPlayRawGiven, "UTF-8");
//        if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT)
//            Log.d(TAG, ".playRemoteFile: url is text; urlToPlayDecoded {"
//                    + urlToPlayDecoded + "}");
//
//        if (urlToPlayDecoded != null && !urlToPlayDecoded.isEmpty()) {
//            // external url or uri to play may be ok, try it
//            if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT || LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
//                Log.e(TAG, ".playRemoteFile: trying to play intent with text URL to remote file {"
//                        + intent + "}");
//            urlToPlayString = urlToPlayDecoded;
//            editTextUrlToPlay.setText(urlToPlayString);
//            // ======================
//            return play(intent);
//            // ======================
//        } else {
//            // external url or uri is not ok, ignore it
//            invalidIntent(intent, urlToPlayRawGiven);
//            return false;
//        }
//    }

//    /**
//     * Not used in this version.
//     * <p/>
//     * see http://developer.android.com/reference/android/media/MediaPlayer.html#setDataSource(java.lang.String)
//     *
//     * @param intent   used for error notification; may be null for manual file entry
//     * @param uriGiven Uri, such as from {@code uri = intent.getData();}; may be null
//     * @param filepath String, for log and messages to user
//     * @return true when successful, false otherwise
//     *
//     * @deprecated
//     */
//    private boolean playLocalFile(final Intent intent, final Uri uriGiven, final String filepath) {
//        if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT)
//            Log.d(TAG, ".playLocalFile: entering with filepath {" + filepath + "}");
//
////      Try to open the file for "read" access using the
////      returned URI. If the file isn't found, write to the error log and return.
//
//        FileDescriptor fd = getFileDescriptorForPrivateFileWithRetries(filepath);
//
//        if (fd == null) {
//            if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT)
//                Log.d(TAG, "playLocalFile: File cannot be opened {" + filepath + "}");
//            showError("File cannot be opened");
//            return false;
//        }
//
//        if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT)
//            Log.d(TAG, "playLocalFile: File opened!!! {" + filepath + "} descriptor {" + fd + "}");
//
//        boolean success = false;
//        try {
//            // =========================
//            success = play(fd, intent); // ===> mediaPlayer.setDataSource(fd)
//            // =========================
//        } finally {
////            try {
////                pfd.close();
////            } catch (IOException ignore) {
////                if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT||LOG_CONFIG.ERROR==AcousticLogConfig.ON)
////                    Log.e(TAG, "playLocalFile: File found: {"+filepath+"}, descriptor {"+fd+"}"
////                    +" IOException "+ignore+" "+Log.getStackTraceString(ignore));
////            }
//        }
//        return success;
//    }

//    /**
//     * @param filepath without file separator
//     * @return FileDescriptor or null
//     */
//    private FileDescriptor getFileDescriptorForPrivateFile(final String filepath) {
//        FileDescriptor fd = null;
//        try {
//            fd = openFileInput(filepath).getFD();
//        } catch (IllegalArgumentException e) {
//            if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT || LOG_CONFIG.ERROR==AcousticLogConfig.ON)
//                Log.e(TAG, "getFileDescriptorForPrivateFile: File {" + filepath + "}"
//                        + " IOException " + e + " " + Log.getStackTraceString(e));
//            //showError("File cannot be opened");
//            return null;
//        } catch (IOException e) {
//            if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT || LOG_CONFIG.ERROR==AcousticLogConfig.ON)
//                Log.e(TAG, "getFileDescriptorForPrivateFile: File {" + filepath + "}"
//                        + " IOException " + e + " " + Log.getStackTraceString(e));
//            //showError("File cannot be opened");
//            return null;
//        }
//        return fd;
//    }

    /*
    if (movieurl.startsWith("rtsp://")) {
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(movieurl));
    startActivity(intent);
}
     */

//    private FileDescriptor getFileDescriptorForPrivateFileWithRetries(final String filepath) {
//        FileDescriptor fd = getFileDescriptorForPrivateFile(filepath);
//        if (fd == null) {
//            //remove prefix
//            if (filepath.startsWith("file://") || filepath.startsWith("rtsp://")) {
//                String absolute = filepath.substring(7);
//                fd = getFileDescriptorForPrivateFile(absolute);
//            }
//        }
//        return fd;
//    }

//    /**
//     * not used not completed
//     *
//     * @param intent
//     * @return FileDescriptor
//     */
//    private FileDescriptor getFileDescriptorWithContentResolverAndInputStream(final Intent intent) {
//        InputStream input = null;
//        try {
//            input = getContentResolver().openInputStream(intent.getData());
//        } catch (FileNotFoundException e) {
//            showError("File not found");
//            return null;
//        }
//        FileDescriptor fd = null; //TO DO
//        return fd;
//    }

//    /**
//     * <p>Not used in this version.</p>
//     *
//     * @param uriGiven Uri
//     * @param filepath String
//     * @return FileDescriptor
//     */
//    private FileDescriptor getFileDescriptorWithContentResolverAndPfd(final Uri uriGiven,
//                                                                      final String filepath) {
//        Uri uri = uriGiven;
//        if (uri == null) {
//            uri = Uri.parse(filepath);
//            if (LOG_CONFIG.DEBUG==AcousticLogConfig.INTENT)
//                Log.d(TAG, ".getFileDescriptorWithContentResolverAndPfd: " +
//                        "Uri.parse(filepath) returned uri {" + uri + "}");
//        }
//
//        ParcelFileDescriptor pfd = null;
//        try {
//            // Get the content resolver instance for this context, and use it
//            // to get a ParcelFileDescriptor for the file.
//            pfd = getContentResolver().openFileDescriptor(uri, "r");
//        } catch (FileNotFoundException e) {
//            showError("File not found");//TO DO was here 2016-4-18 does not work
//            return null;
//        }
//        // Get a regular file descriptor for the file
//        return pfd.getFileDescriptor();
//    }

    //=======================================================

//    //send fake screen touches to prevent cpu from slowing down
//    TimerTask sendFakeTouch = new TimerTask(){
//        public void run(){
//            Instrumentation instrumentation = new Instrumentation();
//            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACKSLASH);
//        }
//    };
//
//    /**
//     * Sends fake screen touches to prevent cpu from slowing down
//     * TO DO b u g make sure that it does not write into an edittext view
//     */
//    private void tempHackForCpuAtFullSpeed() {
//        Timer touchTimer = new Timer();
//        touchTimer.schedule(sendFakeTouch, 1000, 1000);
//    }



//    private boolean doPlayUrlString(final String urlString){//TO DO use to test 2017-7-26 when using the Play button
//        if (LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL)
//            Log.d(TAG, ".doPlayUrlString: entering with urlString {" + urlString + "}");
//        try {
//            Intent intent = makeIntentFromString(urlString); //Intent.parseUri(urlString,Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//            return play(intent);
//
//        } catch (Throwable ex) {
//            //e.printStackTrace();
//            if(LOG_CONFIG.ERROR==AcousticLogConfig.ON)
//                Log.e(TAG, ".doPlayUrlString(String {"+urlString+"}) " + ex + " "
//                        + Log.getStackTraceString(ex));
//        }
//
//        return false;
//    }

    /* this works from download app:
     *
     * 07-26 13:52:35.141 15987-15987/sm.app.spectro D/SmartPlayer: .play(Intent,Callback) entering:
     * Intent {Intent {
     * act=android.intent.action.SEND
     * cat=[android.intent.category.DEFAULT]
     * typ=audio/mpeg
     * flg=0x1b080001
     * cmp=sm.app.spectro/.SpectrogramActivity
     * clip={audio/mpeg U:content://com.android.providers.downloads.documents/document/5834}
     * (has extras) }}
     *
     * this does not work from my app:
     *
     * 07-26 14:45:34.472 29210-29210/sm.app.spectro D/SmartPlayer: .play(Intent,Callback) entering:
     * Intent {Intent {
     * act=android.intent.action.VIEW
     * cat=[android.intent.category.DEFAULT]
     * typ=audio/*
     * (has extras) }}
     *
     *
     * Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
     *
     */

//    private Intent makeIntentFromString(final String urlString){
//        final Intent intent = new Intent(Intent.ACTION_VIEW); //progressive download
//
//        String errorText = "";
//        String s = null;
//        try {
//            s = URLEncoder.encode(urlString, "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//            errorText = "UnsupportedEncodingException "+e.getMessage();
//            throw new IllegalArgumentException(errorText,e);
//        }
//
//        intent.addCategory("android.intent.category.DEFAULT");
//
//        intent.setType(INBOUND_INTENT_PLAY_TYPE_AUDIO_ANY);
//                //"application/vnd.sm.sm.app.seadragon.sm.app.seadragon.v21.spectrogram");
//
//        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //0x1b080001);
//
//        intent.putExtra(Intent.EXTRA_TEXT, s);
//
//        Uri uri = Uri.parse(urlString);
//
//        intent.putExtra(Intent.EXTRA_STREAM, uri); //TO DO 2017-7-26 stream must be a parcellable extra
//
//        intent.setDataAndType(uri,INBOUND_INTENT_PLAY_TYPE_AUDIO_ANY);
//
//        /*
//        TO DO 2017-7-26
//             * cmp=sm.app.spectro/.SpectrogramActivity
//     * clip={audio/mpeg U:content://com.android.providers.downloads.documents/document/5834}
//
//         */
//
//        return intent;
//    }

//    private volatile boolean permissionRequestedForStorageAccess = false;
//
//    /**
//     * Called when the Play button is selected and the URL is to be started or restarted,
//     * not resumed,
//     * i.e., when urlWasPaused is false.
//     * In some cases, the URL has been entered manually instead of coming from an intent.
//     *
//     * <P/>Starts a new thread to prepare and play the given URL, then shows a Snackbar.
//     *
//     * <p/>Used in this version.
//     *
//     * <p/>Designed to be run on the ui thread.
//     * All callers are run on the ui thread in this version.
//     *
//     * @param urlString referencing a remote audio or media file, or a local file;
//     *                  the value is from url edittext view.
//     * @return true when possible success; false when failure.
//     * The returned value is not used in this version of the app.
//     */
//    private boolean doPlayUrl(final String urlString) {
//        isCancelling = false;
//        if (LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL
//                || LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS)
//            Log.d(TAG, ".play(String url): entering with urlString {" + urlString
//                    //+"} permissionGrantedForStorageAccess = "+permissionGrantedForStorageAccess
//                    + "}, SOUND_TO_PLAY_IS_ENABLED = " + SOUND_TO_PLAY_IS_ENABLED
//                    + ", doesSoundOutput = " + doesSoundOutput);
//
//        if (urlString == null) return false;
//        final String urlStrg = urlString.trim();
//        if (urlStrg.isEmpty()) return false;
//        if (!SOUND_TO_PLAY_IS_ENABLED) return false;
//        if (!doesSoundOutput) return false;
//        final String exampleUrl = getString(R.string.url_example);
//        if (urlStrg.equalsIgnoreCase(exampleUrl)) return false;
//
//        // catch local file such as file://...
//        // and don't try if file storage permission access not granted
//
//        if (isLocalFile(urlStrg)) {
//            // is local file, check external storage access permission
//            if (LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL
//                    || LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS)
//                Log.d(TAG, ".play: is local file {" + urlStrg + "}; get access permission...");
//
//            if ( ! getPermissionForExternalStorageAccess(urlStrg)) {
//                // external storage access permission was definitively denied
//                // or permission is being requested, don't play
//                if (LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL
//                        || LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS)
//                    Log.d(TAG, ".play: access denied or being requested...");
//                return false;
//            }
//            // local file access granted, then play it
//            if (LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL
//                    || LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS)
//                Log.d(TAG, ".play: access granted, try to play local file");
//        } else {
//            // not local file, then remote file, try to play
//            if (LOG_CONFIG.DEBUG==AcousticLogConfig.PLAY_URL
//                    || LOG_CONFIG.DEBUG==AcousticLogConfig.PERMISSIONS)
//                Log.d(TAG, ".play: not local file, try to play remote file");
//        }
//
//        // try to play url
//
//        return playLocalFileAfterStorageAccessGranted(urlStrg);
//    }

//    /**
//     * NOT Used in this version.
//     *
//     * <p/>Designed to be run on the ui thread.
//     * All callers are run on the ui thread in this version.
//     *
//     * @param filePath
//     * @return true when started, false when attempt failed.
//     */
//    private boolean playLocalFileAfterStorageAccessGranted(final String filePath) {// TO DO 2017-7-26 called twice !!!  b u g
//        if (LOG_CONFIG.DEBUG==LogConfig.PLAY_URL)
//            Log.d(TAG, ".playLocalFileAfterStorageAccessGranted: entering with filePath {" + filePath + "}");
//
//        //=================================================
//        boolean started = playRemoteOrLocalFile(filePath);
//                            //doPlayUrlString(filePath);//TO DO 2017-7-26 testing
//        //=================================================
//
//        if (LOG_CONFIG.DEBUG==LogConfig.PLAY_URL || LOG_CONFIG.DEBUG==LogConfig.PERMISSIONS)
//            Log.d(TAG, ".playLocalFileAfterStorageAccessGranted(String): "
//                    //+"playRemoteOrLocalFile(urlString) returned "
//                    +"doPlayUrlString returned "
//                    + started
//                    + " with param {" + filePath + "}");
//
//        if (started) setUrlGuiWhenPlaying();
//
//        return started;
//    }
//
//    private boolean isLocalFile(final String urlOrPath) {
//        return (urlOrPath.startsWith("file://")
//                || urlOrPath.startsWith("/")
//                || urlOrPath.startsWith("content://")
//        );
//    }

//    /**
//     * Not just for http, can also be local file path.
//     *
//     * <p/>NOT Used in this version.
//     *
//     * <p/>Designed to be run on the ui thread.
//     * All callers are run on the ui thread in this version.
//     *
//     * @param urlOrFilePath
//     * @return true is success to start play
//     */
//    private boolean playRemoteOrLocalFile(final String urlOrFilePath) {
//        try {
//            if (player == null) {
//                player = new SmartPlayer(this);//TO DO review 2017-7-26
//            }
//            showUrlPrepareSnackbar();
//            player.play(urlOrFilePath);//throws IllegalArgumentException
//        } catch (Throwable ex) {
//            if(LOG_CONFIG.DEBUG==LogConfig.PLAY_URL)
//                Log.d(TAG,".playRemoteOrLocalFile: player.play(urlOrFilePath, this) raised "+ex);
//            doResetUrl(true);
//            //showAnomalyText(ex,ex.getMessage());
//            invalidUrl(urlOrFilePath);//uses a snackbar
//            return false;
//        }
//        setUrlGuiWhenPlaying();
//        return true;
//    }

//    /**
//     * Not used in this version.
//     *
//     * @param filepath
//     * @return boolean success or failure
//     */
//    private boolean playLocalFile(final String filepath) {
//        isCancelling = false;
//        try {
//            if (player == null) {
//                player = new SmartPlayer(this);
//            }
//            showUrlPrepareSnackbar();
//            player.play(filepath);//throws IllegalArgumentException
//        } catch (Throwable ex) {
//            doResetUrl(true);
//            invalidUrl(filepath);//uses a snackbar
//            return false;
//        }
//        setUrlGuiWhenPlaying();
//        return true;
//    }

//    /**
//     * Not used in this version.
//     *
//     * @param path
//     * @return boolean
//     */
//    private boolean playWithoutIntent(final String path) {
//        if (path.startsWith("file://")) {
//            //uri for a local file
//
//            return playLocalFile(path);
//
//        } else {
//            //uri is for not a local file; may be for a remote file such as http://... or https://
//
//            if (path.startsWith("http") || path.startsWith("rtsp")) {
//                //uri for a remote file
//
//                return playRemoteOrLocalFile(path);
//            }
//        }
//
//        return false;
//    }

//    /**
//     * Not used in this version.
//     *
//     * @param path
//     * @return boolean
//     */
//    private boolean play(final String path) {
//        if (LOG_CONFIG.DEBUG==LogConfig.INTENT)
//            Log.d(TAG, ".play(String): path {" + path + "}");
//
//        Uri uri = Uri.parse(path);
//        Intent intent = new Intent(Intent.ACTION_SEND, uri);
//
//        return play(intent);
//    }