package com.applivate.shugatrakii;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * *************************************************************************
 * FRAGMENT MEASUREMENT ACTIVITY
 * <p/>
 * <h3>Purpose of Activity:</h3> &nbsp; Control the Reading Screen Specifically
 * <p/>
 * <h3>Update notes:</h3> v0.1.5: &nbsp; Final beta FragmentMeasurmentActivity
 * ready
 * <p/>
 * <h3>Known errors:</h3> V0.1.5: &nbsp; None Known
 *
 * @author Current: Ryan Hirschthal Original:Ryan Hirschthal &nbsp;
 *         {@code (c) 2014 Ryan Hirschthal, Advanced Decisions. (rhirschthal @ advanceddecisions . com)
 *         All rights reserved}
 * @version V0.1.5: Ryan
 * @category ShugaTrak
 * @see {@link TopLevelActivity}
 * ***************************************************************************
 */

public class FragmentMeasurementActivity extends Fragment {

    private static DataSaver dataSaver;

    // below are the connections to the screen
    private static TextView editBloodLevel;
    private static TextView editDate;
    private static TextView editTime;
    private static TextView editConnection;
    private static ProgressBar loading;
    private static TextView editAdapterOn;
    private static Button retryUploadButton;
    private static Context context;


    //These note the fingers' position on the screen
    int GLOBAL_TOUCH_POSITION_Y = 0;
    int GLOBAL_TOUCH_CURRENT_POSITION_Y = 0;
    boolean hasAlreadyActivated = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    /**
     * {@inheritDoc}
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.activity_fragment_measurement, container, false);

        dataSaver = new DataSaver(getActivity().getApplicationContext());

        editBloodLevel = (TextView) rootView.findViewById(R.id.Level);
        editDate = (TextView) rootView.findViewById(R.id.dateStamp);
        editTime = (TextView) rootView.findViewById(R.id.timeStamp);
        editConnection = (TextView) rootView.findViewById(R.id.connection);
        editConnection.setText(BleService.UIConnected);
        retryUploadButton = (Button) rootView.findViewById(R.id.retryButton);

        editAdapterOn = ((TextView) rootView.findViewById(R.id.adapter_is_on));
        loading = (ProgressBar) rootView.findViewById(R.id.indicate_transferring_readings);

        GLOBAL_TOUCH_POSITION_Y = 0;
        GLOBAL_TOUCH_CURRENT_POSITION_Y = 0;

        RelativeLayout TextLoggerLayout = (RelativeLayout) rootView.findViewById(R.id.activity_fragment_measurement);
        TextLoggerLayout.setOnTouchListener(
                new RelativeLayout.OnTouchListener() {

                    @Override
                    public boolean onTouch(View v, MotionEvent m) {
                        handleTouch(m);
                        return true;
                    }

                });


        return rootView;
    }

    /**
     * {@inheritDoc}
     */
    public void onResume() {
        super.onResume();
        updateVisuals(dataSaver.readSet(DataSaver.lastNumber),
                dataSaver.readSet(DataSaver.lastDate),
                dataSaver.readSet(DataSaver.lastTime));


        context = getActivity().getBaseContext();
        getActivity().registerReceiver(rec, filter());// set up the filter

    }

    /**
     * {@inheritDoc}
     */
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(rec);// remove the filter
    }

    /**
     * Returns a default string if the source string is empty, or null, or
     * contains "---"
     *
     * @param source      the return of the value from the (@link DataSaver) class
     * @param defaultText If the value is not found, return it with this value
     * @return either the value from the DataSaver class, or if
     */
    private static String getDefaultText(String source, String defaultText) {

        String retval = source;

        if (null == source || source.equals(DataSaver.NO_ITEM) || source.length() == 0) {
            retval = defaultText;
        }

        return retval;
    }

    /**
     * Will be used to update the screen quickly from the meter class
     *
     * @param bloodGlucoseLevel The Blood Glucose Level from reading
     * @param date              date from reading
     * @param time              time from reading
     */
    public static void updateVisuals(String bloodGlucoseLevel, String date, String time) {
        if (editBloodLevel == null || editDate == null || editTime == null)
            return;

        Logging.Verbose("bgl=[" + bloodGlucoseLevel + "]  date=[" + date + "]  time=[" + time + "]");

        bloodGlucoseLevel = getDefaultText(bloodGlucoseLevel, "[BG]");
        date = getDefaultText(date, "[date]");
        time = getDefaultText(time, "[time]");
        editAdapterOn.setVisibility(View.INVISIBLE);

        if (!date.equals("[date]")) {
            SimpleDateFormat original = new SimpleDateFormat(
                    "MMM dd, yyyy", Locale.US);
            Date trydate;
            try {
                trydate = original.parse(date.trim());
                original.applyPattern("M/d/yyyy");
                date = original.format(trydate);
            } catch (Exception ex) {
                Logging.Error("Reading.ERROR PARSING", "", ex);
            }

        }
        editBloodLevel.setText(checkHiOrLo(bloodGlucoseLevel));
        editDate.setText(date);
        editTime.setText(time);
        if (!getDefaultText(DataSaver.DeviceAddresses,"").isEmpty() &&BleService.UIConnected.equals(BleService.NO_CONNECTION_PHRASE) ){
            BleService.UIConnected = BleService.disconPhrase;

        }
        editConnection.setText(BleService.UIConnected);

        if (loading != null) {
            if (BleService.UIConnected.equals(BleService.GETTING_READINGS) || BleService.UIConnected.equals(BleService.REQUESTING_READINGS)) {
                loading.setVisibility(View.VISIBLE);
            } else {
                loading.setVisibility(View.INVISIBLE);

            }
        }

        int numberOfReadings = dataSaver.getArray().replaceAll("[^\\{]", "").length();
        if (numberOfReadings > 0) {
            retryUploadButton.setVisibility(View.VISIBLE);
            retryUploadButton.setText("Send " + numberOfReadings + " reading" + (numberOfReadings > 1 ? "s " : " "));
        } else {
            retryUploadButton.setText("");

            retryUploadButton.setVisibility(View.INVISIBLE);
        }

        if (BleService.UIConnected.equals(BleService.NOT_CONNECTED)) {
//			editConnection.setText(BleService.UIConnected);
            editAdapterOn.setVisibility(View.VISIBLE);
        } else {
            editAdapterOn.setVisibility(View.INVISIBLE);

        }

    }


    // ///////////////////////////////////////////////
    public static String checkHiOrLo(String originalBGL) {
        int checkNumber = 0;
        try {
            checkNumber = Integer.parseInt(originalBGL);
            if (checkNumber > 600 || checkNumber == -1) {
                return "HI";
            } else if (checkNumber < 20) {
                return "LO";
            }
        } catch (Exception ex) {
            return originalBGL;
        }
        return originalBGL;
    }

    /**
     * Creates a receiver that will decide what to do with any new information
     * that comes through the filter
     */
    private BroadcastReceiver rec = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BaseService.UPDATES)) {
                updateVisuals(dataSaver.readSet(DataSaver.lastNumber),
                        dataSaver.readSet(DataSaver.lastDate),
                        dataSaver.readSet(DataSaver.lastTime));
            } else if (action.equals(BaseService.MAKE_TOAST)) {
                updateVisuals(dataSaver.readSet(DataSaver.lastNumber),
                        dataSaver.readSet(DataSaver.lastDate),
                        dataSaver.readSet(DataSaver.lastTime));
                Toast.makeText(getActivity().getApplicationContext(),
                        intent.getStringExtra(BaseService.TOAST_EXTRA),
                        Toast.LENGTH_LONG).show();
            } else if (action.equals(BaseService.ENDING_READINGS)) {
                updateVisuals(dataSaver.readSet(DataSaver.lastNumber),
                        dataSaver.readSet(DataSaver.lastDate),
                        dataSaver.readSet(DataSaver.lastTime));
                editConnection.setText(BleService.UIConnected);
            } else if (action.equals(BaseService.STARTING_READINGS)) {
                updateVisuals(dataSaver.readSet(DataSaver.lastNumber),
                        dataSaver.readSet(DataSaver.lastDate),
                        dataSaver.readSet(DataSaver.lastTime));
                editConnection.setText(BleService.UIConnected);
            } else if (action.equals(BleService.A_CONNECTED)) {
                updateVisuals(dataSaver.readSet(DataSaver.lastNumber),
                        dataSaver.readSet(DataSaver.lastDate),
                        dataSaver.readSet(DataSaver.lastTime));
                editConnection.setText(BleService.UIConnected);
            } else if (action.equals(BleService.A_SERVICES_DISCOVERED)) {
                updateVisuals(dataSaver.readSet(DataSaver.lastNumber),
                        dataSaver.readSet(DataSaver.lastDate),
                        dataSaver.readSet(DataSaver.lastTime));
                editConnection.setText(BleService.UIConnected);
            } else if (action.equals(BleService.A_DISCONNECTED)) {
                updateVisuals(dataSaver.readSet(DataSaver.lastNumber),
                        dataSaver.readSet(DataSaver.lastDate),
                        dataSaver.readSet(DataSaver.lastTime));
                editConnection.setText(BleService.UIConnected);
            }

        }
    };

    /**
     * Creates a filter that will listen to all of the calls being sent, and
     * then only let certain ones through
     *
     * @return the filter created
     */
    private IntentFilter filter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BleService.A_CONNECTED);
        intentFilter.addAction(BleService.A_DISCONNECTED);
        intentFilter.addAction(BleService.A_SERVICES_DISCOVERED);

        intentFilter.addAction(BaseService.UPDATES);
        intentFilter.addAction(BaseService.MAKE_TOAST);
        intentFilter.addAction(BaseService.ENDING_READINGS);
        intentFilter.addAction(BaseService.STARTING_READINGS);

        return intentFilter;
    }


    synchronized void handleTouch(MotionEvent m) {

        if(BaseService.processing){
            return;
        }

        //Number of touches
        int pointerCount = m.getPointerCount();
        if (pointerCount != 0) {
            int action = m.getActionMasked();
            int actionIndex = m.getActionIndex();
//			Logging.Verbose("FragmentMeasurementActivity.handleTouch","action: "+ action +"; action index: " +actionIndex +"; pointer count: ");
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    GLOBAL_TOUCH_POSITION_Y = (int) m.getY();
                    Logging.Verbose("FragmentMeasurementActivity.handleTouch", "ACTION_DOWN" + " current " + GLOBAL_TOUCH_CURRENT_POSITION_Y + " prev " + GLOBAL_TOUCH_POSITION_Y);
                    break;
                case MotionEvent.ACTION_UP:
                    GLOBAL_TOUCH_CURRENT_POSITION_Y = 0;
                    hasAlreadyActivated = false;
                    Logging.Verbose("FragmentMeasurementActivity.handleTouch", "ACTION_UP" + " current " + GLOBAL_TOUCH_CURRENT_POSITION_Y + " prev " + GLOBAL_TOUCH_POSITION_Y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    GLOBAL_TOUCH_CURRENT_POSITION_Y = (int) m.getY();
                    int diff = GLOBAL_TOUCH_POSITION_Y - GLOBAL_TOUCH_CURRENT_POSITION_Y;
                    Logging.Verbose("FragmentMeasurementActivity.handleTouch", "ACTION_MOVE   Diff " + diff + " current " + GLOBAL_TOUCH_CURRENT_POSITION_Y + " prev " + GLOBAL_TOUCH_POSITION_Y);

                    if ((BleService.connected || dataSaver.isKCadapter())&& !hasAlreadyActivated) {
                        if (diff < -600) {          //GET ONE READING
                                hasAlreadyActivated = true;
                                Logging.Verbose("FragmentMeasurementActivity.handleTouch", "Should grab one readings");

                            if(dataSaver.isKCadapter()){
                                Intent intent = new Intent(BleService.getNewReadings);
                                context.sendBroadcast(intent);
                                BleService.UIConnected=BleService.LISTENING_PHRASE;
                                Intent updateIntent = new Intent(BaseService.UPDATES);
                                context.sendBroadcast(updateIntent);


                            }else {

                                Intent intent = new Intent(getActivity().getApplicationContext(), BaseService.class);
                                getActivity().startService(intent);
                            }
                        } else if (diff > 600) {    //GET ALL READINGS
                            hasAlreadyActivated = true;
                            try {
                                Logging.Verbose("FragmentMeasurementActivity.handleTouch", "should grab all reading");
                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setMessage("").setTitle(R.string.get_all_meter_readings)
                                        //As negative button is the left button on the adapter, and John wants the positive response on the left button, the positive response has been set to the negative button
                                        .setNegativeButton("Ok", new DialogInterface.OnClickListener() {

                                            public void onClick(DialogInterface dialog, int which) {
                                                dataSaver.removeMemoryOfReadings();
                                                if (dataSaver.isKCadapter()) {
                                                    Intent intent = new Intent(BleService.getNewReadings);
                                                    context.sendBroadcast(intent);

                                                    BleService.UIConnected = BleService.LISTENING_PHRASE;
                                                    Intent updateIntent = new Intent(BaseService.UPDATES);
                                                    context.sendBroadcast(updateIntent);


                                                } else {

                                                    Intent intent = new Intent(getActivity().getApplicationContext(), BaseService.class);
                                                    getActivity().startService(intent);
                                                }

                                            }
                                        });
                                //As positive button is the right button on the adapter, and John wants the negative response on the right button, the negative response has been set to the positive button
                                builder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {


                                    }
                                });
                                AlertDialog dialog = builder.create();
                                dialog.show();
                            } catch (Exception ex) {
                                Logging.Error("FragmentMeasurementActivity.handleTouch", "Error in the swiping event", ex);
                            }
                        }

                    } else if (!hasAlreadyActivated) {
                        if (diff < -600 || diff > 600) {//GET ONE READING
                            hasAlreadyActivated = true;

                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage("Press adapter button").setTitle("Not connected to adapter")
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    }).setCancelable(false);
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    }
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    GLOBAL_TOUCH_POSITION_Y = (int) m.getY(actionIndex);
                    Logging.Verbose("FragmentMeasurementActivity", "A_P_DOWN" + " current " + GLOBAL_TOUCH_CURRENT_POSITION_Y + " prev " + GLOBAL_TOUCH_POSITION_Y);
                    break;
                default:
            }

        } else {
            GLOBAL_TOUCH_POSITION_Y = 0;
            GLOBAL_TOUCH_CURRENT_POSITION_Y = 0;
            hasAlreadyActivated = false;
        }
    }


}
