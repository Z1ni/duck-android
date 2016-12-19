package net.markmakinen.duckclient;

import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

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
    private boolean userRefresh = false;

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
                userRefresh = true;
                refreshSightings();
            }
        });

        // Initialize Sighting ListView
        saa = new SightingArrayAdapter(this, new ArrayList<Sighting>());
        ListView sightingListView = (ListView)findViewById(R.id.sightingListView);
        sightingListView.setAdapter(saa);

        sightingListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Sighting sighting = saa.getItem(i);
                Log.d("DuckClient", "User clicked Sighting: " + sighting.getSightingId() + ", " + sighting.getDescription());

                showSightingInfoDialog(sighting);
            }
        });

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
                        saa.clear();
                        saa.addAll(sightings);
                        refreshLayout.setRefreshing(false);
                        if (userRefresh) Snackbar.make(refreshLayout, R.string.sightings_updated, Snackbar.LENGTH_SHORT).show();
                        userRefresh = false;
                    }

                    // Couldn't get sightings
                    @Override
                    public void gotError(String msg) {
                        refreshLayout.setRefreshing(false);
                        Snackbar.make(refreshLayout, R.string.sightings_get_failed, Snackbar.LENGTH_LONG).show();
                        userRefresh = false;
                    }
                });
            }

            // Couldn't get species
            @Override
            public void gotError(String msg) {
                Log.e("DuckClient", "Species getting failed with error: " + msg);
                refreshLayout.setRefreshing(false);
                Snackbar.make(refreshLayout, R.string.species_get_failed, Snackbar.LENGTH_LONG).show();
                userRefresh = false;
            }
        });
    }

    /**
     * Create and display Sighting info dialog
     * @param sighting Sighting to show
     */
    private void showSightingInfoDialog(Sighting sighting) {

        // Use custom layout
        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        View view = inflater.inflate(R.layout.dialog_sighting_info, null);

        // Set date and description
        TextView dateView = (TextView)view.findViewById(R.id.sightingInfoDatetime);
        TextView descView = (TextView)view.findViewById(R.id.sightingInfoDescription);

        dateView.setText(sighting.getDateTimeText());
        descView.setText(sighting.getDescription());

        // Create AlertDialog and display it
        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle(sighting.getCountAndSpeciesText())
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        dialog.show();

    }
}
