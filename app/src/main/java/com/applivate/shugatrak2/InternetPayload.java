package com.applivate.shugatrak2;


import java.util.ArrayList;




/****************************************************************************
 * INTERNET PAYLOAD
 * 
 * <h3>Purpose of Activity:</h3>
 * 		&nbsp; To make a clean
 * 		way to set up all of
 * 		the information that will
 * 		be sent to the portal
 * 
 * <h3>Update notes:</h3>
 * 		v0.1.5:
 * 		&nbsp; Final beta InternetPayload ready
 * 
 * <h3>Known errors:</h3>
 * 		V0.1.5:
 * 		&nbsp; No known problems
 * 
 * @category ShugaTrak
 * @version  V0.1.5: Ryan
 * @author  Current: Ryan Hirschthal Original:Ryan Hirschthal &nbsp;
 * {@code (c) 2014 Ryan Hirschthal, Advanced Decisions. (rhirschthal @ advanceddecisions . com)
 * All rights reserved}
 * @see {@link InternetSyncing}, {@link Reading}
 *****************************************************************************/
public class InternetPayload {
	/**
	 * Email of the account
	 */
	private String email;
	/**
	 * Password of the account
	 */
	private String password;
	/**
	 * A list of readings to be
	 * sent up to the Portal.
	 * This list is new readings
	 * only, old readings will
	 * be added at a different 
	 * point in the code
	 */
	private ArrayList<Reading> readings;


	private String versionCode = Debug.VERSION_NUMBER;

	private String VersionName = ""+Debug.VERSION_CODE;


	public String errors;
	private String baseInfo;

	/**
	 * Constructor
	 * @param email {@link #email}
	 * @param password {@link #password}
	 * @param reading {@link #readings}
	 */
	public InternetPayload(String email, String password, ArrayList<Reading> reading, String baseInfo){
		this.email = email;
		this.password = password;
		this.readings = reading;
		this.baseInfo = baseInfo;
	}
	
	
	public InternetPayload(String email, String password, ArrayList<Reading> reading, String ErrorInformation, String baseInfo ){
		this.email = email;
		this.password = password;
		this.readings = reading;
		errors = ErrorInformation;

		this.baseInfo = baseInfo;
	}

	/**
	 * Returns this class, as a Json object
	 */
	public String toString(){
		
	
		String returnString;
		if(errors == null)

			returnString =
					"{\"email\":\"" + email +
					"\",\"password\":\"" + password +
					"\",\"app_version\":\"a, " + versionCode +"/ "+VersionName+
					"\",\"mobile_device_info\":\"" + baseInfo+
					"\",\"readings\":" + readings;
		else{

			returnString = "{\"email\":\"" + email +
					"\",\"password\":\"" + password +
					"\",\"meter_error_dump\":\"" + errors +
					"\",\"app_version\":\"a, " + versionCode+" / "+VersionName+
					"\",\"mobile_device_info\":\"" + baseInfo+
					"\",\"readings\":" + readings;
		}
	
		
		return returnString.replace("]", "");//gets rid of end so that uploading it won't mess up with the tailing of the data saved
	}

	
	public ArrayList<Reading> getReadings() {
		return readings;
	}


}
