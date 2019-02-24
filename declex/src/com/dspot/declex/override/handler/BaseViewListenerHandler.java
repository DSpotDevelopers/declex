/**
 * Copyright (C) 2016-2019 DSpot Sp. z o.o
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

import com.dspot.declex.annotation.Populate;
import com.dspot.declex.annotation.UseModel;
import com.dspot.declex.handler.RunWithHandler;
import com.dspot.declex.holder.ViewsHolder;
import com.dspot.declex.holder.ViewsHolder.IdInfoHolder;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.helper.IdValidatorHelper;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.internal.virtual.VirtualElement;
import org.androidannotations.rclass.IRClass.Res;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import static com.dspot.declex.action.util.ExpressionsHelper.expressionToString;
import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;
import static com.dspot.declex.util.ParamUtils.injectParam;
import static com.dspot.declex.util.TypeUtils.isSubtype;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.direct;
import static com.helger.jcodemodel.JExpr.ref;

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
	
	protected void createDeclarationForLists(String referencedId, Map<AbstractJClass, IJExpression> declForListener,
			Element element, ViewsHolder viewsHolder) {
		
		List<? extends Element> elems = element.getEnclosingElement().getEnclosedElements();
		List<Element> allElems = new LinkedList<>(elems);
		allElems.addAll(VirtualElement.getVirtualEnclosedElements(element.getEnclosingElement()));
		
		for (Element elem : allElems) {				
			if (elem.getKind() == ElementKind.FIELD) {
				
				if (elem.getSimpleName().toString().equals(referencedId)) {
					
					Populate populator = adiHelper.getAnnotation(elem, Populate.class);
					if (populator != null && isSubtype(elem, "java.util.Collection", getProcessingEnvironment())) {
						
						String className = elem.asType().toString();
						
						Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(className);
						if (matcher.find()) {
							className = matcher.group(1);
						}
						
						if (!className.endsWith(ModelConstants.generationSuffix())) {
							if (TypeUtils.isClassAnnotatedWith(className, UseModel.class, getEnvironment())) {
								className = TypeUtils.getGeneratedClassName(className, getEnvironment());
							}
						}
						className = codeModelHelper.typeStringToClassName(className, element);
						
						//Get the model
						final JFieldRef position = ref("position");
						final AbstractJClass Model = getJClass(className);
						IJExpression modelAssigner = cast(Model, ref("parent").invoke("getItemAtPosition").arg(position));
						declForListener.put(Model, modelAssigner);
					}
					
					break;
				} else if (referencedId.startsWith(elem.getSimpleName().toString()) &&
						adiHelper.hasAnnotation(elem, Populate.class)) {
					
					String className = elem.asType().toString();
					String fieldName = elem.getSimpleName().toString();

					final boolean isPrimitive = elem.asType().getKind().isPrimitive() || 
							elem.asType().toString().equals(String.class.getCanonicalName());
					
					//Detect when the method is a List, in order to generate all the Adapters structures
					boolean isList = isSubtype(elem, CanonicalNameConstants.LIST, getProcessingEnvironment());
					if (isList) {
						Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(className);
						if (matcher.find()) {
							className = matcher.group(1);
						}
					}
					
					if (isPrimitive || isList) continue;
					
					if (className.endsWith(ModelConstants.generationSuffix())) {
						className = codeModelHelper.typeStringToClassName(className, element);
						className = className.substring(0, className.length()-1);
					}
					
					//Find all the fields and methods that are presented in the layouts
					Map<String, IdInfoHolder> fields = new HashMap<String, IdInfoHolder>();
					Map<String, IdInfoHolder> methods = new HashMap<String, IdInfoHolder>();
					viewsHolder.findFieldsAndMethods(className, fieldName, fields, methods, true);
					
					String composedField = null;
					for (String field : fields.keySet()) {
						
						final IdInfoHolder info = fields.get(field);
						if (!isSubtype(info.type.toString(), CanonicalNameConstants.COLLECTION, getProcessingEnvironment())) continue;
						if (!info.idName.equals(referencedId)) continue;
							
						composedField = "";
						for (String fieldPart : field.split("\\."))
							composedField = composedField + "." + fieldToGetter(fieldPart);
						
						className = info.type.toString();
						
						break;
					}
					
					for (String method : methods.keySet()) {
						
						final IdInfoHolder info = methods.get(method);
						if (!info.idName.equals(referencedId)) continue;
						if (!isSubtype(info.type.toString(), CanonicalNameConstants.COLLECTION, getProcessingEnvironment())) continue;
						
						composedField = "";
						String[] methodSplit = method.split("\\.");
						for (int i = 0; i < methodSplit.length-1; i++) {
							composedField = composedField + "." + fieldToGetter(methodSplit[i]);
						}
						composedField = composedField + "." + methodSplit[methodSplit.length-1];
						
						className = info.type.toString();
						
						break;
					}
					
					if (composedField == null) continue;
					
					isList = isSubtype(className, "java.util.Collection", getProcessingEnvironment());
					if (isList) {
						Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(className);
						if (matcher.find()) {
							className = matcher.group(1);
							if (className.endsWith(ModelConstants.generationSuffix())) {
								className = codeModelHelper.typeStringToClassName(className, element);
							}
						}
					} 
					
					//Get the model
					final JFieldRef position = ref("position");
					final AbstractJClass Model = getJClass(className);
					IJExpression modelAssigner = cast(Model, ref("parent").invoke("getItemAtPosition").arg(position));
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
			if (paramName.equals("refs") && isSubtype(paramType, CanonicalNameConstants.LIST, getProcessingEnvironment())) {
				
				Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(paramType);
				if (matcher.find()) {
					if (isSubtype(matcher.group(1), CanonicalNameConstants.VIEW, getProcessingEnvironment())) {
												
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
						String listItemId = listItem.substring(listItem.lastIndexOf('/') + 1);

						viewsHolder.addLayout(listItemId);
						viewsHolder.setDefLayoutId(listItemId);

						if (viewsHolder.layoutContainsId(paramName)) {

							final JFieldRef idRef = getEnvironment().getRClass().get(Res.ID).getIdStaticRef(paramName, getEnvironment());

							final String className = viewsHolder.getClassNameFromId(paramName);
							if (className.equals(CanonicalNameConstants.VIEW)) {
								invoke.arg(ref("view").invoke("findViewById").arg(idRef));
							} else {
								invoke.arg(cast(getJClass(className), ref("view").invoke("findViewById").arg(idRef)));
							}

							viewsHolder.setDefLayoutId(defLayoutId);
							continue parameters;
						}

						viewsHolder.setDefLayoutId(defLayoutId);
					}

				}

			}
			
			injectParam(paramName, paramType, invoke, viewsHolder);
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
	
}
