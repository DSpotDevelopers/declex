package com.dspot.declex.helper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.apache.commons.lang3.tuple.Pair;

public class ViewsPropertiesReaderHelper {

	//<Class Id, <Getter<Name, Classes>, Setter<Name, Classes>>>
	private Map<String, Pair<Map<String, TypeMirror>, Map<String, Set<TypeMirror>>>> gettersAndSettersPerClass = new HashMap<>();
	
	private AndroidAnnotationsEnvironment environment;
	
	private static ViewsPropertiesReaderHelper instance;
	
	public static ViewsPropertiesReaderHelper getInstance(AndroidAnnotationsEnvironment environment) {
		if (instance == null) {
			instance = new ViewsPropertiesReaderHelper(environment);
		}
		
		return instance;
	}
	
	private ViewsPropertiesReaderHelper(AndroidAnnotationsEnvironment environment) {
		this.environment = environment;
	}
	
	public void readGettersAndSetters(String fromClass, Map<String, TypeMirror> getters, Map<String, Set<TypeMirror>> setters) {
		
		Pair<Map<String, TypeMirror>, Map<String, Set<TypeMirror>>> gettersAndSetters = gettersAndSettersPerClass.get(fromClass);
		if (gettersAndSetters != null) {
			getters.putAll(gettersAndSetters.getKey());
			setters.putAll(gettersAndSetters.getValue());
			return;
		} else {
			gettersAndSetters = Pair.of(getters, setters);
			gettersAndSettersPerClass.put(fromClass, gettersAndSetters);
		}
		
		if (fromClass.contains("<")) fromClass = fromClass.substring(0, fromClass.indexOf('<'));
		
		Element classElement = processingEnv().getElementUtils().getTypeElement(fromClass);
		readGettersAndSetters(classElement, getters, setters);
		
		List<? extends TypeMirror> superTypes = processingEnv().getTypeUtils().directSupertypes(classElement.asType());
		
		for (TypeMirror type : superTypes) {
			String typeName = type.toString();
			if (typeName.contains("<")) typeName = typeName.substring(0, typeName.indexOf('<'));
				
			TypeElement superElement = processingEnv().getElementUtils().getTypeElement(typeName);
			if (superElement == null) continue;
			
			readGettersAndSetters(superElement, getters, setters);
			
			Map<String, TypeMirror> superGetters = new HashMap<>();
			Map<String, Set<TypeMirror>> superSetters = new HashMap<>();
			readGettersAndSetters(type.toString(), superGetters, superSetters);
			
			getters.putAll(superGetters);
			setters.putAll(superSetters);
		}
	}
	
	private void readGettersAndSetters(Element element, Map<String, TypeMirror> getters, Map<String, Set<TypeMirror>> setters) {
		List<? extends Element> elems = element.getEnclosedElements();
		
		for (Element elem : elems) {
			if (elem.getKind() == ElementKind.METHOD && elem.getModifiers().contains(Modifier.PUBLIC)) {

				final String elemName = elem.getSimpleName().toString();
				
				if (elemName.length() >= 4) {
					final String elemNameStart = elemName.substring(0, 4);
					final String elemNameStartForBoolean = elemName.toString().substring(0, 3);
					
					if (elemNameStart.matches("set[A-Z]")) {
					
						ExecutableElement executableElem = (ExecutableElement) elem;
						if (executableElem.getReturnType().toString().equals("void")
							&& executableElem.getParameters().size() == 1) {
							
							final String property = elemName.substring(3);
							Set<TypeMirror> types = setters.get(property);
							if (types == null) {
								types = new HashSet<>();
								setters.put(property, types);
							}
							
							types.add(executableElem.getParameters().get(0).asType());
						}
						
					}
					
					if (elemNameStart.matches("get[A-Z]") 
						|| elemNameStartForBoolean.matches("is[A-Z]")) {
						
						ExecutableElement executableElem = (ExecutableElement) elem;
						if (!executableElem.getReturnType().toString().equals("void")
							&& executableElem.getParameters().size() == 0) {
							
							final String property = elemName.substring(elemNameStart.matches("get[A-Z]") ? 3 : 2);
							getters.put(property, executableElem.getReturnType());
						}
						
					}
				}
			}
		}
	}	

	private ProcessingEnvironment processingEnv() {
		return environment.getProcessingEnvironment();
	}
}
