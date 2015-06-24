package com.applivate.shugatrak2;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/****************************************************************************
 * SYSTEM RECEIVER
 * 
 * <h3>Purpose of Activity:</h3>
 * 		&nbsp; To start 
 * 		{@link BleService} if the
 * 		adapter is already set up
 * 
 * <h3>Update notes:</h3>
 * 		v0.1.5:
 * 		&nbsp; Final beta SystemReceiver ready
 * 
 * <h3>Known errors:</h3>
 * 		V0.1.5:
 * 		&nbsp; None known
 * 
 * @category ShugaTrak
 * @version  V0.1.5: Ryan
 * @author Current: Ryan Hirschthal Original:Ryan Hirschthal &nbsp;
 * {@code (c) 2014 Ryan Hirschthal, Advanced Decisions. (rhirschthal @ advanceddecisions . com)
 * All rights reserved}
 *****************************************************************************/
public class SystemReceiver extends BroadcastReceiver {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Context context, Intent intent) {


		int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
		
		 if(state == BluetoothAdapter.STATE_ON){
			 String address =new DataSaver(context).readSet(DataSaver.DeviceAddresses);
			 if( ! (address).equals(DataSaver.NO_ITEM)){
					if(Debug.DEBUG)Logging.Info("SystemReceiver", "In the State on with BleInfo," +address);
					//Start BleService
					Intent selfLaunch = new Intent(context, BleService.class);
					selfLaunch.putExtra(BleService.DEVICE_ADDRESS, address);
					context.startService(selfLaunch);
				}
			}
	}


}