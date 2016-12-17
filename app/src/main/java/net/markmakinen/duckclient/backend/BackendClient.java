package net.markmakinen.duckclient.backend;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import net.markmakinen.duckclient.model.Sighting;
import net.markmakinen.duckclient.model.Species;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
     */
    public void getSpecies(final GotSpeciesListener listener) {

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
                Gson gson = new Gson();

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
     */
    public void getSightings(final GotSightingsListener listener) {

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
                gsonBuilder.registerTypeAdapter(DateTime.class, new DateTimeDeserializer());   // Register custom dateTime deserializer
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
     * Sends a new sighting to the backend to save
     * @param sighting Sighting to save
     * @param listener Listener to notify
     */
    public void saveSighting(final Sighting sighting, final SightingSaveListener listener) {

        // Check if we have species listing from the backend
        if (allowedSpecies.size() == 0) {
            // Need to get species listing before continuing
            // TODO: Not implemented here, now we just assume that we have loaded species information before trying to save any
            if (listener != null) listener.saveFailed("Get species before saving a new sighting!");
            return;
        }

        // Check that the Species is allowed
        boolean allowed = false;
        Species specToSave = sighting.getSpecies();
        for (Species s : allowedSpecies) {
            if (s.equals(specToSave)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            if (listener != null) listener.saveFailed("Backend does not support species \"" + specToSave.getName() + "\"!");
            return;
        }

        // Send data asynchronously
        class DataSaver extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {

                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(Species.class, new SpeciesSerializer());    // Register custom species serializer
                gsonBuilder.registerTypeAdapter(DateTime.class, new DateTimeSerializer()); // Register custom dateTime serializer
                Gson gson = gsonBuilder.create();

                String sightingJson = gson.toJson(sighting);

                Log.d("BackendClient", "Created sighting JSON:");
                Log.d("BackendClient", sightingJson);

                try {
                    postData("/sightings", sightingJson);
                } catch (IOException e) {
                    Log.e("BackendClient", "Sighting POST failed: " + e.getMessage());
                    if (listener != null) listener.saveFailed("Sighting save failed!");
                    return null;
                }

                if (listener != null) listener.saveCompleted();
                return null;
            }
        }

        // Create and execute AsyncTask
        DataSaver ds = new DataSaver();
        ds.execute();
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

            // Specify that we want JSON data. Not really needed, but doesn't hurt
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
     * Performs a HTTP POST with given data
     * @param path Path when the backend URL is the root
     * @param data Data to send
     * @throws IOException if data sending failed
     */
    private void postData(String path, String data) throws IOException {

        // Combine root URI and given path
        URI reqURI = backendURI.resolve(path);

        HttpURLConnection conn = null;

        try {
            URL reqURL = reqURI.toURL();
            conn = (HttpURLConnection)reqURL.openConnection();

            // Set connection parameters, we're POSTing stuff here
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            // Get output stream
            OutputStream out = new BufferedOutputStream(conn.getOutputStream());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));

            // Write data
            writer.write(data);
            writer.flush();
            writer.close();

            int respCode = conn.getResponseCode();
            if (respCode == -1 || respCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server responded with error: " + conn.getResponseMessage());
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Custom serializer for dateTime
     */
    private class DateTimeSerializer implements JsonSerializer<DateTime> {
        public JsonElement serialize(DateTime dt, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(dt.toString(ISODateTimeFormat.dateTimeNoMillis()));
        }
    }

    /**
     * Custom deserializer for dateTime field "YYYY-MM-DD'T'HH:MM:SS'Z'"
     */
    private class DateTimeDeserializer implements JsonDeserializer<DateTime> {
        public DateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new DateTime(json.getAsJsonPrimitive().getAsString(), DateTimeZone.UTC);
        }
    }

    /**
     * Custom serializer for species
     */
    private class SpeciesSerializer implements JsonSerializer<Species> {
        public JsonElement serialize(Species species, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(species.getName());
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
