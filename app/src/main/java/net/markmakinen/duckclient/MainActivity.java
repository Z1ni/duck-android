package net.markmakinen.duckclient;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import net.markmakinen.duckclient.backend.BackendClient;
import net.markmakinen.duckclient.backend.GotSightingsListener;
import net.markmakinen.duckclient.backend.GotSpeciesListener;
import net.markmakinen.duckclient.model.Sighting;
import net.markmakinen.duckclient.model.Species;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (DuckClient.backendURI == null) finish();

        setContentView(R.layout.activity_main);

        BackendClient bc = new BackendClient(DuckClient.backendURI);
        try {
            bc.getSpecies(new GotSpeciesListener() {
                @Override
                public void gotSpecies(ArrayList<Species> species) {
                    Log.i("DuckClient", "Got " + species.size() + " species!");
                }

                @Override
                public void gotError(String msg) {
                    // TODO: Inform user
                    Log.e("DuckClient", "Species getting failed with error: " + msg);
                }
            });

            bc.getSightings(new GotSightingsListener() {
                @Override
                public void gotSightings(ArrayList<Sighting> sightings) {
                    Log.i("DuckClient", "Got " + sightings.size() + " sightings!");
                }

                @Override
                public void gotError(String msg) {
                    // TODO: Inform user
                    Log.e("DuckClient", "Sightings getting failed with error: " + msg);
                }
            });

        } catch (IOException e) {
            Log.e("DuckClient", "Getting species failed!");
        }

    }
}
