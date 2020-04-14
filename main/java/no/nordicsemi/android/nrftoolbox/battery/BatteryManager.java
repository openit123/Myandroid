package no.nordicsemi.android.nrftoolbox.battery;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import android.os.Build;
import android.util.Log;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.callback.DataReceivedCallback;
import no.nordicsemi.android.ble.common.callback.battery.BatteryLevelDataCallback;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.log.Logger;
import no.nordicsemi.android.nrftoolbox.parser.HeartRateMeasurementParser;
import no.nordicsemi.android.nrftoolbox.profile.LoggableBleManager;

import static android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_HIGH;

/**
 * The Ble Manager with Battery Service support.
 *
 * @param <T> The profile callbacks type.
 * @see BleManager
 */
@SuppressWarnings("WeakerAccess")
public abstract class BatteryManager<T extends BatteryManagerCallbacks> extends LoggableBleManager<T> {
	/** Battery Service UUID. */
	private final static UUID BATTERY_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
	/** Battery Level characteristic UUID. */
	private final static UUID BATTERY_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");
	String b1 = "",b2 = "",b3 = "",b4 = "",b5 = "",b6 = "",b7 = "",b8 = "",b9 = "";
	String b2f="",b2b="",b5f="",b5b="",b8f="",b8b="";
	String c1="",c2="",c3="",c4="",c5="",c6="";
	String hex1="",hex2="",hex3="",hex4="",hex5="",hex6="";
	private BluetoothGattCharacteristic mBatteryLevelCharacteristic;
	/** Last received Battery Level value. */
	private Integer mBatteryLevel;
	BluetoothGatt mBluetoothGatt;
	ArrayList<Integer> signaldata =  new ArrayList<Integer>();
	int motion = 0;
	int checkmotion = -1;
	int flags = 0;
	/**
	 * The manager constructor.
	 *
	 * @param context context.
	 */
	public BatteryManager(final Context context) {
		super(context);
	}

	private DataReceivedCallback mBatteryLevelDataCallback = new BatteryLevelDataCallback() {
		@Override
		public void onBatteryLevelChanged(@NonNull final BluetoothDevice device, final int batteryLevel) {
			log(LogContract.Log.Level.APPLICATION,"Battery Level received: " + batteryLevel + "%");
			mBatteryLevel = batteryLevel;
			mCallbacks.onBatteryLevelChanged(device, batteryLevel);
		}

		@Override
		public void onInvalidDataReceived(@NonNull final BluetoothDevice device, final @NonNull Data data) {
			log(Log.WARN, "Invalid Battery Level data received: " + data);
		}
	};

	public void readBatteryLevelCharacteristic() {
		if (isConnected()) {
			readCharacteristic(mBatteryLevelCharacteristic)
					.with(mBatteryLevelDataCallback)
					.fail((device, status) -> log(Log.WARN,"Battery Level characteristic not found"))
					.enqueue();
		}
	}

	public void enableBatteryLevelCharacteristicNotifications() {
		if (isConnected()) {
			// If the Battery Level characteristic is null, the request will be ignored
			setNotificationCallback(mBatteryLevelCharacteristic)
					.with(mBatteryLevelDataCallback);
			enableNotifications(mBatteryLevelCharacteristic)
					.done(device -> log(Log.INFO, "Battery Level notifications enabled"))
					.fail((device, status) -> log(Log.WARN, "Battery Level characteristic not found"))
					.enqueue();
		}
	}

	/**
	 * Disables Battery Level notifications on the Server.
	 */
	public void disableBatteryLevelCharacteristicNotifications() {
		if (isConnected()) {
			disableNotifications(mBatteryLevelCharacteristic)
					.done(device -> log(Log.INFO, "Battery Level notifications disabled"))
					.enqueue();
		}
	}
	/**
	 * Returns the last received Battery Level value.
	 * The value is set to null when the device disconnects.
	 * @return Battery Level value, in percent.
	 */
	public Integer getBatteryLevel() {
		return mBatteryLevel;
	}

	protected abstract class BatteryManagerGattCallback extends BleManagerGattCallback {

		@Override
		protected void initialize() {
			readBatteryLevelCharacteristic();
			enableBatteryLevelCharacteristicNotifications();
		}

		@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
		@Override
		protected boolean isOptionalServiceSupported(@NonNull final BluetoothGatt gatt) {
			final BluetoothGattService service = gatt.getService(BATTERY_SERVICE_UUID);
			if (service != null) {
				mBatteryLevelCharacteristic = service.getCharacteristic(BATTERY_LEVEL_CHARACTERISTIC_UUID);
				//gatt.requestMtu(247);
				//gatt.requestConnectionPriority(CONNECTION_PRIORITY_HIGH);
			}
			return mBatteryLevelCharacteristic != null;
		}

		@Override
		protected void onDeviceDisconnected() {
			mBatteryLevelCharacteristic = null;
			mBatteryLevel = null;
		}

		@Override
			public void onCharacteristicNotified(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
			if(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)!=null) {
				flags = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
				motion = flags>>1 & 0x01;

			}

			//hrValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
			if(motion==1) {
				int DataCount = 24;
				int ReceiveTime = DataCount * 10;
				signaldata.clear();


				for (int i = 2; i < ReceiveTime; i += DataCount) {

					signaldata.add(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, i)); //X
					signaldata.add(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, i + 2)); //Y
					signaldata.add(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, i + 4));  //Z

					if (characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 6) != null && characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 8) != null && characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 10) != null && characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 12) != null && characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 14) != null && characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 16) != null && characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 18) != null && characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 20) != null && characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 22) != null) {
						int get1 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 6);  //1
						int get2 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 8);  //2
						int get2x = get2 >> 8;
						int led1 = get1 << 8 | get2x;
						signaldata.add(led1);

						int get2y = get2 & 170;
						int get3 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 10);  //3
						int led2 = get2y << 16 | get3;
						signaldata.add(led2);

						int get4 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 12);  //4
						int get5 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 14);  //5
						int get5x = get5 >> 8;
						int led3 = get4 << 8 | get5x;
						signaldata.add(led3);

						int get5y = get5 & 170;
						int get6 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 16);  //6
						int led4 = get5y << 16 | get6;
						signaldata.add(led4);
						int get7 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 18);  //ECG
						int get8 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 20);  //ECG+PS-1
						int get8x = get8 >> 8;
						int ecg = get7 << 8 | get8x;
						signaldata.add(ecg);
						int get9 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 22);  //PS
						signaldata.add(get9);
					}
				}

			}

			if(motion==0) {

				int DataCount = 8;
				int ReceiveTime = DataCount * 10;
				signaldata.clear();


				for (int i = 2; i < ReceiveTime; i += DataCount) {

					signaldata.add(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, i)); //X
					signaldata.add(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, i + 2)); //Y
					signaldata.add(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, i + 4));  //Z

					if (characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 6) != null) {
						int get1 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + 6);  //ps
						signaldata.add(get1);
					}
				}

			}

			if(signaldata.size()>0) {
				if(signaldata.get(1)!=null) {
					mCallbacks.onHRValueReceived(gatt.getDevice(), signaldata, flags);
				}
			}
		}

		private boolean isHeartRateInUINT16(final byte value) {
			return ((value & 0x01) != 0);
		}
		private boolean isTxNEW(final byte value){
			return ((value & 0x02) != 0);
		}
		private  boolean isTxEnd(final byte value){
			return ((value & 0x04) != 0);
		}


	}
}
