package com.mlab.clinometer;

import java.io.File;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
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
import com.mlab.gpx.impl.util.Util;

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

	private final long CLINOMETER_INTERVAL = 250l;
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
	Clinometer clinometer;
	//TimeAverageClinometer clinometer;
	
	Date startRecordingDate;
	
		
	// Status
	private enum Status {
		FIXING_GPS, GPS_FIXED, RECORDING, SAVING
	};
	private Status status;
	float escoraZero = 0.0f, cabeceoZero = 0.0f;
	Date startDate;

	EscoraPanelFragment escoraPanelFragment;
	TextView tv, tvGpsEnabled, tvGpsFixed, tvIsRecording;
	Button btnStartStop, btnFix;
	boolean isRecording;
	
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
		btnStartStop.setText("GRABAR");
		isRecording = false;
	}

	private void configureLayout() {
		FragmentManager fm = getFragmentManager();
		escoraPanelFragment = (EscoraPanelFragment) fm.findFragmentById(R.id.escora_panel);
		
		tv = (TextView) this.findViewById(R.id.lbl);
		
		tvGpsEnabled = (TextView) this.findViewById(R.id.lblGpsEnabled);
		tvGpsFixed = (TextView) this.findViewById(R.id.lblGpsFixed);
		tvIsRecording = (TextView) this.findViewById(R.id.lblIsRecording);
		
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
		clinometer = new TimeAverageClinometer(this, new ClinometerStore(), 2000);
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
		
		gpsDevice.startGpsUpdates();
		
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
			gpsDevice.stopRecording();
			gpsDevice.stopGpsUpdates();
		}
		
		if (clinometer.isRunning()) {
			clinometer.stop();			
			//LOG.debug("onPause() clinometer.stop() at " + System.currentTimeMillis()/1000L);
		}
		
		if (mainTimer != null) {
			mainTimer.cancel();
		}

		saveResult();
		
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
		if(isRecording) {
			stopRecording();
		} else {
			startRecording();
		}
	}
	private void startRecording() {
		LOG.debug("startRecording()");
		
		startRecordingDate = new Date();
		
		if (gpsDevice.isGpsEnabled()) {
			gpsDevice.startRecording(true);
		}
		if(clinometer.isEnabled()) {
			clinometer.start();
		}
		btnStartStop.setText("STOP");
		isRecording = true;		
	}
	private void stopRecording() {
		LOG.debug("stopRecording()");
		gpsDevice.stopRecording();
		clinometer.stop();
		saveResult();
		btnStartStop.setText("GRABAR");
		isRecording = false;
		
	}
	
	// save
	private void saveResult() {
		if(startRecordingDate==null) {
			LOG.error("startRecordingDate == null");
			return;
		}
		String name = Util.getTimeStamp(startRecordingDate, true);
		// saveClinometerAsCsv
		saveClinometerAsCsv(name);
		// addInclinationToTrack
		addInclinationToTrack();
		// saveTrackAsCsv
		saveTrackAsCsv(name);
		// saveTrackAsGpx
		saveTrackAsGpx();
		
		
	}
	private void notifyMediaScanner(File fileAdded) {
		sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileAdded)));
	}
	private void saveClinometerAsCsv(String name) {
		LOG.debug("saveClinometerAsCsv()");
		if (clinometer.getStore().size()>0) {
			String filename = "CLIN_" + name + ".csv";
			LOG.debug("saveClinometerAsCsv() file: " + filename);			
			File file = new File(App.getApplicationDirectory(), filename);
			CsvClinometerWriter writer = new CsvClinometerWriter();
			boolean result = writer.write(file, clinometer.getStore());
			if (result) {
				this.notifyMediaScanner(file);
				LOG.info("saveClinometerAsCsv() file saved: " + file.getPath());
			} else {
				LOG.error("saveClinometerAsCsv() ERROR saving file: + file.getPath()");
			}
		} else {
			LOG.warn("saveClinometerAsCsv(): No se pudo grabar, no hay puntos");	
		}
	}
	private void addInclinationToTrack() {
		LOG.debug("addInclinationToTrack()");
		if(gpsDevice.track.size()>0 && clinometer.getStore().size()>0) {
			LOG.debug("addInclinationToTrack(): merging data");	
		} else {
			LOG.warn("addInclinationToTrack(): No se pudo combinar datos: track o clinometer sin datos");	
		}
	}
	private void saveTrackAsCsv(String name) {
		LOG.debug("saveTrackAsCsv()");
		if (gpsDevice.getTrack().wayPointCount()>0) {
			String filename = "TRK_" + name + ".csv";
			File file = new File(App.getApplicationDirectory(), filename);
			boolean result = gpsDevice.saveTrackAsCsv(file, true);
			if (result) {
				this.notifyMediaScanner(file);
				LOG.info("saveTrackAsCsv() file saved: " + file.getPath());
			} else {
				LOG.error("saveTrackAsCsv() ERROR saving file: " + file.getPath());
			}
		} else {
			LOG.warn("saveTrackAsCsv(): No se pudo grabar, no hay puntos");	
		}
	}
	private void saveTrackAsGpx() {
		LOG.debug("saveTrackAsGpx()");
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
		if(clinometer != null && clinometer.getStore().size() > 0) {
			double[] lasts = clinometer.getStore().getValues(clinometer.getStore().size()-1); 
			escora = Math.toDegrees(lasts[2]);
			cabeceo = Math.toDegrees(lasts[1]);
			rumbo = Math.toDegrees(lasts[0]);			
		}
		escoraPanelFragment.setEscora(escora);
		escoraPanelFragment.setCabeceo(cabeceo);
		tv.setText(String.format("Ciclos: %s\n Azimuth: %10.1f",
			Integer.toString(cicleCounter),rumbo));
		tvGpsEnabled.setText("GPS Enabled: " + String.format("%b", gpsDevice.isGpsEnabled()));
		tvGpsFixed.setText("GPS Fixed: " + String.format("%b", gpsDevice.isGpsFirstFix()));
		tvIsRecording.setText("IsRecording: " + String.format("%b", isRecording));

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
