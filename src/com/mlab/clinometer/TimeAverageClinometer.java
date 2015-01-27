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

	protected Context context;
	protected ClinometerStore store;
	protected SensorManager sensorManager;
	protected Sensor gameSensor;
	protected int cicleCounter;
	protected boolean isEnabled, isRunning;
	
	protected Timer mainTimer; 
	protected TSerie tmpValues;
	protected long interval;
	
	public TimeAverageClinometer(Context context, ClinometerStore store, long interval) {
		this.context = context;

		this.store = store;
		
		this.interval = interval;
		
		isRunning = false;
		
		isEnabled = initSensor();
		LOG.info("TimeAverageClinometer() isEnabled=" + isEnabled);
		if(!isEnabled) {
			String msg = "ERROR: Can't initialize clinometer sensor";
			Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
			LOG.error("TimeAverageClinometer(): " + msg);		
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
		tmpValues = new TSerie();
		if (gameSensor != null) {
			LOG.debug("TimeAverageClinometer.start() : OK");			
			isRunning = sensorManager.registerListener(this, gameSensor,
					SensorManager.SENSOR_DELAY_GAME);
			if(isRunning) {
				mainTimer = new Timer();
				mainTimer.scheduleAtFixedRate(new MainTimerTask(), 0, interval);
			}
		} else {
			LOG.error("TimeAverageClinometer.start() : ERROR, gameSensor == null");
			isRunning = false;			
		}
		return isRunning;
	}

	@Override
	public void stop() {
		LOG.debug("TimeAverageClinometer.stop()");
		LOG.debug("cicleCounter= " + cicleCounter);
		if (sensorManager != null) {
			sensorManager.unregisterListener(this);
		}
		if(mainTimer != null) {
			mainTimer.cancel();
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
		long time = System.currentTimeMillis();
		float[] rotmat = new float[16];
		float[] rotmat2 = new float[16];
		float[] orientation = new float[3];
		
		SensorManager.getRotationMatrixFromVector(rotmat, event.values);
		SensorManager.remapCoordinateSystem(rotmat, SensorManager.AXIS_X,
				SensorManager.AXIS_Z, rotmat2);
		SensorManager.getOrientation(rotmat2, orientation);
		double[] vals = new double[] { (double)orientation[0], (double)orientation[1], (double)orientation[2]};
		addLastValues(time, vals);
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		LOG.debug("onAccuracyChanged()");
	}

	private boolean isLocked = false;
	private synchronized void addLastValues(long t, double[] vals) {
		if(!isLocked) {
			tmpValues.add(t,vals);
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
				store.add(avgt, avrgvalues);
				tmpValues = new TSerie();
			}
			unlock();
		}
		
	}

}
