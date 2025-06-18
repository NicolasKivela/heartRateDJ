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
import java.util.ArrayList;
import java.util.List;
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
    double hrvPrevious = MainActivity.getPreviousHrv();

    int heartRate = MainActivity.getHeartRate();

    private double previousHrv;
    private String currentSongUri;
    private String nextSongUri;
    private final String token;   // pass it in the constructor
    private String[]  idArray;
    private int minBPM, maxBPM;
    private double minEnergy, maxEnergy;
    public SpotifyPlayer(String webApiToken) {
        this.token = webApiToken;
    }
    public void setNewSong(String uri){
        this.currentSongUri = uri;
        Log.d("setNewsong", "Setting current song: " + uri);

        playSong(uri);
    }
    public void setNewSongQueue(String uri){
        this.nextSongUri = uri;
        Log.d("setNewsong", "Setting current song: " + uri);

        queueSong(uri);
    }

    private void onCreate() throws IOException {
        if (remote == null) {
            Log.e("Spotify", "Spotify not connected");
            return;
        }



        //String randomTrack = "%25a%25";

    }
    public void getMinMaxTempo(){
        Log.d("getMinMaxTempo", "hrv " + hrv);
        if (hrv < 30) {
            // stressed → calm, soft music
            minBPM = 60;
            maxBPM = 90;
            minEnergy = 0.0;
            maxEnergy = 0.4;
        } else if (hrv < 70) {
            // balanced mood
            minBPM = 90;
            maxBPM = 120;
            minEnergy = 0.4;
            maxEnergy = 0.7;
        } else {
            // high HRV → upbeat music
            minBPM = 120;
            maxBPM = 160;
            minEnergy = 0.7;
            maxEnergy = 1.0;
        }

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
    public void queueSong(String uri){
        Log.d("Spotify", "queueSong activated: " + currentSongUri);

        if (remote != null && remote.isConnected()) {
            remote.getPlayerApi().queue(uri);
            Log.d("Spotify", "quueues: " + uri);
        } else {
            Log.e("Spotify", "AppRemote is null or not connected");
        }
    }

    private static String randomGenre(String[] genres) {
        return genres[new Random().nextInt(genres.length)];
    }
    public static String recommendGenre(double hrv) {
        Log.d("recommendHrv", "HRV " + hrv);
        if (hrv < 30) {
            // Low HRV → reduce stress
            return randomGenre(new String[]{"ambient", "acoustic", "chill", "classical", "sleep"});
        } else if (hrv < 70) {
            // Medium HRV → maintain balance
            return randomGenre(new String[]{"pop", "indie", "folk", "jazz"});
        } else if (hrv < 110){
            // High HRV → active, energetic
            return randomGenre(new String[]{"electronic", "edm", "rock", "hip-hop", "dance","130BPM"});
        }
        else if (hrv < 140){
            return randomGenre(new String[]{"rally house","drum and bass","140BPM"});
        }
        else{
            return randomGenre(new String[]{"hard style", "hardcore", "heavy metal"});
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
            String genre = recommendGenre(hrv);
            String randomChar = letters[new Random().nextInt(letters.length)];
            int randomOffset = new Random().nextInt(500);
            try {String searchTerm = randomChar + " genre:\"" + genre + "\"";
                String query = URLEncoder.encode(searchTerm, "UTF-8");
            String urlStr = "https://api.spotify.com/v1/search?q=" + query + "&type=track&limit=50&offset=" + randomOffset;

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
                Log.d("JSON tracks", "Tracks" +tracks.length());
                List<String> ids = new ArrayList<>();

                for (int i = 0; i < tracks.length(); i++) {
                    JSONObject track = tracks.getJSONObject(i);
                    String name = track.getString("name");
                    String uri = track.getString("uri");
                    String artist = track.getJSONArray("artists").getJSONObject(0).getString("name");

                    Log.d("TRACK", "Track: " + name + " by " + artist);
                    Log.d("TRACK_URI", "URI: " + uri);
                    String id = track.getString("id");
                    ids.add(id);
                }
                idArray = ids.toArray(new String[0]);

                JSONObject randomTrack = tracks.getJSONObject(new Random().nextInt(tracks.length()));
                String name = randomTrack.getString("name");
                String uri = randomTrack.getString("uri");
                String artist = randomTrack.getJSONArray("artists").getJSONObject(0).getString("name");

                Log.d("TRACK", "Track: " + name + " by " + artist);
                Log.d("TRACK_URI", "URI: " + uri);
                //filterSongs(idArray, token);
                if (Math.abs(hrvPrevious-hrv) < 15) {
                    setNewSongQueue(uri);
                    Log.d("TRACK_URI", "Queue updated" + uri);
                }

                setNewSong(uri);



                previousHrv = hrv;
            } catch (IOException e) {
                Log.e("ERROR", "Error reading response", e);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            } finally {
                urlConnection.disconnect();
            }} catch (IOException e) {
            Log.e("ERROR", "Error reading response", e);
        }});

}
    public void filterSongs(String[] uriArray, String token){
        Log.d("filterSongs", "Filtering songs");
        Log.d("filterSongs", "Token " + token);
        getMinMaxTempo();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String idsParam = String.join(",", uriArray[0]);
                String urlStr = "https://api.spotify.com/v1/audio-features?ids=" + idsParam;
                Log.d("DEBUG", "Audio features URL: " + urlStr);

                URL url = new URL(urlStr);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);

                int code = conn.getResponseCode();
                Log.d("SPOTIFY_AUDIO", "Response code: " + code);

                InputStream in = (code >= 200 && code < 300)
                        ? conn.getInputStream()
                        : conn.getErrorStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder result = new StringBuilder();
                Log.d("SPOTIFY_RAW_RESPONSE", result.toString());
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                in.close();
                conn.disconnect();


                JSONObject json = new JSONObject(result.toString());
                if (json.has("error")) {
                    JSONObject error = json.getJSONObject("error");
                    Log.e("SPOTIFY_API", "Error: " + error.getString("message"));
                    return;
                }
                JSONArray featuresArray = json.getJSONArray("audio_features");
                //List<String> filteredUris = new ArrayList<>();
                for (int i = 0; i < featuresArray.length(); i++) {
                    JSONObject trackFeatures = featuresArray.getJSONObject(i);
                    String id = trackFeatures.getString("id");
                    double tempo = trackFeatures.getDouble("tempo");
                    double danceability = trackFeatures.getDouble("danceability");
                    double energy = trackFeatures.getDouble("energy");
                    if (tempo >= minBPM && tempo <= maxBPM && energy >= minEnergy && energy <= maxEnergy ) {
                        Log.d("HRV_MATCH", "Track BPM: " + tempo + " matches HRV-based range");
                        String uri = trackFeatures.getString("uri");
                        //filteredUris.add(uri);
                        Log.d("FEATURES", "Track ID: " + id + " | BPM: " + tempo + " | Dance: " + danceability + " | Energy: " + energy);
                        setNewSong(uri);
                    }
                    else{
                        Log.d("filterSongs", "No tempo matches, playing default song");
                        setNewSong("3rfpTrCNol2OmFkdzWqOHe");}
                }

            } catch (Exception e) {
                Log.e("SPOTIFY_AUDIO", "Error getting audio features", e);
            }
        });
    }
}
