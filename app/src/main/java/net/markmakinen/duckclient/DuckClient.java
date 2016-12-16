package net.markmakinen.duckclient;

import android.app.Application;
import android.util.Log;

import net.danlew.android.joda.JodaTimeAndroid;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Zini on 16.12.2016 19.45.
 */

public class DuckClient extends Application {

    public static final String BACKEND_ADDRESS = "http://192.168.11.3:8081/";

    public static URI backendURI;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            backendURI = new URI(BACKEND_ADDRESS);
        } catch (URISyntaxException e) {
            // Init fails, as the backend address is invalid
            Log.e("DuckClient", "Invalid backend address!");
            return;
        }
        JodaTimeAndroid.init(this); // Initialize JodaTime (our datetime lib)
    }

}
