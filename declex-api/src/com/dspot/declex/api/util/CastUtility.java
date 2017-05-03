/**
 * Copyright (C) 2016-2017 DSpot Sp. z o.o
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
package com.dspot.declex.api.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.dspot.declex.api.util.annotation.CopyIgnore;
import com.dspot.declex.api.util.annotation.CopyName;

public class CastUtility {
	
	public static Map<EField, Object> getFields(Object object) {
		//Use fieldsMap to return the fields sorted alphabetically
		Map<EField, Object> fieldsMap = new TreeMap<EField, Object>(new Comparator<EField>() {

			@Override
			public int compare(EField lhs, EField rhs) {
				return lhs.getName().compareTo(rhs.getName());
			}
		});
		
		Class<?> cls = object.getClass();
		while (!cls.equals(Object.class)) 
			try {
				Field[] fields = cls.getDeclaredFields();
				for (Field field : fields) {
					//Ignore No-Copiable Modifiers
					int fieldMofiers = field.getModifiers();
					if (Modifier.isTransient(fieldMofiers) || 
						Modifier.isStatic(fieldMofiers) ||
						Modifier.isFinal(fieldMofiers) || 
						field.getAnnotation(CopyIgnore.class) != null) continue;
					
					field.setAccessible(true);
					fieldsMap.put(new EField(field), field.get(object));
				}

				cls = cls.getSuperclass();
			} catch (IllegalAccessException e) {
			} catch (IllegalArgumentException e) {
			}
		
		return fieldsMap;
		
	}
	
	public static <T> void copy(String fieldName, Object from, List<T> to) {
		
		Iterable<?> iterableFrom = null;
		if (from.getClass().isArray()) {
			List<Object> newList = new ArrayList<>(Array.getLength(from));
			for (int i = 0; i < Array.getLength(from); i++) {
				newList.add(Array.get(from, i));
			}
			iterableFrom = newList;
		} else if (from instanceof Iterable<?>) {
			iterableFrom = ((Iterable<?>) from);
		} 
		
		Iterator<?> fromIterator = iterableFrom != null ? iterableFrom.iterator() : null;
		if (to.size() == 0) return;
		
		Map<EField, Object> toFields = getFields(to.get(0));
		Iterator<EField> toFieldsIterator = toFields.keySet().iterator();
		
		iter: while (toFieldsIterator.hasNext()) {
			EField field = toFieldsIterator.next();
			if (field.name.equals(fieldName)) {
				
				for (Object toObject : to) {
					Object fromObject = from;
					
					if (fromIterator != null) {
						if (fromIterator.hasNext()) {
							fromObject = fromIterator.next();
						} else break iter;
					}
											
					try {
						field.field.set(toObject, fromObject);
					} catch (IllegalAccessException
							| IllegalArgumentException e) {
						e.printStackTrace();
					}
				}
				
				break iter;
			}
		}
	}
	
	public static void copy(Object from, Object to, String ... toIgnore) {
		Map<EField, Object> fromFields = getFields(from);
		Map<EField, Object> toFields = getFields(to);
		
		Iterator<EField> fromFieldsIterator = fromFields.keySet().iterator();
		Iterator<EField> toFieldsIterator = toFields.keySet().iterator();
		
		EField fromField = fromFieldsIterator.next();
		EField toField = toFieldsIterator.next();
		
		List<String> toIgnoreList = toIgnore == null ? new ArrayList<>() : new ArrayList(Arrays.asList(toIgnore));
		
		while (true) {
			
			while (fromField.getName().compareTo(toField.getName()) < 0) {
				if (!fromFieldsIterator.hasNext()) return;
				
				fromField = fromFieldsIterator.next();
			}
			
			while (fromField.getName().compareTo(toField.getName()) > 0) {
				if (!toFieldsIterator.hasNext()) return;
				
				toField = toFieldsIterator.next();
			}

			if (fromField.getName().equals(toField.getName())) {
				
				if (!toIgnoreList.contains(toField.getName())) {
					if (fromField.getType().equals(toField.getType())) {
						try {
							toField.set(to, fromFields.get(fromField));
						} catch (IllegalAccessException | IllegalArgumentException e) {
						}
					} else {
						//Handle field castings
						try {
							//Handling Primitive types
							Class<?> toFieldClass = toField.getType();
							if (toFieldClass.isPrimitive()) {
								if (toField.getType().equals(short.class)) toFieldClass = Short.class;
								else if (toField.getType().equals(byte.class)) toFieldClass = Byte.class;
								else if (toField.getType().equals(char.class)) toFieldClass = Character.class;
								else if (toField.getType().equals(int.class)) toFieldClass = Integer.class;
								else if (toField.getType().equals(byte.class)) toFieldClass = Byte.class;
								else if (toField.getType().equals(float.class)) toFieldClass = Float.class;
								else if (toField.getType().equals(double.class)) toFieldClass = Double.class;
							}
							
							Method valueOf = toFieldClass.getMethod("valueOf", String.class);
	
							if (valueOf != null) {
								Object valueOfFrom = valueOf.invoke(null, fromFields.get(fromField).toString());
								toField.set(to, valueOfFrom);
							}
							
						} catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						}
						
						
					}
				}
				
				if (!fromFieldsIterator.hasNext() || !toFieldsIterator.hasNext()) return;
				fromField = fromFieldsIterator.next();
				toField = toFieldsIterator.next();
			}			
		}
		
	}
	
	private static class EField {
		private Field field;
		private String name;
		
		public EField(Field field) {
			this.field = field;
			
			CopyName copyName = field.getAnnotation(CopyName.class);
			if (copyName != null) {
				this.name = copyName.value();
			} else {
				name = field.getName();
			}
		}
		
		public String getName() {
			return this.name;
		}
		
		public Class<?> getType() {
			return field.getType();
		}
		
		public void set(Object object, Object value) throws IllegalAccessException, IllegalArgumentException {
			field.set(object, value);
		}
		
		@Override
		public String toString() {
			return this.name;
		}
	}
}
