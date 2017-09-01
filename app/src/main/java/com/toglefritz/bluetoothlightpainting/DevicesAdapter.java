package com.toglefritz.bluetoothlightpainting;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class DevicesAdapter extends ArrayAdapter<BluetoothDevice> {
    public DevicesAdapter(Context context, ArrayList<BluetoothDevice> devices) {
        super(context, 0, devices);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the device name and MAC address from the BluetoothDevice object
        BluetoothDevice device = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.bluetooth_list_item, parent, false);
        }
        // Lookup view for data population
        TextView name = (TextView) convertView.findViewById(R.id.deviceName);
        TextView address = (TextView) convertView.findViewById(R.id.address);
        // Populate the data into the template view using the data object
        name.setText(device.getName());
        address.setText("MAC Address: " + device.getAddress());


        // Return the completed view to render on screen
        return convertView;
    }
}