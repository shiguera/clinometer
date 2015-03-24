package com.mlab.clinometer;

public interface Clinometer {
	
	// Status
	/**
	 * Indica si existe clinómetro en el dispositivo
	 * 
	 * @return
	 */
	boolean isEnabled();
	/**
	 * Indica si el clinómetro está leyendo, o sea,
	 * si está registrado como listener del sensor
	 * @return
	 */
	boolean isReading();
	/**
	 * Indica si se están grabando los datos en el ClinometerStore
	 * 
	 * @return
	 */
	boolean isRecording();
	
	// Management
	/**
	 * Registra el clinómetro como listener del sensor.
	 * Pone en true la variable isReading
	 * @return
	 */
	boolean startReading();
	/**
	 * Des-registra el clinómetro como listener del sensor.
	 * Pone en false la variable isReading
	 */
	void stopReading();
	
	// Store
	/**
	 * Clase encargada de guardar los datos leidos del clinómetro
	 * @return
	 */
	ClinometerStore getStore();
	/**
	 * Comienza el almacenamiento de los valores del clinómetro en 
	 * el ClinometerDataStore.
	 * Pone a true la variable isRecording.
	 * @return
	 */
	boolean startRecording();
	/**
	 * Detiene el almacenamiento de datos en el ClinometerDataStore.
	 * Pone a false el valor de isRecording
	 */
	void stopRecording();

	// Calibrate
	void setFixEscora(double escora);
	void setFixCabeceo(double escora);
	void setFixAzimuth(double escora);
	double getFixEscora();
	double getFixCabeceo();
	double getFixAzimuth();
	
	// getters
	public double[] getLastRawValues();
}
