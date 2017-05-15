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
package com.dspot.declex.localdb;

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.dotclass;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.ref;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EComponentHolder;
import org.atteo.evo.inflector.English;

import com.dspot.declex.api.extension.Extension;
import com.dspot.declex.api.localdb.LocalDBModel;
import com.dspot.declex.api.localdb.UseLocalDB;
import com.dspot.declex.api.model.Model;
import com.dspot.declex.api.model.UseModel;
import com.dspot.declex.api.util.FormatsUtils;
import com.dspot.declex.handler.BaseTemplateHandler;
import com.dspot.declex.model.UseModelHolder;
import com.dspot.declex.util.SharedRecords;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JMethod;

public class LocalDBModelHandler extends BaseTemplateHandler<EComponentHolder> {
	
	private Map<String, String> columnFields = new LinkedHashMap<String, String>();
	private List<String> isList = new ArrayList<String>();
	
	public LocalDBModelHandler(AndroidAnnotationsEnvironment environment) {
		super(LocalDBModel.class, environment, 
				"com/dspot/declex/localdb/", "LocalDBModel.ftl.java");
	}
	
	@Override
	public void getDependencies(Element element, Map<Element, Class<? extends Annotation>> dependencies) {
		if (element.getKind().equals(ElementKind.CLASS)) {
			dependencies.put(element, UseModel.class);
		}
	}

	@Override
	protected void setTemplateDataModel(Map<String, Object> rootDataModel,
			Element element, EComponentHolder holder) {
		super.setTemplateDataModel(rootDataModel, element, holder);
		
		rootDataModel.put("columnFields", columnFields);
		rootDataModel.put("isList", isList);
		
		UseModelHolder useModelHolder = holder.getPluginHolder(new UseModelHolder(holder));
		rootDataModel.put("fullInitVar", useModelHolder.getFullInitVar().name());
	}
	
	@Override
	public void validate(Element element, ElementValidation valid) {
		if (element.getKind().isField()) {
			Model annotated = element.getAnnotation(Model.class);
			if (annotated == null) {
				valid.addError("You can only apply this annotation in a field annotated with @Model");
			}
			
			return;
		}
		
		String applicationClassName = getEnvironment().getAndroidManifest().getApplicationClassName();
		if (applicationClassName != null) {		
			
			if (applicationClassName.endsWith(ModelConstants.generationSuffix())) {
				applicationClassName = applicationClassName.substring(0, applicationClassName.length()-1);
			}
			
			if (!TypeUtils.isClassAnnotatedWith(applicationClassName, UseLocalDB.class, getEnvironment())) {
				valid.addError("The current application \"" + applicationClassName + "\" should be annotated with @UseLocalDB");				
			}
		} else {
			valid.addError("To use LocalDBModels you should declare an application object");
		}
		
		validatorHelper.extendsType(element, "com.activeandroid.Model", valid);
		
		if (valid.isValid()) {
			TypeElement typeElement = (TypeElement) element;
			final String qualifiedName = typeElement.getQualifiedName().toString();
			SharedRecords.addDBModelGeneratedClass(qualifiedName, getEnvironment());
		}
	}

	
	public void getLocalDBModelFields(TypeElement element) {
		//Get @Column annotated fields
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems)
			if (elem.getKind() == ElementKind.FIELD) {
				List<? extends AnnotationMirror> annotations = elem.getAnnotationMirrors();
				for (AnnotationMirror annotation : annotations) 
					if (annotation.getAnnotationType().toString().equals("com.activeandroid.annotation.Column") || 
						annotation.getAnnotationType().toString().equals(Model.class.getCanonicalName())) {
						String elemName = elem.getSimpleName().toString();
						String elemType = elem.asType().toString();

						if (!elem.getModifiers().isEmpty() && annotation.getAnnotationType().toString().equals("com.activeandroid.annotation.Column"))
							for (Modifier modifier : elem.getModifiers()) {
								if (!modifier.equals(Modifier.PUBLIC) && !modifier.equals(Modifier.PROTECTED)) {
									LOGGER.error(
											"The @Column field " + elemName + " shouldn't have that modifier." +
											"\nRemove " + elem.getModifiers().iterator().next().name() + " modifier",
											element,
											element.getAnnotation(LocalDBModel.class)
										);									
								}								
							}

						if (elem.getModifiers().contains(Modifier.STATIC)) continue;
						
						//Detect when the method is a List, in order to generate all the Adapters structures
						boolean isList = TypeUtils.isSubtype(elem, CanonicalNameConstants.LIST, getProcessingEnvironment());		
						if (isList) {
							Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(elemType);
							if (matcher.find()) {
								elemType = matcher.group(1);
							}
						} 

						if (elemType.endsWith(ModelConstants.generationSuffix())) {
							String elemGeneratedType = TypeUtils.typeFromTypeString(elemType, getEnvironment());
							TypeElement typeElement = getProcessingEnvironment().getElementUtils().getTypeElement(elemGeneratedType.substring(0, elemGeneratedType.length()-1));
							if (typeElement == null) continue;
							
							LocalDBModel annotated = typeElement.getAnnotation(LocalDBModel.class);
							if (annotated != null) {
								this.columnFields.put(elemName, elemGeneratedType);
								this.isList.add(isList ? "true" : "false");
								
								break;
							}							
						}
					}
			}
		
		//Apply to Extensions
		List<? extends TypeMirror> superTypes = getProcessingEnvironment().getTypeUtils().directSupertypes(element.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = getProcessingEnvironment().getElementUtils().getTypeElement(type.toString());
			
			if (superElement.getAnnotation(Extension.class) != null) {
				getLocalDBModelFields(superElement);
			}
			
			break;
		}
	}

	@Override
	public void process(Element element, EComponentHolder holder) {
		
		if (element.getKind().isField()) return;
		if (element instanceof ExecutableElement) return;
		
		columnFields.clear();
		isList.clear();
		getLocalDBModelFields((TypeElement) element);
		
		final UseModelHolder useModelHolder = holder.getPluginHolder(new UseModelHolder(holder));
		
		ExecutableElement dbModelLoaded = useModelHolder.getAfterLoadMethod();
		if (dbModelLoaded != null 
			&& dbModelLoaded.getAnnotation(LocalDBModel.class) == null) {
			dbModelLoaded = null;
		}

		ExecutableElement dbModelPut = useModelHolder.getAfterPutMethod();
		if (dbModelPut != null 
			&& dbModelPut.getAnnotation(LocalDBModel.class) == null) {
			dbModelPut = null;
		}

		
		super.process(element, holder);
		
		//Set the table name
		String tableName = element.getAnnotation(LocalDBModel.class).table();
		if (tableName.equals("")) {
			tableName = ((TypeElement)element).getSimpleName().toString().toLowerCase();
			if (!tableName.endsWith("s")) tableName = English.plural(tableName);
		}
		useModelHolder.getGeneratedClass().annotate(getJClass("com.activeandroid.annotation.Table")).param("name", tableName);
		
		insertInGetModel(dbModelLoaded, element, useModelHolder);
		
		insertInSelectedGetModel(dbModelLoaded, element, useModelHolder);
		
		insertInGetModelList(dbModelLoaded, element, useModelHolder);
		
		insertInSelectedGetModelList(dbModelLoaded, element, useModelHolder);
		
		if (!element.getAnnotation(LocalDBModel.class).ignorePut()) {
			insertInPutModel(dbModelPut, element, useModelHolder);
		}
		
		JMethod readObjectMethod = useModelHolder.getReadObjectMethod();
		JMethod writeObjectMethod = useModelHolder.getWriteObjectMethod();
		writeObjectMethod.body().invoke(ref("oos"), "writeObject").arg(_this().invoke("getId"));
		readObjectMethod.body().invoke(_this(), "setId")
		                       .arg(cast(getJClass(Long.class), ref("ois").invoke("readObject")));
	}

	private void insertInSelectedGetModelList(ExecutableElement dbModelLoaded, Element element,
			UseModelHolder holder) {
		
		LocalDBModel annotation = element.getAnnotation(LocalDBModel.class);
		JFieldRef context = ref("context");
		JFieldRef query = ref("query");
		JFieldRef orderBy = ref("orderBy");
		JFieldRef useModel = ref("useModel");
		
		//Write the getLocalDbModels in the generated getModelList() method inside the UseModel clause
		JBlock block = holder.getGetModelListUseBlock();
		block = block._if(useModel.invoke("contains").arg(dotclass(getJClass(LocalDBModel.class))))._then();
		
		if (!annotation.defaultQuery().equals("")) {
			block._if(query.invoke("equals").arg(""))._then()
		     	 .assign(query, FormatsUtils.expressionFromString(annotation.defaultQuery()));	
		}
		
		JFieldRef localDbModels = ref("models");
		block.assign(localDbModels, 
				invoke("getLocalDBModels").arg(context).arg(query).arg(orderBy)
			);
		block._return(localDbModels);
	}

	private void insertInGetModelList(ExecutableElement dbModelLoaded, Element element,
			UseModelHolder holder) {
		
		LocalDBModel annotation = element.getAnnotation(LocalDBModel.class);
		JFieldRef context = ref("context");
		JFieldRef query = ref("query");
		JFieldRef orderBy = ref("orderBy");
		
		//Write the getLocalDbModels in the generated getModelList() method
		JBlock block = holder.getGetModelListBlock();
		
		if (!annotation.defaultQuery().equals("")) {
			block._if(query.invoke("equals").arg(""))._then()
		     	 .assign(query, FormatsUtils.expressionFromString(annotation.defaultQuery()));	
		}
		
		JFieldRef localDbModels = ref("models");
		block.assign(localDbModels, 
				invoke("getLocalDBModels").arg(context).arg(query).arg(orderBy)
			);
		block._if(localDbModels.ne(_null()).cand(localDbModels.invoke("isEmpty").not()))
			 ._then()._return(localDbModels);
	}

	private void insertInSelectedGetModel(ExecutableElement dbModelLoaded, Element element,
			UseModelHolder holder) {

		LocalDBModel annotation = element.getAnnotation(LocalDBModel.class);
		JFieldRef context = ref("context");
		JFieldRef query = ref("query");
		JFieldRef orderBy = ref("orderBy");
		JFieldRef useModel = ref("useModel");

		//Write the getLocalDbModel in the generated getModel() method inside the UseModel clause
		JBlock block = holder.getGetModelUseBlock();
		block = block._if(useModel.invoke("contains").arg(dotclass(getJClass(LocalDBModel.class))))._then();
		
		if (!annotation.defaultQuery().equals("")) {
			block._if(query.invoke("equals").arg(""))._then()
		     	 .assign(query, FormatsUtils.expressionFromString(annotation.defaultQuery()));	
		}
		
		JFieldRef localDbModel = ref("model");
		block.assign(localDbModel, 
				invoke("getLocalDBModel").arg(context).arg(query).arg(orderBy)
			);
		
		JConditional cond = block._if(localDbModel.ne(_null()));
		cond._then()._return(localDbModel);
		cond._else()._return(_new(holder.getGeneratedClass()).arg(context));
		
	}

	private void insertInPutModel(ExecutableElement dbModelPut, Element element,
			UseModelHolder holder) {
		
		JFieldRef query = ref("query");
		JFieldRef orderBy = ref("orderBy");
		
		//Write the putLocalDbModel in the generated putModels() method
		JBlock block = holder.getPutModelInitBlock();
		
		JBlock putLocalDBModel = new JBlock();
		putLocalDBModel._if(ref("result").ne(_null()))._then()
		               .assign(ref("result"), _this().invoke("putLocalDBModel").arg(query).arg(orderBy));
		
		
		SharedRecords.priorityAdd(
				block, 
				putLocalDBModel, 
				200
			);
		
	}

	private void insertInGetModel(ExecutableElement dbModelLoaded,
			Element element, UseModelHolder holder) {
		
		LocalDBModel annotation = element.getAnnotation(LocalDBModel.class);
		JFieldRef context = ref("context");
		JFieldRef query = ref("query");
		JFieldRef orderBy = ref("orderBy");
		
		//Write the getLocalDbModel in the generated getModel() method
		JBlock block = holder.getGetModelBlock();
		
		if (!annotation.defaultQuery().equals("")) {
			block._if(query.invoke("equals").arg(""))._then()
		     	 .assign(query, FormatsUtils.expressionFromString(annotation.defaultQuery()));	
		}
		
		JFieldRef localDbModel = ref("model");
		block.assign(localDbModel, 
				invoke("getLocalDBModel").arg(context).arg(query).arg(orderBy)
			);
		block._if(localDbModel.ne(_null()))._then()._return(localDbModel);
		
	}
}
