package net.markmakinen.duckclient;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.markmakinen.duckclient.model.Sighting;

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

        // Populate Views
        countAndSpeciesView.setText(sighting.getCountAndSpeciesText());
        dateTimeView.setText(sighting.getDateTimeText());
        shortDescView.setText(sighting.getShortDescription());

        return convertView;
    }
}
