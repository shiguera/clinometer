package com.mlab.clinometer;

import java.io.File;

import android.app.Application;
import android.content.Context;

public class App extends Application {
	
	private static final String APP_DIRECTORY_NAME = "Clinometer";
	protected static File applicationDirectory;
	protected static String logFileName = "clinometer.log";

	private static Context appContext;
	
	App() {
		appContext = getApplicationContext();
	}
	
	public static Context getContext() {
		return appContext;
	}
	
	public static String getAppDirectoryName() {
		return APP_DIRECTORY_NAME;
	}

	
	public static String getLogFileName() {
		return logFileName;
	}

	public static void setLogFileName(String logFileName) {
		App.logFileName = logFileName;
	}


	public static File getApplicationDirectory() {
		return applicationDirectory;
	}

	public static void setApplicationDirectory(File applicationDirectory) {
		App.applicationDirectory = applicationDirectory;
	}
	
	
}
