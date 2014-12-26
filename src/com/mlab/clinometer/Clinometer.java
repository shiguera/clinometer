package com.mlab.clinometer;

import java.util.List;

/**
 * Proporciona un API para utilizar un clinómetro
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
	 * Devuelve el último valor de inclinaciones leído por el sensor
	 * @return
	 */
	public double[] getLast();
	/**
	 * Devuelve los valores leídos hasta el momento por el sensor,
	 * desde que se inició con start()
	 * @return
	 */
	public List<double[]> getValues();
	/**
	 * Devuelve true si el sensor está activado
	 * @return
	 */
	public boolean isRunning();

}
