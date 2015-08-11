//connect to Internet sites to look at
//http://jatin4rise.wordpress.com/2010/10/03/webservicecallfromandroid/
//http://stackoverflow.com/questions/2256082/best-way-to-implement-client-server-database-architecture-in-an-android
//http://android-developers.blogspot.com/2010/07/multithreading-for-performance.html
//http://www.doubleencore.com/2013/12/bluetooth-smart-for-android/
package com.applivate.shugatrak2;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

/****************************************************************************
 * TOP LEVEL ACTIVITY
 * 
 * <h3>Purpose of Activity:</h3> &nbsp; Over arching Class that will hold all of
 * the fragments, or first level screens
 * 
 * <h3>Update notes:</h3> v0.1.5: &nbsp; Final beta TopLevelActivity ready
 * 
 * <h3>Known errors:</h3> V0.1.5: &nbsp; None known
 * 
 * @category ShugaTrak
 * @version V0.1.5: Ryan
 * @author Current: Ryan Hirschthal Original:Ryan Hirschthal &nbsp;
 *         {@code (c) 2014 Ryan Hirschthal, Advanced Decisions. (rhirschthal @ advanceddecisions . com)
 * All rights reserved}
 * @see {@link FragmentMeasurementActivity}, {@link FragmentSettingsActivity},
 *      {@link FragmentWebActivity}
 *****************************************************************************/
public class TopLevelActivity extends Activity {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a {@link FragmentStatePagerAdapter}
	 * derivative, which will keep every loaded fragment in memory. If this
	 * becomes too memory intensive, it may be best to switch to a
	 * {@link android.support.v13.app.FragmentStatePagerAdapter}.
	 */
	// Adapter
	private SectionsPagerAdapter mSectionsPagerAdapter;

	// Main page transition
	private ViewPager mViewPager;

	private DataSaver datas;

	private static final String[] meterNames = { Ultra2.signatureA, Ultra2.signatureB, UltraMini.SIGNATURE,
					FreeStyle.signatureA, FreeStyle.signatureB };

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_top_level);

		// Open dataSaver
		datas = new DataSaver(this);
		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				// When swiping between pages, select the
				// corresponding tab.
				getActionBar().setSelectedNavigationItem(position);
				if (position == 0){
					AlertToSetupAdapter(datas.readSet(DataSaver.DeviceAddresses).equals(DataSaver.NO_ITEM));
				}else if(position == 1){
					FragmentWebActivity.createWeb();
				}
			}
		});

		// The following sets up the Navigation Bars on top
		final ActionBar ACTION = getActionBar();
		 ACTION.setDisplayShowTitleEnabled(false); ///MAKES MAIN ACTION BAR
		// DISAPPEAR
		 ACTION.setDisplayShowHomeEnabled(false); ///MAKES MAIN ACTION BAR
		// DISAPPEAR

		ACTION.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Add the Tabs
		View readingTab = getLayoutInflater().inflate(R.layout.tab_view, null);
		View logTab = getLayoutInflater().inflate(R.layout.tab_view, null);
		View settingTab = getLayoutInflater().inflate(R.layout.tab_view, null);

		((TextView) readingTab.findViewById(R.id.TabTitle)).setText("Reading");
		((ImageView) readingTab.findViewById(R.id.TabImage)).setImageDrawable(getResources().getDrawable(
				R.drawable.meter12));
//		((ImageView) readingTab.findViewById(R.id.TabImage)).setLayoutParams(new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		((TextView) logTab.findViewById(R.id.TabTitle)).setText("Log");
		((ImageView) logTab.findViewById(R.id.TabImage))
						.setImageDrawable(getResources().getDrawable(R.drawable.ic_log));

		((TextView) settingTab.findViewById(R.id.TabTitle)).setText("Settings");
		((ImageView) settingTab.findViewById(R.id.TabImage)).setImageDrawable(getResources().getDrawable(
						R.drawable.ic_settings));

		ACTION.addTab(ACTION.newTab().setCustomView(readingTab).setTabListener(tListener));
		ACTION.addTab(ACTION.newTab()

		.setCustomView(logTab)
		// .setIcon(R.drawable.ic_log)
		// .setText("Log")
						.setTabListener(tListener));
		ACTION.addTab(ACTION.newTab().setCustomView(settingTab)
		// .setIcon(R.drawable.ic_settings)
		// .setText("Settings")
						.setTabListener(tListener));


		// If else rules depending on what is saved
		if (datas.readSet(DataSaver.DeviceAddresses).equals(DataSaver.NO_ITEM)) {
			mViewPager.setCurrentItem(2);
		} else {
//			Intent serviceIntent = new Intent(this, BleService.class);
//			serviceIntent.putExtra(BleService.DEVICE_ADDRESS, datas.readSet(DataSaver.DeviceAddresses));
//			startService(serviceIntent);

		}

		if (!datas.didAcceptToS()) {
			checkTerms();
		}


	}
	/**
	 * Makes a listener that will connect the A.Bar tabs, and the swipes
	 */
	ActionBar.TabListener tListener = new ActionBar.TabListener() {

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			// OVERRIDDEN FROM ABSTRACT
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			mViewPager.setCurrentItem(tab.getPosition());
//			if(tab.getPosition()==0){
//				AlertToSetupAdapter(datas.readSet(DataSaver.DeviceAddresses).equals(DataSaver.NO_ITEM));
//			}
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// OVERRIDDEN FROM ABSTRACT
		}
	};

	/**
	 * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		/**
		 * Decides which activity to put up
		 */
		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
//				AlertToSetupAdapter(datas.readSet(DataSaver.DeviceAddresses).equals(DataSaver.NO_ITEM));
				return new FragmentMeasurementActivity();
			case 1:

				return new FragmentWebActivity();
			case 2:
				return new FragmentSettingsActivity();
			default:
				// in case there is a weird return from the position. It should
				// never get to this point
				return new FragmentSettingsActivity();

			}
		}

		/**
		 * Returns the number of pages currently on the view
		 */
		@Override
		public int getCount() {
			// Show 3 total pages.
			return 3;
		}
	}

	/**
	 * quick call to change the meter name in the settings tab
	 */
	public void updateMeter() {
		TextView meterType = (TextView) findViewById(R.id.MeterType);

		meterType.setText(datas.readSet(DataSaver.meterType));
		if(!datas.readSet(DataSaver.meterType).equals(DataSaver.NO_ITEM)){
			((Button) findViewById(R.id.cnmtButton)).setText("Change Meter");
		}
	}

	/**
	 * call to verify whether the login was successful or not<br>
	 * CALLED FROM CLICKING VERIFY BUTTON
	 * 
	 * @param v
	 *            view, used for polymorphism
	 */
	public void verify(View v) {

		FragmentSettingsActivity.saveLogin();
		InternetSyncing sync = new InternetSyncing(this);
		sync.TestPassword();
		// FragmentWebActivity.createWeb();
	}

	/**
	 * call to change meters <br>
	 * CALLED FROM CLICKING CHANGE METER BUTTON
	 * 
	 * @param v
	 *            view, used for polymorphism
	 */
	public void cmnt(View v) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Currently Selected: " + datas.readSet(DataSaver.meterType)).setItems(meterNames,
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								((TextView) findViewById(R.id.stmtButton)).setClickable(true);
								((TextView) findViewById(R.id.stmtButton)).setEnabled(true);

								switch (which) {
								case 0:
									datas.addSet(DataSaver.meterType, Ultra2.signatureA);

									break;
								case 1:
									datas.addSet(DataSaver.meterType, Ultra2.signatureB);
									break;
								case 2:
									datas.addSet(DataSaver.meterType, UltraMini.SIGNATURE);
									break;
								case 3:
									datas.addSet(DataSaver.meterType, FreeStyle.signatureA);
									break;
								case 4:
									datas.addSet(DataSaver.meterType, FreeStyle.signatureB);
									break;

								default:
									break;
								}
								updateMeter();
								Toast.makeText(getApplicationContext(), R.string.press_adapter_button,
												Toast.LENGTH_LONG).show();
							}
						});
		builder.create().show();

		// Intent intent = new Intent(this, SearchActivity.class);
		// startActivity(intent);
	}

	/**
	 * Designed to setup a new adapter, as well as disconnect Bluetooth so it
	 * will appear as well <br>
	 * CALLED FROM CLICKING VERIFY BUTTON
	 * 
	 * @param v
	 *            view, used for polymorphism
	 * 
	 */
	public void setupMeter(View v) {
		Intent broadcastIntent = new Intent(BleService.SEARCH_DISCONNECT);
		sendBroadcast(broadcastIntent);
		Intent intent = new Intent(this, SearchActivity.class);
		startActivity(intent);
	}

	/**
	 * Shows the pop up of the about box<br>
	 * CALLED FROM CLICKING VERIFY BUTTON
	 * 
	 * @param v
	 *            view, used for polymorphism
	 */
	public void About(View v) {
		Logging.Info("TopLevelActivity.About", "building dialog box");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.AboutInfo).setTitle("ABOUT").setPositiveButton("OK", new OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {

			}
		});
		builder.setNegativeButton("Go To Website", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("http://www.shugatrak.com/"));
				startActivity(intent);

			}
		});

		builder.setNeutralButton(getString(R.string.DiagnoseButtonDescription), new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

				Logging.Info("TopLevelActivity.diagnose", "In diagnose");
				throw new RuntimeException("Throw Exception/Send Diagnostic");
			}

		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	public void checkTerms() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.tos).setTitle("Terms of Service")
						.setPositiveButton("Accept", new OnClickListener() {

							public void onClick(DialogInterface dialog, int which) {
								datas.AcceptedToS(true);
							}
						});
		builder.setNegativeButton("Decline", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				datas.AcceptedToS(false);
				finish();

			}
		}).setCancelable(false);
		AlertDialog dialog = builder.create();
		dialog.show();

	}

	//TODO add method header
	public void AlertToSetupAdapter(boolean shouldAlert){
		if (!shouldAlert) return;

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setMessage("Go to Settings screen to select a meter and set up adapter before proceeding").setTitle("No Adapter Selected") //TODO make sure that these get moved to Strings.xml
				.setPositiveButton("Ok", new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				});


		AlertDialog dialog = builder.create();
		dialog.show();

	}


	public void retryUpload(View v){
		((Button) findViewById(R.id.retryButton)).setText("Sending...");
		Intent retryIntent = new Intent( getApplicationContext(),BaseService.class);
		retryIntent.setAction(BaseService.RETRY_UPLOAD);
		startService(retryIntent);
	}



	public void changeTimeForKCAdapter(View v){
		Logging.Debug("You clicked the meter string");
		if(!Debug.KC_DEBUG) return;


		String[] times = {"1 minute", "5 minutes", "15 minutes","30 minutes", "1 hour", "4 hours", "6 hours", "12 hours"};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Set the times").setItems(times,
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						switch (which) {
							case 0:
								BleService.KC_ADAPTER_FREQUENCY =
//										4*
//										60*/*hours*/
										60*/*minutes*/
										1000/*seconds*/;


								break;
							case 1:
								BleService.KC_ADAPTER_FREQUENCY =
									5*
//									60*/*hours*/
									60*/*minutes*/
									1000/*seconds*/;

								break;
							case 2:
								BleService.KC_ADAPTER_FREQUENCY =
										15*
//										60*/*hours*/
										60*/*minutes*/
										1000/*seconds*/;

								break;
							case 3:
								BleService.KC_ADAPTER_FREQUENCY =
										30*
//										60*/*hours*/
										60*/*minutes*/
										1000/*seconds*/;

								break;
							case 4:
								BleService.KC_ADAPTER_FREQUENCY =
//										1*
										60*/*hours*/
										60*/*minutes*/
										1000/*seconds*/;

								break;
							case 5:
								BleService.KC_ADAPTER_FREQUENCY =
										4*
										60*/*hours*/
										60*/*minutes*/
										1000/*seconds*/;
								break;
							case 6:
								BleService.KC_ADAPTER_FREQUENCY =
										6*
												60*/*hours*/
												60*/*minutes*/
												1000/*seconds*/;

								break;
							case 7:
								BleService.KC_ADAPTER_FREQUENCY =
										12*
												60*/*hours*/
												60*/*minutes*/
												1000/*seconds*/;
								break;


							default:
								break;
						}
						updateMeter();
					}
				});
		builder.create().show();

		Intent reconnectIntent = new Intent(BleService.getNewReadings);
		sendBroadcast(reconnectIntent);


	}



}
