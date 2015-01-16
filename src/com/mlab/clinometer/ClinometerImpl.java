package com.mlab.clinometer;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Implementación de un TimedClinometer. 
 * Eje x= rumbo, ejey=cabeceo, ejez=escora
 * El dispositivo permite fijar un intervalo de tiempo
 * y almacena la media de los valores leídos en cada intervalo de tiempo.
 * 
 * @author shiguera
 *
 */
public class ClinometerImpl implements TimedClinometer, SensorEventListener {
	private final Logger LOG  = Logger.getLogger(ClinometerImpl.class);

	/**
	 *  Si es true, en cada ciclo se borran los valores 
	 * 	de rawValues para no colmar la memoria
	 */
	private boolean deleteRawValues = true;
	List<double[]> rawValues;
	List<double[]> lastValues;
	List<double[]> filteredValues;
	
	Context context;
	
	SensorManager sensorManager;
	Sensor gameSensor;
	boolean isRunning;
	long startTime;
	
	/**
	 * Intervalo en milisegundos entre dos almacenamientos sucesivos de datos.
	 */
	private long timeLapse = 500; 
	/**
	 *  
	 */
	Timer mainTimer; 
	/**
	 *  Tiempo en milisegundos para el mainTimer
	 */
	int cicleCounter;
	
	public ClinometerImpl(Context context) {		
		this.context = context;
		
		boolean result = initSensor();
		if(!result) {
			LOG.error("ClinometerImpl() ERROR initializing sensor");
		}
		
		initArrays();
				
	}
	private boolean initSensor() {
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		gameSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
		if (gameSensor == null) {
			return false;
		}
		return true;
	}
	private void initArrays() {
		rawValues = new ArrayList<double[]>();
		lastValues = new ArrayList<double[]>();
		filteredValues = new ArrayList<double[]>();
	}
	
	// Clinometer
	@Override
	public boolean start() {
		initArrays();
		if (gameSensor != null) {
			LOG.debug("ClinometerImpl.start() : OK");			
			isRunning = sensorManager.registerListener(this, gameSensor,
					SensorManager.SENSOR_DELAY_GAME);
			if(isRunning) {
				mainTimer = new Timer();
				mainTimer.scheduleAtFixedRate(new MainTimerTask(), 0, timeLapse);
			}
		} else {
			LOG.error("ClinometerImpl.start() : ERROR, gameSensor == null");
			isRunning = false;			
		}
		return isRunning;
	}

	@Override
	public boolean stop() {
		LOG.debug("ClinometerImpl.stop()");
		LOG.debug("cicleCounter= " + cicleCounter);
		LOG.debug("rawValues.size()= " + rawValues.size());
		LOG.debug("filteredValues.size()= " + filteredValues.size());
		if (sensorManager != null) {
			sensorManager.unregisterListener(this);
		}
		if(mainTimer != null) {
			mainTimer.cancel();
		}
		isRunning = false;
		return true;
	}

	@Override
	public double[] getLastFilteredValue() {
		if(filteredValues.size() > 0) {
			return filteredValues.get(filteredValues.size()-1);
		}
		return null;
	}

	@Override
	public List<double[]> getFilteredValues() {
		return filteredValues;
	}
	@Override
	public boolean isRunning() {
		return isRunning;
	}
	
	// SensorEventListener
	float[] orientation = new float[3];
	@Override
	public void onSensorChanged(SensorEvent event) {
		//LOG.debug("ClinometerImpl.onSensorChanged() " + event.values);
		switch (event.sensor.getType()) {
		case Sensor.TYPE_GAME_ROTATION_VECTOR:
			float[] rotmat = new float[16];
			float[] rotmat2 = new float[16];
			SensorManager.getRotationMatrixFromVector(rotmat, event.values);
			SensorManager.remapCoordinateSystem(rotmat, SensorManager.AXIS_X,
					SensorManager.AXIS_Z, rotmat2);
			SensorManager.getOrientation(rotmat2, orientation);
			double[] vals = new double[] { (double)orientation[0], (double)orientation[1], (double)orientation[2]};
			rawValues.add(vals);
			addLastValues(vals);
			break;
		}
	}
	private boolean isLocked = false;
	private synchronized void addLastValues(double[] vals) {
		if(!isLocked) {
			lastValues.add(vals);
		}
	}
	private synchronized void lock() {
		isLocked = true;
	}
	private synchronized void unlock() {
		isLocked = false;
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		LOG.debug("ClinometerImpl.onAccuracyChanged()");		
	}

	class MainTimerTask extends TimerTask {

		@Override
		public void run() {
			lock();
			cicleCounter++;
			double size = (double)lastValues.size();
			if(size>0) {
				double[] sums = new double[] {0.0, 0.0, 0.0};
				for(double[] d: lastValues) {
					sums[0] += d[0];
					sums[1] += d[1];
					sums[2] += d[2];
				}
				double[] avrg = new double[]{sums[0]/size, sums[1]/size, sums[2]/size};
				filteredValues.add(avrg);
				lastValues.clear();
				if(deleteRawValues) {
					rawValues.clear();
				}
			}
			unlock();
		}
		
	}

	// TimedClinometer
	@Override
	public long getTimeLapse() {
		return timeLapse;
	}
	@Override
	public void setTimeLapse(long t) {
		timeLapse = t;
	}
	@Override
	public void setDeleteRawValues(boolean delete) {
		deleteRawValues = delete;
	}
	@Override
	public boolean getDeleteRawValues() {
		return deleteRawValues;
	}
	@Override
	public double[] getLastRawValue() {
		if(rawValues != null && rawValues.size()>0) {
			return rawValues.get(rawValues.size()-1);
		}
		return null;
	}
	@Override
	public List<double[]> getRawValues() {
		return rawValues;
	}
	@Override
	public int getRawValuesCount() {
		return rawValues.size();
	}
	@Override
	public int getFilteredValuesCount() {
		return filteredValues.size();
	}
}
