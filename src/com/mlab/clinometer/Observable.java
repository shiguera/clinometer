package com.mlab.clinometer;

/**
 * Los elementos<em>Observable</em> mantienen un lista de 
 * {@link com.mlab.roadplayer.api.Observer} que son notificados 
 * a través del método <em>notifyObservers()</em>.<br/>
 * Los {@link com.mlab.roadplayer.api.Observer} disponen de un método <em>update()</em> que
 * es el utilizado por el <em>Observer</em> en el método <em>notifyObservers()</em> 
 * para notificarles.<br/>
 * <em>Observable</em> también dispone de una pareja de métodos, 
 * <em>stoptNotifications()</em> y <em>startNotifications()</em>,
 * que permiten detener las notificaciones de manera temporal.<br/> 
 * Por defecto, tras su creación, el estado es de notificaciones activadas.<br/>
 * 
 * El {@link com.mlab.roadplayer.api.Observer} tiene que utilizar el método 
 * <em>registerObserver()</em> para ser incluido en la lista de objetos 
 * que serán notificados.<br/>
 * 
 * <em>Observable</em> es el interface base para los <em>Modelos</em> utilizados
 * en el patrón de diseño <em>Model-View-Observer</em>
 *
 * <p><img src='doc-files/observable.jpg' width='400'/></p>
 * 
 * @author shiguera
 *
 */
public interface Observable {
	
	boolean registerObserver(Observer o);
	boolean removeObserver(Observer o);
	void notifyObservers();
	void stopNotifications();
	void startNotifications();

}
