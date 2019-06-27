package com.tti.tticaplog.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Debug;

import com.tti.tticaplog.service.CaptureLogService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            Intent caplogserviceintent = new Intent(context, CaptureLogService.class);
            context.startService(caplogserviceintent);
        }
    }
}
