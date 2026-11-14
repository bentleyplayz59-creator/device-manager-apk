package com.bentleyplayz.devicemanager;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        Toast.makeText(context, "Device admin enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        Toast.makeText(context, "Device admin disabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "Disabling device admin removes all management features.";
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        Toast.makeText(context, "Wrong password attempt detected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        Toast.makeText(context, "Unlock succeeded", Toast.LENGTH_SHORT).show();
    }
}
