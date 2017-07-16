package com.dspot.declex.api.injection;


public class PropertyWrapper<T> implements Property<T> {

	T property;
	
	public PropertyWrapper() {}
	
	public PropertyWrapper(T property) {
		this.property = property;
	}
	
	@Override
	public T get() {
		return property;
	}

	@Override
	public void set(T value) {
		this.property = value;
	}
	
}
