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
package com.dspot.declex.viewsinjection;

import static com.helger.jcodemodel.JExpr.ref;
import static com.helger.jcodemodel.JExpr._null;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;

import com.dspot.declex.api.viewsinjection.AdapterClass;
import com.dspot.declex.plugin.JClassPlugin;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.AbstractJType;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;

public class AdapterClassHandler extends BaseAnnotationHandler<EComponentHolder> implements JClassPlugin {
	
	protected static final Logger LOGGER = LoggerFactory.getLogger(AdapterClassHandler.class);
	
	public AdapterClassHandler(AndroidAnnotationsEnvironment environment) {
		super(AdapterClass.class, environment);
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		String classField = TypeUtils.getClassFieldValue(element, getTarget(), "value", getEnvironment());
		if (classField == null) {
			valid.addError("The provided class isn't a valid class");
		}
		
		boolean isList = TypeUtils.isSubtype(element, "java.util.Collection", getProcessingEnvironment());		
		if (!isList) {
			valid.addError("This annotation shoud be used only on @Populate for an AdapterView");
		} 
	}

	@Override
	public void process(Element element, EComponentHolder holder)
			throws Exception {
	}

	@Override
	public void process(Element element, JDefinedClass AdapterClass) {
		if (element.getAnnotation(AdapterClass.class) == null) return;

		String classField = TypeUtils.getClassFieldValue(element, getTarget(), "value", getEnvironment());
		TypeElement typeElement = getProcessingEnvironment().getElementUtils().getTypeElement(classField);
		
		if (typeElement == null) {
			LOGGER.error("Error getting the TypeElement", element, element.getAnnotation(AdapterClass.class));
			return;
		}
		
		//Get all the fields and methods
		List<? extends Element> elems = typeElement.getEnclosedElements();
		for (Element elem : elems) {
			final String elemName = elem.getSimpleName().toString();
			
			if (elem.getKind() == ElementKind.METHOD) {
				ExecutableElement executableElement = (ExecutableElement) elem;
				
				List<? extends VariableElement> params = executableElement.getParameters();
				
				if (elemName.equals("inflate")) {
					
					if (params.size() > 0 && params.size() < 4) {
						
						boolean isViewAdapter = false;
						JMethod inflaterMethod = AdapterClass.getMethod("inflate", new AbstractJType[]{getCodeModel().INT, getClasses().VIEW_GROUP, getClasses().LAYOUT_INFLATER});
						if (inflaterMethod == null) {
							inflaterMethod = AdapterClass.getMethod("inflate", new AbstractJType[]{getCodeModel().INT, getClasses().VIEW, getClasses().VIEW_GROUP, getClasses().LAYOUT_INFLATER});
							isViewAdapter = true;
						}
							
						
						//Remove previous method body
						codeModelHelper.removeBody(inflaterMethod);
						
						JFieldRef position = ref("position");
						JFieldRef viewType = ref("viewType");
						JFieldRef parent = ref("parent");
						JFieldRef convertView = ref("convertView");
						JFieldRef inflater = ref("inflater");

						JInvocation invoke = JExpr._super().invoke(inflaterMethod);
						for (VariableElement param : params) {
							if (param.asType().toString().equals("int")) {
								if (isViewAdapter) invoke = invoke.arg(position);
								else invoke = invoke.arg(viewType);
							} else
							
							if (isViewAdapter && param.asType().toString().equals(getClasses().VIEW.fullName())) {
								invoke = invoke.arg(convertView);
							} else
								
							if (param.asType().toString().equals(getClasses().VIEW_GROUP.fullName())) {
								invoke = invoke.arg(parent);
							} else
							
							if (param.asType().toString().equals(getClasses().LAYOUT_INFLATER.fullName())) {
								invoke = invoke.arg(inflater);
							} else					
								
							{
								invoke = invoke.arg(_null());
							}
						}
						
						inflaterMethod.body()._return(invoke);
					}
					
					return;				
				}				
			}
		}
		
	}

	@Override
	public AbstractJClass getBaseAdapter(Element element) {
		if (element.getAnnotation(AdapterClass.class) == null) return null;
		
		String classField = TypeUtils.getClassFieldValue(element, getTarget(), "value", getEnvironment());
		return getJClass(classField);
	}

}