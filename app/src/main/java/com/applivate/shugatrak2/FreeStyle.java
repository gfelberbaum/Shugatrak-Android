package com.applivate.shugatrak2;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.os.SystemClock;

/**
 * *************************************************************************
 * FreeStyle
 * <p/>
 * <h3>Purpose of Activity:</h3> &nbsp; This class handles the commands and the
 * decoding of data from the FreeStyle meters.
 * <p/>
 * <h3>Update notes:</h3> v1.0.1: &nbsp; Removed Checksum <br>
 * v0.1.5: &nbsp; Final beta FreeStyle ready
 * <p/>
 * <h3>Known errors:</h3> V1.0.1: &nbsp; None known V0.1.5: &nbsp; None known
 *
 * @author Current: Ryan Hirschthal; Original: Ryan Hirschthal &nbsp;
 *         {@code (c) 2014 Ryan Hirschthal, Advanced Decisions. (rhirschthal @ advanceddecisions . com)
 *         All rights reserved}
 * @version V0.1.5: Ryan; V0.1.5: Ryan
 * @category ShugaTrak
 * @see {@link BaseMeter}, {@link MeterInterface}
 * ***************************************************************************
 */
public class FreeStyle extends BaseMeter implements MeterInterface {

    final static String signatureA = "Freestyle Freedom Lite";
    final static String signatureB = "Freestyle Lite";

    /**
     * Saves the information from the meter in as a String
     */
    String receivedData;
    /**
     * The minimum amount of information preferred before start calculating
     */
    private final int minLength = 75;
    /**
     * Number used to check record lines to see if the current record is only a
     * header
     */
    private static final int END_OF_HEADER = 6;
    /**
     * Command used by the meter to start dumping information
     */
    private final byte[] FULL_METER_DUMP = {'m', 'e', 'm'};
    private final String SINGLE_LINE = "$log,";
    private final byte[] TURN_OFF_CABLE_DETECTION = {'$', 'c', 'o', 'l', '1', ',', '0', '\r', '\n'};
//    private final byte[] CHECK_CONNECTION = {'$', 'c', 'o', 'l', '2', '\r', '\n'};

    /**
     * Holds the first instance of an unsuccessful (Broad definition) line that
     * was processed to possibly be reprocessed later
     */
    private int endOfProcess;
    /**
     * Time that the program will Sleep, in seconds
     */
    private final int sleepTimeFullDump = 2;
    private final double getSleepTimeSingleGrab = 1;

    /**
     * Constructor
     *
     * @param context send to parent
     */
    public FreeStyle(Context context, String signature) {
        super(context, signature);
        Logging.Info("Freestyle", "Constructor");
        receivedData = "";
        endOfProcess = 0;
        repeatData = false;
        finalReadings = new ArrayList<>();
    }

    @Override
    protected void onNewData(byte[] byteArrayExtra) {
        // OVERRIDDEN FROM ABSTRACT
//        Logging.Debug("Freestyle.onNewData", new String(byteArrayExtra));
        receivedData += new String(byteArrayExtra);
    }

    // //////////////////////////////////////
    // COMMUNICATEWITHDEVICE/////////////////
    // //////////////////////////////////////
    @Override
    public String[] communicateWithDevice() {
        createRegister();

        sendCommand(TURN_OFF_CABLE_DETECTION);
//		sendCommand(CHECK_CONNECTION);

        if (alreadyHaveInfo) {
            doSingleReadings();
        } else {
            doFullDump();
        }

        resetAdapterPhrase();

        if (checkForReadings()) {
            InternetSyncing.errorDump = "BAD CHECKSUM\n" + receivedData;
        }

        return finalReadings.toArray(new String[finalReadings.size()]);
    }

    /**
     * Will do a quick analysis of the data, then pass it to either
     * {@link #processHeader(String)} or {@link #processLine(String)} to be
     * processed
     *
     * @param data the entire string of characters, received by the meter, this
     *             method specifically ignores previously successful ones
     */
    public void process(String data) {
        String[] dataArray = data.split("\n");

        Logging.Info("Freestyle.Process", "data array size: " + dataArray.length);
        Logging.Verbose("FreeStyle.process()", Logging.arrayToDumpString(dataArray, "dataArray[]"));
        if (endOfProcess == 0) {
            processHeader(dataArray[0]);
            endOfProcess = END_OF_HEADER;
        }

        int i;
        for (i = endOfProcess; i < dataArray.length; i++) {
            Logging.Verbose("FreeStyle.process", "Processing Line #: " + i + " of " + dataArray.length);
            if (!processLine(dataArray[i])) {
                break;

            }
            if (i == END_OF_HEADER) {
                Logging.Debug("Freestyle.Process", "Try visual update");
                try {
                    update();
                    Logging.Debug("Freestyle.Process", "Success");
                } catch (Exception ex) {
                    Logging.Error("Freestyle.Process", "EXCEPTION: ", ex);
                }
            }

            Logging.Info("Freestyle.Process", "line = " + i);
        }
        endOfProcess = i;

    }


    public void doSingleReadings() {
        Logging.Info("Freestyle.CommunicateWithDevice", "DoSingleReadings - START");

        try {
            SystemClock.sleep((long) (.5 * SECONDS));
        } catch (Exception ex) {
            Logging.Error("Freestyle.doSingleReadings", "EXCEPTION: ", ex);
        }

        int readingToGrab = 1;

        while (connected && !repeatData && badGrabCount < 3) {
            byte[] readingCommand = (SINGLE_LINE + readingToGrab + "\r\n").getBytes();

            Logging.Info("Freestyle.CommunicateWithDevice", "DoSingleReadings.presend");
            receivedData = "";

            sendCommand(readingCommand);
            Logging.Info("Freestyle.CommunicateWithDevice", "DoSingleReadings.post send");
            do {
                try {
                    newInfo = false;
                    SystemClock.sleep((long) (getSleepTimeSingleGrab * SECONDS));
                    Logging.Info("Freestyle.CommunicateWithDevice", "DoSingleReadings.sleep");
                } catch (Exception ex) {
                    Logging.Error("Freestyle.communicateWithDevice", "EXCEPTION: ", ex);
                }
            }
            while (newInfo);
            Logging.Info("Freestyle.CommunicateWithDevice", "This is the reading" + receivedData);

            if (singleReadingProcessing(receivedData)) {
                //DO PROCESSING HERE

                if (readingToGrab++ == 1) {
                    update();
                }

            }
        }
        Logging.Info("Freestyle.CommunicateWithDevice", "DoSingleReadings - END");
    }


    public boolean singleReadingProcessing(String fullPayload) {
        String[] brokenUpPayload = fullPayload.split("\n");

        if (fullPayload.toLowerCase().contains("log not found")) {
            repeatData = true;
            return false;
        }
        if (fullPayload.contains("FAIL") || !isSingleLineChecksumValid(fullPayload)) {
            badGrabCount++;
            return false;
        }

        return processLine(brokenUpPayload[4]);
        //TODO create actual processing for this, this will fail the program if tried  //  PAUL:  2015-06-23:  Is this TODO comment still valid??
    }

    public void doFullDump() {

        Logging.Info("Freestyle.doFullDump", "start, sending message");
        sendCommand(FULL_METER_DUMP);
        int numberOfResends = 0;

        boolean goodPacket = false;
        while (badGrabCount < 3 && !goodPacket && numberOfResends < 3) {
            // Start While loop
            receivedData = "";
//            while (connected && (receivedData.equals("") || newInfo) && !repeatData && numberOfResends < 3) {
            while (connected && (receivedData.equals("") || newInfo) && !repeatData) {
                Logging.Info("Freestyle.communicate with device", "New loop");

                if (2 == numberOfResends) {
                    createNotification(R.string.meter_not_responding, R.raw.failure_sound, true);
                }

                newInfo = false;
                try {
                    SystemClock.sleep(sleepTimeFullDump * SECONDS);
                } catch (Exception e) {
                    Logging.Error("doFullDump():  sleep(" + (sleepTimeFullDump * SECONDS) + ") - EXCEPTION: ", e.getMessage(), e);
                }

                // If there is nothing back
                // try again
                if (receivedData.equals("")) {
                    sendCommand(FULL_METER_DUMP);
                    ++numberOfResends;
                    Logging.Info("Freestyle.communicate with device", "resent the FULL_METER_DUMP: " + numberOfResends);

                    // If junk, wait and flush
                } else if (!receivedData.startsWith("\r\n")) {
                    SystemClock.sleep(1 * SECONDS);
                    receivedData = "";
                    sendCommand(FULL_METER_DUMP);
                    // If there is a decent amount of data
                    // }else if(receivedData.length() > minLength){
                    // process(receivedData);
                }
            }// end while
            Logging.Info("Freestyle.communicate with device", "ended the while loop");

            if (receivedData.toLowerCase().contains("log empty")) {
                Logging.Info("FreeStyle.doFullDump()", "receivedData contains 'log empty'");
                return;
            }
            // TODO NOT DOING ANYTHING RIGHT NOW
            if (hasFullPacket(receivedData) && isChecksumValid(receivedData)) {
                goodPacket = true;
            } else {
                badGrabCount++;
            }

        }
        process(receivedData);
    }


    /**
     * This will run through the records, individually, and process each one
     * seperately
     *
     * @param record the record that will be checked next
     * @return false if line is not complete OR is the newest already
     */

    public boolean processLine(String record) {

        try {
            Logging.Debug("Freestyle.processLine()", "start:   record = " + record);

            record = record.replaceAll("[^ a-zA-Z0-9:]", " ").replace("   ", " ").replace("  ", " ");// Removes
            // all
            // Spaces
            String[] RecordLines = record.split(" ");

            Logging.Info("PAUL", "PDS:  RecordLines[0] = " + RecordLines[0]);

            Logging.Debug("Freestyle.Processing line", "start:" + record);

            if (RecordLines.length < 6) {
                Logging.Debug("Freestyle.processLine", "Returning FALSE:  Does not have the necessary information");
                return false;
            }

            // [1,2,3] is date
            // Date date;
            String endDate = (RecordLines[1] + " " + RecordLines[2] + ", " + RecordLines[3]).trim();

            // [4] is time
            // ///////////////////////////////////////////////////////////////////////////////////////////////
            String endTime = null;
            Date time = null;

            SimpleDateFormat original = new SimpleDateFormat("HH:mm", Locale.US);
            try {
                time = original.parse(RecordLines[4].trim());
                original.applyPattern("h:mm a");
                endTime = original.format(time);
            } catch (Exception ex) {

                Logging.Error("DUMP: ", Logging.arrayToDumpString(RecordLines, "RecordLines[]"));
                if (RecordLines.length > 4) {
                    Logging.Error("FreeStyle.processLine", "Probable parsing error: RecordLines[4] = " + String.valueOf(RecordLines[4]));
                }
                Logging.Error("FreeStyle.processLine", "Probable parsing error: EXCEPTION: ", ex);
            }

            Logging.Info("FreeStyle.ProcessLine time", "endTime: " + endTime + "; Time: " + time + "; RecordLines[4]"
                    + RecordLines[4]);
            // if(endTime.length()<2) endTime = "1:00 am";
            // /////////////////////////////////////////////////////////////////////////////////////////////////////
            // [0] is BGL

            String bloodGlucoseLevel;
            switch (RecordLines[0]) {
                case "LO":
                    bloodGlucoseLevel = "-2";
                    break;
                case "HI":
                    bloodGlucoseLevel = "-1";
                    break;

                default:
                    bloodGlucoseLevel = "" + Integer.parseInt(RecordLines[0].replaceAll("[^0-9.]", ""));
            }

            Logging.Debug("Freestyle.Processing Line", "checking the databases");

            // Check here if it is the old newest addition
            if (isMatchedReading(bloodGlucoseLevel, endDate, endTime)) {
                Logging.Debug("Freestyle.processLine", "Returning FALSE:  isMatchedReading is true");
                return false;
            }

            // Append the info
            finalReadings.add(bloodGlucoseLevel);
            finalReadings.add(endDate);
            finalReadings.add(endTime != null ? endTime.trim() : null);

            Logging.Debug("Freestyle.processLine End", "Successfully went through," + bloodGlucoseLevel + " " + endDate + " " + endTime);
        } catch (Exception ex)  {
            Logging.Error("FreeStyle.processLine()", "   EXCEPTION: ", ex);
        }

        return true;
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////TODO
    public boolean isChecksumValid(String info) {
        int calculatedChecksum = 0;

        for (long i = 0; i < info.length() - 13; i++) {
            calculatedChecksum += info.charAt((int) i);
        }

        String valueToParse = info.substring(info.length() - 11, info.length() - 7);
        Logging.Verbose("VALUE TO PARSE: [" + valueToParse + "]");
        int realChecksum = Integer.parseInt(valueToParse, 16);  // Paul Sellards:  2015-06-24:  Changed this from Short.parse...  the hex value was overflowing.

        Logging.Verbose("Freestyle.isChecksumValid",
                "Calculated: " + Integer.toHexString(calculatedChecksum) + "  Real: "
                        + Integer.toHexString(realChecksum));

        return calculatedChecksum == realChecksum;
    }

    public boolean isSingleLineChecksumValid(String info) {
        Logging.Info("FreeStyle.isSingleLineChecksumValid(info)   info=[" + info + "]");
        int calculatedChecksum = 0;
        int realChecksum = 0;
        boolean retval = false;

        try {
            String[] brokenUpInfo = info.split("\n");
            Logging.Debug("FreeStyle.isSingleLineChecksumValid(info)", Logging.arrayToDumpString(brokenUpInfo, "brokenUpInfo"));

            for (long i = 0; i < info.indexOf(brokenUpInfo[5]); i++) {
                calculatedChecksum += info.charAt((int) i);
            }

            try {
                realChecksum = Integer.parseInt(brokenUpInfo[5].substring(2, 6).trim(), 16);    // Paul Sellards:  2015-06-24:  Also changed this from Short.parse to prevent overflow.
            } catch (Exception ex) {
                Logging.Error("Freestyle.isSingleLineChecksumValid", "EXCEPTION: ", ex);
            }

            Logging.Verbose("Freestyle.isSingleLineChecksumValid",
                    "Calculated: " + Integer.toHexString(calculatedChecksum) + "  Real: "
                            + Integer.toHexString(realChecksum));

            retval = calculatedChecksum == realChecksum;
        } catch (Exception ex) {
            retval = false;
            Logging.Error("FreeStyle.isSingleLineChecksumValid(info)", "EXCEPTION: ", ex);
        }
        return retval;

    }


    public boolean hasFullPacket(String info) {
        String[] individualReadings = info.split("\n");
        String endingLine = individualReadings[individualReadings.length - 1];

        Logging.Verbose("Freestyle.hasFullPacket", "Ending Line:  " + endingLine);
        Logging.Verbose("Freestyle.hasFullPacket", "hasFullPacket:  " + endingLine.contains("END"));
        return endingLine.toLowerCase().contains("end");
    }

    /**
     * goes through the header, and if something should be done with it do it
     * here. UNUSED currently
     *
     * @param header The header record
     */
    public void processHeader(String header) {
        Logging.Debug("Freestyle.ProcessHeader", "" + header);
    }

    /**
     * Wake up string used to be checked against the bytes being received AFTER
     * the processing is all finished
     */
    byte[] voiceMod = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

    /**
     * {@inheritDoc}
     */
    @Override
    public void listenForWakeUpString() {
        WakeUpOnThisString voiceModule = new WakeUpOnThisString(new String(voiceMod));
        voiceModule.run();
    }

}
