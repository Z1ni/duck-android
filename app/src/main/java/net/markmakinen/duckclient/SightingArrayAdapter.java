package net.markmakinen.duckclient;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.markmakinen.duckclient.model.Sighting;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;

/**
 * Created by Zini on 19.12.2016 3.06.
 */

/**
 * Custom ArrayAdapter for displaying Sighting information in a ListView
 */
public class SightingArrayAdapter extends ArrayAdapter<Sighting> {

    private final Context context;

    public SightingArrayAdapter(Context context, ArrayList<Sighting> sightings) {
        super(context, 0, sightings);
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Sighting sighting = getItem(position);
        if (sighting == null) return convertView;

        if (convertView == null) convertView = LayoutInflater.from(context).inflate(R.layout.item_sighting, parent, false);

        // Get TextViews
        TextView countAndSpeciesView = (TextView)convertView.findViewById(R.id.sightingCountSpecies);
        TextView dateTimeView = (TextView)convertView.findViewById(R.id.sightingDate);
        TextView shortDescView = (TextView)convertView.findViewById(R.id.sightingShortDescription);

        // Create count and species text
        int count = sighting.getCount();
        String speciesName = sighting.getSpecies().getName();
        speciesName += (count > 1 ? "s" : ""); // Add 's' to the end to pluralize
        String countAndSpecies = context.getResources().getString(
                R.string.sighting_item_count_species,
                count, speciesName);

        // Format date/time according to system locale
        LocalDateTime local = sighting.getDateTime().withZone(DateTimeZone.getDefault()).toLocalDateTime();
        String dateTimeStr = DateTimeFormat.fullDateTime().print(local);

        // Create short description text
        // Add our own ellipsis just in case (even though we set the TextView to do this automatically)
        String desc = sighting.getDescription();
        String shortDesc = desc.substring(0, Math.min(desc.length(), 254));
        if (shortDesc.length() >= 254) shortDesc += "...";

        // Populate Views
        countAndSpeciesView.setText(countAndSpecies);
        dateTimeView.setText(dateTimeStr);
        shortDescView.setText(shortDesc);

        return convertView;
    }
}
