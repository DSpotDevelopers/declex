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
package com.dspot.declex;

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

import org.androidannotations.internal.model.AnnotationElements;
import org.androidannotations.internal.process.ModelProcessor.ProcessResult;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerContext;
import org.androidannotations.logger.LoggerFactory;
import org.androidannotations.plugin.AndroidAnnotationsPlugin;

import com.dspot.declex.action.Actions;
import com.dspot.declex.util.LayoutsParser;
import com.dspot.declex.util.MenuParser;
import com.dspot.declex.util.SharedRecords;

public class DeclexProcessor extends org.androidannotations.internal.AndroidAnnotationProcessor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DeclexProcessor.class);
	
	protected LayoutsParser layoutsParser;
	protected MenuParser menuParser;
	protected Actions actions;
	
	@Override
	protected AndroidAnnotationsPlugin getCorePlugin() {
		return new DeclexCorePlugin();
	}
	
	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		try {
			layoutsParser = new LayoutsParser(processingEnv, LOGGER);
			menuParser = new MenuParser(processingEnv, LOGGER);
		} catch (Throwable e) {
			LOGGER.error("Cannot parse resources", e);
			
			e.printStackTrace();
			
			return;
		}	
			
		super.init(processingEnv);
		
		actions = new Actions(androidAnnotationsEnv);
		
	}
	
	@Override
	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment roundEnv) {
		
		LOGGER.info("Executing Declex");
				
		try {
			//Update actions information in each round
			actions.getActionsInformation();
			
			return super.process(annotations, roundEnv);
		} catch (Throwable e) {
			LOGGER.error("An error occured", e);
			LoggerContext.getInstance().close(true);
			
			e.printStackTrace();
			
			return false;
		}
		
	}
	
	@Override
	protected ProcessResult processAnnotations(AnnotationElements validatedModel)
			throws Exception {
		ProcessResult result = super.processAnnotations(validatedModel);
		
		SharedRecords.priorityExecute();
		
		return result;
	}
	
	@Override
	protected void generateSources(ProcessResult processResult)
			throws IOException {
		
		//Generate Actions
		actions.buildActionsObject();
				
		SharedRecords.writeEvents(processingEnv);
		SharedRecords.writeModels(processingEnv);
		
		super.generateSources(processResult);
		
	}
	
	public static void main(String[] args) {
		System.out.println("Declex Processor");
	}
}
