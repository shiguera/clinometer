package com.mlab.clinometer;

import java.io.File;

import org.apache.log4j.Logger;

import com.mlab.gpx.impl.util.Util;

/**
 * Escribe un fichero CSV con los valores almacenados en un ClinometerStore
 * escribiendo los ángulos en grados sexagesimales
 * 
 * @author shiguera
 *
 */
public class CsvClinometerWriter implements ClinometerWriter {
	private final Logger LOG = Logger.getLogger(CsvClinometerWriter.class);
	
	@Override
	public boolean write(File file, ClinometerStore store) {
//		if(file.canWrite()==false) {
//			LOG.error("CsvClinometerWriter.write() ERROR: file.canWrite() == false");
//			return false;
//		}
		if(store.size() == 0) {
			LOG.error("CsvClinometerWriter.write() ERROR: store.size() == 0");
			return false;
		}
		StringBuilder builder = new StringBuilder();
		for (int i =0; i< store.size(); i++) {
			builder.append(buildString(store.getTime(i), store.getValues(i)));
			builder.append("\n");			
		}
		int result = Util.write(file.getPath(), builder.toString());
		return result == 1;
	}
	private String buildString(long t, double[] values) {
		StringBuilder builder = new StringBuilder();
		builder.append(t);
		builder.append(",");
		builder.append(Util.dateTimeToString(t, true));
		builder.append(",");
		if(values != null) {
			builder.append(Math.toDegrees(values[0]));
			builder.append(",");			
			builder.append(Math.toDegrees(values[1]));
			builder.append(",");			
			builder.append(Math.toDegrees(values[2]));
		}
		return builder.toString();
	}

}
