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
package com.dspot.declex;

import com.dspot.declex.action.Actions;
import com.dspot.declex.annotation.*;
import com.dspot.declex.annotation.action.ActionFor;
import com.dspot.declex.api.util.FormatsUtils;
import com.dspot.declex.helper.ActionHelper;
import com.dspot.declex.parser.LayoutsParser;
import com.dspot.declex.parser.MenuParser;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.SharedRecords;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.wrapper.RoundEnvironmentByCache;
import com.dspot.declex.wrapper.generate.DeclexCodeModelGenerator;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldRef;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.export.Export;
import org.androidannotations.annotations.export.Exported;
import org.androidannotations.annotations.export.Import;
import org.androidannotations.handler.AnnotationHandler;
import org.androidannotations.helper.AndroidManifest;
import org.androidannotations.helper.CompilationTreeHelper;
import org.androidannotations.internal.generation.CodeModelGenerator;
import org.androidannotations.internal.helper.AndroidManifestFinder;
import org.androidannotations.internal.model.AnnotationElements;
import org.androidannotations.internal.model.AnnotationElementsHolder;
import org.androidannotations.internal.model.ModelExtractor;
import org.androidannotations.internal.process.ModelProcessor.ProcessResult;
import org.androidannotations.internal.virtual.VirtualElement;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerContext;
import org.androidannotations.logger.LoggerFactory;
import org.androidannotations.plugin.AndroidAnnotationsPlugin;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.*;

public class DeclexProcessor extends org.androidannotations.internal.AndroidAnnotationProcessor {
	
	private static final boolean PRE_GENERATION_ENABLED = false;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DeclexProcessor.class);
	
	protected LayoutsParser layoutsParser;
	protected MenuParser menuParser;
	protected Actions actions;

	private CompilationTreeHelper compilationTreeHelper;
	
	@Override
	protected AndroidAnnotationsPlugin getCorePlugin() {
		return new DeclexCorePlugin();
	}
	
	@Override
	protected String getFramework() {
		return "DecleX";
	}
	
	@Override
	protected void helpersInitialization() {
		super.helpersInitialization();
		
		try {
			timeStats.start("Helpers Initialization");
						
			layoutsParser = new LayoutsParser(androidAnnotationsEnv, LOGGER);
			menuParser = new MenuParser(androidAnnotationsEnv, LOGGER);
			
			actions = new Actions(androidAnnotationsEnv);

			compilationTreeHelper = new CompilationTreeHelper(androidAnnotationsEnv);

			timeStats.stop("Helpers Initialization");
			timeStats.logStats();
			
		} catch (Throwable e) {
			System.err.println("Something went wrong starting the framework");
			e.printStackTrace();
		}
	}
		
	@Override
	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment roundEnv) {
		
		LOGGER.info("Executing Declex");
						
		try {

			return super.process(annotations, roundEnv);
			
		} catch (Throwable e) {
			LOGGER.error("An error occured", e);
			LoggerContext.getInstance().close(true);
			
			e.printStackTrace();
			
			return false;
		}
		
	}
	
	@Override
	protected boolean nothingToDo(Set<? extends TypeElement> annotations,
			RoundEnvironment roundEnv) {		
		
		boolean nothingToDo = super.nothingToDo(annotations, roundEnv);
		
		if (nothingToDo) {
			return true;
		} else {

			try {
				boolean libraryOption = androidAnnotationsEnv.getOptionBooleanValue(AndroidManifestFinder.OPTION_LIBRARY);
				if (libraryOption) {
					final AndroidManifest androidManifest = new AndroidManifestFinder(androidAnnotationsEnv).extractAndroidManifest();
					final String libraryPackage = androidManifest.getApplicationPackage(); 
					
					DeclexConstant.ACTION = libraryPackage + ".Action";
					DeclexConstant.EVENT_PATH = libraryPackage + ".event.";
				}
			} catch (Exception e) {}
			
			//Update actions information in each round
			timeStats.start("Update Actions");
			actions.getActionsInformation();		
			timeStats.stop("Update Actions");
		}
		
		return false;
	}
	
	@Override
	protected AnnotationElementsHolder extractAnnotations(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		
		timeStats.start("Scan for Exports");
		Map<TypeElement, Set<? extends Element>> virtualAnnotatedElements = new HashMap<>();
		scanForExports(roundEnv, annotations, virtualAnnotatedElements);
		timeStats.stop("Scan for Exports");

		timeStats.start("Extract Annotations");

		final ModelExtractor modelExtractor = new ModelExtractor();
		final AnnotationElementsHolder extractedModel;

		if (!virtualAnnotatedElements.isEmpty()) {

			Set<TypeElement> completeAnnotations = new HashSet<>(annotations);
			completeAnnotations.addAll(virtualAnnotatedElements.keySet());

			for (TypeElement annotation : annotations) {
				Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
				if (virtualAnnotatedElements.containsKey(annotation)) {

					Set virtualElements = virtualAnnotatedElements.get(annotation);
					for (Element element : elements) {
						virtualElements.add(element);
					}

				} else {
					virtualAnnotatedElements.put(annotation, elements);
				}
			}


			extractedModel = modelExtractor.extract(
				completeAnnotations,
				getSupportedAnnotationTypes(),
				new RoundEnvironmentByCache(roundEnv, virtualAnnotatedElements)
			);

		} else {
			extractedModel = modelExtractor.extract(annotations, getSupportedAnnotationTypes(), roundEnv);
		}

		timeStats.stop("Extract Annotations");

		return extractedModel;
									
	}
	
	/**
	 * This method scans all the compilation units of generating annotations searching 
	 * for Actions and Beans which export methods. It creates Virtual Elements for these
	 * exported methods
	 * @param roundEnv
	 * @param annotations 
	 * @param virtualAnnotatedElements 
	 */
	private void scanForExports(RoundEnvironment roundEnv, Set<? extends TypeElement> annotations, 
			final Map<TypeElement, Set<? extends Element>> virtualAnnotatedElements) {
	
		//TODO find the best way to call the annotation dependency injection, the method below is a simple hack
		Set<String> generatingTargets = new HashSet<>();
		Set<Element> processedElements = new HashSet<>();
		{
			for (AnnotationHandler<?> annotationHandler : androidAnnotationsEnv.getGeneratingHandlers()) {
				generatingTargets.add(annotationHandler.getTarget());
			}
			
			generatingTargets.add(Exported.class.getCanonicalName());
			generatingTargets.add(UseModel.class.getCanonicalName());
			generatingTargets.add(UseEvents.class.getCanonicalName());
			generatingTargets.add(LocalDBModel.class.getCanonicalName());
			generatingTargets.add(JsonModel.class.getCanonicalName());
			generatingTargets.add(ServerModel.class.getCanonicalName());
		}
		
		//Import exported methods
		for (String generatingTarget : generatingTargets) {
			
			final TypeElement annotation = processingEnv.getElementUtils().getTypeElement(generatingTarget);
			final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
						
			//Check all the actions
			for (final Element element : elements) {
				
				if (!element.getKind().isClass()) continue;
				
				if (processedElements.contains(element)) continue;
				processedElements.add(element);

                final CompilationUnitTree compilationUnit = compilationTreeHelper.getCompilationUnitImportFromElement(element);
				if (compilationUnit == null) continue;

		    	boolean actionScanNeeded = false;
		    	for (ImportTree importTree : compilationUnit.getImports()) {
		    	
		            if (actions.isAction(importTree.getQualifiedIdentifier().toString())) {
		            	actionScanNeeded = true;
		            	
		            	//The loop continues in order to check different libraries actions  
		            } 
		            
		    	}		    	
		    			    	
		    	if (actionScanNeeded) {
		    		
	            	for (Element elem : element.getEnclosedElements()) {

	            	    compilationTreeHelper.visitElementTree(elem, new TreePathScanner<Boolean, Trees>() {

                            @Override
                            public Boolean visitIdentifier(IdentifierTree id, Trees trees) {

                                String name = id.getName().toString();

                                if (Actions.getInstance().hasActionNamed(name)) {
                                    TypeElement actionHolderElement = Actions.getInstance().getActionHolderForAction(name);
                                    final ActionFor actionForAnnotation = actionHolderElement.getAnnotation(ActionFor.class);

                                    //Only global actions can export methods
                                    if (actionForAnnotation.global()) {

                                        final int position = (int) trees.getSourcePositions()
                                                .getStartPosition(compilationUnit, id);
                                        final String actionName = name.substring(0, 1).toLowerCase() + name.substring(1) + position;

                                        scanForExports(
                                                actionHolderElement,
                                                (TypeElement)element,
                                                null,
                                                JExpr.ref(actionName),
                                                JExpr.ref(actionName),
                                                virtualAnnotatedElements,
                                                false);
                                    }
                                }

                                return super.visitIdentifier(id, trees);
                            }

                        });

	            	} //for
		    	} //if (actionScanNeeded)
		    	
		    	//Scan all the Beans used
            	for (Element elem : element.getEnclosedElements()) {
            		if (elem.getKind().isField() && elem.getAnnotation(Bean.class) != null) {
	            		TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(elem.asType().toString());
	            		
	            		if (typeElement != null) {

	            			JFieldRef beanReference = JExpr.ref(elem.getSimpleName().toString());
	            			IJExpression castedBeanReference = JExpr.cast(
	            					androidAnnotationsEnv.getJClass(TypeUtils.getGeneratedClassName(typeElement, androidAnnotationsEnv)), beanReference);
	            			
        					scanForExports(
        							typeElement, (TypeElement)element, elem,
        							beanReference, castedBeanReference, virtualAnnotatedElements, true);
	            		}
	            	}
            	}
			}
		}
	}
	
	private void scanForExports(
			TypeElement element, TypeElement enclosingElement, Element referenceElement,
			IJExpression referenceExpression,  IJExpression castedReferenceExpression,
			Map<TypeElement, Set<? extends Element>> virtualAnnotatedElements,
			boolean castToForward) {
		
		for (Element elem : element.getEnclosedElements()) {

			Export exportAnnotation = elem.getAnnotation(Export.class);
			Import importAnnotation = elem.getAnnotation(Import.class);
			if (exportAnnotation != null || importAnnotation != null) {
				
				if (elem.getKind() == ElementKind.METHOD) {
						
					//This element should be exported
					VirtualElement virtualElement = VirtualElement.from(elem);
					virtualElement.setEnclosingElement(enclosingElement);
					virtualElement.setReference(referenceElement);
					
					if (importAnnotation != null) {
						virtualElement.setReferenceExpression(castedReferenceExpression);
					} else {
						virtualElement.setReferenceExpression(referenceExpression);	
					}					
					
					for (AnnotationMirror annotation : virtualElement.getAnnotationMirrors()) {
						
						if (androidAnnotationsEnv.getSupportedAnnotationTypes().contains(annotation.getAnnotationType().toString())) {
							TypeElement annotationType = (TypeElement) annotation.getAnnotationType().asElement();
								
							Set elements = virtualAnnotatedElements.get(annotationType);							
							if (elements == null) {
								elements = new HashSet<>();
								virtualAnnotatedElements.put(annotationType, elements);
							}
							
							elements.add(virtualElement);
						}	
						
					}						
				}
				
				//Scan exported subfields beans
				if (elem.getKind().isField() && elem.getAnnotation(Bean.class) != null) {
					TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(elem.asType().toString());
            		
					IJExpression forwardExpression;
					if (castToForward) {
						forwardExpression = 
								JExpr.cast(androidAnnotationsEnv.getJClass(TypeUtils.getGeneratedClassName(element, androidAnnotationsEnv)), referenceExpression)
								     .invoke(FormatsUtils.fieldToGetter(elem.getSimpleName().toString()));
					} else {
						forwardExpression = referenceExpression.invoke(FormatsUtils.fieldToGetter(elem.getSimpleName().toString()));
					}
					
            		if (typeElement != null) {
    					scanForExports(
    							typeElement, 
    							enclosingElement, 
    							referenceElement,
    							forwardExpression,
    							castedReferenceExpression.invoke(FormatsUtils.fieldToGetter(elem.getSimpleName().toString())),
    							virtualAnnotatedElements, 
    							true);
            		}
				}
			}

		}
		
		List<? extends TypeMirror> superTypes = processingEnv.getTypeUtils().directSupertypes(element.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = processingEnv.getElementUtils().getTypeElement(type.toString());
			if (superElement == null) continue;
			if (superElement.getKind().equals(ElementKind.INTERFACE)) continue;
			if (superElement.asType().toString().equals(Object.class.getCanonicalName())) break;
			
			scanForExports(
					superElement, enclosingElement, referenceElement,
					referenceExpression, castedReferenceExpression, 
					virtualAnnotatedElements, castToForward);
		}
		
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
		
		int numberOfFiles = processResult.codeModel.countArtifacts();

		if (actions.buildActionsObject()) {
			LOGGER.debug("Generating Action Object");
			numberOfFiles++;
		}
		
		LOGGER.info("Number of files generated by DecleX: {}", numberOfFiles);
		
		if (processResult.codeModel.countArtifacts() > 0) {
			CodeModelGenerator modelGenerator = new DeclexCodeModelGenerator(
				coreVersion, 
				androidAnnotationsEnv.getOptionValue(CodeModelGenerator.OPTION_ENCODING), 
				androidAnnotationsEnv
			);
			modelGenerator.generate(processResult);
		}

		timeStats.stop("Generate Sources");
		
		timeStats.start("Save Config");				
		SharedRecords.writeEvents(processingEnv);
		SharedRecords.writeDBModels(processingEnv);				
		timeStats.stop("Save Config");

	}

}
