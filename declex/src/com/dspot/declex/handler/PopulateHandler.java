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
package com.dspot.declex.handler;

import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;
import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.holder.GeneratedClassHolder;
import org.androidannotations.internal.process.ProcessHolder;
import org.androidannotations.internal.process.ProcessHolder.Classes;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;
import org.androidannotations.rclass.IRClass.Res;

import com.dspot.declex.adapter.AdapterClassCreator;
import com.dspot.declex.adapter.RecyclerViewAdapterClassCreator;
import com.dspot.declex.adapter.RecyclerViewAdapterPopulator;
import com.dspot.declex.adapter.ViewAdapterPopulator;
import com.dspot.declex.adapter.plugin.JClassPlugin;
import com.dspot.declex.annotation.ExternalPopulate;
import com.dspot.declex.annotation.LoadOnEvent;
import com.dspot.declex.annotation.Model;
import com.dspot.declex.annotation.Populate;
import com.dspot.declex.annotation.UseModel;
import com.dspot.declex.api.action.runnable.OnFailedRunnable;
import com.dspot.declex.api.util.FormatsUtils;
import com.dspot.declex.helper.AfterPopulateHelper;
import com.dspot.declex.helper.EventsHelper;
import com.dspot.declex.helper.ViewsHelper;
import com.dspot.declex.helper.ViewsPropertiesReaderHelper;
import com.dspot.declex.holder.ModelHolder;
import com.dspot.declex.holder.ViewsHolder;
import com.dspot.declex.holder.ViewsHolder.IWriteInBloc;
import com.dspot.declex.holder.ViewsHolder.IdInfoHolder;
import com.dspot.declex.holder.view_listener.ViewListenerHolder;
import com.dspot.declex.parser.LayoutsParser.LayoutObject;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.ParamUtils;
import com.dspot.declex.util.SharedRecords;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.util.TypeUtils.ClassInformation;
import com.dspot.declex.wrapper.element.VirtualElement;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.AbstractJType;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JCatchBlock;
import com.helger.jcodemodel.JClassAlreadyExistsException;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JTryBlock;
import com.helger.jcodemodel.JVar;

public class PopulateHandler extends BaseAnnotationHandler<EComponentWithViewSupportHolder> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PopulateHandler.class);
	
	private List<JClassPlugin> adapterPlugins; 

	private Map<Element, Map<String, ExecutableElement>> populatorMethods = new HashMap<>();
	
	private static int uniquePriorityCounter = 1000;
	
	private EventsHelper eventsHelper;
	private ViewsPropertiesReaderHelper propertiesHelper;
	private AfterPopulateHelper afterPopulateHelper;
	
	public PopulateHandler(AndroidAnnotationsEnvironment environment, List<JClassPlugin> adapterPlugins) {
		super(Populate.class, environment);
		
		this.adapterPlugins = adapterPlugins;
		
		eventsHelper = EventsHelper.getInstance(environment);
		propertiesHelper = ViewsPropertiesReaderHelper.getInstance(environment);
		afterPopulateHelper = new AfterPopulateHelper(getEnvironment());
	}
	
	@Override
	public void validate(Element element, ElementValidation valid) {
				
		final String elementName = element.getSimpleName().toString();
		
		//Ignore @Populate Methods
		if (element instanceof ExecutableElement) {
			
			if (afterPopulateHelper.existsPopulateFieldWithElementName(element)) {
				Map<String, ExecutableElement> methods = populatorMethods.get(element.getEnclosingElement());
				if (methods == null) {
					methods = new HashMap<>();
					populatorMethods.put(element.getEnclosingElement(), methods);
				}
				
				methods.put(elementName, (ExecutableElement) element);
				
				return;
			}
			
		}
				
		validatorHelper.enclosingElementHasEnhancedComponentAnnotation(element, valid);
		validatorHelper.isNotPrivate(element, valid);		
		
		//Validate special methods
		List<? extends Element> elems = element.getEnclosingElement().getEnclosedElements();
		for (Element elem : elems) {
			if (elem.getKind() == ElementKind.METHOD) {
				ExecutableElement executableElement = (ExecutableElement) elem;
				
				if (executableElement.getSimpleName().toString().equals("assignField")) {
					validatorHelper.returnTypeIsVoid(executableElement, valid);
					
					List<? extends VariableElement> parameters = executableElement.getParameters();

					if (parameters.size() != 2) {
						valid.addError("%s can only be used on a method with  2 parameters, instead of " + parameters.size());
					} else {
						VariableElement firstParameter = parameters.get(0);
						VariableElement secondParameter = parameters.get(1);

						if (!TypeUtils.isSubtype(firstParameter, CanonicalNameConstants.VIEW, getProcessingEnvironment())) {
							valid.addError("The first parameter should be an instance of View");									
						}
						
						if (!secondParameter.asType().toString().equals(Object.class.getCanonicalName())) {
							valid.addError("The second parameter should be an Object");
						}
					}
					
					break;
				}
				
			}
		}
		
		ViewsHelper viewsHelper = new ViewsHelper(element.getEnclosingElement(), annotationHelper, getEnvironment());
		if (!(element instanceof ExecutableElement)) {
			
			boolean isList = TypeUtils.isSubtype(element, CanonicalNameConstants.LIST, getProcessingEnvironment());		
			if (isList) {
				String className = element.asType().toString();
	
				Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(className);
				if (!matcher.find()) {
					valid.addError("Cannot infer the List Type from " + className);
				}
							
				if (!viewsHelper.getLayoutObjects().containsKey(elementName)) {
					valid.addError("The element with Id \"" + elementName + "\" cannot be found in the Layout");
				} else {
					LayoutObject layoutObject = viewsHelper.getLayoutObjects().get(elementName);
					
					if (layoutObject.domElement.hasAttribute("tools:listitem")) {
						String listItem = layoutObject.domElement.getAttribute("tools:listitem");
						String listItemId = listItem.substring(listItem.lastIndexOf('/')+1);
						
						if (!getEnvironment().getRClass().get(Res.LAYOUT).containsField(listItemId)) {
							valid.addError(
									"The \"tools:listitem\" layout provided to the "
									+ layoutObject.className + " with id \"" + elementName + "\" in your layout is not valid"
									+ " The current value it is \"" + listItem + "\"");
						};
					} else {
						valid.addError(
								"You should provide an attribute \"tools:listitem\" as the layout for the list items, "
								+ "please review the " + layoutObject.className + " with id \"" + elementName + "\" in the layout"
							);
					}
				}
			}
			
		}
		
	}

	@Override
	public void process(Element element, EComponentWithViewSupportHolder holder) {		
		uniquePriorityCounter++;
		
		final ViewsHolder viewsHolder = holder.getPluginHolder(new ViewsHolder(holder, annotationHelper));
		
		final boolean isMethod;
		if (element instanceof ExecutableElement) {
		
			//Ignore @Populate support methods
			if (afterPopulateHelper.existsPopulateFieldWithElementName(element)) {
				return;
			}
			
			isMethod = true;
		} else {
			isMethod = false;
		}

		ClassInformation classInformation = TypeUtils.getClassInformation(element, getEnvironment());
		final String className = classInformation.generatorClassName;
		final String originalClassName = classInformation.originalClassName;
		final boolean isList = classInformation.isList;
		
		OnEventMethods onEventMethods = getOnEventMethods(className, element, viewsHolder);
		if (onEventMethods == null) return;	
				
		int layoutId = 0;
		int index = 0;
		int[] values = adiHelper.getAnnotation(element, Populate.class).value();
		
		//Restoration objects
		String defLayoutId = viewsHolder.getDefLayoutId();
		JVar onViewChangedHasViewsParam = holder.getOnViewChangedHasViewsParam();
		
		do {
			if (layoutId != 0) {
				viewsHolder.inflateLayoutAndUse(layoutId);
				processEventsInViews(element, viewsHolder);
			}
			
			IdInfoHolder info = findInfoHolder(element, viewsHolder);
			if (info != null) {
				if (isList) {
					
					if (layoutId == 0) {
						processList(originalClassName, element, viewsHolder, onEventMethods);					
					}
					
				} else {
					
					processDirectReference(info, className, element, viewsHolder, onEventMethods);
					
				}
			} else if (isMethod) {
				
				processMethod(element, viewsHolder, onEventMethods, classInformation, layoutId);
				
			} else {
				
				processModel(className, element, viewsHolder, onEventMethods);
				
			}	
			
			if (!adiHelper.getAnnotation(element, Populate.class).independent()) {
				SharedRecords.priorityAdd(
						viewsHolder.holder().getOnViewChangedBody(), 
						JExpr.invoke(onEventMethods.populateFieldMethod).arg(_null()).arg(_null()), 
						uniquePriorityCounter
					);
			}
			
			createViewsForPopulateMethod(element.getSimpleName().toString(), element, viewsHolder);
			
			if (index >= values.length) break;
			layoutId = values[index];
			index++;
							
		} while (layoutId != 0);
		
		if (!isList) {
			callPopulateSupportMethod(
					element.getSimpleName().toString(), 
					holder.getOnViewChangedBody(), 
					null, null, element, viewsHolder
			);			
		}			

		viewsHolder.setDefLayoutId(defLayoutId);
		holder.setOnViewChangedHasViewsParam(onViewChangedHasViewsParam);		
	}	
	
	private OnEventMethods getOnEventMethods(String className, Element element, ViewsHolder viewsHolder) {
		final String fieldName = element.getSimpleName().toString();
		
		final boolean hasExternalPopulate = adiHelper.getAnnotation(element, ExternalPopulate.class) != null;
		
		//If there's a LoadOnEvent annotation, then instantiate the object only on that event
		JBlock loadOnEventBlock = null;
		LoadOnEvent loadOnEvent = element.getAnnotation(LoadOnEvent.class);
		if (loadOnEvent != null) {
			String classField = TypeUtils.getClassFieldValue(element, LoadOnEvent.class.getCanonicalName(), "value", getEnvironment());
			String eventClass = SharedRecords.getEvent(classField, getEnvironment());
			
			if (classField == null || eventClass == null) {
				LOGGER.error("@LoadOnEvent failed in " + className, element, loadOnEvent);
				return null;
			}

			loadOnEventBlock = eventsHelper.addEventListener(eventClass, element.getEnclosingElement(), viewsHolder);
		}
		
		//Create the populate method
		JMethod populateFieldMethod = viewsHolder.getGeneratedClass().getMethod(
				"_populate_" + fieldName,
				new AbstractJType[] {getJClass(Runnable.class), getJClass(OnFailedRunnable.class)}
			);
		if (populateFieldMethod == null) {
			populateFieldMethod = viewsHolder.getGeneratedClass().method(JMod.NONE, getCodeModel().VOID, "_populate_" + fieldName);
			populateFieldMethod.param(JMod.FINAL, getJClass(Runnable.class), "afterPopulate");
			populateFieldMethod.param(JMod.FINAL, getJClass(OnFailedRunnable.class), "onFailed");
		}
		
		//Create the "populate this" method
		JMethod populateThisMethod = viewsHolder.getGeneratedClass().getMethod(
				"_populate_this",
				new AbstractJType[] {getJClass(Runnable.class), getJClass(OnFailedRunnable.class)}
			);
		if (populateThisMethod == null) {
			populateThisMethod = viewsHolder.getGeneratedClass().method(JMod.NONE, getCodeModel().VOID, "_populate_this");
			JVar afterPopulate = populateThisMethod.param(JMod.FINAL, getJClass(Runnable.class), "afterPopulate");
			populateThisMethod.param(JMod.FINAL, getJClass(OnFailedRunnable.class), "onFailed");
			
			JBlock block = new JBlock();
			block._if(afterPopulate.neNull())._then()
			     .invoke(afterPopulate, "run");
			SharedRecords.priorityAdd(populateThisMethod.body(), block, uniquePriorityCounter);
		}
		
		if (!adiHelper.getAnnotation(element, Populate.class).independent()) {
			populateThisMethod.body().invoke(populateFieldMethod).arg(_null()).arg(ref("onFailed"));			
		}
		
		Model model = adiHelper.getAnnotation(element, Model.class); 
		if (model != null) {			
			final ModelHolder modelHolder;
			
			//Support ExternalPopulate
			if (hasExternalPopulate) {
				
				final Element referenceElement = ((VirtualElement) element).getReference();
				ClassInformation classInformation = TypeUtils.getClassInformation(referenceElement, getEnvironment(), true);
				ProcessHolder processHolder = getEnvironment().getProcessHolder();
				EComponentHolder holder = (EComponentHolder) processHolder.getGeneratedClassHolder(classInformation.generatorElement);
				
				modelHolder = holder.getPluginHolder(new ModelHolder(holder));
				element = ((VirtualElement) element).getElement();
				
			} else {
				modelHolder = viewsHolder.holder().getPluginHolder(new ModelHolder(viewsHolder.holder()));				
			}
			
			JBlock methodBody = modelHolder.getAfterLoadModelBlock(element);
			
			if (model.async()) {
				JDefinedClass annonimousRunnable = getCodeModel().anonymousClass(Runnable.class);
				JMethod annonimousRunnableRun = annonimousRunnable.method(JMod.PUBLIC, getCodeModel().VOID, "run");
				annonimousRunnableRun.annotate(Override.class);
				
				JVar handler = methodBody.decl(getClasses().HANDLER, "handler", 
						_new(getClasses().HANDLER).arg(getClasses().LOOPER.staticInvoke("getMainLooper")));
				methodBody.invoke(handler, "post").arg(_new(annonimousRunnable));
				
				methodBody = annonimousRunnableRun.body();
			} 
			
			JBlock ifPopulate = methodBody._if(
				ref("args").invoke("containsKey").arg("populate").not()
				.cor(cast(getJClass(Boolean.class), ref("args").invoke("get").arg("populate")))
			)._then();
			
			if (hasExternalPopulate) {
				final String populateListenerName = "populate" + fieldName.substring(0, 1).toUpperCase()
                        + fieldName.substring(1);
				
				JFieldRef listenerField = ref(populateListenerName);				
  			    ifPopulate._if(listenerField.neNull())._then()
						  .invoke(listenerField, "populateModel")
			              .arg(_null()).arg(ref("onFailed"));
				
			} else {
				ifPopulate.invoke(populateFieldMethod).arg(_null()).arg(ref("onFailed"));
			}
			
		}
		
		return new OnEventMethods(loadOnEventBlock, populateFieldMethod, viewsHolder.holder());
	}
	
	private void processMethod(Element element, ViewsHolder viewsHolder, OnEventMethods onEventMethods, ClassInformation classInformation, int layoutId) {
		final String methodName = element.getSimpleName().toString();		
		final String className = classInformation.generatorClassName;
		
		Map<String, IdInfoHolder> allFields = new HashMap<String, IdInfoHolder>();
		Map<String, IdInfoHolder> allMethods = new HashMap<String, IdInfoHolder>();		
		viewsHolder.findFieldsAndMethods(
				viewsHolder.holder().getAnnotatedElement().asType().toString(), 
				null, element, allFields, allMethods, true);
		
		for (Entry<String, IdInfoHolder> entry : allFields.entrySet()) {
			final IdInfoHolder fieldInfo = entry.getValue();
			final String infoNameForMethod = "get" + fieldInfo.idName.substring(0, 1).toUpperCase()
					                               + fieldInfo.idName.substring(1);
			
			final boolean startsWithInfo = methodName.startsWith(fieldInfo.idName) || methodName.startsWith(infoNameForMethod);
			final boolean endsWithGetterOrSetter = fieldInfo.getterOrSetter != null && methodName.endsWith(fieldInfo.getterOrSetter);

			if (startsWithInfo && endsWithGetterOrSetter) {
				
				JFieldRef view = viewsHolder.createAndAssignView(fieldInfo.idName);
				
				JBlock block = onEventMethods.populateFieldMethodBody.block();
				putAssignInBlock(fieldInfo, block, view, invoke(methodName), element, viewsHolder, onEventMethods);
				
				return;
			}
		}
		
		for (Entry<String, IdInfoHolder> entry : allMethods.entrySet()) {
			final IdInfoHolder methodInfo = entry.getValue();
			final String infoNameForMethod = "get" + methodInfo.idName.substring(0, 1).toUpperCase()
                                                   + methodInfo.idName.substring(1);

			final boolean startsWithInfo = methodName.startsWith(methodInfo.idName) || methodName.startsWith(infoNameForMethod);
			final boolean endsWithGetterOrSetter = methodInfo.getterOrSetter != null && methodName.endsWith(methodInfo.getterOrSetter);
			
			if ((startsWithInfo && endsWithGetterOrSetter) 
				|| (methodName.equals(infoNameForMethod) || methodName.equals(methodInfo.idName))) {
				
				JFieldRef view = viewsHolder.createAndAssignView(methodInfo.idName);
				
				JBlock block = onEventMethods.populateFieldMethodBody.block();
				putAssignInBlock(methodInfo, block, view, invoke(methodName), element, viewsHolder, onEventMethods);
				
				return;
			}
		}
		
		processModel(className, element, viewsHolder, onEventMethods);
	}
	
	private IdInfoHolder findInfoHolder(Element element, ViewsHolder viewsHolder) {
		
		final String fieldName = element.getSimpleName().toString();
		
		final Map<String, IdInfoHolder> allFields = new HashMap<String, IdInfoHolder>();
		final Map<String, IdInfoHolder> allMethods = new HashMap<String, IdInfoHolder>();		
		viewsHolder.findFieldsAndMethods(
				viewsHolder.holder().getAnnotatedElement().asType().toString(), 
				null, element, allFields, allMethods, true);
		for (Entry<String, IdInfoHolder> entry : allFields.entrySet()) {
			
			IdInfoHolder info = entry.getValue();
			
			if (info.getterOrSetter != null && fieldName.length() > info.getterOrSetter.length() 
				&& fieldName.substring(0, fieldName.length() - info.getterOrSetter.length()).equals(info.idName)) {
				return info;
			}
			
			if (info.getterOrSetter == null && fieldName.equals(info.idName)) {
				return info;
			}
		}
		
		final String annotatedElementClass = viewsHolder.holder().getAnnotatedElement().asType().toString();
		
		final Map<String, TypeMirror> getters = new HashMap<>();
		final Map<String, Set<TypeMirror>> setters = new HashMap<>();
		propertiesHelper.readGettersAndSetters(annotatedElementClass, getters, setters);
		
		final String property = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
		if (setters.containsKey(property)) {
			for (TypeMirror propertyType : setters.get(property)) {
				if (TypeUtils.isSubtype(element.asType(), propertyType, getProcessingEnvironment())) {
					return new IdInfoHolder(null, element, element.asType(), annotatedElementClass, new ArrayList<String>(0), property);
				}							
			}
		}
		
		return null;
	}
	
	private boolean processDirectReference(final IdInfoHolder info, String className, Element element, ViewsHolder viewsHolder, OnEventMethods onEventMethods) {
		
		final String fieldName = element.getSimpleName().toString();
						
		//Create getter
		final String fieldGetter = FormatsUtils.fieldToGetter(fieldName);

		//Not coinciding field found
		if (info == null) return false;	

		IJExpression assignRef = invoke(element instanceof ExecutableElement? fieldName : fieldGetter);				
		if (info.getterOrSetter == null && !element.asType().toString().equals(String.class.getCanonicalName())) {
			assignRef = getClasses().STRING.staticInvoke("valueOf").arg(assignRef);
		}
		
		if (adiHelper.getAnnotation(element, Populate.class).debug())
			LOGGER.warn("\nField: " + fieldName, element, adiHelper.getAnnotation(element, Populate.class));
		
		IJExpression view = info.idName != null? viewsHolder.createAndAssignView(info.idName) : _this();
		JBlock block = onEventMethods.populateFieldMethodBody.block();
		putAssignInBlock(info, block, view, assignRef, element, viewsHolder, onEventMethods);
		
		return true;
	}
	
	private void processList(String className, Element element, ViewsHolder viewsHolder, final OnEventMethods onEventMethods) {

		final String fieldName = element.getSimpleName().toString();

		if (adiHelper.getAnnotation(element, Model.class) != null) {
			final ModelHolder modelHolder = viewsHolder.holder().getPluginHolder(new ModelHolder(viewsHolder.holder()));

			this.processList(
					viewsHolder.getClassNameFromId(fieldName), fieldName, className, 
					element instanceof ExecutableElement? invoke(fieldName) : invoke(modelHolder.getGetterMethod(element)), 
					onEventMethods.populateFieldMethodBody, element, viewsHolder, onEventMethods);			
		} else {
			this.processList(
					viewsHolder.getClassNameFromId(fieldName), fieldName, className, 
					element instanceof ExecutableElement? invoke(fieldName) : ref(fieldName), 
					onEventMethods.populateFieldMethodBody, element, viewsHolder, onEventMethods);			
		}
	}
	
	private void processList(String viewClass, String fieldName, String className, final IJExpression assignRef, JBlock block, 
			Element element, ViewsHolder viewsHolder, final OnEventMethods onEventMethods) {
		
		final String adapterName = fieldName + "$adapter";
		final String adapterClassName = adapterName.substring(0, 1).toUpperCase() + adapterName.substring(1);
		final AbstractJClass AdapterClass = getJClass(adapterClassName);
		final JFieldRef adapter = ref(adapterName);

		block = block._if(assignRef.neNull())._then();
		
		final JConditional ifNotifBlock = block._if(adapter.ne(_null()));
		JBlock notifyBlock = ifNotifBlock._then();
		
		final boolean foundAdapterDeclaration = TypeUtils.fieldInElement(adapterName, element.getEnclosingElement());
		final Populate annotation = adiHelper.getAnnotation(element, Populate.class);
		JFieldRef view = viewsHolder.createAndAssignView(fieldName, new IWriteInBloc() {
			@Override
			public void writeInBlock(String viewName, AbstractJClass viewClass, JFieldRef view, JBlock block) {
				if (onEventMethods != null) {
					JBlock notifyBlock = ifNotifBlock._else()._if(view.ne(_null()))._then();
					
					if (!annotation.custom()) {
						notifyBlock.assign(adapter, _new(AdapterClass).arg(assignRef));
					}
					
					notifyBlock.invoke(view, "setAdapter").arg(adapter);
				}
			}
		});
		
		AbstractJClass WrapperListAdapter = getJClass("android.widget.WrapperListAdapter");
		IJExpression castedList = cast(WrapperListAdapter, view.invoke("getAdapter"));
		
		notifyBlock._if(view.ne(_null()).cand(view.invoke("getAdapter").ne(adapter)))
		           ._then()
		           ._if(view.invoke("getAdapter")._instanceof(WrapperListAdapter).not().cor(
	        		   view.invoke("getAdapter")._instanceof(WrapperListAdapter).cand(
        				   castedList.invoke("getWrappedAdapter").ne(adapter)
    				   )
				   ))._then()
		           .invoke(view, "setAdapter").arg(adapter);
		notifyBlock.invoke(adapter, "setModels").arg(assignRef);
		notifyBlock.invoke(adapter, "notifyDataSetChanged");
		
		if (!foundAdapterDeclaration) {
			if (viewsHolder.getGeneratedClass().fields().get(adapterName)!=null) {
				//Another annotation tried to create the same adapter, this is an error
				LOGGER.error("Tying to create a List Adapter twice for field " + fieldName, element, adiHelper.getAnnotation(element, Populate.class));
				return;
			}
					
			viewsHolder.getGeneratedClass().field(JMod.PRIVATE, AdapterClass, adapterName);
		}
			
		
		if (onEventMethods != null && onEventMethods.loadOnEventBlock != null) {
			if (foundAdapterDeclaration && annotation.custom()) {
				onEventMethods.loadOnEventBlock.assign(adapter, _new(AdapterClass).arg(assignRef));			
				onEventMethods.loadOnEventBlock.invoke(view, "setAdapter").arg(adapter);
				return;
			}
			
			onEventMethods.loadOnEventBlock.invoke(view, "setAdapter").arg(adapter);
		}
		
		//AdapterView
		if (TypeUtils.isSubtype(viewClass, "android.widget.AdapterView", getProcessingEnvironment())) {
			//If the adapter was not assigned, then create a local adapter
			createBaseAdapter(fieldName, className, element, viewsHolder);			
		}
		
		//RecyclerView
		if (TypeUtils.isSubtype(viewClass, "android.support.v7.widget.RecyclerView", getProcessingEnvironment())) {
			createRecyclerViewAdapter(fieldName, className, element, viewsHolder);
		}
	}
	
	private void createRecyclerViewAdapter(String fieldName, String modelClassName, Element element, 
			ViewsHolder viewsHolder) {

		String adapterName = fieldName + "$adapter";
		String adapterClassName = adapterName.substring(0, 1).toUpperCase() + adapterName.substring(1);
		
		try {
			
			RecyclerViewAdapterPopulator adapterPopulator = new RecyclerViewAdapterPopulator(this, fieldName, adapterClassName, modelClassName, viewsHolder);

			List<JClassPlugin> plugins = new LinkedList<>(adapterPlugins);
			plugins.add(plugins.size()-1, adapterPopulator); //Insert before the AdapterClass plugin

			RecyclerViewAdapterClassCreator classCreator = new RecyclerViewAdapterClassCreator(modelClassName, adapterClassName, element, viewsHolder.holder(), plugins);
			classCreator.getDefinedClass();
			
		} catch (JClassAlreadyExistsException e) {
		}
	}

	private void createBaseAdapter(String fieldName, String modelClassName, Element element, 
			ViewsHolder viewsHolder) {
		String adapterName = fieldName + "$adapter";
		String adapterClassName = adapterName.substring(0, 1).toUpperCase() + adapterName.substring(1);
		
		try {
			
			ViewAdapterPopulator adapterPopulator = new ViewAdapterPopulator(this, fieldName, adapterClassName, modelClassName, viewsHolder);

			List<JClassPlugin> plugins = new LinkedList<>(adapterPlugins);
			plugins.add(plugins.size()-1, adapterPopulator); //Insert before the AdapterClass plugin
			
			AdapterClassCreator classCreator = new AdapterClassCreator(modelClassName, adapterClassName, element, viewsHolder.holder(), plugins);			
			classCreator.getDefinedClass();
			
		} catch (JClassAlreadyExistsException e) {
		}
	}	

	private void processModel(String className, Element element, ViewsHolder viewsHolder, OnEventMethods onEventMethods) {
		String fieldName = element.getSimpleName().toString();
		
		//Find all the fields and methods that are presented in the layouts
		Map<String, IdInfoHolder> fields = new HashMap<String, IdInfoHolder>();
		Map<String, IdInfoHolder> methods = new HashMap<String, IdInfoHolder>();
		viewsHolder.findFieldsAndMethods(className, fieldName, element, fields, methods, true);
		
		if (adiHelper.getAnnotation(element, Populate.class).debug())
			LOGGER.warn("\nClass: " + className + "\nFields: " + fields + "\nMethods: " + methods, element, adiHelper.getAnnotation(element, Populate.class));
		
		/*TODO See how to integrate Plugins to the @Populates and @Recollects
		String mapField = null;
		UseMap useMap = element.getEnclosingElement().getAnnotation(UseMap.class);
		if (useMap != null) {
			mapField = getEnvironment().getRClass().get(Res.ID).getIdQualifiedName(useMap.value());
			mapField = mapField.substring(mapField.lastIndexOf('.') + 1);
			
			boolean found = false;
			for (IdInfoHolder value : methods.values()) {
				if (value.idName.equals(mapField)) {
					found = true;
					break;
				}
			}
			
			if (!found) mapField = null;
		}
		*/
		
		for (String field : fields.keySet()) {
			String composedField = "";
			for (String fieldPart : field.split("\\."))
				composedField = composedField + "." + fieldToGetter(fieldPart);
			
			injectAndAssignField(
				fields.get(field), composedField, 
				element, viewsHolder, onEventMethods
			);
		}
		
		for (String method : methods.keySet()) {
			String composedField = "";
			String[] methodSplit = method.split("\\.");
			for (int i = 0; i < methodSplit.length-1; i++) {
				composedField = composedField + "." + fieldToGetter(methodSplit[i]);
			}
			composedField = composedField + "." + methodSplit[methodSplit.length-1];
			
			injectAndAssignField(
				methods.get(method), composedField, 
				element, viewsHolder, onEventMethods
				/*,mapField != null && mapField.equals(methods.get(method).idName)*/
			);
			
		}
		
		//Call _populate_ method after setting everything
		SharedRecords.priorityAdd(
				viewsHolder.holder().getOnViewChangedBody(), 
				JExpr.invoke(onEventMethods.populateFieldMethod).arg(_null()).arg(_null()), 
				uniquePriorityCounter
			);
	}
	
	private void injectAndAssignField(IdInfoHolder info, String methodName,  
			Element element, ViewsHolder viewsHolder, OnEventMethods onEventMethods/*, boolean isMapField*/) {
		

		final String fieldName = element.getSimpleName().toString();
		IJExpression fieldRef = element instanceof ExecutableElement? invoke(fieldName) : ref(fieldName);
		
		if (adiHelper.getAnnotation(element, Model.class) != null) {
			ModelHolder modelHolder = viewsHolder.holder().getPluginHolder(new ModelHolder(viewsHolder.holder()));
			fieldRef = invoke(modelHolder.getGetterMethod(element));
		}
				
		boolean castNeeded = false;
		String className = element instanceof ExecutableElement? ((ExecutableElement)element).getReturnType().toString()
																 : element.asType().toString();
		if (!className.endsWith(ModelConstants.generationSuffix())) {
			if (TypeUtils.isClassAnnotatedWith(className, UseModel.class, getEnvironment())) {
				className = TypeUtils.getGeneratedClassName(className, getEnvironment());
				castNeeded = true;
			}
		}
		
		IJExpression assignRef = castNeeded ? cast(getJClass(className), fieldRef) : fieldRef;
		IJExpression methodsCall = assignRef;
		JBlock checkForNull = new JBlock();
		JBlock changedBlock = checkForNull._if(fieldRef.ne(_null()))._then();
		
		String[] methodSplit = methodName.split("\\.");
		for (int i = 0; i < methodSplit.length; i++) {
			String methodPart = methodSplit[i];
			if (!methodPart.equals("")) {
				methodsCall = methodsCall.invoke(methodPart);		
				
				boolean theresMoreAfter = false;
				for (int j = i+1; j < methodSplit.length; j++) {
					if (!methodSplit[j].equals("")) {
						theresMoreAfter = true;
						break;
					}
				}
				
				if (theresMoreAfter) changedBlock = changedBlock._if(methodsCall.ne(_null()))._then();
				else if (!info.type.getKind().isPrimitive()) {
					changedBlock = changedBlock._if(methodsCall.ne(_null()))._then();
				}
			}			
		}
		
		/*TODO see how to integrate plugins to the populator
		if (isMapField) {
			JBlock body = holder.getOnViewChangedBody();
			body.assign(ref("mapPosition"), methodsCall);
			body.invoke("setMapFromPosition").arg(ref("mapPosition"));
			return;
		}
		*/
		
		JFieldRef view = viewsHolder.createAndAssignView(info.idName);
		
		putAssignInBlock(info, changedBlock, view, methodsCall, element, viewsHolder, onEventMethods);
		onEventMethods.populateFieldMethodBody.add(checkForNull);
	}
	
	private void putAssignInBlock(IdInfoHolder info, JBlock block, IJExpression view, 
			IJExpression assignRef, Element element, 
			ViewsHolder viewsHolder, OnEventMethods onEventMethods) {
		this.putAssignInBlock(info, block, view, assignRef, element, viewsHolder, onEventMethods, null);
	}
	
	public void putAssignInBlock(IdInfoHolder info, JBlock block, IJExpression view, 
			IJExpression assignRef, Element element, 
			ViewsHolder viewsHolder, OnEventMethods onEventMethods,
			String layoutItemId) {
		
		String idName = info.idName;
		TypeMirror type = info.type;
		String viewClass = info.viewClass;		
		org.w3c.dom.Element node = idName != null? viewsHolder.getDomElementFromId(idName, layoutItemId) : null;
		
		if (!view.equals(_this())) {
			block = block._if(view.ne(_null()))._then();
		}
		
		//If the method is declared with VOID, the first parameter is assumed to be the View component.
		if (type.getKind().equals(TypeKind.VOID)) {
			if (info.extraParams.size() > 0) {
				assignRef = ((JInvocation)assignRef).arg(view);
				
				for (int i = 1; i < info.extraParams.size(); i++) {
					final String viewId = info.extraParams.get(i);
					assignRef = ParamUtils.injectParam(viewId, info.type.toString(), (JInvocation) assignRef, viewsHolder);
				}
				
				block.add((JInvocation)assignRef);
				return;
			}
		}		
		
		if (assignRef instanceof JInvocation) {
			for (String param : info.extraParams) {
				assignRef = ParamUtils.injectParam(param, info.type.toString(), (JInvocation) assignRef, viewsHolder);
			}
		}
		
		IJExpression origAssignRef = assignRef;
		if (!type.toString().equals(String.class.getCanonicalName())) {
			assignRef = getClasses().STRING.staticInvoke("valueOf").arg(assignRef);
		}
			
		boolean thisContext = false;
		IJExpression context = viewsHolder.holder().getContextRef();
		if (context == _this()) {
			thisContext = true;
			context = viewsHolder.getGeneratedClass().staticRef("this");
		}
		
		//Support for getters and setters
		if (info.getterOrSetter != null) {
			block.invoke(view, "set" + info.getterOrSetter).arg(origAssignRef);
			return;
		}
		
		//CompoundButtons, if the param is boolean, it will set the checked state
		if (TypeUtils.isSubtype(viewClass, "android.widget.CompoundButton", getProcessingEnvironment())) {
			if (type.getKind().equals(TypeKind.BOOLEAN)) {
				block.invoke(view, "setChecked").arg(origAssignRef);
				return;
			}
		}
		
		if (TypeUtils.isSubtype(viewClass, "android.widget.TextView", getProcessingEnvironment())) {
			if (TypeUtils.isSubtype(type.toString(), "android.text.Spanned", getProcessingEnvironment())) {
				block.invoke(view, "setText").arg(origAssignRef);	
			} else {
				block.invoke(view, "setText").arg(assignRef);
			}			
			return;
		}
		
		if (TypeUtils.isSubtype(viewClass, "android.widget.ImageView", getProcessingEnvironment())) {
			AbstractJClass Picasso = getJClass("com.squareup.picasso.Picasso");
			
			JInvocation PicassoBuilder = Picasso.staticInvoke("with").arg(context)
								    .invoke("load");

			if (type.toString().equals(String.class.getCanonicalName())) {
				PicassoBuilder = PicassoBuilder.arg(assignRef);
			} else {
				PicassoBuilder = PicassoBuilder.arg(origAssignRef);
			}
			
			if (node != null && node.hasAttribute("android:src")) {
				String src = node.getAttribute("android:src");
				String srcId = src.substring(src.lastIndexOf('/')+1);
				PicassoBuilder = PicassoBuilder.invoke("placeholder")
						.arg(getEnvironment().getRClass().get(Res.DRAWABLE).getIdStaticRef(srcId, getEnvironment()));
			}
		
			JBlock ifBlock;
			if (type.toString().equals(String.class.getCanonicalName())) {
				ifBlock = block._if(assignRef.invoke("equals").arg("").not())._then();
			} else {
				ifBlock = block.blockVirtual();			
			}
			
			if (!thisContext) ifBlock = ifBlock._if(context.ne(_null()))._then();
			ifBlock.add(PicassoBuilder.invoke("into").arg(view));
			
			return;
		}		
		
		if (TypeUtils.isSubtype(viewClass, "android.widget.AdapterView", getProcessingEnvironment())
			|| TypeUtils.isSubtype(viewClass, "android.support.v7.widget.RecyclerView", getProcessingEnvironment())) {
			
			String className = info.type.toString();
			boolean isList = TypeUtils.isSubtype(className, "java.util.Collection", getProcessingEnvironment());		
			if (isList) {
				Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(className);
				if (matcher.find()) {
					className = matcher.group(1);
					String originalClassName = className;
					if (className.endsWith(ModelConstants.generationSuffix())) {
						className = TypeUtils.typeFromTypeString(className, getEnvironment());
						originalClassName = className;
						className = className.substring(0, className.length()-1);
					}
					
					processList(viewClass, info.idName, originalClassName, origAssignRef, block, element, viewsHolder, onEventMethods);
					
					return;
				}
			} 
		}
		
		block.invoke("assignField").arg(view).arg(assignRef);
	}

	private void createViewsForPopulateMethod(String viewName, Element element, ViewsHolder viewsHolder) {
		Map<String, ExecutableElement> methods = populatorMethods.get(element.getEnclosingElement());
		if (methods == null) return;
		
		ExecutableElement exeElem = methods.get(viewName);
		if (exeElem != null) {
			
			for (VariableElement param : exeElem.getParameters()) {
				String fieldName = param.getSimpleName().toString();
				if (viewsHolder.layoutContainsId(fieldName)) {
					viewsHolder.createAndAssignView(fieldName);
				}
			}
		}
	}
	
	public void callPopulateSupportMethod(String viewName, JBlock block, IJExpression viewHolder, List<String> fields, 
			Element element, ViewsHolder viewsHolder) {
		Map<String, ExecutableElement> methods = populatorMethods.get(element.getEnclosingElement());
		if (methods == null) return;
		
		ExecutableElement exeElem = methods.get(viewName);
		if (exeElem != null) {
			JInvocation invoke = block.invoke(viewName);
			
			for (VariableElement param : exeElem.getParameters()) {
				final String paramName = param.getSimpleName().toString();
				final String paramType = param.asType().toString();
				
				if (viewHolder != null && viewHolder != _this() && fields.contains(paramName)) {
					invoke.arg(viewHolder.ref(paramName + DeclexConstant.VIEW));
				} else {
					ParamUtils.injectParam(paramName, paramType, invoke, viewsHolder);
				}
			}
		}
	}

	private void processEventsInViews(Element element, final ViewsHolder viewsHolder) {
		
		Map<Class<?>, Object> listenerHolders = viewsHolder.holder().getPluginHolders();
		for (Object listenerHolderObject : listenerHolders.values()) {
			if (!ViewListenerHolder.class.isInstance(listenerHolderObject)) continue;
			final ViewListenerHolder listenerHolder = (ViewListenerHolder)listenerHolderObject;
			
			for (String viewId : listenerHolder.getViewFieldNames()) {
				if (!viewsHolder.layoutContainsId(viewId)) continue;
				
				viewsHolder.createAndAssignView(viewId, new IWriteInBloc() {

					@Override
					public void writeInBlock(String viewName, AbstractJClass viewClass,
							JFieldRef view, JBlock block) {
						listenerHolder.createListener(viewName, block);
					}
				});
			}
		}
	}
	
	public static class OnEventMethods {
		JBlock loadOnEventBlock;
		JMethod populateFieldMethod;
		JBlock populateFieldMethodBody;
		
		public OnEventMethods(JBlock loadOnEventBlock, JMethod populateFieldMethod, GeneratedClassHolder holder) {
			super();
			this.loadOnEventBlock = loadOnEventBlock;
			this.populateFieldMethod = populateFieldMethod;
			
			if (populateFieldMethod != null) {
				final Classes classes = holder.getEnvironment().getClasses();				
				final JFieldRef afterPopulate = ref("afterPopulate");				
				
				JTryBlock tryBlock = populateFieldMethod.body()._try();
				{//Catch block
					JCatchBlock catchBlock = tryBlock._catch(classes.THROWABLE);
					JVar caughtException = catchBlock.param("e");
								
					IJStatement uncaughtExceptionCall = classes.THREAD 
							.staticInvoke("getDefaultUncaughtExceptionHandler") 
							.invoke("uncaughtException") 
							.arg(classes.THREAD.staticInvoke("currentThread")) 
							.arg(caughtException);
					
					JFieldRef onFailed = ref("onFailed");
					JConditional ifOnFailedAssigned = catchBlock.body()._if(onFailed.ne(_null()));
					ifOnFailedAssigned._then().invoke(onFailed, "onFailed").arg(caughtException);
					ifOnFailedAssigned._else().add(uncaughtExceptionCall);
				}
				this.populateFieldMethodBody = tryBlock.body().block();
				
				
				tryBlock.body()._if(afterPopulate.ne(_null()))._then()
				               .invoke(afterPopulate, "run");
			}
		}	
	}
}
