package no.nordicsemi.android.nrftoolbox.battery;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.ble.common.profile.battery.BatteryLevelCallback;

public interface BatteryManagerCallbacks extends BleManagerCallbacks, BatteryLevelCallback {
    void onHRValueReceived(final BluetoothDevice device, ArrayList<Integer> rrIntervals,int motion);

}
