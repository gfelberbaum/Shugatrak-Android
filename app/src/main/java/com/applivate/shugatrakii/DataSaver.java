package com.applivate.shugatrakii;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


/****************************************************************************
 * DATA SAVER
 * 
 * <h3>Purpose of Activity:</h3>
 * 		&nbsp; To make saving information
 * 		much simpler. This class will
 * 		hold ALL of the necessary 
 * 		code needed to save any information.
 * 
 * <p>
 * 	WARNING: 
 * 				CHANGING THE TITLE STRINGS 
 * 				REQUIRE USER TO RESET THEIR 
 * 				DATA
 * 
 * <h3>Update notes:</h3>
 * 		v0.1.5:
 * 		&nbsp; Final beta DataSaver ready
 * 
 * <h3>Known errors:</h3>
 * 		V0.1.5:
 * 		&nbsp; Nothing known yet
 * 
 * @category ShugaTrak
 * @version  V0.1.5: Ryan
 * @author Current: Ryan Hirschthal Original:Ryan Hirschthal &nbsp;
 * {@code (c) 2014 Ryan Hirschthal, Advanced Decisions. (rhirschthal @ advanceddecisions . com)
 * All rights reserved}
 *  *****************************************************************************/
public class DataSaver {
	/**
	 * Used to pull any new information out
	 */
	private  SharedPreferences pref;
	/**
	 * Used to save any new information
	 */
	private SharedPreferences.Editor editor;
	

	/**
	 * If there is nothing in the save place,
	 * this will be the typical return
	 */
	public static final String NO_ITEM = "---";
	
	
	
	//WARNING: SEE ABOVE
	
	/**
	 * This saves the meter type. It is used primarily
	 * to tell {@link BaseService} which meter to start
	 */
	public static final String meterType = "MeterType";
	/**
	 * The user-name of the account
	 */
	public static final String userName = "UserName";
	/**
	 * The password of the account
	 */
	public static final String Password = "Password";
	/**
	 * The newest Blood Glucose Level of Reading, used with
	 * {@link FragmentMeasurementActivity#updateVisuals()}
	 * and {@link BaseMeter#isMatchedReading(String, String, String)}
	 */
	public static final String lastNumber= "LatestReading";
	/**
	 * The newest Date of Reading, used with
	 * {@link FragmentMeasurementActivity#updateVisuals()}
	 * and {@link BaseMeter#isMatchedReading(String, String, String)}
	 */
	public static final String lastDate = "LatestDate";
	/**
	 * The newest Time of Reading, used with
	 * {@link FragmentMeasurementActivity#updateVisuals()}
	 * and {@link BaseMeter#isMatchedReading(String, String, String)}
	 */
	public static final String lastTime ="LatestTime";
	/**
	 * The current Adapter MAC-address, so that if the
	 * phone is restarted, it will still be able to find
	 * the address
	 */
	public static final String DeviceAddresses = " BLEMacAddress";
	/**
	 * A String, set as a Json String, of all of the readings that seemed
	 * to have failed going up
	 */
	public static final String MISSED_READINGS = "FailedReadings";


	public static final String KANSAS_CITY = "isThisAKansasCityAdapter";

	public static final String NAME_OF_ADAPTER = "New convention string";


	////////////////////////////////////////////////////////
	public static final String TOS = "AgreedToTOS";


	/**
	 * Constructor
	 * @param c Used to set up the save files
	 */
	public DataSaver(Context c){
		Logging.Verbose("DataSaver", "constructor - in");
		pref = PreferenceManager.getDefaultSharedPreferences(c);
		editor = pref.edit();
		editor.commit();//used to suppress lint warning on pref.edit command
		Logging.Verbose("DataSaver", "constructor - out");
	}
	/**
	 * Used to save any item
	 *
	 * @param title The place you want to save
	 * 			the String
	 * @param item The String that will be saved
	 */
	public void addSet(String title, String item){
		if (title == null) return;
		editor.putString(title, item);
		editor.commit();
	}

	/**
	 * Used to get a String that was saved at
	 * that location
	 *
	 * @param title The location of the String
	 * 			that is wanted
	 * @return The String desired. If there is
	 * 			no String there, it will return
	 * 			{@link #NO_ITEM}
	 */
	public String readSet(String title){
		return pref.getString(title, NO_ITEM);
	}


	public boolean didAcceptToS(){
		return pref.getBoolean(TOS, false);
	}

	public  void AcceptedToS( boolean accepted){
		editor.putBoolean(TOS, accepted);
		editor.commit();
	}

	public boolean isKCadapter(){
		return pref.getBoolean(KANSAS_CITY, false);
	}

	public  void setIsKCAdapter( boolean accepted){
		editor.putBoolean(KANSAS_CITY, accepted);
		editor.commit();
	}


	
	public void  removeMemoryOfReadings(){
		editor
				.remove(lastDate)
				.remove(lastNumber)
				.remove(lastTime)
				.remove(MISSED_READINGS)
				.commit();
	}
	
	//In order to save the failed attempts
	/**
	 * If there are any readings that failed to go
	 * to the portal, save them with this call.
	 * This call will append any old missed readings
	 * as well
	 * 
	 * @param readings The readings to be saved
	 */
	public void saveArray(ArrayList<Reading> readings){
		editor.putString(MISSED_READINGS, "," + readings.toString().replace("[", "").replace("]", "")+pref.getString(MISSED_READINGS, ""));



//		editor.putString(
//				MISSED_READINGS, readings.toString().replace("[", "").replace("]", "")+
//
//						(pref.getString(MISSED_READINGS, "").equals("")  ?  "": "," + pref.getString(MISSED_READINGS, ""))
//		);

		editor.commit();
	}
	
	/**
	 * To grab any readings that failed to go up to the 
	 * portal before
	 * 
	 * @return any readings that have yet to make it 
	 * 			successfully to the portal
	 */
	public String getArray(){
		return pref.getString(MISSED_READINGS, "");
	}
	
	/**
	 * If the readings successfully make it up to the 
	 * Portal, Remove the old readings with this call
	 */
	public void removeArray(){
		editor.remove(MISSED_READINGS);
		editor.commit();
	}


	
	
}
