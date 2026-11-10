package com.tdeboer.widgetapp.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;

// android tells here how the install went, or that the user still has to confirm it
public class InstallResultReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            UpdateManager.onConfirmationNeeded(confirmIntent(intent));
        } else {
            UpdateManager.onInstallResult(status, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE));
        }
    }

    @SuppressWarnings("deprecation")
    private static Intent confirmIntent(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class);
        }
        return intent.getParcelableExtra(Intent.EXTRA_INTENT);
    }
}
