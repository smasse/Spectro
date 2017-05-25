package sm.app.spectro;

import sm.leafy.util.forandroid.AppConfigForAndroidGrandParent;

/**
 * Not a child of AppConfigParent but a child of AppConfigForAndroidGrandParent,
 * because AppConfigParent is in a component which is not involved here.
 *
 * <p>Example usage: AppConfig.getIt().isDevMode()</p>
 *
 * <p>Created by SM on 5/25/2015.</p>
 */
public final class AppConfig extends AppConfigForAndroidGrandParent {

    private AppConfig(){
        super();
        appType = APP_TYPE_FREE;
    }

    private final static AppConfig appConfig = new AppConfig();

    public static AppConfig getIt(){
        return appConfig;
    }

    /**
     * Important for testing ads and in-app-purchase iab;
     * used for logging and for setting the test device id when setting up ads
     * to avoid clicking on real ads which is forbidden by google.
     *
     * <p/>Return false when building a production version.
     */
    public boolean isDevMode() {
        return false;
    }

    public boolean isSimulatingPermissionGranted(){
        return false;
    }

    public boolean isAdsCapable(){
        return false;
    }

    public boolean isSupportEmailEnabled(){
        return true;
    }

    public boolean isSignalEmissionCapable(){
        return false;
    }

    public boolean isSoundOutputCapable(){
        return true;
    }

    /**
     * should be in English because normally sent to developer.
     *
     * @return String in English.
     */
    public String getSupportEmailSubject(){
        return "User requesting support";
    }

}
