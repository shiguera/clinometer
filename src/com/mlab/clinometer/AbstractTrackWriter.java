package com.mlab.clinometer;

import java.io.File;

import com.mlab.gpx.api.GpxFactory;
import com.mlab.gpx.impl.Track;

public abstract class AbstractTrackWriter implements TrackWriter {

	protected GpxFactory gpxFactory;
	protected Track track;
	protected boolean writeUtmCoords;
	
	public AbstractTrackWriter(GpxFactory factory, Track track, boolean writeUtmCoords) {
		this.gpxFactory = factory;
		this.track = track;
		this.writeUtmCoords = writeUtmCoords;
	}
	
	public abstract boolean write(File file);
	
}
