package com.example.heartratedj;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.spotify.android.appremote.api.SpotifyAppRemote;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;

public class SpotifyPlayer {
    SpotifyAppRemote remote = MainActivity.getSpotifyAppRemote();

    double hrv = MainActivity.getHrv();

    private double previousHrv;
    private String currentSongUri;
    private String nextSongUri;
    private final String token;   // pass it in the constructor

    public SpotifyPlayer(String webApiToken) {
        this.token = webApiToken;
    }
    public void setNewSong(String uri){
        this.currentSongUri = uri;
        Log.d("setNewsong", "Setting current song: " + uri);

        playSong(uri);
    }
    private void onCreate() throws IOException {
        if (remote == null) {
            Log.e("Spotify", "Spotify not connected");
            return;
        }



        //String randomTrack = "%25a%25";

    }
    public void playSong(String uri){
        Log.d("Spotify", "playSong activated: " + currentSongUri);

        if (remote != null && remote.isConnected()) {
            remote.getPlayerApi().play(uri);
            Log.d("Spotify", "Playing random track: " + uri);
        } else {
            Log.e("Spotify", "AppRemote is null or not connected");
        }
    }

    private void setContentView(int spotifyPlayer) {
    }

    private void playPlaylist() {
        if (remote != null) {
            remote.getPlayerApi().play("spotify:playlist:37i9dQZF1DX2sUQwD7tbmL");
        }
    }
    public void search_songs() throws IOException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String[] letters = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k","l","m","n","o","p","q","r","s","t","u","p","x","y","z","ö","ä","å"};
            String randomChar = letters[new Random().nextInt(letters.length)];
            try {String query = URLEncoder.encode(randomChar, "UTF-8");
            String urlStr = "https://api.spotify.com/v1/search?q=" + query + "&type=track&limit=50";

            URL endPoint = new URL(urlStr);
            HttpsURLConnection urlConnection = (HttpsURLConnection) endPoint.openConnection();
            Log.d("urlConnection", "connection" + urlConnection);
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Authorization", "Bearer " + token);
            Log.d("TOKEN", "Bearer " + token);
            //NULL ACCESSTOKEN
            Log.d("endPoint", "URL: " + endPoint);
            int responseCode = urlConnection.getResponseCode();
            Log.d("SpotifyAPI", "Response code: " + responseCode);
            try {
                Log.d("RESPONSE", "computing response");
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                Log.d("RESPONSE", result.toString());
                String response = result.toString();
                JSONArray tracks = new JSONObject(response).getJSONObject("tracks").getJSONArray("items");
                if (tracks.length() == 0) {
                    Log.e("SPOTIFY", "No tracks found for query.");
                    return;
                }
                JSONObject randomTrack = tracks.getJSONObject(new Random().nextInt(tracks.length()));
                String name = randomTrack.getString("name");
                String uri = randomTrack.getString("uri");
                String artist = randomTrack.getJSONArray("artists").getJSONObject(0).getString("name");

                Log.d("TRACK", "Track: " + name + " by " + artist);
                Log.d("TRACK_URI", "URI: " + uri);
                setNewSong(uri);
            } catch (IOException e) {
                Log.e("ERROR", "Error reading response", e);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            } finally {
                urlConnection.disconnect();
            }} catch (IOException e) {
            Log.e("ERROR", "Error reading response", e);
        }});

}}
