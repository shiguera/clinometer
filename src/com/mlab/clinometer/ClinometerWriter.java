package com.mlab.clinometer;

import java.io.File;

public interface ClinometerWriter {
	
	boolean write(File file, ClinometerStore store);

}
