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
package com.dspot.declex.adapter;

import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;
import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;
import org.androidannotations.rclass.IRClass.Res;

import com.dspot.declex.adapter.plugin.BaseClassPlugin;
import com.dspot.declex.annotation.UseModel;
import com.dspot.declex.handler.PopulateHandler;
import com.dspot.declex.holder.ViewsHolder;
import com.dspot.declex.holder.ViewsHolder.ICreateViewListener;
import com.dspot.declex.holder.ViewsHolder.IWriteInBloc;
import com.dspot.declex.holder.ViewsHolder.IdInfoHolder;
import com.dspot.declex.holder.view_listener.ViewListenerHolder;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.AbstractJType;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JClassAlreadyExistsException;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class ViewAdapterPopulator extends BaseClassPlugin {
	
	protected static final Logger LOGGER = LoggerFactory.getLogger(ViewAdapterPopulator.class);
	
	private PopulateHandler handler;
	private ViewsHolder viewsHolder;
	
	private String adapterClassName;
	private String modelClassName;
	private String fieldName;
	
	public ViewAdapterPopulator(PopulateHandler handler, String fieldName, String adapterClassName, String modelClassName, 
			ViewsHolder viewsHolder) {
		super(viewsHolder.environment());
		
		this.handler = handler;
		this.fieldName = fieldName;
		this.adapterClassName = adapterClassName;
		this.modelClassName = modelClassName;
		this.viewsHolder = viewsHolder;

	}
		
	@Override
	public void process(Element element, JDefinedClass AdapterClass) {
		JMethod getViewMethod = AdapterClass.getMethod("getView", new AbstractJType[]{getCodeModel().INT, getClasses().VIEW, getClasses().VIEW_GROUP});
		JFieldRef position = ref("position");
		JFieldRef convertView = ref("convertView");
		JFieldRef parent = ref("parent");
		
		String viewHolderClassName = adapterClassName + "ViewHolder";
		JDefinedClass ViewHolderClass = null;
		try {
			ViewHolderClass = viewsHolder.getGeneratedClass()._class(JMod.PRIVATE | JMod.STATIC, viewHolderClassName);
		} catch (JClassAlreadyExistsException e) {}
		
		AbstractJClass Model = getJClass(modelClassName);
		
		JFieldRef inflater = ref("inflater");
		JFieldRef models = _this().ref("models");
		
		JBlock methodBody = getViewMethod.body();
		
		JFieldRef contentViewId = null;
		String listItemId = null;
		String defLayoutId = viewsHolder.getDefLayoutId();
		
		//Read the Layout from the XML file
		org.w3c.dom.Element node = viewsHolder.getDomElementFromId(fieldName);
		if (node.hasAttribute("tools:listitem")) {
			String listItem = node.getAttribute("tools:listitem");
			listItemId = listItem.substring(listItem.lastIndexOf('/')+1);
			contentViewId = environment.getRClass().get(Res.LAYOUT).getIdStaticRef(listItemId, environment);
			
			viewsHolder.addLayout(listItemId);
			viewsHolder.setDefLayoutId(listItemId);
		} 
		
		Map<String, IdInfoHolder> fields = new HashMap<String, IdInfoHolder>();
		Map<String, IdInfoHolder> methods = new HashMap<String, IdInfoHolder>();
		if (!modelClassName.equals(String.class.getCanonicalName())) {
			String className = modelClassName;
			if (className.endsWith(ModelConstants.generationSuffix())) {
				className = className.substring(0, className.length()-1);
			}
			viewsHolder.findFieldsAndMethods(className, fieldName, element, fields, methods, true, true, listItemId);
		}

		
		JMethod inflaterMethod = AdapterClass.getMethod("inflate", new AbstractJType[]{getCodeModel().INT, getClasses().VIEW, getClasses().VIEW_GROUP, getClasses().LAYOUT_INFLATER});
		JConditional conditional = inflaterMethod.body()._if(convertView.eq(_null()).cor(convertView.invoke("getTag").eq(_null()).cor(convertView.invoke("getTag")._instanceof(ViewHolderClass).not())));
		conditional._then()._return(inflater.invoke("inflate").arg(contentViewId).arg(parent).arg(false));
		conditional._else()._return(convertView);

		final JVar rootView = methodBody.decl(
				getClasses().VIEW, 
				"rootView",
				_this().invoke("inflate").arg(position).arg(convertView).arg(parent).arg(inflater)
			);
		
		final JVar viewHolder = methodBody.decl(
				JMod.FINAL,
				ViewHolderClass, 
				"viewHolder"
			);
				
		conditional = methodBody._if(rootView.ne(convertView));
		final JBlock createViewBody = conditional._then();
		JBlock useConvertViewBody = conditional._else();		
		createViewBody.assign(viewHolder, _new(ViewHolderClass));

		//Find the Views by Id and fill the viewHolder
		List<String> fieldNames = new LinkedList<>();
		if (modelClassName.equals(String.class.getCanonicalName())) {
			String viewClass = viewsHolder.getClassNameFromId("text");
			if (viewClass != null) {				
				final String holderFieldName = "text";
				
				AbstractJClass idNameClass = getJClass(viewClass);
				JFieldRef idRef = environment.getRClass().get(Res.ID).getIdStaticRef(holderFieldName, environment);				
				JVar viewField = ViewHolderClass.field(JMod.PUBLIC, idNameClass, holderFieldName + DeclexConstant.VIEW);
				
				IJExpression findViewById = rootView.invoke("findViewById").arg(idRef);
				if (!idNameClass.equals(CanonicalNameConstants.VIEW))
					findViewById = cast(idNameClass, findViewById);
				
				createViewBody.assign(viewHolder.ref(viewField), findViewById);
				
				fieldNames.add(holderFieldName);
			}
		}
		
		for (String field : fields.keySet()) {
			final String holderFieldName = fields.get(field).idName;
			
			AbstractJClass idNameClass = getJClass(fields.get(field).viewClass);
			JFieldRef idRef = environment.getRClass().get(Res.ID).getIdStaticRef(holderFieldName, environment);
			
			JVar viewField = ViewHolderClass.field(JMod.PUBLIC, idNameClass, holderFieldName + DeclexConstant.VIEW);
			
			IJExpression findViewById = rootView.invoke("findViewById").arg(idRef);
			if (!idNameClass.equals(CanonicalNameConstants.VIEW))
				findViewById = cast(idNameClass, findViewById);
			
			createViewBody.assign(viewHolder.ref(viewField), findViewById);
			
			fieldNames.add(holderFieldName);
		}
		
		for (String methodName : methods.keySet()) {
			final String holderFieldName = methods.get(methodName).idName;
			
			AbstractJClass idNameClass = getJClass(methods.get(methodName).viewClass);
			JFieldRef idRef = environment.getRClass().get(Res.ID).getIdStaticRef(holderFieldName, environment);

			JVar viewField = ViewHolderClass.fields().get(holderFieldName + DeclexConstant.VIEW);
			if (viewField == null) viewField = ViewHolderClass.field(JMod.PUBLIC, idNameClass, methods.get(methodName).idName + DeclexConstant.VIEW);
			
			
			IJExpression findViewById = rootView.invoke("findViewById").arg(idRef);
			if (!idNameClass.equals(CanonicalNameConstants.VIEW))
				findViewById = cast(idNameClass, findViewById);
			
			createViewBody.assign(viewHolder.ref(viewField), findViewById);
			
			fieldNames.add(holderFieldName);
		}	
		
		useConvertViewBody.assign(viewHolder, cast(ViewHolderClass, convertView.invoke("getTag")));
		
		
		boolean castNeeded = false;
		if (!modelClassName.endsWith(ModelConstants.generationSuffix())) {
			if (TypeUtils.isClassAnnotatedWith(modelClassName, UseModel.class, environment)) {
				modelClassName = TypeUtils.getGeneratedClassName(modelClassName, environment);
				castNeeded = true;
				Model = getJClass(modelClassName);
			}
		}
		
		//Listener to create the findViewById for every created view
		final JDefinedClass FinalViewHolderClass = ViewHolderClass;
		viewsHolder.setCreateViewListener(new ICreateViewListener() {
			
			@Override
			public JFieldRef createView(String viewId, String viewName, 
					AbstractJClass viewClass, JBlock declBlock) {
				
				JVar viewField = FinalViewHolderClass.fields().get(viewName);
				if (viewField == null) { 
					AbstractJClass idNameClass = getJClass(viewsHolder.getClassNameFromId(viewId));	
					JFieldRef idRef = environment.getRClass().get(Res.ID).getIdStaticRef(viewId, environment);
					
					viewField = FinalViewHolderClass.field(JMod.PUBLIC, idNameClass, viewName);
					IJExpression findViewById = rootView.invoke("findViewById").arg(idRef);
					if (!idNameClass.equals(CanonicalNameConstants.VIEW))
						findViewById = cast(idNameClass, findViewById);
					
					createViewBody.assign(viewHolder.ref(viewField), findViewById);
				}
				
				return viewHolder.ref(viewField);
			}
		});
		
		//Get the model
		JVar model = methodBody.decl(JMod.FINAL, Model, "model");
		
		//Synchronize reading the models 
		IJExpression modelAssigner = models.invoke("get").arg(position);
		if (castNeeded) modelAssigner = cast(Model, models.invoke("get").arg(position));
		methodBody.assign(model, modelAssigner);
		
		if (modelClassName.equals(String.class.getCanonicalName())) {
			String viewClass = viewsHolder.getClassNameFromId("text");
			if (viewClass != null) {
				TypeElement stringElement = environment.getProcessingEnvironment().getElementUtils().getTypeElement(String.class.getCanonicalName());
				IdInfoHolder info = new IdInfoHolder("text", stringElement, stringElement.asType(), viewClass, new LinkedList<String>());
				JFieldRef view = viewHolder.ref(info.idName + DeclexConstant.VIEW);
				
				handler.putAssignInBlock(
					info, methodBody, view, model, element, viewsHolder, 
					null, listItemId
				);				
			} else {
				//TODO Assume the hole view it is the object to be assigned	
			}
		}
		
		for (String field : fields.keySet()) {
			IJExpression methodsCall = model;
			JBlock checkForNull = methodBody;
			
			String[] fieldSplit = field.split("\\.");
			int index = 0;
			for (String fieldPart : fieldSplit)
				if (!fieldPart.equals("")) {
					methodsCall = methodsCall.invoke(fieldToGetter(fieldPart));

					if (index < fieldSplit.length-1) {
						checkForNull = checkForNull._if(methodsCall.ne(_null()))._then();	
					} else if (!fields.get(field).type.getKind().isPrimitive()) {
						checkForNull = checkForNull._if(methodsCall.ne(_null()))._then();
					}
					index++;
				}
			
			IdInfoHolder info = fields.get(field);
			JFieldRef view = viewHolder.ref(info.idName + DeclexConstant.VIEW);
			handler.putAssignInBlock(
				info, checkForNull, view, methodsCall, element, viewsHolder, 
				null, listItemId
			);
		}
		
		for (String methodName : methods.keySet()) {
			IJExpression methodsCall = model;
			JBlock checkForNull = methodBody;
			
			String[] methodSplit = methodName.split("\\.");
			for (int i = 0; i < methodSplit.length-1; i++) 
				if (!methodSplit[i].equals("")) {
					methodsCall = methodsCall.invoke(fieldToGetter(methodSplit[i]));
					checkForNull = checkForNull._if(methodsCall.ne(_null()))._then();
				}
			methodsCall = methodsCall.invoke(methodSplit[methodSplit.length-1]);
			
			IdInfoHolder info = methods.get(methodName);
			JFieldRef view = viewHolder.ref(info.idName + DeclexConstant.VIEW);
			handler.putAssignInBlock(
				info, checkForNull, view, methodsCall, element, viewsHolder, 
				null, listItemId
			);
		}	

		//Process Events
		Map<Class<?>, Object> listenerHolders = viewsHolder.holder().getPluginHolders();
		for (Object listenerHolderObject : listenerHolders.values()) {
			if (!ViewListenerHolder.class.isInstance(listenerHolderObject)) continue;
			final ViewListenerHolder listenerHolder = (ViewListenerHolder)listenerHolderObject;
			
			for (String viewId : listenerHolder.getViewFieldNames()) {
				if (!viewsHolder.layoutContainsId(viewId)) continue;
				
				final JBlock eventsBlock = new JBlock();
				viewsHolder.createAndAssignView(viewId, new IWriteInBloc() {

					@Override
					public void writeInBlock(String viewName, AbstractJClass viewClass,
							JFieldRef view, JBlock block) {
						listenerHolder.createListener("viewHolder." + viewName, eventsBlock);
					}
				});
				methodBody.add(eventsBlock);
			}
		}
		
		handler.callPopulateSupportMethod(fieldName, methodBody, viewHolder, fieldNames, element, viewsHolder);
		
		createViewBody.invoke(rootView, "setTag").arg(viewHolder);
		methodBody._return(rootView);
		
		viewsHolder.setCreateViewListener(null);
		viewsHolder.setDefLayoutId(defLayoutId);
	}

	@Override
	public AbstractJClass getBaseAdapter(Element element) {
		return null;
	}
}

