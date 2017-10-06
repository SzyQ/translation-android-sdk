package io.stringx;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import io.stringx.client.R;

class StringXActivityBase extends AppCompatActivity {


    public static final int REQUEST_CODE = 3001;
    public static final String PACKAGE_NAME = "io.stringx";
    public static final String STRINGX_ACTIVITY = "io.stringx.MainActivity";
    public static final String KEY_PACKAGE = "KEY_PACKAGE";
    private static final int REQUEST_CODE_TRANSLATE = 3002;
    private BroadcastReceiver receiver;

    protected void startStringXService() throws UnsupportedLanguageException {
        final StringX stringX = StringX.get(this);
        stringX.setEnabled(true);
        stringX.setEnabled(stringX.getDeviceLanguage(), true);
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(PACKAGE_NAME, STRINGX_ACTIVITY));
        intent.putExtra(KEY_PACKAGE, getPackageName());
        try {
            StringX.TranslationListener listener = stringX.getListener();
            if(listener != null){
                listener.onTranslationLaunched();
            }
            startActivityForResult(intent, REQUEST_CODE_TRANSLATE);
        } catch (ActivityNotFoundException e) {
            CharSequence label;
            try {
                label = getPackageManager().getPackageInfo(getPackageName(), 0).applicationInfo.loadLabel(getPackageManager());
            } catch (PackageManager.NameNotFoundException e1) {
                label = getString(R.string.sX_app);
            }
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.sX_dialog_title,"stringX"))
                    .setMessage(label + " " + getString(R.string.sX_dialog_message,"stringX"))
                    .setPositiveButton(R.string.sX_dialog_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            registerPackageChangedReceiver();
                            openStore(PACKAGE_NAME);
                        }
                    }).show();
        }
    }

    private void openStore(String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException anfe) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_TRANSLATE) {
            if (resultCode == RESULT_OK) {
                StringX.TranslationListener listener = StringX.get(StringXActivityBase.this).getListener();
                if(listener != null){
                    listener.onApplicationTranslated();
                }
                setResult(RESULT_OK);
                finish();
                StringX stringX = StringX.get(this);
                stringX.invalidate();
                stringX.restart();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void registerPackageChangedReceiver() {
        if (receiver != null) {
            return;
        }
        LL.d("Registering package listener");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addDataScheme("package");
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LL.d("Package added: " + intent.getData());
                Uri data = intent.getData();
                if (data != null && PACKAGE_NAME.equals(data.getSchemeSpecificPart())) {
                    Intent stringxIntent = new Intent();
                    stringxIntent.setComponent(new ComponentName(PACKAGE_NAME, STRINGX_ACTIVITY));
                    stringxIntent.putExtra(KEY_PACKAGE, getPackageName());
                    try {
                        StringX.TranslationListener listener = StringX.get(StringXActivityBase.this).getListener();
                        if(listener != null){
                            listener.onTranslationLaunched();
                        }
                        startActivityForResult(stringxIntent, REQUEST_CODE_TRANSLATE);
                    } catch (Exception ignored) {
                        LL.e("Failed to start stringx!", ignored);
                    }
                    unregisterReceiver();
                }
            }
        };
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver();
    }

    private void unregisterReceiver() {
        if (receiver != null) {
            try {
                unregisterReceiver(receiver);
            } catch (Exception ignored) {

            }
        }
    }
}
