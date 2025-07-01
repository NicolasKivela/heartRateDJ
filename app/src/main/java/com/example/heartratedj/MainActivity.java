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
import android.widget.TextView;

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
    private static SpotifyAppRemote mSpotifyAppRemote;

    private SpotifyPlayer player;

    private static double hrv;
    private static double hrvPrevious;

    private static double hrvDiff;
    private static int heartRate;

    private AntPlusHeartRatePcc hrPcc;
    private AuthorizationRequest authRequest;
    private AuthorizationService authService;
    private static final String ACTION_HANDLE_AUTH = "HANDLE_AUTH_DONE";
    private int  prevHrForJump   = -1;
    private long lastHrJumpCheck = 0;
    private TokenCallback webApiTokenCallback;
    private PccReleaseHandle<AntPlusHeartRatePcc> releaseHandle;
    private final List<Double> beatEventTimes = new ArrayList<>();
    private final List<Double> rrIntervals = new ArrayList<>();
    List<Double> rrValues = new ArrayList<>();

    public static String webApiAccessToken;

    TextView tvHeartRate, tvHRV, tvCurrentSong, tvNextSong;

    @Override protected void onStart() {
        super.onStart();

        ConnectionParams params = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build();

        SpotifyAppRemote.connect(this, params, new Connector.ConnectionListener() {
            @Override public void onConnected(SpotifyAppRemote remote) {
                mSpotifyAppRemote = remote;
                Log.d("MainActivity", "Connected!");
                if (player != null) {
                    player.updateRemote(remote);
                }
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
        tvHeartRate     = findViewById(R.id.tvHeartRate);
        tvHRV           = findViewById(R.id.tvHRV);
        tvCurrentSong   = findViewById(R.id.tvCurrentSong);
        tvNextSong      = findViewById(R.id.tvNextSong);
        handleAuthIntent(getIntent());
        findViewById(R.id.antConnect).setOnClickListener(v -> {
            Log.d("ANT CONNECT", "CONNECTED TO ANT");
            antConnection();
        });
        findViewById(R.id.btnSpotifyAuth).setOnClickListener(v -> {
            Log.d("WEB-API", "Connecting to spotify webpi");
            requestWebApiToken();
        });
        findViewById(R.id.playMusic).setOnClickListener(v -> {
            Log.d("Play music clicked", "Play Music");

            player.fetchCurrentPlayback();

            try {
                player.search_songs(true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });





    }

    public static SpotifyAppRemote getSpotifyAppRemote() {
        return mSpotifyAppRemote;
    }
    public static double getHrv(){
        return hrv;
    }
    public static double getPreviousHrv(){
        return hrvPrevious;
    }
    public static int getHeartRate(){
        return heartRate;
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleAuthIntent(intent);
    }
    private void handleAuthIntent(Intent intent) {
        // Was this intent generated by AppAuth?
        AuthorizationResponse resp = AuthorizationResponse.fromIntent(intent);
        AuthorizationException  ex  = AuthorizationException.fromIntent(intent);
        Log.d("RESPONSE", "RESPONSE" + resp);
        if (resp == null && ex == null) {
            // not an auth response → nothing to do
            return;
        }

        if (authService == null) {
            authService = new AuthorizationService(this);
        }

        if (resp != null) {
            TokenRequest tokenReq = resp.createTokenExchangeRequest();
            authService.performTokenRequest(tokenReq, (tokResp, tokEx) -> {
                if (tokResp != null) {
                    webApiAccessToken = tokResp.accessToken;
                    Log.d("WEBAPI", "access-token = " + webApiAccessToken);
                    getUserProfile(webApiAccessToken);

                    if (webApiTokenCallback != null) {
                        webApiTokenCallback.onTokenReady(webApiAccessToken);
                    }
                    player = new SpotifyPlayer(webApiAccessToken);
                    player.updateRemote(mSpotifyAppRemote);
                    player.setSongUpdateListener(new SpotifyPlayer.SongUpdateListener() {
                        @Override
                        public void onSongChanged(String name, String artist) {
                            runOnUiThread(() ->
                                    tvCurrentSong.setText("Now Playing: " + name + " – " + artist)
                            );
                        }

                        @Override
                        public void onNextSongQueued(String name, String artist) {
                            runOnUiThread(() ->
                                    tvNextSong.setText("Added to queue: " + name + " – " + artist)
                            );
                        }
                    });

                } else {
                    Log.e("WEBAPI", "token exchange failed", tokEx);
                }
            });
        } else {
            Log.e("WEBAPI", "authorization failed", ex);
        }
    }

    private static final String[] WEBAPI_SCOPES = {
            "user-read-private",
            "user-read-email",
            "playlist-read-private",
            "playlist-modify-private",
            "app-remote-control",
            "user-read-playback-state",
            "user-modify-playback-state",

    };
    public interface TokenCallback {
        void onTokenReady(String token);
    }
    private void requestWebApiToken() {
        AuthorizationServiceConfiguration cfg = new AuthorizationServiceConfiguration(
                Uri.parse("https://accounts.spotify.com/authorize"),
                Uri.parse("https://accounts.spotify.com/api/token"));

        AuthorizationRequest authReq = new AuthorizationRequest.Builder(
                cfg,
                CLIENT_ID,
                ResponseTypeValues.CODE,
                Uri.parse("heartratedj://callback")   // same redirect URI!
        )
                .setScopes(WEBAPI_SCOPES)             // <- any scopes you need
                .build();

        if (authService == null) authService = new AuthorizationService(this);

        Intent complete = new Intent(this, MainActivity.class)
                .setAction("WEBAPI_AUTH_DONE")
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        //complete.setData(Uri.parse(REDIRECT_URI));
        PendingIntent authIntent = PendingIntent.getActivity(
                this, 0, complete,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        Log.d("AUTHINTENT", "Auth intent" + authIntent);
        Log.d("AUTHINTENT", "Auth req" + authReq);
        authService.performAuthorizationRequest(authReq, authIntent);
        //this.webApiTokenCallback = cb;
    }
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
        Log.d("getUserProfile", "accessToken"+accessToken);
        new Thread(()->{
            try {
                URL spotifyApi = new URL("https://api.spotify.com/v1/me");
                HttpURLConnection conn = (HttpURLConnection) spotifyApi.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                Log.d("Spotify", "TOKEN: " + accessToken);

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
                    heartRate = computedHeartRate;
                    if (player != null) {
                        player.updateMetrics(hrv, computedHeartRate);
                    }

                    long now = System.currentTimeMillis();
                    int difference = heartRate - prevHrForJump;
                    boolean hrRising   = difference >= 10;
                    boolean hrFalling  = difference <= -20;

                    long risingCooldown = 12_000;
                    long fallingCooldown = 30_000;
                    long fallbackSkip    = 240_000;

                    long cooldown = hrRising ? risingCooldown : hrFalling ? fallingCooldown : fallbackSkip;
                    long elapsed = now - lastHrJumpCheck;

                    Log.d("HR-JUMP", "Current HR: " + heartRate);
                    Log.d("HR-JUMP", "Previous HR: " + prevHrForJump);
                    Log.d("HR-JUMP", "Diff: " + difference + " bpm");
                    Log.d("HR-JUMP", "Now: " + now);
                    Log.d("HR-JUMP", "Last check: " + lastHrJumpCheck);
                    Log.d("HR-JUMP", "HR rising? " + hrRising + " | falling? " + hrFalling);
                    Log.d("HR-JUMP", "Cooldown set to: " + cooldown + " ms");
                    Log.d("HR-JUMP", "Elapsed since last: " + elapsed + " ms");

                    if (elapsed >= cooldown && Math.abs(difference) >= 5) {
                        Log.d("HR-JUMP", "🔥 Skipping song due to HR change");
                        if (player != null) {
                            try {
                                player.search_songs(true);
                            } catch (IOException e) {
                                Log.e("HR-JUMP", "search_songs failed", e);
                            }
                        }
                        prevHrForJump = heartRate;
                        lastHrJumpCheck = now;
                    }


                    double heartBeatEventTimeInt = heartBeatEventTime.doubleValue();
                    //if (player != null) {
                       // player.adjustVolumeByHeartRate(computedHeartRate);
                    //}

                    if (heartBeatEventTimeInt == lastProcessedBeatTime) {
                        return;
                    }
                    runOnUiThread(() -> {
                        tvHeartRate.setText("Heart Rate: " + computedHeartRate + " bpm");
                    });
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
                            hrvPrevious = hrv;
                            double rmssd = calculateRMSSD(rrValues);
                            if (player != null) {
                                int newVolume = 65; // or calculate based on heart rate
                                player.setSpotifyVolumeWithoutDeviceId(newVolume);
                            }
                            //Log.d("HRV", "RMSSD (2min): " + rmssd);
                            hrv = rmssd;
                            if (player != null) {
                                player.updateMetrics(hrv, heartRate);   // <-- send fresh HRV + HR
                            }
                            runOnUiThread(() -> {
                                tvHRV.setText("HRV: " + String.format("%.1f", hrv));
                            });
                            startTime[0] = System.currentTimeMillis();
                            rrValues.clear();
                            //SpotifyPlayer player = new SpotifyPlayer(webApiAccessToken);

                            Log.d("SpotifyPLayer", "player" + player);
                            try {
                                if (player != null) {
                                    player.search_songs(false);
                                }
                                else{
                                    Log.d("PLAYER", "PLAYER IS NULL");
                                }

                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
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
