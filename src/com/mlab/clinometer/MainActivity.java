package com.mlab.clinometer;

import java.io.File;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.mlab.android.utils.AndroidUtils;
import com.mlab.clinometer.acore.Observable;
import com.mlab.clinometer.acore.Observer;

import de.mindpipe.android.logging.log4j.LogConfigurator;

public class MainActivity extends ActionBarActivity implements Observer {
	
	private final Logger LOG = Logger.getLogger(MainActivity.class);
	private enum RunModes {
		Test, Debug, Production
	};
	/**
	 * Interviene en la configuración del Logger
	 */
	private final RunModes RUNMODE = RunModes.Test;

	
	/**
	 * Número de milisegundos de cada grabación de ficheros
	 */
	private static final long MAX_RECORDING_TIME = 1200000; 
	
	// MainTimer
	public static final int TIME_LAPSE = 250; 
	
	/**
	 * A intervalos definidos en TIME_LAPSE proporciona actualizaciones de pantalla
	 */
	Timer mainTimer; 

	/**
	 *  Tiempo en milisegundos para el mainTimer
	 */
	int cicleCounter;
	
	// GpsModel
	GpsDevice gpsDevice;
	
	// Clinometer
	TimedClinometer clinometer;
		
	// Status
	private enum Status {
		FIXING_GPS, GPS_FIXED, RECORDING, SAVING
	};
	private Status status;
	float escoraZero = 0.0f, cabeceoZero = 0.0f;
	Date startDate;

	EscoraPanelFragment escoraPanelFragment;
	TextView tv;
	Button btnStartStop, btnFix;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (!initApplicationDirectory()) {
			exit("ERROR: Can't open application directory");
			return;
		}

		configureLogger();
		LOG.info("-----------------------");
		LOG.info("MainActivity.onCreate()");
		LOG.info("-----------------------");
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		configureLayout();

		initClinometer();

		// GpsModel
		initGpsDevice();
		
		// Inicializar Timer
		cicleCounter = 0;
		
	}

	private void configureLayout() {
		FragmentManager fm = getFragmentManager();
		escoraPanelFragment = (EscoraPanelFragment) fm.findFragmentById(R.id.escora_panel);
		
		tv = (TextView) this.findViewById(R.id.lbl);
		
		btnStartStop = (Button) findViewById(R.id.btnStartStop);
		btnStartStop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				btnStartStopClick();
			}
			
		});
		btnFix = (Button) this.findViewById(R.id.btnFix);
		btnFix.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				btnFixClick();
			}
		});
		
	}

	private void initClinometer() {
		clinometer = new ClinometerImpl(this);
	}
	private void initGpsDevice() {
		status = Status.FIXING_GPS;
		gpsDevice = new GpsDevice(this);
	}
	@Override
	protected void onResume() {

		mainTimer = new Timer();
		mainTimer.scheduleAtFixedRate(new MainTimerTask(), 0, TIME_LAPSE);		
		
		clinometer.start();
		
		LOG.debug("onResume() gpsModel.isRecording(): " + gpsDevice.isRecording());
		//System.out.println("onResume() gpsModel.isRecording(): " + gpsDevice.isRecording());
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
		if(gpsDevice.isRecording()) {
			
		}
		if (mainTimer != null) {
			mainTimer.cancel();
		}
		if (clinometer.isRunning()) {
			clinometer.stop();			
		}
		super.onPause();
	}

	/**
	 * Configura el Logger de android-logging-log4j
	 */
	private void configureLogger() {
		File logfile = new File(App.getApplicationDirectory(),
				App.getLogFileName());
		final LogConfigurator logConfigurator = new LogConfigurator();
		logConfigurator.setMaxBackupSize(1);
		logConfigurator.setMaxFileSize(500 * 1024);
		logConfigurator.setFileName(logfile.getPath());
		logConfigurator.setFilePattern("%d - %t - %p [%c{1}]: %m%n");

		if (RUNMODE == RunModes.Production) {
			logConfigurator.setRootLevel(org.apache.log4j.Level.INFO);
			logConfigurator.setLevel("com.mlab.clinometer",
					org.apache.log4j.Level.INFO);
			logConfigurator.setUseLogCatAppender(false);
		} else {
			logConfigurator.setRootLevel(org.apache.log4j.Level.ALL);
			logConfigurator.setLevel("com.mlab.clinometer",
					org.apache.log4j.Level.ALL);
			logConfigurator.setUseLogCatAppender(true);
		}

		logConfigurator.configure();
	}
	private boolean initApplicationDirectory() {
		File outdir = new File(AndroidUtils.getExternalStorageDirectory(),
				App.getAppDirectoryName());
		return setApplicationDirectory(outdir);
	}
	private boolean setApplicationDirectory(File outdir) {
		if (!outdir.exists()) {
			if (!outdir.mkdir()) {
				return false;
			}
		}
		App.setApplicationDirectory(outdir);
		return true;
	}
	private void exit(final String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				LOG.info("exit(): " + message);
				finish();
				return;
			}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	// btnStartStop
	private void btnStartStopClick() {
		LOG.debug("btnStartStopClick()");
		
	}
	// btnFix
	private void btnFixClick() {
		LOG.debug("btnFixClick()");
		
	}

	// UpdateUI
	private void updateUI() {
		double escora = -1.0;
		double cabeceo = -1.0;
		double rumbo = -1.0;
		if(clinometer != null && clinometer.getFilteredValuesCount() > 0) {
			escora = Math.toDegrees(clinometer.getLastFilteredValue()[2]);
			cabeceo = Math.toDegrees(clinometer.getLastFilteredValue()[1]);
			rumbo = Math.toDegrees(clinometer.getLastFilteredValue()[0]);			
		}
		escoraPanelFragment.setEscora(escora);
		escoraPanelFragment.setCabeceo(cabeceo);
		tv.setText(String.format("Ciclos: %s\n Azimuth: %10.1f",
			Integer.toString(cicleCounter),rumbo));
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
			}
		}
	}


	class MainTimerTaskOnUIThread implements Runnable {
		@Override
		public void run() {
			updateUI();
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
