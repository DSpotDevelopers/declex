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

import static com.helger.jcodemodel.JExpr.invoke;
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

import com.dspot.declex.annotation.*;
import com.dspot.declex.helper.FilesCacheHelper;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.export.NonExport;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.EComponentHolder;

import org.androidannotations.annotations.export.Exported;
import com.dspot.declex.helper.AfterPopulateHelper;
import com.dspot.declex.helper.FilesCacheHelper.FileDependency;
import com.dspot.declex.helper.FilesCacheHelper.FileDetails;
import com.dspot.declex.override.helper.DeclexAPTCodeModelHelper;
import com.dspot.declex.util.TypeUtils;
import org.androidannotations.internal.virtual.VirtualElement;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JVar;

public class ExportedHandler extends BaseAnnotationHandler<EComponentHolder> {
	
	public ExportedHandler(AndroidAnnotationsEnvironment environment) {
		this(Exported.class, environment);
		
		codeModelHelper = new DeclexAPTCodeModelHelper(environment);
	}

	public ExportedHandler(Class<? extends Annotation> targetClass, AndroidAnnotationsEnvironment environment) {
		super(targetClass, environment);
	}
	
	@Override
	public void getDependencies(Element element, Map<Element, Object> dependencies) {
		
		//Exported in the super class will inject through ADI all the export methods
		if (element.getKind().equals(ElementKind.CLASS)) {
			
			AfterPopulateHelper afterPopulateHelper = new AfterPopulateHelper(getEnvironment());
			
			dependencies.put(element, EBean.class);
			
			List<? extends Element> elems = element.getEnclosedElements();
			for (Element elem : elems) {
		
				if (elem.getModifiers().contains(Modifier.STATIC)) continue;
				if (elem.getModifiers().contains(Modifier.ABSTRACT)) continue;
				if (elem.getAnnotation(NonExport.class) != null) continue;
								
				if (elem.getKind() == ElementKind.METHOD) {
				
					if (!elem.getModifiers().contains(Modifier.PUBLIC)) continue;
					
					if (elem.getAnnotation(AfterInject.class) != null) continue;
					if (elem.getAnnotation(ExportPopulate.class) != null) continue;
					if (elem.getAnnotation(ExportRecollect.class) != null) continue;
					
					if (elem.getAnnotation(Populate.class) != null) {
						if (!afterPopulateHelper.existsPopulateFieldWithElementName(elem)) {
							dependencies.put(elem, ExportPopulate.class);
							continue;
						}						
					}
					
					if (elem.getAnnotation(Recollect.class) != null) {
						dependencies.put(elem, ExportRecollect.class);
						continue;
					}
					
					boolean externalMethod = adiHelper.hasAnnotation(elem, Exported.class);
					if (!externalMethod) {
						List<? extends AnnotationMirror> annotations = elem.getAnnotationMirrors();
						for (AnnotationMirror annotation : annotations) {
							if (getEnvironment().getSupportedAnnotationTypes()
									            .contains(annotation.getAnnotationType().toString()))
							{
								dependencies.put(elem, Exported.class);
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
										dependencies.put(param, Exported.class);
										break;
									}
								}
								
							}
						}
					}
					
				} else {
					
					if (elem.getKind().isField()) {
						if (elem.getAnnotation(Populate.class) != null) {
							dependencies.put(elem, ExportPopulate.class);
						}				
						if (elem.getAnnotation(Recollect.class) != null) {
							dependencies.put(elem, ExportRecollect.class);
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
				valid.addError("You cannot use @Exported in an @AfterInject method");
				return;
			}
			
			if (element.getModifiers().contains(Modifier.STATIC)) {
				valid.addError("You cannot use @Exported in a static element");
				return;
			}
			
			if (!element.getModifiers().contains(Modifier.PUBLIC)) {
				valid.addError("You can use @Exported only on public methods");
				return;
			}

			//TODO
			//Now the rootElement generated class depends on this element
			final Element rootElement = TypeUtils.getRootElement(element);
			final String generatedRootElementClass = TypeUtils.getGeneratedClassName(rootElement, getEnvironment());
			final FilesCacheHelper filesCacheHelper = FilesCacheHelper.getInstance();

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
						
				if (method.type().fullName().equals("void")) {
					method.body()._if(ref(referenceName).neNull())._then().add(invocation);
				} else {
					method.body()._if(ref(referenceName).neNull())._then()._return(invocation);					
					method.body()._return(getDefault(method.type().fullName()));
				}
						
						                              ;
				for (JVar param : method.params()) {
					invocation.arg(ref(param.name()));
				}
				
			}
		}
		
		
	}
	
	private IJExpression getDefault(String type) {
		switch (type) {
		case "int":
			return JExpr.lit(0);
		case "float":
			return JExpr.lit(0f);
		case "double":
			return JExpr.lit(0d);
		case "long":
			return JExpr.lit(0L);
		case "short":
			return JExpr.lit((short) 0);
		case "char":
			return JExpr.lit((char) 0);
		case "byte":
			return JExpr.lit((byte) 0);
		case "boolean":
			return JExpr.lit(false);

		default:
			return JExpr._null();
		}
	}

}
