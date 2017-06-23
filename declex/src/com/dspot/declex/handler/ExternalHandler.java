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
package com.dspot.declex.handler;

import static com.helger.jcodemodel.JExpr.FALSE;
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.lit;
import static com.helger.jcodemodel.JExpr.ref;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EComponentHolder;

import com.dspot.declex.annotation.External;
import com.dspot.declex.annotation.ExternalPopulate;
import com.dspot.declex.annotation.ExternalRecollect;
import com.dspot.declex.annotation.NonExternal;
import com.dspot.declex.annotation.Populate;
import com.dspot.declex.annotation.Recollect;
import com.dspot.declex.helper.FilesCacheHelper.FileDependency;
import com.dspot.declex.helper.FilesCacheHelper.FileDetails;
import com.dspot.declex.override.helper.DeclexAPTCodeModelHelper;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.wrapper.element.VirtualElement;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JVar;

public class ExternalHandler extends BaseAnnotationHandler<EComponentHolder> {
	
	public ExternalHandler(AndroidAnnotationsEnvironment environment) {
		this(External.class, environment);
		
		codeModelHelper = new DeclexAPTCodeModelHelper(environment);
	}

	public ExternalHandler(Class<? extends Annotation> targetClass, AndroidAnnotationsEnvironment environment) {
		super(targetClass, environment);
	}
	
	@Override
	public void getDependencies(Element element, Map<Element, Object> dependencies) {
		
		//External in the super class will inject through ADI all the external methods
		if (element.getKind().equals(ElementKind.CLASS)) {
			
			dependencies.put(element, EBean.class);
			
			List<? extends Element> elems = element.getEnclosedElements();
			for (Element elem : elems) {
		
				if (elem.getModifiers().contains(Modifier.STATIC)) continue;
				if (elem.getModifiers().contains(Modifier.ABSTRACT)) continue;
				if (elem.getAnnotation(NonExternal.class) != null) continue;
								
				if (elem.getKind() == ElementKind.METHOD) {
				
					if (!elem.getModifiers().contains(Modifier.PUBLIC)) continue;
					
					if (elem.getAnnotation(AfterInject.class) != null) continue;
					if (elem.getAnnotation(ExternalPopulate.class) != null) continue;
					if (elem.getAnnotation(ExternalRecollect.class) != null) continue;
					
					if (elem.getAnnotation(Populate.class) != null) {
						dependencies.put(elem, ExternalPopulate.class);
						continue;
					}
					
					if (elem.getAnnotation(Recollect.class) != null) {
						dependencies.put(elem, ExternalRecollect.class);
						continue;
					}
					
					boolean externalMethod = adiHelper.hasAnnotation(elem, External.class);
					if (!externalMethod) {
						List<? extends AnnotationMirror> annotations = elem.getAnnotationMirrors();
						for (AnnotationMirror annotation : annotations) {
							if (getEnvironment().getSupportedAnnotationTypes()
									            .contains(annotation.getAnnotationType().toString()))
							{
								dependencies.put(elem, External.class);
								externalMethod = true;
								break;
							}
						}
					}
					
					if (externalMethod) {
						List<? extends Element> params = ((ExecutableElement)elem).getParameters();
						for (Element param : params) {
							
							if (param.getKind() == ElementKind.PARAMETER) {
								
								List<? extends AnnotationMirror> annotations = param.getAnnotationMirrors();
								
								for (AnnotationMirror annotation : annotations) {
									if (getEnvironment().getSupportedAnnotationTypes()
											            .contains(annotation.getAnnotationType().toString()))
									{
										dependencies.put(param, External.class);
										break;
									}
								}
								
							}
						}
					}
					
				} else {
					
					if (elem.getKind().isField()) {
						if (elem.getAnnotation(Populate.class) != null) {
							dependencies.put(elem, ExternalPopulate.class);
						}				
						if (elem.getAnnotation(Recollect.class) != null) {
							dependencies.put(elem, ExternalRecollect.class);
						}
					} 
					
				}
			}
		}
	}
	
	@Override
	public void validate(final Element element, final ElementValidation valid) {
		
		if (element.getKind() == ElementKind.METHOD) {
			if (element.getAnnotation(AfterInject.class) != null) {
				valid.addError("You cannot use @External in an @AfterInject method");
				return;
			}
			
			if (element.getModifiers().contains(Modifier.STATIC)) {
				valid.addError("You cannot use @External in a static element");
				return;
			}
			
			if ((element instanceof ExecutableElement) && !element.getModifiers().contains(Modifier.PUBLIC)) {
				valid.addError("You can use @External only on public methods");
				return;
			}

			//TODO
			//Now the rootElement generated class depends on this element
			final Element rootElement = TypeUtils.getRootElement(element);
			final String generatedRootElementClass = TypeUtils.getGeneratedClassName(rootElement, getEnvironment());
			
			System.out.println("XX: " + generatedRootElementClass);
			if (filesCacheHelper.hasCachedFile(generatedRootElementClass)) {
				
				FileDetails details = filesCacheHelper.getFileDetails(generatedRootElementClass);
				System.out.println("XY: " + details);
				
				FileDependency dependency = filesCacheHelper.getFileDependency(((VirtualElement)element).getElement().getEnclosingElement().asType().toString());
				
				if (!details.dependencies.contains(dependency)) {		
					System.out.println("XZ: " + dependency);
					
					details.invalidate();
					valid.addError("Please rebuild the project to update the cache");
				}			
			}
			
			filesCacheHelper.addGeneratedClass(
					generatedRootElementClass, 
					((VirtualElement)element).getElement().getEnclosingElement()
				);
	
		}
	}
	
	@Override
	public void process(Element element, EComponentHolder holder) {
		
		if (element instanceof VirtualElement) {
			
			if (element instanceof ExecutableElement) {
			
				final String referenceName = ((VirtualElement) element).getReference().getSimpleName().toString();
				final ExecutableElement executableElement = (ExecutableElement) element;
				
				//Check if the method exists in the super class, in this case super should be called
				boolean placeOverrideAndCallSuper = false;
				List<? extends Element> elems = element.getEnclosingElement().getEnclosedElements();
				ELEMENTS: for (Element elem : elems) {
					if (elem instanceof ExecutableElement) {
						ExecutableElement executableElem = (ExecutableElement) elem;
						
						if (elem.getSimpleName().toString().equals(element.getSimpleName().toString())
							&& executableElem.getParameters().size() == executableElement.getParameters().size()) {
							
							for (int i = 0; i < executableElem.getParameters().size(); i++) {
								VariableElement paramElem = executableElem.getParameters().get(i);
								VariableElement paramElement = executableElement.getParameters().get(i);
								
								if (!paramElem.asType().toString().equals(paramElement.asType().toString())) {
									continue ELEMENTS;
								}
							}
							
							placeOverrideAndCallSuper = true;
							break;
							
						}
					}
				}
				
				final JMethod method = codeModelHelper.overrideAnnotatedMethod(executableElement, holder, false, placeOverrideAndCallSuper);								
				final JInvocation invocation = invoke(ref(referenceName), method);
						
				if (method.type().fullName().toString().equals("void")) {
					method.body()._if(ref(referenceName).neNull())._then().add(invocation);
				} else {
					method.body()._if(ref(referenceName).neNull())._then()._return(invocation);
					
					if (method.type().fullName().equals("boolean")) method.body()._return(FALSE);
					else if (method.type().fullName().contains(".") 
							|| method.type().fullName().endsWith(ModelConstants.generationSuffix())) 
					        {method.body()._return(_null());} 
					else method.body()._return(lit(0));
				}
						
						                              ;
				for (JVar param : method.params()) {
					invocation.arg(ref(param.name()));
				}
				
			}
		}
		
		
	}

}
