package com.dspot.declex.util;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

public class JavaDocUtils {
	
	public static String referenceFromElement(Element element) {
		final String elementType = element.asType().toString();
		final String elementName = element.getSimpleName().toString();
		
		
		String reference = "Created by {@link "; 
		if (element.getKind().equals(ElementKind.CLASS)) {
			reference = reference + elementType + " " + elementName + "}";
		} else {
			final String parentElementType = element.getEnclosingElement().asType().toString();
			final String parentElementName = element.getEnclosingElement().getSimpleName().toString();
			
			reference = reference + parentElementType + "#" + elementName 
					+ " " + parentElementName + "#" + elementName + "}";
		}
		
		return reference;
	}
	
	public static String referenceFromClassName(String className) {
		int index = className.lastIndexOf('.');	
		return "Created by {@link " + className + " " + className.substring(index+1) + "}";
	}
}
