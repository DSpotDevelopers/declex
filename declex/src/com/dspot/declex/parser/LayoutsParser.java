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
package com.dspot.declex.parser;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.logger.Logger;
import org.androidannotations.rclass.IRClass.Res;
import org.androidannotations.rclass.IRInnerClass;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.dspot.declex.util.FileUtils;
import com.dspot.declex.util.TypeUtils;

public class LayoutsParser {
	
	private Logger LOGGER;
	
	private Map<String, Map<String, LayoutObject>> layoutMaps = new HashMap<>();

	private List<File> layoutFolders = new LinkedList<File>();
	
	private ProcessingEnvironment processingEnv;
	private AndroidAnnotationsEnvironment environment;
	
	private static LayoutsParser instance;
	
	public static LayoutsParser getInstance() {
		return instance;
	}
	
	public LayoutsParser(AndroidAnnotationsEnvironment environment, Logger logger) {
		this.LOGGER = logger;
		this.processingEnv = environment.getProcessingEnvironment();
		this.environment = environment;

		File resFolderFile = FileUtils.getResFolder(processingEnv);
		if (resFolderFile.exists()) {
			LOGGER.info("Layout Parsing in: " + resFolderFile.getAbsolutePath());
			
			for (File file : resFolderFile.listFiles()) {
				if (file.isDirectory() && (file.getName().equals("layout") || file.getName().startsWith("layout-"))) {
					layoutFolders.add(file);
				}
			}
			
			LOGGER.info("Layout Folders Found: " + layoutFolders);			
		} else {
			LOGGER.info("Layout Folders Not Found");
		}
			
		
		LayoutsParser.instance = this;
	}
	
	public Map<String, LayoutObject> getLayoutObjects(String layoutName) {
		return getLayoutObjects(layoutName, null);
	}
	
	private Map<String, LayoutObject> getLayoutObjects(String layoutName, String layoutId) {
		Map<String, LayoutObject> layoutObjects = layoutMaps.get(layoutName);
		
		if (layoutObjects == null) {
			for (File layout : layoutFolders) {
				for (File file : layout.listFiles()) {
					if (file.isFile() && file.getName().equals(layoutName + ".xml")) {
						
						layoutObjects = parseLayout(file, layoutId);
						
						if (layoutMaps.containsKey(layoutName)) {
							//Merge layouts
							layoutMaps.get(layoutName).putAll(layoutObjects);
						} else {						
							layoutMaps.put(layoutName, layoutObjects);
						}
						
					}
				}
			}
		}
		
		return layoutObjects;
	}
	
	private void searchInNode(Element node, Map<String, LayoutObject> foundObjects, String layoutId) {
		//documentElement.normalize();
		
		final String[] packages = {
				"android.widget.", "android.view.", "android.webkit."
		};
				
		//Navigate in the <include> tag
		if (node.getTagName().equals("include")) {
			if (node.hasAttribute("layout")) {

				String id = null;
				if (node.hasAttribute("android:id")) {
					id = node.getAttribute("android:id");
					id = id.substring(id.lastIndexOf('/') + 1);
				}
				
				String layoutName = node.getAttribute("layout");
				if (layoutName == null || "".equals(layoutName)) {
					layoutName = node.getAttribute("android:layout");
				}
				
				layoutName = layoutName.substring(layoutName.lastIndexOf('/') + 1);
				
				foundObjects.putAll(getLayoutObjects(layoutName, id));
			}
			return;
		}
		
		if (node.hasAttribute("android:id") || layoutId != null) {
			
			String id;
			if (layoutId != null)
				id = layoutId;
			else {
				id = node.getAttribute("android:id");
				id = id.substring(id.lastIndexOf('/') + 1);				
			}
			
			//Support for "fragment" tag
			if (node.getTagName().equals("fragment")) {
				if (node.hasAttribute("android:name")) {
					String fragmentClassName = node.getAttribute("android:name");
					if (containsField(id, Res.ID))	{
						foundObjects.put(
								id,
								new LayoutObject(fragmentClassName, node)
						);
					}
				}
				
				//If this fragment will load a specific layout, and it is reflected
				//in the tools:layout, load all the components of that layout too.
				if (node.hasAttribute("tools:layout")) {					
					String layoutName = node.getAttribute("tools:layout");
					layoutName = layoutName.substring(layoutName.lastIndexOf('/') + 1);
					
					foundObjects.putAll(getLayoutObjects(layoutName, null));
				}
				
				return;
			}
			
			String className = node.getTagName();
			
			//Find canonical name of the class if needed
			if (!className.contains(".")) {
				for (String pkg : packages) {
					String testName = pkg + className;
					TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(testName);
					if (typeElement != null) {
						className = testName;
						break;
					}
				}
			}
			
			if (containsField(id, Res.ID))	{
				LayoutObject layoutObject = new LayoutObject(className, node);
				foundObjects.put(id, layoutObject);
				
				//Support for new NavigationView
				if (TypeUtils.isSubtype(className, "android.support.design.widget.NavigationView", processingEnv)) {
					if (node.hasAttribute("app:headerLayout")) {
						String layoutName = node.getAttribute("app:headerLayout");
						layoutName = layoutName.substring(layoutName.lastIndexOf('/') + 1);
				
						Map<String, LayoutObject> headerLayoutObjects = getLayoutObjects(layoutName);
						
						for (LayoutObject headerLayoutObject : headerLayoutObjects.values()) {
							headerLayoutObject.holderId = id;
						}
						
						foundObjects.putAll(headerLayoutObjects);
					}
				}
				
				if (TypeUtils.isSubtype(className, "android.support.design.widget.NavigationView", processingEnv)) {
					if (node.hasAttribute("app:headerLayout")) {
						String layoutName = node.getAttribute("app:headerLayout");
						layoutName = layoutName.substring(layoutName.lastIndexOf('/') + 1);
				
						Map<String, LayoutObject> headerLayoutObjects = getLayoutObjects(layoutName);
						
						for (LayoutObject headerLayoutObject : headerLayoutObjects.values()) {
							headerLayoutObject.holderId = id;
						}
						
						foundObjects.putAll(headerLayoutObjects);
					}
				}
			}
		}		
		
		NodeList nodes = node.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) 
			if (nodes.item(i) instanceof Element){
			searchInNode((Element)nodes.item(i), foundObjects, null);
		}
	}
	
	private Map<String, LayoutObject> parseLayout(File xmlLayoutFile, String layoutId) {
		LOGGER.info("Layout Parsing: " + xmlLayoutFile.getName());
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

		Document doc;
		try {
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			doc = docBuilder.parse(xmlLayoutFile);
		} catch (Exception e) {
			LOGGER.error("Could not parse Layout file at path {}", xmlLayoutFile.getName(), e);
			return new HashMap<>();
		}

		Map<String, LayoutObject> foundObjects = new TreeMap<>();
		Element documentElement = doc.getDocumentElement();
		
		searchInNode(documentElement, foundObjects, layoutId);
		
		LOGGER.info("Layout Parsing Found: " + foundObjects);
		
		return foundObjects;
	}
	
	private boolean containsField(String name, Res res) {
		IRInnerClass rInnerClass = environment.getRClass().get(res);
		return rInnerClass.containsField(name);
	}
	
	public static class LayoutObject {
		public String className;
		public Element domElement;
		
		public String holderId; //Used by NavigationView
		
		public LayoutObject(String className, Element domElement) {
			this.className = className;
			this.domElement = domElement;
		}
		
		@Override
		public String toString() {
			return className;
		}
	}
}
