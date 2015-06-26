package com.applivate.shugatrak2;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/****************************************************************************
 * SEARCH ACTIVITY
 * 
 * <h3>Purpose of Activity:</h3> &nbsp; To look for Bluetooth adapters, and
 * connect to the desired adapter
 * 
 * <h3>Update notes:</h3> v0.1.5: &nbsp; Final beta SearchActivity ready
 * 
 * <h3>Known errors:</h3> V0.1.5: &nbsp; Will show any Ble Device
 * 
 * @category ShugaTrak
 * @version V0.1.5: Ryan
 * @author Current: Ryan Hirschthal Original:Ryan Hirschthal &nbsp;
 *         {@code (c) 2014 Ryan Hirschthal, Advanced Decisions. (rhirschthal @ advanceddecisions . com)
 * All rights reserved}
 * @see {@link TopLevelActivity}
 *****************************************************************************/
public class SearchActivity extends ListActivity {
	/**
	 * The list that holds both the device, and its RSSI
	 */
	private DeListAdapter DA;
	/**
	 * Adapter used to look for the Bluetooth device
	 */
	private BluetoothAdapter BleA;
	/**
	 * Boolean used for the class to know if it is searching or not
	 */
	private boolean searching;
	/**
	 * Handler used to set up a timer
	 */
	private Handler timerHandler;

	/**
	 * Internal id for an external request
	 */
	private static final int request_BT = 1;
	/**
	 * Time to scan for, in seconds
	 */
	private static final int SCAN_SECONDS = 30;

	private PostRun postRun;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		postRun = new PostRun();
		super.onCreate(savedInstanceState);
		// getActionBar().setTitle("Scanning for Devices");
		// getActionBar().setDisplayHomeAsUpEnabled(true);
		timerHandler = new Handler();
		BleA = ((BluetoothManager) getSystemService(BLUETOOTH_SERVICE)).getAdapter();
		if (BleA == null) {
			finish();
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.searching, menu);

		if (searching) {
			menu.findItem(R.id.search).setVisible(false);
			menu.findItem(R.id.stop).setVisible(true);
			menu.findItem(R.id.loading).setVisible(true).setActionView(R.layout.loading);
		} else {
			menu.findItem(R.id.search).setVisible(true);
			menu.findItem(R.id.stop).setVisible(false);
			menu.findItem(R.id.loading).setVisible(false).setActionView(null);

		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// What to do if A.Bar is clicked

		if (item.getItemId() == R.id.search) {
			DA.clear();
			scanLeDevice(true);
		} else if (item.getItemId() == R.id.stop) {
			scanLeDevice(false);
			// }else if(item.getItemId() == android.R.id.home){
			// String account;
			// if( !(account = new
			// DataSaver(this).readSet(DataSaver.DeviceAddresses)).equals(DataSaver.NO_ITEM)
			// ){
			// Intent BleIntent = new Intent(this, BleService.class);
			// BleIntent.putExtra(BleService.DEVICE_ADDRESS, account);
			// }
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * {@inheritDoc}
	 */
	public void onResume() {
		super.onResume();

		// If Bluetooth not enabled, enable it
		if (!BleA.isEnabled()) {
			if (!BleA.isEnabled()) {
				startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), request_BT);
			}
		}
		// Restart search
		DA = new DeListAdapter();
		setListAdapter(DA);
		scanLeDevice(true);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void onActivityResult(int request, int result, Intent data) {
		if (request == request_BT && result == Activity.RESULT_CANCELED) {
			finish();
			return;
		}
		super.onActivityResult(request, result, data);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void onPause() {
		super.onPause();
		scanLeDevice(false);
		DA.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	public void onBackPressed() {
		// If back is pressed and there already is a Adapter set
		String account;
		if (!(account = new DataSaver(this).readSet(DataSaver.DeviceAddresses)).equals(DataSaver.NO_ITEM)) {
			Intent BleIntent = new Intent(this, BleService.class);
			BleIntent.putExtra(BleService.DEVICE_ADDRESS, account);
		}
		super.onBackPressed();
	}

	/**
	 * {@inheritDoc}
	 */
	public void onListItemClick(ListView l, View v, int i, long id) {
		final BluetoothDevice device = DA.getDevice(i);
		if (device == null) {
			return;
		}

		scanLeDevice(false);

		// start BleService
		final Intent intent = new Intent(this, BleService.class);
		intent.putExtra(BleService.DEVICE_ADDRESS, device.getAddress());

		if (device.getName().toUpperCase().contains(ADAPTER_NEW_NAME)){
			(new DataSaver(getApplicationContext())).addSet(DataSaver.NAME_OF_ADAPTER,device.getName());
			(new DataSaver(getApplicationContext())).setIsKCAdapter(device.getName().split("-")[1].equals(ADAPTER_KC));
		}
		startService(intent);
		finish();
		Toast.makeText(this, R.string.adapter_is_set_up, Toast.LENGTH_SHORT).show();
		Toast.makeText(this, R.string.transferring_readings, Toast.LENGTH_LONG).show();
		Toast.makeText(this, R.string.goto_reading_screen, Toast.LENGTH_LONG).show();

	}

	/**
	 * a 'timer' that will do this at the end of it for use with
	 * {@link SearchActivity#scanLeDevice(boolean)}
	 */
	private class PostRun implements Runnable {
		public void run() {
			searching = false;
			BleA.stopLeScan(CallBack);
			invalidateOptionsMenu();

		}
	}

	/**
	 * This call will either run a scan, or stop it, depending on enable
	 * 
	 * @param enable
	 *            If true, will start scanning
	 */
	private void scanLeDevice(final boolean enable) {
		if (enable) {
			timerHandler.postDelayed(postRun, SCAN_SECONDS * BaseMeter.SECONDS);
			DA.clear();
			searching = true;
			BleA.startLeScan(CallBack);
			invalidateOptionsMenu();
		} else {
			searching = false;
			BleA.stopLeScan(CallBack);
			timerHandler.removeCallbacks(postRun, null);
			invalidateOptionsMenu();
		}
	}

	/**
	 * Makes the ListView that will be set up as the view for the screen.
	 */
	private class DeListAdapter extends BaseAdapter {
		List<BluetoothDevice> DL;
		LayoutInflater inf;
		List<Integer> RSSI;

		/**
		 * {@inheritDoc}
		 */
		public DeListAdapter() {
			super();
			inf = SearchActivity.this.getLayoutInflater();
			DL = new ArrayList<BluetoothDevice>();
			RSSI = new ArrayList<Integer>();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getCount() {
			return DL.size();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object getItem(int position) {
			return DL.get(position);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public long getItemId(int position) {
			return position;
		}

		public void addDevice(BluetoothDevice device, Integer rssi) {
			if (!DL.contains(device)) {
				DL.add(device);
				RSSI.add(rssi);
			}
			// else
			// RSSI.set((DL.indexOf(device)), rssi);

		}

		public BluetoothDevice getDevice(int position) {
			return DL.get(position);
		}

		public int getRSSI(int position) {
			return RSSI.get(position);
		}

		public void clear() {
			DL.clear();
			;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHelper holder;
			if (convertView == null) {// if no tag yet
				convertView = inf.inflate(R.layout.activity_searching, null);
				holder = new ViewHelper();

				holder.adapterText = (TextView) convertView.findViewById(R.id.name_of_device);
				holder.name = (TextView) convertView.findViewById(R.id.rssi_of_device);
				convertView.setTag(holder);

			} else {// if tag
				holder = (ViewHelper) convertView.getTag();
			}

			// set information about the tag
			BluetoothDevice device = DA.getDevice(position);
			String deviceName = device.getName();
			if (deviceName != null) {
				holder.adapterText.setText("ShugaTrak Bluetooth Adapter");
				if(deviceName.toUpperCase().contains(ADAPTER_NEW_NAME)){
					holder.name.setText(deviceName);
				}else{
					holder.name.setText(device.getAddress());
				}
			} else {
				holder.adapterText.setText("unknown name");
				holder.name.setText(device.getAddress());
			}

			return convertView;
		}

	}


	public static final String ADAPTER_NEW_NAME="SHG";
	public static final String ADAPTER_OLD_NAME_1="OLS425";
	public static final String ADAPTER_OLD_NAME_2="OLS426";
	public static final String ADAPTER_BUTTON = "01";
	public static final String ADAPTER_KC = "02";


	/**
	 * Class specifically designed to update as soon as there is a new device in
	 * view
	 */
	private BluetoothAdapter.LeScanCallback CallBack = new BluetoothAdapter.LeScanCallback() {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
			if (device.getName().contains(ADAPTER_OLD_NAME_1)||device.getName().contains(ADAPTER_OLD_NAME_2)||device.getName().toUpperCase().contains(ADAPTER_NEW_NAME)) {

				final Integer rs = rssi;
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						DA.addDevice(device, rs);
						DA.notifyDataSetChanged();

					}
				});
			}

		}
	};

	/**
	 * Static call to be used as tag
	 */
	public static class ViewHelper {      
		TextView adapterText;
		TextView name;
	}

}
