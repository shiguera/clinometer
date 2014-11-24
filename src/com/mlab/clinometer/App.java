package com.mlab.clinometer;

import java.io.File;

import android.app.Application;

public class App extends Application {
	
	private static final String APP_DIRECTORY_NAME = "Clinometer";
	protected static File applicationDirectory;
	protected static String logFileName = "clinometer.log";

	
	
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
