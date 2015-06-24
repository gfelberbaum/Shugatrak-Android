package com.applivate.shugatrak2;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.util.EncodingUtils;


import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
/****************************************************************************
 * FRAGMENT WEB ACTIVITY
 * 
 * <h3>Purpose of Activity:</h3>
 * 		&nbsp; Control the 
 * 		Portal view Specifically 
 * 
 * <h3>Update notes:</h3>
 * 		v0.1.5:
 * 		&nbsp; Final beta FragmentWebActivity ready
 * 
 * <h3>Known errors:</h3>
 * 		V0.1.5:
 * 		&nbsp; None Known
 * 
 * @category ShugaTrak
 * @version  V0.1.5: Ryan
 * @author  Current: Ryan Hirschthal Original:Ryan Hirschthal &nbsp;
 * {@code (c) 2014 Ryan Hirschthal, Advanced Decisions. (rhirschthal @ advanceddecisions . com)
 * All rights reserved}
 * @see {@link TopLevelActivity}
 *****************************************************************************/

public class FragmentWebActivity extends Fragment {

	/**
	 * instance of the web view
	 */
	private static WebView website;
	/**
	 * Instance of the swipe view
	 */
	private static SwipeRefreshLayout swipe;
	
	private static DataSaver data;
	/**
	 * Website to post to
	 */
	public static final String LOGIN_URL ="https://www.shugatrak.com/login/";
	


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
	@SuppressLint("SetJavaScriptEnabled")
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = (View) inflater.inflate(
				R.layout.activity_fragment_web, container, false);

		data = new DataSaver(getActivity().getApplicationContext());

		
		//Set swipe information
        swipe = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
        swipe.setOnRefreshListener(new N());
        swipe.setColorScheme(android.R.color.holo_blue_bright, 
        		android.R.color.holo_blue_dark,
        		android.R.color.holo_blue_light,
        		android.R.color.holo_blue_dark);
    
		//set up the webview
		website = (WebView) rootView.findViewById(R.id.Web);
		website.setWebViewClient(new WebViewClient());
		website.getSettings().setJavaScriptEnabled(true);





		return rootView;
	}
	/**
	 * called when the user swipes down on the screen
	 *
	 */
	private class N implements OnRefreshListener{
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onRefresh() {
			createWeb();
			//timer to kill the refresh look
			new Handler().postDelayed(new Runnable() {
				@Override public void run() {
					swipe.setRefreshing(false);
				}
			}, 5000);
		}
		
	}
	


	/**
	 * {@inheritDoc}
	 */
	public void onResume(){
		super.onResume();
		createWeb();
	}
	/**
	 * Will create/refresh the {@link WebView}
	 */
	public static void createWeb(){
		byte[] post ={};
			try {
				post = EncodingUtils.getBytes("uc=m&email="+URLEncoder.encode(data.readSet(DataSaver.userName),"UTF-8")+"&password="+URLEncoder.encode(data.readSet(DataSaver.Password), "UTF-8"), "Ascii85");
				Logging.Debug("FragmentWebActivity.createWeb", new String(post));
				website.postUrl(LOGIN_URL, post);
			} catch (UnsupportedEncodingException ex) {
				//			website.loadUrl(HOME_URL);
				Logging.Error("Web","bad send up",ex);
			}
	}
	

}
