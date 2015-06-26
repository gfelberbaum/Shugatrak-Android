package com.applivate.shugatrak2;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

/**
 * *************************************************************************
 * FRAGMENT SETTINGS ACTIVITY
 * <p/>
 * <h3>Purpose of Activity:</h3> &nbsp; Control the Settings Screen Specifically
 * <p/>
 * <h3>Update notes:</h3> v0.1.5: &nbsp; Final beta FragmentSettingsActivity
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
public class FragmentSettingsActivity extends Fragment {

    private static DataSaver data;
    private static View rootView;
    /**
     * variable to hold the TextView of the email
     */
    private static TextView emailTV;
    /**
     * variable to hold the TextView of the password
     */
    private static TextView passwordTV;

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
        rootView = (View) inflater.inflate(R.layout.activity_fragment_settings,
                container, false);

        // Connect the TextView to the variable
        emailTV = (TextView) rootView.findViewById(R.id.takenUser);
        passwordTV = (TextView) rootView.findViewById(R.id.takenPass);

        // Add listener to variable
        emailTV.setOnEditorActionListener(new I());
        passwordTV.setOnEditorActionListener(new I());

        //
        data = new DataSaver(getActivity());
        if (!(data.readSet(DataSaver.userName).equals(DataSaver.NO_ITEM))) {
            emailTV.setText(data.readSet(DataSaver.userName));
        }
        if (!(data.readSet(DataSaver.Password).equals(DataSaver.NO_ITEM))) {
            passwordTV.setHint("**********");
        }

        return rootView;
    }

    /**
     * Makes an IME listener, which will listen for the enter button to be
     * pressed, then save info and make the keyboard disappear
     */
    private class I implements TextView.OnEditorActionListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            saveLogin();
            if (actionId == EditorInfo.IME_NULL
                    && event.getAction() == KeyEvent.ACTION_UP) {
                Logging.Verbose("FragmentSettingsActivity.onEditorAction:  ACTION_UP");
            }

            return false;// false means it will be post over ride
        }

    }

    /**
     * {@inheritDoc}
     */
    public void onResume() {
        super.onResume();
        updateMeter();
        ((TextView) rootView.findViewById(R.id.stmtButton)).setClickable(!data
                .readSet(DataSaver.meterType).equals(DataSaver.NO_ITEM));
        ((TextView) rootView.findViewById(R.id.stmtButton)).setEnabled(!data
                .readSet(DataSaver.meterType).equals(DataSaver.NO_ITEM));
    }

    /**
     * {@inheritDoc}
     */
    public void onPause() {
        super.onPause();
        saveLogin();
    }

    /**
     * Saves the information that is within the email TextView and Password
     * TextView
     */
    public static void saveLogin() {
        data.addSet(DataSaver.userName, emailTV.getText().toString());
        if (!passwordTV.getText().toString().equals("")) {
            data.addSet(
                    DataSaver.Password,
                    PasswordEncrypter.getInstance(
                            passwordTV.getText().toString())
                            .getEncryptedBase64());
        }
        Logging.Verbose("Settings.saveLogin", "Saving info - out");
    }

    /**
     * Updates the area that holds the Meter name
     */
    private void updateMeter() {

        TextView meterType = (TextView) rootView.findViewById(R.id.MeterType);
        TextView adapterTView = (TextView) rootView
                .findViewById(R.id.adapterSelected);

        if (data.readSet(DataSaver.DeviceAddresses).equals(DataSaver.NO_ITEM)) {
            adapterTView.setText("no adapter set up");
        } else {
            if(data.readSet(DataSaver.NAME_OF_ADAPTER).equals(DataSaver.NO_ITEM)){

                adapterTView.setText(data.readSet(DataSaver.DeviceAddresses));
            }else{
                adapterTView.setText(data.readSet(DataSaver.NAME_OF_ADAPTER));
            }
        }

        if (data.readSet(DataSaver.meterType).equals(DataSaver.NO_ITEM)) {
            meterType.setText("no meter selected");
        } else {
            meterType.setText(data.readSet(DataSaver.meterType));
        }
    }

}
