package com.dspot.declex.override.handler;

import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.direct;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.rclass.IRClass.Res;

import com.dspot.declex.action.ActionHandler;
import com.dspot.declex.api.populator.Populator;
import com.dspot.declex.share.holder.ViewsHolder;
import com.dspot.declex.share.holder.ViewsHolder.IdInfoHolder;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.ParamUtils;
import com.dspot.declex.util.SharedRecords;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;

public class BaseViewListenerHandler extends ActionHandler {

	public BaseViewListenerHandler(Class<?> targetClass, AndroidAnnotationsEnvironment environment) {
		super(targetClass, environment);
	}
	
	protected void createDeclarationForLists(String referecedId, Map<AbstractJClass, IJExpression> declForListener, 
			Element element, ViewsHolder viewsHolder) {
		
		List<? extends Element> elems = element.getEnclosingElement().getEnclosedElements();
		for (Element elem : elems) {				
			if (elem.getKind() == ElementKind.FIELD) {
				
				if (elem.getSimpleName().toString().equals(referecedId)) {
					
					Populator populator = elem.getAnnotation(Populator.class);
					if (populator != null && TypeUtils.isSubtype(elem, "java.util.Collection", getProcessingEnvironment())) {
						
						String className = elem.asType().toString();
						String fieldName = elem.getSimpleName().toString();
						
						Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(className);
						if (matcher.find()) {
							className = matcher.group(1);
						}
						
						boolean castNeeded = false;
						if (!className.endsWith(ModelConstants.generationSuffix())) {
							if (SharedRecords.getModel(className, getEnvironment()) != null) {
								className = className + ModelConstants.generationSuffix();
								castNeeded = true;
							}
						}
						className = TypeUtils.typeFromTypeString(className, getEnvironment());
						
						//Get the model
						JFieldRef position = ref("position");
						IJExpression modelAssigner = ref(fieldName).invoke("get").arg(position);
						AbstractJClass Model = getJClass(className);
						if (castNeeded) modelAssigner = cast(Model, ref(fieldName).invoke("get").arg(position));
						declForListener.put(Model, modelAssigner);
					}
					
					break;
				} else 	if (referecedId.startsWith(elem.getSimpleName().toString()) && 
						    elem.getAnnotation(Populator.class)!=null) {
					
					String className = elem.asType().toString();
					String fieldName = elem.getSimpleName().toString();

					final boolean isPrimitive = elem.asType().getKind().isPrimitive() || 
							elem.asType().toString().equals(String.class.getCanonicalName());
					
					//Detect when the method is a List, in order to generate all the Adapters structures
					boolean isList = TypeUtils.isSubtype(elem, CanonicalNameConstants.LIST, getProcessingEnvironment());		
					if (isList) {
						Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(className);
						if (matcher.find()) {
							className = matcher.group(1);
						}
					}
					
					if (isPrimitive || isList) continue;
					
					if (className.endsWith("_")) {
						className = TypeUtils.typeFromTypeString(className, getEnvironment());
						className = className.substring(0, className.length()-1);
					}
					
					//Find all the fields and methods that are presented in the layouts
					Map<String, IdInfoHolder> fields = new HashMap<String, IdInfoHolder>();
					Map<String, IdInfoHolder> methods = new HashMap<String, IdInfoHolder>();
					viewsHolder.findFieldsAndMethods(className, fieldName, elem, fields, methods, true);
					
					String composedField = null;
					for (String field : fields.keySet()) {
						if (!fields.get(field).idName.equals(referecedId)) continue;
							
						composedField = "";
						for (String fieldPart : field.split("\\."))
							composedField = composedField + "." + fieldToGetter(fieldPart);
						
						className = fields.get(field).type.toString();
					}
					
					for (String method : methods.keySet()) {
						if (!methods.get(method).idName.equals(referecedId)) continue;
						
						composedField = "";
						String[] methodSplit = method.split("\\.");
						for (int i = 0; i < methodSplit.length-1; i++) {
							composedField = composedField + "." + fieldToGetter(methodSplit[i]);
						}
						composedField = composedField + "." + methodSplit[methodSplit.length-1];
						
						className = methods.get(method).type.toString();
					}
					
					if (composedField == null) continue;
					
					isList = TypeUtils.isSubtype(className, "java.util.Collection", getProcessingEnvironment());		
					if (isList) {
						Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(className);
						if (matcher.find()) {
							className = matcher.group(1);
							if (className.endsWith("_")) {
								className = TypeUtils.typeFromTypeString(className, getEnvironment());
							}
						}
					} 
					
					IJExpression assignRef = ref(fieldName);
					
					String[] methodSplit = composedField.split("\\.");
					for (int i = 0; i < methodSplit.length; i++) {
						String methodPart = methodSplit[i];
						if (!methodPart.equals("")) {
							assignRef = assignRef.invoke(methodPart);		
						}			
					}
					
					//Get the model
					JFieldRef position = ref("position");
					IJExpression modelAssigner = assignRef.invoke("get").arg(position);
					AbstractJClass Model = getJClass(className);
					declForListener.put(Model, modelAssigner);
				}

			}
		}						
	}
	
	@Override
	protected IJStatement getStatement(AbstractJClass elementClass, Element element, ViewsHolder viewsHolder) {
		
		//The Field actions are executed by the ActionHandler
		if (!(element instanceof ExecutableElement)) {
			return super.getStatement(elementClass, element, viewsHolder);	
		}
    	
		final String methodName = element.getSimpleName().toString();
		
		JInvocation invoke = JExpr.invoke(methodName);
		
		ExecutableElement exeElem = (ExecutableElement) element;
		for (VariableElement param : exeElem.getParameters()) {
			final String paramName = param.getSimpleName().toString();
			final String paramType = param.asType().toString();
			
			//Reference for fields
			if (paramName.equals("refs") && TypeUtils.isSubtype(paramType, CanonicalNameConstants.LIST, getProcessingEnvironment())) {
				
				Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(paramType);
				if (matcher.find()) {
					if (TypeUtils.isSubtype(matcher.group(1), CanonicalNameConstants.VIEW, getProcessingEnvironment())) {
						
						String invocation = Arrays.class.getCanonicalName() + ".<" + matcher.group(1) + ">asList(";
						List<String> names = getNames(element);
						for (String name : names) {
							invocation = invocation + name + DeclexConstant.VIEW + ",";
						}
						invocation = invocation.substring(0, invocation.length() - 1) + ")";
						
						invoke.arg(direct(invocation));
				
						continue;
					}
				}				
			}
			
			ParamUtils.injectParam(paramName, invoke, viewsHolder);
		}
		
		return invoke;
	}
	
	@Override
	protected List<String> getNames(Element element) {
		List<String> idsRefs = annotationHelper.extractAnnotationResources(element, Res.ID, true);
		
		List<String> names = new ArrayList<>(idsRefs.size());
		
		for (String field : idsRefs) {
			names.add(field.substring(field.lastIndexOf('.') + 1));
		}
		
		return names;
	}
	
	@Override
	protected String getClassName(Element element) {
		return null;
	}
	
}
