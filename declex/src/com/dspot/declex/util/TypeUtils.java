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

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.helper.*;

import com.dspot.declex.helper.FilesCacheHelper;
import com.dspot.declex.wrapper.element.VirtualElement;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JAnnotationUse;
import com.helger.jcodemodel.JVar;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

public class TypeUtils {
	
	public static TypeElement getRootElement(Element element) {
		Element rootElement = element;
		while (!rootElement.getEnclosingElement().getKind().equals(ElementKind.PACKAGE)) {
			rootElement = rootElement.getEnclosingElement();
		} 
		
		return (TypeElement) rootElement;
	}
	
	public static void annotateVar(JVar var, AnnotationMirror annotationMirror, AndroidAnnotationsEnvironment env) {
		JAnnotationUse fieldAnnotation = var.annotate(env.getJClass(annotationMirror.getAnnotationType().toString()));

		Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror.getElementValues();
		for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
			String name = entry.getKey().getSimpleName().toString();
			Object value = entry.getValue().getValue();
			
			if (value instanceof Boolean)
				fieldAnnotation.param(name, (Boolean)value);
			
			if (value instanceof Byte)
				fieldAnnotation.param(name, (Byte)value);
			
			if (value instanceof Character)
				fieldAnnotation.param(name, (Character)value);
			
			if (value instanceof Class<?>)
				fieldAnnotation.param(name, (Class<?>)value);
			
			if (value instanceof Double)
				fieldAnnotation.param(name, (Double)value);
			
			if (value instanceof Enum<?>)
				fieldAnnotation.param(name, (Enum<?>)value);
			
			if (value instanceof Float)
				fieldAnnotation.param(name, (Float)value);
			
			if (value instanceof Integer)
				fieldAnnotation.param(name, (Integer)value);
			
			if (value instanceof Long)
				fieldAnnotation.param(name, (Long)value);
			
			if (value instanceof Short)
				fieldAnnotation.param(name, (Short)value);
			
			if (value instanceof String)
				fieldAnnotation.param(name, (String)value);
		}
	}
	
	public static String ___classNameFromElement(Element element, boolean useInnerClass, AndroidAnnotationsEnvironment environment) {
		
		final ProcessingEnvironment processingEnvironment = environment.getProcessingEnvironment();
		
		final TypeMirror typeMirror = element instanceof ExecutableElement? ((ExecutableElement)element).getReturnType() 
				: element.asType(); 
		
		//Determine if the class exists
		String className = typeMirror.toString();		
		
		//Remove Generic part
		if (useInnerClass) {
			Matcher matcher = Pattern.compile("(<((\\w|\\.)+)>)$").matcher(className);
			if (matcher.find()) {
				
				//Only process if it contains only one inner class
				if (!matcher.group(2).contains(",")) {
					className = matcher.group(2);
				}
			}
		}
		
		if (processingEnvironment.getElementUtils().getTypeElement(className) != null 
			|| typeMirror.getKind().isPrimitive()) {
			
			return className;
			
		}
			
		//Uses API tree to determine the element type
		final Trees trees = Trees.instance(processingEnvironment);
    	final TreePath treePath = trees.getPath(
    			element instanceof VirtualElement? ((VirtualElement)element).getElement() : element
		);
    	
    	for (ImportTree importTree : treePath.getCompilationUnit().getImports()) {
    		
			String lastElementImport = importTree.getQualifiedIdentifier().toString();
			String firstElementName = className;
			String currentVariableClass = "";
			
			int pointIndex = lastElementImport.lastIndexOf('.');
			if (pointIndex != -1) {
				lastElementImport = lastElementImport.substring(pointIndex + 1);
			}
			
			pointIndex = firstElementName.indexOf('.');
			if (pointIndex != -1) {
				firstElementName = firstElementName.substring(0, pointIndex);
				currentVariableClass = className.substring(pointIndex);
			}
			
			while (firstElementName.endsWith("[]")) {
				firstElementName = firstElementName.substring(0, firstElementName.length()-2);
				if (currentVariableClass.isEmpty()) currentVariableClass = currentVariableClass + "[]";
			}
			
			if (lastElementImport.equals(firstElementName)) {
				
				System.out.println("DD: " + className + " to " + importTree.getQualifiedIdentifier() + currentVariableClass);
				
				return importTree.getQualifiedIdentifier() + currentVariableClass;
			}
    		
    	}
    	
    	System.out.println("DD: " + className + " to " + treePath.getCompilationUnit().getPackageName() + "." + className);
    	
    	return treePath.getCompilationUnit().getPackageName() + "." + className;

	}

	public static String getGeneratedClassName(Element element, AndroidAnnotationsEnvironment environment) {
		boolean checkNested = true;
		if (element.getEnclosingElement().getKind().equals(ElementKind.PACKAGE)) checkNested = false;
		return getGeneratedClassName(element.asType().toString(), element, environment, checkNested);
	}

	public static String getGeneratedClassName(String clazz, AndroidAnnotationsEnvironment environment) {
		return getGeneratedClassName(clazz, null, environment);
	}

	public static String getGeneratedClassName(String clazz, Element referenceElement, AndroidAnnotationsEnvironment environment) {
		return getGeneratedClassName(clazz, referenceElement, environment, true);
	}

	public static String getGeneratedClassName(String clazz, AndroidAnnotationsEnvironment environment, boolean checkNested) {
		return getGeneratedClassName(clazz, null, environment, checkNested);
	}

	public static String getGeneratedClassName(String clazz, Element referenceElement, AndroidAnnotationsEnvironment environment, boolean checkNested) {

		if (clazz.trim().equals("")) return "";
		
		if (!clazz.endsWith(ModelConstants.generationSuffix())) {
			clazz = clazz + ModelConstants.generationSuffix();
		}
		
		if (!clazz.contains(".")) {
			APTCodeModelHelper codeModelHelper = new APTCodeModelHelper(environment);
			return codeModelHelper.typeStringToClassName(clazz, referenceElement);
		}		
		
		if (checkNested) {
			int lastPoint = clazz.lastIndexOf('.');
			TypeElement typeElement = environment.getProcessingEnvironment()
					                             .getElementUtils()
					                             .getTypeElement(clazz.substring(0, lastPoint));
			if (typeElement != null) {
				//It is a nested class
				clazz = clazz.substring(0, lastPoint) + ModelConstants.generationSuffix() 
						+ clazz.substring(lastPoint);  
			}
		}
		
		return clazz;
	}
	
	public static String getClassFieldValue(Element element, final String annotationName, String methodName, AndroidAnnotationsEnvironment environment) {
		TargetAnnotationHelper helper = new TargetAnnotationHelper(environment, annotationName);
		
		AnnotationMirror annotationMirror = helper.findAnnotationMirror(element, annotationName);
		Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror.getElementValues();

		for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
			/*
			 * "methodName" is unset when the default value is used
			 */
			if (methodName.equals(entry.getKey().getSimpleName().toString())) {

				AnnotationValue annotationValue = entry.getValue();
				
				String result = annotationValue + "";
				if (result.endsWith(".class")) result = result.substring(0, result.length()-6);
			
				if (result.equals("<error>")) {
					
					final StringBuilder resultBuilder = new StringBuilder();
					
					//Try to get the value using Compiler API Tree
					final Trees trees = Trees.instance(environment.getProcessingEnvironment());
					final TreePath treePath = trees.getPath(
			    			element instanceof VirtualElement? ((VirtualElement)element).getElement() : element
					);
					
		        	TreePathScanner<Object, Trees> scanner = new TreePathScanner<Object, Trees>() {
		        		
		        		private Pattern pattern = Pattern.compile("value\\s*=\\s*([a-zA-Z_][a-zA-Z_0-9.]+)\\.class$");
		        		
		        		@Override
		        		public Object visitAnnotation(AnnotationTree annotationTree,
		        				Trees trees) {
		        			
		        			if (!annotationName.endsWith("." + annotationTree.getAnnotationType().toString()))
		        				return super.visitAnnotation(annotationTree, trees);
		        			
		        			List<? extends ExpressionTree> args = annotationTree.getArguments();
		        			for (ExpressionTree arg : args) {
		        				Matcher matcher = pattern.matcher(arg.toString());
		        				if (matcher.find()) {
		        					resultBuilder.append(matcher.group(1));
		        					break;
		        				}
		        			}
		        			
		        			return super.visitAnnotation(annotationTree, trees);
		        		}
		        		
		        	};
		        	
		        	scanner.scan(treePath, trees);
		        	result = resultBuilder.toString();
				}
				
				return result;				
			}
		}
		
		return null;
	}
	
	public static boolean isSubTypeRecusive(TypeMirror potentialSubtype, TypeMirror potentialSupertype, ProcessingEnvironment processingEnv) {
		String subType = potentialSubtype.toString();
		String superType = potentialSupertype.toString();
		
		int indexGenericSubType = subType.indexOf('<');
		int indexGenericSuperType = superType.indexOf('<');
		if (indexGenericSubType != -1) subType = subType.substring(0, indexGenericSubType);
		if (indexGenericSuperType != -1) superType = superType.substring(0, indexGenericSuperType);
		
		if (subType.equals(superType)) {
			return true;
		}
		
		List<? extends TypeMirror> superTypes = processingEnv.getTypeUtils().directSupertypes(potentialSubtype);
		for (TypeMirror type : superTypes) {
			if (isSubTypeRecusive(type, potentialSupertype, processingEnv)) return true;
		}
		
		return false;
	}
	
	public static boolean isSubtype(TypeMirror potentialSubtype, TypeMirror potentialSupertype, ProcessingEnvironment processingEnv) {		
		
		//This is because isSubtype is failing with generic classes in gradle
		return isSubTypeRecusive(potentialSubtype, potentialSupertype, processingEnv);
	}

	public static boolean isSubtype(TypeMirror t1, String t2, ProcessingEnvironment processingEnv) {
		if (t2.contains("<")) t2 = t2.substring(0, t2.indexOf('<'));
		
		TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(t2);
		if (typeElement != null) {
			TypeMirror expectedType = typeElement.asType();
			return isSubtype(t1, expectedType, processingEnv);
		}
		
		return false;
	}
	
	public static boolean isSubtype(TypeElement t1, TypeElement t2, ProcessingEnvironment processingEnv) {
		return isSubtype(t1.asType(), t2.asType(), processingEnv);
	}
	
	public static boolean isSubtype(Element t1, String t2, ProcessingEnvironment processingEnv) {
		TypeMirror elementType = t1.asType();
		if (t2.contains("<")) t2 = t2.substring(0, t2.indexOf('<'));
		
		TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(t2);
		if (typeElement != null) {
			TypeMirror expectedType = typeElement.asType();
			return isSubtype(elementType, expectedType, processingEnv);
		}
		
		return false;
	}

	public static boolean isSubtype(String t1, String t2, ProcessingEnvironment processingEnv) {
		if (t1.equals(t2)) return true;
		
		if (t1.contains("<")) t1 = t1.substring(0, t1.indexOf('<'));

		TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(t1);
		if (typeElement != null) {
			return isSubtype(typeElement, t2, processingEnv);
		}
		
		return false;
	}
	
	public static boolean fieldInElement(String fieldName, Element element) {
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems)
			if (elem.getKind() == ElementKind.FIELD) {
				if (elem.getSimpleName().toString().equals(fieldName)) {
					if (elem.getModifiers().contains(Modifier.PRIVATE)) continue;
					
					return true;
				}
				
			}
		
		return false;
	}
	
	public static Element fieldElementInElement(String fieldName, Element element) {
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems)
			if (elem.getKind() == ElementKind.FIELD) {
				if (elem.getSimpleName().toString().equals(fieldName)) {
					if (elem.getModifiers().contains(Modifier.PRIVATE)) continue;
					
					return elem;
				}
				
			}
		
		return null;
	}
	
	public static boolean isClassAnnotatedWith(String className, Class<? extends Annotation> annotation, AndroidAnnotationsEnvironment environment) {
		ADIHelper adiHelper = new ADIHelper(environment);
		
		TypeElement element = environment.getProcessingEnvironment().getElementUtils().getTypeElement(className);				
		if (element == null) return false;
		
		return adiHelper.hasAnnotation(element, annotation);

	}
	
	public static <T extends Annotation> T getClassAnnotation(String className, Class<T> annotation, AndroidAnnotationsEnvironment environment) {
		ADIHelper adiHelper = new ADIHelper(environment);
		
		TypeElement element = environment.getProcessingEnvironment().getElementUtils().getTypeElement(className);				
		if (element == null) return null;
		
		return adiHelper.getAnnotation(element, annotation);		
	}
	
	public static ClassInformation getClassInformation(Element element, AndroidAnnotationsEnvironment environment) {
		return getClassInformation(element, environment, false);
	}
	
	public static ClassInformation getClassInformation(Element element, AndroidAnnotationsEnvironment environment, boolean getElement) {
		
		TypeMirror elementTypeMirror;
		if (element instanceof ExecutableElement) {
			elementTypeMirror = ((ExecutableElement) element).getReturnType();			
		} else {
			elementTypeMirror = element.asType();
		}
		String className = elementTypeMirror.toString();
		
		final boolean isList = TypeUtils.isSubtype(elementTypeMirror, CanonicalNameConstants.LIST, environment.getProcessingEnvironment());		
		if (isList) {
			Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(className);
			if (matcher.find()) {
				className = matcher.group(1);
			}
		}
		
		String originalClassName = className;
		if (className.endsWith(ModelConstants.generationSuffix())) {
			APTCodeModelHelper codeModelHelper = new APTCodeModelHelper(environment);
			className = codeModelHelper.elementTypeToJClass(element, true).fullName();
			originalClassName = className;
			className = className.substring(0, className.length()-1);
		}
		
		TypeElement typeElement = null;
		if (getElement) {
			typeElement = environment.getProcessingEnvironment().getElementUtils().getTypeElement(className);
			
			if (typeElement == null && className.contains(".")) {
				//Check if it is an inner class
				String prevClass = className.substring(0, className.lastIndexOf("."));
				if (prevClass.endsWith(ModelConstants.generationSuffix())) {
					className = prevClass.substring(0, prevClass.length()-1) + "." + className.substring(className.lastIndexOf(".") + 1);
					typeElement = environment.getProcessingEnvironment().getElementUtils().getTypeElement(className);
				}
			}

		}
		
		return new ClassInformation(isList, className, originalClassName, typeElement);
	}
	
	public static class ClassInformation {
		public boolean isList;
		public String generatorClassName;
		public String originalClassName;
		public TypeElement generatorElement;
		
		public ClassInformation(boolean isList, String className,
				String originalClassName, TypeElement generatorElement) {
			super();
			this.isList = isList;
			this.generatorClassName = className;
			this.originalClassName = originalClassName;
			this.generatorElement = generatorElement;
		}
		
		public String getGeneratorClassName() {
			return generatorClassName;
		}
		
		public String getOriginalClassName() {
			return originalClassName;
		}
		
		public boolean getList() {
			return this.isList;
		}
		
		public boolean isList() {
			return this.isList;
		}
		
		@Override
		public String toString() {
			return "[gen: " + generatorClassName + ", elem: " + generatorElement 
					+ ", isList: " + isList + ", original: " + originalClassName + "]";
		}
	}

}
