package com.dspot.declex.api.injection;

import javax.annotation.Nullable;

import android.util.Log;

public class NotFoundProperty<T> implements Property<T> {
	
	@Nullable
	@Override
	public T get() {
		Log.w("DecleX", "Reading not found property value");
		return null;
	};
	
	@Override
	public void set(T value){
		Log.w("DecleX", "Assigning value to not found property");
	};
	
}
