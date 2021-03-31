package io.github.infinyte7.ankilink;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ichi2.anki.api.AddContentApi;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageButton startButton, stopButton, optionsButton;
    private RelativeLayout startButtonLayout, stopButtonLayout;

    private int port = 8080;
    private boolean isLoopback;

    private String ANKI_LINK = "Anki Link";
    private static final String LOG_TAG = "AnkiDroidAPI";

    private AnkiDroidHelper mAnkiDroid;
    AnkiNanoHTTPD ankiServer;

    private static final int PERMISSION_REQUEST_CODE = 200;

    private TextView connectedTv, ipAddressTv;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        startButton = findViewById(R.id.start_image_button);
        stopButton = findViewById(R.id.stop_image_button);
        optionsButton = findViewById(R.id.optionsButton);

        startButtonLayout = findViewById(R.id.layout_start_button);
        stopButtonLayout = findViewById(R.id.layout_stop_button);

        connectedTv = findViewById(R.id.connected_tv);
        ipAddressTv = findViewById(R.id.ip_address_tv);

        mAnkiDroid = new AnkiDroidHelper(this);

        if (checkPermission()) {
            Log.i(LOG_TAG, String.valueOf(mAnkiDroid.isApiAvailable(MainActivity.this)));
        } else {
            requestPermission();
        }

        startButton.setOnClickListener(v -> {
            Log.i(ANKI_LINK, ":: Server Started");
            startServer();
        });

        stopButton.setOnClickListener(v -> {
            Log.i(ANKI_LINK, ":: Server Stopped");
            stopServer();
        });

        optionsButton.setOnClickListener(v -> {
            showPopupMenu();
        });
    }

    // start server
    private void startServer() {
        // if prefs changed and restart then stop it then start
        if (ankiServer != null && ankiServer.isAlive()) {
            ankiServer.stop();
        }

        port = Integer.parseInt(sharedPreferences.getString(getString(R.string.port), "8080"));
        isLoopback = sharedPreferences.getBoolean(getString(R.string.loopback_ip), true);

        try {
            if (isLoopback) {
                ankiServer = new AnkiNanoHTTPD("localhost", port, getApplicationContext());
            } else {
                ankiServer = new AnkiNanoHTTPD(port, getApplicationContext());
            }

            ankiServer.start();

        } catch (IOException e) {
            e.printStackTrace();
        }

        String ipAddress;
        if (isLoopback) {
            ipAddress = ankiServer.getHostname() + ":" + ankiServer.getListeningPort();
        } else {
            String ip = getIPAddress(true);
            ipAddress = ip + ":" + ankiServer.getListeningPort();
        }

        ipAddressTv.setText(ipAddress);
        startButtonLayout.setVisibility(View.GONE);
        stopButtonLayout.setVisibility(View.VISIBLE);
        connectedTv.setText(getString(R.string.connected));
        connectedTv.setTextColor(getResources().getColor(R.color.mat_green_500));

    }

    // stop running server
    private void stopServer() {
        if (ankiServer.isAlive()) {
            ankiServer.stop();
        }

        startButtonLayout.setVisibility(View.VISIBLE);
        stopButtonLayout.setVisibility(View.GONE);
        connectedTv.setText(getString(R.string.disconnected));
        connectedTv.setTextColor(getResources().getColor(R.color.mat_red_500));
        ipAddressTv.setText("");
    }


    private boolean checkPermission() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                || (ContextCompat.checkSelfPermission(this, AddContentApi.READ_WRITE_PERMISSION) != PackageManager.PERMISSION_GRANTED)) {
            // Permission is not granted
            return false;
        }
        return true;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, AddContentApi.READ_WRITE_PERMISSION},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED &&
                                ContextCompat.checkSelfPermission(this, AddContentApi.READ_WRITE_PERMISSION)
                                        != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel("You need to allow Storage and AnkiDroid read write permissions",
                                    (dialog, which) -> {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            requestPermission();
                                        }
                                    });
                        }
                    }
                }
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    public static File getFilesDir(Context c) {
        File filesDir;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            if (Build.VERSION.SDK_INT <= 18)
                filesDir = new File(Environment.getExternalStorageDirectory()
                        + "/Android/data/"
                        + c.getPackageName()
                        + "/files"
                );
            else
                filesDir = c.getExternalFilesDir(null);
        } else {
            filesDir = c.getFilesDir();
        }
        return filesDir;
    }


    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface networkInterface : interfaces) {
                List<InetAddress> addressList = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress addr : addressList) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (useIPv4) {

                            if (isIPv4) {
                                return sAddr;
                            }

                        } else {

                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%');
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }

                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    private void showPopupMenu() {
        PopupMenu popup = new PopupMenu(MainActivity.this, optionsButton, Gravity.END);
        popup.getMenuInflater().inflate(R.menu.option_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            Intent intent;

            switch (item.getItemId()) {
                case R.id.settings:
                    intent = new Intent(MainActivity.this, PreferencesActivity.class);
                    startActivity(intent);
                    return true;
                case R.id.help:
                    String url = "http://github.com/infinyte7/anki-link";
                    intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                    return true;
                case R.id.about:
                    return true;
            }

            return true;
        });
        popup.show();
    }
}