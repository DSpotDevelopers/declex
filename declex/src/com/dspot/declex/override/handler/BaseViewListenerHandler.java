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
package com.dspot.declex.override.handler;

import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.direct;
import static com.helger.jcodemodel.JExpr.ref;

import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.helper.IdValidatorHelper;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.rclass.IRClass.Res;

import com.dspot.declex.api.model.UseModel;
import com.dspot.declex.api.viewsinjection.Populate;
import com.dspot.declex.runwith.RunWithHandler;
import com.dspot.declex.share.holder.ViewsHolder;
import com.dspot.declex.share.holder.ViewsHolder.IdInfoHolder;
import com.dspot.declex.util.ParamUtils;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JFormatter;
import com.helger.jcodemodel.JInvocation;

public class BaseViewListenerHandler extends RunWithHandler<EComponentWithViewSupportHolder> {

	public BaseViewListenerHandler(Class<? extends Annotation> targetClass, AndroidAnnotationsEnvironment environment) {
		super(targetClass, environment);
	}
	
	@Override
	public void validate(Element element, ElementValidation valid) {
		super.validate(element, valid);
		
		validatorHelper.enclosingElementHasEnhancedViewSupportAnnotation(element, valid);

		validatorHelper.resIdsExist(element, Res.ID, IdValidatorHelper.FallbackStrategy.USE_ELEMENT_NAME, valid);

		validatorHelper.isNotPrivate(element, valid);

		if (element instanceof ExecutableElement) {
			validatorHelper.doesntThrowException(element, valid);
		}
		
	}
	
	protected void createDeclarationForLists(String referecedId, Map<AbstractJClass, IJExpression> declForListener, 
			Element element, ViewsHolder viewsHolder) {
		
		List<? extends Element> elems = element.getEnclosingElement().getEnclosedElements();
		for (Element elem : elems) {				
			if (elem.getKind() == ElementKind.FIELD) {
				
				if (elem.getSimpleName().toString().equals(referecedId)) {
					
					Populate populator = elem.getAnnotation(Populate.class);
					if (populator != null && TypeUtils.isSubtype(elem, "java.util.Collection", getProcessingEnvironment())) {
						
						String className = elem.asType().toString();
						String fieldName = elem.getSimpleName().toString();
						
						Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(className);
						if (matcher.find()) {
							className = matcher.group(1);
						}
						
						boolean castNeeded = false;
						if (!className.endsWith(ModelConstants.generationSuffix())) {
							if (TypeUtils.isClassAnnotatedWith(className, UseModel.class, getEnvironment())) {
								className = className + ModelConstants.generationSuffix();
								castNeeded = true;
							}
						}
						className = TypeUtils.typeFromTypeString(className, getEnvironment());
						
						//Get the model
						JFieldRef position = ref("position");
						IJExpression modelAssigner = ref(fieldName).invoke("get").arg(position);
						AbstractJClass Model = getJClass(className);
						if (castNeeded) modelAssigner = cast(Model, ref(fieldName).invoke("get").arg(position));
						declForListener.put(Model, modelAssigner);
					}
					
					break;
				} else 	if (referecedId.startsWith(elem.getSimpleName().toString()) && 
						    elem.getAnnotation(Populate.class)!=null) {
					
					String className = elem.asType().toString();
					String fieldName = elem.getSimpleName().toString();

					final boolean isPrimitive = elem.asType().getKind().isPrimitive() || 
							elem.asType().toString().equals(String.class.getCanonicalName());
					
					//Detect when the method is a List, in order to generate all the Adapters structures
					boolean isList = TypeUtils.isSubtype(elem, CanonicalNameConstants.LIST, getProcessingEnvironment());		
					if (isList) {
						Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(className);
						if (matcher.find()) {
							className = matcher.group(1);
						}
					}
					
					if (isPrimitive || isList) continue;
					
					if (className.endsWith("_")) {
						className = TypeUtils.typeFromTypeString(className, getEnvironment());
						className = className.substring(0, className.length()-1);
					}
					
					//Find all the fields and methods that are presented in the layouts
					Map<String, IdInfoHolder> fields = new HashMap<String, IdInfoHolder>();
					Map<String, IdInfoHolder> methods = new HashMap<String, IdInfoHolder>();
					viewsHolder.findFieldsAndMethods(className, fieldName, elem, fields, methods, true);
					
					String composedField = null;
					for (String field : fields.keySet()) {
						if (!fields.get(field).idName.equals(referecedId)) continue;
							
						composedField = "";
						for (String fieldPart : field.split("\\."))
							composedField = composedField + "." + fieldToGetter(fieldPart);
						
						className = fields.get(field).type.toString();
					}
					
					for (String method : methods.keySet()) {
						if (!methods.get(method).idName.equals(referecedId)) continue;
						
						composedField = "";
						String[] methodSplit = method.split("\\.");
						for (int i = 0; i < methodSplit.length-1; i++) {
							composedField = composedField + "." + fieldToGetter(methodSplit[i]);
						}
						composedField = composedField + "." + methodSplit[methodSplit.length-1];
						
						className = methods.get(method).type.toString();
					}
					
					if (composedField == null) continue;
					
					isList = TypeUtils.isSubtype(className, "java.util.Collection", getProcessingEnvironment());		
					if (isList) {
						Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(className);
						if (matcher.find()) {
							className = matcher.group(1);
							if (className.endsWith("_")) {
								className = TypeUtils.typeFromTypeString(className, getEnvironment());
							}
						}
					} 
					
					IJExpression assignRef = ref(fieldName);
					
					String[] methodSplit = composedField.split("\\.");
					for (int i = 0; i < methodSplit.length; i++) {
						String methodPart = methodSplit[i];
						if (!methodPart.equals("")) {
							assignRef = assignRef.invoke(methodPart);		
						}			
					}
					
					//Get the model
					JFieldRef position = ref("position");
					IJExpression modelAssigner = assignRef.invoke("get").arg(position);
					AbstractJClass Model = getJClass(className);
					declForListener.put(Model, modelAssigner);
				}

			}
		}						
	}
	
	protected boolean isList() {
		return false;
	}
	
	@Override
	public IJStatement getStatement(AbstractJClass elementClass, Element element, ViewsHolder viewsHolder, EComponentWithViewSupportHolder holder) {
		
		//The Field actions are executed by the ActionHandler
		if (!(element instanceof ExecutableElement)) {
			return super.getStatement(elementClass, element, viewsHolder, holder);	
		}
    	
		final String methodName = element.getSimpleName().toString();
		
		JInvocation invoke = JExpr.invoke(methodName);
		
		ExecutableElement exeElem = (ExecutableElement) element;
		
		parameters:
		for (VariableElement param : exeElem.getParameters()) {
			final String paramName = param.getSimpleName().toString();
			final String paramType = param.asType().toString();
			
			//Reference for fields
			if (paramName.equals("refs") && TypeUtils.isSubtype(paramType, CanonicalNameConstants.LIST, getProcessingEnvironment())) {
				
				Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(paramType);
				if (matcher.find()) {
					if (TypeUtils.isSubtype(matcher.group(1), CanonicalNameConstants.VIEW, getProcessingEnvironment())) {
												
						String invocation = Arrays.class.getCanonicalName() + ".<" + matcher.group(1) + ">asList(";
						List<String> names = getNames(element);
						for (String name : names) {
							JFieldRef fieldRef = viewsHolder.createAndAssignView(name);
							invocation = invocation + expressionToString(fieldRef) + ",";
						}
						invocation = invocation.substring(0, invocation.length() - 1) + ")";
						
						invoke.arg(direct(invocation));
				
						continue;
					}
				}				
			}
			
			if (isList()) {				
				List<String> names = getNames(element);
				if (viewsHolder.layoutContainsId(methodName)) {
					names.add(methodName);
				}
				
				for (String name : names) {
					//Read the Layout from the XML file
					org.w3c.dom.Element node = viewsHolder.getDomElementFromId(name);
					if (node != null && node.hasAttribute("tools:listitem")) {
						final String defLayoutId = viewsHolder.getDefLayoutId();
						
						String listItem = node.getAttribute("tools:listitem");
						String listItemId = listItem.substring(listItem.lastIndexOf('/')+1);
						
						viewsHolder.addLayout(listItemId);
						viewsHolder.setDefLayoutId(listItemId);
						
						if (viewsHolder.layoutContainsId(paramName)) {
							final JFieldRef idRef = getEnvironment().getRClass().get(Res.ID)
				                       .getIdStaticRef(paramName, getEnvironment());
							
							final String className = viewsHolder.getClassNameFromId(paramName);
							if (className.equals(CanonicalNameConstants.VIEW)) {
								invoke.arg(ref("view").invoke("findViewById").arg(idRef));
							} else {
								invoke.arg(cast(getJClass(className), ref("view").invoke("findViewById").arg(idRef)));
							}
							
							viewsHolder.setDefLayoutId(defLayoutId);
							continue parameters;
						};
						
						viewsHolder.setDefLayoutId(defLayoutId);
					}		
				}			
			}
			
			ParamUtils.injectParam(paramName, paramType, invoke, viewsHolder);
		}
		
		return invoke;
	}
	
	@Override
	protected List<String> getNames(Element element) {
		List<String> idsRefs = annotationHelper.extractAnnotationResources(element, Res.ID, true);
		
		List<String> names = new ArrayList<>(idsRefs.size());
		
		for (String field : idsRefs) {
			names.add(field.substring(field.lastIndexOf('.') + 1));
		}
		
		return names;
	}
	
	@Override
	protected String getClassName(Element element) {
		return null;
	}
	
	private String expressionToString(IJExpression expression) {
	    if (expression == null) {
	        throw new IllegalArgumentException("Generable must not be null.");
	    }
	    final StringWriter stringWriter = new StringWriter();
	    final JFormatter formatter = new JFormatter(stringWriter);
	    expression.generate(formatter);
	    
	    return stringWriter.toString();
	}
	
}
