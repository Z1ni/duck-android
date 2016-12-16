package net.markmakinen.duckclient.backend;

import net.markmakinen.duckclient.model.Sighting;

import java.util.ArrayList;

/**
 * Created by Zini on 16.12.2016 22.39.
 */

public interface GotSightingsListener {
    void gotSightings(ArrayList<Sighting> sightings);
    void gotError(String msg);
}
