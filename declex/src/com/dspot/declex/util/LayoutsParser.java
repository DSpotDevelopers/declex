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
package com.dspot.declex.util;

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

import org.androidannotations.helper.IdAnnotationHelper;
import org.androidannotations.logger.Logger;
import org.androidannotations.rclass.IRClass.Res;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class LayoutsParser {
	private Logger LOGGER;
	
	private Map<String, Map<String, String>> layoutMaps = new HashMap<String, Map<String, String>>();
	private Map<String, Map<String, Element>> layoutNodes = new HashMap<String, Map<String, Element>>();

	private List<File> layoutFolders = new LinkedList<File>();
	
	private ProcessingEnvironment processingEnv;
	
	private static LayoutsParser instance;
	
	public static LayoutsParser getInstance() {
		return instance;
	}
	
	public LayoutsParser(ProcessingEnvironment processingEnv, Logger logger) {
		this.LOGGER = logger;
		this.processingEnv = processingEnv;
		
		File resFolderFile = FileUtils.getResFolder(processingEnv);
		LOGGER.info("Layout Parsing in: " + resFolderFile.getAbsolutePath());
		
		for (File file : resFolderFile.listFiles()) {
			if (file.isDirectory() && (file.getName().equals("layout") || file.getName().startsWith("layout-"))) {
				layoutFolders.add(file);
			}
		}
		
		LOGGER.info("Layout Folders found: " + layoutFolders);
		
		LayoutsParser.instance = this;
	}
	
	public Map<String, String> getLayoutObjects(String layoutName, IdAnnotationHelper idHelper) {
		return getLayoutObjects(layoutName, idHelper, null);
	}
	
	private Map<String, String> getLayoutObjects(String layoutName, IdAnnotationHelper idHelper, String layoutId) {
		Map<String, String> layoutObjects = layoutMaps.get(layoutName);
		
		if (layoutObjects == null) {
			for (File layout : layoutFolders) {
				for (File file : layout.listFiles()) {
					if (file.isFile() && file.getName().equals(layoutName + ".xml")) {
						Map<String, Element> layoutNode = new TreeMap<String, Element>();
						
						layoutObjects = parse(file, idHelper, layoutNode, layoutId);
						
						layoutMaps.put(layoutName, layoutObjects);
						layoutNodes.put(layoutName, layoutNode);
						break;
					}
				}
			}
		}
		
		return layoutObjects;
	}
	
	public Map<String, Element> getLayoutNodes(String layoutName) {
		return layoutNodes.get(layoutName);
	}
	
	private void searchInNode(Element node, IdAnnotationHelper idHelper, Map<String, String> foundObjects, Map<String, Element> layoutNode, String layoutId) {
		//documentElement.normalize();
		
		final String[] packages = {
				"android.widget.", "android.view.", "android.webkit."
		};
		
		//Support for new NavigationView
		if (node.getTagName().equals("android.support.design.widget.NavigationView")) {
			if (node.hasAttribute("app:headerLayout")) {
				String layoutName = node.getAttribute("app:headerLayout");
				layoutName = layoutName.substring(layoutName.lastIndexOf('/') + 1);
				
				foundObjects.putAll(getLayoutObjects(layoutName, idHelper));
				layoutNode.putAll(getLayoutNodes(layoutName));
			}
		}
		
		//Navigate in the <include> tag
		if (node.getTagName().equals("include")) {
			if (node.hasAttribute("layout")) {

				String id = null;
				if (node.hasAttribute("android:id")) {
					id = node.getAttribute("android:id");
					id = id.substring(id.lastIndexOf('/') + 1);
				}
				
				String layoutName = node.getAttribute("layout");
				layoutName = layoutName.substring(layoutName.lastIndexOf('/') + 1);
				
				foundObjects.putAll(getLayoutObjects(layoutName, idHelper, id));
				layoutNode.putAll(getLayoutNodes(layoutName));
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
			
			if (node.getTagName().equals("fragment")) {
				if (node.hasAttribute("android:name")) {
					String fragmentClassName = node.getAttribute("android:name");
					if (idHelper.containsField(id, Res.ID))	{
						foundObjects.put(id, fragmentClassName);
						layoutNode.put(id, node);
					}
				}
				
				//If this fragment will load a specific layout, and it is reflected
				//in the tools:layout, load all the components of that layout too.
				if (node.hasAttribute("tools:layout")) {					
					String layoutName = node.getAttribute("tools:layout");
					layoutName = layoutName.substring(layoutName.lastIndexOf('/') + 1);
					
					foundObjects.putAll(getLayoutObjects(layoutName, idHelper, null));
					layoutNode.putAll(getLayoutNodes(layoutName));
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
			
			if (idHelper.containsField(id, Res.ID))	{
				foundObjects.put(id, className);
				layoutNode.put(id, node);
			}
		}		
		
		NodeList nodes = node.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) 
			if (nodes.item(i) instanceof Element){
			searchInNode((Element)nodes.item(i), idHelper, foundObjects, layoutNode, null);
		}
	}
	
	private Map<String, String> parse(File xmlLayoutFile, IdAnnotationHelper idHelper, Map<String, Element> layoutNode, String layoutId) {
		LOGGER.info("Layout Parsing: " + xmlLayoutFile.getName());
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

		Document doc;
		try {
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			doc = docBuilder.parse(xmlLayoutFile);
		} catch (Exception e) {
			LOGGER.error("Could not parse Layout file at path {}", xmlLayoutFile.getName(), e);
			return new HashMap<String, String>();
		}

		Map<String, String> foundObjects = new TreeMap<String, String>();
		Element documentElement = doc.getDocumentElement();
		
		searchInNode(documentElement, idHelper, foundObjects, layoutNode, layoutId);
		
		LOGGER.info("Layout Parsing Found: " + foundObjects);
		
		return foundObjects;
	}
}
