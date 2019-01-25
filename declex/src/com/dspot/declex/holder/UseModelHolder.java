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
import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.dotclass;
import static com.helger.jcodemodel.JExpr.lit;
import static com.helger.jcodemodel.JExpr.ref;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.annotations.export.Exported;
import org.androidannotations.Option;
import org.androidannotations.helper.ADIHelper;
import org.androidannotations.helper.APTCodeModelHelper;
import org.androidannotations.holder.BaseGeneratedClassHolder;
import org.androidannotations.holder.EBeanHolder;
import org.androidannotations.plugin.PluginClassHolder;

import com.dspot.declex.annotation.RunWith;
import com.dspot.declex.annotation.UseModel;
import com.dspot.declex.api.action.runnable.OnFailedRunnable;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JForEach;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class UseModelHolder extends PluginClassHolder<BaseGeneratedClassHolder> {
	
	public static final Option OPTION_GENERATE_IS_GETTERS = new Option("generation.booleanIsGetters", "true");

	private JMethod writeObjectMethod;
	private JMethod readObjectMethod;
	
	private Map<String, Element> fields;
	private Map<Element, JMethod> getters;
	private Map<Element, JMethod> setters;
	
	private JMethod getModelMethod;
	private JBlock getModelInitBlock;
	private JBlock getModelUseBlock;
	private JBlock getModelBlock;
	
	
	private JMethod getModelListMethod;
	private JBlock getModelListInitBlock;
	private JVar getModelInitBlockOnFailed;
	private JBlock getModelListUseBlock;
	private JBlock getModelListBlock;
	
	private JMethod modelInitMethod;
	
	private JMethod constructorMethod;
	private JBlock constructorBody;
	
	private JMethod putModelMethod;
	private JBlock putModelInitBlock;
	
	private JFieldVar fullInitVar;
	private JFieldVar existsVar;
	
	final AbstractJClass STRING;
	final AbstractJClass MAP;
	final AbstractJClass LIST;
	final AbstractJClass CONTEXT;
	final AbstractJClass OBJECT;
	
	private ExecutableElement afterLoadMethod;
	private ExecutableElement afterPutMethod;
	
	private ADIHelper adiHelper;
	private APTCodeModelHelper codeModelHelper;
	
	public UseModelHolder(BaseGeneratedClassHolder holder) {
		super(holder);
		
		this.adiHelper = new ADIHelper(environment());
		this.codeModelHelper = new APTCodeModelHelper(environment());
		
		STRING = environment().getClasses().STRING;
		MAP = environment().getClasses().MAP.narrow(String.class, Object.class);
		LIST = environment().getClasses().LIST;
		CONTEXT = environment().getClasses().CONTEXT;
		OBJECT = environment().getClasses().OBJECT;
	}
	
	public ExecutableElement getAfterLoadMethodFor(Class<Annotation> annotation) {
		if (afterLoadMethod == null) return null;
		if (afterLoadMethod.getAnnotation(annotation)==null) return null;
		
		return afterLoadMethod;
	}
	
	public ExecutableElement getAfterPutMethodFor(Class<Annotation> annotation) {
		if (afterPutMethod == null) return null;
		if (afterPutMethod.getAnnotation(annotation)==null) return null;

		return afterPutMethod;
	}
	
	public ExecutableElement getAfterLoadMethod() {
		return afterLoadMethod;
	}
	
	public ExecutableElement getAfterPutMethod() {
		return afterPutMethod;
	}
	
	public void setAfterLoadMethod(ExecutableElement afterLoadMethod) {
		this.afterLoadMethod = afterLoadMethod;
	}
	
	public void setAfterPutMethod(ExecutableElement afterPutMethod) {
		this.afterPutMethod = afterPutMethod;
	}
	
	public JFieldVar getFullInitVar() {
		if (fullInitVar == null) {
			setExistenceStructure();
		}
		return fullInitVar;
	}
	
	public JFieldVar getExistsVar() {
		if (existsVar == null) {
			setExistenceStructure();
		}
		return existsVar;
	}
	
	public JMethod getWriteObjectMethod() {
		if (writeObjectMethod == null) {
			setSerializable();
		}
		return writeObjectMethod;
	}
	
	public JMethod getReadObjectMethod() {
		if (readObjectMethod == null) {
			setSerializable();
		}
		return readObjectMethod;
	}
	
	public Map<String, Element> getFields() {
		if (fields == null) {
			getFieldsPlusGettersAndSetters();
		}
		return fields;
	}
	
	private void setSerializable() {
		
		getGeneratedClass()._implements(Serializable.class);
		
		getGeneratedClass().field(JMod.PRIVATE | JMod.FINAL | JMod.STATIC, getCodeModel().LONG, "serialVersionUID", lit(-2030719779765967535L));
		
		writeObjectMethod = getGeneratedClass().method(JMod.PRIVATE, getCodeModel().VOID, "writeObject");
		writeObjectMethod._throws(IOException.class);
		JVar oos = writeObjectMethod.param(ObjectOutputStream.class, "oos");
				
		readObjectMethod = getGeneratedClass().method(JMod.PRIVATE, getCodeModel().VOID, "readObject");
		readObjectMethod._throws(IOException.class);
		readObjectMethod._throws(ClassNotFoundException.class);
		JVar ois = readObjectMethod.param(ObjectInputStream.class, "ois");
		
		for (Entry<String, Element> field : getFields().entrySet()) {
			final String fieldName = field.getKey();
			final String fieldClass = field.getValue().asType().toString();
			
			final JFieldRef fieldRef = ref(fieldName);
			
			//If it is primitive
			if (!fieldClass.contains(".")) {
				if (fieldClass.equals("boolean")) {
					writeObjectMethod.body().invoke(oos, "writeBoolean").arg(fieldRef);
					readObjectMethod.body().assign(fieldRef, ois.invoke("readBoolean"));
				} else if (fieldClass.equals("int")) {
					writeObjectMethod.body().invoke(oos, "writeInt").arg(fieldRef);
					readObjectMethod.body().assign(fieldRef, ois.invoke("readInt"));
				} else if (fieldClass.equals("short")) {
					writeObjectMethod.body().invoke(oos, "writeShort").arg(fieldRef);
					readObjectMethod.body().assign(fieldRef, ois.invoke("readShort"));
				} else if (fieldClass.equals("double")) {
					writeObjectMethod.body().invoke(oos, "writeDouble").arg(fieldRef);
					readObjectMethod.body().assign(fieldRef, ois.invoke("readDouble"));
				} else if (fieldClass.equals("float")) {
					writeObjectMethod.body().invoke(oos, "writeFloat").arg(fieldRef);
					readObjectMethod.body().assign(fieldRef, ois.invoke("readFloat"));
				} else if (fieldClass.equals("byte")) {
					writeObjectMethod.body().invoke(oos, "writeByte").arg(fieldRef);
					readObjectMethod.body().assign(fieldRef, ois.invoke("readByte"));
				} else if (fieldClass.equals("long")) {
					writeObjectMethod.body().invoke(oos, "writeLong").arg(fieldRef);
					readObjectMethod.body().assign(fieldRef, ois.invoke("readLong"));
				} 
			} else {
				AbstractJClass fieldJClass = codeModelHelper.elementTypeToJClass(field.getValue());
				writeObjectMethod.body().invoke(oos, "writeObject").arg(fieldRef);
				readObjectMethod.body().assign(fieldRef, cast(fieldJClass, ois.invoke("readObject")));
			}
		}
	}
	
	private void getFieldsPlusGettersAndSetters() {
		
		fields = new HashMap<>();
		getters = new HashMap<>();
		setters = new HashMap<>();
		
		Map<String, List<ExecutableElement>> methodsToCheck = new HashMap<>();
		getFieldsPlusGettersAndSetters(getAnnotatedElement(), methodsToCheck);
		
		for (String field : fields.keySet()) {
			final Element fieldElement = fields.get(field);
			final String fieldName = fieldElement.getSimpleName().toString();
			
			final String getterName = fieldToGetter(field);
			final String getterIsName = "is" + fieldToGetter(field).substring(3);
			final String setterName = fieldToSetter(field);
			
			boolean createIsGetter = false;
			
			boolean optionGenerateIsGetters = environment().getOptionBooleanValue(OPTION_GENERATE_IS_GETTERS);
			if (optionGenerateIsGetters) {
				createIsGetter = fieldElement.asType().toString().equals("boolean")
		                 || fieldElement.asType().toString().equals(Boolean.class.getCanonicalName());
			}
			
			boolean createGetter = true;
			boolean createSetter = true;
			
			if (createGetter && methodsToCheck.containsKey(getterName)) {
					
				for (ExecutableElement element : methodsToCheck.get(getterName)) {
					if (element.getParameters().size() > 0) continue;
					if (element.getReturnType().toString().equals("void")) continue;
					createGetter = false;
					break;
				}
				
			}
			
			if (createIsGetter && methodsToCheck.containsKey(getterIsName)) {
				
				for (ExecutableElement element : methodsToCheck.get(getterIsName)) {
					if (element.getParameters().size() > 0) continue;
					if (element.getReturnType().toString().equals("void")) continue;
					createIsGetter = false;
					break;
				}
				
			}
			
			if (methodsToCheck.containsKey(setterName)) {
				
				for (ExecutableElement element : methodsToCheck.get(setterName)) {
					if (element.getParameters().size() != 1) continue;
					if (!element.getReturnType().toString().equals("void")) continue;					
					
					VariableElement param = element.getParameters().get(0);
					if (!param.asType().toString().equals(fieldName)) continue;
					
					createSetter = false;
					break;
				}
			}
			
			if (createGetter) {
				JMethod getterMethod = getGeneratedClass().method(
						JMod.PUBLIC, 
						codeModelHelper.elementTypeToJClass(fieldElement),
						getterName
					);
				getterMethod.body()._return(_this().ref(fieldName));
				getters.put(fieldElement, getterMethod);				
			}
			
			if (createIsGetter) {
				JBlock getterBody = getGeneratedClass().method(
						JMod.PUBLIC, 
						getCodeModel().BOOLEAN, 
						getterIsName
					).body();
				
				getterBody._return(_this().ref(fieldName));
			}
			
			if (createSetter) {
				final AbstractJClass fieldClass = codeModelHelper.elementTypeToJClass(fieldElement);
				JMethod setterMethod = getGeneratedClass().method(JMod.PUBLIC, getCodeModel().VOID, setterName);
				JVar setterParam = setterMethod.param(fieldClass, fieldName);
				setterMethod.body().assign(_this().ref(fieldName), setterParam);				
				setters.put(fieldElement, setterMethod);
			}
		}
	}
	
	private void getFieldsPlusGettersAndSetters(TypeElement element, Map<String, List<ExecutableElement>> methodsToCheck) {

		List<? extends Element> elems = element.getEnclosedElements();
		ELEMENTS: for (Element elem : elems) {
			final String elemName = elem.getSimpleName().toString();
			
			if (elem.getModifiers().contains(Modifier.STATIC)) continue;
			if (elem.getModifiers().contains(Modifier.PRIVATE)) continue;
			if (elem.getModifiers().contains(Modifier.TRANSIENT)) continue;

			//Omit specials fields
			if (elem.getAnnotation(RunWith.class) != null) continue;
			if (adiHelper.hasAnnotation(elem, Exported.class)) continue;
			
			for (AnnotationMirror annotation : elem.getAnnotationMirrors()) {
				if (environment().getSupportedAnnotationTypes()
			            .contains(annotation.getAnnotationType().toString())) {

					try {
						@SuppressWarnings("unchecked")
						Class<Annotation> annotationClass = (Class<Annotation>) getClass().getClassLoader().loadClass(annotation.getAnnotationType().toString());
						if (adiHelper.hasAnnotation(element, annotationClass)) continue ELEMENTS;
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}	
			}
						
			if (elem.getKind() == ElementKind.FIELD) {
				fields.put(elemName, elem);
			}
			
			if (elem.getKind() == ElementKind.METHOD) {
				ExecutableElement executableElement = (ExecutableElement) elem;
				
				List<ExecutableElement> methods = methodsToCheck.get(elemName);
				if (methods == null) {
					methods = new LinkedList<>();
					methodsToCheck.put(elemName, methods);
				}
				
				methods.add(executableElement);
			}
		}
		
		//Apply to Extensions
		final ProcessingEnvironment env = environment().getProcessingEnvironment();
		List<? extends TypeMirror> superTypes = env.getTypeUtils().directSupertypes(element.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = env.getElementUtils().getTypeElement(type.toString());
			if (superElement == null) continue;
			
			if (adiHelper.hasAnnotation(superElement, UseModel.class)) {
				getFieldsPlusGettersAndSetters(superElement, methodsToCheck);
			}
		}
		
		
	}
	
	private void setExistenceStructure() {
		try {
			fullInitVar = getGeneratedClass().field(JMod.PUBLIC | JMod.TRANSIENT, getCodeModel().BOOLEAN, "_fullInit", JExpr.FALSE);
			existsVar = getGeneratedClass().field(JMod.TRANSIENT, getCodeModel().BOOLEAN, "_exists", JExpr.TRUE);
			
			Field constructorField = EBeanHolder.class.getDeclaredField("constructor");
			constructorField.setAccessible(true);
			JMethod constructor = (JMethod) constructorField.get(holder());
			constructor.body().assign(existsVar, JExpr.FALSE);
			
			JMethod method = getGeneratedClass().method(JMod.PUBLIC, getCodeModel().BOOLEAN, "exists");
			method.body()._return(existsVar);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}		
	}
	
	public JMethod getLoadModelMethod() {
		if (getModelMethod == null) {
			setGetModel();
		}
		return getModelMethod;
	}
	
	public JBlock getGetModelInitBlock() {
		if (getModelInitBlock == null) {
			setGetModel();
		}
		return getModelInitBlock;
	}
	
	public JVar getGetModelInitBlockOnFailed() {
		
		if (getModelInitBlockOnFailed == null) {
			
			JDefinedClass anonymousRunnable = getCodeModel().anonymousClass(OnFailedRunnable.class);
			JMethod anonymousRunnableRun = anonymousRunnable.method(JMod.PUBLIC, getCodeModel().VOID, "run");
			anonymousRunnableRun.annotate(Override.class);
			anonymousRunnableRun.body()._throw(_new(getJClass(RuntimeException.class)).arg(ref("e")));

			JBlock block = getGetModelInitBlock();
			getModelInitBlockOnFailed = block.decl(
					getJClass(OnFailedRunnable.class), 
					"onFailed",
					_new(anonymousRunnable)
				);			
			
			block = getGetModelListInitBlock();
			block.decl(
					getJClass(OnFailedRunnable.class), 
					"onFailed",
					_new(anonymousRunnable)
				);
		}
		
		return getModelInitBlockOnFailed;
	}
	
	public JMethod getGetModelMethod() {
		if (getModelMethod == null) {
			setGetModel();
		}
		return getModelMethod;
	}
	
	public JBlock getGetModelUseBlock() {
		if (getModelUseBlock == null) {
			setGetModel();
		}
		return getModelUseBlock;
	}
	
	public JBlock getGetModelBlock() {
		if (getModelBlock == null) {
			setGetModel();
		}
		return getModelBlock;
	}
	
	public JMethod getGetModelListMethod() {
		if (getModelListMethod == null) {
			setGetModelList();
		}
		return getModelListMethod;
	}
	
	public JBlock getGetModelListInitBlock() {
		if (getModelListInitBlock == null) {
			setGetModelList();
		}
		return getModelListInitBlock;
	}
	
	public JBlock getGetModelListUseBlock() {
		if (getModelListUseBlock == null) {
			setGetModelList();
		}
		return getModelListUseBlock;
	}
	
	public JBlock getGetModelListBlock() {
		if (getModelListBlock == null) {
			setGetModelList();
		}
		return getModelListBlock;
	}
	
	public JMethod getModelInitMethod() {
		if (modelInitMethod == null) {
			setModelInit();
		}
		return modelInitMethod;
	}
	
	public JMethod getPutModelMethod() {
		if (putModelMethod == null) {
			setPutModel();
		}
		return putModelMethod;
	}
	
	public JBlock getPutModelInitBlock() {
		if (putModelInitBlock == null) {
			setPutModel();
		}
		return putModelInitBlock;
	}
	
	public JMethod getConstructorMethod() {
		if (constructorMethod == null) {
			setConstructor();
		}
		return constructorMethod;
	}
	
	public JBlock getConstructorBody() {
		if (constructorBody == null) {
			setConstructor();
		}
		return constructorBody;
	}
	
	public static String getModelName() {
		return "getModel_";
	}
	
	public static String modelInitName() {
		return "modelInit_";
	}
	
	public static String putModelName() {
		return "putModel_";
	}
	
	public static String getModelListName() {
		return "getModelList_";
	}
	
	private void setConstructor() {
		constructorMethod = getGeneratedClass().constructor(JMod.PUBLIC);
		constructorBody = constructorMethod.body();
		constructorBody.invoke("super");
	}
	
	private void setGetModel() {
		getModelMethod = getGeneratedClass().method(JMod.PUBLIC | JMod.STATIC, getGeneratedClass(), getModelName());
		JVar context = getModelMethod.param(CONTEXT, "context");
		getModelMethod.param(MAP, "args");
		
		JVar useModels = getModelMethod.param(LIST.narrow(getJClass(Class.class).narrow(getCodeModel().ref(Annotation.class).wildcard())), "useModels");
		getModelInitBlock = getModelMethod.body().block();
		
		getModelMethod.body().decl(getGeneratedClass(), "model");
		
		JForEach forEach = getModelMethod.body()._if(useModels.neNull())._then()
				.forEach(getJClass(Class.class).narrow(getCodeModel().ref(Annotation.class).wildcard()), "useModel", useModels);
		
		JVar useModel = forEach.var();
		getModelUseBlock = forEach.body();
		
		getModelUseBlock._if(useModel.invoke("equals").arg(dotclass(getJClass(UseModel.class))))._then()
		                ._return(_new(getGeneratedClass()).arg(context));
		
		getModelBlock = getModelMethod.body().block();
		getModelMethod.body()._return( _new(getGeneratedClass()).arg(context));	
	}
	
	private void setModelInit() {
		modelInitMethod = getGeneratedClass().method(JMod.PUBLIC, getCodeModel().VOID, modelInitName());		
		modelInitMethod.param(MAP, "args");
	}
	
	private void setGetModelList() {
		//getModelList method
		getModelListMethod = getGeneratedClass().method(JMod.PUBLIC | JMod.STATIC, LIST.narrow(getGeneratedClass()), getModelListName());
		getModelListMethod.param(CONTEXT, "context");		
		getModelListMethod.param(MAP, "args");
		
		JVar useModels = getModelListMethod.param(LIST.narrow(getJClass(Class.class).narrow(getCodeModel().ref(Annotation.class).wildcard())), "useModels");
		getModelListInitBlock = getModelListMethod.body().block();
		
		getModelListMethod.body().decl(LIST.narrow(getGeneratedClass()), "models");
		
		JForEach forEach = getModelListMethod.body()._if(useModels.neNull())._then()
				.forEach(getJClass(Class.class).narrow(getCodeModel().ref(Annotation.class).wildcard()), "useModel", useModels);
		
		JVar useModel = forEach.var();
		getModelListUseBlock = forEach.body();
		
		JBlock ifUseModelBlock = getModelListUseBlock._if(useModel.invoke("equals").arg(dotclass(getJClass(UseModel.class))))._then();
		
		JVar result = ifUseModelBlock.decl(
				getJClass("java.util.ArrayList").narrow(getGeneratedClass()), 
				"result", 
				_new(getJClass("java.util.ArrayList").narrow(getGeneratedClass())));
		ifUseModelBlock._return(result);
		
		/*TODO
		JBlock ifUseModelBlockWithQuery = ifUseModelBlock._if(query.invoke("equals").arg("").not())._then();
		ifUseModelBlock._return(result);
				
		JVar entries = ifUseModelBlockWithQuery.decl(STRING.array(), "entries", query.invoke("split").arg("$"));
		JBlock ifUseModelBlockWithQueryForEach = ifUseModelBlockWithQuery.forEach(STRING, "entry", entries).body();
		JFieldRef entry = ref("entry");
		
		JVar key = ifUseModelBlockWithQueryForEach.decl(
					STRING, 
					"key", 
					entry.invoke("substring").arg(lit(0)).arg(entry.invoke("indexOf").arg("="))
				);
		JVar value = ifUseModelBlockWithQueryForEach.decl(
					STRING, 
					"value",
					entry.invoke("substring").arg(entry.invoke("indexOf").arg("=").plus(lit(1)))
				);
		
		JForLoop subFor = ifUseModelBlockWithQueryForEach._if(key.invoke("equals").arg("count"))._then()._for();
		JVar i = subFor.init(getCodeModel().INT, "i", lit(0));
		subFor.test(i.lt(getJClass("Integer").staticInvoke("valueOf").arg(value)));
		subFor.update(i.incr());
		subFor.body().invoke(result, "add").arg(_new(getGeneratedClass()).arg(context));
		subFor.body()._continue();
		
		ifUseModelBlockWithQueryForEach.directStatement("//TODO assign fields directly");
		*/	
		
		getModelListBlock = getModelListMethod.body().blockVirtual();
		getModelListMethod.body()._return(_new(getJClass("java.util.ArrayList").narrow(getGeneratedClass())));		
	}
	
	private void setPutModel() {
		putModelMethod = getGeneratedClass().method(JMod.PUBLIC, OBJECT, putModelName());
		putModelMethod.param(MAP, "args");

		JBlock putModelMethodBody = putModelMethod.body(); 
		JVar result = putModelMethodBody.decl(OBJECT, "result", _new(OBJECT));
		putModelInitBlock = putModelMethodBody.block();
				
		putModelMethodBody._return(result);
	}

}
