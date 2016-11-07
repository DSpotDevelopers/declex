package com.dspot.declex.model;

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._null;
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.holder.BaseGeneratedClassHolder;
import org.androidannotations.holder.EBeanHolder;
import org.androidannotations.plugin.PluginClassHolder;

import com.dspot.declex.api.action.Action;
import com.dspot.declex.api.extension.Extension;
import com.dspot.declex.api.localdb.LocalDBModel;
import com.dspot.declex.api.model.UseModel;
import com.dspot.declex.api.server.ServerModel;
import com.dspot.declex.util.ParamUtils;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JForLoop;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class UseModelHolder extends PluginClassHolder<BaseGeneratedClassHolder> {

	private JMethod writeObjectMethod;
	private JMethod readObjectMethod;
	
	private Map<String, String> fields;
	
	private JMethod getModelMethod;
	private JBlock getModelInitBlock;
	private JBlock getModelUseBlock;
	private JBlock getModelBlock;
	
	private JMethod getModelListMethod;
	private JBlock getModelListInitBlock;
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
	final AbstractJClass LIST;
	final AbstractJClass CONTEXT;
	final AbstractJClass OBJECT;
	
	private ExecutableElement afterLoadMethod;
	private ExecutableElement afterPutMethod;
	
	public UseModelHolder(BaseGeneratedClassHolder holder) {
		super(holder);
		
		STRING = environment().getClasses().STRING;
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
	
	public Map<String, String> getFields() {
		if (fields == null) {
			getFieldsAndMethods(getAnnotatedElement());
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
		
		for (Entry<String, String> field : getFields().entrySet()) {
			final String fieldName = field.getKey();
			final String fieldClass = field.getValue();
			
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
				} else if (fieldClass.equals("short")) {
					writeObjectMethod.body().invoke(oos, "writeFloat").arg(fieldRef);
					readObjectMethod.body().assign(fieldRef, ois.invoke("readFloat"));
				} else if (fieldClass.equals("byte")) {
					writeObjectMethod.body().invoke(oos, "writeByte").arg(fieldRef);
					readObjectMethod.body().assign(fieldRef, ois.invoke("readByte"));
				} else if (fieldClass.equals("short")) {
					writeObjectMethod.body().invoke(oos, "writeLong").arg(fieldRef);
					readObjectMethod.body().assign(fieldRef, ois.invoke("readLong"));
				} 
			} else {
				writeObjectMethod.body().invoke(oos, "writeObject").arg(fieldRef);
				readObjectMethod.body().assign(fieldRef, cast(getJClass(fieldClass), ois.invoke("readObject")));
			}
		}
	}
	
	private void getFieldsAndMethods(TypeElement element) {
		if (fields == null) {
			fields = new HashMap<>();
		}
		
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems) {
			final String elemName = elem.getSimpleName().toString();
			final String elemType = elem.asType().toString();
			
			if (elem.getModifiers().contains(Modifier.STATIC)) continue;

			//Omit specials and private fields
			if (elem.getAnnotation(Action.class) != null) continue;
			if (elem.getAnnotation(ServerModel.class) != null) continue;
			if (elem.getAnnotation(LocalDBModel.class) != null) continue;
			if (elem.getAnnotation(UseModel.class) != null) continue;
			
			if (elem.getKind() == ElementKind.FIELD) {
				if (elem.getModifiers().contains(Modifier.PRIVATE)) continue;
				
				fields.put(elemName, TypeUtils.typeFromTypeString(elemType, environment(), false));
			}
		}
		
		//Apply to Extensions
		final ProcessingEnvironment env = environment().getProcessingEnvironment();
		List<? extends TypeMirror> superTypes = env.getTypeUtils().directSupertypes(element.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = env.getElementUtils().getTypeElement(type.toString());
			
			if (superElement.getAnnotation(Extension.class) != null) {
				getFieldsAndMethods(superElement);
			}
			
			break;
		}
	}
	
	private void setExistenceStructure() {
		try {
			fullInitVar = getGeneratedClass().field(JMod.PUBLIC, getCodeModel().BOOLEAN, "_fullInit", JExpr.FALSE);
			existsVar = getGeneratedClass().field(JMod.NONE, getCodeModel().BOOLEAN, "_exists", JExpr.TRUE);
			
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
	
	public JMethod getGetModelMethod() {
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
	
	private void setConstructor() {
		constructorMethod = getGeneratedClass().constructor(JMod.PUBLIC);
		constructorBody = constructorMethod.body();
		constructorBody.invoke("super");
	}
	
	private void setGetModel() {
		getModelMethod = getGeneratedClass().method(JMod.PUBLIC | JMod.STATIC, getGeneratedClass(), "getModel_");
		JVar context = getModelMethod.param(CONTEXT, "context");
		getModelMethod.param(STRING, "query");
		getModelMethod.param(STRING, "orderBy");

		JVar useModel = getModelMethod.param(LIST.narrow(getJClass(Class.class).narrow(getCodeModel().ref(Annotation.class).wildcard())), "useModel");
		getModelInitBlock = getModelMethod.body().block();
		
		getModelMethod.body().decl(getGeneratedClass(), "model");
		
		getModelUseBlock = getModelMethod.body().block();
		getModelUseBlock._if(useModel.invoke("contains").arg(dotclass(getJClass(UseModel.class))))._then()
		             ._return(_new(getGeneratedClass()).arg(context));
		
		getModelBlock = getModelMethod.body().block();
		getModelMethod.body()._return( _new(getGeneratedClass()).arg(context));	
	}
	
	
	
	private void setModelInit() {
		modelInitMethod = getGeneratedClass().method(JMod.PUBLIC, getCodeModel().VOID, "modelInit_");
		modelInitMethod.param(STRING, "query");
		modelInitMethod.param(STRING, "orderBy");
	}
	
	private void setGetModelList() {
		//getModelList method
		getModelListMethod = getGeneratedClass().method(JMod.PUBLIC | JMod.STATIC, LIST.narrow(getGeneratedClass()), "getModelList_");
		JVar context = getModelListMethod.param(CONTEXT, "context");
		JVar query = getModelListMethod.param(STRING, "query");
		getModelListMethod.param(STRING, "orderBy");
		JVar useModel = getModelListMethod.param(LIST.narrow(getJClass(Class.class).narrow(getCodeModel().ref(Annotation.class).wildcard())), "useModel");
		getModelListInitBlock = getModelListMethod.body().block();
		
		getModelListMethod.body().decl(LIST.narrow(getGeneratedClass()), "models");
		
		getModelListUseBlock = getModelListMethod.body().block();
		JBlock ifUseModelBlock = getModelListUseBlock._if(useModel.invoke("contains").arg(dotclass(getJClass(UseModel.class))))._then();
		
		JVar result = ifUseModelBlock.decl(
				getJClass("java.util.ArrayList").narrow(getGeneratedClass()), 
				"result", 
				_new(getJClass("java.util.ArrayList").narrow(getGeneratedClass())));

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
	
		
		getModelListBlock = getModelListMethod.body().block();
		getModelListMethod.body()._return(_new(getJClass("java.util.ArrayList").narrow(getGeneratedClass())));		
	}
	
	private void setPutModel() {
		putModelMethod = getGeneratedClass().method(JMod.PUBLIC, OBJECT, "putModel_");
		putModelMethod.param(STRING, "query");
		putModelMethod.param(STRING, "orderBy");
		
		JBlock putModelMethodBody = putModelMethod.body(); 
		JVar result = putModelMethodBody.decl(OBJECT, "result", _new(OBJECT));
		putModelInitBlock = putModelMethodBody.block();
				
		putModelMethodBody._return(result);
	}

}
