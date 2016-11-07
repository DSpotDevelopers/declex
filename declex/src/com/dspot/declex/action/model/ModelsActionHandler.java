/**
 * Copyright (C) 2016 DSpot Sp. z o.o
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
package com.dspot.declex.action.model;

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.holder.EComponentWithViewSupportHolder;

import com.dspot.declex.action.BaseActionHandler;
import com.dspot.declex.action.sequence.BaseListActionHandler.ElementContainer;
import com.dspot.declex.api.action.model.BaseModelAction;
import com.dspot.declex.api.action.model.GetAndPutModel;
import com.dspot.declex.api.action.model.GetModel;
import com.dspot.declex.api.action.model.PutAndGetModel;
import com.dspot.declex.api.action.model.PutModel;
import com.dspot.declex.api.model.Model;
import com.dspot.declex.api.util.FormatsUtils;
import com.dspot.declex.model.ModelHolder;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;

public class ModelsActionHandler extends BaseActionHandler {
	
	@Override
	public boolean canProcessElement(Element element, AndroidAnnotationsEnvironment environment) {
		super.canProcessElement(element, environment);
		
		String elementClass = element.asType().toString();
		try {
			Class<?> clazz = getClass().getClassLoader().loadClass(elementClass);
			clazz.asSubclass(BaseModelAction.class);
			
			return true;
		} catch (Exception e) {
		};
		
		return false;
	}

	@Override
	public void validate(String[] parameters, Element element, ElementValidation valid) {
		super.validate(parameters, element, valid);
		
		boolean fieldProvided = false;
		for (String value : parameters) {
			if (value.equals("")) continue;
			
			char lastChar = value.charAt(value.length()-1);
			if (lastChar == ';' || lastChar == '?' || lastChar == '!') {
				continue;
			}
			
			Matcher matcher = Pattern.compile("\\A\\s*(\\w+)\\s*=").matcher(value);
			if (matcher.find()) {
				String param = matcher.group(1);
				//TODO validate parameters
			} else {
				fieldProvided = true;
				
				//The parameter is a field
				List<? extends Element> elems = element.getEnclosingElement().getEnclosedElements();
				
				Element found = null;
				for (Element elem : elems) {
					if (!elem.getSimpleName().toString().equals(value)) continue;
					found = elem;
					break;
				}
				
				if (found == null) {
					valid.addError(
							element instanceof ElementContainer ? ((ElementContainer)element).getElement() : element, 
							"Unknown field \"" + value + "\"."
						);
				} else {
					if (found.getAnnotation(Model.class) == null) {
						valid.addError(
								element instanceof ElementContainer ? ((ElementContainer)element).getElement() : element, 
								"The provided field \"" + value + "\" isn't annotated with @Model"
							);
					}
				}
			}
		}
		
		if (!fieldProvided) {
			valid.addError(
					element instanceof ElementContainer ? ((ElementContainer)element).getElement() : element, 
					"You should provide a valid @Model annotated field name, ref: " + Arrays.asList(parameters)
				);
		}
	}
	
	@Override
	protected IJStatement getStatement(AbstractJClass ModelClass, JFieldRef runnableRef, String[] parameters,
			Element element, EComponentWithViewSupportHolder holder) {
		
		JBlock invokes = new JBlock();
		ModelHolder modelHolder = holder.getPluginHolder(new ModelHolder(holder));
		
		String elementClass = element.asType().toString();
		try {
			Class<?> clazz = getClass().getClassLoader().loadClass(elementClass);
			
			IJExpression query = null;
			IJExpression orderBy = null;
			JInvocation invocation = null;
			
			IJExpression putQuery = null;
			IJExpression getQuery = null;
			IJExpression putOrderBy = null;
			IJExpression getOrderBy = null;
			
			for (String value : parameters) {
				if (value.equals("")) continue;
				
				char lastChar = value.charAt(value.length()-1);
				if (lastChar == ';' || lastChar == '?' || lastChar == '!') {
					continue;
				}
								
				Matcher matcher = Pattern.compile("\\A\\s*(\\w+)\\s*=").matcher(value);
				if (matcher.find()) {
					String assignment = value.substring(matcher.group(0).length());
					String param = matcher.group(1);
					
					if (param.equals("query")) {
						query = FormatsUtils.expressionFromString(assignment);
					}
					
					if (param.equals("orderBy")) {
						orderBy = FormatsUtils.expressionFromString(assignment);
					}
					
					if (param.equals("putQuery")) {
						putQuery = FormatsUtils.expressionFromString(assignment);
					}
					
					if (param.equals("putOrderBy")) {
						putOrderBy = FormatsUtils.expressionFromString(assignment);
					}
					
					if (param.equals("getQuery")) {
						getQuery = FormatsUtils.expressionFromString(assignment);
					}
					
					if (param.equals("getOrderBy")) {
						getOrderBy = FormatsUtils.expressionFromString(assignment);
					}
				} else {
					List<? extends Element> elems = element.getEnclosingElement().getEnclosedElements();
					
					for (Element elem : elems) {
						if (!elem.getSimpleName().toString().equals(value)) continue;
						
						Model modelAnnotation = elem.getAnnotation(Model.class);
						if (modelAnnotation != null) {
							if (query==null) 
								query = FormatsUtils.expressionFromString(modelAnnotation.query());
							if (orderBy==null) 
								orderBy = FormatsUtils.expressionFromString(modelAnnotation.orderBy());
							
							try {
								clazz.asSubclass(GetModel.class);
								
								IJExpression contextExpr = holder.getContextRef();
								if (contextExpr == _this()) {
									contextExpr = holder.getGeneratedClass().staticRef("this");
								}
								
								if (invocation != null) invocation.arg(_null());
								invocation = invokes.invoke(modelHolder.getGetModelMethod(elem))
										            .arg(contextExpr).arg(query).arg(orderBy);
							} catch (Exception e) {};
														
							try {
								clazz.asSubclass(PutModel.class);
																
								if (invocation != null) invocation.arg(_null());
								invocation = invokes.invoke(modelHolder.getPutModelMethod(elem))
										            .arg(query).arg(orderBy);
							} catch (Exception e) {};
							
							try {
								clazz.asSubclass(GetAndPutModel.class);
								
								IJExpression contextExpr = holder.getContextRef();
								if (contextExpr == _this()) {
									contextExpr = holder.getGeneratedClass().staticRef("this");
								}
								
								JDefinedClass annonimousRunnable = environment.getCodeModel().anonymousClass(Runnable.class);
								JMethod annonimousRunnableRun = annonimousRunnable.method(JMod.PUBLIC, environment.getCodeModel().VOID, "run");
								annonimousRunnableRun.annotate(Override.class);
								
								if (invocation != null) invocation.arg(_null());
								invokes.invoke(modelHolder.getGetModelMethod(elem))
								    .arg(contextExpr)
								 	.arg(getQuery == null ? query : getQuery)
								 	.arg(getOrderBy == null ? orderBy : getOrderBy)
								 	.arg(_new(annonimousRunnable));
								
								invocation = annonimousRunnableRun.body()
										.invoke(modelHolder.getPutModelMethod(elem))
										.arg(putQuery == null ? query : putQuery)
										.arg(putOrderBy == null ? orderBy : putOrderBy);
								
							} catch (Exception e) {};
							
							try {
								clazz.asSubclass(PutAndGetModel.class);
								
								IJExpression contextExpr = holder.getContextRef();
								if (contextExpr == _this()) {
									contextExpr = holder.getGeneratedClass().staticRef("this");
								}
								
								JDefinedClass annonimousRunnable = environment.getCodeModel().anonymousClass(Runnable.class);
								JMethod annonimousRunnableRun = annonimousRunnable.method(JMod.PUBLIC, environment.getCodeModel().VOID, "run");
								annonimousRunnableRun.annotate(Override.class);

								if (invocation != null) invocation.arg(_null());
								invokes.invoke(modelHolder.getPutModelMethod(elem))
									.arg(putQuery == null ? query : putQuery).arg(putOrderBy == null ? orderBy : putOrderBy)
									.arg(_new(annonimousRunnable));
								
								invocation = annonimousRunnableRun.body()
									.invoke(modelHolder.getGetModelMethod(elem))
									.arg(contextExpr)
								 	.arg(getQuery == null ? query : getQuery)
								 	.arg(getOrderBy == null ? orderBy : getOrderBy);
																
							} catch (Exception e) {};
						}
						
						break;
					}
					
					
				}
			}
		
			if (invocation != null) invocation.arg(runnableRef != null ? runnableRef : _null());
			
		} catch (Exception e) {};
		
		return invokes;
	}

}
