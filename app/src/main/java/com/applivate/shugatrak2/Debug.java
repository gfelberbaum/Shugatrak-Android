package com.applivate.shugatrak2;

public class Debug {
	public static final boolean DEBUG = true;
	public static final boolean VERBOSE = true;
	public static final boolean KC_DEBUG = true; //This boolean is made so that we can leave out most of the logging calls, but can still have the beta features for KC work fine
	public static final int VERSION_CODE= 345;  //   Build number must be set in: strings.xml, AndroidManifest, Debug.java
	public static final String VERSION_NUMBER = "1.3.1";
	public static final boolean GRAB_ALL = false;
	public static final String COMMENT = "A shiny new Australia";
}