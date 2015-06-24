
package com.applivate.shugatrak2;


import java.util.ArrayList;


import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.media.MediaPlayer;

/**
 * *************************************************************************
 * BASE SERVICE
 * <p/>
 * <h3>Purpose of Activity:</h3>
 * &nbsp; After {@link BleService} successfully finds
 * all of the services, this class is started to
 * make everything happen in the order it is supposed
 * to.
 * <p>	This class will:<br>
 * Grab internal signature of meter; Use signature
 * to find the right meter; Tell the meter class to
 * decode and return info that we want it to; Try
 * uploading to the Internet; Upon successful
 * connection, make notification, and delete the old
 * info; upon failure, save the info; and then update
 * the foreground.
 * <p/>
 * <h3>Update notes:</h3>
 * v0.1.5:
 * &nbsp; webView was fixed <br>
 * v0.1.4:
 * &nbsp; Final beta BaseService ready
 * <p/>
 * <h3>Known errors:</h3>
 * v0.1.5:
 * &nbsp; nothing known <br>
 * v0.1.4:
 * &nbsp; webview
 *
 * @author Current: Ryan Hirschthal Original: Ryan Hirschthal &nbsp;
 *         {@code (c) 2014 Ryan Hirschthal, Advanced Decisions. (rhirschthal @ advanceddecisions . com)
 *         All rights reserved}
 * @version V0.1.5: Ryan; V0.1.4: Ryan
 * @category ShugaTrak
 * @see {@link BaseMeter}
 * ***************************************************************************
 */
public class BaseService extends IntentService {
    /**
     * base constructor to start the activity
     *
     * @param name What the service will be called by the OS
     */
    public BaseService(String name) {
        super(name);
    }

    /**
     * Base constructor to start the activity
     */
    public BaseService() {
        super("BaseService");
    }

    /**
     * Signature to hold what type of meter the
     * meter connected is. It needs to be out here
     * So the {@link CreateJsonTask} will be able to reach
     * it.
     */
    private static String Signature;

    //ALL STRINGS BELOW ARE USED FOR SENDING BROADCASTS

    /**
     * Broadcast String to say that there is new saved info
     */
    public static final String UPDATES = "SHUGATRAK_FRONT_END_UPDATE";
    /**
     * Broadcast String to say that the front screen should post a toast message
     */
    public static final String MAKE_TOAST = "SHUGATRAK_MAKE_SUCCESSFUL_TOAST";
    /**
     * Extra to the toast Message, the title of where the wanted String is
     */
    public static final String TOAST_EXTRA = "EXTRAS IN THIS THING";
    /**
     * Broadcast String to say change the {@link BleService#UIConnected}
     * to be set to {@link BleService#GETTING_READINGS}
     */
    public static final String STARTING_READINGS = "SHUGATRAK IS STARTING READINGS";
    /**
     * Broadcast String to change the {@link BleService#UIConnected}
     * back to either connected or disconnected.
     */
    public static final String ENDING_READINGS = "SHUGATRAK IS ENDING THE READINGS";

    /////////////////////////////////////////////////////////////////////
    public static final String RETRY_UPLOAD = "SHUGATRAK IS TRYING TO UPLOAD INFO AGAIN";


    /**
     * Instance of the wrapper class, to save the information that
     * was returned from the meter classes
     */
    private DataSaver dataSaver;


    //All Strings below are used to set up a successful
    //notification and toast message
    private static final String NOTIFICATION_0 = "Sent ";
    public static int numberOfReadingsSentUp;
    private static final String NOTIFICATION_1 = " reading";
    private static final String NOTIFICATION_2 = " to ShugaTrak";

    private static final String neverReceivedAnything = "Unable to communicate with meter";


    public static boolean badChecksumFlag;


    /**
     * This is the 'main' method of this class. As soon as either
     *  the button to restart uploading is pressed or
     * when the Bluetooth connects, and gets all readings from the meter (if started not by
     * a button), as well as initiate uploading.
     *
     * @param intent an intent that is passed to the application whe it is set to start running
     *               Can hold an extra that specifies whether this is retrying upload or not
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        //called after discovered services

        //get rid of the process that might have started before this one
        if (runCheck.isAlive()) runCheck.stop();//TODO
        //If this is only a retry, it gets handled here.
        if (intent.getAction() == RETRY_UPLOAD) {//TODO leaving it as a == because the intent might be null, and it will be pointing to the same place
            CreateJsonTask task = new CreateJsonTask();
            task.execute();
            return;
        }

        Logging.Info("BaseService.start.onHandleRequest");

        badChecksumFlag = false;

        //Creates the saving wrapper class
        dataSaver = new DataSaver(getApplicationContext());


        runCheck.start();

    }

//	private void sendReadingsToServer() {

//	}

    /**
     * Creates a Background thread which does nothing more than
     * take the information that was given and converts it to a
     * {@linkplain ArrayList} of {@link Reading}. It will start
     * Internet syncing afterwards
     */
    private class CreateJsonTask extends AsyncTask<String, Void, ArrayList<Reading>> {


        protected ArrayList<Reading> doInBackground(String... returnInfo) {
            ArrayList<Reading> readings = new ArrayList<>();
            try {
                Logging.Info("BaseService:   CreateJSON async class - doInBackground - in");

                //Convert everything into an easy arraylist to send everything
                //up easily

                IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = registerReceiver(null, intentFilter);
                int level = -1;
                int scale = 1;
                float batteryLevel = -1.0F;
                if (null != batteryStatus) {
                    level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
                    batteryLevel = ((float) level) / ((float) scale);
                }
                int i;
                for (i = 0; i < returnInfo.length; i = i + 3) {
                    readings.add(new Reading(
                            Integer.parseInt(returnInfo[i]),    //BGL
                            returnInfo[i + 1],                  //DATE
                            returnInfo[i + 2],                  //TIME
                            Signature,                          //METER TYPE
                            dataSaver.readSet(DataSaver.DeviceAddresses),   //DEVICE ADDRESS
                            batteryLevel                        //PHONE BATTERY LEVEL
                    ));
                }
                Logging.Debug("Create JSON.doInBackground:  finish creation loop, have " + (i / 3) + "readings");
                numberOfReadingsSentUp = readings.size();
            }
            catch (Exception ex) {
                Logging.Error("BaseMeter.doInBackground()", "   EXCEPTION: ", ex);
            }
            finally {
                Logging.Info("BaseService:   CreateJsonTask.doInBackground() - out");
            }
            return readings;
        }

        public void onPostExecute(ArrayList<Reading> readings) {

            //Upload the information
            InternetSyncing sync = new InternetSyncing(getApplicationContext());
            sync.SyncData(readings);

            //wait for upload to complete
//			while(!InternetSyncing.synced){
//				SystemClock.sleep(10);//arbitrary sleep time
//			};

            //TODO testing, if the below code is necessary for the project to work correctly,
            //		should Be moved to InternetSyncing.SyncDataTask now.

            //AFTER UNBINDING, set a notification to send. Easy to get rid of, and opens the activity
//			createNotification(getApplicationContext());
//			try{
//				FragmentWebActivity.createWeb();
//			} catch (Exception ex) {
//				Logging.Warning("BaseService.onPostExecute()", "   EXCEPTION: ", ex);
//			}
        }
    }


    /**
     * runCheck is the way that this service pushes the time consuming parts of the application into
     * another
     */
    Thread runCheck = new Thread() {
        @Override
        public void run() {
            String[] returnInfo;
            MeterInterface meter;

            meter = compareMeters();

            Logging.Info("BaseService.onHandleRequest:  end switch, start communicate with device");

            //update the screen to say transferring readings
            BleService.UIConnected = BleService.REQUESTING_READINGS;
            Intent startReadings = new Intent(STARTING_READINGS);
            sendBroadcast(startReadings);


            //Try to grab the information, put it into an array to save
            //[0] BLOOD GLUCOSE LEVEL
            //[1] DATE
            //[2] TIME
            returnInfo = meter.communicateWithDevice();

            // re-update to stop saying transfering readings
            Intent endReadings = new Intent(ENDING_READINGS);
            sendBroadcast(endReadings);

            Logging.Info("BaseService.onHandleRequest:  Ended communication");

            //NOTE: SAVING THE MOST RECENT READING AND DISPLAYING ON
            //SCREEN NOW HAPPEN ON BASEMETER.UPDATE METHOD

            if (badChecksumFlag) {
                Logging.Error("BaseService.onHandleIntent  Had bad checksum error with " + meter + ", running alternate route");
                badChecksumErrorHandling(returnInfo, meter, "Bad Checksum");
                return;
            }


            //make sure there is something to try sending up
            //before we do so
            if (returnInfo.length > 2) {

                //SEND INFO UPWARDS USING RETURN INFO
                CreateJsonTask task = new CreateJsonTask();
                task.execute(returnInfo);
            } else {
                Logging.Info("BaseService.onHandleRequest:  No new Information");
                //If it got here, and is not connected currently, most likely BLE timeout, Needs to be fixed by user
                if (!BleService.connected) {
                    createGotNothingNotification();
                } else {
                    Logging.Info("BaseService.onHandleRequest:  No new Information");

                        Notification.Builder notificationBuilder = new Notification.Builder(getApplicationContext())
                                .setSmallIcon(R.drawable.notification_icon_1)//Action Bar icon for the notification
                                .setContentTitle("ShugaTrak")//Title for notification
                                .setContentText("No readings obtained from meter")// body of text for notification
                                        //		.setStyle( new Notification.BigTextStyle().bigText(finalString))//makes the drop down if there is more
                                .setAutoCancel(true)//gets rid of when clicked
//                                .setContentIntent(notifPendingIntent)// makes the place when you clicked, as specified above
                                .setTicker("No readings obtained from meter")//what shows up quickly at the top of the not. bar
                                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))// makes the main icon, on the left
                                ;
                    ((NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE)).notify(1, notificationBuilder.build());
                    Intent toastIntent = new Intent(MAKE_TOAST);
                    toastIntent.putExtra(TOAST_EXTRA, "No readings obtained from meter");
                    sendBroadcast(toastIntent);
                }

                meter.listenForWakeUpString();


            }
        }

    };


    ////////////////////////////////////////////
    public void createGotNothingNotification() {

        //Start intent for a go-to
        Intent notifyIntent = new Intent(getApplicationContext(), TopLevelActivity.class);
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        //end Intent

        //start building
        Notification.Builder notificationBuilder = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.notification_icon_1)//Action Bar icon for the notification
                .setContentTitle("ShugaTrak")//Title for notification
                .setContentText(neverReceivedAnything)// body of text for notification
                        //		.setStyle( new Notification.BigTextStyle().bigText(finalString))//makes the drop down if there is more
                .setAutoCancel(true)//gets rid of when clicked
                .setContentIntent(notifyPendingIntent)// makes the place when you clicked, as specified above
                .setTicker(neverReceivedAnything)//what shows up quickly at the top of the not. bar
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))// makes the main icon, on the left
                ;
        //end pre-build


        //show the notification
        ((NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE)).notify(1, notificationBuilder.build());


    }

    public static void playFailureSound(Context context) {
        Logging.Info("Entering BaseService.playFailureSound()");
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.failure_sound);
            mediaPlayer.start();
        }
        catch (Exception ex) {
            Logging.Error("BaseService.playFailureSound():  ");
        }
    }


    /**
     * Creates a notification, to notify the user of the
     * newest information only. if there is no new information,
     * It will reshow the old information. If no information
     * what-so-ever, than it will display spaces.
     *
     * @param context Context for the class, to be able to
     */
    public static void createNotification(Context context) {
        Logging.Info("BaseService.createNotification:  start");
        playFailureSound(context);

        Intent notifIntent = new Intent(context, TopLevelActivity.class);
        PendingIntent notifPendingIntent = PendingIntent.getActivity(context, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //Set up default bad answer, instead of using else
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        String finalString = InternetSyncing.portalErrorString;


        Notification.BigTextStyle bigScreen = new Notification.BigTextStyle();
        //Set up an S to make the message plural
        String multi = "";
        if (numberOfReadingsSentUp != 1) {
            multi = "s";
        }
        //start building
        Notification.Builder notificationBuilder = new Notification.Builder(context);

        //If it was an unsuccessful upload, add a button to try again
        if (InternetSyncing.portalErrorString != InternetSyncing.SD_GOOD_STRING) {

            playFailureSound(context);

            Intent retryIntent = new Intent(context.getApplicationContext(), BaseService.class);
            retryIntent.setAction(RETRY_UPLOAD);
            PendingIntent pendingRetryIntent = PendingIntent.getService(context.getApplicationContext(), 0, retryIntent, 0);

            notificationBuilder
//					.setStyle(new Notification.BigTextStyle().bigText(finalString+WAIT_STRING))
                    .addAction(0, context.getString(R.string.retry), pendingRetryIntent);
        }

        //If the upload was successful, change the message from error to good
        if (InternetSyncing.portalErrorString == InternetSyncing.SD_GOOD_STRING) {
            soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.data_ready2);
            finalString = NOTIFICATION_0 + numberOfReadingsSentUp + NOTIFICATION_1 + multi + NOTIFICATION_2;
        }
        bigScreen.bigText(finalString);

        notificationBuilder
                .setSmallIcon(R.drawable.notification_icon_1)//Action Bar icon for the notification
                .setContentTitle("ShugaTrak")//Title for notification
                .setContentText(finalString)// body of text for notification
                .setStyle(new Notification.BigTextStyle().bigText(finalString))//makes the drop down if there is more
                .setAutoCancel(true)//gets rid of when clicked
                .setContentIntent(notifPendingIntent)// makes the place when you clicked, as specified above
                .setSound(soundUri)//specify the sound to play here
                .setTicker(finalString)//what shows up quickly at the top of the not. bar
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher))// makes the main icon, on the left
        ;
        //end pre-build


        //show the notification
        ((NotificationManager) context.getSystemService(NOTIFICATION_SERVICE)).notify(001, notificationBuilder.build());

//			Logging.Debug("BaseService.CreateNotification:  Saved array: " + dataSaver.getArray());
            Logging.Debug("BaseService.CreateNotification:  finished making notification");

        //Make a toast message saying the same thing
        Intent toastIntent = new Intent(MAKE_TOAST);
        toastIntent.putExtra(TOAST_EXTRA, finalString);
        context.sendBroadcast(toastIntent);

    }


    /**
     * Set up the meter instance with the correct meter class
     */
    public MeterInterface compareMeters() {
        //connect to the right meter
        Signature = dataSaver.readSet(DataSaver.meterType);
        MeterInterface meter;

        // every constructor should be able to accept context
        Logging.Debug("BaseService.compareMeters:  start switch");
        switch (Signature) {
            case (Ultra2.signatureA):
            case (Ultra2.signatureB):
                meter = new Ultra2(getApplicationContext(), Signature);
                break;

            case (UltraMini.SIGNATURE):
                meter = new UltraMini(getApplicationContext(), Signature);
                break;

            case (FreeStyle.signatureA):
            case (FreeStyle.signatureB):
                meter = new FreeStyle(getApplicationContext(), Signature);
                break;

            default:
                Logging.Info("baseService.compareMeters:  error condition");
                meter = new Ultra2(getApplicationContext(), Signature);
                break;
        }
        return meter;
    }


    //TODO write the javadoc

    public void badChecksumErrorHandling(String[] returnInfo, MeterInterface meter, String errorReason) {


        ArrayList<Reading> readings = new ArrayList<Reading>();
        //Convert everything into an easy arraylist to send everything up easily
        int i;
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        int level = -1;
        int scale = 1;
        Intent batteryStatus = registerReceiver(null, ifilter);
        if (null != batteryStatus) {
            level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
        }
        float batteryLevel = ((float) level) / ((float) scale);

        for (i = 0; i < returnInfo.length; i = i + 3) {
            readings.add(new Reading(
                    Integer.parseInt(returnInfo[i]),//BGL
                    returnInfo[i + 1],//DATE
                    returnInfo[i + 2],//TIME
                    Signature,//METER TYPE
                    dataSaver.readSet(DataSaver.DeviceAddresses),//DEVICE ADDRESS
                    batteryLevel//PHONE BATTERY LEVEL

            ));
        }


        InternetPayload payload = new InternetPayload(
                dataSaver.readSet(DataSaver.userName),
                dataSaver.readSet(DataSaver.Password),
                readings,
                errorReason);

        InternetSyncing sync = new InternetSyncing(getApplicationContext());
        sync.ErrorSync(payload);//does both send to the web, and create the notification

        //	make the payloadVVVVVV
        //	upload payload (make seperate method to do so)VVVVVVVVVVVVVVvv
        //	notify userVVVVVVVVVVVVVVVVVVV
        //	start the listeningModule?????

    }
}
