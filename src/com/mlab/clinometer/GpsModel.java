package com.mlab.clinometer;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
//import android.util.Log;
import android.widget.Toast;

import com.mlab.android.gpsmanager.GpsListener;
import com.mlab.android.gpsmanager.GpsManager;
import com.mlab.gpx.api.GpxDocument;
import com.mlab.gpx.api.GpxFactory;
import com.mlab.gpx.api.WayPoint;
import com.mlab.gpx.impl.AndroidWayPoint;
import com.mlab.gpx.impl.Track;
import com.mlab.gpx.impl.util.Util;

/**
 * Dispone de un GpsManager para acceder al GPS. Registra las posiciones
 * en un Track que se puede grabar en formato GPX y CSV
 * 
 * @author shiguera
 *
 */
public class GpsModel extends AbstractObservable implements GpsListener {

	//private final String TAG = "ROADRECORDER";

	private final Logger LOG = Logger.getLogger(GpsModel.class);
	
	private Context context;
	private GpxFactory gpxFactory;

	protected GpsManager gpsManager;
	protected Track track;

	// Datos de status guardados para el caso isRecording=true
	protected boolean isRecording;
	// Primer punto guardado: lon, lat, alt, t
	protected WayPoint firstWayPoint;
	// Ultimo punto guardado: lon, lat, alt, t
	protected WayPoint lastWayPoint;
	// Ultimo tramo recorrido (Del último punto al anetrior): distance, speed, bearing, incT, incAltitude 
	protected long lastIncT;
	protected double lastDistance;
	protected double lastSpeed;
	protected double lastBearing;
	protected double lastIncAltitude;
	// Datos a origen
	protected long accT;
	protected double accDistance;
	protected double accDistanceUp;
	protected double accDistanceDown;
	protected double accIncAltitude;
	protected double accIncAltitudeUp;
	protected double accIncAltitudeDown;
	// Medias
	protected double avgSpeed;
	
	// Constructor
	public GpsModel(Context context) {
		super();
		this.context = context;
		gpsManager = new GpsManager(context);
		gpsManager.registerGpsListener(this);
		gpxFactory = GpxFactory.getFactory(GpxFactory.Type.ClinometerGpxFactory);
		track = new Track();
		
		initStatusValues();
	}
	
	// GpsManager management
	public boolean startGpsUpdates() {
		boolean result = gpsManager.startGpsUpdates();
		if (result) { 
			notifyObservers();
		} 
		return result;
	}
	public void stopGpsUpdates() {
		gpsManager.stopGpsUpdates();
		notifyObservers();
		return;
	}
	
	// Recording management (Recording = add points to track)
	
	// TODO Aquí se puede gestionar un nuevo segmento
	/**
	 * Comienza a añadir puntos al track en memoria.
	 * 
	 * @param newtrack Si newtrack=true se inicializa un nuevo track,
	 * si newtrack=false los puntos se añaden al track ya existente
	 * 
	 * @return true si todo va bien, false si el GPS no está habilitado
	 */
	public boolean startRecording(boolean newtrack) {
		LOG.debug("GpsModel.startRecording()");
		if(newtrack) {
			track = new Track();
			initStatusValues();
		}
		isRecording = true;
		notifyObservers();
		return true;
	}
	private void initStatusValues() {
		firstWayPoint = null;
		lastWayPoint = null;
		//
		lastIncT = 0l;
		lastDistance = -1.0;
		lastSpeed = -1.0;
		lastBearing = -1.0;
		lastIncAltitude = 0.0;
		//
		accT = 0l;
		accDistance = 0.0;
		accDistanceUp = 0.0;
		accDistanceDown = 0.0;
		accIncAltitude = 0.0;
		accIncAltitudeUp = 0.0;
		accIncAltitudeDown = 0.0;
		// 
		avgSpeed = 0.0;
		
	}
	/**
	 * Deja de añadir puntos al track en memoria
	 */
	public void stopRecording() {
		LOG.debug("GpsModel.stopRecording()");
		isRecording = false;
		notifyObservers();
	}
	
	// Interface GpsListener
	@Override
	public void firstFixEvent() {
		LOG.debug("GpsModel.firstFixEvent()");
		notifyObservers();
	}
	@Override
	public void updateLocation(Location loc) {
		//LOG.debug("GpsModel.updateLocation(): "+loc.toString());		
		if(isRecording) {
			addPointToTrack(locToWayPoint(loc));
		}
		notifyObservers();
	}
	// Track management
	public int wayPointCount() {
		return track.wayPointCount();
	}
	private void addPointToTrack(WayPoint wp) {
		// TODO Pasar a AsyncTask con Synchronized
		if(wp != null) {
			if(firstWayPoint == null) {
				firstWayPoint = wp.clone();
				lastWayPoint = wp.clone();
				return;
			}
			// Last point
			lastIncT = (long)((wp.getTime()-lastWayPoint.getTime())/1000l);
			lastDistance = Util.dist3D(lastWayPoint, wp);
			lastSpeed = lastDistance / (double)lastIncT;
			lastBearing = Util.bearing(lastWayPoint, wp);
			lastIncAltitude = wp.getAltitude() - lastWayPoint.getAltitude();
			// Accumulates
			accT += lastIncT;
			accDistance += lastDistance;
			if(lastIncAltitude>0.0) {
				accDistanceUp += lastIncAltitude;
				accIncAltitudeUp += lastIncAltitude;
			} else {
				accDistanceDown += lastIncAltitude;
				accIncAltitudeDown += lastIncAltitude;
			}
			// Averages
			avgSpeed = accDistance / (double)accT;
			//
			track.addWayPoint(wp, false);	
			lastWayPoint = wp.clone();
		}
	}

	/**
	 * Graba el Track en un fichero en formato GPX.
	 * Utiliza un proceso asíncrono, pero espera hasta la respuesta
	 * 
	 * @param outputfile Fichero de salida
	 * 
	 * @return true si ok, false en caso de errores
	 */
	public boolean saveTrackAsGpx2(File outputfile) {
		LOG.debug("GpsModel.saveTrackAsGpx() "+outputfile.getPath());
		GpxSaver saver = new GpxSaver(outputfile);
		saver.execute();		
		boolean result = false;
		try { 
			saver.get();
			result = true;
		} catch (Exception e) {
			LOG.error("GpsModel.saveTrackAsGpx(); ERROR : can't save gpx track");
			result = false;
		}
		return result;
	}
	public boolean saveTrackAsGpx(File outputfile) {
		LOG.debug("GpsModel.saveTrackAsGpx() "+outputfile.getPath());
		boolean result = false;
    	GpxDocument doc = gpxFactory.createGpxDocument();
    	doc.addTrack(track);
    	int resp = Util.write(outputfile.getPath(), doc.asGpx());
		if(resp == 1) {
			result = true;
		}
    	return result;
	}
	/**
	 * AsyncTask para grabar el Track en un fichero en formato GPX.
	 * Notifica a través de un Toast si hay error
	 * 
	 */
	class GpxSaver extends AsyncTask<Void,String, Boolean> {
		File outFile;
		GpxSaver(File outfile) {
			LOG.debug("GpxSaver()");
			this.outFile = new File(outfile.getPath());
		}
		@Override
		protected Boolean doInBackground(Void... params) {
			LOG.debug("GpsModel.GpxSaver.doInBackground()");
			boolean result=false;
	        try {
	        	GpxDocument doc = gpxFactory.createGpxDocument();
	        	doc.addTrack(track);
	        	int resp = Util.write(outFile.getPath(), doc.asGpx());
	        	if(resp == 0) {
	        		onProgressUpdate("File saved: "+outFile.getPath());
	        		result = true;
	        	} else {
	        		onProgressUpdate("Error " + resp + "saving file "+outFile.getPath());
	        		result = false;
	        	} 
	        } catch (Exception e) {
	        	String s = "GpsModel.GpxSaver.doInBackground(): Error saving file";
	        	onProgressUpdate(s);
	        	result = false;
	        }	
			return result;
		}
		@Override
		protected void onProgressUpdate(String... values) {
			for(String s: values) {
				log(s);
			}
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if(!result) {
				String msg = "Error can't save Gpx Document";
	        	log("GpsModel.saveTrackAsGpx(): " + msg);
	        	showNotification(msg);
			}
			//super.onPostExecute(result);
		}
	}
	private void log(String msg) {
		LOG.debug(msg);
	}
	/**
	 * Graba el Track en un fichero en formato CSV.
	 * Utiliza un proceso asíncrono, pero espera hasta la respuesta
	 * 
	 * @param outputfile Fichero de salida
	 * 
	 * @return true si ok, false en caso de errores
	 */
	public boolean saveTrackAsCsv2(File outputfile, boolean withutmcoords) {
		CsvSaver saver = new CsvSaver(outputfile, withutmcoords);
		saver.execute();
		boolean result = true;
		try {
			//result = saver.get();
		} catch (Exception e ) {
			LOG.error("GpsModel.saveTrackAsCsv() ERROR: Can't save csv track");
			result = false;
		}
		return result;
	}
	public boolean saveTrackAsCsv(File outputfile, boolean withutmcoords) {
    	GpxDocument doc = gpxFactory.createGpxDocument();
    	doc.addTrack(track);
    	boolean result = false;
    	int resp = Util.write(outputfile.getPath(), track.asCsv(withutmcoords));
		if(resp==1) {
			result = true;
		}
    	return result;
	}

	/**
	 * AsyncTask para grabar el Track en un fichero en formato CSV
	 * Notifica a través de un Toast si hay error
	 * 
	 */
	public class CsvSaver extends AsyncTask<Void, Void, Boolean> {
		File outFile;
		boolean withUtmCoords;
		CsvSaver(File outfile, boolean withUtmCoords) {
			this.outFile = outfile;
			this.withUtmCoords = withUtmCoords;
		}
		@Override
		protected Boolean doInBackground(Void... params) {
			boolean result=false;
	        try {
	        	GpxDocument doc = gpxFactory.createGpxDocument();
	        	doc.addTrack(track);
	        	Util.write(outFile.getPath(), track.asCsv(withUtmCoords));
	        	result = true;
	        } catch (Exception e) {
	        	result = false;
	        }
			return result;
		}
		@Override
		protected void onPostExecute(Boolean result) {
			if(!result) {
				String msg = "Error can't save CSV Document";
	        	LOG.error("GpsModel.saveTrackAsCsv(): " + msg);
	        	GpsModel.this.showNotification(msg);
			}
        	super.onPostExecute(result);
		}
	}
	// Getters
	public Location getLastLocReceived() {
		return gpsManager.getLastLocation();
	}
	public GpsManager getGpsManager() {
		return gpsManager;
	}
	public Track getTrack() {
		return this.track;
	}
	public AndroidWayPoint getLastWayPoint() {
		if(getLastLocReceived() != null) {
			return locToWayPoint(getLastLocReceived());
		}
		return null;
	}
	public int getPointsCount() {
		if(this.track != null) {
			return this.track.wayPointCount();
		}
		return 0;
	}
	public double getSpeed() {
		return lastSpeed;
	}
	public double getBearing() {
		return lastBearing;
	}
	public double getDistance() {
		return lastDistance;
	}

	// Status
	public boolean isGpsEnabled() {
		return this.gpsManager.isGpsEnabled();
	}
	@Deprecated
	public boolean isReceiving() {
		return this.gpsManager.isGpsEnabled();
		//return this.gpsManager.isGpsEventFirstFix();
	}
	public boolean isRecording() {
		return this.isRecording;
	}

	// Utilities
	private AndroidWayPoint locToWayPoint(Location loc) {
		List<Double> listvalues = Arrays.asList(new Double[]{loc.getLongitude(),
				loc.getLatitude(), loc.getAltitude(), (double) loc.getSpeed(), 
				(double) loc.getBearing(), (double) loc.getAccuracy()});
		WayPoint point = gpxFactory.createWayPoint("", "", loc.getTime(), listvalues); 
		return (AndroidWayPoint)point;
	}
	private void showNotification(String msg) {
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
	}

	public WayPoint getFirstWayPoint() {
		return firstWayPoint;
	}

	public long getLastIncT() {
		return lastIncT;
	}

	public double getLastDistance() {
		return lastDistance;
	}

	public double getLastSpeed() {
		return lastSpeed;
	}

	public double getLastBearing() {
		return lastBearing;
	}

	public double getLastIncAltitude() {
		return lastIncAltitude;
	}

	public long getAccT() {
		return accT;
	}

	public double getAccDistance() {
		return accDistance;
	}

	public double getAccDistanceUp() {
		return accDistanceUp;
	}

	public double getAccDistanceDown() {
		return accDistanceDown;
	}

	public double getAccIncAltitude() {
		return accIncAltitude;
	}

	public double getAccIncAltitudeUp() {
		return accIncAltitudeUp;
	}

	public double getAccIncAltitudeDown() {
		return accIncAltitudeDown;
	}

	public double getAvgSpeed() {
		return avgSpeed;
	}
}
