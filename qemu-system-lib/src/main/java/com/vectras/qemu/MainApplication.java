package com.vectras.qemu;

import android.app.Application;

public class MainApplication extends Application {

    @Override
	public void onCreate() {
        super.onCreate();
		try {
			Class.forName("android.os.AsyncTask");
		} catch (Throwable ignore) {
			// ignored
		}

		

	}

}
