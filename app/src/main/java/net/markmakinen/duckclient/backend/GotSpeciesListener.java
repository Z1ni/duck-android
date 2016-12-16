package net.markmakinen.duckclient.backend;

import net.markmakinen.duckclient.model.Species;

import java.util.ArrayList;

/**
 * Created by Zini on 16.12.2016 21.50.
 */

public interface GotSpeciesListener {
    void gotSpecies(ArrayList<Species> species);
    void gotError(String msg);
}
