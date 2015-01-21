package com.mlab.clinometer;

import org.apache.log4j.Logger;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Toast;

public class BasicClinometer implements Clinometer, SensorEventListener {
	private final Logger LOG  = Logger.getLogger(BasicClinometer.class);

	protected Context context;
	protected ClinometerStore store;
	protected SensorManager sensorManager;
	protected Sensor gameSensor;
	protected int cicleCounter;
	protected boolean isEnabled, isRunning;
	
	public BasicClinometer(Context context, ClinometerStore store) {
		this.context = context;

		this.store = store;
		
		isRunning = false;
		
		isEnabled = initSensor();
		LOG.info("BasicClinometer() isEnabled=" + isEnabled);
		if(!isEnabled) {
			String msg = "ERROR: Can't initialize clinometer sensor";
			Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
			LOG.error("BasicClinometer(): " + msg);		
		}

	}
	private boolean initSensor() {
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		gameSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
		if (gameSensor == null) {
			
			return false;
		}
		return true;
	}
	@Override
	public boolean start() {
		cicleCounter = 0;
		if(!isRunning) {
			if (gameSensor != null) {
				LOG.debug("BasicClinometer.start() : OK");			
				isRunning = sensorManager.registerListener(this, gameSensor,
						SensorManager.SENSOR_DELAY_GAME);
			} else {
				LOG.error("BasicClinometer.start() : ERROR, gameSensor == null");
				isRunning = false;			
			}			
		}
		store.empty();
		return isRunning;
	}

	@Override
	public void stop() {
		LOG.debug("BasicClinometer.stop()");
		LOG.debug("cicleCounter= " + cicleCounter);
		if (sensorManager != null) {
			sensorManager.unregisterListener(this);
		}
		isRunning = false;
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

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
		float[] rotmat = new float[16];
		float[] rotmat2 = new float[16];
		float[] orientation = new float[3];
		
		SensorManager.getRotationMatrixFromVector(rotmat, event.values);
		SensorManager.remapCoordinateSystem(rotmat, SensorManager.AXIS_X,
				SensorManager.AXIS_Z, rotmat2);
		SensorManager.getOrientation(rotmat2, orientation);
		double[] vals = new double[] { (double)orientation[0], (double)orientation[1], (double)orientation[2]};
		store.add(event.timestamp, vals);
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		LOG.debug("onAccuracyChanged()");
	}

}
