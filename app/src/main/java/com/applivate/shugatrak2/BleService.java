
package com.applivate.shugatrak2;


import java.sql.Time;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;


import android.app.AlarmManager;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.ContactsContract;

/**
 * *************************************************************************
 * BLE SERVICE
 * <p/>
 * <p/>
 * <h3>Purpose of Activity:</h3>
 * &nbsp;This service is to connect to the
 * Bluetooth adapter as soon as it becomes
 * available and to talk to the adapter
 * when prompted
 * <p/>
 * <h3>Update notes:</h3>
 * v0.1.5:
 * &nbsp;Made the final beta service class
 * <p/>
 * <h3>Known errors:</h3>
 * v0.1.5:
 * &nbsp; Nothing yet
 *
 * @author Current: Ryan Hirschthal Original:Ryan Hirschthal &nbsp;
 *         {@code (c) 2014 Ryan Hirschthal, Advanced Decisions. (rhirschthal @ advanceddecisions . com)
 *         All rights reserved}
 *         ***************************************************************************
 * @version V0.1.5: Ryan
 * @category ShugaTrak
 */
public class BleService extends Service {
    /**
     * Constructor
     */
    public BleService() {
        super();
    }

    /**
     * Adapter for bluetooth, the phones side
     * of the bluetooth
     */
    private BluetoothAdapter BleA;
    /**
     * android built BLE wrapper class
     */
    private BluetoothGatt BleG;

    /**
     * A place to save the address if the
     * app has lost the connection for some
     * reason
     */
    private String DeviceMacAddress;
    /**
     * the Characteristic that will be written to most of the
     * time
     */
    private BluetoothGattCharacteristic charFIFO;


    //All the code below identifies UI States corresponding to
    //the BleService state.
    /**
     * getting readings from the meter
     * <p/>
     * UIConnected should be set up to this in the base service
     * and then switched back in BaseMeter.deleteRegister().
     */
    public static String GETTING_READINGS = "transferring readings";  //  R.string.transferring_readings
    /**
     * connecting to the meter, but does not
     * have the services yet
     */
    private static String conPhrase = "connecting to adapter";  //  R.string.connecting_to_adapter

    /**
     * no longer connected to the adapter
     */
    private static String disconPhrase = "adapter is set up";  //  R.string.adapter_is_set_up
    /**
     * completely connected to the adapter
     */
    private static String servicePhase = "communicating with adapter";  //R.string.connected_to_adapter
    ///////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////
    private static String NO_CONNECTION_PHRASE = "adapter is not set up";// R.string.adapter_not_set_up
    public static String REQUESTING_READINGS = "requesting readings";  //  R.string.requesting_readings
    public static String NOT_CONNECTED = "Bluetooth is off";  //  R.string.bluetooth_is_off


    public final boolean isSamsung = (Build.MANUFACTURER.toLowerCase(Locale.ENGLISH).contains("samsung"));
    public final boolean isHTC = (Build.MANUFACTURER.toLowerCase(Locale.ENGLISH).contains("htc"));
    public final boolean NEED_CLOSE_CONNECT = !isSamsung;//Moto TRUE, HTC TRUE, Samsung FALSE			//TODO
    public final boolean AUTO_CONNECT = true;
    /**
     * To be used with the User Interface.
     * <p>This variable is a {@code String} that tells the user what
     * state the app is in. This will be updated as the state changes,
     * such as connected, transferring readings, or disconnected </p>
     */
    public static String UIConnected = NO_CONNECTION_PHRASE;


    //the code below does not matter what they say, because they will be checked
    //with a .equals(BleService.____), so it will check against itself
    /**
     * This string tell the app the the program is searching for a
     * new adapter. The app disconnect from the current adapter,
     * so that it will be visible to the user looking for it.
     */
    public static final String SEARCH_DISCONNECT = "SHUGATRAK SEARCH HAS STARTED";
    /**
     * Signal used to signal the receiver that the Gatt has connected
     */
    public final static String A_CONNECTED = "GATT HAS CONNECTED";
    /**
     * Signal used to signal the receiver that the Gatt has disconnected
     */
    public final static String A_DISCONNECTED = "GATT HAS DISCONNECTED";
    /**
     * Signal used to signal the receiver that the Gatt has found services
     */
    public final static String A_SERVICES_DISCOVERED = "GATT DISCOVERED SERVICES";
    /**
     * Signal used to signal the receiver that the Gatt has data available
     */
    public final static String A_DATA_AVAILABLE = "NEW DATA FROM GATT";
    /**
     * Signal used to signal the receiver that the Gatt has extra data
     */
    public final static String A_EXTRA_DATA = "GATT HAS EXTRA DATA";

    public final static String DEVICE_ADDRESS = "SHUGATRAK DEVICE ADDRESS";

    ///////////////////////
    //ASSUMES CONNECTBLUE//
    ///////////////////////
    private final UUID UUID_FIFO = UUID.fromString("2456e1b9-26e2-8f83-e744-f34f01e9d703");//contains the FIFO UUID
    private final UUID UUID_FIFO_CHARA = UUID.fromString("00002902-0000-1000-8000-0805f9b34fb");//contains the descriptor needed

    /**
     * String to make the {@link #UIConnected} show the right message
     * after it finishes the readings
     */
    public static boolean connected = false;


    private void initializeFromResourceStrings(Context context) {

        GETTING_READINGS = context.getResources().getString(R.string.transferring_readings);
        conPhrase = context.getResources().getString(R.string.connecting_to_adapter);
        NO_CONNECTION_PHRASE = context.getResources().getString(R.string.adapter_not_set_up);
        disconPhrase = context.getResources().getString(R.string.adapter_is_set_up);
        REQUESTING_READINGS = context.getResources().getString(R.string.requesting_readings);
        NOT_CONNECTED = context.getResources().getString(R.string.adapter_is_set_up) + " ";
        servicePhase = context.getResources().getString(R.string.connected_to_adapter);
//		NOT_CONNECTED = context.getResources().getString(R.string.bluetooth_is_off);
    }


    /**
     * Makes a broadcast receiver for the application. If there is
     * a change in either calling to search, or Bluetooth on device
     * state has changed, this is the part of the class that decides how to change it
     */
    public BroadcastReceiver received = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            initializeFromResourceStrings(context);

            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                adapterChanged(context, intent);
            } else if (action.equals(SEARCH_DISCONNECT)) {
                close();
            }else if(action.equals(BaseService.ENDING_READINGS)){
                if((new DataSaver(getApplicationContext())).isKCadapter())
                    disconnect();
            }else if(action.equals(getNewReadings)){
                if(isDisconnected)
                    connect((new DataSaver(getApplicationContext())).readSet(DataSaver.DeviceAddresses));
            }else if (action.equals(BaseMeter.justSayConnected)){
                justSayConnected();
            }else if (action.equals(BaseMeter.WRITE_COMMAND)){
                Logging.Debug("BleService.OnReceive -> in write command");
                writeCharacteristic(intent.getByteArrayExtra(A_EXTRA_DATA));
            }

        }
    };

    /**
     * creates a filter for all of the broadcasts that could happen
     * because there are a ton of intents flying everywhere in
     * android OS, this method makes sure that it will only allow
     * ones that are preferred to get through. decisions on how to
     * use them are made in {@link #received}
     *
     * @return IntentFilter The finished filter for the intents
     */
    public IntentFilter filter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction((BluetoothAdapter.ACTION_STATE_CHANGED));
        intentFilter.addAction(SEARCH_DISCONNECT);
        intentFilter.addAction(getNewReadings);
        intentFilter.addAction(BaseService.ENDING_READINGS);
        intentFilter.addAction(BaseMeter.justSayConnected);
        intentFilter.addAction(BaseMeter.WRITE_COMMAND);
        return intentFilter;
    }


    /**
     * Called by {@link #received}. If it was called, then Bluetooth
     * Has changed what state it is in, and will need to be set up
     * accordingly. If it is turning off, then it will make a notification
     * If it is turning on, then it will try to reconnect
     *
     * @param context context so it will be a single call to dataSaver,
     *                instead of making a complete instance for this
     *                one call
     * @param intent  The intent from the call, so that we can get the
     *                state of the Bluetooth Device
     */
    private void adapterChanged(Context context, Intent intent) {
        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

        if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
            if (!(new DataSaver(context).readSet(DataSaver.DeviceAddresses)).equals(DataSaver.NO_ITEM)) {

                //Create the foreground notification
                Notification.Builder not = new Builder(context);
                not.setContentTitle("ShugaTrak")    //change to R.string
                        .setSmallIcon(R.drawable.notification_icon_1)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))   // makes the main icon, on the left
                        .setContentText("Please turn Bluetooth On");

                Notification notification = not.build();
                startForeground(1, notification);
                //Update with the front screen the status of the front screen
                UIConnected = NOT_CONNECTED;
                Intent updateIntent = new Intent(BaseService.UPDATES);
                sendBroadcast(updateIntent);

//				Intent intent = new Intent()
//				TextView errorText = (TextView) rootView.findViewById(R.id.errorCodeTextView);
//				errorText.setText(R.string.bluetooth_is_off);
//				errorText.setTextColor(Color.RED);
            }
        } else if (state == BluetoothAdapter.STATE_ON) {
            Logging.Info("BleService.adapterChanged", "In the State on with BleInfo");

            if (!(new DataSaver(context).readSet(DataSaver.DeviceAddresses)).equals(DataSaver.NO_ITEM)) {

                //Update with the front screen the status of the front screen
                UIConnected = disconPhrase;
                Intent updateIntent = new Intent(BaseService.UPDATES);
                sendBroadcast(updateIntent);

                stopForeground(true);
                DeviceMacAddress = (new DataSaver(getApplicationContext())).readSet(DataSaver.DeviceAddresses);
                close();
                connect(DeviceMacAddress);
            }
        }
    }


    /**
     * a call to state that the Service without
     * having it be connected to that class
     * specifically. This will allow it to run
     * even in the background
     *
     * @param intent  the intent used to start it
     *                if it started by OS, not app,
     *                it may be null
     * @param flags   set by the OS to allow the start
     *                if it was resent or not, and if it
     *                has the original intent
     * @param startID set by user. If there are multiple
     *                times that the service would be started
     *                and ended, it only keeps one going, but
     *                updates it each time from this method
     *                if the user wants to make sure not to
     *                close if a newer one has been started
     *                then the number here is the way to do so
     *                <p/>
     *                return {@code Service#START_STICKY} to
     *                tell the phone that if the app
     *                is destroyed by the the phone,
     *                then it will restart it
     */
    public int onStartCommand(Intent intent, int flags, int startID) {
        if (BleG != null) return START_STICKY;


        BleThread thread = new BleThread(intent);
        thread.start();
//		if(intent != null)
//			DeviceMacAddress = intent.getStringExtra(DEVICE_ADDRESS);
//		else DeviceMacAddress = (new DataSaver(getApplicationContext())).readSet(DataSaver.DeviceAddresses);
//
//
//		registerReceiver(received, filter());
//		if(BleA==null) initialize();
//		connect(DeviceMacAddress);
        return START_STICKY;

    }

    //////////////////////////////////////////////////////////////////////////////////////
    private class BleThread extends Thread {
        Intent intent;

        public BleThread(Intent intent) {
            super();
            this.intent = intent;
        }

        public void run() {
            Logging.Debug("BleService.run->"
                    + (new DataSaver(getApplicationContext())).isKCadapter());
            if (intent != null)
                DeviceMacAddress = intent.getStringExtra(DEVICE_ADDRESS);
            else
                DeviceMacAddress = (new DataSaver(getApplicationContext())).readSet(DataSaver.DeviceAddresses);

//            if (
//                    intent==null
//                    ||
//                    (
//                    !intent.getBooleanExtra(hasStartedTimer,false)
//                    &&
//                    (new DataSaver(getApplicationContext())).isKCadapter()
//                    )
//                    ){
//                setTimer();
//            }

            registerReceiver(received, filter());
            if (BleA == null) initialize();
            connect(DeviceMacAddress);
        }
    }


    /**
     * Holds all of the async. callback information from the Bluetooth Adapter
     */
    private final BluetoothGattCallback callback = new BluetoothGattCallback() {
        /**
         * If the Bluetooth adapter connects or disconnects from the adapter,
         * this method is called through asynchronous callbacks
         */
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int state) {
            String intentAction;
            Logging.Debug("BleService.onConnectionStateChange:  State Changed, status is " + status);
            if (status == 133) {
                Logging.Debug("BleService.onConnectionStateChange:  Got a 133, retrying");
                close();
                connect(DeviceMacAddress);
            }
            if (status != 0){
                close();
                connect(DeviceMacAddress);
                return;
            }

            if (state == BluetoothProfile.STATE_CONNECTED) {
                Logging.Info("BleService.onConnectionStateChange:  State connected, no services");
                connected = true;

                //Broadcast connecting
                intentAction = A_CONNECTED;
                UIConnected = conPhrase;
                broadcastUpdate(intentAction);

                BleG.discoverServices();

            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                Logging.Info("BleService.onConnectionStateChange:  State Disconnected");
                connected = false;

                isDisconnected = true;
                //Broadcast disconnected
                intentAction = A_DISCONNECTED;
                UIConnected = disconPhrase;
                broadcastUpdate(intentAction);
                //TODO
                Logging.Debug("BleService.onConnectionStateChange:  Manufacterer is " + Build.MANUFACTURER);
                if (NEED_CLOSE_CONNECT &&!(new DataSaver(getApplicationContext())).isKCadapter()) {
                    Logging.Debug("BleService.onConnectionStateChange:  Issuing manual Reconnect");
                    close();
                    connect(DeviceMacAddress);
                }

            }
        }

        /**
         * Once the services are discovered (or error code thereof) then
         * it will send out a call to start {@link BaseService} to start
         */
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Logging.Info("BleService.onServicesDiscovered:  Services info, Status = " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                //Broadcast update
                UIConnected = servicePhase;
                broadcastUpdate(A_SERVICES_DISCOVERED);

                //find the desired services
                configureGattServices(getServices());

                //start the BaseService
                //////////////////////////////////////////////////////////////////////
                Intent intent = new Intent(getApplicationContext(), BaseService.class);
                startService(intent);
            }
        }

        /**
         * a callback saying that the characteristic specified has been read, such as
         * setting up the enable notifications
         */
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic chara, int status) {
            Logging.Info("BleService.onCharacteristicRead:  Characteristic Read info, Status = " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(A_DATA_AVAILABLE, chara);

            }
        }

        /**
         * a callback saying that the characteristic specified has been updated/changed
         * such as receiving new information
         */
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic chara) {
//			Logging.Info("BleService.onCharacteristicChanged:  Characteristic Changed");
            broadcastUpdate(A_DATA_AVAILABLE, chara);
        }
    };

    /**
     * sends the broadcast out to the receiver in the base class, which
     * will decide what to do base on the state the device is in
     *
     * @param action the state the receiver will be notified of
     */
    private void broadcastUpdate(String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * sends the broadcast out to the receiver in the base class,
     * stating there is new data available for the application
     * to look at and use. it will also send them the data
     * to  decipher it there
     *
     * @param action action the state the receiver will be notified of, found or change
     * @param chara  the new information that is has been received by the application
     */
    private void broadcastUpdate(String action, BluetoothGattCharacteristic chara) {
        final Intent intent = new Intent(action);
        final byte[] data = chara.getValue();

        intent.putExtra(A_EXTRA_DATA, data);
        sendBroadcast(intent);
    }

    /**
     * This method is to reset the UIconnected phrase back to normal, so that it will
     * get off of transferring readings
     */
    public void justSayConnected() {
        if (connected) {
            UIConnected = servicePhase;
        } else if (UIConnected.equals(NOT_CONNECTED)) {
            //It is already set up to what it supposed to be anyway
        } else {
            UIConnected = disconPhrase;
        }
    }

    /**
     * Makes the class into a binder, allowing for the
     * class to connect and interact with other classes
     */
    public class LocalBinder extends Binder {
        BleService getService() {
            return BleService.this;
        }
    }

    /**
     * called when a class wants to bind and connect
     * to the application. Should be done if the
     * class wants to interact
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * makes a binder from this class, so others can
     * interact with it
     */
    private final IBinder mBinder = new LocalBinder();

    /**
     * Callback when a class is done being binded
     * to the application, or if the class ends
     * before stopping
     * <p> This method will make it so the
     * service will end if that is all it
     * is worrying about
     */
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    /**
     * creates an instance of the adapter, so that it can connect
     * to anything that it finds
     *
     * @return returns true if it can make the adapter
     * false otherwise
     */
    public boolean initialize() {
        Logging.Debug("BleService.initialize:  Initializing the Adapter");
        BleA = (BluetoothAdapter) ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if (BleA == null) {
            Logging.Info("BleService.initialize:  No Adapter Found");
            return false;
        }
        Logging.Debug("BleService.initialize:  Adapter Successfully Made");
        return true;
    }

    /**
     * To connect to the device. it will make
     * a new connection to the device
     *
     * @param address the address of the device
     * @return true if it connects, false otherwise
     */
    public boolean connect(final String address) {
        Logging.Debug("BleService.connect:  in connect method");
        DataSaver dataSaver = new DataSaver(getApplicationContext());
        if (BleA == null || address == null) {
            Logging.Warning("BleService.connect:  BleA is not on, or there is no address");
            return false;
        }
        if(BleG!= null){
            BleG.connect();
            return true;
        }

        isDisconnected = false;
        final BluetoothDevice device = BleA.getRemoteDevice(address);

        BleG = device.connectGatt(this, AUTO_CONNECT, callback);
        DeviceMacAddress = address;
        dataSaver.addSet(DataSaver.DeviceAddresses, DeviceMacAddress);
        Logging.Debug("BleService.connect:  Successfully finished the connect method");
        UIConnected = disconPhrase;
        broadcastUpdate(BaseService.UPDATES);
//        FragmentMeasurementActivity.updateVisuals(dataSaver.readSet(DataSaver.lastNumber), dataSaver.readSet(DataSaver.lastDate), dataSaver.readSet(DataSaver.lastTime));
        return true;

    }

    /**
     * if the gatt was not disconnected, do it here
     */
    public void disconnect() {
        Logging.Debug("BleService.disconnect", "In Disconnect");
        if (BleA == null || BleG == null) return;
        BleG.disconnect();

        setTimer();
        //Below has been written so that we can monitor how the connection rates work with the current KC adapter. This call only happens if it is KC, so that part is not necessary
        if(Debug.KC_DEBUG){
            Date currentTimeDate = new Date();
            BaseService.createNotification(
                    getApplicationContext(),
                    "This app has disconnected " + (++numberOfConnects)+ "time"+(numberOfConnects!=1?"s ":" ")+ "with the last time being at " +currentTimeDate.toString(),
                    0,
                    false,
                    5
            );
        }
    }

    long numberOfConnects;



    /**
     * to formally end the service, close and remove
     * the connection to the gatt
     */
    public void close() {
        Logging.Debug("BleService.close", "closing the method");
        if (BleG == null) return;
        BleG.disconnect();
        BleG.close();
        BleG = null;
        broadcastUpdate(A_DISCONNECTED);
    }

    /**
     * read a characteristic chosen to be read
     *
     * @param chara the characteristic to read
     */
    public void readCharacteristic(BluetoothGattCharacteristic chara) {
        Logging.Debug("BleService.readCharacteristic:  reading characteristic");
        if (BleG == null || BleA == null) return;
        BleG.readCharacteristic(chara);
    }


    /**
     * A shortcut for the meter classes to be able to write without
     * knowing anything about the BleService class
     *
     * @param data the command that you want to be sent off to the adapter
     */
    public void writeCharacteristic(byte[] data) {
        Logging.Debug("BleService.writeCharacteristic:  Writing Characteristic w/ byte[]");
        if (BleG == null || BleA == null) return;
        charFIFO.setValue(data);
        BleG.writeCharacteristic(charFIFO);
    }

    /**
     * choose a characteristic that you either want
     * to read, or to stop reading
     *
     * @param chara   the characteristic to update
     * @param enabled whether to update it or not
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic chara, boolean enabled) {
        if (BleA == null || BleG == null) return;
        BleG.setCharacteristicNotification(chara, enabled);

        if (UUID_FIFO.equals(chara.getUuid())) {
            BluetoothGattDescriptor descripter = chara.getDescriptor(UUID_FIFO_CHARA);
            descripter.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            BleG.writeDescriptor(descripter);
        }

    }

    /**
     * if it is connected, gets all of the services of the device
     *
     * @return the services found, in List, BluetoothGattService
     */
    public List<BluetoothGattService> getServices() {
        if (BleA == null || BleG == null) return null;
        return BleG.getServices();
    }

    /**
     * This method runs through each and every service and characteristic, and if it finds
     * one that is preferred, it will tell the program to update upon any changes
     *
     * @param gattServices the services to run through to check
     */
    private void configureGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        Logging.Debug("BleServices.configureGattServices:  " + gattServices.toString());
        for (BluetoothGattService serv : gattServices) {
            List<BluetoothGattCharacteristic> gatcaras = serv.getCharacteristics();
            for (BluetoothGattCharacteristic cara : gatcaras) {
                configureGattCharacteristic(cara);
            }
        }
    }

    /**
     * this method looks at the characteristic individually and makes the decisions for
     * configureGattServices.
     *
     * @param cara a characteristic to check and see if it is identified.
     */
    private void configureGattCharacteristic(BluetoothGattCharacteristic cara) {
        final int charaProp = cara.getProperties();
        if (cara.getUuid().equals(UUID_FIFO)) {
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                charFIFO = cara;
                setCharacteristicNotification(cara, true);
            }
        }
    }



    public static boolean isDisconnected;
    public final String hasStartedTimer = "Started KC Timer";
    public static final String getNewReadings = "timeToGetNewReadings";
    public static  long KC_ADAPTER_FREQUENCY =
//            1000*60;//Test timer todisconnect for one minute and reconnect after
                   4*
                    60*/*hours*/
                    60*/*minutes*/
                    1000/*seconds*/;


    private void setTimer(){
        Logging.Verbose("BleService.set timer - in set timer now");
        AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent connectIntent = new Intent (getNewReadings);
        connectIntent.putExtra(hasStartedTimer, true);



        PendingIntent pendConnect =  PendingIntent.getBroadcast(this, 1, connectIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarm.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + KC_ADAPTER_FREQUENCY, pendConnect);
    }
}