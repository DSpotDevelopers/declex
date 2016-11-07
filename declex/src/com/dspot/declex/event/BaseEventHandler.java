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
package com.dspot.declex.event;

import static com.helger.jcodemodel.JExpr.TRUE;
import static com.helger.jcodemodel.JExpr._super;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.ref;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.holder.HasOptionsMenu;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;
import org.androidannotations.rclass.IRClass.Res;

import com.dspot.declex.api.eventbus.UseEventBus;
import com.dspot.declex.event.holder.ViewListenerHolder;
import com.dspot.declex.share.holder.ViewsHolder;
import com.dspot.declex.share.holder.ViewsHolder.WriteInBlockWithResult;
import com.dspot.declex.util.EventUtils;
import com.dspot.declex.util.SharedRecords;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;

public abstract class BaseEventHandler extends BaseAnnotationHandler<EComponentWithViewSupportHolder> {

	protected static final Logger LOGGER = LoggerFactory.getLogger(BaseEventHandler.class);
	
	protected final String[] actions = {
			"Event", "_event",
			"Method", "_method",
			"Menu", "_menu", "MenuItem", "_menuitem",
	};
			
	private String referecedId;
	
	public BaseEventHandler(Class<?> targetClass, AndroidAnnotationsEnvironment environment) {
		super(targetClass, environment);
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		
		validatorHelper.enclosingElementHasEnhancedComponentAnnotation(element, valid);
		 
		String elementName = element.getSimpleName().toString();
		String elementNameAsClass = elementName;
		int actionIndex = -1;
		for (int i = 0; i < actions.length; i++) {
			String action = actions[i];
			
			Matcher match = Pattern.compile("(((?:\\w|_|\\d|\\$)+)" + action + ")\\d*$").matcher(elementName);
			if (match.find()) {
				elementName = match.group(2);
				elementNameAsClass = match.group(1);
				actionIndex = i;
				break;
			}
		}
		
		if (actionIndex == 0 || actionIndex == 1) {
			//It should start with "on"
			if (!elementName.startsWith("on")) {
				valid.addError("An event method shoud start with \"on\"");
			} else {
				String eventClassName = SharedRecords.getEvent(elementNameAsClass.substring(2), getEnvironment()); 
				
				if (eventClassName == null) {
					eventClassName = SharedRecords.getEvent(elementName.substring(2), getEnvironment());
					if (eventClassName == null) {
						valid.addError(
							"No event found with the name \"" + elementName.substring(2) + 
							"\" or \"" + elementNameAsClass.substring(2) + "\"" 
						);
					}					
				}
				
				if (element.getEnclosingElement().getAnnotation(UseEventBus.class) == null) {
					valid.addError("The enclosing element should be annotated with @UseEventBus");
				}
			}
			
		} else if (actionIndex==4 || actionIndex==5 || actionIndex==6 || actionIndex==7) {
//			List<String> menuObjects = getMenuObjects(element);
//			if (!menuObjects.contains(elementName)) {
//				valid.addError("The menu element " + element.getSimpleName() + 
//						" cannot be found with the id \"" + elementName + "\"");
//			}
		} 

	}
	
	protected List<String> getNames(Element element) {
		List<String> names = new ArrayList<>(1);
		names.add(element.getSimpleName().toString());
		return names;
	}	
	
	protected ViewListenerHolder getListenerHolder(String elementName, String elementClass, Map<AbstractJClass, IJExpression> declForListener, 
			Element element, ViewsHolder viewsHolder, EComponentWithViewSupportHolder holder) {
		
		String elementNameAsClass = elementName; 
		
		int actionIndex = -1;
		for (int i = 0; i < actions.length; i++) {
			String action = actions[i];
			
			Matcher match = Pattern.compile("(((?:\\w|_|\\d|\\$)+)" + action + ")\\d*$").matcher(elementName);
			if (match.find()) {
				referecedId = match.group(2);
				elementNameAsClass = match.group(1);
				actionIndex = i;
				break;
			}
		}
		
		switch (actionIndex) {
		case 0:
		case 1: 
			{
				String eventClassName = SharedRecords.getEvent(elementNameAsClass.substring(2), getEnvironment()); 
				if (eventClassName == null) {
					eventClassName = SharedRecords.getEvent(referecedId.substring(2), getEnvironment());
				}	
				
				JMethod method = EventUtils.getEventMethod(eventClassName, element.getEnclosingElement(), viewsHolder, getEnvironment());
				method.body().add(getStatement(getJClass(eventClassName), element, viewsHolder));
				return null;
			}
		
		case 2:
		case 3:
			//reserved for methods
			methodHandler(elementClass, referecedId, element, viewsHolder);
			return null;
			
		case 4:
		case 5:
		case 6:
		case 7:
			//Menu
			menuHandler(elementClass, referecedId, element, viewsHolder);
			return null;
			
		default:
			String referencedIdEnumerated = null;
			Matcher match = Pattern.compile("((?:\\w|_|\\d|\\$)+\\w)\\d+$").matcher(elementName);
			if (match.find()) {
				referencedIdEnumerated = match.group(1);
			}
			
			//If detected as a menu, fall back as menuEvent
			List<String> menuObjects = viewsHolder.getMenuObjects();
			if (referencedIdEnumerated != null && menuObjects.contains(referencedIdEnumerated)) {
				referecedId = referencedIdEnumerated;
			}
			if (menuObjects.contains(referecedId)) {
				menuHandler(elementClass, referecedId, element, viewsHolder);
				return null;
			}
			
			if (referencedIdEnumerated != null && viewsHolder.layoutContainsId(referencedIdEnumerated)) {
				referecedId = referencedIdEnumerated;
			}
			
			//Check if it is an event
			if (referecedId.startsWith("on")) {
				String eventClassName = SharedRecords.getEvent(referecedId.substring(2), getEnvironment()); 
				
				if (eventClassName != null) {
					JMethod method = EventUtils.getEventMethod(eventClassName, element.getEnclosingElement(), viewsHolder, getEnvironment());
					method.body().add(getStatement(getJClass(eventClassName), element, viewsHolder));
					return null;
				}
			}
			
			//Fallback as Method call
			methodHandler(elementClass, referecedId, element, viewsHolder);
			return null;
		}		
	}
	
	protected String getClassName(Element element) {
		String elementClass = element.asType().toString();
		if (!elementClass.endsWith("_")) elementClass = elementClass + "_";
		
		return elementClass;
	}

	@Override
	public void process(Element element, final EComponentWithViewSupportHolder holder) {
		
		final ViewsHolder viewsHolder = holder.getPluginHolder(new ViewsHolder(holder, annotationHelper));
		
		Map<AbstractJClass, IJExpression> declForListener = new HashMap<>();
		String elementClass = getClassName(element);
		
		for (final String elementName : getNames(element)) { 
			
			referecedId = elementName;
			
			final ViewListenerHolder listenerHolder = 
					getListenerHolder(elementName, elementClass, declForListener, element, viewsHolder, holder);
			if (listenerHolder == null) continue;
			
			for (AbstractJClass declClass : declForListener.keySet()) {
				listenerHolder.addDecl(referecedId, JMod.FINAL, declClass, "model", declForListener.get(declClass));
			}
			
			listenerHolder.addStatement(
					referecedId, 
					getStatement(elementClass==null ? null : getJClass(elementClass), element, viewsHolder)
				);
			
			//If it's found the the class associated layout, then process the event here			
			if (viewsHolder.layoutContainsId(referecedId)) {				
				viewsHolder.createAndAssignView(referecedId, new WriteInBlockWithResult<JBlock>() {
	
					@Override
					public void writeInBlock(String viewName, AbstractJClass viewClass, JFieldRef view, JBlock block) {
						listenerHolder.createListener(viewName, block);
					}
				});
								
				continue;
			}
			
		}
	}
	
	private void menuHandler(String elementClass, String elementName, Element element, ViewsHolder viewsHolder) {
		final HasOptionsMenu holder = (HasOptionsMenu) viewsHolder.holder();
		
		JFieldRef idRef = getEnvironment().getRClass().get(Res.ID).getIdStaticRef(elementName, getEnvironment());

		IJExpression ifExpr = holder.getOnOptionsItemSelectedItemId().eq(idRef);

		JBlock block = holder.getOnOptionsItemSelectedMiddleBlock();
		JBlock itemIfBody = block._if(ifExpr)._then();

		itemIfBody.add(getStatement(getJClass(elementClass), element, viewsHolder));
		itemIfBody._return(TRUE);
	}
	
	private boolean methodHandler(String elementClass, String elementName, Element element, ViewsHolder viewsHolder) {
		final EComponentWithViewSupportHolder holder = viewsHolder.holder();
		
		if (holder.getGeneratedClass().fullName().endsWith("AddCardFragment_")) {
			System.out.println(holder.getGeneratedClass().fullName() + ": Checking method for " + elementName);
		}
		
		//See if the method exists in the holder
		final String methodName = "get" + elementName.substring(0, 1).toUpperCase() + elementName.substring(1);
    	
    	//Try to find the method using reflection
    	Method holderMethod = null;
    	try {
			holderMethod = holder.getClass().getMethod(methodName);
		} catch (NoSuchMethodException | SecurityException e) {
			try {
				holderMethod = holder.getClass().getMethod(methodName + "Body");
			} catch (NoSuchMethodException | SecurityException e1) {
				try {
					holderMethod = holder.getClass().getMethod(methodName + "Method");
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
					block.add(getStatement(getJClass(elementClass), element, viewsHolder));
					return true;
				}
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}
    	}
    	
    	if (element instanceof ExecutableElement) {
    		//The same method invocating itself is handled in a different
    		//handler (Ex. ActionHandler)
    		return true;
    	}
    	
		//Search coinciding methods
    	//TODO recursive search in all the methods of the super classes 
		List<? extends Element> elems = element.getEnclosingElement().getEnclosedElements();
		for (Element elem : elems)
			if (elem.getKind() == ElementKind.METHOD) {
				if (elem.getModifiers().contains(Modifier.PRIVATE) ||
					elem.getModifiers().contains(Modifier.STATIC)) {
					continue;
				}
				
				if (elem.getSimpleName().toString().equals(elementName)) {
					ExecutableElement executableElem = (ExecutableElement) elem;
					
					List<? extends VariableElement> parameters = executableElem.getParameters();
					TypeMirror resultType = executableElem.getReturnType();
					
					JMethod method = viewsHolder.getGeneratedClass().method(
							JMod.PUBLIC, getJClass(resultType.toString()), elementName
						);
					method.body().add(getStatement(getJClass(elementClass), element, viewsHolder));
					
					JInvocation superInvoke = invoke(_super(), elementName);
					for (VariableElement param : parameters) {
						method.param(getJClass(param.asType().toString()), param.getSimpleName().toString());
						superInvoke = superInvoke.arg(ref(param.getSimpleName().toString()));
					}
					
					if (!elem.getModifiers().contains(Modifier.ABSTRACT)) {
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

	protected abstract IJStatement getStatement(AbstractJClass elementClass, Element element, ViewsHolder viewsHolder);
}
