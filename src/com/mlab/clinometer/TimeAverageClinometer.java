package com.mlab.clinometer;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Toast;

import com.mlab.gpx.impl.tserie.TSerie;

public class TimeAverageClinometer implements Clinometer, SensorEventListener {
	private final Logger LOG  = Logger.getLogger(TimeAverageClinometer.class);
	private final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_GAME;
	
	protected Context context;
	protected ClinometerStore store;
	protected SensorManager sensorManager;
	protected Sensor gameSensor;

	/**
	 * Variables de estado del clinómetro. 
	 * (Ver descripción en el interface)
	 */
	protected boolean isEnabled, isReading, isRecording;
	
	protected Timer mainTimer; 
	/**
	 * Cuenta los ciclos del mainTimer desde el 
	 * último inicio
	 */
	protected int cicleCounter;
	protected TSerie tmpValues;
	/**
	 * Intervalo de tiempo entre lecturas almacenadas del clinómetro.
	 * El valor de inclinaciones almacenado en cada intervalo será la
	 * media de los valores leidos en ese periodo. 
	 */
	protected long saveInterval;

	double fixEscora, fixCabeceo, fixAzimuth;
	/**
	 * Guarda los últimos valores leídos del sensor,
	 * afectados de los valores fix, en radianes
	 */
	double[] lastRawValues;
	
	public TimeAverageClinometer(Context context, ClinometerStore store, long interval) {
		this.context = context;

		this.store = store;
		
		this.saveInterval = interval;
		fixAzimuth = 0.0;
		fixCabeceo = 0.0;
		fixEscora = 0.0;
		
		isRecording = false;
		isReading = false;
		
		isEnabled = initSensor();
		LOG.info("TimeAverageClinometer() isEnabled=" + isEnabled);
		if(!isEnabled) {
			String msg = "ERROR: Can't initialize clinometer sensor";
			Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
			LOG.error("TimeAverageClinometer(): " + msg);		
			return;
		}
		
		isReading = startReading();
		if(!isReading) {
			String msg = "ERROR: Can't start reading ";
			Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
			LOG.error("TimeAverageClinometer(): " + msg);		
			return;
		}
	}
	private boolean initSensor() {
		
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		if(sensorManager == null) {
			LOG.error("initSensor() ERROR: SensorManager == null");
			return false;
		}
		
		gameSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
		if (gameSensor == null) {			
			LOG.error("initSensor() ERROR: GameSensor == null");
			return false;
		}

		return true;		
	}
	@Override
	public boolean isEnabled() {
		return isEnabled;
	}
	@Override
	public boolean isReading() {
		return isReading;
	};
	
	@Override
	public boolean startReading() {
		if(isReading) {
			return true;
		}
		tmpValues = new TSerie();
		if (gameSensor != null ) {
			boolean result = sensorManager.registerListener(this, gameSensor,SENSOR_DELAY);
			if (!result) {
				LOG.error("TimeAverageClinometer.start() : ERROR can't registerListener");
				return false;
			}
			startMainTimer();	
			isReading = true;
			return isReading;
		} else {
			LOG.error("TimeAverageClinometer.start() : ERROR, gameSensor == null or can't registerListener");
			return false;
		}
	}
	private void startMainTimer() {
	
		mainTimer = new Timer();
		cicleCounter = 0;
		mainTimer.scheduleAtFixedRate(new MainTimerTask(), 0, saveInterval);		

	}
	@Override
	public void stopReading() {
		LOG.debug("TimeAverageClinometer.stop()");
		LOG.debug("cicleCounter= " + cicleCounter);
		if (isRecording) {
			stopRecording();
		}
		if (sensorManager != null) {
			sensorManager.unregisterListener(this);
		}
		if(mainTimer != null) {
			mainTimer.cancel();
		}
		isRecording = false;
		isReading = false;
	}

	// Recording state
	@Override
	public boolean isRecording() {
		return isRecording;
	}	
	@Override
	public boolean startRecording() {
		if(isEnabled) {
			isRecording = true;
			return isRecording;
		} else {
			LOG.error("startRecording() ERROR: can't start recording");
			return false;
		}
	};
	@Override
	public void stopRecording() {
		isRecording = false;
	};

	@Override
	public ClinometerStore getStore() {
		return store;
	}
	
	// SensorEventListener
	@Override
	public void onSensorChanged(SensorEvent event) {
		calcAndStoreOrientation(event);
	}
	private synchronized void calcAndStoreOrientation(SensorEvent event) {
		long time = System.currentTimeMillis();
		float[] rotmat = new float[16];
		float[] rotmat2 = new float[16];
		float[] orientation = new float[3];
		
		SensorManager.getRotationMatrixFromVector(rotmat, event.values);
		//SensorManager.getOrientation(rotmat, orientation);
		//LOG.debug("Default: " + Math.toDegrees(orientation[0]) + " " + Math.toDegrees(orientation[1]) + " " + Math.toDegrees(orientation[2]));
		SensorManager.remapCoordinateSystem(rotmat, SensorManager.AXIS_X,
				SensorManager.AXIS_Z, rotmat2);
		SensorManager.getOrientation(rotmat2, orientation);
		//LOG.debug("Remaped: " + Math.toDegrees(orientation[0]) + " " + Math.toDegrees(orientation[1]) + " " + Math.toDegrees(orientation[2]));
		lastRawValues = new double[] { (double)orientation[0],(double)orientation[1], (double)orientation[2]};
		double[] vals = new double[] { (double)orientation[0] - fixAzimuth, 
				(double)orientation[1] - fixCabeceo, (double)orientation[2] - fixEscora};
		addLastValues(time, vals);
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		LOG.debug("onAccuracyChanged()");
	}

	private boolean isLocked = false;
	private synchronized void addLastValues(long t, double[] vals) {
		if(!isLocked) {
			lock();
			tmpValues.add(t,vals);
			unlock();
		}
	}
	private synchronized void lock() {
		isLocked = true;
	}
	private synchronized void unlock() {
		isLocked = false;
	}
	class MainTimerTask extends TimerTask {

		@Override
		public void run() {
			lock();
			cicleCounter++;
			double size = (double)tmpValues.size();
			if(size>0) {
				long sumt = 0l;
				double[] sums = new double[] {0.0, 0.0, 0.0};
				for(int i =0; i<size; i++) {
					sumt += tmpValues.getTime(i);
					sums[0] += tmpValues.getValues(i)[0];
					sums[1] += tmpValues.getValues(i)[1];
					sums[2] += tmpValues.getValues(i)[2];
				}
				long avgt = sumt / (long)size;
				double[] avrgvalues = new double[]{sums[0]/size, sums[1]/size, sums[2]/size};
				if (isRecording) {
					store.add(avgt, avrgvalues);					
				}
				tmpValues = new TSerie();
			}
			unlock();
		}
		
	}
	@Override
	public double getFixEscora() {
		return fixEscora;
	}
	@Override
	public void setFixEscora(double fixEscora) {
		this.fixEscora = fixEscora;
	}
	@Override
	public double getFixCabeceo() {
		return fixCabeceo;
	}
	@Override
	public void setFixCabeceo(double fixCabeceo) {
		this.fixCabeceo = fixCabeceo;
	}
	@Override
	public double getFixAzimuth() {
		return fixAzimuth;
	}
	@Override
	public void setFixAzimuth(double fixAzimuth) {
		this.fixAzimuth = fixAzimuth;
	}
	@Override
	public double[] getLastRawValues() {
		return lastRawValues;
	}

}
