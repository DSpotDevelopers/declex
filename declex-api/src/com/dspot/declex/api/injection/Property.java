package com.dspot.declex.api.injection;

import android.support.annotation.Nullable;

public interface Property<T> {
	
	@Nullable
	T get();
	void set(T value);
	
}
