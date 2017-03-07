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
package com.dspot.declex.viewsinjection;

import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;
import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.helper.TargetAnnotationHelper;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;
import org.androidannotations.rclass.IRClass;
import org.androidannotations.rclass.IRClass.Res;

import com.dspot.declex.event.holder.ViewListenerHolder;
import com.dspot.declex.plugin.BaseClassPlugin;
import com.dspot.declex.share.holder.ViewsHolder;
import com.dspot.declex.share.holder.ViewsHolder.ICreateViewListener;
import com.dspot.declex.share.holder.ViewsHolder.IWriteInBloc;
import com.dspot.declex.share.holder.ViewsHolder.IdInfoHolder;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.SharedRecords;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.AbstractJType;
import com.helger.jcodemodel.IJAssignmentTarget;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JClassAlreadyExistsException;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

class RecyclerViewAdapterPopulator extends BaseClassPlugin {

	protected static final Logger LOGGER = LoggerFactory
			.getLogger(RecyclerViewAdapterPopulator.class);

	private PopulateHandler handler;
	private ViewsHolder viewsHolder;

	private String adapterClassName;
	private String modelClassName;
	private String fieldName;

	private TargetAnnotationHelper annotationHelper;

	public RecyclerViewAdapterPopulator(PopulateHandler handler,
			String fieldName, String adapterClassName, String modelClassName,
			ViewsHolder viewsHolder) {
		super(viewsHolder.environment());

		this.handler = handler;
		this.fieldName = fieldName;
		this.adapterClassName = adapterClassName;
		this.modelClassName = modelClassName;
		this.viewsHolder = viewsHolder;

		annotationHelper = new TargetAnnotationHelper(environment,
				handler.getTarget());
	}

	@Override
	public void process(Element element, JDefinedClass AdapterClass) {

		String viewHolderClassName = adapterClassName + "ViewHolder";
		JDefinedClass ViewHolderClass = null;
		try {
			final AbstractJClass RecyclerViewHolder = getJClass("android.support.v7.widget.RecyclerView.ViewHolder");
			ViewHolderClass = viewsHolder.getGeneratedClass()
					._class(JMod.PRIVATE | JMod.STATIC, viewHolderClassName)
					._extends(RecyclerViewHolder);

			JMethod constructor = ViewHolderClass.constructor(JMod.PUBLIC);
			constructor.param(getClasses().VIEW, "view");
			constructor.body().directStatement("super(view);");

		} catch (JClassAlreadyExistsException e) {
		}

		AbstractJClass Model = getJClass(modelClassName);

		JFieldRef models = _this().ref("models");

		// =========================onCreateViewHolder
		// Method======================

		JMethod onCreateViewHolder = AdapterClass.getMethod(
				"onCreateViewHolder", new AbstractJType[] {
						getClasses().VIEW_GROUP, getCodeModel().INT });
		JFieldRef parent = ref("parent");
		JFieldRef position = ref("position");
		JFieldRef viewType = ref("viewType");
		JFieldRef inflater = ref("inflater");
		final JBlock onCreateViewMethodBody = onCreateViewHolder.body();

		JFieldRef contentViewId = null;
		String listItemId = null;
		String defLayoutId = viewsHolder.getDefLayoutId();

		// Read the Layout from the XML file
		org.w3c.dom.Element node = viewsHolder.getDomElementFromId(fieldName);
		if (node.hasAttribute("tools:listitem")) {
			String listItem = node.getAttribute("tools:listitem");
			listItemId = listItem.substring(listItem.lastIndexOf('/') + 1);
			contentViewId = environment.getRClass().get(Res.LAYOUT)
					.getIdStaticRef(listItemId, environment);

			viewsHolder.addLayout(listItemId);
			viewsHolder.setDefLayoutId(listItemId);
		} else {
			// If the layout is not found, read it from the param value of the
			// annotation
			List<JFieldRef> fieldRefs = annotationHelper
					.extractAnnotationFieldRefs(element, handler.getTarget(),
							environment.getRClass().get(IRClass.Res.LAYOUT),
							false);
			if (fieldRefs.size() == 1) {
				contentViewId = fieldRefs.get(0);
			}
			if (contentViewId == null)
				return;

			// TODO not sure of behavior
		}

		// if (element.getAnnotation(Populator.class).debug())
		// LOGGER.warn("\nPopulator layouts: " + layoutObjects, element,
		// element.getAnnotation(Populator.class));

		Map<String, IdInfoHolder> fields = new HashMap<String, IdInfoHolder>();
		Map<String, IdInfoHolder> methods = new HashMap<String, IdInfoHolder>();
		if (!modelClassName.equals(String.class.getCanonicalName())) {
			String className = modelClassName;
			if (className.endsWith("_")) {
				className = className.substring(0, className.length() - 1);
			}
			viewsHolder.findFieldsAndMethods(className, fieldName, element,
					fields, methods, true, true, listItemId);
		}
		
		JMethod inflaterMethod = AdapterClass.getMethod("inflate", new AbstractJType[]{getCodeModel().INT, getClasses().VIEW_GROUP, getClasses().LAYOUT_INFLATER});
		inflaterMethod.body()._return(inflater.invoke("inflate").arg(contentViewId).arg(parent).arg(false));

		final JVar rootView = onCreateViewMethodBody.decl(getClasses().VIEW,
				"rootView", invoke("inflate").arg(viewType).arg(parent).arg(inflater));

		IJAssignmentTarget viewHolder = onCreateViewMethodBody.decl(
				ViewHolderClass, "viewHolder",
				_new(ViewHolderClass).arg(rootView));

		List<String> fieldNames = new LinkedList<>();
		if (modelClassName.equals(String.class.getCanonicalName())) {
			String viewClass = viewsHolder.getClassNameFromId("text");
			if (viewClass != null) {
				final String holderFieldName = "text";

				AbstractJClass idNameClass = getJClass(viewClass);
				JFieldRef idRef = environment.getRClass().get(Res.ID)
						.getIdStaticRef(holderFieldName, environment);

				JVar viewField = ViewHolderClass.field(JMod.PUBLIC,
						idNameClass, holderFieldName + DeclexConstant.VIEW);

				IJExpression findViewById = rootView.invoke("findViewById")
						.arg(idRef);
				if (!idNameClass.equals(CanonicalNameConstants.VIEW))
					findViewById = cast(idNameClass, findViewById);

				onCreateViewMethodBody.assign(viewHolder.ref(viewField),
						findViewById);

				fieldNames.add(holderFieldName);
			}
		}

		for (String field : fields.keySet()) {
			final String holderFieldName = fields.get(field).idName;

			AbstractJClass idNameClass = getJClass(fields.get(field).viewClass);
			JFieldRef idRef = environment.getRClass().get(Res.ID)
					.getIdStaticRef(holderFieldName, environment);

			JVar viewField = ViewHolderClass.field(JMod.PUBLIC, idNameClass,
					holderFieldName + DeclexConstant.VIEW);

			IJExpression findViewById = rootView.invoke("findViewById").arg(
					idRef);
			if (!idNameClass.equals(CanonicalNameConstants.VIEW))
				findViewById = cast(idNameClass, findViewById);

			onCreateViewMethodBody.assign(viewHolder.ref(viewField),
					findViewById);

			fieldNames.add(holderFieldName);
		}

		for (String methodName : methods.keySet()) {
			final String holderFieldName = methods.get(methodName).idName;

			AbstractJClass idNameClass = getJClass(methods.get(methodName).viewClass);
			JFieldRef idRef = environment.getRClass().get(Res.ID)
					.getIdStaticRef(holderFieldName, environment);

			JVar viewField = ViewHolderClass.fields().get(
					holderFieldName + DeclexConstant.VIEW);
			if (viewField == null)
				viewField = ViewHolderClass.field(JMod.PUBLIC, idNameClass,
						methods.get(methodName).idName + DeclexConstant.VIEW);

			IJExpression findViewById = rootView.invoke("findViewById").arg(
					idRef);
			if (!idNameClass.equals(CanonicalNameConstants.VIEW))
				findViewById = cast(idNameClass, findViewById);

			onCreateViewMethodBody.assign(viewHolder.ref(viewField),
					findViewById);

			fieldNames.add(holderFieldName);
		}

		// =========================onBindViewHolder
		// Method======================

		JMethod onBindViewHolder = AdapterClass.getMethod("onBindViewHolder",
				new AbstractJType[] { getJClass(viewHolderClassName),
						getCodeModel().INT });
		viewHolder = ref("viewHolder");
		position = ref("position");
		JBlock onBindMethodBody = onBindViewHolder.body();

		onBindMethodBody.decl(getClasses().VIEW, "rootView",
				viewHolder.ref("itemView"));

		boolean castNeeded = false;
		if (!modelClassName.endsWith(ModelConstants.generationSuffix())) {
			if (SharedRecords.getModel(modelClassName, environment) != null) {
				modelClassName = modelClassName
						+ ModelConstants.generationSuffix();
				castNeeded = true;
				Model = getJClass(modelClassName);
			}
		}

		// Listener to create the findViewById for every created view
		final JDefinedClass FinalViewHolderClass = ViewHolderClass;
		final IJAssignmentTarget finalViewHolder = viewHolder;
		viewsHolder.setCreateViewListener(new ICreateViewListener() {

			@Override
			public JFieldRef createView(String viewId, String viewName,
					AbstractJClass viewClass, JBlock declBlock) {

				JVar viewField = FinalViewHolderClass.fields().get(
						viewId + DeclexConstant.VIEW);
				if (viewField == null) {
					AbstractJClass idNameClass = getJClass(viewsHolder
							.getClassNameFromId(viewId));
					JFieldRef idRef = environment.getRClass().get(Res.ID)
							.getIdStaticRef(viewId, environment);

					viewField = FinalViewHolderClass.field(JMod.PUBLIC,
							idNameClass, viewId + DeclexConstant.VIEW);
					IJExpression findViewById = rootView.invoke("findViewById")
							.arg(idRef);
					if (!idNameClass.equals(CanonicalNameConstants.VIEW))
						findViewById = cast(idNameClass, findViewById);

					onCreateViewMethodBody.assign(
							finalViewHolder.ref(viewField), findViewById);
				}

				return finalViewHolder.ref(viewField);
			}
		});
		
		// Get the model
		JVar model = onBindMethodBody.decl(JMod.FINAL, Model, "model");		
		
		IJExpression modelAssigner = models.invoke("get").arg(position);
		if (castNeeded)	modelAssigner = cast(Model, models.invoke("get").arg(position));
		onBindMethodBody.assign(model, modelAssigner);
		
		if (modelClassName.equals(String.class.getCanonicalName())) {
			String viewClass = viewsHolder.getClassNameFromId("text");
			if (viewClass != null) {
				TypeElement stringElement = environment
						.getProcessingEnvironment().getElementUtils()
						.getTypeElement(String.class.getCanonicalName());
				IdInfoHolder info = new IdInfoHolder("text", stringElement,
						stringElement.asType(), viewClass,
						new LinkedList<String>());
				JFieldRef view = viewHolder.ref(info.idName
						+ DeclexConstant.VIEW);

				handler.putAssignInBlock(info, onBindMethodBody, view, model,
						element, viewsHolder, null,
						listItemId);
			}
		}

		for (String field : fields.keySet()) {
			IJExpression methodsCall = model;
			JBlock checkForNull = onBindMethodBody;

			String[] fieldSplit = field.split("\\.");
			int index = 0;
			for (String fieldPart : fieldSplit)
				if (!fieldPart.equals("")) {
					methodsCall = methodsCall.invoke(fieldToGetter(fieldPart));

					if (index < fieldSplit.length - 1) {
						checkForNull = checkForNull
								._if(methodsCall.ne(_null()))._then();
					} else if (!fields.get(field).type.getKind().isPrimitive()) {
						checkForNull = checkForNull
								._if(methodsCall.ne(_null()))._then();
					}
					index++;
				}

			IdInfoHolder info = fields.get(field);
			JFieldRef view = viewHolder.ref(info.idName + DeclexConstant.VIEW);
			handler.putAssignInBlock(info, checkForNull, view, methodsCall,
					element, viewsHolder, null,
					listItemId);
		}

		for (String methodName : methods.keySet()) {
			IJExpression methodsCall = model;
			JBlock checkForNull = onBindMethodBody;

			String[] methodSplit = methodName.split("\\.");
			for (int i = 0; i < methodSplit.length - 1; i++)
				if (!methodSplit[i].equals("")) {
					methodsCall = methodsCall
							.invoke(fieldToGetter(methodSplit[i]));
					checkForNull = checkForNull._if(methodsCall.ne(_null()))
							._then();
				}
			methodsCall = methodsCall
					.invoke(methodSplit[methodSplit.length - 1]);

			IdInfoHolder info = methods.get(methodName);
			JFieldRef view = viewHolder.ref(info.idName + DeclexConstant.VIEW);
			handler.putAssignInBlock(info, checkForNull, view, methodsCall,
					element, viewsHolder, null,
					listItemId);
		}

		// Process the events
		Map<Class<?>, Object> listenerHolders = viewsHolder.holder()
				.getPluginHolders();
		for (Object listenerHolderObject : listenerHolders.values()) {
			if (!ViewListenerHolder.class.isInstance(listenerHolderObject))
				continue;
			final ViewListenerHolder listenerHolder = (ViewListenerHolder) listenerHolderObject;

			for (String viewId : listenerHolder.getViewFieldNames()) {
				if (!viewsHolder.layoutContainsId(viewId))
					continue;

				final JBlock eventsBlock = new JBlock();
				viewsHolder.createAndAssignView(viewId, new IWriteInBloc() {

					@Override
					public void writeInBlock(String viewName,
							AbstractJClass viewClass, JFieldRef view,
							JBlock block) {
						listenerHolder.createListener("viewHolder." + viewName,
								eventsBlock);
					}
				});
				onBindMethodBody.add(eventsBlock);
			}
		}

		handler.callPopulatorMethod(fieldName, onBindMethodBody, viewHolder,
				fieldNames, element, viewsHolder);

		onCreateViewMethodBody._return(viewHolder);

		viewsHolder.setCreateViewListener(null);
		viewsHolder.setDefLayoutId(defLayoutId);
	}

	@Override
	public AbstractJClass getBaseAdapter(Element element) {
		return null;
	}
}
