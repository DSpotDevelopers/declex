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
package com.dspot.declex.share.holder;

import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.direct;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.ArrayList;
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
import javax.lang.model.type.TypeMirror;

import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.helper.IdAnnotationHelper;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.holder.FoundViewHolder;
import org.androidannotations.plugin.PluginClassHolder;
import org.androidannotations.rclass.IRClass.Res;

import com.dspot.declex.api.extension.Extension;
import com.dspot.declex.helper.ViewsHelper;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.LayoutsParser.LayoutObject;
import com.dspot.declex.util.MenuParser;
import com.dspot.declex.util.ParamUtils;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class ViewsHolder extends
		PluginClassHolder<EComponentWithViewSupportHolder> {

	// <Layout Id, <View Id, View Information>>
	private Map<String, Map<String, LayoutObject>> layoutObjects = new HashMap<>();
	private Map<String, JVar> onViewChangedHasViewsParamValues = new HashMap<>();
	private List<String> menuObjects;
	
	private String defLayoutId = null;
	
	private MenuParser menuParser;
	private IdAnnotationHelper annotationHelper;

	private Map<String, ViewInfo> views = new HashMap<>();
	
	private ICreateViewListener createViewListener;
	
	private ViewsHelper viewsHelper;

	public ViewsHolder(EComponentWithViewSupportHolder holder,
			IdAnnotationHelper annotationHelper) {
		super(holder);

		this.annotationHelper = annotationHelper;
		
		this.menuParser = MenuParser.getInstance();
		
		viewsHelper = new ViewsHelper(holder.getAnnotatedElement(), annotationHelper, environment());
		defLayoutId = viewsHelper.getLayoutId();
		if (defLayoutId != null) {
			layoutObjects.put(defLayoutId, viewsHelper.getLayoutObjects());
		}
	}


	public String getDefLayoutId() {
		return defLayoutId;
	}

	public void setDefLayoutId(String defLayoutId) {
		this.defLayoutId = defLayoutId;
	}

	public List<String> getMenuObjects() {
		if (menuObjects == null) {
			OptionsMenu optionMenu = holder().getAnnotatedElement()
					.getAnnotation(OptionsMenu.class);
			if (optionMenu != null) {
				int menu = optionMenu.value()[0];
				if (menu != -1) {
					String idQualifiedName = environment().getRClass()
							.get(Res.MENU).getIdQualifiedName(menu);
					Matcher matcher = Pattern.compile("\\.(\\w+)$").matcher(
							idQualifiedName);

					if (matcher.find()) {
						menuObjects = menuParser.getMenuObjects(
								matcher.group(1), annotationHelper);
					}
				}
			}

			if (menuObjects == null)
				menuObjects = new ArrayList<>(0);
		}

		return menuObjects;
	}

	public void inflateLayoutAndUse(int layoutId) {
		
		String idQualifiedName = environment().getRClass().get(Res.LAYOUT)
				.getIdQualifiedName(layoutId);
		Matcher matcher = Pattern.compile("\\.(\\w+)$")
				.matcher(idQualifiedName);
		
		// Generate the view for the layout that should be inferred
		if (matcher.find()) {
			final String layoutIdString = matcher.group(1);
			String viewName = layoutIdString + DeclexConstant.VIEW;
			
			if (!views.containsKey(viewName)) {
				if (getGeneratedClass().fields().get(viewName)==null &&
						!TypeUtils.fieldInElement(viewName, holder().getAnnotatedElement()))
					getGeneratedClass().field(JMod.PRIVATE, environment().getClasses().VIEW, viewName);
				
				views.put(viewName, new ViewInfo());
			}			
			
			JFieldRef view = ref(viewName);
			JInvocation inflater = holder().getContextRef().invoke(
					"getLayoutInflater");
			holder().getOnViewChangedBodyBeforeInjectionBlock().assign(
					view,
					inflater.invoke("inflate").arg(direct(idQualifiedName))
							.arg(_null())
			);

			defLayoutId = layoutIdString;
			
			//This is to use the view of the layout to findByViewId the Views
			JVar layoutViewVar = new JBlock().decl(getClasses().VIEW, layoutIdString + DeclexConstant.VIEW);
			holder().setOnViewChangedHasViewsParam(layoutViewVar);
			
			addLayout(layoutIdString);

			layoutObjects.get(layoutIdString).put(
					layoutIdString,
					new LayoutObject(CanonicalNameConstants.VIEW, null)
					
			);
		}
	}

	public void addLayout(String layoutId) {

		if (!layoutObjects.containsKey(layoutId)) {
			Map<String, LayoutObject> elementLayoutObjects = viewsHelper.getLayoutObjects(layoutId);
			layoutObjects.put(layoutId, elementLayoutObjects);
			
			onViewChangedHasViewsParamValues.put(layoutId, holder().getOnViewChangedHasViewsParam());
		}

	}

	public JInvocation checkFieldNameInInvocation(String fieldName, String fieldType, JInvocation invocation) {
		for (String layoutId : layoutObjects.keySet()) {
			if (this.layoutContainsId(fieldName, layoutId)) {
				
				final String savedDefLayoutId = defLayoutId;
				defLayoutId = layoutId;
				
				JFieldRef view = this.createAndAssignView(fieldName);
			
				defLayoutId = savedDefLayoutId;
				
				return invocation.arg(view);
			}	
		}
		
		return ParamUtils.injectParam(fieldName, fieldType, invocation);	
	}
	
	public boolean viewsDeclared(String id) {
		return views.containsKey(id);
	}
	
	public boolean layoutContainsId(String id) {
		return layoutContainsId(id, null);
	}

	public boolean layoutContainsId(String id, String layoutId) {
		if (layoutId == null)
			layoutId = defLayoutId;
		if (layoutId == null)
			return false;

		return layoutObjects.get(layoutId).containsKey(id);
	}

	public String getClassNameFromId(String id) {
		return getClassNameFromId(id, null);
	}

	public String getClassNameFromId(String id, String layoutId) {
		if (layoutId == null)
			layoutId = defLayoutId;
		if (layoutId == null)
			return null;

		return layoutObjects.get(layoutId).get(id).className;
	}

	public org.w3c.dom.Element getDomElementFromId(String id) {
		return getDomElementFromId(id, null);
	}

	public org.w3c.dom.Element getDomElementFromId(String id, String layoutId) {
		if (layoutId == null)
			layoutId = defLayoutId;
		if (layoutId == null)
			return null;

		return layoutObjects.get(layoutId).get(id).domElement;
	}
	
	private Map<String, LayoutObject> getLayoutObjects(String layoutId) {
		if (layoutId == null)
			layoutId = defLayoutId;
		if (layoutId == null)
			return null;

		return layoutObjects.get(layoutId);
	}

	public void setCreateViewListener(ICreateViewListener createViewListener) {
		this.createViewListener = createViewListener;
	}
	
	public JFieldRef createAndAssignView(String fieldName) {
		return this.createAndAssignView(fieldName, null);
	}

	public JFieldRef createAndAssignView(final String fieldName,
			IWriteInBloc writeInBlock) {
		final String viewName = fieldName + DeclexConstant.VIEW;
		final AbstractJClass viewClass = getJClass(getClassNameFromId(fieldName));
		
		if (createViewListener != null) {
			final JBlock declBlock = writeInBlock==null? null : new JBlock();
			final JFieldRef view = createViewListener.createView(fieldName, viewName, viewClass, declBlock);
			
			if (writeInBlock != null)
				writeInBlock.writeInBlock(viewName, viewClass, view, declBlock);
			
			return view; 
		}

		final JFieldRef view = ref(viewName);		
		final JFieldRef idRef = environment().getRClass().get(Res.ID)
				                       .getIdStaticRef(fieldName, environment());
		
		//If idRef is null, this means the view was generated by DecleX,
		//Ex. the view of an automatic inflated layout
		if (idRef != null) {
			JVar savedOnViewChangedHasViewsParam = null;
			JVar onViewChangedHasViewsParam = onViewChangedHasViewsParamValues.get(defLayoutId);
			
			JBlock declaredBlock = null;
			IJExpression viewRef = null;
			
			
			if (onViewChangedHasViewsParam != null) {
				savedOnViewChangedHasViewsParam = holder().getOnViewChangedHasViewsParam();
				holder().setOnViewChangedHasViewsParam(onViewChangedHasViewsParam);
			} else {
				String holderId = getLayoutObjects(defLayoutId).get(fieldName).holderId;
				if (holderId != null) {
										
					WriteInBlockWithResult<JBlock> writeInBlockWithResult = new WriteInBlockWithResult<JBlock>() {

						@Override
						public void writeInBlock(String viewName,
								AbstractJClass viewClass, JFieldRef view,
								JBlock block) {							
							result = block;
						}
					};	
					
					JFieldRef holderView = createAndAssignView(holderId, writeInBlockWithResult);
					
					declaredBlock = writeInBlockWithResult.result;
					
					JVar headerView = null;
					for (Object statement : declaredBlock.getContents()) {
						if (statement instanceof JVar) {					
							if (((JVar) statement).name().equals("header")) {
								headerView = (JVar) statement;
								break;
							}
						}
					}
					
					if (headerView == null) {
						headerView = declaredBlock.decl(
								environment().getClasses().VIEW, 
								"header", 
								holderView.invoke("getHeaderView").arg(0)
							);
					}
					
					viewRef = invoke(headerView, "findViewById").arg(idRef);
				}
			}
			
			if (declaredBlock == null) {
				FoundViewHolder foundViewHolder = holder().getFoundViewHolder(idRef, viewClass);
				declaredBlock = foundViewHolder.getIfNotNullBlock();	
				viewRef = foundViewHolder.getRef();
			}

			if (!views.containsKey(viewName)) {
				if (getGeneratedClass().fields().get(viewName)==null &&
						!TypeUtils.fieldInElement(viewName, holder().getAnnotatedElement()))
					getGeneratedClass().field(JMod.PRIVATE, viewClass, viewName);
				
				declaredBlock.assign(view, cast(viewClass, viewRef));
				views.put(viewName, new ViewInfo());
			}
			
			if (writeInBlock != null)
				writeInBlock.writeInBlock(viewName, viewClass, view, declaredBlock);
			
			if (savedOnViewChangedHasViewsParam != null) {
				holder().setOnViewChangedHasViewsParam(savedOnViewChangedHasViewsParam);
			}
		}
		
		return view;
	}
	
	public void findFieldsAndMethods(String className, String fieldName,
			Element element, Map<String, IdInfoHolder> fields,
			Map<String, IdInfoHolder> methods, boolean getter) {
		
		findFieldsAndMethods(className, fieldName, element, fields, methods,getter, false, null);
	}

	public void findFieldsAndMethods(String className, String fieldName,
			Element element, Map<String, IdInfoHolder> fields,
			Map<String, IdInfoHolder> methods, boolean getter, boolean isList,
			String layoutId) {
		
		TypeElement typeElement = environment().getProcessingEnvironment()
				.getElementUtils().getTypeElement(className);
		findFieldsAndMethods(fieldName, typeElement, fields, methods,
				className, getLayoutObjects(layoutId), getter, isList);

		// Apply to Extensions
		List<? extends TypeMirror> superTypes = environment()
				.getProcessingEnvironment().getTypeUtils()
				.directSupertypes(typeElement.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = environment().getProcessingEnvironment()
					.getElementUtils().getTypeElement(type.toString());

			if (superElement.getAnnotation(Extension.class) != null) {
				findFieldsAndMethods(type.toString(), fieldName, element,
						fields, methods, getter, isList, layoutId);
			}

			break;
		}
	}

	private void findFieldsAndMethods(String fieldName,
			TypeElement typeElement, Map<String, IdInfoHolder> fields,
			Map<String, IdInfoHolder> methods, String className,
			Map<String, LayoutObject> layoutObjects, boolean getter, boolean isList) {
		
		String classSimpleName = getJClass(className).name();
		for (String id : layoutObjects.keySet()) {
			String startsWith = null;
			String originalId = id;

			if (id.startsWith(fieldName)) {
				startsWith = fieldName;
			}

			if (id.startsWith(fieldName + "_")) {
				startsWith = fieldName + "_";
			}

			if (id.startsWith(classSimpleName + "_")) {
				startsWith = classSimpleName + "_";
			}

			if (id.startsWith(classSimpleName.toLowerCase() + "_")) {
				startsWith = classSimpleName.toLowerCase() + "_";
			}

			if (startsWith != null) {
				id = id.substring(startsWith.length());
			} else {
				if (!isList)
					continue;
			}

			deepFieldsAndMethodsSearch(id, null, typeElement, fields, methods,
					originalId, layoutObjects.get(originalId).className, getter);
		}
	}

	private void deepFieldsAndMethodsSearch(String id, String prevField,
			TypeElement testElement, Map<String, IdInfoHolder> fields,
			Map<String, IdInfoHolder> methods, String originalId,
			String idClass, boolean getter) {
		
		if (id == null || id.isEmpty())
			return;

		final String normalizedId = id.substring(0, 1).toLowerCase() + id.substring(1);

		List<? extends Element> elems = testElement.getEnclosedElements();
		for (Element elem : elems) {
			final String elemName = elem.getSimpleName().toString();
			final String completeElemName = prevField == null ? elemName : prevField + "." + elemName;

			if (elem.getKind() == ElementKind.FIELD) {

				// If the class element is not a primitive, then call the method
				// in that element combinations (recursive DFS)
				if (!elem.asType().getKind().isPrimitive()) {
					String elemType = TypeUtils.typeFromTypeString(elem.asType().toString(), environment());
					if (elemType.endsWith(ModelConstants.generationSuffix()))
						elemType = elemType.substring(0, elemType.length() - 1);

					TypeElement fieldTypeElement = environment()
							.getProcessingEnvironment().getElementUtils()
							.getTypeElement(elemType);

					if (fieldTypeElement != null
						&& !fieldTypeElement.toString().equals(String.class.getCanonicalName())) {

						if (id.startsWith(elemName) || normalizedId.startsWith(elemName)) {
							int extraToRemove = id.startsWith(elemName + "_") ? 1 : 0;
							
							deepFieldsAndMethodsSearch(
									id.substring(elemName.length()
											+ extraToRemove), completeElemName,
									fieldTypeElement, fields, methods,
									originalId, idClass, getter);
						}
					}
				}

				if (id.equals(elemName) || normalizedId.equals(elemName)) {
					fields.put(completeElemName, new IdInfoHolder(originalId,
							elem, elem.asType(), idClass,
							new ArrayList<String>(0)));
				}
			}

			if (elem.getKind() == ElementKind.METHOD) {
				if (id.equals(elemName) || normalizedId.equals(elemName)) {
					// Only setter methods
					ExecutableElement exeElem = (ExecutableElement) elem;

					if (exeElem.getParameters().size() < (getter ? 0 : 1))
						continue;

					List<String> extraParams = new LinkedList<String>();
					for (int i = (getter ? 0 : 1); i < exeElem.getParameters()
							.size(); i++) {
						extraParams.add(exeElem.getParameters().get(i)
								.getSimpleName().toString());
					}

					TypeMirror paramType = null;
					if (getter) {
						paramType = exeElem.getReturnType();
					} else {
						paramType = exeElem.getParameters().get(0).asType();
					}

					methods.put(completeElemName, new IdInfoHolder(originalId,
							elem, paramType, idClass, extraParams));
				}
			}
		}
	}

	public static class IdInfoHolder {
		public String idName;
		public String viewClass;

		public TypeMirror type;

		public List<String> extraParams;
		public Element element;

		public IdInfoHolder(String idName, Element element, TypeMirror type,
				String className, List<String> extraParams) {
			super();
			this.element = element;
			this.idName = idName;
			this.type = type;
			this.viewClass = className;
			this.extraParams = extraParams;
		}

		@Override
		public String toString() {
			return idName + (type != null ? ": " + type.toString() : "") + " "
					+ extraParams;
		}
	}

	public static interface IWriteInBloc {
		public void writeInBlock(String viewName, AbstractJClass viewClass,
				JFieldRef view, JBlock block);
	}

	public static abstract class WriteInBlockWithResult<T> implements
			IWriteInBloc {
		protected T result;

		public T getResult() {
			return result;
		}
	}
	
	public interface ICreateViewListener {
		public JFieldRef createView(String viewId, String viewName, AbstractJClass viewClass, JBlock declBlock);
	}

	private class ViewInfo {
		
	}

}
