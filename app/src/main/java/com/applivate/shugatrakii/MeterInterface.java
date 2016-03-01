
package com.applivate.shugatrakii;


/****************************************************************************
 * METER INTERFACE
 * 
 * <h3>Purpose of Activity:</h3>
 * 		&nbsp; This interface is the one declared just before the 
 * 		{@link BaseService#onHandleIntent(android.content.Intent)}
 * 		figures out which meter to user start. It will contain
 * 		any and all of the calls that BaseService will be able to
 * 		call on the meter classes
 * 
 * <h3>Update notes:</h3>
 * 		v0.1.5:
 * 		&nbsp; Final beta MeterInterface ready
 * 
 * <h3>Known errors:</h3>
 * 		v0.1.5:
 * 		&nbsp; No errors known
 * 
 * 
 * 
 * @category ShugaTrak
 * @version V0.1.5: Ryan
 * @author  Current: Ryan Hirschthal Original:Ryan Hirschthal &nbsp;
 * {@code (c) 2014 Ryan Hirschthal, Advanced Decisions. (rhirschthal @ advanceddecisions . com)
 * All rights reserved}
 * @see {@link UltraMini}, {@link Ultra2}, {@link FreeStyle}, {@link BaseMeter}
 *****************************************************************************/
public interface MeterInterface {

	

	/**
	 * This is the command after constructing the device to tell
	 * the meter to start giving us info. The amount will be different
	 * depending on the thing, but should all of the information
	 * before ending
	 * 
	 * @return Returns the latest data. 
	 *  The newest reading will be at the very beginning, oldest at end,
	 *  with the order <br>
	 *  (i = 0; i < n, i+=3)
	 *  [i + 0] Blood Glucose Level<br>
	 *  [i + 1] Date <br>
	 *  [i + 2] Time <br>
	 *  
	 */
	public String[] communicateWithDevice();
	
	public void unRegister();
	
	/**
	 * This command is used at the end of {@link BaseService}'s
	 * main run, to start looking for a wake-up string. 
	 * The purpose of this code is to start
	 * a thread in the base meter with a passed in thread
	 */
	public void listenForWakeUpString();


	


	
	
}
