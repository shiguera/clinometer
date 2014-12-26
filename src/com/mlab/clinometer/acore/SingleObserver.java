package com.mlab.clinometer.acore;


public abstract class SingleObserver implements Observer {

	protected Observable observable;
	
	protected SingleObserver(Observable observable) {
		this.observable = observable;
		this.observable.registerObserver(this);
	}
	@Override
	public Observable getObservable() {
		return observable;
	}

	@Override
	public boolean addComponent(Observer o) {
		// ignore method for SingleObserver
		return false;
	}

	@Override
	public boolean removeComponent(Observer o) {
		// ignore method for SingleObserver
		return false;
	}

	@Override
	public Observer getComponent(int index) {
		// ignore method for SingleObserver
		return null;
	}

	public abstract void update();
}
