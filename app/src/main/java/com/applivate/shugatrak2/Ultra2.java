package com.applivate.shugatrak2;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

/****************************************************************************
 * ULTRA 2
 * 
 * <h3>Purpose of Activity:</h3>
 * 		&nbsp; This class handles
 * 		the commands and the decoding
 * 		of data from the Ultra2 and
 * 		Ultra Link meters. 
 * 
 * <h3>Update notes:</h3>
 * 		v0.1.5:
 * 		&nbsp; Final beta Ultra2 ready
 * 
 * <h3>Known errors:</h3>
 * 		V0.1.5:
 * 		&nbsp; None known
 * 
 * @category ShugaTrak
 * @version  V0.1.5: Ryan
 * @author  Current: Ryan Hirschthal Original:Ryan Hirschthal &nbsp;
 * {@code (c) 2014 Ryan Hirschthal, Advanced Decisions. (rhirschthal @ advanceddecisions . com)
 * All rights reserved}
 * @see {@link BaseMeter}, {@link MeterInterface}
 *****************************************************************************/
public class Ultra2 extends BaseMeter implements MeterInterface {


	public final static String signatureA = "OneTouch Ultra 2";
	public final static String signatureB = "OneTouch UltraLink";

	/**
	 * Saves the information from the meter in as a String
	 */
	private String receivedData;
	/**
	 * The minimum amount of information preferred before start calculating
	 */
	private final int minLength = 75;
	/**
	 * The number of "fields" in each record, used to make
	 * sure that most of the info is there so that errors
	 * such as no date field or checksum check outOfBounds
	 * happens
	 */
	private static final int LINES_IN_RECORD = 7;
	/**
	 * Command used by all meters to start receiving information
	 */
	private final byte[] command = {0x11, 0x0d, 'D', 'M', 'P'};
	/**
	 * CURRENTLY UNUSED, might not need to be <br>
	 * 
	 * Wake up command used ONLY by Ultra 2 Rev C
	 * This command will wake up the Rev C, but
	 * will put Rev A & B into a weird state
	 * Ultra 2revc does not need it
	 */
//	private final byte[] wakeupCommand = {0x02, 0x0A, 0x03, 0x05, 0x1F, 0x00, 0x00, 0x03, 0x4B, 0x5F};


	/**
	 * Holds the first instance of an unsuccessful
	 * (Broad definition) line that was processed
	 * to possibly be reprocessed later
	 */
	private int endOfProcess;
	/**
	 * Time that the program will Sleep,
	 * in seconds
	 */
	private final int sleepTime = 2 ;
	/**
	 * The difference between the checksum
	 * to the end of the record
	 */
	private final int checkSumOffset=6;
	
	
	
	
	

	/**
	 * Constructor
	 * @param c send to parent
	 */
	public Ultra2(Context c, String Sig){
		super(c,Sig);
		Logging.Info("Ultra 2", "Constructor");
		receivedData = "";
		endOfProcess = 0;
		repeatData = false;
		finalReadings = new ArrayList<String>();
	
	}
	

	
	protected void onNewData(byte[] byteArrayExtra) {
		//OVERRIDDEN FROM ABSTRACT
		String holder = "";
		for(byte aByte:byteArrayExtra)
		holder += "[" +aByte+"] " ;
		if(Debug.DEBUG)Logging.Info("Ultra2.OnNewData", new String(byteArrayExtra) + " " +byteArrayExtra + ":::: " +holder);

		receivedData+= new String(byteArrayExtra);
	}

	////////////////////////////////////////
	//COMMUNICATEWITHDEVICE/////////////////
	////////////////////////////////////////
	@Override
	public String[] communicateWithDevice() {
		createRegister();

		Logging.Info("Ultra 2.communicate with device", "start, sending message");
		sendCommand(command);

		Logging.Info("Ultra2.communicate with device", "sent command, starting");

		int numberResends = 0;
		int resendOrWakeup = 0;
		
		//Start while loop
		while( connected && (receivedData.equals("") || newInfo) && !repeatData && badGrabCount<3 &&numberResends<3 ){
			Logging.Info("Ultra2.communicate with device", "New loop");

			//set the new info flag false, it should either
			//be set to true by an async call onNewInfo
			//during the wait if any new info came in
			newInfo = false;
			try{
				SystemClock.sleep((long) sleepTime*SECONDS);
			} catch(Exception e){}


			if(receivedData.equals("")){

				if(numberResends==2){
					createNotification(R.string.meter_not_responding, R.raw.failure_sound, true);
					meterNotResponding=true;
					break;

				}

				//If there is nothing back
				//try one more time the normal command
				// then fluctuate back and forth
				//between command and wakeup command
				if(resendOrWakeup++%2 == 1)
					//					sendCommand(wakeupCommand)
					;
				else{
					sendCommand(command);
					Logging.Info("Ultra 2 communicate with device", "resent the command: " + ++numberResends);
				}

				//If received junk, wait and flush
			}else if(!receivedData.startsWith("P")){
				SystemClock.sleep(1*SECONDS);
				receivedData = "";
				sendCommand(command);

				//If have a good amount of data
			}else if(receivedData.length() > minLength){
				process(receivedData);
			}
		}//end while loop
		Logging.Info("Ultra2.communicate with device", "ended the for loop");

		process(receivedData);

		resetAdapterPhrase();
		//////////////////////////////////

		if(checkForReadings()){
			InternetSyncing.errorDump="BAD CHECKSUM\n" + receivedData;
		}
		
		
		return (String[]) finalReadings.toArray(new String[0]);
	}

	/**
	 * Will do a quick analysis of the data, then
	 * pass it to either {@link #processHeader(String)}
	 * or {@link #processLine(String)} to be processed
	 * 
	 * @param data the entire string of characters, received
	 *  		Bye the meter, this method specifically ignores
	 *  		previously successful ones
	 */
	public void process(String data){
		String[] dataArray = data.split("\n");

		Logging.Info("Ultra2.Process", "data array size: " + dataArray.length);
		if(endOfProcess == 0){
			processHeader(dataArray[0]);
			endOfProcess = 1;
		}

		int i = endOfProcess;
		for( i = endOfProcess; i< dataArray.length; i++){
			if(!processLine(dataArray[i])){
				break;
			}
			if(i==1){//if it is the first reading
				if(Debug.DEBUG)Logging.Info("Ultra2.Process", "Try visual update");
				try{

					update();//update visuals
					if(Debug.DEBUG)Logging.Info("Ultra2.Process", "Success");
				}catch(Exception ex){
					Logging.Error("Ultra2.Process", "error", ex);
				}
			}
			Logging.Info("Ultra2.Process", "line = " + i);
		}
		endOfProcess = i;


	}

	/**
	 * This will run through the records, individually, and process each one
	 * separately
	 * 
	 * @param record the record that will be checked next
	 * @return false if line is not complete OR is the newest already
	 */

	public boolean processLine(String record){

		if(record.contains("/r")){
			if(Debug.DEBUG)Logging.Info("Ultra2.Processing line", "Does not contain character");
			return false;
		}
		
		
		//CHECK SUM
		try{

			if(  Checksum(record)!= Integer.parseInt( record.substring(record.length()-checkSumOffset, record.length()).trim(), 16 )   ){
				Logging.Info("Ultra2.ProcessLine", "Bad Checksum");
				badGrabCount = 3;
				return false;
			}else{
				Logging.Info("Ultra2.ProcessLine", "Good Checksum");
				badGrabCount = 0;
			}
		}
		catch(Exception ex){
			Logging.Error("Ultra2.Processing line", "error on checksum", ex);
			return false;
		}

		if(Debug.DEBUG)Logging.Info("Ultra2.Processing line", "Passed checksum:start:" + record);
		record = record.replace(" ", "");// Removes all Spaces
		record = record.replace("\"", "");//Removes all double quotes (")
		String[] RecordLines = record.split(",");

		if (RecordLines.length<LINES_IN_RECORD){
			return false;
		}







		if(Debug.DEBUG)Logging.Info("Ultra2.Processing line", "removed useless spots" + record);





		//[0] is commandType and DoW UNUSED

		//[1] is date
		SimpleDateFormat original = new SimpleDateFormat("MM/dd/yy", Locale.US);
		Date date;
		String endDate = "";
		try {
			date = original.parse(RecordLines[1]);
			original.applyPattern("MMM dd, yyyy");
			endDate = original.format(date);
		} catch (ParseException ex) {
			Logging.Error("Ultra2.ProcessLine","ERROR PARSING",ex);
		}

		//[2] is time
		String endTime ="";
		Date time;
		original = new SimpleDateFormat("HH:mm:ss", Locale.US);
		try{
			time = original.parse(RecordLines[2]);
			original.applyPattern("h:mm a");
			endTime = original.format(time);
		}catch(ParseException ex){
			Logging.Error("Ultra2.ProcessLine","ERROR PARSING",ex);
		}
		if(endTime.length()<2) endTime = "01:00 am";



		//[3] is BGL
		int BGL;
		BGL = Integer.parseInt(RecordLines[3].replaceAll("[^0-9.]", ""));
		
//		if (BGL <20)
//			BGL = -2;
//		else if (BGL > 600)
//			BGL = -1;
		

		Logging.Info("Ultra2.Processing Line", "checking the databases");

		//Check here if the reading has already been stored before
		if(isMatchedReading(BGL+"", endDate, endTime)){
			return false;
		}

		//Append the info
		finalReadings.add(BGL+"");
		finalReadings.add(endDate);
		finalReadings.add(endTime);

		Logging.Info("Ultra2.Processing line", "Successfully went through");

		return true;
	}

	/**
	 * goes through the header, and if something should be done with it
	 * do it here. UNUSED currently
	 * @param header The header record
	 */
	public void processHeader(String header){
		if(Debug.DEBUG)Logging.Info("Ultra 2", "PcsHeader: " + header);
	}
	/**
	 * Makes a checksum of an entire record, to check
	 * against what it should be
	 * @param record the ENTIRE record to be passed in
	 * @return the resultant checksum
	 */
	private int Checksum(String record){
		Logging.Info("Ultra2", "In checksum");
		int check = 0;
		for(int i = 0; i< record.length() - checkSumOffset; i++){
			check+= record.charAt(i);
		}
		if(Debug.DEBUG)Logging.Info("Ultra2.Checksum answer",Integer.toHexString(check) );
		return check;
	}


	public void unRegister(){
		unregister();
	}
	
	/**
	 * Wake up string used to be checked against the bytes 
	 * received from an adapter connected to a Ultra2 AFTER
	 * the processing is all finished
	 */
	String voiceMod2 = "0,\"";
	/**
	 * Wake up string used to be checked against the bytes 
	 * received from an adapter connected to a UltraLink
	 *  AFTER the processing is all finished
	 */
	byte[] voiceModLink = {(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF};

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void listenForWakeUpString(){
		WakeUpOnThisString voiceModule;
		if(signature.equals(signatureA))
		 voiceModule = new WakeUpOnThisString(voiceMod2);
		else{
			 voiceModule = new WakeUpOnThisString(new String(voiceModLink));

		}
		voiceModule.run();
	}

}
