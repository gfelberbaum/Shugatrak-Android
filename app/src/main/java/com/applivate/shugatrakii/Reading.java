package com.applivate.shugatrakii;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.os.Build;
import android.text.format.Time;

/****************************************************************************
 * READING
 * 
 * <h3>Purpose of Activity:</h3>
 * 		&nbsp; This class handles
 * 		sets up a wrapper for a
 * 		single reading.
 * 
 * <h3>Update notes:</h3>
 * 		v0.1.5:
 * 		&nbsp; Final beta Reading ready
 * 
 * <h3>Known errors:</h3>
 * 		V0.1.5:
 * 		&nbsp; None known
 * 
 * @category ShugaTrak
 * @version  V0.1.5: Ryan
 * @author Current: Ryan Hirschthal; Original: Ryan Hirschthal &nbsp;
 * {@code (c) 2014 Ryan Hirschthal, Advanced Decisions. (rhirschthal @ advanceddecisions . com)
 * All rights reserved}
 * @see {@link InternetPayload}
 *****************************************************************************/
public class Reading {

	/**
	 * Blood Glucose Level
	 */
	private int value;
	/**
	 * Date and time of the reading
	 */
	private String date;
	/**
	 * The meter type, set up with
	 * {@link DataSaver#meterType}
	 */
	private String meter_model;
	/**
	 * The MAC-address of the
	 * addapter, set up with
	 * {@link DataSaver#DeviceAddresses}
	 */
	private String adapter_sn;
	private String adapter_name;


	//////////////////////////
	private String versionCode = Debug.VERSION_NUMBER;
	private String OS = "a, " + Build.VERSION.RELEASE;
	private String currentTime;
	private int outOfRangeValue=-5;


	private String note = "";
	private String units = "mg/dL";
	private String meter_sn = "";
	private float phone_battery_levels;
	private String phone_timezone;
	private String reading_error_message = "";


	/**
	 * Base constructor
	 * 
	 * @param value {@link #value}
	 * @param date {@link #date}
	 * @param time {@link #date}
	 * @param meter_model {@link #meter_model}
	 * @param adapter_sn {@link #adapter_sn}
	 */
	public Reading(int value, String date, String time, String meter_model,
			String adapter_sn, String adapter_name, float phone_battery_levels) {

		if ((value > 20 && value < 600) || (value == -1 || value == -2)) {
			this.value = value;

		} else {
			outOfRangeValue = value;
			if (value < 20) {
				this.value = -2;
			} else {
				this.value = -1;
			}
		}

		this.meter_model = meter_model;
		this.adapter_sn = adapter_sn;
		this.adapter_name = adapter_name;

		Time now = new Time();
		now.setToNow();


		currentTime=now.format("%m-%d-%Y %H:%M");

		phone_timezone = now.timezone;





		SimpleDateFormat original = new SimpleDateFormat(
				"MMM dd, yyyy hh:mm a", Locale.US);
		Date trydate;
		try {
			trydate = original.parse((date.trim() + " " + time.trim()).trim());
			original.applyPattern("MM-dd-yyyy HH:mm");
			this.date = original.format(trydate);
		} catch (ParseException ex) {
			Logging.Error("Reading.ERROR PARSING", "", ex);
		}

		this.phone_battery_levels = phone_battery_levels;
	}

	//returns the class set up as a Json Object
	public String toString(){

		return
				"{\"value\":" + value +
				",\"date\":\"" + date + 
				"\",\"note\":\""+note +
				"\",\"units\":\""+units +
				"\",\"meter_model\":\"" +meter_model +
				"\",\"meter_sn\":\""+meter_sn +
				"\",\"os_version\":\""+OS+
				"\",\"phone_battery_levels\":\""+phone_battery_levels +
				"\",\"phone_date\":\""+currentTime +
				"\",\"phone_time_zone\":\""+phone_timezone +
				"\",\"reading_error_message\":\""+reading_error_message +
				(outOfRangeValue!=-5? "\",\"outOfRangeValue\":\"" + outOfRangeValue :"")+
				"\",\"adapter_sn\":\"" + adapter_sn +
				"\",\"adapter_name\":\"" + adapter_name +
				"\"}";

//				"\",\"Version\":\""+versionCode+
//				"\",\"phone_date\":\""+currentTime +
	}


}
