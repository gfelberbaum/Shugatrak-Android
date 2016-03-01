package com.applivate.shugatrakii;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;

/****************************************************************************
 * ULTRA MINI
 * 
 * <h3>Purpose of Activity:</h3>
 * 		&nbsp; This class handles
 * 		the commands and the decoding
 * 		of data from the UltraMini meters. 
 * 
 * <h3>Update notes:</h3>
 * 		V1.1.0:
 * 		&nbsp; got rid of ghost readings <br>
 * 		V1.0.5:
 * 		&nbsp; Possible fix in error, was in byte change <br>
 * 		V0.1.5:
 * 		&nbsp; Put in timer, now seems to be
 * 		a reading error w/ Transfer show up (fixed) <br>
 * 		v0.1.5:
 * 		&nbsp; Final beta UltraMini ready
 * 
 * <h3>Known errors:</h3>
 * 		V1.1.0:
 * 		&nbsp; non known <br>
 * 		V1.0.5:
 * 		&nbsp; ghost readings from bad timer <br>
 * 		V0.1.5:
 * 		&nbsp; have a base timer set up to run instead<br>
 * 		V0.1.5:
 * 		&nbsp; Needs to switch the sleep loop to a 
 * 		{@link Handler#postDelayed(Runnable, long)}
 * 		call;
 * 		&nbsp; if wire disconnects in middle of transfer
 * 		might not catch on readings until disconnected
 * 		(even if user reconnects wire)
 * 
 * @category ShugaTrak
 * @version V0.1.1.5 Ryan &nbsp; V0.1.5: Ryan
 * @author  Current: Ryan Hirschthal Original:Ryan Hirschthal &nbsp;
 * {@code (c) 2014 Ryan Hirschthal, Advanced Decisions. (rhirschthal @ advanceddecisions . com)
 * All rights reserved}
 * @see {@link BaseMeter}, {@link MeterInterface}
 *****************************************************************************/
public class UltraMini extends BaseMeter implements MeterInterface{

	public static final String SIGNATURE = "OneTouch UltraMini";
	
	
	/**
	 * Saves all of the information that is coming in
	 * as a {@link ByteArrayOutputStream}, to put into
	 * a {@code byte[]}
	 */
	private ByteArrayOutputStream receivedInfo;
	/**
	 *A flag that will only be true if the
	 *reading that was grabbed indicates
	 *that it went over the number of readings
	 *recorded
	 */
	private boolean pastLast; 
	/**
	 * Flag that will be true after a 
	 * processing of a single flag
	 */
	private boolean finishedFlag;
	/**
	 * Flag to indicate if
	 * the reading is valid
	 */
	private boolean goodReading;
	/**
	 * Flag to indicate whether or not
	 * there is a junk character for 
	 * the first character
	 */
	private boolean junkCharFlag;

	//BELOW IN SPEC
	private final int STX = 0x02;
	private final int STX_POS = 0;
	private final int ACK_LEN_POS = 1;

	/**
	 * After acknowledge message is through,
	 * there is another message, this int
	 * holds the position of that, then
	 * adds this and the {@link #ACK_LEN}
	 * together and check against final length
	 */
	private final int ANSWER_LEN_POS = 7;
	
	
	/**
	 * Length of an acknowledge
	 */
	private final int ACK_LEN = 0x06;
	/**
	 * Length of a 'full' record
	 */
	private final int ANS_LEN = 0x10;
	/**
	 * Length of a {@link #pastLast} reading
	 */
	private final int OVER_LEN = 0x0A;
	/**
	 * Length of Acknowledge and 'full' record
	 * together
	 */
	private final int FULL_ANS_LEN = ACK_LEN + ANS_LEN;
	/**
	 * Length of a Acknowledge and {@link #pastLast} reading
	 */
	private final int FULL_OVER_LEN = ACK_LEN + OVER_LEN;
	
	/**
	 * A byte used to clear off any negative signs in bytes
	 */
	private final int CLEAR_BYTE =  0xFF; 
	
	//Assuming a good record, each position
	//below will hold each and every pos 
	//necessary for processing
	private final int DATE_LL_POS = 5;
	private final int DATE_ML_POS = 6;
	private final int DATE_MH_POS = 7;
	private final int DATE_HH_POS = 8;
	private final int BGL_LL_POS = 9;
	private final int BGL_ML_POS = 10;
	private final int BGL_MH_POS = 11;
	private final int BGL_HH_POS = 12;
	private final int CHECKSUM_LOW_POS = 14;
	private final int CHECKSUM_HI_POS  = 15;
	
	
	/**
	 * Set the date zone to the correct format for UltraMini meters
	 */
	protected SimpleDateFormat dateFomat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
	/**
	 * Set the time zone to the corrected format for UltraMini meters
	 */
	protected SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);

	
	

	/**
	 * How long the program waits before it 
	 * checks to see if the computer has edited 
	 * anything in seconds
	 */
	private final double SLEEP_TIMEOUT = .005;
	/**
	 * Flag for timeout: pushing the the program
	 * out of the waiting loop. True if timeout is 
	 * passed
	 */
	private boolean TMOFlag;
	/**
	 * The timeout of pushing the the program
	 * out of the waiting loop in seconds. will
	 * only be activated on the condition no
	 * information has arrived. time specified 
	 * by spec
	 */
	private final double TMO_TIME_OUT = 1.250;
	private class tmoTimer extends TimerTask{

		@Override
		public void run() {
			Logging.Info("UltraMini.run", "hit timer");

			TMOFlag = true;
		}
		
	}
	
	
	/**
	 *This String holds each and every byte necessary to
	 *request a record from the Meter, except for the 
	 *record 'offset' and the checksum<p>
	 *
	 *WARNING:  
	 *			BASE_REQ_RECORD[5] needs to be
	 *			OFFSET BY TWO BYTES (the record
	 *			offset bytes)
	 */
																		//VVVV Need offset
	private final byte[] BASE_REQ_RECORD = {0x02, 0x0A, 0x03, 0x05, 0x1F, 0x03};
	private final byte[] ACKNOWLEDGE = {0x02, 0x06, 0x04, 0x03, (byte) 0xAF, 0x27};
	private final byte[] DISCONNECT = {0x02, 0x06, 0x08, 0x03, (byte) 0xC2, 0x62};
	

	/**
	 * Constructor
	 * @param c context sent to parent
	 */
	public UltraMini(Context c,String Sig) {
		super(c, Sig);
		createRegister();
		finalReadings = new ArrayList<String>();
		pastLast = false;
		dateFomat.setTimeZone(TimeZone.getTimeZone("GMT"));
		timeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onNewData(byte[] bs) {
		//OVERRIDDEN FROM ABSTRACT		

		for(byte byteVal:bs)
			receivedInfo.write(byteVal);

		byte[] info = receivedInfo.toByteArray();

		
		
		if (info[STX_POS] ==   STX){
			//if not a junk character
				if(overRecord(info)){
					//If it is the past oldest reading
					
					pastLast = true;
					goodReading = false;
					finishedFlag =true;
					Logging.Info("UltraMini.onNewData", "OverRecord");

				}else if (fullRecord(info)){
					//if size is correct for oldest reading
					goodReading = processLine(info);
					finishedFlag = true;
					Logging.Info("UltraMini.onNewData", "FullRecord");
				}
		}else{
			//if first character is a junk character
			goodReading = false;
			junkCharFlag = true;
			Logging.Info("UltraMini.onNewData", "Hit a junk character");
			
		}
					
					
					
					
	
	}
	
	
	/**
	 * Boolean check to see if the 'over' record
	 * has been retrieved
	 * @param info The ENTIRE record
	 * @return True if this is the past
	 * 			the final record
	 */
	private boolean overRecord(byte[] info){
		return (info.length == FULL_OVER_LEN
				&&   info[ACK_LEN_POS] == ACK_LEN  
				&&  info[ANSWER_LEN_POS] == OVER_LEN);
	}
	
	
	/**
	 * Boolean check to see if the entire record
	 * has been retrieved
	 * @param info The ENTIRE record
	 * @return True if it is a full record
	 */
	private boolean fullRecord(byte[] info){
		return (info.length == FULL_ANS_LEN 
				&& info[ACK_LEN_POS] == ACK_LEN  
				&& info[ANSWER_LEN_POS] == ANS_LEN);
	}

	
	
	///////////////////////////////////////////
	//////COMMUNICATE//////////////////////////
	///////////////////////////////////////////
	@Override
	public String[] communicateWithDevice() {
		byte[] getRecord = null;
		int offset = 0;
		goodReading = true;
		int numberOfRetries = 0;
		
		while(connected && !repeatData && !pastLast && badGrabCount<3){//START WHILE////////////
			if(goodReading){
				getRecord = changeRecord(offset++);
				if(offset == 2){//after the first reading is complete
					update();
				}
				badGrabCount = 0;
			}else if(!repeatData&&badGrabCount >2){
				if(Debug.DEBUG)Logging.Info("UltraMini.communicate with device", "KickedByBadChecksum");
				InternetSyncing.errorDump = "BAD CHECKSUM::";
					break;
			}
			
			//These flags state when the process finishes and if it finished correctly
			finishedFlag = false;
			goodReading = false;
			junkCharFlag = false;
			
			receivedInfo =new ByteArrayOutputStream();//flush the buffer
			
			
			
			sendCommand(getRecord);
			
			
			
			//Set up and running of the sleep cycle
			tmoTimer task = new tmoTimer();
			TMOFlag = false;
			Timer time = new Timer();
			time.schedule(task, (long) (TMO_TIME_OUT * SECONDS));
			
			try{//Start try
				do{//Start dohwhile
					SystemClock.sleep( (long)(SLEEP_TIMEOUT * SECONDS) );
//					if(Debug.DEBUG)Logging.Info("communicate with device", "Sleep");
				}while( connected && (!finishedFlag && !TMOFlag) );//end dowhile
				
			}catch(Exception ex){
				Logging.Error("UltraMini.CommunicateWithDevice","ERROR", ex);
			}//end try Catch
			
			time.cancel();

			if(receivedInfo.size() == 0){
				numberOfRetries++;
				if(numberOfRetries ==3){
					createNotification(R.string.meter_not_responding, R.raw.failure_sound, true);
					meterNotResponding=true;
					break;
				}
			}

			if(receivedInfo.size() != 0  && !junkCharFlag){
				sendCommand(ACKNOWLEDGE);

			}
			Logging.Info("UltraMini.communicate with device", "end of postprocess");
			
		}//END WHILE////////////////

		sendCommand(DISCONNECT);
		checkForReadings();

		resetAdapterPhrase();
		return finalReadings.toArray(new String[0]);
	}




	/**
	 * Goes through the Answer response from the meter
	 * and processes it
	 * 
	 * @param info the record that will be checked next
	 * @return false if line is not complete OR is the newest already
	 */
	public boolean processLine(byte[] info) {

		//convert the Acknowledge and reading
		//into ONLY a reading
		byte[] record = new byte[ANS_LEN];		
		for(int i = ACK_LEN; i<info.length; i++){
			record[i-ACK_LEN] = (byte) (info[i]);
		}
		
		Logging.Info("UltraMini.Process", "process line: " + record[ACK_LEN_POS]);

		//finish conversion
		
		
		//CHECKSUM SETUP
		byte[] checkRecord = new byte[ANS_LEN-2];
		for(int i =0; i<ANS_LEN-2 ; i++)
			checkRecord[i] = record[i];
		if (  ( (record[CHECKSUM_LOW_POS]&CLEAR_BYTE) +((record[CHECKSUM_HI_POS]&CLEAR_BYTE)<<8) )  != addChecksum(checkRecord) ){

			if(Debug.DEBUG)Logging.Info("UltraMini.processLine", "did not pass checksum:"+ Integer.toHexString((record[CHECKSUM_LOW_POS]
					+(((record[CHECKSUM_HI_POS])&CLEAR_BYTE)<<8)))+"  " + Integer.toHexString(addChecksum(checkRecord)));
			badGrabCount++;
			return false;
		}
		//END CHECKSUM


	
		//START Date Time
		String date;
		String time;

		try{
	
			
			long currentMillis = (
					   (record[DATE_LL_POS] & CLEAR_BYTE) +
					 ( (record[DATE_ML_POS] & CLEAR_BYTE) <<8 ) +
					 ( (record[DATE_MH_POS] & CLEAR_BYTE) <<16) +
					 ( (record[DATE_HH_POS] & CLEAR_BYTE) <<24)
					)*SECONDS;//might mess up... no L at end
		
			
			
			Date millisDate = new Date(currentMillis);
			date =dateFomat.format(millisDate);
			time = timeFormat.format(millisDate);

			if(Debug.DEBUG)Logging.Info("UltraMini.Processline ", date +" " +time);



		}catch(Exception ex){
			Logging.Error("Ultra mini.ProcessLine", "Error with dateTime parsing",ex);
			date = "";
			time = "";
		}
		if(date.length()<2) date = "Jan 01, 2001";

		if(time.length()<2) time = "1:00 am";

		//END DATE TIME


		
		//MAKE BGL
		long bgl = 
				  (record[BGL_LL_POS]&CLEAR_BYTE) +
				((record[BGL_ML_POS]&CLEAR_BYTE)<<8) +
				((record[BGL_MH_POS]&CLEAR_BYTE)<<16) +
				((record[BGL_HH_POS]&CLEAR_BYTE)<<24);
		
		
		
		//according to john, this is LOW reading
		if(bgl ==  65534){
		bgl = -2;	
		}
		
		if(bgl ==  65535){
			bgl = -1;	
		}
			
			

		if(Debug.DEBUG)Logging.Info("UltraMini.Process Line", ""+bgl);
		
		//Check here if it is the old newest addition
		if(isMatchedReading(bgl+"", date, time))return false;		
		
		//append the info
		finalReadings.add(bgl+"");
		finalReadings.add(date);
		finalReadings.add(time);
		
		if(Debug.DEBUG)Logging.Info("UltraMini.Process Line full", date + " " + time +" :: " + bgl);



		return true;
	}

	/**
	 * This method will generate a new record for the program
	 * @param offset The new record to grab
	 * @return The completed record
	 */
	private byte[] changeRecord(int offset) {
		
		//Switch offset to two bytes
		byte offsetL = (byte) (offset % 0x100);
		byte offsetH = (byte) (offset / 0x100);
		
		if(Debug.DEBUG)Logging.Info("UltraMini.changeFile","Offset :"+ offset+" low: "+(int) (offsetL&0xFF) +" hi: " +offsetH);
		
		//Start make checksum
		byte[] preCSRecord ={BASE_REQ_RECORD[0], BASE_REQ_RECORD[1], BASE_REQ_RECORD[2], BASE_REQ_RECORD[3], BASE_REQ_RECORD[4], offsetL, offsetH, BASE_REQ_RECORD[5]};
		
		int cs = addChecksum(preCSRecord);
		//End make Checksum
		
		//Switch Checksum to two bytes
		byte csl =(byte) (cs%0x100);
		byte csh =(byte) (cs/0x100);
		
		//Make final {@code byte[]}
		byte[] postCSRecord = {BASE_REQ_RECORD[0], BASE_REQ_RECORD[1], BASE_REQ_RECORD[2], BASE_REQ_RECORD[3], BASE_REQ_RECORD[4], offsetL, offsetH, BASE_REQ_RECORD[5], csl, csh};
		return postCSRecord;
	}

	/**
	 * Creates the checksum to either check 
	 * against or add to making a new record
	 * 
	 * <p>ORIGIN OF CODE FOUND HERE
	 * 
	 * <br>http://introcs.cs.princeton.edu/java/51data/CRC16CCITT.java.html
	 * 
	 * <br>Copyright (C) 2000, 2011, Robert Sedgewick and Kevin Wayne.
	 * Last updated: Wed Feb 9 09:20:16 EST 2011.
	 *  
	 * @param record The record that wants to 
	 * 			be made into a checksum, WITH
	 * 			NO OTHER BYTES
	 * @return The checksum
	 */
	private int addChecksum(byte[] record){
		int initial_CRC = (short) 0xFFFF;

		int CRC = initial_CRC;




		int polynomial = 0x1021;  
		for (byte b : record) {
			for (int i = 0; i < 8; i++) {
				boolean bit = ((b   >> (7-i) & 1) == 1);
				boolean c15 = ((CRC >> 15    & 1) == 1);
				CRC <<= 1;
				if (c15 ^ bit) CRC ^= polynomial;
			}
		}

		CRC &= 0xffff;

		return CRC;



	}



	public void unRegister(){
		unregister();
	}
	
	
	
	
	/**
	 * Wake up string used to be checked against the bytes 
	 * received from an adapter connected to a UltraMini AFTER
	 * the processing is all finished
	 */
	String voiceMod = "0,\"";
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void listenForWakeUpString(){
		WakeUpOnThisString voiceModule = new WakeUpOnThisString(voiceMod);
		voiceModule.run();
//		VoiceModuleTrigger voiceModule = new VoiceModuleTrigger();
//		
//		voiceModule.execute(voiceMod);
	}
	
	
	
	
	
	
	
	
	
	
}
