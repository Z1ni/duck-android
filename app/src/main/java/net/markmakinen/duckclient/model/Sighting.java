package net.markmakinen.duckclient.model;

/**
 * Created by Zini on 16.12.2016 19.41.
 */

import com.google.gson.annotations.Expose;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;

/**
 * Class for representing a sighting
 */
public class Sighting {

    @Expose(serialize = false) private String id;
    @Expose private DateTime dateTime;
    @Expose private String description;
    @Expose private Species species;
    @Expose private int count;

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
    public DateTime getDateTime() {
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
     * String containing formatted count and species name
     * @return Formatted string
     */
    public String getCountAndSpeciesText() {
        String speciesName = species.getName();
        speciesName += (count > 1 ? "s" : ""); // Add 's' to the end to pluralize
        return String.format("%d %s", count, speciesName);
    }

    /**
     * String containing sighting date and time
     * @return Formatted string
     */
    public String getDateTimeText() {
        // Format date/time according to system locale
        LocalDateTime local = dateTime.withZone(DateTimeZone.getDefault()).toLocalDateTime();
        return DateTimeFormat.fullDateTime().print(local);
    }

    /**
     * Short description of the sighting. Has ellipsis if the description is too long.
     * @return Short description
     */
    public String getShortDescription() {
        // Create short description text
        // Add our own ellipsis just in case (even though we set the TextView to do this automatically)
        String shortDesc = description.substring(0, Math.min(description.length(), 254));
        if (shortDesc.length() >= 254) shortDesc += "...";
        return shortDesc;
    }

    /**
     * Creates a new Sighting
     * @param id ID for this Sighting, can and must be null if creating a new one
     * @param dateTime Date and time of the sighting
     * @param description Descriptive text
     * @param species Species
     * @param count Duck count
     */
    public Sighting(String id, DateTime dateTime, String description, Species species, int count) {
        this.id = id;
        this.dateTime = dateTime;
        this.description = description;
        this.species = species;
        this.count = count;
    }

}
