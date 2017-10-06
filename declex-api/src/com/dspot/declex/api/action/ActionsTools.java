package com.dspot.declex.api.action;

import java.util.List;

import javax.annotation.Nonnull;

public class ActionsTools {

	public static Object injectDefault = null;
	
	@Nonnull
	@SuppressWarnings("unchecked")
	public static <T> T $inject() {
		return (T)injectDefault;
	}
	
	@Nonnull
	@SuppressWarnings("unchecked")
	public static <T> T $inject(String name) {
		return (T)injectDefault;
	}
	
	@Nonnull
	@SuppressWarnings("unchecked")
	public static <T> T $injectItem(T[] array, String name) {
		return (T)injectDefault;
	}
	
	@Nonnull
	@SuppressWarnings("unchecked")
	public static <T> T $injectItem(List<T> list, String name) {
		return (T)injectDefault;
	}
	
	@Nonnull
	@SuppressWarnings("unchecked")
	public static <T> T $cast(Object variable) {
		return (T)variable;
	}
	
	@Nonnull
	public static <T> T $item(T[] array, int position) {
		return array[position];
	}
	
	@Nonnull
	public static <T> T $item(List<T> list, int position) {
		return list.get(position);
	}
}
