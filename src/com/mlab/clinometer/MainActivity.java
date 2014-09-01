package com.mlab.clinometer;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity implements SensorEventListener{
    public static final int TIME_LAPSE = 1000; // Tiempo en milisegundos para el timer principal
    private static final long MAX_RECORDING_TIME = 1200000; // Número de milisegundos de cada grabación de ficheros 

	// Status
	private enum Status {FIXING_GPS, GPS_FIXED, RECORDING, SAVING};
	private Status status;
	Date startDate;

	Timer mainTimer; // A intervalos definidos en TIME_LAPSE proporciona actualizaciones de pantalla

	// Components: SensorManager
	SensorManager sensorManager;
	Sensor accelerometerSensor, magneticSensor, linearAccSensor, pressureSensor;
	double pressure = 0.0;

	TextView tv;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		tv = (TextView)this.findViewById(R.id.lbl);
		
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

     
        // Inicializar Timer
        mainTimer = new Timer();
        mainTimer.scheduleAtFixedRate(new MainTimerTask(), 0, TIME_LAPSE);

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
	// UpdateUI
	private void updateUI() {
	
	}
	private void saveAndResume() {
		
	}
	// Sensores
		float[] localGravity = new float[3];
		float[] geomagneticVector = new float[3];
		float[] localAcceleration = new float[3];
		float[] rotationMatrix = new float[9];
		float[] inclinationMatrix = new float[9];
		float[] orientation = new float[3];
		//float[] oldOreintation = new float[3];
		
		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void onSensorChanged(SensorEvent event) {
			switch(event.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				System.arraycopy(event.values, 0, localGravity, 0, 3);
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				System.arraycopy(event.values, 0, geomagneticVector, 0, 3);			
				break;
			case Sensor.TYPE_LINEAR_ACCELERATION:
				System.arraycopy(event.values, 0, localAcceleration, 0, 3);			
				break;
			case Sensor.TYPE_PRESSURE:
				pressure = event.values[0];
			}
			
	        boolean success = SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, 
	        		localGravity, geomagneticVector);
	        if(success) {
	            SensorManager.getOrientation(rotationMatrix, orientation);
	        }		
			
		}
		private float[] calculateGlobalAcceleration() {
			float[] rtrasp = traspose(rotationMatrix);
			float[] globalAcceleration = this.vectorByMatrixMultiplication(localAcceleration, rtrasp);
			return globalAcceleration;
		}
		/**
		 * Multiplica un vector de dimensión 3 por una matriz 3x3 y devuelve el vector resultado
		 * @param vector vector fila de tres dimensiones [v0 v1 v2]
		 * @param matrix matriz de 3x3
		 * @return vector fila de tres dimensiones [r0 r1 r2] resultado de multiplicar
		 * el vector original por la matriz
		 */
		private float[] vectorByMatrixMultiplication(float[] vector, float[] matrix) {
			//
			float[] result = new float[3];
			// Comprobar dimension del vector y matriz de entrada
			if (vector.length != 3 || matrix.length != 9) {
				return null;
			}
			// Calcular elproducto vectorxmatriz
			result[0] = vector[0]*matrix[0]+vector[1]*matrix[3]+vector[2]*matrix[6];
			result[1] = vector[0]*matrix[1]+vector[1]*matrix[4]+vector[2]*matrix[7];
			result[2] = vector[0]*matrix[2]+vector[1]*matrix[5]+vector[2]*matrix[8];
			
			// Devolver el resultado
			return result;
		}
		/**
		 * Traspone una matriz 3x3 recibida en forma de Array float  
		 * @param originalMatrix [a00 a01 a02 a10 a11 a12 a20 a21 a22]
		 * @return Trasposed matrix [a00 a10 a20 a01 a11 a21 a02 a12 a22]
		 */
		private float[] traspose(float[] originalMatrix) {
			float[] result = new float[9];
			
			// Comprobar dimensión matriz original
			if(originalMatrix.length!=9) {
				return null;
			}
			// Trasponer
			result[0]=originalMatrix[0];
			result[1]=originalMatrix[3];
			result[2]=originalMatrix[6];
			result[3]=originalMatrix[1];
			result[4]=originalMatrix[4];
			result[5]=originalMatrix[7];
			result[6]=originalMatrix[2];
			result[7]=originalMatrix[5];
			result[8]=originalMatrix[8];
			
			// Devolver el resultado
			return result;
		}
		class MainTimerTask extends TimerTask {
			@Override
			public void run() {
				MainTimerTaskOnUIThread task = new MainTimerTaskOnUIThread();
				runOnUiThread(task);
				// Comprobar el límite de tiempo de grabación
				if(status == Status.RECORDING) {
					Date now = new Date();
					long recordingTime = now.getTime() - startDate.getTime();
					if(recordingTime>=MAX_RECORDING_TIME) {
						saveAndResume();
					}	
				}
			}
		}
		class MainTimerTaskOnUIThread implements Runnable {
			@Override
			public void run() {
				updateUI();
			}		
		}

}
