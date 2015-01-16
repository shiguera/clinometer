package com.mlab.clinometer;

import java.util.List;

/**
 * Proporciona un API para utilizar un clinómetro.
 * 
 * @author shiguera
 *
 */
public interface Clinometer {
	
	/**
	 * Activa el sensor
	 * @return
	 */
	public boolean start();
	/**
	 * Desactiva el sensor
	 * @return
	 */
	public boolean stop();
	/**
	 * Devuelve true si el sensor está activado
	 * @return
	 */
	public boolean isRunning();

	/**	
	 * Devuelve el último valor de inclinaciones leído por el sensor
	 * @return
	 */
	public double[] getLastRawValue();
	/**
	 * Devuelve los valores leídos hasta el momento por el sensor,
	 * desde que se inició con start(). En función del valor de 
	 * isSavingRawValues() puede suceder que los raw values
	 * no se estén guardando, y solo haya filtered values;
	 * @return
	 */
	public List<double[]> getRawValues();
	public int getRawValuesCount();

}
