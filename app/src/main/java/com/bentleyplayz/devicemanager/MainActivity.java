package com.bentleyplayz.devicemanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity {

    private static final int REQ_ENABLE_ADMIN = 1;

    private interface Action {
        void run() throws Exception;
    }

    private DevicePolicyManager dpm;
    private ComponentName admin;
    private TextView status;
    private final List<Button> adminButtons = new ArrayList<>();
    private Button enableButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        admin = new ComponentName(this, AdminReceiver.class);

        int pad = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, dp(32), pad, pad);

        TextView title = new TextView(this);
        title.setText("Device Manager");
        title.setTextSize(26);
        title.setTextColor(Color.rgb(16, 52, 120));
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(15);
        status.setPadding(0, dp(12), 0, dp(12));
        root.addView(status);

        enableButton = addButton(root, "Enable device admin", false, () -> {
            Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
            i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.admin_explanation));
            startActivityForResult(i, REQ_ENABLE_ADMIN);
        });

        addButton(root, "Lock screen now", true, () -> dpm.lockNow());

        addButton(root, "Disable / enable camera", true, () -> {
            boolean off = !dpm.getCameraDisabled(admin);
            dpm.setCameraDisabled(admin, off);
            toast(off ? "Camera disabled" : "Camera enabled");
        });

        addButton(root, "Set auto-lock to 30 seconds", true, () -> {
            dpm.setMaximumTimeToLock(admin, 30_000L);
            toast("Auto-lock set to 30s");
        });

        addButton(root, "Set a new screen lock password", true, () ->
                startActivity(new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD)));

        addButton(root, "Require 6+ character password", true, () -> {
            dpm.setPasswordQuality(admin, DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
            dpm.setPasswordMinimumLength(admin, 6);
            toast("Policy set. Change your lock to apply it.");
        });

        addButton(root, "Toggle wipe after 10 failed unlocks", true, () -> {
            final boolean enabling = dpm.getMaximumFailedPasswordsForWipe(admin) == 0;
            confirm(enabling
                            ? "Your device will be ERASED after 10 wrong unlock attempts. Enable?"
                            : "Turn off wipe after failed unlocks?",
                    () -> {
                        dpm.setMaximumFailedPasswordsForWipe(admin, enabling ? 10 : 0);
                        toast(enabling ? "Wipe after 10 fails ON" : "Wipe after fails OFF");
                        refresh();
                    });
        });

        addButton(root, "Factory reset (wipe all data)", true, this::wipeDialog);

        addButton(root, "Remove device admin", true, () -> {
            dpm.removeActiveAdmin(admin);
            refresh();
        });

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        refresh();
    }

    private void refresh() {
        boolean active = dpm.isAdminActive(admin);
        StringBuilder sb = new StringBuilder();
        sb.append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append(" (Android ").append(Build.VERSION.RELEASE).append(")\n");
        sb.append("Device admin: ").append(active ? "ACTIVE" : "NOT ACTIVE");
        if (active) {
            try {
                sb.append("\nCamera: ").append(dpm.getCameraDisabled(admin) ? "disabled" : "enabled");
                sb.append("\nStorage encryption: ").append(encryptionName(dpm.getStorageEncryptionStatus()));
                sb.append("\nAuto-lock: ").append(dpm.getMaximumTimeToLock(admin) / 1000).append("s");
                int w = dpm.getMaximumFailedPasswordsForWipe(admin);
                sb.append("\nWipe after failed unlocks: ").append(w == 0 ? "off" : String.valueOf(w));
                sb.append("\nLock meets policy: ").append(dpm.isActivePasswordSufficient() ? "yes" : "no");
            } catch (Exception e) {
                sb.append("\n(Some policies are restricted on this Android version)");
            }
        }
        status.setText(sb.toString());
        enableButton.setEnabled(!active);
        for (Button b : adminButtons) {
            b.setEnabled(active);
        }
    }

    private static String encryptionName(int s) {
        switch (s) {
            case DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED: return "unsupported";
            case DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE: return "inactive";
            case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING: return "activating";
            case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE: return "active";
            case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY: return "active (default key)";
            case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER: return "active (per user)";
            default: return "unknown";
        }
    }

    private void wipeDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Type WIPE");
        new AlertDialog.Builder(this)
                .setTitle("Erase everything?")
                .setMessage("This factory resets the device and cannot be undone. Type WIPE to confirm.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Erase", (d, w) -> {
                    if ("WIPE".equals(input.getText().toString().trim())) {
                        try {
                            dpm.wipeData(0);
                        } catch (Exception e) {
                            toast("Wipe failed: " + e.getMessage());
                        }
                    } else {
                        toast("Not confirmed");
                    }
                })
                .show();
    }

    private void confirm(String message, Action onYes) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Yes", (d, w) -> safe(onYes))
                .show();
    }

    private Button addButton(LinearLayout parent, String text, boolean needsAdmin, Action action) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener((View v) -> safe(action));
        parent.addView(b);
        if (needsAdmin) {
            adminButtons.add(b);
        }
        return b;
    }

    private void safe(Action a) {
        try {
            a.run();
            refresh();
        } catch (Exception e) {
            toast("Not allowed here: " + e.getMessage());
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
