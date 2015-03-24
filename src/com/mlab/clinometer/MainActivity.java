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
import android.os.AsyncTask;
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
import com.mlab.gpx.api.GpxFactory;
import com.mlab.gpx.impl.TrackSegment;
import com.mlab.gpx.impl.extensions.ClinometerWayPoint;
import com.mlab.gpx.impl.tserie.AverageStrategy;
import com.mlab.gpx.impl.tserie.StrategyOnValues;
import com.mlab.gpx.impl.util.Util;

import de.mindpipe.android.logging.log4j.LogConfigurator;

public class MainActivity extends ActionBarActivity implements Observer {
	
	private final Logger LOG = Logger.getLogger(MainActivity.class);

	/**
	 * El RunMode interviene en la configuración del Logger
	 */
	private final RunModes RUNMODE = RunModes.Test;
	private enum RunModes {
		Test, Debug, Production
	};

	/**
	 * Número de milisegundos máximo de cada grabación de ficheros
	 */
	private static final long MAX_RECORDING_TIME = 1200000; 
	
	/**
	 * El MainTimer se utiliza para actualizar la pantalla 
	 * y contar el número de ciclos
	 */
	private Timer mainTimer; 
	/**
	 * Intervalo en milisegundos entre actualizaciones de pantalla
	 */
	private static final int TIMER_INTERVAL = 250; 	
	
	// GpsDevice
	GpsDevice gpsDevice;
	
	// Clinometer
	Clinometer clinometer;
	/**
	 * La instancia de TimeAverageClinometer hace la media de valores
	 * para cada intervalo CLINOMEtER_INTERVAL
	 */
	private final long CLINOMETER_INTERVAL = 1000l;
	
		
	// Status
	private boolean isRecording;
	private Date startRecordingDate;	
	float escoraZero = 0.0f, cabeceoZero = 0.0f;

	EscoraPanelFragment escoraPanelFragment;
	TextView tvGpsStatus, tvCinometerStatus, tvIsMainStatus;
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
		isRecording = false;
		btnStartStop.setText("GRABAR");
	}

	private void configureLayout() {
		FragmentManager fm = getFragmentManager();
		escoraPanelFragment = (EscoraPanelFragment) fm.findFragmentById(R.id.escora_panel);
		
		tvIsMainStatus = (TextView) this.findViewById(R.id.lblMainStatus);
		tvGpsStatus = (TextView) this.findViewById(R.id.lblGpsStatus);
		tvCinometerStatus = (TextView) this.findViewById(R.id.lblClinometerStatus);
		
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
		clinometer = new TimeAverageClinometer(this, new ClinometerStore(), CLINOMETER_INTERVAL);
	}
	private void initGpsDevice() {
		gpsDevice = new GpsDevice(this, GpxFactory.Type.ClinometerGpxFactory);
	}
	@Override
	protected void onResume() {

		mainTimer = new Timer();
		mainTimer.scheduleAtFixedRate(new MainTimerTask(), 0, TIMER_INTERVAL);		
		
		clinometer.startReading();
		
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
		}
		
		if (clinometer.isRecording()) {
			clinometer.stopRecording();			
			//LOG.debug("onPause() clinometer.stop() at " + System.currentTimeMillis()/1000L);
		}
		if (clinometer.isReading()) {
			clinometer.stopReading();			
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
			clinometer.startRecording();
		}
		btnStartStop.setText("STOP");
		btnFix.setEnabled(false);
		isRecording = true;		
	}
	private void stopRecording() {
		LOG.debug("stopRecording()");
		gpsDevice.stopRecording();
		clinometer.stopRecording();
		saveResult();
		btnStartStop.setText("GRABAR");
		btnFix.setEnabled(true);
		isRecording = false;
		
	}

	// btnFix
	private void btnFixClick() {
		LOG.debug("btnFixClick()");
		if(clinometer != null) {
			double[] values = clinometer.getLastRawValues();
			clinometer.setFixAzimuth(values[0]);
			clinometer.setFixCabeceo(values[1]);
			clinometer.setFixEscora(values[2]);		
		}
		
	}

	// save
	private void saveResult() {
		if(startRecordingDate==null) {
			LOG.debug("startRecordingDate == null, perhaps first onResume()");
			return;
		}
		String name = Util.getTimeStamp(startRecordingDate, true);
		// saveClinometerAsCsv
		saveClinometerAsCsv(name);
		// addInclinationToTrack
		addInclinationToTrack(name);
		
		
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
	private void addInclinationToTrack(String name) {
		LOG.debug("addInclinationToTrack()");
		if(gpsDevice.track.size()>0 && clinometer.getStore().size()>0) {
			LOG.debug("addInclinationToTrack(): merging data");	
			InclinationMerger merger = new InclinationMerger(name);
			merger.execute();
		} else {
			LOG.warn("addInclinationToTrack(): No se pudo combinar datos: track o clinometer sin datos");	
		}
	}
	private void inclinationMerged(String filename) {
		// saveTrackAsCsv
		saveTrackAsCsv(filename);
		// saveTrackAsGpx
		saveTrackAsGpx(filename);
				
	}
	class InclinationMerger extends AsyncTask<Void, Void, Void> {

		private String filename;
		private TrackSegment segment;
		public InclinationMerger(String name) {
			filename = name;
		}
		@Override
		protected Void doInBackground(Void... params) {
			segment = gpsDevice.getTrack().getTrackSegment(0);
			StrategyOnValues strategy = new AverageStrategy(500l);
			if(segment.size() > 0) {
				for(int i=0; i< segment.size(); i++) {
					ClinometerWayPoint wp = (ClinometerWayPoint) segment.get(i);
					double[] inc = clinometer.getStore().getValues(wp.getTime(), strategy);					
					if(inc != null) {
						wp.setGuinada(inc[0]);
						wp.setCabeceo(inc[1]);
						wp.setEscora(inc[2]);						
					}
				}
			}
			
			return null;
		}
		@Override
		protected void onPostExecute(Void result) {
			inclinationMerged(filename);
			super.onPostExecute(result);
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
	private void saveTrackAsGpx(String fname) {
		LOG.debug("saveTrackAsGpx()");
		if (gpsDevice.getTrack().wayPointCount()>0) {
			String filename = "TRK_" + fname + ".gpx";
			File file = new File(App.getApplicationDirectory(), filename);
			boolean result = gpsDevice.saveTrackAsGpx(file);
			if (result) {
				this.notifyMediaScanner(file);
				LOG.info("saveTrackAsGpx() file saved: " + file.getPath());
			} else {
				LOG.error("saveTrackAsGpx() ERROR saving file: " + file.getPath());
			}
		} else {
			LOG.warn("saveTrackAsGpx(): No se pudo grabar, no hay puntos");	
		}
	}


	// UpdateUI
	private void updateUI() {
		double escora = -1.0;
		double cabeceo = -1.0;
		double rumbo = -1.0;
		if(clinometer != null) {
			double[] lasts = clinometer.getLastRawValues(); 
			if (lasts != null) {
				escora = Math.toDegrees(lasts[2] - clinometer.getFixEscora());
				cabeceo = Math.toDegrees(lasts[1] - clinometer.getFixCabeceo());
				rumbo = Math.toDegrees(lasts[0] - clinometer.getFixAzimuth());							
			}
		}
		escoraPanelFragment.setEscora(escora);
		escoraPanelFragment.setCabeceo(cabeceo);
		
		tvIsMainStatus.setText(getMainStatus());
		tvGpsStatus.setText(getGpsStatus());
		tvCinometerStatus.setText(getClinometerStatus());
		
	}
	private String getMainStatus() {
		StringBuilder builder = new StringBuilder();
		builder.append("MAIN: isRecording= ");
		builder.append(isRecording);
		return builder.toString();
	}
	private String getGpsStatus() {
		StringBuilder builder = new StringBuilder();
		builder.append("GPS:\n\tisEnabled= ");
		builder.append(gpsDevice.isGpsEnabled());
		builder.append("\n\tisFirstFix= ");
		builder.append(gpsDevice.isGpsFirstFix());
		builder.append("\n\tisRecording= ");
		builder.append(gpsDevice.isRecording());
		return builder.toString();
	}
	private String getClinometerStatus() {
		StringBuilder builder = new StringBuilder();
		builder.append("CLINOMETER:\n\tisEnabled= ");
		builder.append(clinometer.isEnabled());
		builder.append("\n\tisReading= ");
		builder.append(clinometer.isReading());
		builder.append("\n\tisRecording= ");
		builder.append(clinometer.isRecording());
		return builder.toString();
	}

	private void saveAndResume() {

	}

	class MainTimerTask extends TimerTask {
		@Override
		public void run() {
			MainTimerTaskOnUIThread task = new MainTimerTaskOnUIThread();
			runOnUiThread(task);
			// Comprobar el límite de tiempo de grabación
			if (isRecording) {
				Date now = new Date();
				long recordingTime = now.getTime() - startRecordingDate.getTime();
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
