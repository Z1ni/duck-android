package net.markmakinen.duckclient.model;

/**
 * Created by Zini on 16.12.2016 19.41.
 */

import org.joda.time.LocalDateTime;

/**
 * Class for representing a sighting
 */
public class Sighting {

    private String id;
    private LocalDateTime dateTime;
    private String description;
    private Species species;
    private int count;

    // Getters
    /**
     * Server-side Sighting ID
     * @return Sighting ID
     */
    public String getSightingId() {
        return this.id;
    }

    /**
     * Sighting datetime
     * @return Datetime
     */
    public LocalDateTime getDateTime() {
        return this.dateTime;
    }

    /**
     * Description of the sighting
     * @return Description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Species of the sighting
     * @return Species
     */
    public Species getSpecies() {
        return this.species;
    }

    /**
     * Amount of ducks in this sighting
     * @return Duck count
     */
    public int getCount() {
        return this.count;
    }

    /**
     * Creates a new Sighting
     * @param id ID for this Sighting, can and must be null if creating a new one
     * @param dateTime Date and time of the sighting
     * @param description Descriptive text
     * @param species Species
     * @param count Duck count
     */
    public Sighting(String id, LocalDateTime dateTime, String description, Species species, int count) {
        this.id = id;
        this.dateTime = dateTime;
        this.description = description;
        this.species = species;
        this.count = count;
    }

}