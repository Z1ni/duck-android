package net.markmakinen.duckclient;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import net.markmakinen.duckclient.backend.BackendClient;
import net.markmakinen.duckclient.backend.GotSightingsListener;
import net.markmakinen.duckclient.backend.GotSpeciesListener;
import net.markmakinen.duckclient.backend.SightingSaveListener;
import net.markmakinen.duckclient.model.Sighting;
import net.markmakinen.duckclient.model.Species;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * The Main Activity of the Application
 */
public class MainActivity extends AppCompatActivity {

    private SightingArrayAdapter saa;                   // Custom ArrayAdapter for Sightings
    private BackendClient bc;                           // Backend client instance
    private SwipeRefreshLayout refreshLayout;           // Layout containing the ListView
    private boolean userRefresh = false;                // True if the refresh was done by the user
    private ArrayList<Species> allowedSpecies;          // List of allowed species; comes from the backend
    private boolean currentSortingAscending = false;    // Defaults to descending; greater dates are on top of the listing
    private ListView sightingListView;                  // ListView containing the Sightings

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (DuckClient.backendURI == null) finish();    // Close the app if the backend URI was invalid

        setContentView(R.layout.activity_main);

        allowedSpecies = new ArrayList<>();

        // Floating Action Button
        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.actionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCreateNewSightingDialog(allowedSpecies);
            }
        });

        // Floating sorting button
        FloatingActionButton fabSort = (FloatingActionButton)findViewById(R.id.sortActionButton);
        fabSort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentSortingAscending = !currentSortingAscending;
                sortSightings(currentSortingAscending);
            }
        });

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
        sightingListView = (ListView)findViewById(R.id.sightingListView);
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
     * Sorts the Sighting listing by datetime ascending or descending
     * @param ascending true if sorting must be ascending, false if descending
     */
    private void sortSightings(final boolean ascending) {
        saa.sort(new Comparator<Sighting>() {
            @Override
            public int compare(Sighting a, Sighting b) {
                if (ascending) return a.getDateTime().compareTo(b.getDateTime());
                return b.getDateTime().compareTo(a.getDateTime());
            }
        });
        sightingListView.startLayoutAnimation();
    }

    /**
     * Refreshes the Sighting listing
     */
    private void refreshSightings() {

        if (refreshLayout == null || bc == null) return;

        // Run this on the UI thread so we can use this method from other threads
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(true);

                bc.getSpecies(new GotSpeciesListener() {
                    @Override
                    public void gotSpecies(ArrayList<Species> species) {
                        Log.i("DuckClient", "Got " + species.size() + " species!");

                        // Set allowed species
                        allowedSpecies = species;

                        // Get sightings
                        bc.getSightings(new GotSightingsListener() {
                            @Override
                            public void gotSightings(ArrayList<Sighting> sightings) {
                                // Populate the Sighting ListView
                                Log.i("DuckClient", "Got " + sightings.size() + " sightings!");
                                saa.clear();
                                saa.addAll(sightings);
                                sortSightings(currentSortingAscending);
                                refreshLayout.setRefreshing(false);
                                if (userRefresh) Snackbar.make(refreshLayout, R.string.sightings_updated, Snackbar.LENGTH_SHORT).show();
                                userRefresh = false;
                            }

                            // Couldn't get sightings
                            @Override
                            public void gotError(String msg) {
                                refreshLayout.setRefreshing(false);
                                String snackMsg = getResources().getString(R.string.sightings_get_failed, msg);
                                Snackbar.make(refreshLayout, snackMsg, Snackbar.LENGTH_LONG).show();
                                userRefresh = false;
                            }
                        });
                    }

                    // Couldn't get species
                    @Override
                    public void gotError(String msg) {
                        Log.e("DuckClient", "Species getting failed with error: " + msg);
                        refreshLayout.setRefreshing(false);
                        String snackMsg = getResources().getString(R.string.species_get_failed, msg);
                        Snackbar.make(refreshLayout, snackMsg, Snackbar.LENGTH_LONG).show();
                        userRefresh = false;
                    }
                });

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

    /**
     * Create and display Sighting creation dialog
     */
    private void showCreateNewSightingDialog(ArrayList<Species> allowedSpecies) {

        // Use custom layout
        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        View view = inflater.inflate(R.layout.dialog_new_sighting, null);

        final Sighting newSighting = new Sighting();

        // Get Views
        final Spinner speciesSpinner = (Spinner)view.findViewById(R.id.newSightingSpeciesSpinner);
        TextView dateTimeHeader = (TextView)view.findViewById(R.id.newSightingDateTimeHeader);
        final TextView dateView = (TextView)view.findViewById(R.id.newSightingDate);
        final TextView timeView = (TextView)view.findViewById(R.id.newSightingTime);
        final EditText countView = (EditText)view.findViewById(R.id.newSightingCount);
        final EditText descView = (EditText)view.findViewById(R.id.newSightingDescription);

        // Populate species spinner
        ArrayAdapter<Species> speciesAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, allowedSpecies);
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        speciesSpinner.setAdapter(speciesAdapter);

        // Populate Date and Time fields
        LocalDateTime now = LocalDateTime.now();
        newSighting.setDateTime(now.toDateTime().withZone(DateTimeZone.UTC));
        dateView.setText(DateTimeFormat.fullDate().print(now));
        timeView.setText(DateTimeFormat.fullTime().print(now));

        NewSightingDateTimeClickHandler dateTimeHandler = new NewSightingDateTimeClickHandler(new SightingDateTimeSetListener() {
            @Override
            public void dateTimeSet(DateTime selected) {
                LocalDateTime selectedLocal = selected.withZone(DateTimeZone.getDefault()).toLocalDateTime();
                dateView.setText(DateTimeFormat.fullDate().print(selectedLocal));
                timeView.setText(DateTimeFormat.fullTime().print(selectedLocal));
                newSighting.setDateTime(selected);
            }
        });
        // Set click handler for the date and time fields
        // This is needed so we can display a date and a time picker.
        dateTimeHeader.setOnClickListener(dateTimeHandler);
        dateView.setOnClickListener(dateTimeHandler);
        timeView.setOnClickListener(dateTimeHandler);

        countView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {}

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                if (text.length() == 0) {
                    countView.setError(getResources().getString(R.string.new_sighting_count_empty));
                } else if (Integer.parseInt(text.toString()) == 0) {
                    countView.setError(getResources().getString(R.string.new_sighting_count_zero));
                }
            }
        });

        // Create dialog
        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.new_sighting_title)
                .setView(view)
                .setPositiveButton(R.string.save_new_sighting, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Set Sighting values
                        // Species
                        Species selSpecies = (Species)speciesSpinner.getSelectedItem();
                        newSighting.setSpecies(selSpecies);
                        // Count
                        int count;
                        try {
                            count = Integer.parseInt(countView.getText().toString());
                        } catch (NumberFormatException e) {
                            Snackbar.make(refreshLayout, R.string.new_sighting_count_empty, Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        newSighting.setCount(count);
                        // Description
                        String desc = descView.getText().toString();
                        newSighting.setDescription(desc);

                        // Save the created Sighting
                        bc.saveSighting(newSighting, new SightingSaveListener() {
                            @Override
                            public void saveCompleted() {
                                Log.i("DuckClient", "New Sighting saved!");
                                Snackbar.make(refreshLayout, R.string.new_sighting_save_successful, Snackbar.LENGTH_LONG).show();
                                // Update ListView
                                refreshSightings();
                            }

                            @Override
                            public void saveFailed(String msg) {
                                Log.e("DuckClient", "New Sighting save failed!");
                                String snackMsg = getResources().getString(R.string.new_sighting_save_failed, msg);
                                Snackbar.make(refreshLayout, snackMsg, Snackbar.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();

    }

    /**
     * Interface for NewSightingDateTimeClickHandler
     * dateTimeSet gets called when the user has selected both date and time
     */
    interface SightingDateTimeSetListener {
        void dateTimeSet(DateTime selected);
    }

    /**
     * Handler for Sighting creation dialog date and time field onClick
     */
    class NewSightingDateTimeClickHandler implements View.OnClickListener {

        DateTime selected;
        SightingDateTimeSetListener listener;

        /**
         * Constructor
         * @param listener Listener to notify when user has chosen a date and a time
         */
        public NewSightingDateTimeClickHandler(SightingDateTimeSetListener listener) {
            this.listener = listener;
        }

        @Override
        public void onClick(View view) {
            final DateTime now = DateTime.now();

            // Create and show DatePickerDialog
            DatePickerDialog dpd = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, final int year, final int month, final int dayOfMonth) {

                    // When the user has chosen a date, he/she will choose a time
                    TimePickerDialog tpd = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker timePicker, int hours, int minutes) {
                            // Create a DateTime from the selected date and time
                            // month+1 because DatePickerDialog uses month numbering from 0 to 11, where 0 is January and 11 is December
                            selected = new DateTime(year, month+1, dayOfMonth, hours, minutes, DateTimeZone.getDefault());
                            selected = selected.withZone(DateTimeZone.UTC); // Use UTC instead of local timezone
                            Log.i("onTimeSet", "Datetime: " + selected.toString());

                            if (listener != null) listener.dateTimeSet(selected);   // Notify the listener
                        }
                    }, now.getHourOfDay(), now.getMinuteOfHour(), true);
                    tpd.show();

                }
            }, now.getYear(), now.getMonthOfYear()-1, now.getDayOfMonth());
            dpd.show();
        }
    }
}
