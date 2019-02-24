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
package com.dspot.declex.holder;

import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;
import static com.dspot.declex.api.util.FormatsUtils.fieldToSetter;
import static com.dspot.declex.util.TypeUtils.isSubtype;
import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.cond;
import static com.helger.jcodemodel.JExpr.direct;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.ref;
import static org.androidannotations.helper.ModelConstants.generationSuffix;

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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import com.sun.codemodel.internal.JClass;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.helper.ADIHelper;
import org.androidannotations.helper.APTCodeModelHelper;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.holder.FoundViewHolder;
import org.androidannotations.plugin.PluginClassHolder;
import org.androidannotations.rclass.IRClass.Res;

import com.dspot.declex.annotation.UseModel;
import com.dspot.declex.api.injection.NotFoundProperty;
import com.dspot.declex.api.injection.Property;
import com.dspot.declex.helper.ViewsHelper;
import com.dspot.declex.helper.ViewsPropertiesReaderHelper;
import com.dspot.declex.parser.LayoutsParser.LayoutObject;
import com.dspot.declex.parser.MenuParser;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.ParamUtils;
import com.dspot.declex.util.TypeUtils;
import org.androidannotations.internal.virtual.VirtualElement;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JAnonymousClass;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
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

	private Map<String, ViewInfo> views = new HashMap<>();
	
	private ICreateViewListener createViewListener;
	
	private APTCodeModelHelper codeModelHelper;
	private ViewsHelper viewsHelper;
	private ADIHelper adiHelper;
	private ViewsPropertiesReaderHelper propertiesHelper;

	private boolean inList;

	public ViewsHolder(EComponentWithViewSupportHolder holder) {
		super(holder);
		
		this.adiHelper = new ADIHelper(environment());
		this.propertiesHelper = ViewsPropertiesReaderHelper.getInstance(environment());		
		this.menuParser = MenuParser.getInstance();
		this.codeModelHelper = new APTCodeModelHelper(environment());
		
		viewsHelper = new ViewsHelper(holder.getAnnotatedElement(), environment());
		defLayoutId = viewsHelper.getLayoutId();
		if (defLayoutId != null) {
			layoutObjects.put(defLayoutId, viewsHelper.getLayoutObjects());
			onViewChangedHasViewsParamValues.put(defLayoutId, holder().getOnViewChangedHasViewsParam());
		}
	}

	public void setInList() {
		this.inList = true;
	}

	public void resetInList() {
		this.inList = false;
	}

	public ViewsHelper getViewsHelper() {
		return viewsHelper;
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
						menuObjects = menuParser.getMenuObjects(matcher.group(1));
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
					new LayoutObject(layoutIdString, CanonicalNameConstants.VIEW, null)
					
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

	public ViewProperty getPropertySetterForFieldName(final String fieldName, final String fieldType) {

		for (String layoutId : layoutObjects.keySet()) {

			if (this.layoutContainsId(fieldName, layoutId)) {

				final String savedDefLayoutId = defLayoutId;
				defLayoutId = layoutId;

				JFieldRef view = this.createAndAssignView(fieldName);

				defLayoutId = savedDefLayoutId;

				AbstractJClass viewClass = getJClass(getClassNameFromId(fieldName));
				return new ViewProperty(fieldName, viewClass, view, null);
			}

			for (Entry<String, LayoutObject> entry : layoutObjects.get(layoutId).entrySet()) {

				final String viewId = entry.getKey();
				final LayoutObject layoutObject = entry.getValue();

				if (fieldName.startsWith(viewId)) {

					final Map<String, TypeMirror> getters = new HashMap<>();
					final Map<String, Set<TypeMirror>> setters = new HashMap<>();
					propertiesHelper.readGettersAndSetters(layoutObject.className, getters, setters);

					final String property = fieldName.substring(viewId.length());

					if (setters.containsKey(property)) {

						boolean hasSetter = false;

						for (TypeMirror setter : setters.get(property)) {
							hasSetter = isSubtype(fieldType, setter.toString(), getProcessingEnvironment())
									|| isSubtype(wrapperToPrimitive(fieldType), setter.toString(), getProcessingEnvironment());
							if (hasSetter) break;
						}

						if (hasSetter) {

							final String savedDefLayoutId = defLayoutId;
							defLayoutId = layoutId;

							JFieldRef view = this.createAndAssignView(viewId);

							defLayoutId = savedDefLayoutId;

							AbstractJClass viewClass = getJClass(getClassNameFromId(viewId));
							return new ViewProperty(fieldName, viewClass, view, null);

						}

					}
				}

			}

		}

		return null;

	}

	public JInvocation checkFieldNameInInvocation(final String fieldName, final String fieldType, final JInvocation invocation) {

		//Model is obtained again from the position, to permit DeclexAdapterList to do some processing
		//on the "models.get(int)"
		if (inList && fieldName.equals("model")) {
			return invocation.arg(ref("models").invoke("get").arg(ref("position")));
		}

		for (String layoutId : layoutObjects.keySet()) {
			
			if (this.layoutContainsId(fieldName, layoutId)) {
				
				final String savedDefLayoutId = defLayoutId;
				defLayoutId = layoutId;
				
				JFieldRef view = this.createAndAssignView(fieldName);
			
				defLayoutId = savedDefLayoutId;
				
				return invocation.arg(view);
			}	
			
			for (Entry<String, LayoutObject> entry : layoutObjects.get(layoutId).entrySet()) {
				
				final String viewId = entry.getKey();
				final LayoutObject layoutObject = entry.getValue();
				
				if (fieldName.startsWith(viewId)) {

					final Map<String, TypeMirror> getters = new HashMap<>();
					final Map<String, Set<TypeMirror>> setters = new HashMap<>();
					propertiesHelper.readGettersAndSetters(layoutObject.className, getters, setters);

					final String property = fieldName.substring(viewId.length());

					boolean isProperty = false;
					boolean callPropertyGetter = false;
					boolean callPropertySetter = false;
					String fieldTypeToMatch = fieldType;
					if (isSubtype(fieldType, Property.class.getCanonicalName(), getProcessingEnvironment())) {
						
//						final String pattern = "\\Q" + Property.class.getCanonicalName() + "\\E" 
//								               + "<([a-zA-Z_][a-zA-Z_$0-9.]+)>";
//						
//						//Create the classTree
//						List<TypeMirror> classTree = new LinkedList<>();
//						String subClass = fieldType;
//						while (!subClass.matches(pattern)) {
//							
//							if (subClass.contains("<")) subClass = subClass.substring(0, subClass.indexOf('<'));
//							
//							TypeElement typeElement = getProcessingEnvironment().getElementUtils().getTypeElement(subClass);
//							classTree.add(0, typeElement.asType());
//							
//							List<? extends TypeMirror> superTypes = getProcessingEnvironment().getTypeUtils().directSupertypes(typeElement.asType());
//							for (TypeMirror type : superTypes) {
//								if (TypeUtils.isSubtype(type, Property.class.getCanonicalName(), getProcessingEnvironment())) {
//									subClass = type.toString(); 
//								}
//							}
//						}
						
						final String pattern = "[a-zA-Z_][a-zA-Z_$0-9.]+<([a-zA-Z_][a-zA-Z_$0-9.]+)>";
						
						Matcher matcher = Pattern.compile(pattern).matcher(fieldTypeToMatch);
						if (matcher.find()) {
							fieldTypeToMatch = matcher.group(1);
							isProperty = true;
						}
						
					}

					if (getters.containsKey(property) || (isProperty && setters.containsKey(property))) {
												
						boolean hasGetter = getters.containsKey(property) && 
											(isSubtype(getters.get(property).toString(), fieldTypeToMatch, getProcessingEnvironment())
											|| isSubtype(getters.get(property).toString(), wrapperToPrimitive(fieldTypeToMatch), getProcessingEnvironment()));
											
						boolean hasGetterAsString = getters.containsKey(property) && fieldTypeToMatch.equals(String.class.getCanonicalName());
						
						boolean hasSetter = false;
						if (isProperty && setters.containsKey(property)) {
							for (TypeMirror setter : setters.get(property)) {
								hasSetter = isSubtype(fieldTypeToMatch, setter.toString(), getProcessingEnvironment())
										|| isSubtype(wrapperToPrimitive(fieldTypeToMatch), setter.toString(), getProcessingEnvironment());
								if (hasSetter) break;
							}
						}
						
						if (hasGetter || hasGetterAsString || (hasSetter && isProperty)) {

							final String getterInitialExpression;
							if (wrapperToPrimitive(fieldTypeToMatch).equals("boolean")) {
								getterInitialExpression = "is";
							} else {
								getterInitialExpression = "get";
							}							
							
							final String savedDefLayoutId = defLayoutId;
							defLayoutId = layoutId;
							
							JFieldRef view = this.createAndAssignView(viewId);
							IJExpression getProperty = view.invoke(getterInitialExpression + property);
						
							defLayoutId = savedDefLayoutId;
														
							if (isProperty) {
								AbstractJClass fieldClass = getJClass(fieldTypeToMatch);
								
								JAnonymousClass anonymousProperty = getCodeModel().anonymousClass(
										getJClass(Property.class).narrow(fieldClass));
								
								JMethod getMethod = anonymousProperty.method(JMod.PUBLIC, fieldClass, "get");
								getMethod.annotate(Override.class);								
								if (hasGetter) {
									getMethod.body()._if(view.eqNull())._then()._return(getDefault(fieldTypeToMatch));
									getMethod.body()._return(getProperty);
								} else if (hasGetterAsString) {
									getMethod.body()._if(view.eqNull())._then()._return(getDefault(fieldTypeToMatch));
									if (getters.get(property).getKind().isPrimitive()) {
										getMethod.body()._return(
												getJClass(String.class).staticInvoke("valueOf").arg(getProperty));
									} else {
										getMethod.body()._return(cond(
												getProperty.eq(_null()),
												_null(),
												getProperty.invoke("toString")));
									}
								} else {
									getMethod.body()._return(getDefault(fieldTypeToMatch));
								}
								
								JMethod setMethod = anonymousProperty.method(JMod.PUBLIC, getCodeModel().VOID, "set");								
								setMethod.annotate(Override.class);								
								JVar value = setMethod.param(fieldClass, "value");
								if (hasSetter) {
									setMethod.body()._if(view.eqNull())._then()._return();
									setMethod.body().add(invoke(view, "set" + property).arg(value));
								}
								
								return invocation.arg(_new(anonymousProperty));
								
							} else if (hasGetterAsString) {
								if (getters.get(property).getKind().isPrimitive()) {
									getProperty = getJClass(String.class).staticInvoke("valueOf").arg(getProperty);
								} else {
									getProperty = cond(getProperty.eq(_null()), _null(),getProperty.invoke("toString"));
								}
							}
							
							return invocation.arg(getProperty);
							
						}
					}
				}
				
			}
						
		}

		//Not found properties
		if (isSubtype(fieldType, Property.class.getCanonicalName(), getProcessingEnvironment())) {
			
			Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(fieldType);
			if (matcher.find()) {
				AbstractJClass fieldClass = getJClass(matcher.group(1));
				return invocation.arg(_new(getJClass(NotFoundProperty.class).narrow(fieldClass)));
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

	public LayoutObject getLayoutObject(String id) {
	    return getLayoutObjects(defLayoutId).get(id);
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

	public JFieldRef createAndAssignView(final String fieldName, IWriteInBloc writeInBlock) {
		
		final String viewName = fieldName + DeclexConstant.VIEW;
		final AbstractJClass viewClass = getJClass(getClassNameFromId(fieldName));
		
		if (createViewListener != null) {			
			
			final JBlock declBlock = writeInBlock==null? null : new JBlock();
			final JFieldRef view = createViewListener.createView(fieldName, viewName, viewClass, declBlock);
			
			if (view != null) {
				if (writeInBlock != null)
					writeInBlock.writeInBlock(viewName, viewClass, view, declBlock);
				
				return view;
			}
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
	
	public void findFieldsAndMethods(
			String className, String fieldName,
			Map<String, IdInfoHolder> fields, Map<String, IdInfoHolder> methods, boolean getter) {
		
		findFieldsAndMethods(className, fieldName, fields, methods, getter, false, null);
	}

	public void findFieldsAndMethods(
			String className, String fieldName,
			Map<String, IdInfoHolder> fields, Map<String, IdInfoHolder> methods,
			boolean getter, boolean isList, String layoutId) {

		TypeElement typeElement = environment().getProcessingEnvironment()
				                               .getElementUtils().getTypeElement(className);
		if (typeElement == null) return;

		Map<String, LayoutObject> layoutObjects = getLayoutObjects(layoutId);
		if (layoutObjects == null) return;

		findFieldsAndMethodsInternal(
			className, fieldName, typeElement, fields, methods,
			layoutObjects, getter, isList
		);

		List<? extends TypeMirror> superTypes = environment().getProcessingEnvironment().getTypeUtils().directSupertypes(typeElement.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = environment().getProcessingEnvironment().getElementUtils().getTypeElement(type.toString());
			if (superElement == null) continue;
			if (superElement.getKind().equals(ElementKind.INTERFACE)) continue;
			if (superElement.asType().toString().equals(Object.class.getCanonicalName())) break;

			findFieldsAndMethods(
					type.toString(), fieldName,
					fields, methods, getter, isList, layoutId
			);

			break;
		}
	}

	private void findFieldsAndMethodsInternal(
			String className, String fieldName, TypeElement typeElement,
			Map<String, IdInfoHolder> fields, Map<String, IdInfoHolder> methods,
			Map<String, LayoutObject> layoutObjects,
			boolean getter, boolean isList) {
		
		final String classSimpleName = getJClass(className).name();

		for (String id : layoutObjects.keySet()) {
			String startsWith = null;
			String originalId = id;
			
			if (fieldName == null) {
				deepFieldsAndMethodsSearch(
					id, null, typeElement, 
					fields, methods,
					originalId, layoutObjects.get(originalId).className, getter
				);
				continue;
			}

			if (id.startsWith(fieldName)) {
				startsWith = fieldName;
				
				if (id.startsWith(fieldName + "_")) {
					startsWith = fieldName + "_";
				}
			} if (id.startsWith(classSimpleName)) {
				startsWith = classSimpleName;
				
				if (id.startsWith(classSimpleName + "_")) {
					startsWith = classSimpleName + "_";
				}				
						
			} else if (id.startsWith(classSimpleName.toLowerCase())) {
				startsWith = classSimpleName.toLowerCase();
				
				if (id.startsWith(classSimpleName.toLowerCase() + "_")) {
					startsWith = classSimpleName.toLowerCase() + "_";
				}
			}

			if (startsWith != null) {
				id = id.substring(startsWith.length());
			} else {
				if (!isList) continue;
				
				//List element will be checked for coinciding IDs
			}

			deepFieldsAndMethodsSearch(
				id, null, typeElement, 
				fields, methods,
				originalId, layoutObjects.get(originalId).className, getter
			);
		}
	}	
	
	private void deepFieldsAndMethodsSearch(
			final String id, final String prevField,
			final TypeElement testElement, 
			final Map<String, IdInfoHolder> fields, Map<String, IdInfoHolder> methods, 
			final String layoutElementId, final String layoutElementIdClass, final boolean getter) {
		
		if (id == null || id.isEmpty()) return;
		
		final String normalizedId = id.substring(0, 1).toLowerCase() + id.substring(1);

		List<? extends Element> elems = testElement.getEnclosedElements();
		List<Element> allElems = new LinkedList<>(elems);
		allElems.addAll(VirtualElement.getVirtualEnclosedElements(testElement));
		
		for (Element elem : allElems) {

			final String elemName = elem.getSimpleName().toString();
			final String completeElemName = prevField == null ? elemName : prevField + "." + elemName;
			final String expectedMethodName = (getter? fieldToGetter(id) : fieldToSetter(id));

			if (elem.getKind() == ElementKind.FIELD && !elem.getModifiers().contains(Modifier.TRANSIENT)) {

				// If the class element is not a primitive, then call the method
				// in that element combinations (recursive DFS)
				if (!elem.asType().getKind().isPrimitive()) {
					
					String elemType = codeModelHelper.elementTypeToJClass(elem, true).fullName();
					if (elemType.endsWith(generationSuffix()))
						elemType = elemType.substring(0, elemType.length() - 1);

					TypeElement fieldTypeElement = environment()
							.getProcessingEnvironment().getElementUtils()
							.getTypeElement(elemType);

					if (fieldTypeElement != null
						&& !fieldTypeElement.toString().equals(String.class.getCanonicalName())) {

						if (id.startsWith(elemName) || normalizedId.startsWith(elemName)) {
							int extraToRemove = id.startsWith(elemName + "_") ? 1 : 0;
							
							deepFieldsAndMethodsSearch(
									id.substring(elemName.length() + extraToRemove), completeElemName,
									fieldTypeElement, fields, methods,
									layoutElementId, layoutElementIdClass, getter);
						}
					}

				}

				if (id.equals(elemName) || normalizedId.equals(elemName)) {

					fields.put(completeElemName, new IdInfoHolder(layoutElementId, elem, layoutElementIdClass));

				} else if (elemName.startsWith(id)) {

					final Map<String, TypeMirror> getters = new HashMap<>();
					final Map<String, Set<TypeMirror>> setters = new HashMap<>();
					propertiesHelper.readGettersAndSetters(layoutElementIdClass, getters, setters);
					
					final String property = elemName.substring(id.length());
					
					if (getter && setters.containsKey(property)) {

						for (TypeMirror propertyType : setters.get(property)) {
							if (isSubtype(elem.asType(), propertyType, getProcessingEnvironment())
								|| isSubtype(wrapperToPrimitive(elem.asType().toString()), getters.get(property).toString(), getProcessingEnvironment())) {
								fields.put(
									completeElemName, 
									new IdInfoHolder(layoutElementId, elem, layoutElementIdClass, new ArrayList<VariableElement>(0), property)
								);
							}							
						}

					} else if (!getter && getters.containsKey(property)) {

						if (isSubtype(elem.asType(), getters.get(property), getProcessingEnvironment())
							|| isSubtype(wrapperToPrimitive(elem.asType().toString()), getters.get(property).toString(), getProcessingEnvironment())) {
							fields.put(
								completeElemName, 
								new IdInfoHolder(layoutElementId, elem, layoutElementIdClass, new ArrayList<VariableElement>(0), property)
							);								
						}

					}

				}
			}

			if (elem.getKind() == ElementKind.METHOD && propertiesHelper.hasValidModifiers(elem.getModifiers())) {
				
				ExecutableElement exeElem = (ExecutableElement) elem;
				
				if (id.equals(elemName) || normalizedId.equals(elemName) || elemName.startsWith(id)
					|| expectedMethodName.equals(elemName) || elemName.startsWith(expectedMethodName)) {
					
					// Only setter methods
					if (exeElem.getParameters().size() < (getter ? 0 : 1)) continue;
					
					List<VariableElement> extraParams = new LinkedList<>();
					for (int i = (getter ? 0 : 1); i < exeElem.getParameters().size(); i++) {
						extraParams.add(exeElem.getParameters().get(i));
					}

					TypeMirror paramType;
					if (getter) {
						paramType = exeElem.getReturnType();
					} else {
						paramType = exeElem.getParameters().get(0).asType();
					}

					if (id.equals(elemName) || normalizedId.equals(elemName) || expectedMethodName.equals(elemName)) {
						
						IdInfoHolder info = new IdInfoHolder(layoutElementId, elem, paramType, layoutElementIdClass, extraParams, null);
						
						if (methods.containsKey(completeElemName) && getter) {
							IdInfoHolder existingInfo = methods.get(completeElemName);
							
							//The getter with 0 arguments has higher priority
							if (existingInfo.extraParams.size() == 0) {
								info = existingInfo;
							}
						}
						
						methods.put(completeElemName, info);
						
					} else { //elemName.startsWith(id)

						final Map<String, TypeMirror> getters = new HashMap<>();
						final Map<String, Set<TypeMirror>> setters = new HashMap<>();
						propertiesHelper.readGettersAndSetters(layoutElementIdClass, getters, setters);
						
						final String property = elemName.startsWith(expectedMethodName)?
														elemName.substring(expectedMethodName.length())
														: elemName.substring(id.length());

						if (getter && setters.containsKey(property)) {

							for (TypeMirror propertyType : setters.get(property)) {
								if (isSubtype(paramType, propertyType, getProcessingEnvironment())
									|| isSubtype(wrapperToPrimitive(paramType.toString()), propertyType.toString(), getProcessingEnvironment())) {
									methods.put(
										completeElemName, 
										new IdInfoHolder(layoutElementId, elem, paramType, layoutElementIdClass, extraParams, property)
									);
								}
							}

						} else if (!getter && getters.containsKey(property)) {

							if (isSubtype(paramType, getters.get(property), getProcessingEnvironment())
								|| isSubtype(wrapperToPrimitive(paramType.toString()), getters.get(property).toString(), getProcessingEnvironment())) {
								methods.put(
									completeElemName, 
									new IdInfoHolder(layoutElementId, elem, paramType, layoutElementIdClass, extraParams, property)
								);								
							}

						}
					}

				} else {

					String elemType = codeModelHelper.elementTypeToJClass(exeElem, true).fullName();
					if (elemType.endsWith(generationSuffix())) {
						elemType = elemType.substring(0, elemType.length() - 1);
					}

					TypeElement executableTypeElement = environment().getProcessingEnvironment().getElementUtils().getTypeElement(elemType);

					if (executableTypeElement != null
						&& !executableTypeElement.toString().equals(String.class.getCanonicalName())) {

						if (id.startsWith(elemName) || normalizedId.startsWith(elemName)) {
							int extraToRemove = id.startsWith(elemName + "_") ? 1 : 0;
							
							deepFieldsAndMethodsSearch(
									id.substring(elemName.length() + extraToRemove), completeElemName,
									executableTypeElement, fields, methods,
									layoutElementId, layoutElementIdClass, getter);
						}
					}

				}

			}
		}
	}
	
	private String wrapperToPrimitive(String wrapper) {
		if (wrapper.equals(Boolean.class.getCanonicalName())) return "boolean";
		if (wrapper.equals(Integer.class.getCanonicalName())) return "int";
		if (wrapper.equals(Short.class.getCanonicalName())) return "short";
		if (wrapper.equals(Long.class.getCanonicalName())) return "long";
		if (wrapper.equals(Character.class.getCanonicalName())) return "char";
		if (wrapper.equals(Byte.class.getCanonicalName())) return "byte";
		if (wrapper.equals(Float.class.getCanonicalName())) return "float";
		if (wrapper.equals(Double.class.getCanonicalName())) return "double";
		return wrapper;
	}
	
	private IJExpression getDefault(String type) {
		switch (type) {
		case "int":
			return JExpr.lit(0);
		case "float":
			return JExpr.lit(0f);
		case "double":
			return JExpr.lit(0d);
		case "long":
			return JExpr.lit(0L);
		case "short":
			return JExpr.lit((short) 0);
		case "char":
			return JExpr.lit((char) 0);
		case "byte":
			return JExpr.lit((byte) 0);
		case "boolean":
			return JExpr.lit(false);

		default:
			return JExpr._null();
		}
	}

	public static class ViewProperty {

		public String idName;

		public AbstractJClass viewClass;

		public JFieldRef view;

		public String property;

		public ViewProperty(String idName, AbstractJClass viewClass, JFieldRef view, String property) {
			this.idName = idName;
			this.viewClass = viewClass;
			this.view = view;
			this.property = property;
		}

		@Override
		public String toString() {
			return idName + (property != null? "<" + property + ">" : "") + ": " + viewClass.name();
		}
	}

	public static class IdInfoHolder {

		public String idName;
		public String viewClass;

		public TypeMirror type;
		
		public List<VariableElement> extraParams;
		public Element element;
		
		public String getterOrSetter;

		public IdInfoHolder(String idName, Element element, String className) {
			this(idName, element, className, new ArrayList<VariableElement>(0));
		}
		
		public IdInfoHolder(String idName, Element element,
				String className, List<VariableElement> extraParams) {
			this(idName, element, className, extraParams, null);
		}

        public IdInfoHolder(String idName, Element element,
                            String className, List<VariableElement> extraParams, String getterOrSetter) {
            this(idName, element, element.asType(), className, extraParams, getterOrSetter);
        }

		public IdInfoHolder(String idName, Element element, TypeMirror typeMirror,
				String className, List<VariableElement> extraParams, String getterOrSetter) {
			super();
			this.element = element;
			this.type = typeMirror;
			this.idName = idName;
			this.viewClass = className;
			this.extraParams = extraParams;
			this.getterOrSetter = getterOrSetter;
		}

		@Override
		public String toString() {
			return idName + (element != null ? ": " + element : "")
					+ (getterOrSetter != null ? ": " + getterOrSetter : "")
					+ " " + extraParams;
		}
	}

	public interface IWriteInBloc {
		void writeInBlock(String viewName, AbstractJClass viewClass,
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
		JFieldRef createView(String viewId, String viewName, AbstractJClass viewClass, JBlock declBlock);
	}

	private class ViewInfo {
		
	}

}
