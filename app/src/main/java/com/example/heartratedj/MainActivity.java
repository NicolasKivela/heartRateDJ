package com.example.heartratedj;

import android.app.PendingIntent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.List;
import java.lang.String;
import android.content.Intent;
import android.net.Uri;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;


public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "bb6c51e8855242ba8cca8c251759cd79";
    private static final String REDIRECT_URI = "heartratedj://callback";
    private SpotifyAppRemote mSpotifyAppRemote;
    private AntPlusHeartRatePcc hrPcc;
    private PccReleaseHandle<AntPlusHeartRatePcc> releaseHandle;
    private final List<Double> beatEventTimes = new ArrayList<>();
    private final List<Double> rrIntervals = new ArrayList<>();
    List<Double> rrValues = new ArrayList<>();
    public static String accessToken;
    @Override protected void onStart() {
        super.onStart();

        ConnectionParams params = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)   // EXACTLY the same string
                .showAuthView(true)             // pops the green “Allow” sheet once
                .build();

        SpotifyAppRemote.connect(this, params, new Connector.ConnectionListener() {
            @Override public void onConnected(SpotifyAppRemote remote) {
                mSpotifyAppRemote = remote;
                Log.d("MainActivity", "Connected!");
                // e.g. remote.playerApi.play("spotify:playlist:37i9dQZF1DX2sUQwD7tbmL");
            }

            @Override public void onFailure(Throwable error) {
                Log.e("MainActivity", "AppRemote error", error);
            }
        });
    }

    @Override protected void onStop() {
        super.onStop();
        if (mSpotifyAppRemote != null) {
            SpotifyAppRemote.disconnect(mSpotifyAppRemote);
            mSpotifyAppRemote = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.antConnect).setOnClickListener(v -> {
            Log.d("ANT CONNECT", "CONNECTED TO ANT");
            antConnection();
        });
        findViewById(R.id.btnSpotifyAuth).setOnClickListener(v -> {

        });
        findViewById(R.id.playMusic).setOnClickListener(v -> {
            Log.d("Play music clicked", "Play Music");
            SpotifyPlayer player = new SpotifyPlayer();
            Log.d("SpotifyPLayer", "player" + player);
            try {
                player.search_songs();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });





    }
    private AuthorizationRequest authRequest;
    private AuthorizationService authService;
    private static final String ACTION_HANDLE_AUTH = "HANDLE_AUTH_DONE";




    private void antConnection(){
        releaseHandle = AntPlusHeartRatePcc.requestAccess(this, this,
                new AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc>() {
                    @Override
                    public void onResultReceived(AntPlusHeartRatePcc result, RequestAccessResult resultCode, DeviceState initialDeviceState) {
                        if (resultCode == RequestAccessResult.SUCCESS) {
                            hrPcc = result;
                            subscribeToHeartRate();
                        } else {
                            Log.e("ANT+", "Failed to connect: " + resultCode);
                        }
                    }
                },
                new AntPluginPcc.IDeviceStateChangeReceiver() {
                    @Override
                    public void onDeviceStateChange(DeviceState newDeviceState) {
                        Log.d("ANT+", "Device State Changed: " + newDeviceState);
                    }

                });
    }


    private static void getUserProfile(String accessToken){
        new Thread(()->{
            try {
                URL spotifyApi = new URL("https://api.spotify.com/v1/me");
                HttpURLConnection conn = (HttpURLConnection) spotifyApi.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                int responseCode = conn.getResponseCode();
                Log.d("Spotify", "Response code: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK){
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    Log.d("SpotifyAPI", "Response: " + response.toString());
                }
                else {
                    Log.e("SpotifyAPI", "Error: " + responseCode);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }).start();
    }
    private double lastProcessedBeatTime = -1;
    private void subscribeToHeartRate() {
        final long[] startTime = {System.currentTimeMillis()};

        hrPcc.subscribeHeartRateDataEvent(
                (estTimestamp, eventFlags, computedHeartRate, heartBeatCount, heartBeatEventTime, dataState) -> {
                    //Log.d("ANT+", "HR: " + computedHeartRate);
                    double heartBeatEventTimeInt = heartBeatEventTime.doubleValue();


                    if (heartBeatEventTimeInt == lastProcessedBeatTime) {
                        return;
                    }

                    lastProcessedBeatTime = heartBeatEventTimeInt;
                    //Log.d("heartbeattime", "This is HRBtime" + heartBeatEventTimeInt);
                    if (!beatEventTimes.isEmpty()){
                    double prevTime = beatEventTimes.get(beatEventTimes.size() - 1);
                    double diff = heartBeatEventTimeInt - prevTime;
                    //Log.d("DIFF", "This is" + diff);

                    if (diff < 0) {
                        diff += 65536;
                    }

                    double rr = diff * 1000.0;
                    //Log.d("RR", "This is rr" + rr);
                    if (rr > 300 && rr < 2000) {
                        if (rrValues.size() >= 60) {
                            rrValues.remove(0);  // Remove the oldest
                        }
                        rrValues.add(rr);
                        if (System.currentTimeMillis() - startTime[0] >= 1 * 60 * 1000) {
                            double rmssd = calculateRMSSD(rrValues);
                            //Log.d("HRV", "RMSSD (2min): " + rmssd);
                            startTime[0] = System.currentTimeMillis();
                            rrValues.clear();
                        }
                    }
                    else{
                        Log.d("RR", "Skipped out-of-range RR: " + rr);
                    }
                }
                    beatEventTimes.add(heartBeatEventTimeInt);

                });
    }
    private double calculateRMSSD(List<Double> rr) {
        //Log.d("check", "Size of rr" + rr.size());
        if (rr.size() < 2) return 0;
        //Log.d("RMSSD","Calculating RMSSD" + rr);
        double sum = 0;
        for (int i = 1; i < rr.size(); i++) {
            double diff = rr.get(i) - rr.get(i - 1);
            sum += diff * diff;
        }

        return Math.sqrt(sum / (rr.size() - 1));
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hrPcc != null) {
            hrPcc.releaseAccess();
        }
    }
}
