package com.mlab.clinometer;

import java.util.List;

/**
 * Este clinómetro permite definir un TimeLapse, de forma que solo
 * se almacenan lecturas a intervalos prefijados. Las clases derivadas
 * podrán decidir si realizan la media de las lecturas u otro tipo de 
 * operación para seleccionar los valores que se almacenan
 *  
 * @author shiguera
 *
 */
public interface TimedClinometer extends Clinometer {
	/**
	 * Establece el intervalo de tiempo entre lecturas guardadas del clinómetro
	 * @return
	 */
	public long getTimeLapse();
	public void setTimeLapse(long t);

	/**
	 * Permite borrar el histórico de raw values en cada ciclo
	 * para no colmar la memoria
	 * @param delete
	 */
	public void setDeleteRawValues(boolean delete);
	public boolean getDeleteRawValues();

	public double[] getLastFilteredValue();
	public List<double[]> getFilteredValues();
	public int getFilteredValuesCount();
	
}
