/*
    Author:  Toglefritz

    Description: This Android application allows the strength of a Bluetooth signal to be
        visualized in a long-exposure photograph. When connected to a Bluetooth device, five
        dots on the screen change color according to the strength of the Bluetooth signal (RSSI).
        When the signal is weak, the color is mostly blue, when the signal is strong, the signal is
        mostly red, and at intermediate signal strengths, the color is mostly green. By moving the
        phone around in the space around a Bluetooth device, while being recorded in a long
        exposure photograph, the result is a kind of heat map of Bluetooth signal strength.

    Instructions for Use:
        1. Open the application and, when prompted, connect to the desired Bluetooth device.
        2. Set a camera to a long exposure (bulb) setting and open  the shutter.
        3. Move the phone around in the space surrounding the Bluetooth device, within the camera's
           field of vision.
        4. Close the camera shutter when desired, or after a set long exposure time as elapsed.
        5. Post-process the long-exposure photographs as desired.
*/

package com.toglefritz.bluetoothlightpainting;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.MenuAdapter;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static android.telecom.Call.STATE_DISCONNECTED;

public class MainActivity extends AppCompatActivity  {

    // Create a constant variable to store a tag to idenfity messages in the log
    private static final String TAG = MainActivity.class.getName();

    // These three variables are used to capture and interpret swipes on the screen, used to
    // switch the type of brush used.
    private float x1,x2, y1, y2;
    static final int MIN_DISTANCE = 150;

    // These two variables are used to configure the bounds of the RSSI values.
    // You may wish to change these values to suit your own devices.
    // Note that RSSI values are actually negative. Therefore, when we look at the absolute value
    // of the RSSI value, a lower number indicates a stronger signal.
    long rssiMin = 15;
    long rssiMax = 1;

    // This variable is used to convert RSSI values to color values
    long mappedRSSI = 0;

    // The ultimate goal of this entire app is to set the color of the dots on the screen based on
    // an RSSI value from a Bluetooth connection. The color is expressed in RGB format.
    int red = 0;
    int green = 0;
    int blue = 0;

    // This is a constant variable we use later on if we need to prompt the user to turn
    // Bluetooth on.
    public static int REQUEST_BLUETOOTH = 1;

    // This variable is used to create a socket for Bluetooth communication
    BluetoothSocket mSocket = null;

    BluetoothDevice mdevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_painting_flipper);

        // Get the name of the Bluetooth device selected in the BluetoothSelectActivity
        Intent intent = getIntent();
        String selectedDevice = intent.getStringExtra("bluetoothDeviceName");


        // We have five dots that are colored according to the RSSI value
        final ImageView sbrush1 = (ImageView) findViewById(R.id.sbrush1);
        final ImageView sbrush2 = (ImageView) findViewById(R.id.sbrush2);
        final ImageView sbrush3 = (ImageView) findViewById(R.id.sbrush3);
        final ImageView sbrush4 = (ImageView) findViewById(R.id.sbrush4);
        final ImageView sbrush5 = (ImageView) findViewById(R.id.sbrush5);
        final ImageView cbrush1 = (ImageView) findViewById(R.id.cbrush1);
        final ImageView cbrush2 = (ImageView) findViewById(R.id.cbrush2);
        final ImageView cbrush3 = (ImageView) findViewById(R.id.cbrush3);
        final ImageView cbrush4 = (ImageView) findViewById(R.id.cbrush4);
        final ImageView cbrush5 = (ImageView) findViewById(R.id.cbrush5);
        final ImageView scbrush1 = (ImageView) findViewById(R.id.scbrush1);
        final ImageView bbrush1 = (ImageView) findViewById(R.id.bbrush1);
        final ImageView[] brushes = new ImageView[]{sbrush1, sbrush2, sbrush3, sbrush4, sbrush5,
                cbrush1, cbrush2, cbrush3, cbrush4, cbrush5, scbrush1, bbrush1};

        /*
         To work with Bluetooth, we need to first establish a programatic connection with
         the phone's Bluetooth radio, called the Bluetooth adapter. For more information
         about working with Bluetooth in Android development, check out this tutorial:
         https://code.tutsplus.com/tutorials/android-quick-look-bluetoothadapter--mobile-7813
        */
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // To begin the process of visually mapping a Bluetooth signal around a
        // device, we will get a list of bonded Bluetooth peripherals.
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        //  Get the name and address of each paired device.
        for (final BluetoothDevice device : pairedDevices) {
            String deviceName = device.getName();
            String deviceHardwareAddress = device.getAddress(); // MAC address

            if (device.getName().equals(selectedDevice)) {
                mdevice = device;
                mBluetoothAdapter.cancelDiscovery();
            }
        }

        // If the name of the current Bluetooth device matches the one chosen in the
        // BluetoothSelectActivity, connect to that device
        // Get the UUID for the device
        UUID uuid = mdevice.getUuids()[0].getUuid();
        Log.d(TAG, "" + uuid);


        try {
            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();
            // Create Bluetooth socket
            mSocket = mdevice.createRfcommSocketToServiceRecord(uuid);
            Log.d(TAG, "Created socket");
        } catch (IOException e) {
            Log.d(TAG, "Failed to create socket");
        }
        try {
            Log.d(TAG, "Attempting to connect. Attempt 1.");
            // Connect to the Bluetooth device
            mSocket.connect();
            Log.d(TAG, "Successfully connected");

            // After a successful connection is made, set a timer
            // to measure the Bluetooth RSSI once per millisecond
            final BluetoothGatt mBluetoothGatt = mdevice.connectGatt
                    (MainActivity.this, false, mGattCallback);
            new Timer().schedule(new TimerTask() {
                public void run() {
                    // Read Bluetooth RSSI. When this happens, it
                    // triggers a call to the onReadRemoteRssi
                    // function down below. It is inside that function
                    // where the RSSI is mapped to a color value
                    // for the dots on the phone screen.
                    boolean readRssiFlag = mBluetoothGatt.readRemoteRssi();

                    // Once we've measured the RSSI value mapped it
                    // to a value for red, green, and blue, we can now
                    // set the color of the dots on the screen to
                    // that value
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            for (ImageView brush : brushes) {
                                // Set the color of the brushes
                                brush.setColorFilter(Color.rgb(red, green, blue),
                                        PorterDuff.Mode.SRC_ATOP);
                            }
                        }
                    });
                }
            }, 1, 1);
        }
        catch (IOException e) {
            // We get here if the app fails to connect to a bonded Bluetooth
            // device. This is most likely because the selected device is
            // turned off or not within Bluetooth range. The user will be
            // prompted to check the device settings and afterwords, the
            // app will restart.
            Log.d(TAG, "" + e);
            e.printStackTrace();
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Connection failed!")
                    .setMessage("I'm sorry. I did not manage to connect to your Bluetooth device.")
                    .setPositiveButton("Check Settings", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Send user to the Bluetooth settings
                            startActivityForResult(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS), 0);
                        }
                    })
                    .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Close the app
                            System.exit(0);
                        }
                    })
                    .setIcon(R.drawable.ic_error_black_24dp)
                    .show();
        }
    }

    /*
      Set the app into immersive mode so that the navigation bar at the bottom of
      the screen, and the notification bar at the top of the screen are both hidden.
      We want these elements hidden because they would appear in the long exposure
      photographs otherwise.
    */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    // This function catches and interprets swipes on the screen. Swiping left or right is used
    // to chage the type of brush used. Swipes left or right switch the views forwards or backwards.
    // Swipes up and down are used to adjust the minimum RSSI value for adjusting the scaling
    // of the colors.
    public boolean onTouchEvent(MotionEvent event) {
        // Get the ViewFlipper from the layout
        final ViewFlipper flipper = (ViewFlipper) findViewById(R.id.flipper);

        // When the user swipes the screen, figure out the gesture
        switch(event.getAction()) {
            // Get the starting position of the swipe
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                y1 = event.getY();
                break;
            // Get the ending position of the swipe and compare it to the starting position
            // to determine if the swipe was L2R, R2L, upwards, or downwards
            case  MotionEvent.ACTION_UP:
                x2 = event.getX();
                y2 = event.getY();
                // Change in X
                float deltaX = x2 - x1;
                // Change in Y
                float deltaY = y2 - y1;

                // If the distance of the swipe was greater than the threshold distance
                if (Math.abs(deltaX) > MIN_DISTANCE || Math.abs(deltaY) > MIN_DISTANCE) {
                    // If the change in X was greater than the change in Y, we had a
                    // horizontal swipe
                    if(Math.abs(deltaX) >= Math.abs(deltaY)) {
                        // L2R swipe
                        if (deltaX > 0) {
                            flipper.showPrevious();
                        }
                        // R2L swipe
                        else if (deltaX < 0) {
                            flipper.showNext();
                        }
                    }
                    // If the change in Y was greater than the change in X, we had a
                    // vertical swipe
                    else if (Math.abs(deltaY) > Math.abs(deltaX)) {
                        // Downward swipe
                        if(deltaY > 0) {
                            rssiMin++;
                            Toast.makeText(MainActivity.this, "Minimum RSSI: -" + rssiMin, Toast.LENGTH_SHORT).show();
                        }
                        // Upward swipe
                        else if(deltaY < 0) {
                            if(rssiMin > rssiMax + 5) {
                                rssiMin--;
                                Toast.makeText(MainActivity.this, "Minimum RSSI: -" + rssiMin,
                                        Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Toast.makeText(MainActivity.this, "Minimum RSSI cannot be increased " +
                                        "further.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
                break;
        }

        return super.onTouchEvent(event);
    }

    // This is a callback for the BluetoothGatt connection
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        /*
         It is inside this function where a big chung of the work for the display is done. Up
         above, after connecting to the Bluetooth device, the app calls this function once per
         millisecond. This function reads the RSSI value for the currently connected device. This
         single value then must be mapped to three values: red, green, and blue. These three values
         are used to determine the color for the dots on the screen. To map a single RSSI value to
         a set of color values, a set of linear equations are used for each color channel.
         The linear equations used are described in a spreadsheet:
         https://drive.google.com/open?id=1SZRTu5d6wJdEmbz52OuqjMhpThPXu4DvGEEIlf1tvXM
        */
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            // Convert thee RSSI value to a value between 0 and 255
            mappedRSSI = map(-rssi, rssiMin, rssiMax, 0, 255);

            // RED
            // Convert the mapped RSSI value (between 0 and 255) to a value for the red channel
            if(mappedRSSI < 127.5) {
                // Below 50% of the pre-set RSSI range, there is no red in the dot color.
                red = 0;
            }
            else {
                // Otherwise, above 50% of the RSSI range, the value for red is determined by a
                // linear equation in slope-intercept form.
                red = (int) (2 * mappedRSSI - 255);
            }

            // GREEN
            // Convert the mapped RSSI value to a value for the green channel
            if(mappedRSSI < 127.5) {
                // Below 50% of the RSSI range, the value for green increases towards
                // its maximum at 127.5
                green = (int) (2 * mappedRSSI);
            }
            else {
                // Above 50% of the RSSI range, the value for three decreases towards zero
                green = (int) (-2 * mappedRSSI + 510);
            }

            // BLUE
            // Convert the mapped RSSI value to a value for the blue channel
            if(mappedRSSI < 127.5) {
                // Below 50% of the RSSI range, the value for blue is determined by
                // a linear equation
                blue = (int) (-2 * mappedRSSI + 255);
            }
            else if(mappedRSSI > 127.5) {
                // Above 50% of the RSSI range, there is no blue in the dot color
                blue = 0;
            }

            //Log.i(TAG, "Color: (" + red + ", " + green + ", " + blue + ")");
        }
    };

    // This is a simple function that will map one set of values to another. It is used to
    // normalize the RSSI values we get while running the app.
    long map(long x, long in_min, long in_max, long out_min, long out_max) {
        long mappedValue = (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
        if(mappedValue > out_max) {
            mappedValue = out_max;
        }
        else if(mappedValue < out_min) {
            mappedValue = out_min;
        }
        return mappedValue;
    }

    // If the user exits the application and restarts it, we want the app to reset and start over
    // to make sure the correct Bluetooth device is being connected. This will also happen if
    // a Bluetooth connection fails and the user go to and then returns from the Bluetooth settings.
    @Override
    protected void onRestart() {
        super.onRestart();

        // Restart the app
        Intent i = getBaseContext().getPackageManager().
                getLaunchIntentForPackage(getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }
}