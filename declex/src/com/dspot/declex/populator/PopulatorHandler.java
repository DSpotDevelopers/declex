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
package com.dspot.declex.populator;

import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;
import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.ref;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.internal.process.ProcessHolder.Classes;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;
import org.androidannotations.rclass.IRClass.Res;

import com.dspot.declex.api.action.runnable.OnFailedRunnable;
import com.dspot.declex.api.eventbus.LoadOnEvent;
import com.dspot.declex.api.model.Model;
import com.dspot.declex.api.populator.Populator;
import com.dspot.declex.event.holder.ViewListenerHolder;
import com.dspot.declex.model.ModelHolder;
import com.dspot.declex.plugin.JClassPlugin;
import com.dspot.declex.populator.adapter.AdapterClassCreator;
import com.dspot.declex.populator.adapter.RecyclerViewAdapterClassCreator;
import com.dspot.declex.share.holder.ViewsHolder;
import com.dspot.declex.share.holder.ViewsHolder.IWriteInBloc;
import com.dspot.declex.share.holder.ViewsHolder.IdInfoHolder;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.EventUtils;
import com.dspot.declex.util.ParamUtils;
import com.dspot.declex.util.SharedRecords;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.util.TypeUtils.ClassInformation;
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

public class PopulatorHandler extends BaseAnnotationHandler<EComponentWithViewSupportHolder> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PopulatorHandler.class);
	
	private List<JClassPlugin> adapterPlugins; 

	private Map<Element, Map<String, ExecutableElement>> populatorMethods = new HashMap<>();
	
	private static int uniquePriorityCounter = 1000;
	
	public PopulatorHandler(AndroidAnnotationsEnvironment environment, List<JClassPlugin> adapterPlugins) {
		super(Populator.class, environment);
		
		this.adapterPlugins = adapterPlugins;
	}
	
	@Override
	public void validate(Element element, ElementValidation valid) {
		//Ignore @Populator Methods
		if (element instanceof ExecutableElement) {
			Map<String, ExecutableElement> methods = populatorMethods.get(element.getEnclosingElement());
			if (methods == null) {
				methods = new HashMap<>();
				populatorMethods.put(element.getEnclosingElement(), methods);
			}
			
			methods.put(element.getSimpleName().toString(), (ExecutableElement) element);
			
			return;
		}
		
		
		validatorHelper.enclosingElementHasEnhancedComponentAnnotation(element, valid);

		validatorHelper.isNotPrivate(element, valid);
		
		//Validate special methods
		boolean specialAssignField = false;
		List<? extends Element> elems = element.getEnclosingElement().getEnclosedElements();
		for (Element elem : elems)
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
					
					specialAssignField = true;
					
					break;
				}
				
			}
		
		//Check if the field is a primitive or a String
//		if (element.asType().getKind().isPrimitive() || 
//			element.asType().toString().equals(String.class.getCanonicalName())) {
//			
//			if (!layoutObjects.containsKey(element.getSimpleName().toString())) {
//				valid.addError("The element with Id \"" + element.getSimpleName() + "\" cannot be found in the Layout ");
//			} else if (!specialAssignField) {
//				String className = layoutObjects.get(element.getSimpleName().toString());
//				if (!specialAssignField) {
//					if (!TypeUtils.isSubtype(className, "android.widget.TextView", getProcessingEnvironment()) &&
//						!TypeUtils.isSubtype(className, "android.widget.ImageView", getProcessingEnvironment())) {
//						valid.addError("You should provide an assignField method for the class \"" + className + 
//								"\" used on the field " + element.getSimpleName());
//					}
//				}
//			}
//		}
				
		
		boolean isList = TypeUtils.isSubtype(element, CanonicalNameConstants.LIST, getProcessingEnvironment());		
		if (isList) {
			String className = element.asType().toString();

			Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(className);
			if (!matcher.find()) {
				valid.addError("Cannot infer the List Type from " + className);
			}
						
//			if (!layoutObjects.containsKey(element.getSimpleName().toString())) {
//				valid.addError("The element with Id \"" + element.getSimpleName() + "\" cannot be found in the Layout ");
//			}
		} 		
		
	}

	@Override
	public void process(Element element, EComponentWithViewSupportHolder holder) {		
		uniquePriorityCounter++;
		
		//Ignore @Populator Methods
		if (element instanceof ExecutableElement) return;

		final boolean isPrimitive = element.asType().getKind().isPrimitive() || 
				element.asType().toString().equals(String.class.getCanonicalName());
		
		ClassInformation classInformation = TypeUtils.getClassInformation(element, getEnvironment());
		final String className = classInformation.generatorClassName;
		final String originalClassName = classInformation.originalClassName;
		final boolean isList = classInformation.isList;
		
		final ViewsHolder viewsHolder = holder.getPluginHolder(new ViewsHolder(holder, annotationHelper));
		
		OnEventMethods onEventMethods = getOnEventMethods(className, element, viewsHolder);
		if (onEventMethods == null) return;	
		
		int layoutId = 0;
		int index = 0;
		int[] values = element.getAnnotation(Populator.class).value();
		
		//Restoration objects
		String defLayoutId = viewsHolder.getDefLayoutId();
		Object onViewChangedHasViewsParam = null;
		Field onViewChangedHasViewsParamField = null;
		
		try {
			onViewChangedHasViewsParamField = EComponentWithViewSupportHolder.class.getDeclaredField("onViewChangedHasViewsParam");
			onViewChangedHasViewsParamField.setAccessible(true);			
			
			//It's important to invoke it like this to create the onViewChangedHasViewsParam object
			//in case it has not been created before
			onViewChangedHasViewsParam = holder.getOnViewChangedHasViewsParam();
			
			do {
				if (layoutId != 0) {
					String layoutIdString = viewsHolder.addLayoutWithView(layoutId);
					
					//This will cause' all operations with viewHolder to be executed with the new layout
					viewsHolder.setDefLayoutId(layoutIdString);
					
					//This is to use the view of the layout to findByViewId the Views
					JVar layoutViewVar = new JBlock().decl(getClasses().VIEW, layoutIdString + DeclexConstant.VIEW);
					onViewChangedHasViewsParamField.set(holder,  layoutViewVar);
					
					processEventsInViews(element, viewsHolder);
				}
				
				//Check if the field is a primitive or a String
				if (isPrimitive) {
					
					processPrimitive(className, element, viewsHolder, onEventMethods);
					
				} else if (isList) {
					
					if (layoutId == 0) {
						processList(originalClassName, element, viewsHolder, onEventMethods);					
					}
					
				} else {
					
					processModel(className, element, viewsHolder, onEventMethods);
					
				}		
				
				createViewsForPopulatorMethod(element.getSimpleName().toString(), element, viewsHolder);
				
				if (index >= values.length) break;
				layoutId = values[index];
				index++;
				
//				if (element.getAnnotation(Populator.class).debug())
//					LOGGER.warn("\nPopulator layouts: " + getLayoutNodes(element), element, element.getAnnotation(Populator.class));
				
			} while (layoutId != 0);
			
			if (!isList) {
				callPopulatorMethod(
						element.getSimpleName().toString(), 
						holder.getOnViewChangedBody(), 
						null, null, element, viewsHolder
				);			
			}			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			//Restore the objects of the holders
			viewsHolder.setDefLayoutId(defLayoutId);
			try {
				onViewChangedHasViewsParamField.set(holder,  onViewChangedHasViewsParam);
			} catch (Exception e) {}
		}
		
	}
	
	private OnEventMethods getOnEventMethods(String className, Element element, ViewsHolder viewsHolder) {
		final String fieldName = element.getSimpleName().toString();
		
		//If there's a LoadOnEvent annotation, then instantiate the object only on that event
		JMethod loadOnEventMethod = null;
		LoadOnEvent loadOnEvent = element.getAnnotation(LoadOnEvent.class);
		if (loadOnEvent != null) {
			String classField = TypeUtils.getClassFieldValue(element, LoadOnEvent.class.getCanonicalName(), "value", getEnvironment());
			String eventClass = SharedRecords.getEvent(classField, getEnvironment());
			
			if (classField == null || eventClass == null) {
				LOGGER.error("@LoadOnEvent failed in " + className, element, loadOnEvent);
				return null;
			}

			loadOnEventMethod = EventUtils.getEventMethod(eventClass, element.getEnclosingElement(), viewsHolder, getEnvironment());
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
			
		Model model = element.getAnnotation(Model.class); 
		if (model != null) {
			ModelHolder modelHolder = viewsHolder.holder().getPluginHolder(new ModelHolder(viewsHolder.holder()));
			JBlock methodBody = modelHolder.getAfterGetModelBlock(element);
			
			if (model.async()) {
				JDefinedClass annonimousRunnable = getCodeModel().anonymousClass(Runnable.class);
				JMethod annonimousRunnableRun = annonimousRunnable.method(JMod.PUBLIC, getCodeModel().VOID, "run");
				annonimousRunnableRun.annotate(Override.class);
				
				JVar handler = methodBody.decl(getClasses().HANDLER, "handler", 
						_new(getClasses().HANDLER).arg(getClasses().LOOPER.staticInvoke("getMainLooper")));
				methodBody.invoke(handler, "post").arg(_new(annonimousRunnable));
				
				methodBody = annonimousRunnableRun.body();
			} 
			
			methodBody.invoke(populateFieldMethod).arg(_null()).arg(ref("onFailed"));
		}
		
		return new OnEventMethods(loadOnEventMethod, populateFieldMethod, getClasses());
	}
	
	private void processPrimitive(String className, Element element, ViewsHolder viewsHolder, OnEventMethods onEventMethods) {
			
		String fieldName = element.getSimpleName().toString();
		IJExpression assignRef = ref(fieldName);
		
		if (element.getAnnotation(Populator.class).debug())
			LOGGER.warn("\nField: " + fieldName, element, element.getAnnotation(Populator.class));
		
		TypeElement typeElement = getProcessingEnvironment().getElementUtils().getTypeElement(className);
		if (typeElement.asType().getKind().isPrimitive()) {
			assignRef = getClasses().STRING.staticInvoke("valueOf").arg(assignRef);
		}
		
		JFieldRef view = viewsHolder.createAndAssignView(fieldName);
		IdInfoHolder info = new IdInfoHolder(
			fieldName, element, element.asType(), 
			viewsHolder.getClassNameFromId(fieldName), new LinkedList<String>()
		);

		JBlock block = onEventMethods.populateFieldMethodBody.block();
		putAssignInBlock(info, block, view, assignRef, element, viewsHolder, onEventMethods);	
		
		SharedRecords.priorityAdd(
				viewsHolder.holder().getOnViewChangedBody(), 
				JExpr.invoke(onEventMethods.populateFieldMethod).arg(_null()).arg(_null()), 
				uniquePriorityCounter
			);
	}
	
	private void processList(String className, Element element, ViewsHolder viewsHolder, final OnEventMethods onEventMethods) {
		final String fieldName = element.getSimpleName().toString();
		this.processList(viewsHolder.getClassNameFromId(fieldName), fieldName, className, ref(fieldName), onEventMethods.populateFieldMethodBody, element, viewsHolder, onEventMethods);
		
		SharedRecords.priorityAdd(
				viewsHolder.holder().getOnViewChangedBody(), 
				JExpr.invoke(onEventMethods.populateFieldMethod).arg(_null()).arg(_null()), 
				uniquePriorityCounter
			);
	}
	
	private void processList(String viewClass, String fieldName, String className, final IJExpression assignRef, final JBlock block, 
			Element element, ViewsHolder viewsHolder, final OnEventMethods onEventMethods) {
		
		final String adapterName = fieldName + "$adapter";
		final String adapterClassName = adapterName.substring(0, 1).toUpperCase() + adapterName.substring(1);
		final AbstractJClass AdapterClass = getJClass(adapterClassName);
		final JFieldRef adapter = ref(adapterName);

		final JConditional ifNotifBlock = block._if(adapter.ne(_null()));
		JBlock notifyBlock = ifNotifBlock._then();
		
		final boolean foundAdapterDeclaration = TypeUtils.fieldInElement(adapterName, element.getEnclosingElement());
		final Populator annotation = element.getAnnotation(Populator.class);
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
				LOGGER.error("Tying to create a List Adapter twice for field " + fieldName, element, element.getAnnotation(Populator.class));
				return;
			}
					
			viewsHolder.getGeneratedClass().field(JMod.PRIVATE, AdapterClass, adapterName);
		}
			
		
		if (onEventMethods != null && onEventMethods.loadOnEventMethod != null) {
			if (foundAdapterDeclaration && annotation.custom()) {
				onEventMethods.loadOnEventMethod.body().assign(adapter, _new(AdapterClass).arg(assignRef));			
				onEventMethods.loadOnEventMethod.body().invoke(view, "setAdapter").arg(adapter);
				return;
			}
			
			onEventMethods.loadOnEventMethod.body().invoke(view, "setAdapter").arg(adapter);
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
		
		if (element.getAnnotation(Populator.class).debug())
			LOGGER.warn("\nClass: " + className + "\nFields: " + fields + "\nMethods: " + methods, element, element.getAnnotation(Populator.class));
		
		/*See how to integrate Plugins to the @Populators and Recollectors
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
		
		boolean castNeeded = false;
		String className = element.asType().toString();
		if (!className.endsWith(ModelConstants.generationSuffix())) {
			if (SharedRecords.getModel(className, getEnvironment()) != null) {
				className = className + ModelConstants.generationSuffix();
				castNeeded = true;
			}
		}
		
		IJExpression assignRef = castNeeded ? cast(getJClass(className), ref(fieldName)) : ref(fieldName);
		IJExpression methodsCall = assignRef;
		JBlock checkForNull = new JBlock();
		JBlock changedBlock = checkForNull._if(ref(fieldName).ne(_null()))._then();
		
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
	
	private void putAssignInBlock(IdInfoHolder info, JBlock block, JFieldRef view, 
			IJExpression assignRef, Element element, 
			ViewsHolder viewsHolder, OnEventMethods onEventMethods) {
		this.putAssignInBlock(info, block, view, assignRef, element, viewsHolder, onEventMethods, null);
	}
	
	public void putAssignInBlock(IdInfoHolder info, JBlock block, JFieldRef view, 
			IJExpression assignRef, Element element, 
			ViewsHolder viewsHolder, OnEventMethods onEventMethods,
			String layoutItemId) {
		
		String idName = info.idName;
		TypeMirror type = info.type;
		String viewClass = info.viewClass;		
		org.w3c.dom.Element node = viewsHolder.getXMLElementFromId(idName, layoutItemId);
		
		block = block._if(view.ne(_null()))._then();
		
		//If the method is declared with VOID, the first parameter is assumed to be the View component.
		if (type.getKind().equals(TypeKind.VOID)) {
			if (info.extraParams.size() > 0) {
				assignRef = ((JInvocation)assignRef).arg(view);
				
				for (int i = 1; i < info.extraParams.size(); i++) {
					final String viewId = info.extraParams.get(i);
					assignRef = ParamUtils.injectParam(viewId, (JInvocation) assignRef, viewsHolder);
				}
				
				block.add((JInvocation)assignRef);
				return;
			}
		}		
		
		if (assignRef instanceof JInvocation) {
			for (String param : info.extraParams) {
				assignRef = ParamUtils.injectParam(param, (JInvocation) assignRef, viewsHolder);
			}
		}
		
		IJExpression origAssignRef = assignRef;
		if (!type.toString().equals(String.class.getCanonicalName())) {
			assignRef = getClasses().STRING.staticInvoke("valueOf").arg(assignRef);
		}
			
		IJExpression context = viewsHolder.holder().getContextRef();
		if (context == _this()) {
			context = viewsHolder.getGeneratedClass().staticRef("this");
		}
		
		//CompoundButtons, if the param is boolean, it will set the checked state
		if (TypeUtils.isSubtype(viewClass, "android.widget.CompoundButton", getProcessingEnvironment())) {
			if (type.getKind().equals(TypeKind.BOOLEAN)) {
				block.invoke(view, "setChecked").arg(origAssignRef);
				return;
			}
		}
		
		if (TypeUtils.isSubtype(viewClass, "android.widget.TextView", getProcessingEnvironment())) {
			block.invoke(view, "setText").arg(assignRef);
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
			
			if (node.hasAttribute("android:src")) {
				String src = node.getAttribute("android:src");
				String srcId = src.substring(src.lastIndexOf('/')+1);
				PicassoBuilder = PicassoBuilder.invoke("placeholder")
						.arg(getEnvironment().getRClass().get(Res.DRAWABLE).getIdStaticRef(srcId, getEnvironment()));
			}
		
			if (type.toString().equals(String.class.getCanonicalName())) {
				block._if(assignRef.invoke("equals").arg("").not())._then()
					 .add(PicassoBuilder.invoke("into").arg(view));	
			} else {
				block.add(PicassoBuilder.invoke("into").arg(view));
			}
			return;
		}		
		
		if (TypeUtils.isSubtype(viewClass, "android.widget.AdapterView", getProcessingEnvironment())) {
			
			String className = info.type.toString();
			boolean isList = TypeUtils.isSubtype(className, "java.util.Collection", getProcessingEnvironment());		
			if (isList) {
				Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(className);
				if (matcher.find()) {
					className = matcher.group(1);
					String originalClassName = className;
					if (className.endsWith("_")) {
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

	private void createViewsForPopulatorMethod(String viewName, Element element, ViewsHolder viewsHolder) {
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
	
	public void callPopulatorMethod(String viewName, JBlock block, IJExpression viewHolder, List<String> fields, 
			Element element, ViewsHolder viewsHolder) {
		Map<String, ExecutableElement> methods = populatorMethods.get(element.getEnclosingElement());
		if (methods == null) return;
		
		ExecutableElement exeElem = methods.get(viewName);
		if (exeElem != null) {
			JInvocation invoke = block.invoke(viewName);
			
			for (VariableElement param : exeElem.getParameters()) {
				final String paramName = param.getSimpleName().toString();
				
				if (viewHolder != null && viewHolder != _this() && fields.contains(paramName)) {
					invoke.arg(viewHolder.ref(paramName + DeclexConstant.VIEW));
				} else {
					ParamUtils.injectParam(paramName, invoke, viewsHolder);
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
		JMethod loadOnEventMethod;
		JMethod populateFieldMethod;
		JBlock populateFieldMethodBody;
		
		public OnEventMethods(JMethod loadOnEventMethod, JMethod populateFieldMethod, Classes classes) {
			super();
			this.loadOnEventMethod = loadOnEventMethod;
			this.populateFieldMethod = populateFieldMethod;
			
			if (populateFieldMethod != null) {
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
				
				JFieldRef afterPopulate = ref("afterPopulate");
				tryBlock.body()._if(afterPopulate.ne(_null()))._then()
				               .invoke(afterPopulate, "run");
			}
		}	
	}
}
