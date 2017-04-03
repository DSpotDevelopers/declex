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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.internal.InternalAndroidAnnotationsEnvironment;
import org.androidannotations.internal.generation.CodeModelGenerator;
import org.androidannotations.internal.model.AnnotationElements;
import org.androidannotations.internal.model.AnnotationElementsHolder;
import org.androidannotations.internal.model.ModelExtractor;
import org.androidannotations.internal.process.ModelProcessor.ProcessResult;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerContext;
import org.androidannotations.logger.LoggerFactory;
import org.androidannotations.plugin.AndroidAnnotationsPlugin;

import com.dspot.declex.action.ActionHelper;
import com.dspot.declex.action.Actions;
import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.generate.DeclexCodeModelGenerator;
import com.dspot.declex.helper.FilesCacheHelper;
import com.dspot.declex.helper.FilesCacheHelper.FileDetails;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.LayoutsParser;
import com.dspot.declex.util.MenuParser;
import com.dspot.declex.util.SharedRecords;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.wrapper.RoundEnvironmentByCache;

public class DeclexProcessor extends org.androidannotations.internal.AndroidAnnotationProcessor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DeclexProcessor.class);
	
	protected LayoutsParser layoutsParser;
	protected MenuParser menuParser;
	protected Actions actions;
	
	protected FilesCacheHelper cacheHelper;
	protected Set<FileDetails> cachedFiles;
	protected int cachedFilesGenerated;
	
	@Override
	protected AndroidAnnotationsPlugin getCorePlugin() {
		return new DeclexCorePlugin();
	}
	
	@Override
	protected String getFramework() {
		return "DecleX";
	}
	
	@Override
	protected AndroidAnnotationsEnvironment getAndroidAnnotationEnvironment() {
		AndroidAnnotationsEnvironment env = super.getAndroidAnnotationEnvironment();

		actions = new Actions((InternalAndroidAnnotationsEnvironment) env);	
		
		cacheHelper = new FilesCacheHelper(env);
		cachedFiles = new HashSet<>();
		cachedFilesGenerated = 0;
		
		return env; 
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
	}
	
	@Override
	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment roundEnv) {
		
		LOGGER.info("Executing Declex");
				
		try {
			//Update actions information in each round
			actions.getActionsInformation(roundEnv);
			
			boolean processResult = super.process(annotations, roundEnv); 
			
			//Save classes cached when the process is over
			if (roundEnv.processingOver()) {
				cacheHelper.saveGeneratedClasses();
			}
			
			return processResult;
		} catch (Throwable e) {
			LOGGER.error("An error occured", e);
			LoggerContext.getInstance().close(true);
			
			e.printStackTrace();
			
			return false;
		}
		
	}
	
	@Override
	protected AnnotationElementsHolder extractAnnotations(
			Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		
		if (!FilesCacheHelper.isCacheFilesEnabled())
			return super.extractAnnotations(annotations, roundEnv);
		
		timeStats.start("Extract Annotations");
		
		Map<TypeElement, Set<? extends Element>> annotatedElements = new HashMap<>();
		
		Set<TypeElement> noCachedAnnotations = new HashSet<>();
		for (TypeElement annotation : annotations) {
			Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
						
			Set<Element> annotatedElementsWithAnnotation = new HashSet<>();			
			
			for (Element element : elements) {
				
				//Get rootElement
				final Element rootElement = TypeUtils.getRootElement(element);			
				final String className = TypeUtils.getGeneratedClassName(rootElement, androidAnnotationsEnv);
				
				if (cacheHelper.hasCachedFile(className)) {

					Set<FileDetails> detailsList = cacheHelper.getFileDetailsList(className);
					for (FileDetails details : detailsList) {						
						cachedFiles.add(details);
					}
					
					//Actions should be always processed to permit the Actions object
					//to be regenerated accordingly
					if (annotation.asType().toString().equals(ActionFor.class.getCanonicalName())) {
						annotatedElementsWithAnnotation.add(element);
					}
					
				} else {
					annotatedElementsWithAnnotation.add(element);
				}
			}
			
			if (!annotatedElementsWithAnnotation.isEmpty()) {
				noCachedAnnotations.add(annotation);
				annotatedElements.put(annotation, annotatedElementsWithAnnotation);
			}
		}
		
		ModelExtractor modelExtractor = new ModelExtractor();
		AnnotationElementsHolder extractedModel = modelExtractor.extract(
			noCachedAnnotations, 
			getSupportedAnnotationTypes(), 
			new RoundEnvironmentByCache(roundEnv, annotatedElements)
		);
		
		timeStats.stop("Extract Annotations");
				
		return extractedModel;
	}
	
	@Override
	protected AnnotationElements validateAnnotations(
			AnnotationElements extractedModel,
			AnnotationElementsHolder validatingHolder) {
		
		AnnotationElements annotationElements = super.validateAnnotations(extractedModel, validatingHolder);
		
		//Run validations for Actions (it should be run after all the normal validations)
		timeStats.start("Validate Actions");
		LOGGER.info("Validating Actions");
		ActionHelper.getInstance(androidAnnotationsEnv).validate();
		timeStats.stop("Validate Actions");
		
		return annotationElements;
	}
	
	@Override
	protected ProcessResult processAnnotations(AnnotationElements validatedModel)
			throws Exception {
		
		ProcessResult result = super.processAnnotations(validatedModel);
		
		SharedRecords.priorityExecute();

		//Process Actions (it should be run after all the normal process)
		timeStats.start("Process Actions");
		LOGGER.info("Processing Actions");
		ActionHelper.getInstance(androidAnnotationsEnv).process();
		ActionHelper.getInstance(androidAnnotationsEnv).clear();
		timeStats.stop("Process Actions");
		
		return result;
	}
	
	@Override
	protected void generateSources(ProcessResult processResult)
			throws IOException {
				
		timeStats.start("Generate Sources");
		
		int numberOfFiles = processResult.codeModel.countArtifacts() + cachedFiles.size() - cachedFilesGenerated; 
				
		//Generate Actions
		if (!cacheHelper.hasCachedFile(DeclexConstant.ACTION) 
			|| !cachedFiles.contains(cacheHelper.getFileDetails(DeclexConstant.ACTION))) {
			if (actions.buildActionsObject()) numberOfFiles++;			
		}
		
		LOGGER.info("Number of files generated by DecleX: {}", numberOfFiles);
		
		CodeModelGenerator modelGenerator = new DeclexCodeModelGenerator(
			coreVersion, 
			androidAnnotationsEnv.getOptionValue(CodeModelGenerator.OPTION_ENCODING), 
			androidAnnotationsEnv
		);
		modelGenerator.generate(processResult);
		
		for (FileDetails details : cachedFiles) {	
			if (!details.generated) {
				LOGGER.debug("Generating class from cache: {}", details.className);
				details.generate(processingEnv);
				cachedFilesGenerated++;
			}
		}
		
		//Wait till all the documents be saved
		while (FileDetails.isSaving()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {};
		}
				
		timeStats.stop("Generate Sources");
		
		timeStats.start("Save Config");				
		SharedRecords.writeEvents(processingEnv);
		SharedRecords.writeDBModels(processingEnv);				
		timeStats.stop("Save Config");

	}
	
	public static void main(String[] args) {
		System.out.println("Declex Processor");
		
		if (args != null && args.length == 1 && args[0].equals("cache")) {
			System.out.println("Running Cache Service");
			FilesCacheHelper.runClassCacheCreation();
		}
	}
}
