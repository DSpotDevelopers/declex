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
package com.dspot.declex.override.helper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.helper.APTCodeModelHelper;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.GeneratedClassHolder;

import com.dspot.declex.helper.FilesCacheHelper;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;


public class DeclexAPTCodeModelHelper extends APTCodeModelHelper {
	
	private AndroidAnnotationsEnvironment environment;
	private Map<TypeMirror, String> mappedNamesForTypeMirrors = new HashMap<TypeMirror, String>();
	
	public DeclexAPTCodeModelHelper(AndroidAnnotationsEnvironment environment) {
		super(environment);
		this.environment = environment;
	}
	
	@Override
	public JMethod findAlreadyGeneratedMethod(
			ExecutableElement executableElement, GeneratedClassHolder holder, boolean checkForAction) {
		return super.findAlreadyGeneratedMethod(executableElement, holder, checkForAction);
	}
	
	@Override
	public TypeMirror getActualType(final Element element, DeclaredType enclosingClassType, GeneratedClassHolder holder) {

		if (element instanceof DeclaredType) {
			TypeMirror typeMirror = getTypeMirrorFor((DeclaredType) element, holder);
			if (typeMirror != element.asType()) return typeMirror;
		} else {
			if (element.asType() instanceof DeclaredType) {
				TypeMirror typeMirror = getTypeMirrorFor((DeclaredType) element.asType(), holder);
				if (typeMirror != element.asType()) return typeMirror;
			}
		}
		
		TypeMirror typeMirror = getTypeMirrorFor(element.asType(), holder);
		if (typeMirror != element.asType()) return typeMirror;
		
		
		return super.getActualType(element, enclosingClassType, holder);
	}
	
	private TypeMirror getTypeMirrorFor(DeclaredType declaredType, GeneratedClassHolder holder) {
		List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
		
		TypeMirror[] replacedTypeArguments = new TypeMirror[typeArguments.size()];
		
		int i = 0;
		boolean typeArgumentReplaced = false;
		for (TypeMirror typeArgument : typeArguments) {
											
			TypeMirror typeMirror = getTypeMirrorFor(typeArgument, holder);
			if (typeMirror != typeArgument) {	
				typeArgumentReplaced = true;
			}
			
			replacedTypeArguments[i++] = typeMirror; 				
		}
		
		if (typeArgumentReplaced) {
							
			return environment.getProcessingEnvironment()
					          .getTypeUtils()
					          .getDeclaredType((TypeElement) declaredType.asElement(), replacedTypeArguments);
		}
		
		return declaredType;
	}
	
	private TypeMirror getTypeMirrorFor(TypeMirror typeMirror, GeneratedClassHolder holder) {
		
		String className = typeMirror.toString();
		if (!className.contains(".") && className.endsWith(ModelConstants.generationSuffix())) {
			
			for (String generatedClass : FilesCacheHelper.getInstance().getGeneratedClasses()) {
				if (generatedClass.endsWith("." + className)) {
					TypeElement typeElement = environment.getProcessingEnvironment()
							                        .getElementUtils()
							                        .getTypeElement(generatedClass.substring(0, generatedClass.length()-1));
					TypeMirror directTypeMirror = typeElement.asType();
					mappedNamesForTypeMirrors.put(directTypeMirror, generatedClass);
					return directTypeMirror;
				}
			}
		}
		
		return typeMirror;
		
	}
	
	private AbstractJClass mappedNameForTypeMirror(TypeMirror type) {
		AbstractJClass result = environment.getJClass(mappedNamesForTypeMirrors.get(type));
		mappedNamesForTypeMirrors.remove(type);
		return result;
	}
	
	@Override
	protected AbstractJClass typeMirrorToJClass(TypeMirror type,
			Map<String, TypeMirror> substitute) {
		
		if (type instanceof DeclaredType) {
			DeclaredType declaredType = (DeclaredType) type;
			List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
			
			AbstractJClass[] replacedClasses = new AbstractJClass[typeArguments.size()];
			
			int i = 0;
			boolean typeArgumentReplaced = false;
			for (TypeMirror typeArgument : typeArguments) {
				
				AbstractJClass clazz = null;
				if (mappedNamesForTypeMirrors.containsKey(typeArgument)) {
					clazz = mappedNameForTypeMirror(typeArgument);
					typeArgumentReplaced = true;
				} else {
					String className = typeArgument.toString();
					if (!className.contains(".") && className.endsWith(ModelConstants.generationSuffix())) {			
						for (String generatedClass : FilesCacheHelper.getInstance().getGeneratedClasses()) {
							if (generatedClass.endsWith("." + className)) {
								clazz = environment.getJClass(generatedClass);
								typeArgumentReplaced = true;
								break;
							}
						}
					}
					
					if (clazz == null) {
						clazz = environment.getJClass(className);
					}
				}
				
				replacedClasses[i++] = clazz; 				
			}
			
			if (typeArgumentReplaced) {
				
				String typeName = type.toString();
				if (typeName.contains("<")) {
					typeName = typeName.substring(0, typeName.indexOf('<'));
				}
				
				AbstractJClass clazz = environment.getJClass(typeName);
				for (AbstractJClass typeArgument : replacedClasses) {
					clazz = clazz.narrow(typeArgument);
				}				
				
				return clazz;
			}
			
		}
		
		if (mappedNamesForTypeMirrors.containsKey(type)) {
			return mappedNameForTypeMirror(type);
		}
		
		String className = type.toString();
		if (!className.contains(".") && className.endsWith(ModelConstants.generationSuffix())) {			
			for (String generatedClass : FilesCacheHelper.getInstance().getGeneratedClasses()) {
				if (generatedClass.endsWith("." + className)) {
					return environment.getJClass(generatedClass);
				}
			}
		}
				
		return super.typeMirrorToJClass(type, substitute);
	}
	
	@Override
	public int elementVisibilityModifierToJMod(Element element) {
		return JMod.PUBLIC;
	}
}
