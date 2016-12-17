package net.markmakinen.duckclient.backend;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import net.markmakinen.duckclient.model.Sighting;
import net.markmakinen.duckclient.model.Species;

import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.ArrayList;

/**
 * Created by Zini on 16.12.2016 20.10.
 */

/**
 * Duck backend client
 */
public class BackendClient {

    private URI backendURI;
    private ArrayList<Species> allowedSpecies;

    /**
     * BackendClient constructor
     * @param backendURI The endpoint URI
     * @throws InvalidParameterException if supplied URI is null
     */
    public BackendClient(URI backendURI) throws InvalidParameterException {
        if (backendURI == null) throw new InvalidParameterException("Invalid backend URI!");
        this.backendURI = backendURI;
        this.allowedSpecies = new ArrayList<>();
    }

    /**
     * Gets list of Species from the backend
     * @param listener Listener to notify
     * @throws IOException if data retrieval failed
     */
    public void getSpecies(final GotSpeciesListener listener) throws IOException {

        // Get the data asynchronously
        class DataGetter extends AsyncTask<Void, Void, String> {

            private String errorMsg;

            @Override
            protected String doInBackground(Void... voids) {
                String resp = null;
                try {
                    resp = getRawText("/species");  // Get textual response
                } catch (IOException e) {
                    Log.e("BackendClient", "getRawText failed: " + e.getMessage());
                    errorMsg = "Data getting failed!";
                    e.printStackTrace();
                }
                return resp;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);

                // If the response is null, an error occurred
                if (s == null) {
                    if (listener != null) listener.gotError(errorMsg);
                    return;
                }

                // Create Gson instance for deserializing
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(LocalDateTime.class, new DateTimeDeserializer());   // Register custom dateTime deserializer
                Gson gson = gsonBuilder.create();

                // Deserialize into Species objects
                // The backend returns a list, so we tell Gson to deserialize a list of Species objects
                Type speciesList = new TypeToken<ArrayList<Species>>(){}.getType();
                ArrayList<Species> species = gson.fromJson(s, speciesList);

                allowedSpecies = species;   // Remember allowed species

                if (listener != null) listener.gotSpecies(species);
            }
        }

        // Run our AsyncTask
        DataGetter dg = new DataGetter();
        dg.execute();
    }

    /**
     * Gets list of Sightings from the backend
     * @param listener Listener to notify
     * @throws IOException if data retrieval failed
     */
    public void getSightings(final GotSightingsListener listener) throws IOException {

        // Get the data asynchronously
        class DataGetter extends AsyncTask<Void, Void, String> {
            private String errorMsg;

            @Override
            protected String doInBackground(Void... voids) {
                String resp = null;
                try {
                    resp = getRawText("/sightings");
                } catch (IOException e) {
                    Log.e("BackendClient", "getRawText failed: " + e.getMessage());
                    errorMsg = "Data getting failed!";
                    e.printStackTrace();
                }
                return resp;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);

                if (s == null) {
                    if (listener != null) listener.gotError(errorMsg);
                    return;
                }

                // Create Gson instance for deserializing
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(Species.class, new SpeciesDeserializer());  // Register custom species deserializer
                gsonBuilder.registerTypeAdapter(LocalDateTime.class, new DateTimeDeserializer());   // Register custom dateTime deserializer
                Gson gson = gsonBuilder.create();

                Type sightingsList = new TypeToken<ArrayList<Sighting>>(){}.getType();
                ArrayList<Sighting> sightings = gson.fromJson(s, sightingsList);

                if (listener != null) listener.gotSightings(sightings);
            }
        }

        DataGetter dg = new DataGetter();
        dg.execute();
    }

    /**
     * Performs a HTTP GET and returns the data as a String
     * @param path Path when the backend URL is the root
     * @throws IOException if getting data failed
     */
    private String getRawText(String path) throws IOException {

        // Combine root URI and given path
        URI reqURI = backendURI.resolve(path);

        HttpURLConnection conn = null;
        StringBuilder respText = new StringBuilder();

        try {
            // Get HttpURLConnection to use
            URL reqURL = reqURI.toURL();
            conn = (HttpURLConnection)reqURL.openConnection();

            // Specify that we want JSON data
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);

            InputStream in = new BufferedInputStream(conn.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            // Read the stream line by line
            String line;
            while ((line = reader.readLine()) != null) {
                respText.append(line);
            }
        } finally {
            // Disconnect after use
            if (conn != null) conn.disconnect();
        }

        return respText.toString();
    }

    /**
     * Custom deserializer for dateTime field "YYYY-MM-DD'T'HH:MM:SS'Z'"
     */
    private class DateTimeDeserializer implements JsonDeserializer<LocalDateTime> {
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return LocalDateTime.parse(json.getAsJsonPrimitive().getAsString(), ISODateTimeFormat.dateTimeNoMillis());
        }
    }

    /**
     * Custom deserializer for species field that contains only name
     */
    private class SpeciesDeserializer implements JsonDeserializer<Species> {
        public Species deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String name = json.getAsJsonPrimitive().getAsString();
            return new Species(name);   // Create new Species by name
            // TODO: Check that the species is allowed
        }
    }

}
