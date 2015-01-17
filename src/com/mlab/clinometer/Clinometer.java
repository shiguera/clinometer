package com.mlab.clinometer;

public interface Clinometer {
	boolean start();
	void stop();
	boolean isRunning();
	boolean isEnabled();
	ClinometerStore getStore();
}
