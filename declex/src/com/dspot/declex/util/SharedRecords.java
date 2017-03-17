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
package com.dspot.declex.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.annotations.EBean;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.internal.model.AnnotationElements;

import com.dspot.declex.api.eventbus.UseEvents;
import com.dspot.declex.api.localdb.LocalDBModel;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JBlock;

public class SharedRecords {
	private static Map<JBlock, Map<Integer, IJStatement>> priorityMethods = new HashMap<>();
	
	private static Map<String, String> events;
	private static Collection<String> models;
	private static Collection<String> actions;
	
	public static void reset() {
		priorityMethods = new HashMap<>();
		events = null;
		models = null;
		actions = null;
	}
	
	public static void priorityAdd(JBlock method, IJStatement code, int priority) {
		Map<Integer, IJStatement> statements = priorityMethods.get(method);
		if (statements == null) {
			statements = new TreeMap<>();
			priorityMethods.put(method, statements);
		}
		
		statements.put(priority, code);
	}
	
	public static void priorityExecute() {
		for (JBlock method : priorityMethods.keySet()) {
			Map<Integer, IJStatement> statements = priorityMethods.get(method);
			for (Integer key : statements.keySet()) {
				method.add(statements.get(key));
			}
		}
	}
	
	
	//=============================EVENTS==================================
	
	public static Map<String, String> getEventGeneratedClasses(AndroidAnnotationsEnvironment environment) {
		final AnnotationElements validatedModel = environment.getValidatedElements(); 
		final ProcessingEnvironment processingEnv = environment.getProcessingEnvironment();
		
		//Get all the events from the event file
		if (events == null) {
			events = new TreeMap<>();
			
			File outputEventsDir = FileUtils.getConfigFile("events", processingEnv);
			File eventsFile = new File(outputEventsDir.getAbsolutePath() + File.separator + "events.txt");
			
			try {
				InputStream in = new FileInputStream(eventsFile);
				byte[] data = new byte[(int) eventsFile.length()];
				in.read(data);
				in.close();	
				
				String[] eventsArray = new String(data, "UTF-8").split("\r\n");
				
				//Check if the event is valid
				for (String event : eventsArray) {
					if (event.trim().equals("")) continue;
					
					final int sep = event.indexOf(':');
					final String eventName = event.substring(0, sep);
					final String savedGenerator = event.substring(sep + 1);
					
					events.put(eventName, savedGenerator);
				}
				
			} catch (IOException e) {
			}
			
			Set<? extends Element> annotatedElements = validatedModel.getRootAnnotatedElements(EBean.class.getCanonicalName());
			for (Element elem : annotatedElements) {
				if (elem.getAnnotation(UseEvents.class) != null) {
					if (events.get(elem.toString()) != null) continue;
					events.put(elem.toString(), null);
				}
			}
		} 
		
		return events;
	}
	
	public static String getEvent(String className, AndroidAnnotationsEnvironment environment) {
		if (className == null) return null;
		
		if (className.endsWith(ModelConstants.generationSuffix())) className = className.substring(0, className.length()-1);
		
		final Collection<String> eventClassNames = getEventGeneratedClasses(environment).keySet();
		
		for (String eventName : eventClassNames) {
			if (eventName.equals(className) || (!className.contains(".") && eventName.endsWith("."+className)))
				return eventName;
		}
		
		return null;
	}
	
	public static void addEventGeneratedClass(String className, AndroidAnnotationsEnvironment environment) {
		addEventGeneratedClass(className, environment, null);
	}
	
	public static void addEventGeneratedClass(String className, AndroidAnnotationsEnvironment environment, String generator) {
		final Map<String, String> eventClassNames = getEventGeneratedClasses(environment);
		
		if (generator != null) {
			if (generator.equals(eventClassNames.get(className))) return;
			eventClassNames.put(className, generator);
		} else {
			if (!eventClassNames.containsKey(className)) {
				eventClassNames.put(className, generator);
			}
		}	
	}
	
	public static void writeEvents(ProcessingEnvironment processingEnv) {
		//Write all the events to the event file
		try {
			File outputEventsDir = FileUtils.getConfigFile("events", processingEnv);
			File eventsFile = new File(outputEventsDir.getAbsolutePath() + File.separator + "events.txt");
			PrintWriter out = new PrintWriter(eventsFile);
			
			String data = "";
			if (events != null) {
				for (Entry<String, String> event : events.entrySet()) {
					if (!data.equals("")) data = data + "\r\n";
					data = data + event.getKey() + ":" + event.getValue();
				}				
			}
			
			out.write(data);
			out.close();
		} catch (IOException e) {
		}
	}

	
	//=====================================MODELS===========================
	
	public static Collection<String> getModelGeneratedClasses(AndroidAnnotationsEnvironment environment) {
		final AnnotationElements validatedModel = environment.getValidatedElements(); 
		final ProcessingEnvironment processingEnv = environment.getProcessingEnvironment();
		
		//Get all the events from the event file
		if (models == null) {
			models = new TreeSet<>();
			
			File outputEventsDir = FileUtils.getConfigFile("models", processingEnv);
			File eventsFile = new File(outputEventsDir.getAbsolutePath() + File.separator + "models.txt");
			
			try {
				InputStream in = new FileInputStream(eventsFile);
				byte[] data = new byte[(int) eventsFile.length()];
				in.read(data);
				in.close();	
				
				String[] modelsArray = new String(data, "UTF-8").split("\r\n");
				
				//Check if the event is valid
				for (String model : modelsArray) {
					if (model.trim().equals("")) continue;
					
					models.add(model);
				}
				
			} catch (IOException e) {
			}
			
			Set<? extends Element> annotatedElements = validatedModel.getRootAnnotatedElements(EBean.class.getCanonicalName());
			for (Element elem : annotatedElements) {
				if (elem.getAnnotation(LocalDBModel.class) != null) {
					if (models.contains(elem.toString())) continue;
					models.add(elem.toString());
				}
			}
		} 
		
		return models;
	}
	
	public static String getModel(String className,  AndroidAnnotationsEnvironment environment) {
		if (className == null) return null;
		
		if (className.endsWith(ModelConstants.generationSuffix())) className = className.substring(0, className.length()-1);
		
		final Collection<String> modelClassNames = getModelGeneratedClasses(environment);
		
		for (String model : modelClassNames) {
			if (model.equals(className) || (!className.contains(".") && model.endsWith("."+className)))
				return model;
		}
		
		return null;
	}
	
	public static void addModelGeneratedClass(String className, AndroidAnnotationsEnvironment environment) {
		final Collection<String> eventClassNames = getModelGeneratedClasses(environment);
		
		eventClassNames.add(className);		
	}
	
	public static void writeModels(ProcessingEnvironment processingEnv) {
		//Write all the events to the event file
		try {
			File outputModelsDir = FileUtils.getConfigFile("models", processingEnv);
			File modelsFile = new File(outputModelsDir.getAbsolutePath() + File.separator + "models.txt");
			PrintWriter out = new PrintWriter(modelsFile);
			
			String data = "";
			if (models != null) {
				for (String model : models) {
					if (!data.equals("")) data = data + "\r\n";
					data = data + model;
				}				
			}
			
			out.write(data);
			out.close();
		} catch (IOException e) {
		}
	}

}
