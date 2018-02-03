package com.dspot.declex.helper;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.helper.ADIHelper;
import org.androidannotations.helper.APTCodeModelHelper;
import org.androidannotations.helper.ModelConstants;

import com.dspot.declex.annotation.Populate;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.util.TypeUtils.ClassInformation;
import com.dspot.declex.wrapper.element.VirtualElement;


public class AfterPopulateHelper {
	
	private AndroidAnnotationsEnvironment environment;
	private ADIHelper adiHelper;
	private APTCodeModelHelper codeModelHelper;
	
	public AfterPopulateHelper(AndroidAnnotationsEnvironment environment) {
		this.environment = environment;
		adiHelper = new ADIHelper(environment);
		codeModelHelper = new APTCodeModelHelper(environment);
	}
	
	public boolean existsPopulateFieldWithElementName(Element element) {
		
		final String elementName = element.getSimpleName().toString();
		String elementNameAsMethod = elementName;
		if (element.getSimpleName().toString().substring(0, 4).matches("get[A-Z]")) {
			elementNameAsMethod = elementName.substring(3, 4).toLowerCase() + elementName.substring(4);
		}
		if (elementNameAsMethod.isEmpty()) return false;
		
		List<? extends Element> elems = element.getEnclosingElement().getEnclosedElements();
		List<Element> allElems = new LinkedList<>(elems);
		allElems.addAll(VirtualElement.getVirtualEnclosedElements(element.getEnclosingElement()));
		
		for (Element elem : allElems) {
			
			if (elem.getKind().isField() && adiHelper.hasAnnotation(elem, Populate.class)) {
				
				final String elemName = elem.getSimpleName().toString();
				
				if (elemName.equals(elementName)) {
					return true;
				}
				
				if (elemName.equals(elementNameAsMethod)) {
					return true;
				}
				
				if (elementName.startsWith(elemName)) {
					
					String fieldName = elementName.substring(elemName.length());
					if (fieldName.startsWith("_")) fieldName = fieldName.substring(1);
				
					if (hasListFields(fieldName, elem)) {
						return true;
					}
				}
			}
		}
	
		return false;
	}
	
	private ProcessingEnvironment getProcessingEnvironment() {
		return environment.getProcessingEnvironment();
	}

	private boolean hasListFields(String queryName, Element element) {
		
		ClassInformation classInformation = TypeUtils.getClassInformation(element, environment, true);
		if (classInformation.generatorElement != null) {
			for (Element field : classInformation.generatorElement.getEnclosedElements()) {
				
				final String normalizedQueryName = queryName.substring(0, 1).toLowerCase() + queryName.substring(1);
				final String fieldName = field.getSimpleName().toString();
				
				
				if (field.getKind().isField()) {
					
					if ((fieldName.equals(queryName) || fieldName.equals(normalizedQueryName))
						&& TypeUtils.isSubtype(field, List.class.getCanonicalName(), getProcessingEnvironment())) {
						return true;
					}
					
					if (queryName.startsWith(fieldName) || normalizedQueryName.startsWith(fieldName)) {
						
						if (!field.asType().getKind().isPrimitive()) {
							
							String elemType = codeModelHelper.elementTypeToJClass(field, true).fullName();
							if (elemType.endsWith(ModelConstants.generationSuffix()))
								elemType = elemType.substring(0, elemType.length() - 1);

							TypeElement fieldTypeElement = getProcessingEnvironment().getElementUtils().getTypeElement(elemType);

							if (fieldTypeElement != null
								&& !fieldTypeElement.toString().equals(String.class.getCanonicalName())) {
								
								String newQuery = null;
								if (queryName.startsWith(fieldName)) {
									newQuery = queryName.substring(fieldName.length());
								} else if (normalizedQueryName.startsWith(fieldName)) {
									newQuery = normalizedQueryName.substring(fieldName.length());
								}
								
								if (hasListFields(newQuery, field)) {
									return true;
								}
								
							}
						}
						
					}
					
				}
			}
		}
		
		return false;
	}

}
