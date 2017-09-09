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
        1. Open the application and select the desired Bluetooth device from the list.
        2. Set a camera to a long exposure (bulb) setting and open  the shutter.
        3. Move the phone around in the space surrounding the Bluetooth device, within the camera's
           field of vision.
        4. Close the camera shutter when desired, or after a set long exposure time as elapsed.
        5. Post-process the long-exposure photographs as desired.
*/

package com.toglefritz.bluetoothlightpainting;

import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.toglefritz.bluetoothlightpainting.MainActivity.REQUEST_BLUETOOTH;

public class BluetoothSelectActivity extends AppCompatActivity {

    // Create a constant variable to store a tag to identify messages in the log
    private static final String TAG = MainActivity.class.getName();

    // This variable is used to create a socket for Bluetooth communication
    BluetoothSocket mSocket = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_selection);

        // Set the title in the action bar
        setTitle("Choose a device");

        /*
         To work with Bluetooth, we need to first establish a programatic connection with
         the phone's Bluetooth radio, called the Bluetooth adapter. For more information
         about working with Bluetooth in Android development, check out this tutorial:
         https://code.tutsplus.com/tutorials/android-quick-look-bluetoothadapter--mobile-7813
        */
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Although this should never happen with properly functioning smartphones, in the interest
        // of avoiding crashes, we will first check to ensure the device supports Bluetooth
        if (mBluetoothAdapter != null) {      // Device supports Bluetooth
            // We also need to check to make sure the Bluetooth functionality of the device is
            // turned on, if not, we will ask the user to enable it.
            if (mBluetoothAdapter.isEnabled()) {
                // Enabled. Work with Bluetooth.
                Log.d(TAG, "Bluetooth supported and enabled.");

                // To begin the process of visually mapping a Bluetooth signal around a
                // device, we will get a list of bonded Bluetooth peripherals.
                Set<BluetoothDevice> pairedDevicesSet = mBluetoothAdapter.getBondedDevices();
                ArrayList<BluetoothDevice> pairedDevices = new ArrayList<>();
                // Convert the set to an ArrayList containing a list of bonded Bluetooth devices
                for (final BluetoothDevice device : pairedDevicesSet) {
                    pairedDevices.add(device);
                }
                // Check to make sure there is at least one bonded Bluetooth device
                if (pairedDevices.size() > 0) {
                    // Create the adapter to convert the array to views that will be placed into
                    // the ListView in the layout file
                    DevicesAdapter adapter = new DevicesAdapter(this, pairedDevices);
                    // Attach the adapter to a ListView
                    ListView listView = (ListView) findViewById(R.id.deviceListView);
                    listView.setAdapter(adapter);

                    // Respond to clicks on Bluetooth devices in the ListView.
                    /*
                       When an item is clicked the application will switch to the MainActivity
                       where the strength of the Bluetooth connection will be translated into a color.
                       This activity will send the name of the chosen Bluetooth device to the MainActivity
                       so that the MainActivity will know to which device the user wishes the app
                       to connect.
                    */
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
                    {
                        @Override
                        public void onItemClick(AdapterView<?> adapter, View v, int position, long id)
                        {
                            // Get the BluetoothDevice object coresponding to the position in the ListView
                            // where the user clicked
                            BluetoothDevice device = (BluetoothDevice) adapter.getItemAtPosition(position);
                            String deviceName = device.getName();
                            Log.d(TAG, deviceName);
                            // Send the user to the MainActivity
                            Intent intent = new Intent(BluetoothSelectActivity.this, MainActivity.class);
                            // Send the name of the chosen Bluetooth device to the MainActivity
                            intent.putExtra("bluetoothDeviceName", device.getName());
                            BluetoothSelectActivity.this.startActivity(intent);
                        }
                    });
                }
                else {
                    // No paired Bluetooth devices found, prompt the user to go pair one and
                    // return to the application.
                    //Log.d(TAG, "No paired Bluetooth devices.");
                    new AlertDialog.Builder(this)
                            .setTitle("No paired devices")
                            .setMessage("Your phone is not paired to any Bluetooth devices. " +
                                    "Please pair a device and re-open the app.")
                            .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    System.exit(0);
                                }
                            })
                            .setIcon(R.drawable.ic_error_black_24dp)
                            .show();
                }
            }
            else {
                // We get here if Bluetooth is turned fof for the device. If this is the case,
                // display a prompt to the user to enable Bluetooth.
                //Log.d(TAG, "Bluetooth is turned off. Prompting user to enable.");
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBT, REQUEST_BLUETOOTH);
            }
        }
        else {
            // We get here if the device does not support Bluetooth. If this is the case, a
            // dialog will be displayed to the user with a button to exit the app.
            //Log.d(TAG, "Bluetooth not supported.");
            new AlertDialog.Builder(this)
                    .setTitle("Not compatible")
                    .setMessage("Your phone does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    // Create an options menu in the upper-right corner of the app
    // The menu contains links to useful pages about the project
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    // Respond to clicks on menu items. Each menu item opens a different website in the default
    // browser
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent browserIntent;

        switch (item.getItemId()) {
            case R.id.menuGallery:
                browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.toglefritz.com"));
                startActivity(browserIntent);
                return true;
            case R.id.menuInstructions:
                browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instructables.com"));
                startActivity(browserIntent);
                return true;
            case R.id.menuSource:
                browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.github.com"));
                startActivity(browserIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
