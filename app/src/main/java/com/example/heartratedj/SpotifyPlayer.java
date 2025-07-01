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
    private int previousHeartRate;
    private double previousHrv;
    private String currentSongUri;
    private String nextSongUri;
    private SongUpdateListener songListener;

    private final String token;   // pass it in the constructor
    private String[]  idArray;
    private int minBPM, maxBPM;
    private double minEnergy, maxEnergy;
    public SpotifyPlayer(String webApiToken) {
        this.token = webApiToken;
    }


    public void updateRemote(SpotifyAppRemote remote) {
        this.remote = remote;
    }
    public void updateMetrics(double newHrv, int newHr) {
        this.hrv = newHrv;
        this.heartRate = newHr;
    }
    public void setNewSong(String uri){
        this.currentSongUri = uri;
        Log.d("setNewsong", "Setting current song: " + uri);

        playSong(uri);
    }
    public void setSongUpdateListener(SongUpdateListener listener) {
        this.songListener = listener;
    }
    public void setNewSongQueue(String uri){
        this.nextSongUri = uri;
        Log.d("setNewsong", "Setting current song: " + uri);

        queueSong(uri);

    }
    public interface SongUpdateListener {
        void onSongChanged(String currentSong, String artist);
        void onNextSongQueued(String nextSong, String artist);
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
    public static String recommendGenre(double hrv, int heartRate) {
        Log.d("recommendGenre", "HR: " + heartRate + " | HRV: " + hrv);

        if (heartRate < 65) {
            // Low HR – calm with a bit of rhythm
            return randomGenre(new String[]{
                    "lo-fi", "indie pop", "folk pop", "dream pop", "synth-pop"
            });
        } else if (heartRate < 80) {
            // Light activity – mellow but upbeat
            return randomGenre(new String[]{
                    "indie rock", "alt pop", "soft rock", "nu-disco", "electropop"
            });
        } else if (heartRate < 100) {
            // Moderate pace – steady, groovy
            return randomGenre(new String[]{
                    "pop rock", "funk", "garage", "dance pop", "deep house"
            });
        } else if (heartRate < 120) {
            // Workout zone – high energy
            return randomGenre(new String[]{
                    "house", "future bass", "edm", "electro house", "trance"
            });
        } else if (heartRate < 140) {
            // Cardio/sprint – aggressive drive
            return randomGenre(new String[]{
                    "techno", "big room", "drum and bass", "hard trance", "dubstep"
            });
        } else {
            // Max effort – hard hitting
            return randomGenre(new String[]{
                    "hardstyle", "industrial", "gabber", "speedcore", "riddim dubstep"
            });
        }
    }





    private void setContentView(int spotifyPlayer) {
    }


    private void playPlaylist() {
        if (remote != null) {
            remote.getPlayerApi().play("spotify:playlist:37i9dQZF1DX2sUQwD7tbmL");
        }
    }
    public void setSpotifyVolumeWithoutDeviceId(int volumePercent) {
        if (token == null || token.isEmpty()) {
            Log.e("SPOTIFY_VOLUME", "Missing token");
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String urlStr = "https://api.spotify.com/v1/me/player/volume?volume_percent=" + volumePercent;
                URL url = new URL(urlStr);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);  // even though body is empty, needed for PUT
                int responseCode = conn.getResponseCode();
                Log.d("SPOTIFY_VOLUME", "Set volume " + volumePercent + " → HTTP " + responseCode);
                conn.disconnect();
            } catch (IOException e) {
                Log.e("SPOTIFY_VOLUME", "Failed to set volume", e);
            }
        });
    }
    public void fetchCurrentPlayback() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Log.d("FETCH CURRENT PLAYBACK", token);
                URL url = new URL("https://api.spotify.com/v1/me/player/currently-playing");
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);

                int code = conn.getResponseCode();
                if (code == 204) {
                    Log.d("Spotify", "No song currently playing.");
                    return;
                }

                if (code != 200) {
                    Log.e("Spotify", "Error fetching playback: " + code);
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                JSONObject json = new JSONObject(sb.toString());
                JSONObject item = json.getJSONObject("item");
                String name = item.getString("name");
                String artist = item.getJSONArray("artists").getJSONObject(0).getString("name");

                if (songListener != null) {
                    songListener.onSongChanged(name, artist);
                }

            } catch (Exception e) {
                Log.e("Spotify", "Failed to fetch current song", e);
            }
        });
    }

    public void search_songs(boolean skip) throws IOException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String[] letters = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k","l","m","n","o","p","q","r","s","t","u","p","x","y","z","ö","ä","å"};
            String genre = recommendGenre(hrv,heartRate);
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




                if (skip) {
                    setNewSong(uri);  // Immediately play if skipping
                    if (songListener != null) {
                        songListener.onSongChanged(name, artist);
                    }
                }
                if (!skip) {
                    setNewSongQueue(uri);
                    if (songListener != null) {
                        songListener.onNextSongQueued(name, artist);
                    }
                }

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
    public void adjustVolumeByHeartRate(int heartRate) {
        int minHR = 60;   // Resting
        int maxHR = 160;  // Running
        int minVolume = 30;
        int maxVolume = 100;

        if (heartRate < minHR) heartRate = minHR;
        if (heartRate > maxHR) heartRate = maxHR;

        int volume = minVolume + (int)(((double)(heartRate - minHR) / (maxHR - minHR)) * (maxVolume - minVolume));
        changeVolume(volume);
    }
    public void changeVolume(int volumePercent) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // First, get the list of devices
                URL deviceUrl = new URL("https://api.spotify.com/v1/me/player/devices");
                HttpsURLConnection connDevices = (HttpsURLConnection) deviceUrl.openConnection();
                connDevices.setRequestMethod("GET");
                connDevices.setRequestProperty("Authorization", "Bearer " + token);

                int deviceCode = connDevices.getResponseCode();
                if (deviceCode != 200) {
                    Log.e("SpotifyVolume", "Failed to get devices | HTTP " + deviceCode);
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connDevices.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);
                reader.close();
                connDevices.disconnect();

                JSONObject json = new JSONObject(result.toString());
                JSONArray devices = json.getJSONArray("devices");

                String activeDeviceId = null;
                for (int i = 0; i < devices.length(); i++) {
                    JSONObject device = devices.getJSONObject(i);
                    if (device.getBoolean("is_active") && !device.getBoolean("is_restricted")) {
                        activeDeviceId = device.getString("id");
                        break;
                    }
                }

                if (activeDeviceId == null) {
                    Log.e("SpotifyVolume", "No active controllable device found.");
                    return;
                }

                // Log info about the current active device
                URL urlplayer = new URL("https://api.spotify.com/v1/me/player");
                HttpsURLConnection connplayer = (HttpsURLConnection) urlplayer.openConnection();
                connplayer.setRequestMethod("GET");
                connplayer.setRequestProperty("Authorization", "Bearer " + token);
                connplayer.setDoOutput(true);
                Log.d("SpotifyVolume", "player: " + connplayer.toString());

                // Now send volume request with device_id
                String volumeUrlStr = "https://api.spotify.com/v1/me/player/volume?volume_percent=" + volumePercent +
                        "&device_id=" + URLEncoder.encode(activeDeviceId, "UTF-8");
                URL url = new URL(volumeUrlStr);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);
                Log.d("CHANGE VOLUME", token);
                int code = conn.getResponseCode();
                Log.d("SpotifyVolume", "Volume set to " + volumePercent + " | HTTP " + code);
                conn.disconnect();

            } catch (IOException | JSONException e) {
                Log.e("SpotifyVolume", "Failed to set volume", e);
            }
        });
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
