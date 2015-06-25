package com.applivate.shugatrak2;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

/****************************************************************************
 * INTERNET SYNCING
 * 
 * <h3>Purpose of Activity:</h3> &nbsp; This class handles the sending of data
 * to the portal, and then the return of response from the portal
 * 
 * <h3>Update notes:</h3> v0.1.5: &nbsp; Final beta InternetSyncing ready
 * 
 * <h3>Known errors:</h3> V0.1.5: &nbsp; None Known yet
 * 
 * @category ShugaTrak
 * @version V0.1.5: Ryan
 * @author Current: Ryan Hirschthal Original:Ryan Hirschthal &nbsp;
 *         {@code (c) 2014 Ryan Hirschthal, Advanced Decisions. (rhirschthal @ advanceddecisions . com)
 * All rights reserved}
 * @see {@link InternetPayload}
 *****************************************************************************/
public class InternetSyncing {
	private Context context;
	private DataSaver dataSaver;
	/**
	 * Flag that holds Base meter Async class from continuing until this gets a
	 * response
	 */
	public static boolean synced;
	/**
	 * Saves the SD_STRING corresponding with the status code given, to give to
	 * make a Notification
	 */
	public static String portalErrorString;

	public static final String POST_URL = "https://www.shugatrak.com/app/isync/";

	// STATUS CODES
	public static final int GOOD_SEND = 200;
	public static final int BAD_REQUEST = 400;
	public static final int BAD_AUTH = 403;
	public static final int SERVER_ERROR = 500;
	public static final int METHOD_NOT_IMPLEMENTED = 501;

	// All of the responses for the alert
	// Dialog that will pop up for
	// Verify
	public static final String TP_GOOD_STRING = "Yay! Your user name and password have been verified";
	public static final String TP_BAD_REQUEST_STRING = "ShugaTrak encountered an error: 400";
	public static final String TP_BAD_AUTHENTICATION_STRING = "Sorry! Your user name or password is incorrect";
	public static final String TP_SERVER_ERROR_STRING = "Server Error: 500";// TODO
	public static final String TP_METHOD_NOT_IMPLEMENTED_STRING = "Method not implemented: 501";
	public static final String TP_NO_INTERNET_STRING = "Sorry, no internet connection";// lack
																						// of
																						// internet
	public static final String TP_UNKNOWN_INTERNET_STRING = "UNKNOWN ERROR CODE: ";

	// All Notifications messages, at least previously
	// now only SD_GOOD_STRING not used, all others
	// will be used if there is a status code call
	// to it
	public static final String SD_GOOD_STRING = "Yay! info was sent up";
	public static final String SD_BAD_REQUEST_STRING = "ShugaTrak encountered an error: 400";
	public static final String SD_BAD_AUTHENTICATION_STRING = "Unable to upload readings, check username and password";
	public static final String SD_SERVER_ERROR_STRING = "Server error: 500";// TODO
	public static final String SD_METHOD_NOT_IMPLEMENTED_STRING = "Method not implemented: 501";
	public static final String SD_NO_INTERNET_STRING = "Unable to upload readings, no internet connection";// lack
																											// of
																											// internet
	public static final String SD_UNKNOWN_INTERNET_STRING = "UNKNOWN ERROR CODE: ";

	public static String EN_GOOD_STRING = "R.string.meter_not_responding";
	public static final String EN_BAD_REQUEST_STRING = SD_BAD_REQUEST_STRING;
	public static final String EN_BAD_AUTHENTICATION_STRING = SD_BAD_AUTHENTICATION_STRING;
	public static final String EN_SERVER_ERROR_STRING = SD_SERVER_ERROR_STRING;// TODO
	public static final String EN_METHOD_NOT_IMPLEMENTED_STRING = SD_METHOD_NOT_IMPLEMENTED_STRING;
	public static final String EN_NO_INTERNET_STRING = SD_NO_INTERNET_STRING;// lack of internet
	public static final String EN_UNKNOWN_INTERNET_STRING = SD_UNKNOWN_INTERNET_STRING;

	private static boolean stringsInitializedFromResources = false;

	public static String errorDump;


	/**
	 * Constructor
	 * 
	 * @param context
	 *            Used to set up the dialog pop up for Verify function
	 */
	public InternetSyncing(Context context) {
		readStringsFromResources(context);
		dataSaver = new DataSaver(context);
		this.context = context;
		synced = false;
		portalErrorString = null;
	}


	public static void readStringsFromResources(Context context) {
		if (!stringsInitializedFromResources) {
			try {
				EN_GOOD_STRING = context.getResources().getString(R.string.meter_not_responding);
                Logging.Verbose("InternetSyncing: readStringsFromResources:   EN_GOOD_STRING:", EN_GOOD_STRING);

				// TODO:  implement this behavior for other strings in this class....

				stringsInitializedFromResources = true;
			}
			catch (Exception ex) {
				Logging.Error("InternetSyncing.readStringsFromResources()", " EXCEPTION: ", ex);
			}
		}
	}

	public void ErrorSync(InternetPayload payload) {

		// start the async class
		ErrorSyncTask task = new ErrorSyncTask();
		task.execute(payload);

	}

	private class ErrorSyncTask extends AsyncTask<InternetPayload, Void, Integer> {

		@Override
		protected Integer doInBackground(InternetPayload... params) {
			Logging.Info("InternetSyncing.ErrorSyncTask.doInBackground()", " start task");

			portalErrorString = SD_NO_INTERNET_STRING;
			try {// START TRY CATCH

				// setup info to send
				InternetPayload payload = params[0];
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
				// if(payload.getReadings() ==
				// null||payload.getReadings().isEmpty())
				// nameValuePairs.add(new BasicNameValuePair("payload",
				// payload.toString()+"["+ data.getArray().replaceFirst(",",
				// "")+"]}"));
				//
				// else {
				nameValuePairs.add(new BasicNameValuePair("payload", payload.toString() + dataSaver.getArray() + "]}"));
//				}

				if (Debug.DEBUG) {
					Logging.Debug("InternetSyncing.ErrorSyncTask.doInBackground() payload: ", payload.toString() + dataSaver.getArray() + "]}");
					Logging.Debug("InternetSyncing.ErrorSyncTask.doInBackground()", "Past nameValuePairs");
				}

				// set up send location and send info up
				DefaultHttpClient client = new DefaultHttpClient();
				HttpPost post = new HttpPost(POST_URL);
				post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				// the response from the portal
				HttpResponse response = client.execute(post);
				StatusLine status = response.getStatusLine();
				int httpStatusCode = status.getStatusCode();
				Logging.Info("InternetSyncing.ErrorSyncTask.doInBackground() ", "httpStatusCode: " + httpStatusCode);
				return httpStatusCode;
			} catch (Exception ex) {
				Logging.Error("InternetSyncing.ErrorSyncTask.doInBackground()", "EXCEPTION:  ", ex);
			}

			if (Debug.DEBUG) {
				Logging.Debug("InternetSyncing.ErrorSyncTask.doInBackground()", "Coonection bad");
			}
			dataSaver.saveArray(params[0].getReadings());
			return 0;
		}

		public void onPostExecute(Integer http_code) {

			String errorPhrase = "";

			switch (http_code) {
			case GOOD_SEND:
				errorPhrase = EN_GOOD_STRING;
				break;
			case BAD_REQUEST:
				errorPhrase = EN_BAD_REQUEST_STRING;
				break;
			case BAD_AUTH:
				errorPhrase = EN_BAD_AUTHENTICATION_STRING;
				break;
			case SERVER_ERROR:
				errorPhrase = EN_SERVER_ERROR_STRING;
				break;
			case METHOD_NOT_IMPLEMENTED:
				errorPhrase = EN_METHOD_NOT_IMPLEMENTED_STRING;
				break;
			case 0:
				errorPhrase = EN_NO_INTERNET_STRING;
				break;
			default:
				errorPhrase = EN_UNKNOWN_INTERNET_STRING + http_code;

				break;
			}

			// TODO: ASK JOHN WHAT HE WOULD LIKE TO DO ABOUT THESE EXTRA SITUATIONS.
			errorPhrase = EN_GOOD_STRING;

			Intent notificationIntent = new Intent(context, TopLevelActivity.class);
			PendingIntent notificationPendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

			// Action Bar icon for the notification
			Notification.Builder not = new Notification.Builder(context).setSmallIcon(R.drawable.notification_icon_1)
							.setContentTitle("ShugaTrak") // Title for notification
							.setContentText(errorPhrase)  // body of text for notification
							// .setStyle( new Notification.BigTextStyle().bigText(finalString))  //makes the drop down if there is more
							.setAutoCancel(true)// gets rid of when clicked
							.setContentIntent(notificationPendingIntent)// makes the place when you clicked, as specified above
							// .setSound(soundUri)  //specify the sound to play here
							.setTicker(errorPhrase)// what shows up quickly at the top of the not. bar
							.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher))  // makes the main icon, on the left
			;

			// show the notification
			((NotificationManager) context.getSystemService(BaseService.NOTIFICATION_SERVICE)).notify(001, not.build());

			// Make a toast message saying the same thing
			Intent toastIntent = new Intent(BaseService.MAKE_TOAST);
			toastIntent.putExtra(BaseService.TOAST_EXTRA, errorPhrase);
			context.sendBroadcast(toastIntent);

		}

	}

	/**
	 * This call sets up the information to send to the portal, then sends it to
	 * a {@link AsyncTask} class {@link SyncDataTask}
	 */
	public void SyncData(ArrayList<Reading> readings) {

		InternetPayload payload = new InternetPayload(dataSaver.readSet(DataSaver.userName),
						dataSaver.readSet(DataSaver.Password), readings);

		// start the async class
		SyncDataTask task = new SyncDataTask();
		task.execute(payload);

	}

	/**
	 * Asynchronous class designed to send the data up To the portal
	 */
	private class SyncDataTask extends AsyncTask<InternetPayload, Void, Void> {

		@Override
		protected Void doInBackground(InternetPayload... params) {
			Logging.Info("InternetSyncing.Sync DataTask", " start task");

			portalErrorString = SD_NO_INTERNET_STRING;
			try {

				// setup info to send
				InternetPayload payload = params[0];
				payload.errors = errorDump;
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
				 if(payload.getReadings() ==
				 null||payload.getReadings().isEmpty())
				 nameValuePairs.add(new BasicNameValuePair("payload",
						 (payload.toString()+"{"+ dataSaver.getArray().substring(2)+"]}").replace("{{","{")));

				 else {
				nameValuePairs.add(new BasicNameValuePair("payload", payload.toString() + dataSaver.getArray() + "]}"));
				}

				if (Debug.DEBUG) {
					Logging.Debug("InternetSync.SyncDataTask.doInBackground()", payload.toString() + dataSaver.getArray() + "]}");
					Logging.Debug("InternetSync.SyncDataTask.doInBackground()", nameValuePairs.toString());
					Logging.Debug("InternetSync.SyncDataTask.doInBackground()", "Past adding nameValuePairs");
				}

				// set up send location and send info up
				DefaultHttpClient client = new DefaultHttpClient();
				HttpPost post = new HttpPost(POST_URL);
				post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				// the response from the portal
				HttpResponse response = client.execute(post);
				StatusLine status = response.getStatusLine();
				int httpStatusCode = status.getStatusCode();

				Logging.Info("InternetSync.SyncDataTask.doInBackground()", "httpStatusCode: " + httpStatusCode);

				// Read Status codes
				switch (httpStatusCode) {
				case (GOOD_SEND):
					if (Debug.DEBUG) {
						Logging.Info("InternetSync.SyncDataTask.doInBackground()", "Connection Good");
					}
					portalErrorString = SD_GOOD_STRING;

					BaseService.numberOfReadingsSentUp = payload.getReadings().size()
									+ dataSaver.getArray().replaceAll("[^\\{]", "").length();

					if (Debug.DEBUG) {
						Logging.Info("InternetSync.SyncDataTask.doInBackground()", "Finished the writeup");
					}
					dataSaver.removeArray();
					break;

				case (BAD_REQUEST):
					if (Debug.DEBUG) {
						Logging.Info("InternetSync.SyncDataTask.doInBackground()", "bad request");
					}
					portalErrorString = SD_BAD_REQUEST_STRING;
					dataSaver.removeArray();
					break;

				case (BAD_AUTH):
					if (Debug.DEBUG) {
						Logging.Info("InternetSync.SyncDataTask.doInBackground()", "bad authentication");
					}
					portalErrorString = SD_BAD_AUTHENTICATION_STRING;
//					dataSaver.saveArray(payload.getReadings());
					break;

				case (SERVER_ERROR):
					if (Debug.DEBUG) {
						Logging.Info("InternetSync.SyncDataTask.doInBackground()", "server error");
					}
					portalErrorString = SD_SERVER_ERROR_STRING;
//					dataSaver.saveArray(payload.getReadings());
					break;

				case (METHOD_NOT_IMPLEMENTED):
					if (Debug.DEBUG) {
						Logging.Info("InternetSync.SyncDataTask.doInBackground()", "method not implemented");
					}
					portalErrorString = SD_METHOD_NOT_IMPLEMENTED_STRING;
//					dataSaver.saveArray(payload.getReadings());
					break;

				default:
					portalErrorString = SD_UNKNOWN_INTERNET_STRING + httpStatusCode;
				}

				if(
						! (params[0] == null) && !params[0].getReadings().isEmpty()
						&&
						httpStatusCode != BAD_REQUEST && httpStatusCode != GOOD_SEND
						)					dataSaver.saveArray(payload.getReadings());


//				synced = true;// Release base meter to make the Not.
				
				
				
				//AFTER UNBINDING, set a notification to send. Easy to get rid of, and opens the activity
				BaseService.createNotification(context);
				try{
					FragmentWebActivity.createWeb();
				} catch (Exception ex) {
					Logging.Error("BaseService.onPostExecute()", "   EXCEPTION: ", ex);
				}

				
				return null;
			} catch (Exception ex) {
				Logging.Error("InternetSync.SyncDataTask.doInBackground()", "EXCEPTION:  ", ex);
				synced = true;  //  PDS:  2015-04-29:   Is this correct?  Do we want synced if an exception.
			}

			if (Debug.DEBUG) {
				Logging.Info("InternetSyncing.syncdataTask", "Coonection bad");
			}
			if(! (params[0] == null) && !params[0].getReadings().isEmpty())
			dataSaver.saveArray(params[0].getReadings());
//			synced = true;// Release base meter to make the Notification
			
			
			
			//AFTER UNBINDING, set a notification to send. Easy to get rid of, and opens the activity
			BaseService.createNotification(context);
			try{
				FragmentWebActivity.createWeb();
			} catch (Exception ex) {
				Logging.Error("BaseService.onPostExecute()", "   EXCEPTION: ", ex);
			}
			
			
			
			
			return null;
		}

	}

	/**
	 * This call sets up the information to verify, with the portal then sends
	 * it to a {@link AsyncTask} class {@link TestPasswordTask}
	 */
	public void TestPassword() {
		Logging.Info("InternetSync.testPassword", "Start Test password");

		InternetPayload payload = new InternetPayload(dataSaver.readSet(DataSaver.userName),
						dataSaver.readSet(DataSaver.Password), new ArrayList<Reading>());

		if (Debug.DEBUG)
			Logging.Info("InternetSync.testPassword", "email is " + dataSaver.readSet(DataSaver.userName));

		TestPasswordTask task = new TestPasswordTask();
		task.execute(new InternetPayload[]{payload});
	}

	/**
	 * Asynchronous task designed to verify the users credentials will produces
	 * a Alert dialog afterwords
	 */
	private class TestPasswordTask extends AsyncTask<InternetPayload, Void, String> {

		@Override
		protected String doInBackground(InternetPayload... params) {
			Logging.Info("InternetSyncing.TestPasswordTask.doInBackground()", " start task");

			Logging.Info("InternetSyncing.TestPasswordTask.doInBackground()", "Start try");

			try {
				
				// Set up info to send
				InternetPayload payload = params[0];
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
				nameValuePairs.add(new BasicNameValuePair("payload", payload.toString() + "]}"));

				// Set up send location and send the info
				DefaultHttpClient client = new DefaultHttpClient();
				HttpPost post = new HttpPost(POST_URL);
				post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				// read response
				HttpResponse response = client.execute(post);
				StatusLine status = response.getStatusLine();
				int http_status_code = status.getStatusCode();

				Logging.Info("Test password task", "httpStatus code" + http_status_code);

				// Read Status codes
				switch (http_status_code) {
					case (GOOD_SEND):
							Logging.Debug("InternetSyncing.TestPasswordTask.doInBackground()", "Connection Good");
						return TP_GOOD_STRING;
						
					case (BAD_REQUEST):
							Logging.Debug("InternetSyncing.TestPasswordTask.doInBackground()", "bad request");
						return TP_BAD_REQUEST_STRING;
						
					case (BAD_AUTH):
							Logging.Debug("InternetSyncing.TestPasswordTask.doInBackground()", "bad authentication");
						return TP_BAD_AUTHENTICATION_STRING;
						
					case (SERVER_ERROR):
							Logging.Debug("InternetSyncing.TestPasswordTask.doInBackground()", "server error");
						return TP_SERVER_ERROR_STRING;
						
					case (METHOD_NOT_IMPLEMENTED):
							Logging.Debug("InternetSyncing.TestPasswordTask.doInBackground()", "method not implemented");
						return TP_METHOD_NOT_IMPLEMENTED_STRING;
						
					default:
							Logging.Debug("InternetSyncing.TestPasswordTask.doInBackground()", "default");
						return TP_UNKNOWN_INTERNET_STRING + http_status_code;
				}

			} catch (Exception ex) {
				Logging.Error("InternetSyncing.TestPasswordTask.doInBackground()", "hit error", ex);
			}

			if (Debug.DEBUG)
				Logging.Info("InternetSyncing.TestPasswordTask.doInBackground()", "Connection bad");
			return TP_NO_INTERNET_STRING;
		}

		@Override
		protected void onPostExecute(String Response) {

			// Build Verify alert dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setMessage(Response).setTitle("Connecting").setPositiveButton("OK", new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {

				}
			});
			AlertDialog dialog = builder.create();
			dialog.show();

		}

	}

}
