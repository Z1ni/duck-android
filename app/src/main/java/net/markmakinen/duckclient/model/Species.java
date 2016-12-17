package net.markmakinen.duckclient.model;

/**
 * Created by Zini on 16.12.2016 19.39.
 */

/**
 * Class for representing Species
 */
public class Species {

    private String name;

    // Getter & setter
    /**
     * Get the species' name
     * @return Name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the species' name
     * @param name Name to be set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Creates a new Species
     * @param name Name of the species
     */
    public Species(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Species)) return false;
        Species s = (Species)obj;
        return (name.equals(s.name));
    }
}
