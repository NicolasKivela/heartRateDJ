package com.example.heartratedj;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.PlayerState;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.protocol.types.Track;

import javax.net.ssl.HttpsURLConnection;

public class SpotifyPlayer {
    private SpotifyAppRemote spotifyAppRemote;
    private String songUri;
    private void onCreate() throws IOException {
        if (spotifyAppRemote == null) {
            Log.e("Spotify", "Spotify not connected");
            return;
        }



        //String randomTrack = "%25a%25";
        if (songUri != null) {
            spotifyAppRemote.getPlayerApi().play(songUri);
            Log.d("Spotify", "Playing random track: " + songUri);
        }
    }
    public void playSong(){

    }

    private void setContentView(int spotifyPlayer) {
    }

    private void playPlaylist() {
        if (spotifyAppRemote != null) {
            spotifyAppRemote.getPlayerApi().play("spotify:playlist:37i9dQZF1DX2sUQwD7tbmL");
        }
    }
    public void search_songs() throws IOException {
        String query = URLEncoder.encode("a", "UTF-8");
        String urlStr = "https://api.spotify.com/v1/search?q=" + query + "&type=track&limit=10";

        URL endPoint = new URL(urlStr);
        HttpsURLConnection urlConnection = (HttpsURLConnection) endPoint.openConnection();
        Log.d("urlConnection", "connection" + urlConnection);
        urlConnection.setRequestMethod("GET");
        urlConnection.setRequestProperty("Authorization", "Bearer" + MainActivity.accessToken);
        Log.d("TOKEN", "ACCESSTOKEN" + MainActivity.accessToken);
        //NULL ACCESSTOKEN
        Log.d("endPoint", "URL: " + endPoint);
        try {
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            Log.d("RESPONSE", result.toString());
        } catch (IOException e) {
            Log.e("ERROR", "Error reading response", e);
        } finally {
            urlConnection.disconnect();
        }
}}
