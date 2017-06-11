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
package com.dspot.declex.handler.base;

import static com.helger.jcodemodel.JExpr._super;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.ref;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;

import com.dspot.declex.annotation.External;
import com.dspot.declex.annotation.RunWith;
import com.dspot.declex.helper.EventsHelper;
import com.dspot.declex.holder.ViewsHolder;
import com.dspot.declex.holder.ViewsHolder.WriteInBlockWithResult;
import com.dspot.declex.holder.view_listener.IStatementCreator;
import com.dspot.declex.holder.view_listener.ViewListenerHolder;
import com.dspot.declex.util.SharedRecords;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.wrapper.element.VirtualElement;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;

public abstract class BaseEventHandler<T extends EComponentHolder> extends BaseAnnotationHandler<T> {

	protected static final Logger LOGGER = LoggerFactory.getLogger(BaseEventHandler.class);
	
	private String referecedId;
	
	protected EventsHelper eventsHelper;
	
	public BaseEventHandler(Class<?> targetClass, AndroidAnnotationsEnvironment environment) {
		super(targetClass, environment);
		eventsHelper = EventsHelper.getInstance(environment);
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		
		validatorHelper.enclosingElementHasEnhancedComponentAnnotation(element, valid);
		
	}
	
	protected List<String> getNames(Element element) {
		return Arrays.asList(element.getSimpleName().toString());
	}	
	
	protected ViewListenerHolder getListenerHolder(String elementName, String elementClass, Map<AbstractJClass, IJExpression> declForListener, 
			Element element, ViewsHolder viewsHolder, T holder) {
		
		//Check if it is an event, only permitted in fields
		if (element.getKind().equals(ElementKind.FIELD) && referecedId.startsWith("on")) {
			String eventClassName = SharedRecords.getEvent(referecedId.substring(2), getEnvironment()); 
			
			if (eventClassName != null) {
				JBlock eventBlock = eventsHelper.addEventListener(eventClassName, element.getEnclosingElement(), viewsHolder);
				
				IJStatement statement = getStatement(getJClass(eventClassName), element, viewsHolder, holder);
				if (statement != null) {
					eventBlock.add(statement);
				}
				
				return null;
			}
		}
		
		//Fallback as Method call
		methodHandler(elementClass, referecedId, element, viewsHolder, holder);
		return null;		
	}
	
	protected String getClassName(Element element) {
		return TypeUtils.getGeneratedClassName(element, getEnvironment());
	}

	@Override
	public void process(Element element, final T holder) {
		
		ViewsHolder viewsHolder = null;
		if (holder instanceof EComponentWithViewSupportHolder) {
			viewsHolder = holder.getPluginHolder(
					new ViewsHolder((EComponentWithViewSupportHolder) holder, annotationHelper)
				);		
		}
		final Map<AbstractJClass, IJExpression> declForListener = new HashMap<>();
		final String elementClass = getClassName(element);
		
		for (final String elementName : getNames(element)) { 
			
			referecedId = elementName;
			
			final ViewListenerHolder listenerHolder = 
					getListenerHolder(elementName, elementClass, declForListener, element, viewsHolder, holder);
			if (listenerHolder == null) continue;
			
			if (viewsHolder != null) {
				for (AbstractJClass declClass : declForListener.keySet()) {
					listenerHolder.addDecl(referecedId, JMod.FINAL, declClass, "model", declForListener.get(declClass));
				}
				
				listenerHolder.addStatement(
						referecedId, 
						new StatementCreator(elementClass==null ? null : getJClass(elementClass), element, viewsHolder, holder)
					);
			
				//If it's found the the class associated layout, then process the event here	
				if (viewsHolder.layoutContainsId(referecedId)) {				
					viewsHolder.createAndAssignView(referecedId, new WriteInBlockWithResult<JBlock>() {
		
						@Override
						public void writeInBlock(String viewName, AbstractJClass viewClass, JFieldRef view, JBlock block) {
							listenerHolder.createListener(viewName, block);
						}
					});
				}				
			}
			
		}
	}
	
	private boolean methodHandler(String elementClass, String elementName, Element element, ViewsHolder viewsHolder, T holder) {
		
		//See if the method exists in the holder
		final String methodName = "get" + elementName.substring(0, 1).toUpperCase() + elementName.substring(1);
    			
    	//Try to find the method using reflection
		//TODO This search by method name in the holder should be improve, one approach is to 
		//search in methods of the holder annotated by some annotation which provides the method which
		//it references
    	Method holderMethod = null;
    	try {
			holderMethod = holder.getClass().getMethod(methodName);
		} catch (NoSuchMethodException | SecurityException e) {
			try {
				holderMethod = holder.getClass().getMethod(methodName + "Method");				
			} catch (NoSuchMethodException | SecurityException e1) {
				try {
					holderMethod = holder.getClass().getMethod(methodName + "Body");
				} catch (NoSuchMethodException | SecurityException e2) {
					try {
						holderMethod = holder.getClass().getMethod(methodName + "AfterSuperBlock");
					} catch (NoSuchMethodException | SecurityException e3) {
						try {
							holderMethod = holder.getClass().getMethod(methodName + "BeforeSuperBlock");
						} catch (NoSuchMethodException | SecurityException e4) {
						
						}
					}
				}					
			}
		} 
    	
    	if (holderMethod != null) {
    		try {
    			Object result = holderMethod.invoke(holder);
    			
    			JBlock block = null;
    			if (result instanceof JMethod) {
    				block = ((JMethod) result).body();
    			} else if (result instanceof JBlock) {
    				block = (JBlock) result;
    			}
    			
				if (block != null) {
					
					if (element.getAnnotation(RunWith.class) != null && adiHelper.getAnnotation(element, External.class) != null) {

						//If this is an @External @RunWith method, it should not be call by itself,
						//this code avoids this issue (an infinite loop)
						
						//This will be called by ExernalHandler as well
						JMethod method = codeModelHelper.overrideAnnotatedMethod((ExecutableElement) element, holder, false, false);
						if (method == result) return true;
					}
					
					IJStatement statement = getStatement(getJClass(elementClass), element, viewsHolder, holder);
					if (statement != null) {
						block.add(statement);
					}
					return true;
				}
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}
    	}
    	
    	if (element instanceof ExecutableElement) {
    		
    		//This give support to Override methods where
    		if (element.getAnnotation(RunWith.class) != null) {
    			
    			if (elementName.endsWith("_")) {
    				elementName = elementName.substring(0, elementName.length()-1);
    				return methodHandler(elementClass, elementName, element, viewsHolder, holder);
    			}			
    			
    			//Override methods is handled in RunWithHandler
    			if (element.getAnnotation(Override.class) != null) {
    				return true;
    			}
    			
    			//Navigate in super parent looking for coinciding methods
    			ExecutableElement executableElement = (ExecutableElement) element;

    			//Look for similar methods in super classes
    			List<? extends TypeMirror> superTypes = 
    					getProcessingEnvironment().getTypeUtils().directSupertypes(element.getEnclosingElement().asType());
    			
    			for (TypeMirror type : superTypes) {
    				TypeElement superElement = getProcessingEnvironment().getElementUtils().getTypeElement(type.toString());
    				if (superElement == null) continue;
    				
    				if (foundMethodIn(superElement, executableElement, elementName)) {
    					TypeMirror resultType = executableElement.getReturnType();
    					
    					JMethod method = holder.getGeneratedClass().method(
    							JMod.PUBLIC, getJClass(resultType.toString()), elementName
    						);
    					method.annotate(Override.class);
    					
    					if (adiHelper.getAnnotation(element, External.class) != null) {

    						//If this is an @External @RunWith method, it should not be call by itself,
    						//this code avoids this issue (an infinite loop)
    						
    						//This will be called by ExernalHandler as well
    						JMethod externalMethod = codeModelHelper.overrideAnnotatedMethod((ExecutableElement) element, holder, false, false);
    						if (method == externalMethod) return true;
    					}
    					
    					method.body().add(getStatement(getJClass(elementClass), element, viewsHolder, holder));

    					List<? extends VariableElement> parameters = executableElement.getParameters();
    					for (VariableElement param : parameters) {
    						method.param(getJClass(param.asType().toString()), param.getSimpleName().toString());
    					}
    					
        				return true;
    				}
    			}
    			
    		}
    		
    		//The same method invocating itself is handled in a different
    		//handler (Ex. RunWithHandler)
    		return true;
    	}
    	
		//Search coinciding methods
    	//TODO recursive search in all the methods of the super classes 
		List<? extends Element> elems = element.getEnclosingElement().getEnclosedElements();
		List<Element> allElems = new LinkedList<>(elems);
		allElems.addAll(VirtualElement.getVirtualEnclosedElements(element.getEnclosingElement()));
		
		for (Element elem : allElems)
			if (elem.getKind() == ElementKind.METHOD) {
				if (elem.getModifiers().contains(Modifier.PRIVATE) ||
					elem.getModifiers().contains(Modifier.STATIC)) {
					continue;
				}
				
				if (elem.getSimpleName().toString().equals(elementName)) {
					ExecutableElement executableElem = (ExecutableElement) elem;
					
					List<? extends VariableElement> parameters = executableElem.getParameters();
					TypeMirror resultType = executableElem.getReturnType();
					
					JMethod method = holder.getGeneratedClass().method(
							JMod.PUBLIC, getJClass(resultType.toString()), elementName
						);
					method.body().add(getStatement(getJClass(elementClass), element, viewsHolder, holder));
					
					JInvocation superInvoke = invoke(_super(), elementName);
					for (VariableElement param : parameters) {
						method.param(getJClass(param.asType().toString()), param.getSimpleName().toString());
						superInvoke = superInvoke.arg(ref(param.getSimpleName().toString()));
					}

					if (!executableElem.getModifiers().contains(Modifier.ABSTRACT)) {
						if (resultType.getKind().equals(TypeKind.VOID)) {
							method.body().add(superInvoke);
						} else {
							method.body()._return(superInvoke);
						}    							
					}
					
					return true;
				}
				
			}
		
		return false;
	}
	
	private boolean foundMethodIn(Element element, ExecutableElement executableElement, String elementName) {
		List<? extends Element> elems = element.getEnclosedElements();
		List<Element> allElems = new LinkedList<>(elems);
		allElems.addAll(VirtualElement.getVirtualEnclosedElements(element));
		
		ELEMENTS:
		for (Element elem : allElems) {
			if (elem.getKind() == ElementKind.METHOD) {
				if (elem.getModifiers().contains(Modifier.PRIVATE) ||
					elem.getModifiers().contains(Modifier.STATIC)) {
					continue;
				}
				
				if (elem.getSimpleName().toString().equals(elementName)) {
					ExecutableElement executableElem = (ExecutableElement) elem;
										
					TypeMirror resultType = executableElem.getReturnType();
					if (!TypeUtils.isSubtype(executableElement.getReturnType(), resultType, getProcessingEnvironment())) continue;
					
					List<? extends VariableElement> parameters = executableElem.getParameters();
					if (parameters.size() != executableElement.getParameters().size()) continue;
					
					for (int i = 0; i < parameters.size(); i++) {
						if (!parameters.get(i).asType().toString().equals(
								executableElement.getParameters().get(i).asType().toString()
							)) continue ELEMENTS;
					}
					
					//Method was found
					return true;
				}
				
			}
		}
		
		List<? extends TypeMirror> superTypes = getProcessingEnvironment().getTypeUtils().directSupertypes(element.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = getProcessingEnvironment().getElementUtils().getTypeElement(type.toString());
			if (superElement == null) continue;
			if (foundMethodIn(superElement, executableElement, elementName)) return true;
		}
		
		return false;
	}

	protected abstract IJStatement getStatement(AbstractJClass elementClass, Element element, 
												ViewsHolder viewsHolder, T holder);
	
	protected class StatementCreator implements IStatementCreator {
		
		AbstractJClass elementClass;
		Element element;
		ViewsHolder viewsHolder;
		T holder;
		
		public StatementCreator(AbstractJClass elementClass, Element element,
				ViewsHolder viewsHolder, T holder) {
			this.elementClass = elementClass;
			this.element = element;
			this.viewsHolder = viewsHolder;
			this.holder = holder;
		}

		@Override
		public IJStatement getStatement() {
			return BaseEventHandler.this.getStatement(elementClass, element, viewsHolder, holder);
		}
		
	}
}
