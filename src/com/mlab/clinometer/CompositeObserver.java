package com.mlab.clinometer;

import java.util.ArrayList;
import java.util.List;

public abstract class CompositeObserver implements Observer {

	protected Observable observable;
	protected List<Observer> components;
	
	// Constructor
	protected CompositeObserver(Observable observable) {
		this.observable = observable;
		this.observable.registerObserver(this);
		this.components = new ArrayList<Observer>();
	}
	
	@Override
	public Observable getObservable() {
		return observable;
	}

	@Override
	public void update() {
		for(Observer o: components) {
			o.update();
		}
	}

	@Override
	public boolean addComponent(Observer o) {
		return components.add(o);
	}

	@Override
	public boolean removeComponent(Observer o) {
		return components.remove(o);
	}

	@Override
	public Observer getComponent(int index) {
		return components.get(index);
	}

}
