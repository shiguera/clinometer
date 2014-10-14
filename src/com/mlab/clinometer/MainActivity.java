package com.mlab.clinometer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity implements Observer, 
		SensorEventListener {
	/**
	 *  Tiempo en milisegundos para el mainTimer
	 */
	public static final int TIME_LAPSE = 1000; 

	/**
	 * Número de milisegundos de cada grabación de ficheros
	 */
	private static final long MAX_RECORDING_TIME = 1200000; 
	// Status
	private enum Status {
		FIXING_GPS, GPS_FIXED, RECORDING, SAVING
	};

	private Status status;
	Date startDate;

	Timer mainTimer; // A intervalos definidos en TIME_LAPSE proporciona
						// actualizaciones de pantalla
	int cicleCounter;
	// GpsModel
	GpsModel gpsModel;
	
	// Components: SensorManager
	SensorManager sensorManager;
	Sensor gameSensor;
	float[] orientation = new float[3];
	List<Float[]> lastOrientationValues;
	boolean isAddToListEnabled = true;
	float escoraZero = 0.0f, cabeceoZero = 0.0f;

	TextView tv;
	Button btnFixEscora, btnFixCabeceo;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		tv = (TextView) this.findViewById(R.id.lbl);
		btnFixEscora = (Button) this.findViewById(R.id.btn1);
		btnFixEscora.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				escoraZero = averageOrientation()[2];
			}
		});
		btnFixCabeceo = (Button) this.findViewById(R.id.btn2);
		btnFixCabeceo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				cabeceoZero = averageOrientation()[1];
			}
		});
		initSensors();

		status = Status.FIXING_GPS;

		// GpsModel
		gpsModel = new GpsModel(this);
		
		// Inicializar Timer
		cicleCounter = 0;
		mainTimer = new Timer();
		mainTimer.scheduleAtFixedRate(new MainTimerTask(), 0, TIME_LAPSE);

	}

	private void initSensors() {
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		gameSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
	}

	@Override
	protected void onResume() {
		sensorManager.registerListener(this, gameSensor,
				SensorManager.SENSOR_DELAY_GAME);
		lastOrientationValues = new ArrayList<Float[]>();
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause() {
		if (mainTimer != null) {
			mainTimer.cancel();
		}
		if (sensorManager != null) {
			sensorManager.unregisterListener(this);
		}
		super.onPause();
	}

	// UpdateUI
	private void updateUI() {
		float[] angles = averageOrientation();
		tv.setText(String.format("Ciclos: %s\n Azimuth: %10.1f\nCabeceo: %10.1f\nEscora: %10.1f",
			Integer.toString(cicleCounter),
			Math.toDegrees((double) angles[0]),
			Math.toDegrees((double) angles[1]),
			Math.toDegrees((double) angles[2]))
		);
	}

	private void saveAndResume() {

	}

	class MainTimerTask extends TimerTask {
		@Override
		public void run() {
			cicleCounter++;
			MainTimerTaskOnUIThread task = new MainTimerTaskOnUIThread();
			runOnUiThread(task);
			// Comprobar el límite de tiempo de grabación
			if (status == Status.RECORDING) {
				Date now = new Date();
				long recordingTime = now.getTime() - startDate.getTime();
				if (recordingTime >= MAX_RECORDING_TIME) {
					saveAndResume();
				}
				Float[] avrg = new Float[3];

			}
		}
	}

	private float[] averageOrientation() {
		this.isAddToListEnabled = false;
		float[] sums = new float[] { 0.0f, 0.0f, 0.0f };
		float numelements = (float) lastOrientationValues.size();
		for (Float[] f : lastOrientationValues) {
			sums[0] += f[0];
			sums[1] += f[1];
			sums[2] += f[2];
		}
		sums[0] = sums[0] / numelements;
		sums[1] = sums[1] / numelements;
		sums[2] = sums[2] / numelements;
		this.lastOrientationValues.clear();
		this.isAddToListEnabled = true;
		return sums;
	}

	class MainTimerTaskOnUIThread implements Runnable {
		@Override
		public void run() {
			updateUI();
		}
	}

	// Sensores
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_GAME_ROTATION_VECTOR:
			float[] rotmat = new float[16];
			float[] rotmat2 = new float[16];
			SensorManager.getRotationMatrixFromVector(rotmat, event.values);
			SensorManager.remapCoordinateSystem(rotmat, SensorManager.AXIS_X,
					SensorManager.AXIS_Z, rotmat2);
			SensorManager.getOrientation(rotmat2, orientation);
			addToListOfLastOrientationValues(orientation);
			break;
		}

	}

	synchronized private void addToListOfLastOrientationValues(
			float[] orientationValues) {
		if (this.isAddToListEnabled) {
			this.isAddToListEnabled = false;
			this.lastOrientationValues.add(new Float[] { orientationValues[0],
					orientationValues[1], orientationValues[2] });
			this.isAddToListEnabled = true;
		}
	}

	// Interface Observer
	@Override
	public Observable getObservable() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void update() {
		// TODO Auto-generated method stub
	}
	@Override
	public boolean addComponent(Observer o) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean removeComponent(Observer o) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public Observer getComponent(int index) {
		// TODO Auto-generated method stub
		return null;
	}

}
