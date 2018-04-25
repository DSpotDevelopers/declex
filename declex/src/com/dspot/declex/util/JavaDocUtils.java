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
package com.dspot.declex.util;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

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
			
			reference = reference + parentElementType + "#" + elementName;
			
			if (element.getKind().equals(ElementKind.METHOD)) {
				
				ExecutableElement executableElement = (ExecutableElement) element;
				
				reference = reference + "(";
				for (int i = 0; i < executableElement.getParameters().size(); i++) {
					VariableElement param  = executableElement.getParameters().get(i); 
					reference = reference + param.asType().toString();
					if (i < executableElement.getParameters().size()) {
						reference = reference + ",";
					}
				}
				reference = reference + ")";
				
					
			} 
			
			reference = reference + " " + parentElementName + "#" + elementName + "}";
		}
		
		return reference;
	}
	
	public static String referenceFromClassName(String className) {
		int index = className.lastIndexOf('.');	
		return "Created by {@link " + className + " " + className.substring(index+1) + "}";
	}
}
