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
    private AntPlusHeartRatePcc hrPcc;
    private PccReleaseHandle<AntPlusHeartRatePcc> releaseHandle;
    private final List<Double> beatEventTimes = new ArrayList<>();
    private final List<Double> rrIntervals = new ArrayList<>();
    List<Double> rrValues = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            Log.d("INTENT", "Handling redirect from onCreate");
            handleSpotifyRedirect(intent);
        }

        findViewById(R.id.antConnect).setOnClickListener(v -> {
            Log.d("ANT CONNECT", "CONNECTED TO ANT");
            antConnection();
        });
        findViewById(R.id.btnSpotifyAuth).setOnClickListener(v -> {
            startSpotifyAuthFlow();
        });
        findViewById(R.id.playMusic).setOnClickListener(v -> {
            Log.d("Play music clicked", "Play Music");
        });





    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (ACTION_HANDLE_AUTH.equals(intent.getAction())) {
            AuthorizationResponse resp = AuthorizationResponse.fromIntent(intent);
            AuthorizationException ex  = AuthorizationException.fromIntent(intent);

            if (resp != null) {                         // success, we have the code
                TokenRequest tokenRq = resp.createTokenExchangeRequest();
                authService.performTokenRequest(tokenRq,
                        (tokResp, tokEx) -> {
                            if (tokResp != null) {
                                Log.d("Spotify", "Access-Token = " + tokResp.accessToken);
                                getUserProfile(tokResp.accessToken);
                            } else {
                                Log.e("Spotify", "Token exchange failed", tokEx);
                            }
                        });
            } else {                                    // something went wrong
                Log.e("Spotify", "Authorization failed", ex);
            }
        }
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
    private void startSpotifyAuthFlow() {
        if (authService == null) authService = new AuthorizationService(this);

        AuthorizationServiceConfiguration cfg = new AuthorizationServiceConfiguration(
                Uri.parse("https://accounts.spotify.com/authorize"),
                Uri.parse("https://accounts.spotify.com/api/token"));

        AuthorizationRequest req = new AuthorizationRequest.Builder(
                cfg,
                "bb6c51e8855242ba8cca8c251759cd79",  // client-id
                ResponseTypeValues.CODE,
                Uri.parse("heartratedj://callback")   // must match Spotify dashboard
        )
                .setScopes("user-read-email", "user-read-private")
                .build();

        /* 1️⃣  Tell AppAuth where to return when it’s done */
        Intent completionIntent = new Intent(this, MainActivity.class)
                .setAction(ACTION_HANDLE_AUTH)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent completionPendingIntent = PendingIntent.getActivity(
                this,
                0,
                completionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        /* 2️⃣  Let AppAuth launch the Custom Tab and manage the redirect */
        authService.performAuthorizationRequest(req, completionPendingIntent);  // <-- correct call :contentReference[oaicite:2]{index=2}
    }


    private void handleSpotifyRedirect(Intent intent) {
        Log.d("AUTH", "Handling intent: " + intent);
        Log.d("AUTH", "Data: " + intent.getData());
        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException ex      = AuthorizationException.fromIntent(intent);
        Log.d("RESPONSE", "REsponse" + response);
        if (ex != null) {
            Log.d("Spotify", "Authorization failed");
            Log.d("Spotify", "Type: " + ex.type); // general category (e.g., AUTHORIZATION_FAILED)
            Log.d("Spotify", "Code: " + ex.code); // specific error code
            Log.d("Spotify", "Description: " + ex.errorDescription); // human-readable explanation
            Log.d("Spotify", "URI: " + ex.errorUri); // link to documentation if available
            Log.d("Spotify", "JSON: " + ex.toJsonString()); // complete details as JSON
            Log.d("Spotify", "Cause: ", ex); // print full stack trace
        }
        if (response != null) {
            // success – we got the code
            Log.d("Spotify", "Auth code = " + response.authorizationCode);

            AuthorizationService authService = new AuthorizationService(this);
            TokenRequest tokenRequest = response.createTokenExchangeRequest(); // PKCE handled automatically

            authService.performTokenRequest(
                    tokenRequest,
                    (tokenResponse, exception) -> {
                        if (tokenResponse != null) {
                            Log.d("Spotify", "Access-Token: " + tokenResponse.accessToken);
                            getUserProfile(tokenResponse.accessToken);
                        } else {
                            Log.e("Spotify", "Token exchange failed", exception);
                        }
                    });
        } else if (ex != null) {
            Log.e("Spotify", "Authorization failed", ex);
            Log.e("Spotify", ex.toJsonString());
        }
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
