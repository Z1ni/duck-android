package net.markmakinen.duckclient;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import net.markmakinen.duckclient.backend.BackendClient;
import net.markmakinen.duckclient.backend.GotSightingsListener;
import net.markmakinen.duckclient.backend.GotSpeciesListener;
import net.markmakinen.duckclient.model.Sighting;
import net.markmakinen.duckclient.model.Species;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private SightingArrayAdapter saa;
    private BackendClient bc;
    private SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (DuckClient.backendURI == null) finish();    // Close the app if the backend URI was invalid

        setContentView(R.layout.activity_main);

        // Initialize SwipeRefreshLayout
        refreshLayout = (SwipeRefreshLayout)findViewById(R.id.activity_main);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // User wants to refresh the list
                refreshSightings();
            }
        });

        // Initialize Sighting ListView
        saa = new SightingArrayAdapter(this, new ArrayList<Sighting>());
        ListView sightingListView = (ListView)findViewById(R.id.sightingListView);
        sightingListView.setAdapter(saa);

        // Create a new BackendClient instance
        bc = new BackendClient(DuckClient.backendURI);

        // Get species from the server
        refreshSightings();

    }

    /**
     * Refreshes the Sighting listing
     */
    private void refreshSightings() {

        if (refreshLayout == null || bc == null) return;

        refreshLayout.setRefreshing(true);

        bc.getSpecies(new GotSpeciesListener() {
            @Override
            public void gotSpecies(ArrayList<Species> species) {
                Log.i("DuckClient", "Got " + species.size() + " species!");

                // Get sightings
                bc.getSightings(new GotSightingsListener() {
                    @Override
                    public void gotSightings(ArrayList<Sighting> sightings) {
                        // Populate the Sighting ListView
                        Log.i("DuckClient", "Got " + sightings.size() + " sightings!");
                        saa.addAll(sightings);
                        refreshLayout.setRefreshing(false);
                    }

                    // Couldn't get sightings
                    @Override
                    public void gotError(String msg) {
                        // TODO: Inform user
                        refreshLayout.setRefreshing(false);
                    }
                });
            }

            // Couldn't get species
            @Override
            public void gotError(String msg) {
                // TODO: Inform user
                Log.e("DuckClient", "Species getting failed with error: " + msg);
                refreshLayout.setRefreshing(false);
            }
        });
    }
}
