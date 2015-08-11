package com.applivate.shugatrak2;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.widget.Toast;

/**
 * *************************************************************************
 * BASE METER
 * <p/>
 * <h3>Purpose of Activity:</h3>
 * &nbsp; This class is the Base
 * of each and every meter that is
 * and will be set up. It is here so
 * it can be middle-man between the
 * {@link BleService} class as well
 * as the {@link DataSaver} class.
 * This class should hide as much as
 * possible from the individual meters
 * that is universal among meters.
 * <p/>
 * <h3>Update notes:</h3>
 * V0.1.6:
 * &nbsp; fixed date/time to be personal
 * meter set up<br>
 * V0.1.5:
 * &nbsp; Fixed the RepeatData error;
 * ready to go <br>
 * v0.1.4:
 * &nbsp; Final beta BaseMeter ready
 * <p/>
 * <h3>Known errors:</h3>
 * V.0.1.6:
 * &nbsp; None known <br>
 * V0.1.5:
 * &nbsp; The date/time was off <br>
 * v0.1.4:
 * &nbsp; repeatData was not active
 *
 * @author Current: Ryan Hirschthal; Original: Ryan Hirschthal &nbsp;
 *         {@code (c) 2014 Ryan Hirschthal, Advanced Decisions. (rhirschthal @ advanceddecisions . com)
 *         All rights reserved}
 * @version V0.1.6 Ryan; V0.1.5:Ryan; V0.1.4: Ryan
 * @category ShugaTrak
 * @see {@link UltraMini}, {@link Ultra2}, {@link FreeStyle}
 * ***************************************************************************
 */
public abstract class BaseMeter {
    /**
     * Context that comes from the
     * class that instantiates it
     * used primarily to get around
     * the fact that this class is
     * not an actual service class
     */
    private Context context;


    /**
     * If true, then the adapter and
     * the app are still connected.
     * This flag is a fail safe
     * in case the meter class is still
     * running while the adapter turns
     * itself off
     */
    protected boolean connected = false;

//    /**
//     * Creates a connection
//     * to the
//     * {@link BleService} class,
//     * which is used to send a
//     * command to the meter
//     */
//    private BleService BleS;
//
//    /**
//     * Sets up and keeps the connection
//     * between this class and the
//     * {@link BleService} class.
//     * will not be able to use
//     */
//    private ServiceConnection mSConnection;

    /**
     * Internal flag to make sure that the
     * app connects to the {@link BleService}
     * class before it starts doing something else
     * to prevent errors
     */
    private boolean bound = false;

    /**
     * If true, then the callback
     * from {@link BleService} class
     * got some information back.
     * set in {@link #Receiver}
     * should be set false in while
     * loop just after checking the
     * flag
     */
    protected boolean newInfo;

    /**
     * Instance of the persistent dataSaver class. To be used with
     * {@link #isMatchedReading(String, String, String)}
     */
    private DataSaver dataSaver;

    /**
     * If true, then the current reading
     * is the one that is saved as the
     * most recent reading in the app
     * <p> Set at the call
     * {@link #isMatchedReading(String, String, String)}
     */
    protected boolean repeatData;

    /**
     * Set up a basic time multiplier,
     * so that the time specified can
     * be in seconds, not milliseconds.
     */
    public static final long SECONDS = 1000L;

    /**
     * Holds the final readings of the meters
     * should be turned into a String[] at the
     * end with {@link ArrayList#toArray(Object[])}
     */
    protected ArrayList<String> finalReadings;


    ////////////////////////////////////////////
    protected int badGrabCount;

    protected boolean alreadyHaveInfo;

    public static boolean meterNotResponding;

    protected String bgl;
    protected String date;
    protected String time;


    /**
     * String to hold what meter it is
     * specifically, in the case that we
     * want to do something with it
     */
    protected String signature;


    /**
     * Constructor
     *
     * @param c Application context specifically used for setting up {@link BleService}
     *          and the save file
     */
    public BaseMeter(Context c, String signature) {
        bound = false;
        context = c;
        dataSaver = new DataSaver(c);
        repeatData = false;
        connected = true;
        buffer = new ByteArrayOutputStream();
        this.signature = signature;
        badGrabCount = 0;
        meterNotResponding=false;

        bgl = (dataSaver.readSet(DataSaver.lastNumber));
        date = (dataSaver.readSet(DataSaver.lastDate));
        time = (dataSaver.readSet(DataSaver.lastTime));

        alreadyHaveInfo = !(bgl.equals(DataSaver.NO_ITEM));


        //Binds BleService
        Logging.Info("BaseMeter", "start bind");
        Intent gsIntent = new Intent(c, BleService.class);

//        mSConnection = new ServiceConnection() {
//
//            @Override
//            public void onServiceDisconnected(ComponentName name) {
//                BleS = null;
//
//            }
//
//            @Override
//            public void onServiceConnected(ComponentName name, IBinder service) {
//                BleS = ((BleService.LocalBinder) service).getService();
//                bound = true;
//                Logging.Info("BaseMeter.onServiceConnected", "just made the connect mSConnection");
//
//                if (!BleS.initialize()) {
//                    Logging.Info("BaseMeter.onServiceConnected", "unable to initialize");
//                }
//
//            }
//        };

//        if (context.bindService(gsIntent, mSConnection, Context.BIND_AUTO_CREATE)) {
//            Logging.Info("basemeter constructor", "successful bind");
//        }

        //END BIND

        //wait for binding to settle
//        while (!bound) ;

    }

    /**
     * Create  what to do if it receives information from the
     * callback
     */
    private BroadcastReceiver Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BleService.A_CONNECTED)) {
                connected = true;
            } else if (action.equals(BleService.A_DISCONNECTED)) {
                connected = false;
            } else if (action.equals(BleService.A_SERVICES_DISCOVERED)) {
                ;  //  do nothing?
            } else if (action.equals(BleService.A_DATA_AVAILABLE)) {
                newInfo = true;

                onNewData(intent.getByteArrayExtra(BleService.A_EXTRA_DATA));
//				Logging.Verbose("BaseMeter.onReceive", intent.getByteArrayExtra(BleService.A_EXTRA_DATA).toString());

                for (byte byteVal : intent.getByteArrayExtra(BleService.A_EXTRA_DATA)) {
                    buffer.write(byteVal);
//					Logging.Verbose("BaseMeter.onReceive", Integer.toHexString((byteVal)));

                }
            }

        }

    };

    /**
     * Creates the register for the ability to listen in for
     * new information from the {@link BleService}. Needs to be called
     * If information is expected
     */
    protected void createRegister() {
        context.registerReceiver(Receiver, filter());

    }



    public static final String justSayConnected="VISUALS SHOULD JUST SAY CONNECTED";

    /**
     * Destroys the register to the {@link BleService}.
     * Needs to be called if creation has been.
     * resetting {@link BleService#UIConnected}
     * to be ready to switch to a normal
     * connected/disconnected mode
     */
    protected void resetAdapterPhrase()
    {
        Intent intent = new Intent (justSayConnected);
     context.sendBroadcast(intent);


//        BleS.justSayConnected();
    }

    /**
     * Destroys the register to the {@link BleService}.
     * called in the handler.
     */
    protected void deleteRegister() {
        context.unregisterReceiver(Receiver);
    }

    /**
     * Creates a broadcast filter. It will be able to tell
     * if there is any new information
     *
     * @return the filter
     */
    private IntentFilter filter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BleService.A_CONNECTED);
        intentFilter.addAction(BleService.A_DISCONNECTED);
        intentFilter.addAction(BleService.A_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.A_DATA_AVAILABLE);

        return intentFilter;
    }

    /**
     * Ends the binding. Should not be called,
     * but ready regardless
     */
    protected void endCon() {
//        BleS.unbindService(mSConnection);
        Logging.Info("BaseMeter.endCon", "Ended the connection between meter classes and BleService");
    }

    /**
     * Sends a command to {@link BleService#writeCharacteristic}
     * This method is preferred to leave Bluetooth out of the
     * meter level. If BLE is taken out of creation, this
     * method should be the one necessary to switch out the
     * command function without messing up the meter class
     *
     * @param command the command that will be sent through
     *                the Ble Adapter, to the meter
     */
    protected void sendCommand(byte[] command) {
        Logging.Debug("BaseMeter.sendcommand", command + new String(command)+ " byte size: " + command.length);


        Intent intent = new Intent(WRITE_COMMAND);
        intent.putExtra(BleService.A_EXTRA_DATA, command);

//        BleS.writeCharacteristic(command);
        context.sendBroadcast(intent);
    }

    public static final String WRITE_COMMAND = "BLE SHOULD WRITE COMMAND TO CHARACTERISTIC";

    /**
     * Checks the strings passed into it with strings that
     * are saved as the newest in the home system
     *
     * @param BGL  The BloodGlucose Level
     * @param Date The date
     * @param Time The time
     * @return True if they are equal with the newest information
     */
    protected boolean isMatchedReading(String BGL, String Date, String Time) {
        if (Debug.GRAB_ALL)
            return false;//if you set debug statement to be true, just go through everything everytime


        return (repeatData = (
                BGL.equals(bgl) &&
                        Date.equals(date) &&
                        Time.equals(time)
        ));
    }

    /**
     * This class updates the visuals by
     * bridging the meter classes and the
     * front screen. Added try catch
     * in case of view not active
     */
    protected void update() {
        //save information first
        dataSaver.addSet(DataSaver.lastNumber, finalReadings.get(0));
        dataSaver.addSet(DataSaver.lastDate, finalReadings.get(1));
        dataSaver.addSet(DataSaver.lastTime, finalReadings.get(2));
        BleService.UIConnected = BleService.GETTING_READINGS;

        try {
            Handler han = new Handler(context.getMainLooper());
            han.post(new Runnable() {

                @Override
                public void run() {
                    FragmentMeasurementActivity.updateVisuals(finalReadings.get(0), finalReadings.get(1), finalReadings.get(2));

                }

            });
        } catch (Exception ex) {
            Logging.Error(this.getClass().getSimpleName(), "EXCEPTION: ", ex);
        }
    }


    /**
     * Receives the information back from the
     * meter. it is a callback
     *
     * @param bs Information from the Device.
     */
    protected abstract void onNewData(byte[] bs);


    ///////////////////////////////////////////////////////////////////////////////TODO
    public boolean checkForReadings() {
        if (badGrabCount > 2) {
            BaseService.badChecksumFlag = true;
            return true;
        }
        return false;
    }

    public String toString() {
        return signature;
    }


    /**
     * Buffer holds all of the bytes that comes
     * in after the processing is completed
     * then checked against any wake-up string in
     * each meter
     */
    private ByteArrayOutputStream buffer;

    protected class WakeUpOnThisString extends Thread {
        /**
         * Holds the string that can be used to
         * be told to wake up
         */
        private String wakeUpString;

        public WakeUpOnThisString(String WakeUpString) {
            wakeUpString = WakeUpString;
        }

        private int SLEEP = 2;

        public void run() {
            //clean up and initialize
            boolean active = false;
            Logging.Info("BaseMeter.WakeUpOnThisString.doInBackground", "initialization");
            SystemClock.sleep(SLEEP * SECONDS);
            buffer = new ByteArrayOutputStream();

            //wait while connected
            while (connected) {
                SystemClock.sleep(SLEEP * SECONDS);
                					if(Debug.DEBUG)Logging.Info("BaseMeter.WakeUpOnThisString.doInBackground", "Array: " +buffer.toString());
                //break out of the loop if you find the wake up string
                if ((new String(buffer.toByteArray()).contains(wakeUpString))) {
                    active = true;
                    break;
                }
            }//end while

            deleteRegister();

            //if there is a new thing, start it up
            if (active) {
                Handler handler = new Handler(context.getMainLooper());
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        //start the BaseService
                        //////////////////////////////////////////////////////////////////////
                        Intent intent = new Intent(context, BaseService.class);
                        context.startService(intent);
                    }

                });

            }
        }
    }


    protected void unregister(){
        deleteRegister();

    }



    public void createNotification(int stringResourceId, int soundId, boolean toastAlso) {
        String notifyText = this.context.getResources().getString(stringResourceId);
//        if (toastAlso) {
//            Toast toast = Toast.makeText(context, notifyText, Toast.LENGTH_LONG);
//            toast.show();
//        }
        BaseService.createNotification(context, notifyText, soundId, toastAlso,1);
    }

}
