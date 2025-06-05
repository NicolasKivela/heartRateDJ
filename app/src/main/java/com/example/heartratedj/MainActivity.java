package com.example.heartratedj;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.List;



public class MainActivity extends AppCompatActivity {
    private AntPlusHeartRatePcc hrPcc;
    private PccReleaseHandle<AntPlusHeartRatePcc> releaseHandle;
    private final List<Integer> beatEventTimes = new ArrayList<>();
    private final List<Double> rrIntervals = new ArrayList<>();
    List<Double> rrValues = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
    private void subscribeToHeartRate() {
        final long[] startTime = {System.currentTimeMillis()};
        hrPcc.subscribeHeartRateDataEvent(
                (estTimestamp, eventFlags, computedHeartRate, heartBeatCount, heartBeatEventTime, dataState) -> {
                    //Log.d("ANT+", "HR: " + computedHeartRate);

                if (!beatEventTimes.isEmpty()){
                    int prevTime = beatEventTimes.get(beatEventTimes.size() - 1);
                    int heartBeatEventTimeInt = heartBeatEventTime.intValue();
                    int diff = Math.abs(heartBeatEventTimeInt - prevTime);

                    if (diff < 0) {
                        diff += 65536;
                    }

                    double rr = (diff * 1000.0) / 1024.0;
                    if (rrValues.size() >= 60) {
                        rrValues.remove(0);  // Remove the oldest
                    }
                    rrValues.add(rr);
                    if (System.currentTimeMillis() - startTime[0] >= 2 * 60 * 1000){
                        double rmssd = calculateRMSSD(rrValues);
                        Log.d("HRV", "RMSSD (2min): " + rmssd);
                        startTime[0] = System.currentTimeMillis();
                        rrValues.clear();
                    }
                }
                else {
                    beatEventTimes.add(heartBeatEventTime.intValue());
                }
                });
    }
    private double calculateRMSSD(List<Double> rr) {
        Log.d("check", "Size of rr" + rr.size());
        if (rr.size() < 2) return 0;
        Log.d("RMSSD","Calculating RMSSD" + rr);
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
