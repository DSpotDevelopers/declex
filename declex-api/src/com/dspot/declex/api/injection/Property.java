package com.dspot.declex.api.injection;

import javax.annotation.Nullable;

public interface Property<T> {
	
	@Nullable
	T get();
	void set(T value);
	
}
