package com.mlab.clinometer;

/**
 * Las clases {@link Observer} se pueden registrar como observadores de las 
 * clases {@link Observable}. Disponen de un método
 * <em>update()</em> que podrá ser llamado por el {@link Observable}
 * para avisar de modificaciones.<br/>
 *  
 * @author shiguera
 *
 */
public interface Observer {
	
	Observable getObservable();
	void update();
	
	boolean addComponent(Observer o);
	boolean removeComponent(Observer o);
	Observer getComponent(int index);
}