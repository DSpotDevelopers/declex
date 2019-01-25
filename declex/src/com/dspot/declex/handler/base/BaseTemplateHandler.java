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
package com.dspot.declex.handler.base;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.EApplication;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.EService;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.BaseGeneratedClassHolder;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;

import com.dspot.declex.transform.ClassFieldsTransform;
import com.dspot.declex.transform.ClassFooterTransform;
import com.dspot.declex.transform.ClassHeadTransform;
import com.dspot.declex.transform.HolderMethodTransform;
import com.dspot.declex.util.FileUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;

public abstract class BaseTemplateHandler<T extends BaseGeneratedClassHolder> extends BaseAnnotationHandler<T> {
	
	protected static final Logger LOGGER = LoggerFactory.getLogger(BaseTemplateHandler.class);
	
	private String templatePath;
	private String templateName;
	
	private List<String> alreadyWritedTemplates = new ArrayList<>();
	
	protected Class<? extends Annotation> targetAnnotation;
	
	protected Map<T, Set<String>> processedHolders = new HashMap<>();
	
	public BaseTemplateHandler(Class<?> targetClass, AndroidAnnotationsEnvironment environment, 
			String templatePath, String templateName) {
		super(targetClass, environment);
		
		this.targetAnnotation = (Class<? extends Annotation>) targetClass;
		
		this.templatePath = templatePath;
		this.templateName = templateName;
	}
	
	protected void setTemplateDataModel(Map<String, Object> rootDataModel, Element element, T holder) {
		rootDataModel.put("className", holder.getGeneratedClass().name());
		rootDataModel.put("fromClass", element.getSimpleName().toString());
		
		rootDataModel.put("classNameFull", holder.getGeneratedClass().fullName());
		rootDataModel.put("fromClassFull", element.asType().toString());
        
        if (element.getKind() == ElementKind.CLASS) {
        	if (adiHelper.getAnnotation(element, EActivity.class) != null) {
        		rootDataModel.put("classType", "Activity");
        	} else if (adiHelper.getAnnotation(element, EFragment.class) != null) {
        		rootDataModel.put("classType", "Fragment");
        	} else if (adiHelper.getAnnotation(element, EBean.class) != null) {
        		rootDataModel.put("classType", "Bean");
        	} if (adiHelper.getAnnotation(element, EApplication.class) != null) {
        		rootDataModel.put("classType", "Application");
        	} if (adiHelper.getAnnotation(element, EService.class) != null) {
        		rootDataModel.put("classType", "Service");
        	}
        }
        
        rootDataModel.put("packageName", getEnvironment().getAndroidManifest().getApplicationPackage());
        
        //Transforms
        rootDataModel.put("holder_method", new HolderMethodTransform<T>(holder));
        rootDataModel.put("class_head", new ClassHeadTransform<T>(holder));
        rootDataModel.put("class_fields", new ClassFieldsTransform<T>(holder));
        rootDataModel.put("class_footer", new ClassFooterTransform<T>(holder));
	}
	
	@Override
	public void process(Element element, T holder) {
		
		//This permits to process efficiently abstract classes
		Set<String> targets = processedHolders.get(holder);
		if (targets == null) {
			targets = new HashSet<>();
			processedHolders.put(holder, targets);
		}
		
		if (targets.contains(getTarget())) return;
		targets.add(getTarget());
		
		Boolean isCustom = false;
		try {
			Method customMethod = targetAnnotation.getMethod("custom", new Class[] {});
			isCustom = (Boolean) customMethod.invoke(adiHelper.getAnnotation(element, targetAnnotation), new Object[] {});
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | 
				 IllegalArgumentException | InvocationTargetException e1) {
			LOGGER.warn(
				"Annotation {} implementation does not includes a \"custom\" method", 
				element, 
				element.getAnnotation(targetAnnotation)
			);
		} 
		
		
		int retries = 0;
		while (true) { //On File not found exception, try several times
			try {
				String ftlFileName = templateName;
				String ftlFile = templatePath + ftlFileName;
	
				LOGGER.info("Starting FTL proccess on {}", ftlFile);
				
				//Get the file from the package
				URL url = getClass().getClassLoader().getResource(ftlFile);
				if (url == null) {
					throw new IllegalStateException(ftlFile + " not found, execute ant on the project to generate it");
				}
				
				String outputDirPath = getProcessingEnvironment().getOptions().get("ftl_source_path");
				if (outputDirPath == null) outputDirPath = "ftl";
	
				//Ensure the route to the file be created
				File outputFtl = FileUtils.getConfigFile(outputDirPath, getProcessingEnvironment());
				if (!outputFtl.exists()) outputFtl.mkdir();
				
				if (isCustom) {
					String classTypeName = element.asType().toString();
					String customPath = classTypeName.substring(0, classTypeName.length() - element.getSimpleName().length() - 1);
					
					String[] ftlFileSegments = customPath.split("\\.");
					int i = 0;
					while (i < ftlFileSegments.length) {
						outputFtl = new File(outputFtl.getAbsolutePath() + File.separator + ftlFileSegments[i]);
						if (!outputFtl.exists()) outputFtl.mkdir();
						i++;
					}
					
					ftlFileName = element.getSimpleName() + "_" + ftlFileName;
				} else {
					String[] ftlFileSegments = templatePath.split("/");
					int i = 0;
					while (i < ftlFileSegments.length) {
						outputFtl = new File(outputFtl.getAbsolutePath() + File.separator + ftlFileSegments[i]);
						if (!outputFtl.exists()) outputFtl.mkdir();
						i++;
					}
				}
				
				boolean writeFtlFile = true;
				String outFileName = outputFtl.getAbsolutePath() + File.separator + ftlFileName;
				File outFile = new File(outFileName);
				if (outFile.exists()) {
					if (isCustom) writeFtlFile = false;
					else {
						if (alreadyWritedTemplates.contains(outFileName)) {
							writeFtlFile = false;
						} else {
							outFile.delete();
						}
					}
				}
				
				if (writeFtlFile) {
					//Write the file to FTL output folder
					OutputStream out = new FileOutputStream(outFile);
					InputStream in = url.openStream();
					int b = in.read();
					while (b != -1) {
						out.write(b);
						b = in.read();
					}
					out.close();
					in.close();
					
					alreadyWritedTemplates.add(outFileName);
					LOGGER.info("FTL writed to {}", outFileName);
				}
				
				//Adjust the FTL configuration 
		        Configuration cfg = Configuration.getDefaultConfiguration();
		        cfg.setDirectoryForTemplateLoading(outputFtl);
	
		        //Create a template 
		        Template temp = cfg.getTemplate(ftlFileName);
	
		        //Create a data model 
		        Map<String, Object> root = new HashMap<String, Object>();
		        setTemplateDataModel(root, element, holder);
		        
		        //Merge data model with template 
		        StringWriter writer = new StringWriter();
		        temp.process(root, writer);
		        writer.flush();
		        
		        //Write the template output directly to the Generated Class
		        holder.getGeneratedClass().direct(writer.toString());
		        
		        break;
			} catch(FileNotFoundException e) {
				LOGGER.warn(
					"FTL Write Error, retry " + (++retries) + ".", 
					element, 
					element.getAnnotation(targetAnnotation)
				);
				
				if (retries > 5) {
					LOGGER.error(
						"FTL PROCESSING ERROR", 
						element, 
						element.getAnnotation(targetAnnotation)
					);
				}
				continue;
			} catch (Throwable e) {
				LOGGER.error(
					"FTL PROCESSING ERROR", 
					e
				);
				
				break;
			}
		}
	}
	
}
