package com.mlab.clinometer;

import java.io.File;

import com.mlab.gpx.api.GpxFactory;
import com.mlab.gpx.impl.Track;

public class GpxTrackWriter extends AbstractTrackWriter {

	
	public GpxTrackWriter(GpxFactory factory, Track track, boolean writeUtmCoords) {
		super(factory, track, writeUtmCoords);		
	}
	
	@Override
	public boolean write(File file) {
		// TODO Auto-generated method stub
		return false;
	}

}
