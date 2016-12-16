package net.markmakinen.duckclient;

import android.app.Application;

import net.danlew.android.joda.JodaTimeAndroid;

/**
 * Created by Zini on 16.12.2016 19.45.
 */

public class DuckClient extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        JodaTimeAndroid.init(this); // Initialize JodaTime (our datetime lib)
    }

}
