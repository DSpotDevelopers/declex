/**
 * Copyright (C) 2016-2019 DSpot Sp. z o.o
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
package com.dspot.declex.transform;

import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.androidannotations.holder.GeneratedClassHolder;

import com.dspot.declex.transform.writer.BaseTemplateTransformWriter;
import com.helger.jcodemodel.AbstractJType;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JMethod;

import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;

public class HolderMethodTransform<T extends GeneratedClassHolder> extends BaseTemplateTransform<T> {
	
	public HolderMethodTransform(T holder) {
		super(holder);
	}

	public Writer getWriter(Writer out, Map args) {
    	return new HolderMethodWriter(out, args.get("name").toString(), holder);
    }

    private class HolderMethodWriter extends BaseTemplateTransformWriter<T> {
    	
    	private String name;
           
        public HolderMethodWriter(Writer out, String name, T holder) {
        	super(out, holder);
            this.name = name;
        }

        @Override
        public void close() {
        	final String methodName = fieldToGetter(name);
        	
        	//Try to find the method using reflection
        	Method method = null;
        	try {
				method = this.holder.getClass().getMethod(methodName);
			} catch (NoSuchMethodException | SecurityException e) {
				try {
					method = this.holder.getClass().getMethod(methodName + "Body");
				} catch (NoSuchMethodException | SecurityException e1) {
					try {
						method = this.holder.getClass().getMethod(methodName + "Method");
					} catch (NoSuchMethodException | SecurityException e2) {
						try {
							method = this.holder.getClass().getMethod(methodName + "AfterSuperBlock");
						} catch (NoSuchMethodException | SecurityException e3) {
							try {
								method = this.holder.getClass().getMethod(methodName + "BeforeSuperBlock");
							} catch (NoSuchMethodException | SecurityException e4) {
							
							}
						}
					}					
				}
			}
        	
        	/*
        	holder.getGeneratedClass().direct("\nMethod: " + methodName + "\n " + method);
        	if (method == null) {
        		for (Method m : holder.getClass().getMethods()) {
        			holder.getGeneratedClass().direct("\nMethod: " + m.getName());
        		}
        	}
        	*/
        	
        	//If not found, see if the method was already created in the holder, 
        	//TODO ... It's assumed empty params, this should be fixed in future
        	//		  params can be provided in the FTL method
        	if (method == null) {
        		JMethod jMethod = holder.getGeneratedClass().getMethod(name, new AbstractJType[]{});
        		if (jMethod != null) {
        			jMethod.body().directStatement(strCode);
        		}
        	}
			
        	if (method != null) {
        		try {
        			Object result = method.invoke(holder);
        			
        			JBlock block = null;
        			if (result instanceof JMethod) {
        				block = ((JMethod) result).body();
        			} else if (result instanceof JBlock) {
        				block = (JBlock) result;
        			}
        			
					if (block != null) {
						block.directStatement(strCode);
					}
				} catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					e.printStackTrace();
				}
        	}
        }
    }
}  

