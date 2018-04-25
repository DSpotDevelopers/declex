/**
 * Copyright (C) 2016-2018 DSpot Sp. z o.o
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

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.lit;

import java.util.List;

import javax.lang.model.element.Element;

import org.androidannotations.holder.EComponentHolder;

import com.dspot.declex.adapter.plugin.HolderClassCreator;
import com.dspot.declex.adapter.plugin.JClassPlugin;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JClassAlreadyExistsException;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class RecyclerViewAdapterClassCreator extends HolderClassCreator {
	
	AbstractJClass RecyclerViewAdapter;
	
	final AbstractJClass RecyclerViewHolder;
	final AbstractJClass Model;
	final AbstractJClass ArrayList;
	
	final String className;
	
	public RecyclerViewAdapterClassCreator(String modelClassName, String className, Element element, 
			EComponentHolder holder, List<JClassPlugin> adapterPlugins) {
		super(element, holder);
		
		RecyclerViewHolder = getJClass(className + "ViewHolder");
		RecyclerViewAdapter = getJClass("android.support.v7.widget.RecyclerView.Adapter").narrow(RecyclerViewHolder);
		Model = getJClass(modelClassName);
		ArrayList = getJClass("java.util.ArrayList").narrow(Model);
		this.className = className;
		
		for (JClassPlugin plugin : adapterPlugins) {
			AbstractJClass newBaseAdapter = plugin.getBaseAdapter(element);
			if (newBaseAdapter != null) {
				RecyclerViewAdapter = newBaseAdapter.narrow(RecyclerViewHolder);
			}
			this.addPlugin(plugin);
		}
	}

	@Override
	public JDefinedClass getDefinedClass() throws JClassAlreadyExistsException {
		
		JDefinedClass AdapterClass = holder.getGeneratedClass()._class(JMod.PRIVATE, className)._extends(RecyclerViewAdapter);
		
		JFieldVar models = AdapterClass.field(JMod.PRIVATE, getClasses().LIST.narrow(Model), "models");
		
		JMethod constructor = AdapterClass.constructor(JMod.PUBLIC);
		JVar paramModels = constructor.param(getClasses().LIST.narrow(Model), "models");
		JConditional ifParamModelsNull = constructor.body()._if(paramModels.eq(_null()));
		ifParamModelsNull._then().assign(
				_this().ref(models), _new(ArrayList)				
		);
		ifParamModelsNull._else().assign(
				_this().ref(models), _new(ArrayList).arg(paramModels)				
		);
		
		//setModels() METHOD
		JMethod setModels = AdapterClass.method(JMod.PUBLIC, getCodeModel().VOID, "setModels");
		JVar modelsParam = setModels.param(getClasses().LIST.narrow(Model), "models");
		JConditional ifModels = setModels.body()._if(modelsParam.ne(_null()));
		ifModels._then().directStatement("//This permits the export modification of the model");
		ifModels._then().directStatement("//without crashing the interface for concurrent modifications");
		ifModels._then().assign(_this().ref(models), _new(ArrayList).arg(modelsParam));
		ifModels._else().invoke(_this().ref(models), "clear");
		
		//getItemCount() METHOD
		JMethod getItemCountMethod = AdapterClass.method(JMod.PUBLIC, getCodeModel().INT, "getItemCount");
		getItemCountMethod.annotate(Override.class);
		getItemCountMethod.body()._if(models.eq(_null()))._then()._return(lit(0));
		
		//inflate() METHOD
		JMethod inflateMethod = AdapterClass.method(JMod.PUBLIC, getClasses().VIEW, "inflate");
		inflateMethod.param(getCodeModel().INT, "viewType");
		inflateMethod.param(getClasses().VIEW_GROUP, "parent");
		inflateMethod.param(getClasses().LAYOUT_INFLATER, "inflater");
				
		JMethod onBindViewHolderMethod = AdapterClass.method(JMod.PUBLIC, getCodeModel().VOID, "onBindViewHolder");
		onBindViewHolderMethod.annotate(Override.class);
		onBindViewHolderMethod.param(JMod.FINAL, RecyclerViewHolder, "viewHolder");
		onBindViewHolderMethod.param(JMod.FINAL, getCodeModel().INT, "position");
		
		JMethod onCreateViewHolderMethod = AdapterClass.method(JMod.PUBLIC, RecyclerViewHolder, "onCreateViewHolder");
		onCreateViewHolderMethod.annotate(Override.class);
		JVar parent = onCreateViewHolderMethod.param(JMod.FINAL, getClasses().VIEW_GROUP, "parent");
		onCreateViewHolderMethod.param(JMod.FINAL, getCodeModel().INT, "viewType");
		
		//Declare an inflater
		onCreateViewHolderMethod.body().decl(
				JMod.FINAL, 
				getJClass("android.view.LayoutInflater"), 
				"inflater", 
				cast(
						getJClass("android.view.LayoutInflater"), 
						parent.invoke("getContext").invoke("getSystemService")
							   .arg(getClasses().CONTEXT.staticRef("LAYOUT_INFLATER_SERVICE"))
					)
			);
		
		for (JClassPlugin plugin : plugins) {
			plugin.process(element, holder, AdapterClass);
		}
		
		getItemCountMethod.body()._return(models.invoke("size"));
		
		return AdapterClass;
	}

}
