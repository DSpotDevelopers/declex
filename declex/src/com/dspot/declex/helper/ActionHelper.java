package com.dspot.declex.helper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.AnnotationHandler;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.holder.GeneratedClassHolder;
import org.androidannotations.internal.model.AnnotationElements.AnnotatedAndRootElements;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;

import com.dspot.declex.action.ActionsProcessor;
import com.dspot.declex.action.ActionsProcessor.ActionCallSuperException;
import com.dspot.declex.override.helper.DeclexAPTCodeModelHelper;
import com.dspot.declex.wrapper.element.VirtualElement;

public class ActionHelper {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ActionHelper.class);
	
	private AndroidAnnotationsEnvironment environment;
	private Map<Element, ElementDetails> actionsMap = new HashMap<>();

	private DeclexAPTCodeModelHelper codeModelHelper;
	
	private static ActionHelper instance;
	
	public static ActionHelper getInstance(AndroidAnnotationsEnvironment environment) {
		if (instance == null) {
			instance = new ActionHelper(environment);
		}
		return instance;
	}
	
	private ActionHelper(AndroidAnnotationsEnvironment environment) {
		this.environment = environment;
		this.codeModelHelper = new DeclexAPTCodeModelHelper(environment);
	}
	
	public boolean hasAction(Element element) {
		return ActionsProcessor.hasAction(element, environment);
	}
	
	public void clear() {
		actionsMap.clear();
	}
	
	public void validate() {
		for (ElementDetails elementData : actionsMap.values()) {
			
			for (Element elem : elementData.element.getEnclosedElements()) {
				
				try {
					if (elem.getKind() == ElementKind.FIELD 
							|| elem.getKind() == ElementKind.METHOD) {
							
						if (ActionsProcessor.hasAction(elem, environment)) {
							
							ElementValidation valid = new ElementValidation(elementData.handler.getTarget(), elem);
							
							if (elem.getModifiers().contains(Modifier.PRIVATE)
								|| elem.getModifiers().contains(Modifier.STATIC)) {
								
								if (elem instanceof ExecutableElement) {
									valid.addError("Action cannot be used inside \"private\" or \"static\" methods");
								} else {
									if (elem.getModifiers().contains(Modifier.PRIVATE)) {
										valid.addError("Action cannot be used with \"private\" fields");
									}
								}
								
							} else {
								ActionsProcessor.validateActions(elem, valid, environment);
							}
							
							if (valid.isValid()) {
								elementData.actions.add(elem);
							} else {
								for (ElementValidation.Error error : valid.getErrors()) {
									LOGGER.error(error.getMessage(), error.getElement());
								}

								for (String warning : valid.getWarnings()) {
									LOGGER.warn(warning, valid.getElement());
								}

								if (!valid.isValid()) {
									LOGGER.warn("Element {} contains an action error", elem);
								}
							}
						}
						
					}					
				} catch (ActionCallSuperException e) {
					
					String message = "Method referenced from " 
					                 + (e.getElement() == elem? "within itself" : elem) 
					                 + ", in order to permit this, ";
					if (e.getElement() instanceof ExecutableElement) {
						if (((ExecutableElement) e.getElement()).getAnnotation(Override.class) != null) {
							message = message + "please remove the @Override annotation and ";
						}
					}
					
					message = message + "you should preappend the symbol \"$\" to your method name. ";
					message = message + "Instad of \"" + e.getElement().getSimpleName() 
							  + "\" it should be named \"$" + e.getElement().getSimpleName() + "\""; 
					
					LOGGER.error(message, e.getElement());
					LOGGER.warn("Element {} contains an action error", e.getElement());
				} catch (Throwable e) {
					LOGGER.error(
							"Internal crash while validating action with element " + elem + ". \n"
							 + "Please report this in " 
							 + elementData.handler.getAndroidAnnotationPlugin().getIssuesUrl(), 
							 elem
						 );
					LOGGER.error("Crash Report: {}", e);
				}
			}
			
		}
	}
	
	public void validate(Element element, AnnotationHandler<?> handler) {
		if (!element.getKind().equals(ElementKind.CLASS)) {
			throw new RuntimeException("Actions should be handled by an enhanced class only");
		}
		
		if (actionsMap.containsKey(element)) {
			throw new RuntimeException("Actions should be validated only once");
		}

		//Actions will be executed in the same object independently of the Virtual State
		if (element instanceof VirtualElement) {
			actionsMap.put(((VirtualElement) element).getElement(), new ElementDetails(((VirtualElement) element).getElement(), handler));
		} else {
			actionsMap.put(element, new ElementDetails(element, handler));
		}
	}
	
	public void process() {
		for (ElementDetails elementDetail : actionsMap.values()) {
			
			for (Element elem : elementDetail.actions) {
				try {
					GeneratedClassHolder holder = environment.getProcessHolder().getGeneratedClassHolder(elementDetail.element);
					if (holder == null) {
						//Ancestor element
						Set<AnnotatedAndRootElements> ancestors = 
								environment.getValidatedElements()
						                   .getAncestorAnnotatedElements(elementDetail.handler.getTarget());
												
						for (AnnotatedAndRootElements ancestor : ancestors) {
							
							if (ancestor.annotatedElement == elementDetail.element) {
								holder = environment.getProcessHolder().getGeneratedClassHolder(ancestor.rootTypeElement);
								if (holder == null) continue;
								
								//Ignore if it was already generated the method
								if (elem instanceof ExecutableElement) {
								
									if (codeModelHelper.findAlreadyGeneratedMethod((ExecutableElement) elem, holder, true) != null) {
										continue;
									}
									
								}
								
								ActionsProcessor.processActions(elem, (EComponentHolder) holder);								
							}
						}
						
						continue;
					}
					
					ActionsProcessor.processActions(elem, (EComponentHolder) holder);
					
				} catch (Throwable e) {
					LOGGER.error(
							"Internal crash while processing action in element " + elem + ". \n"
							 + "Please report this in " 
							 + elementDetail.handler.getAndroidAnnotationPlugin().getIssuesUrl(), 
							 elem
						 );
					LOGGER.error("Crash Report: {}", e);					
				}
				
			}
			
		}		
	}
	
	private class ElementDetails {
		Element element;
		AnnotationHandler<?> handler;
		
		List<Element> actions = new LinkedList<>();
		
		public ElementDetails(Element element, AnnotationHandler<?> handler) {
			super();
			this.element = element;
			this.handler = handler;
		}
	}
}
