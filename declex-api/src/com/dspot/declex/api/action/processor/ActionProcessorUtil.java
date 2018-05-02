/**
 * Copyright (C) 2016-2018 DSpot Sp. z o.o
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dspot.declex.api.action.processor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class ActionProcessorUtil {
	
	public static <T> T getMethodInHolder(String methodName, Object holder) {
		return getMethodInHolder(methodName, holder, null);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getMethodInHolder(String methodName, Object holder, String subHolder, Object ... params) {
		Class<?> clazz = holder.getClass();
		if (subHolder != null) {
			try {
				Method getPluginHolder = getMethod("getPluginHolder", clazz);
				
				clazz = Class.forName(subHolder);
				
				//It's assumed only one constructor that takes as parameter the holder
				//TODO ViewsHolder doesn't follow this pattern
				Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
				Object subHolderInstance = constructor.newInstance(holder);
				
				holder = getPluginHolder.invoke(holder, subHolderInstance);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			
		} 
		
		if (clazz == null) return null;

	    Method method = getMethod(methodName, clazz);	    
	    if (method == null) return null;
	    
	    try {
	    	if (params != null && params.length > 0)
	    		return (T) method.invoke(holder, params);
	    	else {
	    		return (T) method.invoke(holder);
	    	}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
	
	private static Method getMethod(String methodName, Class<?> clazz) {
		Method method = getDeclaredMethod(methodName, clazz);
	    if (method == null) {
	    	Class<?> parent = clazz.getSuperclass();
	    	
	    	while (parent != null) {
	    		method = getDeclaredMethod(methodName, parent);
	    		if (method != null) break;
	    		parent = parent.getSuperclass();
	    	}
	    }
	    
	    return method;
	}
	
	private static Method getDeclaredMethod(String methodName, Class<?> clazz) {
		Method[] methods = clazz.getDeclaredMethods();
		for (Method method : methods) {
			if (method.getName().equals(methodName)) {
				method.setAccessible(true);
				return method;
			}
		}
		
		return null;
	}
}
