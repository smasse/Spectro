/*
 * Copyright 2016-2017 Serge Masse
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * It is the wish of the copyright holder that this software not be used
 * for the purpose of killing, harming, or capturing animals.
 * The use of this software with captive cetaceans and other large mammals
 * in captivity is discouraged.
 */
package sm.app.spectro;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
//import android.app.Instrumentation;
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
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

//import com.google.android.gms.appindexing.Action;
//import com.google.android.gms.appindexing.AppIndex;
//import com.google.android.gms.common.api.GoogleApiClient;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
//import java.util.Timer;
//import java.util.TimerTask;

import sm.lib.acoustic.sound.EmitterGrandParent;
import sm.lib.acoustic.sound.SoundClientInterface;
import sm.lib.acoustic.sound.SoundClientPreferences;
import sm.lib.acoustic.sound.forandroid.DeviceSoundCapabilities;
import sm.lib.acoustic.sound.forandroid.SettingsForSoundPreferences;
import sm.lib.acoustic.sound.forandroid.input.BasicListener;
import sm.lib.acoustic.sound.forandroid.input.SettingsForSoundInput;
import sm.lib.acoustic.sound.forandroid.input.SettingsForSoundInputDisplay;
import sm.lib.acoustic.sound.forandroid.input.spectrogram.SpectrogramView;
import sm.lib.acoustic.sound.forandroid.output.SmartPlayer;
import sm.lib.acoustic.sound.forandroid.quality.SoundQualitySettings;
import sm.leafy.util.forandroid.AppContext;
import sm.leafy.util.forandroid.AppPublisher;
import sm.leafy.util.forandroid.OnAnyThread;
import sm.leafy.util.forandroid.Timestamp;

//TODO 2017-6-3
/*
Window window = activity.getWindow();
window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
window.setStatusBarColor(ContextCompat.getColor(activity, R.color.example_color));

android:statusBarColor = @android:color/transparent

    <!-- Make the status bar traslucent -->
    <style name="AppTheme" parent="AppTheme.Base">
        <item name="android:windowTranslucentStatus">true</item>
    </style>
 */

//TODO washere bug no display of sound bins on tablet 2017-5-6

//also takes a long time to start on a tablet (Nexus 9) - white screen for many seconds
// then black screen with gui but no drawing for many seconds
// then cursor moving but no display as if mic no working
/* app frosen when paused and attempt to restart failed:
05-06 16:32:04.805 5601-5601/sm.app.spectro I/Choreographer: Skipped 595 frames!  The application may be doing too much work on its main thread.
05-06 16:32:04.831 5601-5601/sm.app.spectro D/SettingsForSoundOutput: dependentsOnVoltageSamplingRate(VoltageSampling vs): entering with vs = sm.lib.acoustic.sound.VoltageSampling@bdfa87c
05-06 16:32:04.834 5601-5601/sm.app.spectro D/SettingsForSoundOutput: dependentsOnVoltageSamplingRate(vs): xOutputVSPerFS = 1200.0
                                                                       = vs.RATE_PER_SEC_FLOAT / (float) getXHzSamplingRate().PER_SEC;
                                                                       vs.RATE_PER_SEC_FLOAT = 48000.0
                                                                       getXHzSamplingRate().PER_SEC = 40
...
05-06 16:37:52.835 11068-11068/sm.app.spectro D/SettingsForSoundOutput: dependentsOnVoltageSamplingRate(VoltageSampling vs): entering with vs = sm.lib.acoustic.sound.VoltageSampling@bdfa87c
05-06 16:37:52.835 11068-11068/sm.app.spectro D/SettingsForSoundOutput: dependentsOnVoltageSamplingRate(vs): xOutputVSPerFS = 1200.0
                                                                         = vs.RATE_PER_SEC_FLOAT / (float) getXHzSamplingRate().PER_SEC;
                                                                         vs.RATE_PER_SEC_FLOAT = 48000.0
                                                                         getXHzSamplingRate().PER_SEC = 40
TODO 05-06 16:37:54.848 11068-11081/sm.app.spectro W/AudioSystem: ioConfigChanged() closing unknown input 1350

TODO maybe app audio input source = android default, not mic
 */


//TODO washere washere list of sources of ceta sounds; to add in about text
/* TODO activity with text containing URLs of ceta sounds
TODO in prominence in play store text

Great to analyse live sounds and recorded sound files.
This app does not record sound but will play most types of recorded files and will display the histogram of the sound as it is played.
To do that, one way is to go to the sound file using the Chrome browser and to share the sound to this app.
To share the sound file, you open the pop-up on the sound file link and you select the share option, which has an icon that looks like three linked dots.
When the choices of ways to share comes up, you select the icon for this app.

Some web sites do not let their sounds to be shared.

You can also use other sound apps that can share recorded files with this app, often in a similar fashion to using Chrome for sharing.
 */
/**
 *
 * sm Spectrogram app
 *
 * <p/>COPYRIGHT (C) 2015-2017 Serge Masse
 *
 * <p/>
 * It is the wish of the author that this software not be used
 * for the purpose of killing, harming, or capturing animals.
 * The use of this software with captive cetaceans and
 * other captive large mammals is discouraged.
 *
 * <p/>SmartPlayer is used to play recordings.
 * <pre>SmartPlayer contains public interface Callback {
 void onStartingToPlay();
 void onNormalEndOfPlay(boolean audioFocusIsLost);// or when audio focus is lost
 void onAnomalyDetectedByPlayer(Throwable e, String errorMessage);
 void onAudioFocusRefused();
 Context getContextForSmartPlayer();
 }</pre>
 *
 * @author Serge Masse
 *
 * <!-- https://developer.android.com/studio/publish/index.html -->
 */
public class SpectrogramActivity extends Activity implements SoundClientInterface,
        SmartPlayer.Callback {

    private static final String TAG = SpectrogramActivity.class.getSimpleName();

    public static volatile SpectrogramActivity main = null;

    //private volatile BasicListener listener = null;

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

    private final float ALPHA_DARK = 0.45f;
    private final float ALPHA_NOT_SET = 0.55f;
    private final float ALPHA_NEUTRAL = 0.65f;
    private final float ALPHA_SET = 0.75f;
    private final float ALPHA_LIGHT = 0.85f;

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
    boolean urlIsPlaying = false;
    boolean urlShown = true;
    boolean urlIsPaused = false;

    /**
     * decoded, used to display in edit text
     */
    volatile String urlToPlay = "";
    String urlToPlayFromPref = "";
    /**
     * from external source, before decoding, possibly url-encoded
     */
    String urlToPlayRaw = "";
    /**
     * from external source, after decoding
     */
    String urlToPlayDecoded = "";

    Uri urlToPlayUri = null;

    // use the color that comes with the edittext
    int urlColorForInactive = (Color.GRAY & 0x00FFFFFF) | 0x80000000;
    int urlColorForActive = (Color.GREEN & 0x00FFFFFF) | 0x80000000;
    int urlColorForPaused = (Color.YELLOW & 0x00FFFFFF) | 0x80000000;
    int urlColorForError = (Color.RED & 0x00FFFFFF) | 0x80000000;
    int editTextUrlToPlayTextInitialColor = urlColorForInactive;

    boolean textColorBrighter = false;
    final int textColorTransparent = 0x8833b5e5;
    //designed to be bright blue on a black background
    final int textColorNotTransparent = 0xFF33b5e5;

    SpectrogramView spectrogramView;
    LinearLayout largeGuiLayout;
    LinearLayout buttonsLayout;
    TextView contentTextView;
    Button pause;
    Button hideGui;
    Button device;
    Button about;
    Button ourApps;
    Button emailDev;
    EditText editTextUrlToPlay;
    Button playUrlButton;
    Button hideUrlButton;
    LinearLayout urlLayout;

    Snackbar urlPrepareSnackbar;

    /*
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.WAKE_LOCK" /> not dangerous
    <uses-permission android:name="android.permission.INTERNET" /> not dangerous
    */
    private static final int PERMISSIONS_REQUEST_CODE_FOR_RECORD_AUDIO = 0;

    private static String[] PERMISSIONS_FOR_RECORD_AUDIO = {
            Manifest.permission.RECORD_AUDIO
    };

    // Storage Permissions
    private static final int PERMISSIONS_REQUEST_CODE_FOR_STORAGE_ACCESS = 1;

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
     * to feed onCreateComplete.
     */
    private volatile Bundle savedInstanceStateTemp = null;

    public static final String INBOUND_INTENT_PLAY_TYPE_SPECIAL = "application/vnd.sm.app.spectrogram";
    public static final String INBOUND_INTENT_PLAY_TYPE_TEXT = "text/plain";
    public static final String INBOUND_INTENT_PLAY_TYPE_AUDIO_WAV = "audio/wav";
    public static final String INBOUND_INTENT_PLAY_TYPE_AUDIO_PREFIX = "audio/";
    public static final String INBOUND_INTENT_PLAY_TYPE_AUDIO_ANY = "audio/*";

    // use these with SOUND_TO_PLAY_IS_ENABLED

    public static final boolean INBOUND_INTENT_PLAY_TYPE_SPECIAL_ENABLED = true;
    public static final boolean INBOUND_INTENT_PLAY_TYPE_TEXT_ENABLED = true;
    public static final boolean INBOUND_INTENT_PLAY_TYPE_AUDIO_WAV_ENABLED = true;
    public static final boolean INBOUND_INTENT_PLAY_TYPE_AUDIO_ANY_ENABLED = true;

    public static final boolean INBOUND_INTENT_PLAY_RAW_URL_ENABLED = true;

    //play sound file from intent from other app or from url entered by user;
    //an intent from other app causes this app to start or restart

    volatile SmartPlayer player = null;
    boolean doesSoundInput = true;
    boolean doesSoundOutput = true;

    /* *
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
//    private GoogleApiClient client;

    private final boolean APP_INDEXING_IS_ENABLED = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isTaskRoot()) {
            // Android launched another instance of the root activity into an existing task
            //  so just quietly finish and go away, dropping the user back into the activity
            //  at the top of the stack (ie: the last state of this task)
            /* related to android bug(s)
            http://code.google.com/p/android/issues/detail?id=2373
            http://code.google.com/p/android/issues/detail?id=26658
             */
            if (AppConfig.LOG_ENABLED || AppConfig.INIT_LOG_ENABLED
                    || AppConfig.RESTORE_LOG_ENABLED || AppConfig.ERROR_LOG_ENABLED)
                Log.e(TAG, ".onCreate: finishing because not TaskRoot");

            finish();
            return;
        }

        main = this;
        AppContext.activityContext = this;

        /* for transparent/translucent app/status bar
        Window window = this.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }
        */

//        tempHackForCpuAtFullSpeed();

//        if(APP_INDEXING_IS_ENABLED) {
//            // ATTENTION: This was auto-generated to implement the App Indexing API.
//            // See https://g.co/AppIndexing/AndroidStudio for more information.
//            client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
//        }

        if (AppConfig.getIt().isDevMode()
                && (AppConfig.INIT_LOG_ENABLED
                    || AppConfig.THREADS_LOG_ENABLED
                    || AppConfig.SOUND_INPUT_INIT_LOG_ENABLED)
                || AppConfig.LOG_INTENT)
            Log.d(TAG, ".onCreate: entering..." +
                    "app name {" + AppContext.getAppName()
                    + "} version name {" + AppContext.getVersionName()
                    + "} AppPublisher.emailAddress {" + AppPublisher.emailAddressForSupport
                    + "} Support email subject from AppConfig: "
                    + AppConfig.getIt().getSupportEmailSubject()
                    + "\nAppConfig (for de. mode): " + AppConfig.getIt().forDisplay()
                    + "\n" + Thread.currentThread()
            );

        if(USER_LOG_INIT){
            showStatusSnackbar("Getting permissions");
        }
        getPermissionForRecordAudio(savedInstanceState);
    }

    /**
     * Called on ui thread after initial permissions are processed.
     *
     * @param savedInstanceState
     */
    protected void onCreateComplete(final Bundle savedInstanceState) {
        try {
            if (AppConfig.LOG_ENABLED || AppConfig.INIT_LOG_ENABLED
                    || AppConfig.SETTINGS_CHANGED_LOG_ENABLED
                    || AppConfig.SOUND_INPUT_INIT_LOG_ENABLED
                    || AppConfig.LOG_INTENT
                    || AppConfig.RESTORE_LOG_ENABLED
                    || AppConfig.PERMISSIONS_LOG_ENABLED)
                Log.d(TAG, ".onCreateComplete: entering...");

            setOnRealDeviceOrEmulator();

            if(USER_LOG_INIT){
                showStatusSnackbar("Restoring preferences");
            }

            restorePreferences();

            doesSoundInput = setDeviceSoundCapabilities();
            doesSoundOutput = DeviceSoundCapabilities.isDeviceCapableOfSoundOutput();

            if(USER_LOG_INIT){
                String soundInputSupported = doesSoundInput?"supported":"not supported";
                String soundOutputSupported = doesSoundOutput?"supported":"not supported";;
                showStatusSnackbar("Sound input "+soundInputSupported
                        +", output "+soundOutputSupported);
            }

            initUI();

            if (!doesSoundInput) {
                // device does not support sound input
                if (AppConfig.SOUND_INPUT_INIT_LOG_ENABLED || AppConfig.INIT_LOG_ENABLED)
                    Log.d(TAG, ".onCreateComplete: device does not support sound input");

                disableThePauseButton(null);

                Toast.makeText(this,
                        "This device does not support sound input; this app will not work properly.",
                        Toast.LENGTH_LONG).show();
            } else {
                // does support sound input
                if(USER_LOG_INIT){
                    showStatusSnackbar("Starting the listener");
                }
                BasicListener listener = getListener();

                if (listener != null) {

                    // the listener is started
                    if(USER_LOG_INIT){
                        showStatusSnackbar("Listener started");
                    }
                    //done upstream: enableThePauseButton("Pause");

                    initUrlToPlay(savedInstanceState, urlToPlayFromPref);
                } else {
                    // the listener failed to start
                    if(USER_LOG_INIT){
                        showStatusSnackbar("Listener failed to start");
                    }
                    //done upstream: disableThePauseButton(null);
                }
            }
        } catch (Exception ex) {
            if(AppConfig.ERROR_LOG_ENABLED)Log.e(TAG,".onCreateComplete: "+ex);
            disableThePauseButton(null);
            onExceptionAtInit(ex);//TODO washere 2016-11 does this work???
        } finally {
            if (AppConfig.INIT_LOG_ENABLED
                    || AppConfig.SOUND_INPUT_INIT_LOG_ENABLED
                    || AppConfig.LOG_INTENT
                    || AppConfig.PERMISSIONS_LOG_ENABLED
                    || AppConfig.PLAY_URL_LOG_ENABLED)
                Log.d(TAG, ".onCreateComplete: exiting...");
        }
    }

    private volatile String pauseButtonLabel = null;

    private Runnable RUNNABLE_TO_ENABLE_THE_PAUSE_BUTTON = new Runnable() {
        @Override
        public void run() {
            pause.setOnClickListener(ON_CLICK_LISTENER);
            pause.setAlpha(ALPHA_NEUTRAL);
            if (pauseButtonLabel != null) {
                pause.setText(pauseButtonLabel);
            }
        }
    };

    private Runnable RUNNABLE_TO_DISABLE_THE_PAUSE_BUTTON = new Runnable() {
        @Override
        public void run() {
            if(pause==null)return;
            pause.setOnClickListener(null);
            pause.setClickable(false);
            pause.setAlpha(ALPHA_DARK);
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
     * sound to play from intent from another app; or URL from previous session in urlToPlay.
     *
     * <p/>Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     *
     * @param savedInstanceState
     */
    private void initUrlToPlay(final Bundle savedInstanceState, final String urlFromPref) {

        if (!SOUND_TO_PLAY_IS_ENABLED) return;

        if (!doesSoundOutput) return;

        if (savedInstanceState == null) {
            // starting new session, use url from intent (if any) or from previous session
            // this method does nothing if the start intent has no valid url to play
            if (!handleIncomingIntent(urlFromPref)) {
                // no valid intent, then try url from previous session
            } else {
                // valid intent; was handled by handleIncomingIntent
            }
        } else {
            // restoring session (not with intent), use url from previous session, if any
            setUrlToPlayInUi(urlFromPref);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void initUI() {

        setContentView(R.layout.activity_spectrogram);

        spectrogramView = (SpectrogramView) findViewById(R.id.spectrogram2);

        spectrogramView.setColorForSecMarker(SpectrogramView.COLOR_GREEN_TRANSPARENT);
        spectrogramView.setColorForFreqMarker(SpectrogramView.COLOR_GREEN_TRANSPARENT);

        //spectrogram2_textview_title is defined in the layout xml file
        if (!TITLE_TEXTVIEW_ENABLED) {
            TextView titleTextView = (TextView) findViewById(R.id.spectrogram2_textview_title);
            titleTextView.setVisibility(View.INVISIBLE);
        }

        largeGuiLayout = (LinearLayout) findViewById(R.id.spectrogram2_gui);
        if (AppConfig.UI_LOG_ENABLED)
            Log.d(TAG, "initUI: largeGuiLayout sensitivity is disabled");
        largeGuiLayout.setOnClickListener(ON_CLICK_LISTENER);
        largeGuiLayout.setClickable(true);

        buttonsLayout = (LinearLayout) findViewById(R.id.spectrogram2_buttons);

        hideGui = (Button) findViewById(R.id.spectrogram2_hide);
        hideGui.setOnClickListener(ON_CLICK_LISTENER);
        hideGui.setClickable(true);
        hideGui.setAlpha(ALPHA_NOT_SET);
        hideBgIsSet = false;

        pause = (Button) findViewById(R.id.spectrogram2_pause);
        enableThePauseButton(null);
//        pause.setOnClickListener(ON_CLICK_LISTENER);
//        pause.setClickable(true);
//        pause.setAlpha(ALPHA_NOT_SET);
//        pause.setText("Pause");

        device = (Button) findViewById(R.id.spectrogram2_device);
        device.setOnClickListener(ON_CLICK_LISTENER);
        device.setClickable(true);
        device.setAlpha(ALPHA_NOT_SET);
        deviceShown = false;

        about = (Button) findViewById(R.id.spectrogram2_about);
        about.setOnClickListener(ON_CLICK_LISTENER);
        about.setClickable(true);
        about.setAlpha(ALPHA_NOT_SET);
        aboutShown = false;

        contentTextView = (TextView) findViewById(R.id.content_spectrogram2);
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
            ((ViewStub) findViewById(R.id.stub_for_url_to_play)).setVisibility(View.VISIBLE);
            editTextUrlToPlay = (EditText) findViewById(R.id.url_to_play);
            editTextUrlToPlayTextInitialColor = (editTextUrlToPlay.getCurrentTextColor() & 0x00FFFFFF) | 0x80000000;
            urlColorForInactive = editTextUrlToPlayTextInitialColor;
            //editTextViewUrlToPlay.setFocusableInTouchMode(true);
//            editTextViewUrlToPlay.clearFocus();
//            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//            imm.hideSoftInputFromWindow(editTextViewUrlToPlay.getWindowToken(), 0); //InputMethodManager.HIDE_NOT_ALWAYS);
            //imm.showSoftInput(editTextViewUrlToPlay, InputMethodManager.SHOW_FORCED);
            //imm.showSoftInput(editTextViewUrlToPlay, InputMethodManager.SHOW_IMPLICIT);
            playUrlButton = (Button) findViewById(R.id.button_play);
            hideUrlButton = (Button) findViewById(R.id.button_hide_url);
            //ViewStub:
            urlLayout = (LinearLayout) findViewById(R.id.inflated_for_url_to_play); //url_to_play_layout);
            if (urlLayout == null) Log.e(TAG, ".initUI: urlLayout is null");
            playUrlButton.setOnClickListener(ON_CLICK_LISTENER);
            hideUrlButton.setOnClickListener(ON_CLICK_LISTENER);
            urlIsPlaying = false;
            urlIsPaused = false;
            playUrlButton.setAlpha(ALPHA_NEUTRAL);
            hideUrlButton.setAlpha(ALPHA_NEUTRAL);
        }
//        pause.setClickable(true);
//        pause.setFocusable(true);
//        pause.requestFocus();
        //Log.e(TAG,".initUI: contentTextView is set");
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
        hideGui.setAlpha(ALPHA_DARK);
    }

    private void enableTheHideGuiButton(){
        if(hideGui==null)return;
        hideGui.setAlpha(ALPHA_NEUTRAL);
    }

    //

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
            if (AppConfig.UI_LOG_ENABLED)
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
        if (AppConfig.UI_LOG_ENABLED)
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
            if (AppConfig.UI_LOG_ENABLED)
                Log.d(TAG, "doHideGui: largeGuiLayout sensitivity is enabled");
            largeGuiLayout.setOnClickListener(ON_CLICK_LISTENER);
            largeGuiLayout.setClickable(true);
            Snackbar.make(largeGuiLayout,
                    "To show GUI, tap near middle of screen.",
                    Snackbar.LENGTH_LONG)
                    .setAction("null", null).show();
        }
    }

    private void doShowGui() {
        if (AppConfig.UI_LOG_ENABLED)
            Log.d(TAG, "doShowGui: entering");
        if (buttonsLayout != null) buttonsLayout.setVisibility(View.VISIBLE);
        if (contentTextView != null) {
            if(contentTextView.getText().length()>0) {
                if (AppConfig.UI_LOG_ENABLED)
                    Log.d(TAG, "doShowGui: contentTextView made visible because it has some text");
                contentTextView.setVisibility(View.VISIBLE);
            }else{
                if (AppConfig.UI_LOG_ENABLED)
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
        if (AppConfig.UI_LOG_ENABLED)
            Log.d(TAG, "doShowUrlGui: entering");

        if ( ! SOUND_TO_PLAY_IS_ENABLED) {
            if (AppConfig.UI_LOG_ENABLED)
                Log.d(TAG, "doShowUrlGui: exiting; SOUND_TO_PLAY_IS_ENABLED is false");
            return false;
        }

        if ( ! doesSoundOutput) {
            if (AppConfig.UI_LOG_ENABLED)
                Log.d(TAG, "doShowUrlGui: exiting; doesSoundOutput is false");
            return false;
        }

        if (urlLayout == null) {
            if (AppConfig.UI_LOG_ENABLED)
                Log.d(TAG, "doShowUrlGui: exiting; urlLayout is null");
            return false;
        }

        if (urlLayout.getVisibility() == View.VISIBLE) {
            //do nothing
//                urlLayout.setVisibility(View.GONE);
            if (AppConfig.UI_LOG_ENABLED)
                Log.d(TAG, "doShowUrlGui: urlLayout is already visible");
        } else {
            //show url gui
            if (AppConfig.UI_LOG_ENABLED)
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
        @Override
        public void onClick(View v) {
            try {
                if (AppConfig.UI_LOG_ENABLED)
                    Log.d(TAG, "ON_CLICK_LISTENER_FOR_SPECTROGRAM.onClick(v): entering");

                if (buttonsLayout.isShown()) {
                    //buttons are activated
                    if (AppConfig.UI_LOG_ENABLED)
                        Log.d(TAG, "ON_CLICK_LISTENER_FOR_SPECTROGRAM.onClick(v): " +
                                "buttonsLayout.isShown() returned true");
                    //do action
                    if (v instanceof Button) {
                        //for a button
                        if (AppConfig.UI_LOG_ENABLED)
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
                                                if (AppConfig.UI_LOG_ENABLED || AppConfig.ERROR_LOG_ENABLED)
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
                        if (AppConfig.UI_LOG_ENABLED)
                            Log.d(TAG, "ON_CLICK_LISTENER_FOR_SPECTROGRAM.onClick(v): " +
                                    ". is _not_ an instance of Button: " + v);
                        doShowGui();
                    }
                } else {
                    // layout not shown (buttons not show), then show it and show the buttons and url
                    if (AppConfig.UI_LOG_ENABLED)
                        Log.d(TAG, "ON_CLICK_LISTENER_FOR_SPECTROGRAM.onClick(v): " +
                                "buttonsLayout.isShown() returned false, then show GUI");
                    doShowGui();
                }
            } catch (Throwable ex) {
                if (AppConfig.UI_LOG_ENABLED||AppConfig.ERROR_LOG_ENABLED)
                    Log.e(TAG, "ON_CLICK_LISTENER_FOR_SPECTROGRAM.onClick(v): " + ex
                            + "\n" + Log.getStackTraceString(ex)
                        );
            }
            if (AppConfig.UI_LOG_ENABLED)
                Log.d(TAG, "ON_CLICK_LISTENER_FOR_SPECTROGRAM.onClick(v): exiting");
        }
    };

    /**
     * Sets the buttons look and flags after the actions for the given view have been done.
     *
     * @param v the button that has been selected
     */
    void afterButtonSelected(View v) {
        if (AppConfig.UI_LOG_ENABLED)
            Log.d(TAG,"afterButtonSelected: entering");

        updateAboutButtonOnUIThread();

        if (v.equals(hideUrlButton) || v.equals(playUrlButton)) {
            if (AppConfig.UI_LOG_ENABLED)
                Log.d(TAG,"afterButtonSelected: exiting; button is hideUrlButton or playUrlButton");
            return;
        }

        //setting the pause button
        if (isPaused()) {
            pause.setAlpha(ALPHA_SET);
        } else {
            //is not paused
            if (pause != null) pause.setAlpha(ALPHA_NOT_SET);
//                if(spectrogramShown){
//                    //not paused and spectro should be shown, then show it
//                    if(guiLayout!=null)guiLayout.setBackgroundColor(Color.TRANSPARENT);
//                }
        }

        // setting dependents on device text
        if (v.equals(device) && deviceShown) {
            // user just selected the device button
            device.setAlpha(ALPHA_SET);
            aboutShown = false;
            if (about != null) about.setAlpha(ALPHA_NOT_SET);
            ourAppsShown = false;
        } else {
            if (!deviceShown && device != null) device.setAlpha(ALPHA_NOT_SET);
        }

        //setting dependents on about text
        if (v.equals(about) && aboutShown) {
            about.setAlpha(ALPHA_SET);
            deviceShown = false;
            if (device != null) device.setAlpha(ALPHA_NOT_SET);
            ourAppsShown = false;
        } else {
            if (!aboutShown && about != null) about.setAlpha(ALPHA_NOT_SET);
        }

        //TODO deprecated
        if (SEPARATE_OUR_APPS_GUI_IS_ENABLED) {
            if (v.equals(ourApps) && ourAppsShown) {
                ourApps.setAlpha(ALPHA_SET);
                deviceShown = false;
                aboutShown = false;
                if (device != null) device.setAlpha(ALPHA_NOT_SET);
                if (about != null) about.setAlpha(ALPHA_NOT_SET);

            } else {
                if (!ourAppsShown && ourApps != null) ourApps.setAlpha(ALPHA_NOT_SET);
            }
        }
        //TODO deprecated
        if (SEPARATE_EMAIL_DEV_GUI_IS_ENABLED) {
            if (v.equals(emailDev)) {
                emailDev.setAlpha(ALPHA_SET);
            } else {
                emailDev.setAlpha(ALPHA_NOT_SET);
            }
        }

        //setting the hide xx button
        if (deviceShown || aboutShown) {
            //text shown
            if( !ALWAYS_HIDE_BG_WHEN_TEXT) {
                if (!hideBgIsSet) {
                    if (hideGui != null) hideGui.setText("Hide BG");
                    hideBgIsSet = true;
                }
                if (v.equals(hideGui)) {
                    hideGui.setAlpha(ALPHA_SET);
                } else {
                    if (hideGui != null) hideGui.setAlpha(ALPHA_NEUTRAL);
                }
            }else{
//                if (v.equals(hideGui)) {
//                    hideGui.setAlpha(ALPHA_SET);
//                } else {
//                    if (hideGui != null) hideGui.setAlpha(ALPHA_NEUTRAL);
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
                    hideGui.setAlpha(ALPHA_SET);
                } else {
                    if (hideGui != null) hideGui.setAlpha(ALPHA_NOT_SET);
                }
            }else{
//                if (v.equals(hideGui)) {
//                    hideGui.setAlpha(ALPHA_SET);
//                } else {
//                    if (hideGui != null) hideGui.setAlpha(ALPHA_NEUTRAL);
//                }
            }
        }
    }

    private void deviceButtonSelected(){
        if (AppConfig.UI_LOG_ENABLED)
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
                if (AppConfig.UI_LOG_ENABLED)
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
                Snackbar.make(largeGuiLayout,
                        "Nothing to do for this button in this situation",//TODO res value string
                        Snackbar.LENGTH_LONG).setAction("null", null).show();
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
        if (AppConfig.UI_LOG_ENABLED)
            Log.d(TAG, "aboutButtonSelected: " +
                    "aboutShown = " + aboutShown
                    + "; contentTextView = " + contentTextView
                    + "; guiLayout = " + largeGuiLayout);
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
                if (AppConfig.UI_LOG_ENABLED)
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
//                if (AppConfig.UI_LOG_ENABLED)
//                    Log.d(TAG, "aboutButtonSelected: largeGuiLayout sensitivity is enabled");
//                largeGuiLayout.setOnClickListener(ON_CLICK_LISTENER);
//                largeGuiLayout.setClickable(true);
//                largeGuiLayout.setBackgroundColor(Color.TRANSPARENT);
//            }
        }
    }

    private void playUrlButtonSelected(){
        if (AppConfig.PLAY_URL_LOG_ENABLED)
            Log.d(TAG, "button playUrlButton selected; " +
                    "urlIsPlaying = " + urlIsPlaying
                    + "; urlIsPaused = " + urlIsPaused);
        clearLastAnomaly();
        if (urlIsPlaying) {
            // is playing, so do pause url
            pauseSpectro();
            doPauseUrl(false);
        } else {
            // url is not playing, then do play url or resume
            if (urlIsPaused) {
                restartSpectro();
                doResumeUrl();
            } else {
                // url was not paused, so play, don't resume
                // if spectro paused, then resume
                if (isPaused()) restartSpectro();
                doPlayUrl(
                        editTextUrlToPlay.getText().toString());
            }
        }
    }

    private void hideOrCancelUrlButtonSelected(){
        if (AppConfig.PLAY_URL_LOG_ENABLED)
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

//    /**
//     * Not used in this version.
//     *
//     * @param fd
//     * @param intent
//     * @return true when possible success, and false when definite failure.
//     *
//     * @deprecated in this version
//     */
//    private boolean doPlayUrl(final FileDescriptor fd, final Intent intent) {
//        player = new SmartPlayer();
//        try {
//            showUrlPrepareSnackbar();
//            player.play(fd, this);
//        } catch (Throwable ex) {
//            Log.e(TAG, ".doPlayUrl(FileDescriptor) " + ex + " " + Log.getStackTraceString(ex));
//            doResetUrl(true);
//            //showAnomalyText(ex, ex.getMessage());
//            invalidIntent(intent, null);// uses a snackbar
//            return false;
//        }
//        setUrlGuiWhenPlaying();
//        return true;
//    }

    /**
     * Designed to be called by onCreate and not the Play URL button.
     *
     * <P/>Starts a new thread to prepare and play the given URL, and shows a Snackbar.
     *
     * <p/>Used in this version.
     *
     * <p/>Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     *
     * @param intent referencing a remote audio or media file
     * @throws IllegalArgumentException
     */
    private boolean doPlayUrl(final Intent intent) {
        isCancelling = false;
        player = new SmartPlayer();
        try {
            showUrlPrepareSnackbar();
            player.play(intent, this);
        } catch (Throwable ex) {
            Log.e(TAG, ".doPlayUrl(Intent) " + ex + " " + Log.getStackTraceString(ex));
            doResetUrl(true);
//            showAnomalyText(ex, ex.getMessage()); //TO DO use snackbar maybe
            invalidIntent(intent, null);
            return false;
        }
        setUrlGuiWhenPlaying();
        return true;
    }

    private volatile boolean permissionRequestedForStorageAccess = false;

    /**
     * Called when the Play button is selected and the URL is to be started or restarted,
     * not resumed,
     * i.e., when urlWasPaused is false.
     * In some cases, the URL has been entered manually instead of coming from an intent.
     *
     * <P/>Starts a new thread to prepare and play the given URL, then shows a Snackbar.
     *
     * <p/>Used in this version.
     *
     * <p/>Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     *
     * @param urlString referencing a remote audio or media file, or a local file;
     *                  the value is from url edittext view.
     * @return true when possible success; false when failure.
     * The returned value is not used in this version of the app.
     */
    private boolean doPlayUrl(final String urlString) {
        isCancelling = false;
        if (AppConfig.PLAY_URL_LOG_ENABLED || AppConfig.PERMISSIONS_LOG_ENABLED)
            Log.d(TAG, ".doPlayUrl(String url): entering with urlString {" + urlString
                    //+"} permissionGrantedForStorageAccess = "+permissionGrantedForStorageAccess
                    + "}, SOUND_TO_PLAY_IS_ENABLED = " + SOUND_TO_PLAY_IS_ENABLED
                    + ", doesSoundOutput = " + doesSoundOutput);
        if (urlString == null) return false;
        final String urlStrg = urlString.trim();
        if (urlStrg.isEmpty()) return false;
        if (!SOUND_TO_PLAY_IS_ENABLED) return false;
        if (!doesSoundOutput) return false;
        final String exampleUrl = getString(R.string.url_example);
        if (urlStrg.equalsIgnoreCase(exampleUrl)) return false;

        // catch local file such as file://...
        // and don't try if file storage permission access not granted

        if (isLocalFile(urlStrg)) {
            // is local file, check external storage access permission
            if (AppConfig.PLAY_URL_LOG_ENABLED || AppConfig.PERMISSIONS_LOG_ENABLED)
                Log.d(TAG, ".doPlayUrl: is local file {" + urlStrg + "}; get access permission...");

            if (!getPermissionForExternalStorageAccess(urlStrg)) {
                // external storage access permission was definitively denied
                // or permission is being requested, don't play
                if (AppConfig.PLAY_URL_LOG_ENABLED || AppConfig.PERMISSIONS_LOG_ENABLED)
                    Log.d(TAG, ".doPlayUrl: access denied or being requested...");
                return false;
            }
            // local file access granted, then play it
            if (AppConfig.PLAY_URL_LOG_ENABLED || AppConfig.PERMISSIONS_LOG_ENABLED)
                Log.d(TAG, ".doPlayUrl: access granted, try to play local file");
        } else {
            // not local file, then remote file, try to play
            if (AppConfig.PLAY_URL_LOG_ENABLED || AppConfig.PERMISSIONS_LOG_ENABLED)
                Log.d(TAG, ".doPlayUrl: not local file, try to play remote file");
        }

        // try to play url

        return playLocalFileAfterStorageAccessGranted(urlStrg);
    }

    /**
     * Used in this version.
     * <p/>
     * <p/>Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     *
     * @param filePath
     * @return true when started, false when attempt failed.
     */
    private boolean playLocalFileAfterStorageAccessGranted(final String filePath) {
        if (AppConfig.PLAY_URL_LOG_ENABLED)
            Log.d(TAG, ".playLocalFileAfterStorageAccessGranted: entering with filePath {" + filePath + "}");

        //=================================================
        boolean started = playRemoteOrLocalFile(filePath);
        //=================================================

        if (AppConfig.PLAY_URL_LOG_ENABLED || AppConfig.PERMISSIONS_LOG_ENABLED)
            Log.d(TAG, ".playLocalFileAfterStorageAccessGranted(String): " +
                    "playRemoteOrLocalFile(urlString) returned " + started
                    + " with {" + filePath + "}");

        if (started) setUrlGuiWhenPlaying();

        return started;
    }

    private boolean isLocalFile(final String urlOrPath) {
        return (urlOrPath.startsWith("file://") || urlOrPath.startsWith("/"));
    }

    /**
     * Not just for http, can also be local file path.
     *
     * <p/>Used in this version.
     *
     * <p/>Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     *
     * @param urlOrFilePath
     * @return true is success to start play
     */
    private boolean playRemoteOrLocalFile(final String urlOrFilePath) {
        try {
            if (player == null) {
                player = new SmartPlayer();
            }
            showUrlPrepareSnackbar();
            player.play(urlOrFilePath, this);//throws IllegalArgumentException
        } catch (Throwable ex) {
            doResetUrl(true);
            //showAnomalyText(ex,ex.getMessage());
            invalidUrl(urlOrFilePath);//uses a snackbar
            return false;
        }
        setUrlGuiWhenPlaying();
        return true;
    }

    /**
     * Not used in this version.
     *
     * @param filepath
     * @return boolean success or failure
     */
    private boolean playLocalFile(final String filepath) {
        isCancelling = false;
        try {
            if (player == null) {
                player = new SmartPlayer();
            }
            showUrlPrepareSnackbar();
            player.play(filepath, this);//throws IllegalArgumentException
        } catch (Throwable ex) {
            doResetUrl(true);
            invalidUrl(filepath);//uses a snackbar
            return false;
        }
        setUrlGuiWhenPlaying();
        return true;
    }

    /**
     * Not used in this version.
     *
     * @param path
     * @return boolean
     */
    private boolean playWithoutIntent(final String path) {
        if (path.startsWith("file://")) {
            //uri for a local file

            return playLocalFile(path);

        } else {
            //uri is for not a local file; may be for a remote file such as http://... or https://

            if (path.startsWith("http") || path.startsWith("rtsp")) {
                //uri for a remote file

                return playRemoteOrLocalFile(path);
            }
        }

        return false;
    }

    /**
     * Not used in this version.
     *
     * @param path
     * @return boolean
     */
    private boolean play(final String path) {
        if (AppConfig.LOG_INTENT)
            Log.d(TAG, ".play(String): path {" + path + "}");

        Uri uri = Uri.parse(path);
        Intent intent = new Intent(Intent.ACTION_SEND, uri);

        return doPlayUrl(intent);
    }

    private final Runnable RUNNABLE_FOR_URL_PREPARE = new Runnable() {
        @Override
        public void run() {
            urlPrepareSnackbar = Snackbar.make(largeGuiLayout,
                    "Please wait, preparing media for playing...",//TODO res
                    Snackbar.LENGTH_INDEFINITE).setAction("null", null);
            urlPrepareSnackbar.show();
        }
    };

    private void showUrlPrepareSnackbar() {
        runOnUiThread(RUNNABLE_FOR_URL_PREPARE);
    }


    public static final boolean USER_LOG_INIT = false;

    private Snackbar statusSnackbar;

    private volatile String statusText = "";

    private final Runnable RUNNABLE_FOR_STATUS = new Runnable() {
        @Override
        public void run() {
            if(largeGuiLayout!=null) {
                statusSnackbar = Snackbar.make(largeGuiLayout,
                        statusText,
                        Snackbar.LENGTH_SHORT).setAction("null", null);
                statusSnackbar.show();
            }
        }
    };

    /**
     * Designed to be used when USER_LOG_INIT is true.
     *
     * @param giventStatusText
     */
    private void showStatusSnackbar(final String giventStatusText) {
        statusText = giventStatusText;
        runOnUiThread(RUNNABLE_FOR_STATUS);
    }

    /**
     * Designed to be called when urlIsPaused is true.
     *
     * <p/>Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     */
    private void doResumeUrl() {
        isCancelling = false;
        //if url is not paused then exit
        if (!urlIsPaused) return;
        if (player!=null && player.resumeOrRestart()) {
            setUrlGuiWhenPlaying();
        } else {
            doResetUrl(true);
        }
    }

    /**
     * input: urlToPlay attribute, which has been set when restoring preferences upstream
     *
     * <p/>Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     *
     * @param urlString can be URL from preferences.
     * @return true when url set in GUI; false otherwise.
     */
    private boolean setUrlToPlayInUi(final String urlString) {//TODO review with setUrlGuiWhenPlaying()
        //urlToPlay = urlString;
        if (AppConfig.RESTORE_LOG_ENABLED || AppConfig.LOG_INTENT
                || AppConfig.PLAY_URL_LOG_ENABLED)
            Log.d(TAG, ".setUrlToPlayInUi: urlString {" + urlString + "}");

        if (urlString != null && !urlString.isEmpty()) {
            // url restored from prefs

            if (editTextUrlToPlay != null) {
                urlToPlay = urlString;
                editTextUrlToPlay.setText(urlToPlay);
                return true;
            } else {
                // ui widget is null
                if (AppConfig.RESTORE_LOG_ENABLED || AppConfig.LOG_INTENT
                        || AppConfig.PLAY_URL_LOG_ENABLED)
                    Log.d(TAG, ".setUrlToPlayInUi: editTextUrlToPlay is null");
            }
        }
        return false;
    }

    /**
     * Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     */
    private void setUrlGuiWhenPlaying() {
        //if(urlPrepareSnackbar!=null)urlPrepareSnackbar.dismiss();
        urlIsPlaying = true;
        urlIsPaused = false;
        if (playUrlButton != null) playUrlButton.setText("Pause");
        if (hideUrlButton != null) hideUrlButton.setText("Cancel");
        if (editTextUrlToPlay != null) editTextUrlToPlay.setTextColor(urlColorForActive);
    }

    /**
     * Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     *
     * @param ignoreAnomaly when false and there is an anomaly (pause fails),
     *                      then the url color is set to the error color.
     */
    private void doPauseUrl(boolean ignoreAnomaly) {
        if (player == null) return;
        if (!urlIsPlaying) return;
        if (player.pause()) {
            //pause succeeded
            if (urlPrepareSnackbar != null && urlPrepareSnackbar.isShown()) {
                //preparation is not completed
                urlPrepareSnackbar.dismiss();
                doResetUrl(false);
            } else {
                //preparation was completed earlier
                urlIsPlaying = false;
                urlIsPaused = true;
                if (playUrlButton != null) playUrlButton.setText("Resume");
                if (hideUrlButton != null) hideUrlButton.setText("Cancel");
                if (editTextUrlToPlay != null) editTextUrlToPlay.setTextColor(urlColorForPaused);
            }
        } else {
            //pause failed
            doResetUrl(!ignoreAnomaly);
        }
    }

    /**
     * Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     *
     * @param isForAnomaly when true, the url color is set to the error color
     */
    private void doResetUrl(boolean isForAnomaly) {
        if (player != null && isForAnomaly) player.shutdown();
        if (urlPrepareSnackbar != null) urlPrepareSnackbar.dismiss();
        resetUrlGui();//color is set to urlColorForInactive
        if (isForAnomaly) editTextUrlToPlay.setTextColor(urlColorForError);
        //TODO prepare anomaly text for display; get text fragment from client method
    }

    /**
     * stop the play; for the hideUrlButton button when label is *Cancel* to stop the play and not hide gui
     */
    private void doCancelUrl() {
        if (player == null) return;
        isCancelling=true;
        player.shutdown();
        resetUrlGui();
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
        if (AppConfig.UI_LOG_ENABLED)
            Log.d(TAG, "doHideUrl: entering");

        doCancelUrl();

        if (urlLayout != null) urlLayout.setVisibility(View.GONE);

        if (largeGuiLayout != null) {
            if (AppConfig.UI_LOG_ENABLED)
                Log.d(TAG, "doHideUrl: largeGuiLayout sensitivity is enabled");
            largeGuiLayout.setOnClickListener(ON_CLICK_LISTENER);
        }

        if (withNotif) {
            String s = "Tap the screen to show the media GUI.";
            if(contentTextView.getVisibility()==View.VISIBLE){
                s = "To show the media GUI again, first hide the text by tapping the related text button and then tap the screen.";
            }
            if (largeGuiLayout != null) {
                Snackbar.make(largeGuiLayout,
                        s,
                        Snackbar.LENGTH_LONG)
                        .setAction("null", null).show();
            }
        }
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
            if (AppConfig.PERMISSIONS_LOG_ENABLED) {
                Log.d(TAG, ".getPermissionForExternalStorageAccess: " +
                        "checkSelfPermission returned positive; this method is returning true");
            }
            return true;
        }

        //no permission; did we asked the user before? if yes, then don't ask again
        if (AppConfig.PERMISSIONS_LOG_ENABLED) {
            Log.d(TAG, ".getPermissionForExternalStorageAccess: " +
                    "checkSelfPermission returned negative; storageAccessPermissionWasAsked = "
                    + storageAccessPermissionWasAsked);
        }
        if (storageAccessPermissionWasAsked) {
            // previously denied by user
            if (largeGuiLayout != null)
                Snackbar.make(largeGuiLayout,
                        "The file cannot be played"
                                + ": it is in external storage and access was denied by you",
                        Snackbar.LENGTH_LONG).show();
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
        if (AppConfig.PERMISSIONS_LOG_ENABLED) {
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

    private void getPermissionForRecordAudio(final Bundle savedInstanceStateGiven) {
        savedInstanceStateTemp = savedInstanceStateGiven;
        if (AppConfig.INIT_LOG_ENABLED || AppConfig.SOUND_INPUT_INIT_LOG_ENABLED
                || AppConfig.PERMISSIONS_LOG_ENABLED)
            Log.d(TAG, ".getPermissionForRecordAudio entering...Build.VERSION.SDK_INT = "
                    + Build.VERSION.SDK_INT);

//        getPermissionForExternalStorageAccess();//TO DO was here integrate with the rest of this method
//        if(permissionGrantedForRecordAudio)return true;
        /*
        PackageManager.PERMISSION_GRANTED if you have the permission, or PackageManager.PERMISSION_DENIED if not.
         */
        boolean aprioriGranted = false;
        if (Build.VERSION.SDK_INT >= 23) {
            aprioriGranted = checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            aprioriGranted = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
        }
        if (AppConfig.INIT_LOG_ENABLED || AppConfig.SOUND_INPUT_INIT_LOG_ENABLED
                || AppConfig.PERMISSIONS_LOG_ENABLED)
            Log.d(TAG, ".getPermissionForRecordAudio: aprioriGranted " + aprioriGranted);

        if (!aprioriGranted) {
            // we don't have permission yet, ask user;
            // No explanation needed to give user, we can request the permission.

            String[] permissions = new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE};

            if (Build.VERSION.SDK_INT >= 23) {
                if (AppConfig.INIT_LOG_ENABLED || AppConfig.SOUND_INPUT_INIT_LOG_ENABLED
                        || AppConfig.PERMISSIONS_LOG_ENABLED)
                    Log.d(TAG, ".getPermissionForRecordAudio about to call requestPermissions(...)");
                requestPermissions(permissions, PERMISSIONS_REQUEST_CODE_FOR_RECORD_AUDIO);
            } else {
                if (AppConfig.INIT_LOG_ENABLED || AppConfig.SOUND_INPUT_INIT_LOG_ENABLED
                        || AppConfig.PERMISSIONS_LOG_ENABLED)
                    Log.d(TAG, ".getPermissionForRecordAudio about to call ActivityCompat.requestPermissions(...)");
                ActivityCompat.requestPermissions(this, permissions,
                        PERMISSIONS_REQUEST_CODE_FOR_RECORD_AUDIO);
            }
            // PERMISSIONS_REQUEST_CODE_FOR_RECORD_AUDIO is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        } else {
            //granted apriori
            if (AppConfig.INIT_LOG_ENABLED || AppConfig.SOUND_INPUT_INIT_LOG_ENABLED
                    || AppConfig.PERMISSIONS_LOG_ENABLED)
                Log.d(TAG, ".getPermissionForRecordAudio: was granted apriori" +
                        "; calling onCreateComplete...");

//            permissionGrantedForRecordAudio = true;
            onCreateComplete(savedInstanceStateGiven);
        }
        //here when permission granted before running the app, e.g., older version of android

        if (AppConfig.INIT_LOG_ENABLED || AppConfig.SOUND_INPUT_INIT_LOG_ENABLED
                || AppConfig.PERMISSIONS_LOG_ENABLED)
            Log.d(TAG, ".getPermissionForRecordAudio exiting...");
    }

    /**
     * Called by Android on the ui thread.
     *
     * @param requestCode  int
     * @param permissions  array of String instances
     * @param grantResults array of int primitives
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        if (AppConfig.INIT_LOG_ENABLED || AppConfig.SOUND_INPUT_INIT_LOG_ENABLED
                || AppConfig.PERMISSIONS_LOG_ENABLED)
            Log.d(TAG, ".onRequestPermissionsResult entering with requestCode {" + requestCode
                    + "}; filePathNeedingAccess {" + filePathNeedingAccess + "}");
//        readFileAccepted = false;
//        writeFileAccepted = false;
        //permissionGrantedForRecordAudio = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE_FOR_RECORD_AUDIO: {
                if (AppConfig.INIT_LOG_ENABLED
                        || AppConfig.SOUND_INPUT_INIT_LOG_ENABLED
                        || AppConfig.PERMISSIONS_LOG_ENABLED)
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
                    //not granted, cannot run the app
                    if (AppConfig.INIT_LOG_ENABLED || AppConfig.SOUND_INPUT_INIT_LOG_ENABLED
                            || AppConfig.PERMISSIONS_LOG_ENABLED)
                        Log.d(TAG, ".onRequestPermissionsResult: audio permission denied; calling finish()");
                    finish();
                    return;
                }
                //here when record audio was granted
                if (AppConfig.INIT_LOG_ENABLED || AppConfig.SOUND_INPUT_INIT_LOG_ENABLED
                        || AppConfig.PERMISSIONS_LOG_ENABLED)
                    Log.d(TAG, ".onRequestPermissionsResult: audio permission granted");
                //complete create
                onCreateComplete(savedInstanceStateTemp);
                break;
            } // case

            case PERMISSIONS_REQUEST_CODE_FOR_STORAGE_ACCESS: {
                if (AppConfig.INIT_LOG_ENABLED
                        || AppConfig.SOUND_INPUT_INIT_LOG_ENABLED
                        || AppConfig.PERMISSIONS_LOG_ENABLED)
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

                if (AppConfig.INIT_LOG_ENABLED
                        || AppConfig.SOUND_INPUT_INIT_LOG_ENABLED
                        || AppConfig.PERMISSIONS_LOG_ENABLED)
                    Log.d(TAG, ".onRequestPermissionsResult: " +
                            "PERMISSIONS_REQUEST_CODE_FOR_STORAGE_ACCESS; " +
                            "readFileAccepted = " + readFileAccepted
                            + "; writeFileAccepted = " + writeFileAccepted);

                if (readFileAccepted || writeFileAccepted) {
                    playLocalFileAfterStorageAccessGranted(filePathNeedingAccess);
                } else {
                    // disable the play url functions but only for local files
                }

                break;
            }

            // other 'case' lines to check for other
            // permissions this app might request
            default: {
                if (AppConfig.INIT_LOG_ENABLED || AppConfig.SOUND_INPUT_INIT_LOG_ENABLED
                        || AppConfig.PERMISSIONS_LOG_ENABLED)
                    Log.d(TAG, ".onRequestPermissionsResult: default; requestCode " + requestCode
                            //+"; permissionGrantedForRecordAudio "+ permissionGrantedForRecordAudio
                            //+"; calling onCreateComplete(savedInstanceStateTemp)..."
                    );
//                onCreateComplete(savedInstanceStateTemp);
            }
        }//switch

    }


    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (AppConfig.PAUSE_LOG_ENABLED || AppConfig.THREADS_LOG_ENABLED
                || AppConfig.RESTORE_LOG_ENABLED)
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
    protected void onRestoreInstanceState(Bundle savedInstanceState) {//TODO future restore bitmap
        super.onSaveInstanceState(savedInstanceState);
        if (AppConfig.PAUSE_LOG_ENABLED || AppConfig.THREADS_LOG_ENABLED
                || AppConfig.RESTORE_LOG_ENABLED)
            Log.d(TAG, ".onRestoreInstanceState: calling restoreBitmap(savedInstanceState)..."
                    + Thread.currentThread());

        restoreBitmap(savedInstanceState);
    }

    public static final String PREF_BITMAP_KEY = "bitmap";
    public static final String PREF_URL_KEY = "url_to_play";

    private void restoreBitmap(final Bundle savedInstanceState) {
        if (!SPECTROGRAM_IMAGE_PERSISTENCE_IS_ENABLED) {
            if (AppConfig.PAUSE_LOG_ENABLED || AppConfig.RESTORE_LOG_ENABLED)
                Log.d(TAG, ".restoreBitmap: exiting because SPECTROGRAM_IMAGE_PERSISTENCE_IS_ENABLED is false");
            return;
        }
        if (savedInstanceState != null) {
            // not new session; restoring the app from Android savedInstanceState
            if (AppConfig.PAUSE_LOG_ENABLED || AppConfig.RESTORE_LOG_ENABLED)
                Log.d(TAG, ".restoreBitmap: restoring");
//            SpectrogramView.restoring = true; TODO future restoring bitmap function
            //------------------------------------------------------------
            Object ob = savedInstanceState.getParcelable(PREF_BITMAP_KEY);
            //------------------------------------------------------------
            if (ob != null) {
//                SpectrogramView.bitmap = (Bitmap) ob;
                if (AppConfig.PAUSE_LOG_ENABLED || AppConfig.RESTORE_LOG_ENABLED)
                    Log.d(TAG, ".restoreBitmap: restoring; saved bitmap not null");
            } else {
                if (AppConfig.PAUSE_LOG_ENABLED || AppConfig.RESTORE_LOG_ENABLED)
                    Log.d(TAG, ".restoreBitmap: restoring; saved bitmap is null");
            }

        } else {
            // not restoring; new session
            if (AppConfig.PAUSE_LOG_ENABLED || AppConfig.RESTORE_LOG_ENABLED)
                Log.d(TAG, ".restoreBitmap: not restoring");
//            SpectrogramView.restoring = false;
//            SpectrogramView.bitmap = null;
        }
    }

    private void saveBitmap(Bundle outState) {
        if (!SPECTROGRAM_IMAGE_PERSISTENCE_IS_ENABLED) {
            if (AppConfig.PAUSE_LOG_ENABLED || AppConfig.RESTORE_LOG_ENABLED)
                Log.d(TAG, ".saveBitmap: exiting because SPECTROGRAM_IMAGE_PERSISTENCE_IS_ENABLED is false");
            return;
        }

        if (SpectrogramView.spectrogramView != null) {
            Bitmap bitmap = SpectrogramView.spectrogramView.bitmap;
            if (bitmap != null) {
                //----------------------------------------------
                outState.putParcelable(PREF_BITMAP_KEY, bitmap);
                //----------------------------------------------
                if (AppConfig.PAUSE_LOG_ENABLED || AppConfig.RESTORE_LOG_ENABLED)
                    Log.d(TAG, ".saveBitmap: bitmap saved");
            } else {
                if (AppConfig.PAUSE_LOG_ENABLED || AppConfig.RESTORE_LOG_ENABLED)
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
            if (AppConfig.PAUSE_LOG_ENABLED)
                Log.e(TAG, ".restartSpectro: calling unpause()...");
            unpause();
            pause.setText(getString(R.string.pause_button));
            if (AppConfig.PAUSE_LOG_ENABLED)
                Log.d(TAG, ".restartSpectro: unpause() ok");
        } catch (Exception e) {
            //e.printStackTrace();
            if (AppConfig.SOUND_INPUT_INIT_LOG_ENABLED || AppConfig.ERROR_LOG_ENABLED)
                Log.e(TAG, ".restartSpectro: unpause() raised: " + e);
            return;
        }
//        pauseButton.setText(getString(R.string.pause_button));
        isPausedByHUser = false;
    }

    private void pauseSpectro() {
        if (AppConfig.PAUSE_LOG_ENABLED)
            Log.d(TAG, ".pauseSpectro entering");
        closeListener();
        pause.setText(getString(R.string.pause_button_restart));
        isPausedByHUser = true;
        if (AppConfig.PAUSE_LOG_ENABLED)
            Log.d(TAG, ".pauseSpectro exiting");
    }

    /**
     * <p/>Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     */
    private void pauseToggle() {
        if (AppConfig.PAUSE_LOG_ENABLED)
            Log.d(TAG, ".pauseToggle: entering; isPausedByHUser " + isPausedByHUser
                            + "; isPaused() " + isPaused()
//                    + "; button label = "+pauseButton.getText()
            );
        if (isPausedByHUser || isPaused()) {
            // is paused, then restart
            restartSpectro();
            // url is not playing, then do play url or resume
            if (urlIsPaused) {
                doResumeUrl();
            } else {
                //TODO washere washere bug don't play when restarting spectro and url was not playing
                // was not paused, so play, don't resume
                // there is a time limit on attempt to play
                // doPlayUrl(editTextUrlToPlay.getText().toString());
                if (AppConfig.PAUSE_LOG_ENABLED)
                    Log.d(TAG, ".pauseToggle: entering; isPausedByHUser " + isPausedByHUser
                            + "; urlIsPaused " + urlIsPaused
                    );
            }
        } else {
            //is running, then pause
            pauseSpectro();
            doPauseUrl(false);
        }
        if (AppConfig.PAUSE_LOG_ENABLED)
            Log.d(TAG, ".pauseToggle: exiting; isPausedByHUser " + isPausedByHUser
                            + "; isPaused() " + isPaused()
//                    + "; button label = "+pauseButton.getText()
            );
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
////        getMenuInflater().inflate(R.menu.menu, menu);
////        return true;
//        return false;
//    }
//
//    @Override
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
        if (!AppConfig.getIt().isSupportEmailEnabled()) return;
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("mailto:" + AppConfig.getIt().getSupportEmailAddress()));
        intent.putExtra(Intent.EXTRA_SUBJECT, AppConfig.getIt().getSupportEmailSubject());
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(intent);
    }

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

    /* for resources:
    <string name="my_text">
  <![CDATA[
    <b>Autor:</b> Mr Nice Guy<br>
    <b>Contact:</b> myemail@grail.com<br>
    <i>Copyright © 2011-2012 Intergalactic Spacebar Confederation </i>
  ]]>
</string>
tv.setText(Html.fromHtml(getString(R.string.my_text)));
     */

    /**
     * @return String or null when none;
     * when an anomaly text exists, then it is formatted with html tags for Html.fromHtml().
     */
    private String getLastAnomalyTextInHtml() {
        if (lastAnomalyText == null) return null;
        StringBuilder buf = new StringBuilder();
        buf.append("<h3>Last Anomaly:</h3><p/>");
        buf.append(lastAnomalyText);
        if (AppConfig.getIt().isDevMode()) {
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

    /* text for the Play store:

    See the sounds surrounding you:
    - appreciate the visual structural beauty of bird songs
    - detect sounds that you cannot hear
    - evaluate the sound quality of your immediate environment
    - locate anomalous sound sources in your house or at work
    - get an idea of the quality of your hearing and may help you decide to get a medical hearing test

    This app will also
    - play a remote media file (e.g., a wa. file) of killer whale vocalizations, for example, or the pre-recorded vocalizations from your cat or dog
    - test the audio capabilities of your Android device and present the results in a text form
    - select the best sampling rate supported by your device

    Using the built-in feature of your Android device, you can take screen shots of the spectrogram and share them via email or messenger apps.
    The app does not include special features for doing this; just use the normal functions of your device.

    External microphones may be compatible with your device, for example, via a USB connector or the RCA audio jack.
    The app does not have special features for external mic's and you're on your own for determining if a microphone is compatible or not.
    Future versions may have special functions for testing external mic's.
    We do not recommend microphones that use Bluetooth at this time due to the inherent limitations of the current Bluetooth technology for audio input.

    This app does not record sound.

    Some current Android devices can perform sampling of sounds at 96,000 samples per second, with a relatively narrow latency (maybe 1/2 second);
    this latency is the delay between the actual sounds and the availability of the numeric samples to the application.
    Only Android Marshmallow (i.e., version 6, API level 23), and later versions, do support 96,000 samples per second.
    This app supports 96,000 samples per second. It analyzes sounds 40 times per second. It uses 1024 samples for each analysis (using FFT).
    This app automatically adapts itself to the best sampling rate supported by your device.
     */

    private String getDeviceText(){
        StringBuilder buf = new StringBuilder();
        buf.append( Html.fromHtml( getDeviceTextInHtml() ));
        return buf.toString();
    }

    private String getDeviceTextInHtml(){
        StringBuilder buf = new StringBuilder();

        buf.append(getContentSectionForDeviceTextInHtml())
        .append(DeviceSoundCapabilities.getDeviceCapabilitiesInHtml(true, true, this))
        .append(SettingsForSoundInputDisplay.getForAppTextInHtml(true))
        .append(getPerfMeasurementsInHtml())
        ;

        return buf.toString();
    }

    private String getPerfMeasurementsInHtml(){
        StringBuilder buf = new StringBuilder();

        buf.append("<p/><h2>SOUND PROCESSING PERFORMANCE MEASUREMENTS</h2>")
        .append("<p/>Android devices these days have a delay of about half a second between the actual sound and the processing by the app, when non-native code is used")
        .append("; this app adds about 1/50 sec. of delay due to numerical analysis (e.g., FFT) and display; 1/50 sec. of delay is about 10% of the basic delay or latency for Android in 2016.")
        .append(" The time spent by Android and the app displaying the data as a spectrogram is much more than the time used for numerical analysis.")
        .append("<p/>").append(BasicListener.processingPerfInHtml)
        .append("<p/>End of performance measurements results")
        ;
        return buf.toString();
    }

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
     * TODO move to library washere 2017-5
     * @param html Stxing text
     * @return android.text.Spanned, implements CharSequence
     */
    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String html){
        Spanned result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            result = Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(html);
        }
        return result;
    }

    private String getAboutText() {
        StringBuilder buf = new StringBuilder();

        buf.append(fromHtml( getAboutTextInHtml() ));

        return buf.toString();
    }

    /**
     * Sometimes includes Help text and details about recent anomaly.
     *
     * @return String with html tags compatible with Html.fromHtml.
     */
    private String getAboutTextInHtml() {
        StringBuilder buf = new StringBuilder("<h2>ABOUT THIS APP - "); //.toUpperCase());
        buf.append(getString(R.string.app_name)).append("</h2>");

        String anomaliesText = getLastAnomalyTextInHtml();

        buf.append("<h4>Content:</h4>")
        .append("<p/>Copyright");
        if(!(anomaliesText == null || anomaliesText.length() == 0)){
            buf.append("<br>Anomaly");
        }else{
            buf.append("<br>Contact Info");
        }
        buf.append("<br>Introduction")
        .append("<br>User Interface Guide")
        .append("<br>Privacy Policy")
        .append("<br>Terms")
        .append("<p/>")
                ;

        //buf.append("<p/>Small icon © 2000-2015 AguaSonic.com - Icon made from cetacean sound");
        buf.append("<p/>").append(AppPublisher.copyright);

        buf.append("<p/>")
        .append("Some links to cetacean vocalisation recordings are Copyright Aguasonic Acoustics.");


        // ##### anomaly text if any #####

        if (anomaliesText == null || anomaliesText.length() == 0) {
            //no anomalies
            buf.append("<p/>Questions, defects, suggestions, please contact ")
                    .append(AppPublisher.emailAddressForSupport);
            runOnUiThread(RUNNABLE_TO_CLEAR_ANOMALY_TEXT);
        } else {
            //anomalies
            buf.append("<p/>~~~~~~~~~~<p/>");
            buf.append(anomaliesText);
            buf.append("<p/>End of anomaly text");
            buf.append("<p/>~~~~~~~~~~<p/>");

            lastAnomalyTextIsShown = true;
        }

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
//        buf.append("<p/>") TODO future confirm source of FFT
//                .append("The Fast Fourier Transform (FFT) calculations include parts of the NIST (National Institute of Standards) scimark2 program")
//                .append(", as well as calculations developed by Serge Masse. ")
//                .append("The original NIST Java code, not all used here, was written by Bruce R. Miller, and was inspired by the GSL"
//                .append(" (Gnu Scientific Library) FFT written in C by Brian Gough. ");

        //buf.append("The FFT use 'double' primitives."); TODO future use option for de. for float vs double
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

    /* TODO EULA example from https://www.makingmoneywithandroid.com/2011/05/how-to-eula-android-app/

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

    /* TODO from https://web.archive.org/web/20130205134238/http://www.developer-resource.com/sample-eula.htm

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

    private String getPrivacyPolicyInHtml() {
        StringBuilder buf = new StringBuilder();

        buf.append("<p/><H2>PRIVACY POLICY</H2><p/>");

        buf.append("This app does not send any information to anyone.");

        buf.append("<p/>The author/publisher would keep private any emails or other communications from users.");

        return buf.toString();
    }

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
        buf.append(" To do that, one way is to go to the sound file using the Chrome browser and to share the sound to this app.");
        buf.append(" To share the sound file, you open the pop-up on the sound file link and you select the share option, which has an icon that looks like three linked dots.");
        buf.append(" When the choices of ways to share comes up, you select the icon for this app.");

        buf.append("<p/>You can also use other sound apps that can share recorded files with this app, often in a similar fashion to using Chrome for sharing.");

        buf.append(" Some web sites do not let their sounds to be shared and many do.");

        buf.append("<p/>This site below contains high quality cetacean sounds that you can download to your device")
        .append(" and then share with this app, by using Android downloaded file access or a file management app from a third party.")
        .append(" You may need to create a free user account at freesound.org in order to enable downloads.")
        .append(" Also, these files can be somewhat large, a few megabytes in size, and therefore it is recommended to download them when on wifi.")

        .append("<p/>http://www.freesound.org/people/aguasonic/sounds/");

        buf.append("<p/>You can also use other sound apps that can share recorded files with this app, often in a similar fashion to using Chrome for sharing.");

        buf.append("<p/>The URL to play audio files can be used in two ways: you can type (or copy-paste) the URL in the widget or you can");
        buf.append(" get another app to send an Intent with the appropriate characteristics, and the Spectrogram");
        buf.append(" app will pick it up and play the URL sent by the third party app.");

        buf.append("<p/>For copy-pasting a URL to the widget, ")
        .append("here is an example with the new WHOI Watkins Marine Mammal Sound database at ")

        .append("<p/>http://cis.whoi.edu/science/B/whalesounds/bestOf.cfm?code=BD15F")

        .append("<p/>The above link is the page for the Stenella frontalis sounds.")

        .append("<p/>Steps to play and view the spectrogram:")
        .append("<br>- Ensure that the device output sound level is not zero and sufficient")
        .append(", usually with the sound buttons on the side of the device.")
        .append("<br>- You open the above page with your device Chrome browser,")
        .append("<br>- you long-press the *Download* link for a recording,")
        .append("<br>- you select the *Copy link address* option,")
        .append("<br>- you open or go to the sm Spectrogram app,")
        .append("<br>- if you are viewing this text, then hide it by tapping the ABOUT button,")
        .append("<br>- you tap the URL widget and select *Paste*, ")
        .append("and the link from the clipboard will be written by Android onto the URL widget,")
        .append("<br>- if there was a previous link in the URL widget, ensure that it is entirely replaced by the new link,")
        .append("<br>- you tap the *Play* button, wait a few seconds for the file to get accessed and played,")
        .append(" and you should hear the sound and see the spectrogram.");
        buf.append("<br>- You may want to pause the app in order to better observe the spectrogram being displayed.");
        buf.append("<br>- You can replay the file as many times as needed.");

        buf.append("<p/>The images on this NOAA page below are linked to wav files that are played when you select one.")
        .append(" Using a browser you may copy-paste the link to the wav files into the url text widget in the app.")
        .append(" One way of doing this is to use Chrome to long-press an image and then select the *Copy link address* option,")
        .append(" and then go to the app, touch the url text field and paste the link from the clipboard into the url text field.")

        .append("<p/>https://swfsc.noaa.gov/textblock.aspx?Division=PRD&ParentMenuId=148&id=5776")
        ;

        buf.append("<p/>Here is a Google & Chrome-produced list of other sound sources on the web; some of these may not be compatible with this app.");

        buf.append("<p/>https://www.google.ca/webhp?sourceid=chrome-instant&ion=1&espv=2&ie=UTF-8#q=cetacean%20sound%20library");

        buf.append("<p/><p/>For sharing a file from another app, ")
        .append("most apps sending Intent instances for audio file sharing should normally be compatible with this app")
        .append(" and the user only needs to perform the ordinary sharing-file functions of the Android system.");

        buf.append("<p/>For developing an app that can send an Intent for sharing with this app, a code example is:");
        buf.append("<p/>");
        buf.append("<tt>");
        buf.append("// Example:");
        buf.append("<br>String encoded = null;")
        .append("<br>try {")
        .append("<br>&nbsp;&nbsp;encoded = URLEncoder.encode(")
        .append("<br>&nbsp;&nbsp;&nbsp;&nbsp;\"http://www.wavsource.com/snds_2016-02-14_1408938504723674/animals/dog_whine_duke.wav\",")
        .append("<br>&nbsp;&nbsp;&nbsp;&nbsp;\"UTF-8\");")
        .append("<br>} catch (UnsupportedEncodingException e) {")
        .append("<br>&nbsp;&nbsp;// manage the error here")
        .append("<br>}");
        buf.append("<br><br>Intent intent = new Intent(Intent.ACTION_SEND);");
        buf.append("<br><br>intent.setType(\"application/vnd.sm.app.spectrogram\");");
        buf.append("<br><br>intent.putExtra(Intent.EXTRA_TEXT, encoded);");
        buf.append("<br><br>// End of example");
        buf.append("</tt>");
        buf.append("<p/><p/>Important: The url in the Intent must be encoded with the URLEncoder in Android, and using the UTF-8 character set.");
        //buf.append("<p/>");
        //We published two simple apps to demonstrate this play-intent feature, TODO future demo apps text

        buf.append("<p/><p/><H3>KNOWN ISSUES</H3>");
        buf.append("<p/>After taking a screen shot, the app will restart and the previous image will be lost.")
        .append(" This will be improved in a future version.")
        .append("<p/>Re-orienting your device, e.g., from vertical to horizontal, will cause the app to restart the display in order to use the new image dimensions")
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
        if (AppConfig.THREADS_LOG_ENABLED || AppConfig.RESTORE_LOG_ENABLED)
            Log.d(TAG, ".onResume: entering");
        if (isPausedByHUser) {
            //don't restart bg threads
            if (AppConfig.PAUSE_LOG_ENABLED || AppConfig.THREADS_LOG_ENABLED
                    || AppConfig.RESTORE_LOG_ENABLED)
                Log.d(TAG, ".onResume: isPausedByHUser, don't restart " + Thread.currentThread());
        } else {
            //isPausedByHUser = false
            //was not paused by H user, then restart if paused by system
            if (isPaused()) {
                // is paused, listener is null
                if (AppConfig.PAUSE_LOG_ENABLED || AppConfig.THREADS_LOG_ENABLED
                        || AppConfig.RESTORE_LOG_ENABLED)
                    Log.d(TAG, ".onResume: before unpause() "
                            + Thread.currentThread());
                try {
                    //============
                    unpause();
                    //============
//                    pauseButton.setText(getString(R.string.pause_button));
                } catch (Exception e) {
                    if (AppConfig.PAUSE_LOG_ENABLED || AppConfig.ERROR_LOG_ENABLED
                            || AppConfig.THREADS_LOG_ENABLED || AppConfig.RESTORE_LOG_ENABLED)
                        Log.e(TAG, "onResume: unpause() raised " + e + " " + Thread.currentThread());
                }
                if (AppConfig.PAUSE_LOG_ENABLED || AppConfig.THREADS_LOG_ENABLED
                        || AppConfig.RESTORE_LOG_ENABLED)
                    Log.d(TAG, ".onResume: after unpause() "
                            + Thread.currentThread());
            } else {
                // is not paused
                if (AppConfig.PAUSE_LOG_ENABLED || AppConfig.THREADS_LOG_ENABLED
                        || AppConfig.RESTORE_LOG_ENABLED)
                    Log.d(TAG, ".onResume: not paused, do nothing " + Thread.currentThread());
            }
        }

        // restore url from pref done in onCreateComplete
    }

    /**
     * Designed to be done very early in the activity startup step.
     */
    private void restorePreferences() {
        if (AppConfig.LOG_ENABLED)
            Log.d(TAG, ".restorePreferences: entering...");

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (AppConfig.LOG_ENABLED)
            Log.d(TAG, ".restorePreferences: prefs = " + prefs);

        SettingsForSoundPreferences.restoreInputSettings(prefs);

        restoreUrlToPlay(prefs);

        if (AppConfig.LOG_ENABLED)
            Log.d(TAG, ".restorePreferences: exiting...");
    }

    private void restoreUrlToPlay(final SharedPreferences prefs) {
        urlToPlay = prefs.getString(PREF_URL_KEY, "");
        if (AppConfig.RESTORE_LOG_ENABLED)
            Log.d(TAG, ".restoreUrlToPlay: urlToPlay {" + urlToPlay + "}");
//        if(urlToPlay!=null){
//            if(editTextUrlToPlay!=null)editTextUrlToPlay.setText(urlToPlay);
//        }
        urlToPlayFromPref = urlToPlay;
    }

    private void onExceptionAtInit(Throwable ex) {
        String s = "" + ex; //ex.getLocalizedMessage() + "\n";
        if (AppConfig.ERROR_LOG_ENABLED || AppConfig.THREADS_LOG_ENABLED) {
            s += "\n" + Log.getStackTraceString(ex);
            Log.e(TAG, ".onExceptionAtInit: " + s + " " + Thread.currentThread());
        }
        if (urlPrepareSnackbar != null) urlPrepareSnackbar.dismiss();
        showAnomalyText(ex, "Error detected at startup");
    }

    /**
     *
     * @return true when on real device or false when on emulator.
     */
    private boolean setOnRealDeviceOrEmulator() {

        String onEmulatorText = null;

        if (AppConfig.getIt().isOnRealDevice()) {
            //license check is disabled
            //---------------------------------------------------------------------
//				final boolean licenseCheckStarted = startCheckLicense(
//						Installation.getID(this));
            //---------------------------------------------------------------------
//				if (LeafyLog.SMARTPLAYER_LOG_ENABLED||LeafyLog.INIT_LOG_ENABLED){
//					Log.d(TAG + ".onCreateComplete", "on real device; licenseCheckStarted = "
//							+ licenseCheckStarted);
//					if(! licenseCheckStarted){
//						Log.d(TAG + ".onCreateComplete", "on real device; no license check in this session; previous license check results {"
//								+ OnAnyThread.getLicenseCheckResults(this)+"}");
//					}
//				}
        } else {
            onEmulatorText = "on emulator, not real device";
        }

//        if (onEmulatorText != null) {
//            writeInMonitor(onEmulatorText);
//        }
        return onEmulatorText == null;
    }

    /**
     * @return false when device does not support sound input
     * @throws Exception
     */
    private boolean setDeviceSoundCapabilities() throws Exception {

        //includes sound configs kept in DeviceSoundCapabilities
        //============================================================
        DeviceSoundCapabilities.initFundamentalsForDevice(this);
        //============================================================

        if (!DeviceSoundCapabilities.isDeviceCapableOfSoundInput()) {
            if (AppConfig.INIT_LOG_ENABLED || AppConfig.SOUND_INPUT_INIT_LOG_ENABLED)
                Log.d(TAG, ".setDeviceSoundCapabilities: device cannot do sound input");
            return false;
        }

        SettingsForSoundInput.dependentsOnVoltageSamplingRate(
                DeviceSoundCapabilities.getSelectedVoltageSamplingForInput());//does not depend on preferences in this version; depends on device capabilities

        //writeInMonitor(DeviceSoundCapabilities.getFundamentalsForDisplay());

        //writeInMonitor(DeviceSoundCapabilities.getForDisplay());

//			writeInMonitor(DeviceSoundCapabilities.getForDisplay(
//			    true,false,false)); //LeafyLog.SOUND_QUALITY_LOG_ENABLED)); //getDeviceAudioCapabilities();//2014-11

        if (AppConfig.INIT_LOG_ENABLED || AppConfig.SOUND_INPUT_INIT_LOG_ENABLED)
            Log.d(TAG, ".setDeviceSoundCapabilities: " + Timestamp.getNanosForDisplay()
                + "; first part of initFundamentalsForDevice completed ok;" +
                "\n xInputHzPerBinFloat = " + SettingsForSoundInput.xInputHzPerBinFloat
                + "\n getSelectedVspsInputInt = " + DeviceSoundCapabilities.getSelectedVspsInputInt()
                //+ "\n cTimeIncSecDouble = " + Settings.cTimeIncSec
                + "\n MAX_PCM_ADJUSTED_FORMAT = " + SoundQualitySettings.MAX_PCM_ADJUSTED_FORMAT
                + " is close to the maximum value for a pcm value " +
                "(out of the A/D subsystem, or as input to the D/A subsysstem)" +
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
     * @param ex
     * @return String "A severe anomaly was detected " with text param
     * <!-- and with exception if any. -->
     */
    static String getMonitorTextForAnomalyNotif(final String text, final Throwable ex) {
        return "A severe anomaly was detected " + text
                + (main == null && text != null
                && (!text.contains("main") && !text.contains("appParent"))
                ? "; and attribute *appParent* (or *main*) is null"
                : "")
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
     * if de. email enabled and networked, then try to send email to dev;
     * if de. email not enabled or no network, then write Toast (?).
     * <p/>
     * This adds prefix "A severe anomaly was detected " to the text param.
     * <p/>
     * convenience method.
     *
     * @param text
     * @deprecated not used in this version
     */
    public static void notifyForAnomaly(final String text) {
        notifyForAnomaly(text, null);
    }

    /**
     * if de. email enabled and networked, then try to send email to de. and write in monitor and console (short msg);
     * <p/>if de. email not enabled or no network, then write in monitor and console (short msg) and to file.
     *
     * <p/>This adds prefix "A severe anomaly was detected " to the text param.
     *
     * @param text
     * @param ex   Throwable, may be null.
     * @deprecated not used in this version
     */
    public static void notifyForAnomaly(final String text, final Throwable ex) {

        if(AppConfig.SOUND_QUALITY_LOG_ENABLED || AppConfig.ERROR_LOG_ENABLED) {
            Log.e(TAG, "notifyForAnomaly: AppConfig.getIt().isSupportEmailEnabled() "
                    + AppConfig.getIt().isSupportEmailEnabled()
                    //+"; Settings.anomalyReportToFileIsEnabled "+Settings.anomalyReportToFileIsEnabled
                    + "\n" + text);
        }

        if(USER_LOG_INIT && main!=null){
            main.showStatusSnackbar(text);
        }

        final String detailsForEmail = getAnomalyDetailsForEmail(ex);

        final String monitorText = getMonitorTextForAnomalyNotif(text, ex);

        final String consoleMsg = "Anomaly detected, please consult app messages for details";

        final String all = monitorText + "\n\n" + detailsForEmail;

        if (AppConfig.getIt().isSupportEmailEnabled() && main != null
                && OnAnyThread.IT.isConnected(main, AppConfig.getIt().isSimulatingNoConnection())) {
            //try to send email to de. and write in monitor and console (short msg referring to monitor, app msgs);

            if (main != null) {
                main.sendEmailToSupport(all);
//                appParent.writeInConsoleByApp(consoleMsg);
//                writeInMonitor(monitorText);
            } else {
                //here appParent is null, can only write to file (if room and allowed)
//                writeAnomalyReportToFileNoException(all);
            }

            //rules for adding the stack trace to monitor
//            if (ex!=null && ! AppConfig.getIt().isSupportEmailEnabled()){ // && ! Settings.anomalyReportToFileIsEnabled) {
//                writeInMonitor(LeafyLog.getStack(ex));
//            }

            return;
        }

        //here no de. email or no network or appParent not set;
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

    /**
     * writes results in texts views for user to see
     *
     * @param statusCode
     * @param text
     */
    @Override
    public void results(int statusCode, String text) {
        if(AppConfig.LOG_ENABLED)
            Log.d(TAG, ".results: statusCode " + statusCode + ": " + text);
        if(USER_LOG_INIT) {
            showStatusSnackbar( "results: statusCode " + statusCode + ": " + text );
        }
    }

    /**
     * Designed to write results from background threads, such as recognition.
     *
     * @param statusCode int
     * @param text       signal name or other text
     * @param millis     long TODO future maybe remove, the method will get it from the system
     * @param user       String ID; ex.: H, C, A
     * @param isNewLine  boolean; true will force a new line to be started
     */
    @Override
    public void results(int statusCode, String text, long millis, String user, boolean isNewLine) {
        if(USER_LOG_INIT) {
            String s = "results: statusCode " + statusCode + ": " + text + "; millis " + millis
                    + "; user " + user + "; isNewLine " + isNewLine;
            showStatusSnackbar(s);
            if(AppConfig.LOG_ENABLED)
                Log.d(TAG, "."+s);
        }
    }

    @Override
    public boolean isPaused() {//TODO future review with preserve bg image when app interrupted
        if (DeviceSoundCapabilities.getFundamentalsInitialized()) {
            //initialized, then depends on listener being null or not
            return !BasicListener.isRunning();
        }
        //not initialized, then not paused
        return false;
    }

    @Override
    public void unpause() throws Exception {
        getListener();
    }

//    private void getListenerWithNotif() {
//        try {
//            if (getListener() == null) {
//                Toast.makeText(this, "The listener failed to start", Toast.LENGTH_LONG).show();
//                // show error in text view
//                showFailureInMethod(null, "BasicListener.getARunningListener");
//            }
//        }catch (Exception e){
//            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
//            // show error in text view
//            showFailureInMethod(e, "BasicListener.getARunningListener");
//        }
//    }

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
//                if(AppConfig.PAUSE_LOG_ENABLED)
//                    Log.d(TAG, ".startListener(): *listener.startSoundInput(this)* completed ok.");
//            } catch (Exception e) {
//                if(AppConfig.SOUND_INPUT_INIT_LOG_ENABLED || AppConfig.ERROR_LOG_ENABLED)
//                    Log.e(TAG,".startListener(): *BasicListener.getARunningListener(this)* raised "+e.getMessage()
//                            +"\n"+DeviceSoundCapabilities.getFundamentalsSummaryForDisplay());
//                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
//                // show error in text view
//                showFailureInMethod(e, "BasicListener.getARunningListener");
//            }
//        } //sync
//        return listener;
//    }

    @Override
    public String getHUser() {
        return "H";
    }

    @Override
    public String getNonHUser() {
        return "X";
    }

    /* *
     * 44,100 vsps may have less latency than 48,000 vsps on many devices
     *
     * @return boolean; when true, 48,000 vsps will be used only if it is the only supported rate
     * above 8000 and we are not on an emulator, i.e., if
     * 44,100 and 96,000 are not available.
     */
//    @Override
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
//    @Override
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
//    @Override
//    public boolean isChannelMonoRequested() {
//        return true;
//    }

//    public boolean isMicPreferred(){ return true; }

    /* TODO new pref
    - for mic, offer to user all choices supported by the device and give the app one of the options
    future:
    - for channel, mono or stereo, same as above
    - for float pcm encoding, same as above, float/int for input, float/int for output, depending on version of android
    - for sampling rate, offer to user the native rate and some other rates supported by device, and let user pick one
      and let user decide for same encoding for input and output

      TODO keep list of urls to play, give them names, delete, move up/down, export list (share)
     */

    public SoundClientPreferences getSoundClientPreferences(){
        SoundClientPreferences soundPrefs = new SoundClientPreferences();
        soundPrefs.isMicPreferred = true;//TODO washere washere washere 2016-9 2017-5-6 true or false don't fix the lack of input or display
        soundPrefs.isChannelMonoRequested = true;
        soundPrefs.isEncodingPcmFloatPreferred = false;//TODO future true with new code for float processing
        soundPrefs.isNativeSampleRateRequested = true;
        soundPrefs.isSameEncodingPcmForInputAndOutputRequested = false;
        if(AppConfig.LOG_ENABLED){
            Log.d(TAG,".getSoundClientPreferences: isMicPreferred {"+soundPrefs.isMicPreferred
            +"} isChannelMonoRequested {"+soundPrefs.isChannelMonoRequested
            +"} isEncodingPcmFloatPreferred {"+soundPrefs.isEncodingPcmFloatPreferred
            +"} isNativeSampleRateRequested {"+soundPrefs.isNativeSampleRateRequested
            +"} isSameEncodingPcmForInputAndOutputRequested {"+soundPrefs.isSameEncodingPcmForInputAndOutputRequested
            +"}");
        }
        if(USER_LOG_INIT){
            showStatusSnackbar("SoundClientPreferences were set");
        }
        return soundPrefs;
    }

    /**
     * @return float volume percent
     */
    @Override
    public float notifyVolume() {
        return 0;
    }

    /**
     * not used in this version
     *
     * @param tf boolean; emission success status; true or false.
     */
    @Override
    public void emissionCompletionStatus(boolean tf) {
    }

    /**
     * Not used in this app.
     *
     * @return null for EmitterGrandParent
     */
    @Override
    public EmitterGrandParent getEmitter() {
        return null;
    }


    private final Object LOCK_FOR_LISTENER = new Object();

    //private final ReentrantLock REL_FOR_LISTENER = new ReentrantLock();

    /**
     * The listener is started by calling {@code listener.startSoundInput(clientApp)} in
     * {@code BasicListener.getARunningListener()}.
     *
     * @return A BasicListener which is started. Is null when failure.
     * @throws Exception
     */
    public BasicListener getListener() throws Exception {
        //REL_FOR_LISTENER.hasQueuedThreads();
        synchronized (LOCK_FOR_LISTENER) {
            try {
                enableThePauseButton("Pause");
                BasicListener listener = BasicListener.getARunningListener(this);
                if (listener == null) {
                    disableThePauseButton(null);
                    showProblemStartingListener();
                }
                return listener;
            } catch (Exception e) {
                disableThePauseButton(null);
                showProblemStartingListener();
                if (AppConfig.ERROR_LOG_ENABLED)
                    Log.e(TAG, ".getListener: " + e + " " + Log.getStackTraceString(e));
                throw e;
            }
        }//sync.
    }

    private void showProblemStartingListener() {
        if (largeGuiLayout != null) {
            Snackbar.make(largeGuiLayout, "The sound listener cannot be started", Snackbar.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "The listener failed to start", Toast.LENGTH_LONG).show();
        }
        // show error in text view
        showFailureInMethod(null, "BasicListener.getARunningListener");
    }

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
//                if(AppConfig.PAUSE_LOG_ENABLED)
//                    Log.d(TAG, ".startListener(): *listener.startSoundInput(this)* completed ok.");
//            } catch (Exception e) {
//                if(AppConfig.SOUND_INPUT_INIT_LOG_ENABLED || AppConfig.ERROR_LOG_ENABLED)
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
        BasicListener.shutdownTheRunningListener();
    }

    @Override
    public Object getAndroidActivityContext() {
        return this;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeListener();
        if (player != null) player.shutdown();
    }

    @Override
    protected void onRestart() { //matched with onStop
        super.onRestart();
        // start listener if listener not running
        try {
            getListener();
        } catch (Exception e) {
            //done upstream: Log.e(TAG,"Problem with the sound listener: "+e+" "+Log.getStackTraceString(e));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePrefs();
        isPausedByHUser = false;
        if (AppConfig.PAUSE_LOG_ENABLED || AppConfig.THREADS_LOG_ENABLED)
            Log.d(TAG, ".onPause: prefs saved; calling closeListener() " + Thread.currentThread());
        closeListener();
        if (player != null) player.shutdown();//TODO should we null the player?
        if (urlPrepareSnackbar != null) urlPrepareSnackbar.dismiss();
    }

    private void savePrefs() {

        saveUrl();

        //TODO future more saving e.g. audio settings when user will have some choices

    }

    private void saveUrl() {//TODO future maybe also call this when url from intent at onCreate
        //save url to pref
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (editTextUrlToPlay != null) {
            urlToPlay = editTextUrlToPlay.getText().toString();
            if (urlToPlay == null) {
                urlToPlay = "";
            } else {
                urlToPlay = urlToPlay.trim();
            }
            //save url, even if empty
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_URL_KEY, urlToPlay);
            editor.commit();
            if (AppConfig.RESTORE_LOG_ENABLED || AppConfig.SAVE_PREF_LOG_ENABLED) {
                Log.d(TAG, ".saveUrl: urlToPlay {" + urlToPlay
                        + "} PREF_URL_KEY {" + PREF_URL_KEY + "}");
            }
        }
    }

    private boolean isActionSendIntent(final Intent intent) {
        final String action = intent.getAction();
        return Intent.ACTION_SEND.equals(action);
    }

    /**
     * not empty if http link for wav, from clip from intent
     * <!-- maybe not needed -->
     *     @deprecated
     */
    private String clipDataItemText = "";

    /*
    ex.: {Intent { act=android.intent.action.SEND
    typ=text/plain
    flg=0x1b080001
    cmp=sm.app.spectrogram/.SpectrogramActivity
    clip={text/plain T:http://sounds.aguasonic.com/files/ag25feb015-from-trk07.wav} (has extras) }}
     */
    private boolean isInboundIntentToPlay(final Intent intent) {
        clipDataItemText = "";
        final String action = intent.getAction();
        String type = intent.getType();
        if (!Intent.ACTION_SEND.equals(action) || type == null) {
            if (AppConfig.LOG_INTENT)
                Log.d(TAG,".isInboundIntentToPlay: returning false; not SEND or type is null");
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

                    if (AppConfig.LOG_INTENT)
                        Log.d(TAG,"isInboundIntentToPlay: clipData item "+j+" of "+itemCount+": "
                                +itemAt
                            +"\nText {"+text+"}"
                            +"\nIntent {"+itemAt.getIntent()+"}"
                            +"\nUri {"+itemAt.getUri()+"}"
                            );
                    if(isContainingWavUrl(text)){
                        // this will return the last link if more than one
                        if (AppConfig.LOG_INTENT)
                            Log.d(TAG,"isInboundIntentToPlay: clipData item "+j+" of "+itemCount
                                +": clipDataItemText {"+clipDataItemText+"}");
                        clipDataItemText = text;
                        ok = true;
                        //return true; // this will return the first link
                    }
                }// for
                return ok;
            }else{
                // here when text enabled and version too old for clips

                // is there an http .wa. link in the intent?

                return isContainingWavUrl(intent.toString());
            }
        }else{
            // here when not text or is text and text is not enabled
        }
        if (INBOUND_INTENT_PLAY_TYPE_SPECIAL.equals(type) && INBOUND_INTENT_PLAY_TYPE_SPECIAL_ENABLED)
            return true;

        return false;
    }

    private boolean isContainingWavUrl(String text){
        String s = text.toLowerCase();
        int pos1 = s.indexOf("http");
        int pos2 = s.indexOf(".wav");
        return (pos1>=0 && pos2 > pos1);
    }

    /**
     * Designed to be called from or during onCreate, which would be called when another app sends
     * an intent that matches the filter(s) defined in the manifest.
     * The listener is started upstream.
     * The player uses callbacks onNormalEndOfPlay and onAnomalyDetectedByPlayer
     *
     * <p/>Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     *
     * @return boolean true when intent is valid; false when no valid intent present or when failure
     */
    private boolean handleIncomingIntent(final String urlFromPrefs) {
        if (AppConfig.INIT_LOG_ENABLED || AppConfig.SOUND_INPUT_INIT_LOG_ENABLED
                || AppConfig.LOG_INTENT || AppConfig.RESTORE_LOG_ENABLED
                || AppConfig.PLAY_URL_LOG_ENABLED) {
            Log.d(TAG, ".handleIncomingIntent: entering");
        }
        final Intent intent = getIntent();
        try {
            // ex.:
            // {Intent { act=android.intent.action.SEND typ=text/plain
            // flg=0x13080001 cmp=sm.app.spectrogram/.SpectrogramActivity
            // clip={text/uri-list
            // U:content://com.android.chrome.FileProvider/images/screenshot/1465670376229-1822593518.jpg} (has extras) }
            // ex.:
            // {Intent { act=android.intent.action.SEND typ=text/plain flg=0x1b080001
            // cmp=sm.app.spectrogram/.SpectrogramActivity
            // clip={text/plain
            // T:http://sounds.aguasonic.com/files/ag25feb015-from-trk07.wav} (has extras) }}

            if (!isInboundIntentToPlay(intent)) {
                // the app start intent not valid send intent;
                // could be normal start without an play intent or invalid send or play intent

                if (AppConfig.LOG_INTENT || AppConfig.PLAY_URL_LOG_ENABLED)
                    Log.d(TAG, ".handleIncomingIntent: not valid intent to play {" + intent + "}");
                if (isActionSendIntent(intent)) {
                    // the start intent is a send one from external app, and it's invalid
                    if (largeGuiLayout != null)
                        Snackbar.make(largeGuiLayout,
                                "The URL from an external app is invalid and is ignored",
                                Snackbar.LENGTH_LONG).show();
                } else {
                    // the start intent is not a send intent, so ignore, probably normal start intent
                }
                setUrlToPlayInUi(urlFromPrefs);
                return false;
            }

            //here when type ok for url to play and the function is enabled
            //ex.: "application/vnd.sm.app.spectrogram" or "text/plain"

            urlToPlay = getUrlToPlay(intent);
            setUrlToPlayInUi(urlToPlay);

            if (AppConfig.LOG_INTENT || AppConfig.PLAY_URL_LOG_ENABLED)
                Log.d(TAG, ".handleIncomingIntent: possibly valid intent to play;" +
                        " calling play(\nintent {" + intent
                        + "},\n urlToPlayUri {"+urlToPlayUri
                        + "},\n urlToPlay {"+urlToPlay+"})");

            // ===========================================================
            boolean intentIsValid = play(intent, urlToPlayUri, urlToPlay);
            // ===========================================================

            if (intentIsValid) {
                if (AppConfig.LOG_INTENT)
                    Log.d(TAG,".handleIncomingIntent: true returned by player.play(intent {"
                            + intent + "}, urlToPlayUri, urlToPlay)");
            } else {
                // the url from the intent is invalid
                if (AppConfig.LOG_INTENT || AppConfig.PLAY_URL_LOG_ENABLED)
                    Log.d(TAG, ".handleIncomingIntent: false returned by player.play(intent {"
                            + intent + "}, urlToPlayUri, urlToPlay)");
                invalidIntent(intent, urlToPlay);
                if(urlFromPrefs==null || urlFromPrefs.isEmpty()){
                    // no url from pref, use the invalid url in gui, set upstream

                }else {
                    // some url from pref, use it in gui
                    urlToPlay = urlFromPrefs;
                    setUrlToPlayInUi(urlFromPrefs);
                }
            }
            return intentIsValid;

        } catch (Throwable e) {
            if (AppConfig.ERROR_LOG_ENABLED)
                Log.e(TAG, ".handleIncomingIntent: " + e + "; " + Log.getStackTraceString(e));
            invalidIntent(intent, null);
            setUrlToPlayInUi("");
            return false;
        }
    }

    /**
     * Also sets attributes urlToPlayUri (can be null), urlToPlayRaw (can be null),
     * and urlToPlayDecoded (can be empty).
     *
     * <p/>For an intent with a plain text URL, the returned value is not empty
     * and urlToPlayUri is null.
     *
     * @param intent
     * @return urlToPlayDecoded, can be empty
     * @throws UnsupportedEncodingException
     */
    private String getUrlToPlay(final Intent intent) throws UnsupportedEncodingException {
        urlToPlayRaw = null;
        urlToPlayUri = null;
        urlToPlayDecoded = "";
        //TODO washere washere spectro bug this does not work for extra_stream for type AUDIO_*
        urlToPlayRaw = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (AppConfig.LOG_INTENT)
            Log.d(TAG, ".handleIncomingIntent: urlToPlayRaw from Intent.EXTRA_TEXT {"
                    + urlToPlayRaw + "}");
        if (urlToPlayRaw == null) {
            //no text url, try extra_stream
            urlToPlayUri = getUri(intent);

            if (urlToPlayUri != null) {
                // url is uri, from extra stream or getData

                urlToPlayRaw = urlToPlayUri.toString();
            } else {
                //urlToPlayRaw is null; urlToPlayUri is null
            }
        } else {
            //urlToPlayRaw is not null; urlToPlayUri is null
        }
        if (urlToPlayRaw != null) {
            urlToPlayDecoded = URLDecoder.decode(urlToPlayRaw, "UTF-8");
        }
        return urlToPlayDecoded;
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

    private void invalidUrl(final String urlString) {
        if (AppConfig.LOG_INTENT)
            Log.d(TAG, ".invalidUrl: invalid URL text {" + urlString + "}");
        if (largeGuiLayout != null)
            Snackbar.make(largeGuiLayout,
                    "The URL is invalid and is ignored: " + urlString,
                    Snackbar.LENGTH_LONG).show();
    }

    private void invalidIntent(final Intent intent, final String invalidUrl) {
        if (AppConfig.LOG_INTENT)
            Log.e(TAG, ".invalidIntent: invalid intent {" + intent + "}");
        String x = invalidUrl == null || invalidUrl.isEmpty() ? "" : ": " + invalidUrl;
        if (largeGuiLayout != null) {
            String s = "The URL from another app is invalid and is ignored" + x;
            if (intent == null) {
                // url for a local file
                s = "The URL is invalid and is ignored" + x;
            }
            Snackbar.make(largeGuiLayout, s, Snackbar.LENGTH_LONG).show();
        }
    }

    private void showError(final String text) {
        if (AppConfig.LOG_INTENT || AppConfig.LOG_ENABLED || AppConfig.ERROR_LOG_ENABLED)
            Log.e(TAG, ".showError {" + text + "}");
        if (largeGuiLayout != null) {
            Snackbar.make(largeGuiLayout, text, Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Designed to be run on the ui thread.
     * All callers are run on the ui thread in this version.
     * Used in this version.
     *
     * @param intent references a remote media file with http or rtsp URL
     * @param uri    the Uri from the intent
     * @return true when succeeded, false when failure.
     */
    private boolean play(final Intent intent, final Uri uri, final String urlToPlayGiven) {
        if (AppConfig.LOG_INTENT || AppConfig.PLAY_URL_LOG_ENABLED)
            Log.d(TAG, ".play: trying to play intent for remote file {" + intent
                    + "} uri {" + uri + "} urlToPlayGiven {" + urlToPlayGiven + "}");
        //urlToPlay = urlToPlayDecoded;
        editTextUrlToPlay.setText(urlToPlayGiven);
        // ======================
        return doPlayUrl(intent);
        // ======================
    }

//    private boolean play(final Intent intent, final String urlToPlayGiven){
//        if (AppConfig.LOG_INTENT)
//            Log.d(TAG, ".play: trying to play intent for remote file {" + intent
//                    + "} urlToPlayGiven {"+urlToPlayGiven+"}");
//        //urlToPlay = urlToPlayDecoded;
//        editTextUrlToPlay.setText(urlToPlayGiven);
//        // ======================
//        return doPlayUrl(intent);
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
//        if (AppConfig.LOG_INTENT)
//            Log.d(TAG, ".playRemoteFile: url is text; urlToPlayDecoded {"
//                    + urlToPlayDecoded + "}");
//
//        if (urlToPlayDecoded != null && !urlToPlayDecoded.isEmpty()) {
//            // external url or uri to play may be ok, try it
//            if (AppConfig.LOG_INTENT || AppConfig.PLAY_URL_LOG_ENABLED)
//                Log.e(TAG, ".playRemoteFile: trying to play intent with text URL to remote file {"
//                        + intent + "}");
//            urlToPlay = urlToPlayDecoded;
//            editTextUrlToPlay.setText(urlToPlay);
//            // ======================
//            return doPlayUrl(intent);
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
//        if (AppConfig.LOG_INTENT)
//            Log.d(TAG, ".playLocalFile: entering with filepath {" + filepath + "}");
//
////      Try to open the file for "read" access using the
////      returned URI. If the file isn't found, write to the error log and return.
//
//        FileDescriptor fd = getFileDescriptorForPrivateFileWithRetries(filepath);
//
//        if (fd == null) {
//            if (AppConfig.LOG_INTENT)
//                Log.d(TAG, "playLocalFile: File cannot be opened {" + filepath + "}");
//            showError("File cannot be opened");
//            return false;
//        }
//
//        if (AppConfig.LOG_INTENT)
//            Log.d(TAG, "playLocalFile: File opened!!! {" + filepath + "} descriptor {" + fd + "}");
//
//        boolean success = false;
//        try {
//            // =========================
//            success = doPlayUrl(fd, intent); // ===> mediaPlayer.setDataSource(fd)
//            // =========================
//        } finally {
////            try {
////                pfd.close();
////            } catch (IOException ignore) {
////                if (AppConfig.LOG_INTENT||AppConfig.ERROR_LOG_ENABLED)
////                    Log.e(TAG, "playLocalFile: File found: {"+filepath+"}, descriptor {"+fd+"}"
////                    +" IOException "+ignore+" "+Log.getStackTraceString(ignore));
////            }
//        }
//        return success;
//    }

    /**
     * @param filepath without file separator
     * @return FileDescriptor or null
     */
    private FileDescriptor getFileDescriptorForPrivateFile(final String filepath) {
        FileDescriptor fd = null;
        try {
            fd = openFileInput(filepath).getFD();
        } catch (IllegalArgumentException e) {
            if (AppConfig.LOG_INTENT || AppConfig.ERROR_LOG_ENABLED)
                Log.e(TAG, "getFileDescriptorForPrivateFile: File {" + filepath + "}"
                        + " IOException " + e + " " + Log.getStackTraceString(e));
            //showError("File cannot be opened");
            return null;
        } catch (IOException e) {
            if (AppConfig.LOG_INTENT || AppConfig.ERROR_LOG_ENABLED)
                Log.e(TAG, "getFileDescriptorForPrivateFile: File {" + filepath + "}"
                        + " IOException " + e + " " + Log.getStackTraceString(e));
            //showError("File cannot be opened");
            return null;
        }
        return fd;
    }

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
//            if (AppConfig.LOG_INTENT)
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

    /**
     * @param ex     Throwable
     * @param method a text fragment, example: "mediaPlayer.prepare"
     */
    private void showFailureInMethod(final Throwable ex, final String method) {
        if (urlPrepareSnackbar != null) urlPrepareSnackbar.dismiss();
        showAnomalyText(ex, method + " raised " + ex);
    }

    //TO DO when is this reset to null after it is set to non-null?
    private volatile String previousAnomalyText = null;
    private volatile boolean lastAnomalyTextIsShown = false;

    /**
     * Designed to be used in runOnUiThread.
     */
    private final Runnable RUNNABLE_TO_SHOW_ANOMALY_TEXT = new Runnable() {
        @Override
        public void run() {
            if (AppConfig.RESTORE_LOG_ENABLED || AppConfig.ERROR_LOG_ENABLED)
                Log.e(SpectrogramActivity.TAG, getLastAnomalyTextInHtml());
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

//    /**
//     * Button label changed to "Error" and text shown to user
//     */
//    private void showAnomalyText() {
//        runOnUiThread(RUNNABLE_TO_SHOW_ANOMALY_TEXT);
//    }

    /**
     * @param e    may be null
     * @param text
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
        @Override
        public void run() {
            updateAboutButtonOnUIThread();
        }
    };

    private void updateAboutButtonOnUIThread() {
        if (lastAnomalyText == null || lastAnomalyText.length() == 0) {
            if (about != null) {
                about.setText("About");//TODO use res
                if (AppConfig.UI_LOG_ENABLED)
                    Log.d(TAG,"updateAboutButtonOnUIThread: about button label set to *About*");
            }
        } else {
            if (about != null) {
                about.setText("Error");//TODO use res
                if (AppConfig.UI_LOG_ENABLED)
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
        @Override
        public void run() {
            if (urlPrepareSnackbar != null) urlPrepareSnackbar.dismiss();
        }
    };

    @Override
    public void onStartingToPlay() {
        runOnUiThread(RUNNABLE_FOR_PLAYER_STARTING_TO_PLAY);
    }

    volatile String textForEndOfPlay = "Normal end of play";

    private final Runnable RUNNABLE_FOR_PLAYER_NORMAL_END = new Runnable() {
        @Override
        public void run() {
            if (urlPrepareSnackbar != null) urlPrepareSnackbar.dismiss();
            Snackbar.make(largeGuiLayout, textForEndOfPlay,
                    Snackbar.LENGTH_LONG).show();
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

    private final Runnable RUNNABLE_FOR_ANOMALY_IN_PLAYER = new Runnable() {
        @Override
        public void run() {
            doCancelUrl();
            if (urlPrepareSnackbar != null) urlPrepareSnackbar.dismiss();
            editTextUrlToPlay.setTextColor(Color.RED);
            showAnomalyText(throwableFromPlayer, errorMessageFromPlayer);
            if (largeGuiLayout != null)
                Snackbar.make(largeGuiLayout, errorMessageFromPlayer, Snackbar.LENGTH_LONG)
                        .show();
        }
    };

    private volatile Throwable throwableFromPlayer = null;
    private volatile String errorMessageFromPlayer = null;

    private volatile boolean isCancelling = false;

    /**
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
        @Override
        public void run() {
            doCancelUrl();
            if (urlPrepareSnackbar != null) urlPrepareSnackbar.dismiss();
            editTextUrlToPlay.setTextColor(Color.GRAY);
            String s = "Audio focus refused by Android; try again later";
            //showAnomalyText(null, s);
            if (largeGuiLayout != null)
                Snackbar.make(largeGuiLayout, s, Snackbar.LENGTH_LONG)
                        .show();
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

    private long previousBackButtonMs = 0;
    public static final long BACK_BUTTON_TIMEOUT_MS = 5000;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            //Log.d(this.getClass().getName(), "back button pressed");
            String s = "";
            if (contentTextView.isShown()) {
                s = "; or to hide the text, tap the applicable text button";
            }
            if (previousBackButtonMs > 0) {
                //there is a previous back button press time; this is the second back-key
                if (System.currentTimeMillis() - previousBackButtonMs > BACK_BUTTON_TIMEOUT_MS) {
                    // timeout exceeded, reset previous one; no exit
                    previousBackButtonMs = System.currentTimeMillis();
                    //if a text is showing, add  s = "; to hide the text, touch the text button"
                    Snackbar.make(largeGuiLayout,
                            "Please press the Back key again shortly to confirm exiting the app" + s,
                            Snackbar.LENGTH_LONG)
                            .show();
                    // eat the action and do not propagate it
                    return true;
                } else {
                    // timeout not exceeded, then exit; propagate the action
                    finish();
                }
            } else {
                // there is no previous back button press time; no exit
                previousBackButtonMs = System.currentTimeMillis();
                Snackbar.make(largeGuiLayout,
                        "Please press the Back key again shortly to confirm exiting the app" + s,
                        Snackbar.LENGTH_LONG)
                        .show();
                // eat the action and do not propagate it
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

//    /**
//     * Disable app restart and the loss of data (url) when config changes,
//     * in coordination with THE manifest.xml file.
//     *
//     * @param config
//     */
//    @Override
//    public void onConfigurationChanged(Configuration config){
//        super.onConfigurationChanged(config);
//
////        restoreUrlToPlay();
////        if(editTextUrlToPlay!=null)
////            editTextUrlToPlay.setText(urlToPlay);
////        if(AppConfig.RESTORE_LOG_ENABLED){
////            Log.d(TAG,".onConfigurationChanged: urlToPlay {"+urlToPlay+"}");
////        }
//    }

    public boolean isDevMode() {
        return AppConfig.getIt().isDevMode();
    }

    @Override
    public void onStart() {
        super.onStart();

//        if(APP_INDEXING_IS_ENABLED) {//TODO future enable
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

    @Override
    public void onStop() {
        super.onStop();

        if(player!=null)player.shutdown();

//        if(APP_INDEXING_IS_ENABLED) { // TODO future enable
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
//        }
    }

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
//     * TO DO bug make sure that it does not write into an edittext view
//     */
//    private void tempHackForCpuAtFullSpeed() {
//        Timer touchTimer = new Timer();
//        touchTimer.schedule(sendFakeTouch, 1000, 1000);
//    }

}
