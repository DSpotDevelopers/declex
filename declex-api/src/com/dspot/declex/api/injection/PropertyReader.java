package com.dspot.declex.api.injection;


public class PropertyReader<T> implements Property<T> {

	T property;
	
	public PropertyReader(T property) {
		this.property = property;
	}
	
	@Override
	public T get() {
		return property;
	}

	@Override
	public void set(T value) {}
	
}
