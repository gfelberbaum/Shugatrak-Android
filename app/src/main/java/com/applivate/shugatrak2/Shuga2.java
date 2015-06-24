package com.applivate.shugatrak2;

import org.acra.*;
import org.acra.annotation.*;


import android.app.Application;

@ReportsCrashes(formKey = "", // will not be used
				formUri = "https://www.shugatrak.com/stcrashes/creport/",
				mode = ReportingInteractionMode.TOAST,
				resToastText = R.string.diagnostics_sent_to_shugatrak)

public class Shuga2 extends Application {
	@Override
	public void onCreate() {
		// The following line triggers the initialization of ACRA
		ACRA.init(this);
		super.onCreate();
	}

}
