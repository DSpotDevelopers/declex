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
package com.dspot.declex.server;

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.dotclass;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.lit;
import static com.helger.jcodemodel.JExpr.ref;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.EBean;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.holder.BaseGeneratedClassHolder;

import com.dspot.declex.api.extension.Extension;
import com.dspot.declex.api.model.Model;
import com.dspot.declex.api.model.UseModel;
import com.dspot.declex.api.server.SerializeCondition;
import com.dspot.declex.api.server.ServerModel;
import com.dspot.declex.api.server.ServerRequest;
import com.dspot.declex.api.server.ServerRequest.RequestMethod;
import com.dspot.declex.api.server.ServerRequest.RequestType;
import com.dspot.declex.api.util.CastUtility;
import com.dspot.declex.api.util.FormatsUtils;
import com.dspot.declex.handler.BaseModelAndModelClassHandler;
import com.dspot.declex.model.UseModelHolder;
import com.dspot.declex.util.ParamUtils;
import com.dspot.declex.util.SharedRecords;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJType;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

import freemarker.template.SimpleSequence;

public class ServerModelHandler extends BaseModelAndModelClassHandler {

	private String response;
	private List<String> responseParams = new LinkedList<>();
	private String client;
	
	public ServerModelHandler(AndroidAnnotationsEnvironment environment) {
		super(ServerModel.class, environment, 
				"com/dspot/declex/server/", "ServerModel.ftl.java");
	}
	
	@Override
	protected void setTemplateDataModel(Map<String, Object> rootDataModel,
			Element element, BaseGeneratedClassHolder holder) {
		super.setTemplateDataModel(rootDataModel, element, holder);
		
		rootDataModel.put("responseParams", new SimpleSequence(responseParams));
		rootDataModel.put("client", client);
		rootDataModel.put("response", response);
		
		Map<String, String> fields = getSerializeConditionFields(
				(TypeElement) element, 
				getFields((TypeElement) element)
			);
		
		rootDataModel.put("avoidExceptions", element.getAnnotation(ServerModel.class).avoidExceptions());
		rootDataModel.put("serializeCondition", new SimpleSequence(fields.values()));
		rootDataModel.put("serializeConditionField", new SimpleSequence(fields.keySet()));
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		if (element.getKind().isField()) {
			Model annotated = element.getAnnotation(Model.class);
			if (annotated == null) {
		
				if (element.getEnclosingElement().getAnnotation(ServerModel.class) == null) {
					valid.addError("You can only apply this annotation in a field annotated with @Model or inside @ServerModel component");					
				}
				
			}
			
			return;
		}
		if (element instanceof ExecutableElement) return;
		
		super.validate(element, valid);
		
		validatorHelper.hasInternetPermission(getEnvironment().getAndroidManifest(), valid);
		
		ServerModel annotation = element.getAnnotation(ServerModel.class);
		if (!annotation.baseUrl().startsWith("http://") && !annotation.baseUrl().startsWith("https://")) {
			valid.addError("You should provide a valid URL, ej.: \"http://validurl.com/api\"");
			return;
		}
		
		validatorHelper.typeHasAnnotation(EBean.class, element, valid);
		validatorHelper.typeHasAnnotation(UseModel.class, element, valid);		
	}

	@Override
	public void process(Element element, BaseGeneratedClassHolder holder) {
		if (element.getKind().isField()) return;
		if (element instanceof ExecutableElement) return;
		
		response = "";
		responseParams.clear();
		client = "";
		
		final UseModelHolder useModelHolder = holder.getPluginHolder(new UseModelHolder(holder));
				
		ExecutableElement serverModelLoaded = useModelHolder.getAfterLoadMethod();
		if (serverModelLoaded != null 
			&& serverModelLoaded.getAnnotation(ServerModel.class) == null) {
			serverModelLoaded = null;
		}

		ExecutableElement serverModelPut = useModelHolder.getAfterPutMethod();
		if (serverModelPut != null 
			&& serverModelPut.getAnnotation(ServerModel.class) == null) {
			serverModelPut = null;
		}
		
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems) {
			if (elem.getAnnotation(ServerModel.class) == null) continue;
			
			if (elem.getKind() == ElementKind.FIELD) {
				if (elem.asType().toString().equals("okhttp3.OkHttpClient")) {
					client = elem.getSimpleName().toString();
				}
			}
			
			if (elem.getKind() == ElementKind.METHOD) {
				ExecutableElement executableElement = (ExecutableElement) elem;
				
				if (elem.getModifiers().contains(Modifier.STATIC)) {
					List<? extends VariableElement> params = executableElement.getParameters();
					response = elem.getSimpleName().toString();
					
					for (VariableElement param : params) {
						responseParams.add(param.getSimpleName().toString());
					}
				}
				
			}
		}
		
		super.process(element, holder);
		
		ServerModel annotation = element.getAnnotation(ServerModel.class);
		boolean loadExecuted = false;
		boolean putExecuted = false;
		
		if (!annotation.get().trim().equals("")) {
			processRequest(new GetRequest(annotation), 0, true, element, useModelHolder);
			loadExecuted = true;
		}
		
		int requestNumber = 1;
		for (ServerRequest request : annotation.load()) {
			if (!request.action().equals("NONE")) {
				processRequest(request, requestNumber, true, element, useModelHolder);
				loadExecuted = true;
			}
			
			requestNumber++;
		}

		
		if (!annotation.post().trim().equals("")) {
			processRequest(new PostRequest(annotation), 0, false, element, useModelHolder);
			putExecuted = true;
		}
				
		requestNumber = 1;
		for (ServerRequest request : annotation.put()) {
			if (!request.action().equals("NONE")) {
				processRequest(request, requestNumber, false, element, useModelHolder);
				putExecuted = true;
			}
			
			requestNumber++;
		}		
		
		JMethod getRequestMethod = holder.getGeneratedClass().getMethod(
				"getRequest", 
				new AbstractJType[] {getClasses().STRING, getClasses().STRING, holder.getGeneratedClass()}
			);
		if (getRequestMethod != null)
			getRequestMethod.body()._return(_null());
		
		JMethod processResponseMethod = holder.getGeneratedClass().getMethod(
				"processResponse", 
				new AbstractJType[] {getClasses().STRING, getClasses().STRING, getClasses().STRING, holder.getGeneratedClass()}
			);
		if (processResponseMethod != null) 
			processResponseMethod.body()._return(ref("json"));
		
		JMethod getMockMethod = holder.getGeneratedClass().getMethod(
				"getMock", 
				new AbstractJType[] {getClasses().STRING, getClasses().STRING, holder.getGeneratedClass()}
			);
		if (getMockMethod != null)
			getMockMethod.body()._return(_null());
		
		if (loadExecuted) {
			insertInGetModel(serverModelLoaded, element, useModelHolder);
			
			insertInSelectedGetModel(serverModelLoaded, element, useModelHolder);
			
			insertInGetModelList(serverModelLoaded, element, useModelHolder);
			
			insertInSelectedGetModelList(serverModelLoaded, element, useModelHolder);
		}
		
		if (putExecuted) {
			insertInPutModel(serverModelPut, element, useModelHolder);
		}
	}
	
	private void insertInPutModel(ExecutableElement serverModelPut, Element element, UseModelHolder holder) {
		JFieldRef query = ref("query");
		JFieldRef orderBy = ref("orderBy");
		
		//Write the putLocalDbModel in the generated putModels() method
		JBlock block = holder.getPutModelInitBlock();
			
		JBlock putServerModel = new JBlock();
		JBlock ifBlock = putServerModel._if(ref("result").ne(_null()))._then();
		
		ServerModel annotation = element.getAnnotation(ServerModel.class);
		if (!annotation.defaultQuery().equals("")) {
			ifBlock._if(query.invoke("equals").arg(""))._then()
		     	 .assign(query, FormatsUtils.expressionFromString(annotation.defaultQuery()));	
		}
		
		ifBlock.assign(ref("result"), _this().invoke("putServerModel").arg(query).arg(orderBy));
				
		if (serverModelPut != null) {
			ifBlock = ifBlock._if(ref("result").ne(_null()))._then();
	
			JInvocation invocation = ifBlock.invoke(serverModelPut.getSimpleName().toString());
			
			List<? extends VariableElement> parameters = serverModelPut.getParameters();
			for (VariableElement param : parameters) {
				final String paramName = param.getSimpleName().toString();
				ParamUtils.injectParam(paramName, invocation);
			}
		}
		
		SharedRecords.priorityAdd(
			block, 
			putServerModel, 
			100
		);
		
	}

	private void insertInSelectedGetModelList(ExecutableElement serverModelLoaded,
			Element element, UseModelHolder holder) {
		
		ServerModel annotation = element.getAnnotation(ServerModel.class);
		JFieldRef context = ref("context");
		JFieldRef query = ref("query");
		JFieldRef orderBy = ref("orderBy");
		JFieldRef useModel = ref("useModel");
		
		//Write the getServerModels in the generated getModelList() method inside the UseModel clause
		JBlock block = holder.getGetModelListUseBlock();
		block = block._if(useModel.invoke("contains").arg(dotclass(getJClass(ServerModel.class))))._then();
				
		if (!annotation.defaultQuery().equals("")) {
			block._if(query.invoke("equals").arg(""))._then()
		     	 .assign(query, FormatsUtils.expressionFromString(annotation.defaultQuery()));	
		}
		
		JFieldRef serverModels = ref("models");
		block.assign(serverModels,
				invoke("getServerModels").arg(context)
				                         .arg(query).arg(orderBy)
			);
		if (serverModelLoaded != null) {
			JBlock notNull = block._if(serverModels.ne(_null()).cand(serverModels.invoke("isEmpty").not()))._then();
			JInvocation invocation = notNull.invoke(serverModels.invoke("get").arg(lit(0)), serverModelLoaded.getSimpleName().toString());
			
			List<? extends VariableElement> parameters = serverModelLoaded.getParameters();
			for (VariableElement param : parameters) {
				final String paramName = param.getSimpleName().toString();
				
				if (paramName.equals("model")) {
					invocation.arg(_null());
					continue;
				}
				
				ParamUtils.injectParam(paramName, invocation);
			}
		}
		block._return(serverModels);
		
	}

	private void insertInGetModelList(ExecutableElement serverModelLoaded,
			Element element, UseModelHolder holder) {
		
		ServerModel annotation = element.getAnnotation(ServerModel.class);
		JFieldRef context = ref("context");
		JFieldRef query = ref("query");
		JFieldRef orderBy = ref("orderBy");
		
		//Write the getServerModels in the generated getModelList() method
		JBlock block = holder.getGetModelListBlock();
		
		if (!annotation.defaultQuery().equals("")) {
			block._if(query.invoke("equals").arg(""))._then()
		     	 .assign(query, FormatsUtils.expressionFromString(annotation.defaultQuery()));	
		}
		
		JFieldRef serverModels = ref("models");
		block.assign(serverModels, 
				invoke("getServerModels").arg(context)
				                         .arg(query).arg(orderBy)
			);
		JBlock notNull = block._if(serverModels.ne(_null()).cand(serverModels.invoke("isEmpty").not()))._then();
		if (serverModelLoaded != null) {
			JInvocation invocation = notNull.invoke(serverModels.invoke("get").arg(lit(0)), serverModelLoaded.getSimpleName().toString());
			
			List<? extends VariableElement> parameters = serverModelLoaded.getParameters();
			for (VariableElement param : parameters) {
				final String paramName = param.getSimpleName().toString();
				
				if (paramName.equals("model")) {
					invocation.arg(_null());
					continue;
				}
				
				ParamUtils.injectParam(paramName, invocation);
			}
		}
		notNull._return(serverModels);
		
	}

	private void insertInSelectedGetModel(ExecutableElement serverModelLoaded,
			Element element, UseModelHolder holder) {
		
		ServerModel annotation = element.getAnnotation(ServerModel.class);
		JFieldRef context = ref("context");
		JFieldRef query = ref("query");
		JFieldRef orderBy = ref("orderBy");
		JFieldRef useModel = ref("useModel");
		
		//Write the getServerModel in the generated getModel() method inside the UseModel clause
		JBlock block = holder.getGetModelUseBlock();
		block = block._if(useModel.invoke("contains").arg(dotclass(getJClass(ServerModel.class))))._then();
		
		if (!annotation.defaultQuery().equals("")) {
			block._if(query.invoke("equals").arg(""))._then()
		     	 .assign(query, FormatsUtils.expressionFromString(annotation.defaultQuery()));	
		}
		
		JFieldRef serverModel = ref("model");
		block.assign(serverModel, 
				invoke("getServerModel").arg(context)
				                        .arg(query).arg(orderBy)
			);
		JConditional cond = block._if(serverModel.ne(_null()));
		cond._else()._return(_new(holder.getGeneratedClass()).arg(context));
		JBlock notNull = cond._then();
		if (serverModelLoaded != null) {
			JInvocation invocation = notNull.invoke(serverModel, serverModelLoaded.getSimpleName().toString());
			
			List<? extends VariableElement> parameters = serverModelLoaded.getParameters();
			for (VariableElement param : parameters) {
				final String paramName = param.getSimpleName().toString();
				
				if (paramName.equals("models")) {
					invocation.arg(_null());
					continue;
				}
				
				ParamUtils.injectParam(paramName, invocation);
			}
			
		}
		notNull._return(serverModel);

		
	}

	private void insertInGetModel(ExecutableElement serverModelLoaded, Element element, UseModelHolder holder) {
		
		ServerModel annotation = element.getAnnotation(ServerModel.class);
		JFieldRef context = ref("context");
		JFieldRef query = ref("query");
		JFieldRef orderBy = ref("orderBy");
		
		//Write the getServerModel in the generated getModel() method
		JBlock block = holder.getGetModelBlock();
		
		if (!annotation.defaultQuery().equals("")) {
			block._if(query.invoke("equals").arg(""))._then()
		     	 .assign(query, FormatsUtils.expressionFromString(annotation.defaultQuery()));	
		}
		
		JFieldRef serverModel = ref("model");
		block.assign(serverModel, 
				invoke("getServerModel").arg(context)
				                        .arg(query)
				                        .arg(orderBy)
			);
		JBlock notNull = block._if(serverModel.ne(_null()))._then();
		if (serverModelLoaded != null) {
			JInvocation invocation = notNull.invoke(serverModel, serverModelLoaded.getSimpleName().toString());
			
			List<? extends VariableElement> parameters = serverModelLoaded.getParameters();
			for (VariableElement param : parameters) {
				final String paramName = param.getSimpleName().toString();
				
				if (paramName.equals("models")) {
					invocation.arg(_null());
					continue;
				}
				
				ParamUtils.injectParam(paramName, invocation);
			}			
		}
		notNull._return(serverModel);
	}
	
	private void processRequest(ServerRequest request, int requestNumber, boolean isLoad, Element element, UseModelHolder holder) {
	
		ServerModel annotation = element.getAnnotation(ServerModel.class);
		
		JMethod getRequestMethod = holder.getGeneratedClass().getMethod(
				"getRequest", 
				new AbstractJType[] {getClasses().STRING, getClasses().STRING, holder.getGeneratedClass()}
			);
		
		if (getRequestMethod == null) {
			getRequestMethod = holder.getGeneratedClass().method(JMod.PRIVATE | JMod.STATIC, getJClass("okhttp3.Request"), "getRequest");
			getRequestMethod.param(getClasses().STRING, "query");
			getRequestMethod.param(getClasses().STRING, "orderBy");
			getRequestMethod.param(holder.getGeneratedClass(), "inst");
			
			String baseUrlValue = annotation.baseUrl();
			if (baseUrlValue.endsWith("/")) baseUrlValue = baseUrlValue.substring(0, baseUrlValue.length()-1);
			getRequestMethod.body().decl(
				JMod.FINAL, 
				getClasses().STRING, 
				"baseUrl", 
				FormatsUtils.expressionFromString(baseUrlValue)
			);
		}
				
		JFieldRef json = ref("json");
		JFieldRef query = ref("query");
		JFieldRef orderBy = ref("orderBy");
		JFieldRef inst = ref("inst");
		
		Map<String, String> clsFields = getFields((TypeElement) element);
		
		JBlock newBlock = getRequestMethod.body().block();		
		IJExpression check = orderBy.invoke("equals").arg(String.valueOf(requestNumber));
		
		if (requestNumber == 0)
			check = check.cor(orderBy.invoke("equals").arg(""));
		
		if (!request.name().equals("") && !request.name().equals(String.valueOf(requestNumber))) 
			check = check.cor(orderBy.invoke("equals").arg(request.name()));
		
		check = check.cand(isLoad ? inst.eq(_null()) : inst.ne(_null()));
		newBlock = newBlock._if(check)._then();
		
		JVar requestBuilder = newBlock.decl(getJClass("okhttp3.Request.Builder"), "request", _new(getJClass("okhttp3.Request.Builder")));
		
		String action = request.action();
		if (!action.startsWith("/")) action = "/" + action;
		
		Matcher formatSyntaxMatcher = Pattern.compile(FormatsUtils.FORMAT_SYNTAX_REGX).matcher(action);
		while (formatSyntaxMatcher.find()) {
			String match = formatSyntaxMatcher.group(0);
			
			for (String clsField : clsFields.keySet()) {
				match = match.replaceAll("(?<!\\w)(this\\.)*"+clsField+"(?!\\w)", "inst." + clsField);
			}
			
			action = action.replace(formatSyntaxMatcher.group(0), match);
		}	

		newBlock.invoke(requestBuilder, "url")
		        .arg(ref("baseUrl")
        		.plus(FormatsUtils.expressionFromString(action)));	
		
		String[] headers = request.headers();
		if (headers.length == 1 && headers[0].equals("")) {
			if (isLoad) {
				headers = annotation.getHeaders();
				
				if (headers.length == 1 && headers[0].equals("")) {
					headers = annotation.postHeaders();
				}
			}
			else {
				headers = annotation.postHeaders();
				
				if (headers.length == 1 && headers[0].equals("")) {
					headers = annotation.getHeaders();
				}
			}
		}
		
		for (String header : headers) {
			if (!header.equals("")) {
				String key = header.substring(0, header.indexOf('='));
				String value = header.substring(header.indexOf('=')+1);
				
				formatSyntaxMatcher = Pattern.compile(FormatsUtils.FORMAT_SYNTAX_REGX).matcher(value);
				while (formatSyntaxMatcher.find()) {
					String match = formatSyntaxMatcher.group(0);
					
					for (String clsField : clsFields.keySet()) {
						match = match.replaceAll("(?<!\\w)(this\\.)*"+clsField+"(?!\\w)", "inst." + clsField);
					}
					
					value = value.replace(formatSyntaxMatcher.group(0), match);
				}
				
				newBlock.invoke(requestBuilder, "addHeader")
				        .arg(key)
				        .arg(FormatsUtils.expressionFromString(value));
			}
		}
		
		RequestMethod requestMethod = request.method();
		if (requestMethod.equals(RequestMethod.Default)) {
			if (isLoad) requestMethod = RequestMethod.Get;
			else requestMethod = RequestMethod.Post;
		}
		
		String method = null;
		switch (requestMethod) {
		case Get:
		case Head:
			break;
			
		case Post:
			method = "post";
			break;
			
		case Delete:
			method = "delete";
			break;
			
		case Patch:
			method = "patch";
			break;
			
		case Put:
			method = "put";
			break;
			
		default:
			break;
			
		}
		
		switch (requestMethod) {
		case Get:
		case Head:
			break;
			
		case Post:
		case Put:
		case Patch:
		case Delete:
			RequestType requestType = request.type();
			if (!request.fields()[0].equals("")) requestType = RequestType.Fields;
			
			switch (requestType) {
				case Body: 
					{
						IJExpression createExp = getJClass("okhttp3.RequestBody").staticInvoke("create")
								.arg(getJClass("okhttp3.MediaType").staticInvoke("parse").arg("application/json"))
								.arg(inst.invoke("toJson"));
						JVar requestBody = newBlock.decl(getJClass("okhttp3.RequestBody"), "requestBody", createExp);
						newBlock.invoke(requestBuilder, method).arg(requestBody);
					}
					
					break;
					
				case Fields:
					{
						JVar formBody = newBlock.decl(
							getJClass("okhttp3.FormBody.Builder"), 
							"formBody",
							_new(getJClass("okhttp3.FormBody.Builder"))
						);
						
						if (!request.fields()[0].equals("")) {
							
							for (String field : request.fields()) {
								
								Matcher matcher = Pattern.compile("\\A\\s*(\\w+)\\s*=").matcher(field);
								if (matcher.find()) {
									String assignment = field.substring(matcher.group(0).length());
									String param = matcher.group(1);
									
									formatSyntaxMatcher = Pattern.compile(FormatsUtils.FORMAT_SYNTAX_REGX).matcher(assignment);
									while (formatSyntaxMatcher.find()) {
										String match = formatSyntaxMatcher.group(0);
										
										for (String clsField : clsFields.keySet()) {
											match = match.replaceAll("(?<!\\w)(this\\.)*"+clsField+"(?!\\w)", "inst." + clsField);
										}
										
										assignment = assignment.replace(formatSyntaxMatcher.group(0), match);
									}	
									
									newBlock.assign(
										formBody, 
										formBody.invoke("add")
										        .arg(param)
										        .arg(FormatsUtils.expressionFromString(assignment))
									);									
								}
							}
							
						} else {
							JVar fields = newBlock.decl(getJClass(Map.class).narrow(String.class, String.class), "fields", inst.invoke("getAllFields"));
							newBlock.forEach(getClasses().STRING, "field", fields.invoke("keySet")).body()
							        .assign(
						        		formBody, 
						        		formBody.invoke("add").arg(ref("field"))
						        		        .arg(fields.invoke("get").arg(ref("field")))
					        		);
						}
					
						newBlock.invoke(requestBuilder, method).arg(formBody.invoke("build"));
					}
					break;
			}
			break;
			
		default:
			break;
		}
		
		newBlock._return(requestBuilder.invoke("build"));
		
		
		JMethod processResponseMethod = holder.getGeneratedClass().getMethod(
				"processResponse", 
				new AbstractJType[] {getClasses().STRING, getClasses().STRING, getClasses().STRING, holder.getGeneratedClass()}
			);
		if (processResponseMethod == null) {
			processResponseMethod = holder.getGeneratedClass().method(JMod.PRIVATE | JMod.STATIC, getClasses().STRING, "processResponse");
			processResponseMethod.param(getClasses().STRING, "json");
			processResponseMethod.param(getClasses().STRING, "query");
			processResponseMethod.param(getClasses().STRING, "orderBy");
			processResponseMethod.param(holder.getGeneratedClass(), "inst");
			
			newBlock = processResponseMethod.body();
		}

		if (!request.model().equals("") && !isLoad) {
			newBlock = processResponseMethod.body().block();
			check = orderBy.invoke("equals").arg(String.valueOf(requestNumber));
			
			if (requestNumber == 0)
				check = check.cor(orderBy.invoke("equals").arg(""));

			if (!request.name().equals("") && !request.name().equals(String.valueOf(requestNumber))) 
				check = check.cor(orderBy.invoke("equals").arg(request.name()));
			
			check = check.cand(isLoad ? inst.eq(_null()) : inst.ne(_null()));
			
			newBlock = newBlock._if(check)._then();
			
			JVar elem = newBlock.decl(
					getJClass("com.google.gson.JsonElement"), 
					"elem",
					_new(getJClass("com.google.gson.JsonParser")).invoke("parse").arg(json)
				);
			JConditional ifConditional = newBlock._if(elem.invoke("isJsonArray"));
			JBlock thenBlock = ifConditional._then();						

			if (request.model().equals("this")) {
				thenBlock._if(elem.invoke("getAsJsonArray").invoke("size").eq(lit(0)))._then()._return(_null());
				thenBlock.assign(elem, elem.invoke("getAsJsonArray").invoke("get").arg(lit(0)));
				newBlock._if(elem.invoke("isJsonObject").not())._then()._return(_null());

				newBlock.staticInvoke(getJClass(CastUtility.class), "copy")
				        .arg(
			        		invoke("getGson")
			        		.invoke("fromJson")
			        		.arg(ref("elem"))
			        		.arg(JExpr.dotclass(holder.getGeneratedClass()))
        				).arg(inst);
			} else {
				
				String clsName = clsFields.get(request.model());		
				final boolean modelIsList = TypeUtils.isSubtype(clsName, CanonicalNameConstants.LIST, getProcessingEnvironment()); 
				if (modelIsList) {
					
					JVar gson = thenBlock.decl(
							getJClass("com.google.gson.Gson"), 
							"gson",
							_new(getJClass("com.google.gson.GsonBuilder"))
							       .invoke("excludeFieldsWithModifiers")
							       .arg(getJClass(java.lang.reflect.Modifier.class).staticRef("FINAL"))
							       .arg(getJClass(java.lang.reflect.Modifier.class).staticRef("STATIC"))
							       .invoke("create")
					       );
					
					String dclr = "java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<" 
					               + clsName + ">(){}.getType();";
					
					thenBlock.directStatement(dclr);
					thenBlock.assign(
							inst.ref(request.model()), 
							gson.invoke("fromJson")
							     .arg(ref("elem")).arg(ref("listType"))
						);
					
					ifConditional._else()._return(_null());
					
				} else {
					newBlock._if(elem.invoke("isJsonObject").not())._then()._return(_null());
					thenBlock._if(elem.invoke("getAsJsonArray").invoke("size").eq(lit(0)))._then()._return(_null());
					thenBlock.assign(elem, elem.invoke("getAsJsonArray").invoke("get").arg(lit(0)));

					JVar gson = newBlock.decl(
							getJClass("com.google.gson.Gson"), 
							"gson",
							_new(getJClass("com.google.gson.GsonBuilder"))
							       .invoke("excludeFieldsWithModifiers")
							       .arg(getJClass(java.lang.reflect.Modifier.class).staticRef("FINAL"))
							       .arg(getJClass(java.lang.reflect.Modifier.class).staticRef("STATIC"))
							       .invoke("create")
					       );

					newBlock.assign(
							inst.ref(request.model()), 
							gson.invoke("fromJson")
							     .arg(ref("elem")).arg(getJClass(clsName).dotclass())
						);
				}
								
//				if (!request.modelClass().equals(Object.class)) {
//					newBlock.assign(
//							ref(request.model()), 
//							_new(getJClass("com.google.gson.Gson")).invoke("fromJson")
//							     .arg(ref("elem")).arg(JExpr.dotclass(getJClass(request.modelClass())))
//						);
//				} else {
//					newBlock.assign(
//						ref(request.model()), 
//						_new(getJClass("com.google.gson.Gson")).invoke("fromJson")
//						     .arg(ref("elem")).arg(ref(request.model()).invoke("getClass"))
//					);
//				}
			}
		}

		
		JMethod getMockMethod = holder.getGeneratedClass().getMethod(
				"getMock", 
				new AbstractJType[] {getClasses().STRING, getClasses().STRING, holder.getGeneratedClass()}
			);
		if (getMockMethod == null) {
			getMockMethod = holder.getGeneratedClass().method(JMod.PRIVATE | JMod.STATIC, getClasses().STRING, "getMock");
			getMockMethod.param(getClasses().STRING, "query");
			getMockMethod.param(getClasses().STRING, "orderBy");
			getMockMethod.param(holder.getGeneratedClass(), "inst");
		}
		
		if (request.mock() || annotation.mock()) {
			newBlock = getMockMethod.body().block();
			check = orderBy.invoke("equals").arg(String.valueOf(requestNumber));
			
			if (requestNumber == 0)
				check = check.cor(orderBy.invoke("equals").arg(""));

			if (!request.name().equals("") && !request.name().equals(String.valueOf(requestNumber))) 
				check = check.cor(orderBy.invoke("equals").arg(request.name()));
			
			check = check.cand(isLoad ? inst.eq(_null()) : inst.ne(_null()));
			
			newBlock = newBlock._if(check)._then();
			
			String mockResult = request.mockResult();
			
			formatSyntaxMatcher = Pattern.compile(FormatsUtils.FORMAT_SYNTAX_REGX).matcher(mockResult);
			while (formatSyntaxMatcher.find()) {
				String match = formatSyntaxMatcher.group(0);
				
				for (String clsField : clsFields.keySet()) {
					match = match.replaceAll("(?<!\\w)(this\\.)*"+clsField+"(?!\\w)", "inst." + clsField);
				}
				
				mockResult = mockResult.replace(formatSyntaxMatcher.group(0), match);
			}	

			
			newBlock._return(FormatsUtils.expressionFromString(mockResult));
		}
		
	}
	
	private Map<String, String> getFields(TypeElement element) {
		Map<String, String> fields = new HashMap<>();
		
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems) {
			final String elemName = elem.getSimpleName().toString();
			final String elemType = elem.asType().toString();
			
			if (elem.getModifiers().contains(Modifier.STATIC)) continue;
			
			if (elem.getKind() == ElementKind.FIELD) {
				if (elem.getModifiers().contains(Modifier.PRIVATE)) continue;
				
				fields.put(elemName, TypeUtils.typeFromTypeString(elemType, getEnvironment()));
			}			
		}
		
		//Apply to Extensions
		List<? extends TypeMirror> superTypes = getProcessingEnvironment().getTypeUtils().directSupertypes(element.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = getProcessingEnvironment().getElementUtils().getTypeElement(type.toString());
			
			if (superElement.getAnnotation(Extension.class) != null) {
				fields.putAll(getFields(superElement));
			}
			
			break;
		}
		
		return fields;
	}

	private Map<String, String> getSerializeConditionFields(TypeElement element, Map<String, String> allFields) {
		Map<String, String> fields = new HashMap<>();
		
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems) {
			final String elemName = elem.getSimpleName().toString();
			
			if (elem.getModifiers().contains(Modifier.STATIC)) continue;
			
			if (elem.getKind() == ElementKind.FIELD) {
				if (elem.getModifiers().contains(Modifier.PRIVATE)) continue;
				
				SerializeCondition serializeCondition = elem.getAnnotation(SerializeCondition.class);
				if (serializeCondition == null) continue;
				
				String cond = "!(" + serializeCondition.value() + ")";
				for (String clsField : allFields.keySet()) {
					String prevCond = new String(cond);
					cond = cond.replaceAll("(?<!\\w)(this\\.)*"+clsField+"(?!\\w)", "inst." + clsField);
					
					if (!cond.equals(prevCond) && !cond.startsWith("inst != null")) {
						cond = "inst != null && " + cond;
					}
				}
				
				fields.put(elemName, cond);
			}			
		}
		
		//Apply to Extensions
		List<? extends TypeMirror> superTypes = getProcessingEnvironment().getTypeUtils().directSupertypes(element.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = getProcessingEnvironment().getElementUtils().getTypeElement(type.toString());
			
			if (superElement.getAnnotation(Extension.class) != null) {
				fields.putAll(getSerializeConditionFields(superElement, allFields));
			}
			
			break;
		}
		
		return fields;
	}
	
	@Override
	protected Class<?> getDefaultModelClass() {
		return Object.class;
	}
	
	
	private static class GetRequest implements ServerRequest {

		protected ServerModel annotation;
		
		public GetRequest(ServerModel annotation) {
			this.annotation = annotation;
		}
		
		@Override
		public Class<? extends Annotation> annotationType() {
			return ServerRequest.class;
		}

		@Override
		public String name() {
			return "0";
		}

		@Override
		public RequestMethod method() {
			return RequestMethod.Default;
		}

		@Override
		public RequestType type() {
			return annotation.postType();
		}

		@Override
		public String action() {
			return annotation.get();
		}

		@Override
		public String[] headers() {
			return annotation.getHeaders();
		}

		@Override
		public String[] fields() {
			return annotation.postFields();
		}

		@Override
		public String model() {
			return annotation.model();
		}

		@Override
		public Class<?> modelClass() {
			return annotation.modelClass();
		}

		@Override
		public boolean mock() {
			return annotation.mock();
		}

		@Override
		public String mockResult() {
			return annotation.mockResult();
		}
		
	}
	
	private static class PostRequest extends GetRequest {
		
		public PostRequest(ServerModel annotation) {
			super(annotation);
		}

		@Override
		public String action() {
			return annotation.post();
		}

		@Override
		public String[] headers() {
			return annotation.postHeaders();
		}
	}
}
