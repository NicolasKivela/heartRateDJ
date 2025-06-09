package com.example.heartratedj;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;
public class SpotifyPlayer {
    private SpotifyAppRemote spotifyAppRemote;

    if (spotifyAppRemote == null) {
        Log.e("Spotify", "Spotify not connected");
        return;
    }

    int randomIndex = (int) (Math.random() * trackUris.length);
    String randomTrack = trackUris[randomIndex];

    spotifyAppRemote.getPlayerApi().play(randomTrack);
    Log.d("Spotify", "Playing random track: " + randomTrack);
    public void playSong(){

    }


    private void setContentView(int spotifyPlayer) {
    }

    private void playPlaylist() {
        if (spotifyAppRemote != null) {
            spotifyAppRemote.getPlayerApi().play("spotify:playlist:37i9dQZF1DX2sUQwD7tbmL");
        }
    }
}
