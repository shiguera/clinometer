package com.mlab.clinometer;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.mlab.gpx.api.GpxDocument;
import com.mlab.gpx.api.GpxFactory;
import com.mlab.gpx.impl.Track;
import com.mlab.gpx.impl.util.Util;

public class CsvTrackWriter extends AbstractTrackWriter {

	Context context;
	
	public CsvTrackWriter(Context context, GpxFactory factory, Track track, boolean writeUtmCoords) {
		super(factory, track, writeUtmCoords);
		this.context = context;
	}

	@Override
	public boolean write( File file) {
    	GpxDocument doc = gpxFactory.createGpxDocument();
    	doc.addTrack(track);
    	int resp = Util.write(file.getPath(), track.asCsv(writeUtmCoords));
		if (resp == 1) {
			context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, 
					Uri.fromFile(file)));
			return true;
		}
    	return false;
	}

}
