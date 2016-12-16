package net.markmakinen.duckclient.backend;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.markmakinen.duckclient.model.Species;

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
    private Gson gson;

    /**
     * BackendClient constructor
     * @param backendURI The endpoint URI
     * @throws InvalidParameterException if supplied URI is null
     */
    public BackendClient(URI backendURI) throws InvalidParameterException {
        if (backendURI == null) throw new InvalidParameterException("Invalid backend URI!");
        this.backendURI = backendURI;
        this.gson = new Gson();
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

                // Deserialize into Species objects
                // The backend returns a list, so we tell Gson to deserialize a list of Species objects
                Type speciesList = new TypeToken<ArrayList<Species>>(){}.getType();
                ArrayList<Species> species = gson.fromJson(s, speciesList);

                if (listener != null) listener.gotSpecies(species);
            }
        }

        // Run our AsyncTask
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

}
