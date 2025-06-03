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

public class MainActivity extends AppCompatActivity {
    private AntPlusHeartRatePcc hrPcc;
    private PccReleaseHandle<AntPlusHeartRatePcc> releaseHandle;

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
        hrPcc.subscribeHeartRateDataEvent(
                (estTimestamp, eventFlags, computedHeartRate, heartBeatCount, heartBeatEventTime, dataState) ->
                        Log.d("ANT+", "HR: " + computedHeartRate)

        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hrPcc != null) {
            hrPcc.releaseAccess();
        }
    }
}
