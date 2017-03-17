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
package com.dspot.declex.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.androidannotations.helper.IdAnnotationHelper;
import org.androidannotations.logger.Logger;
import org.androidannotations.rclass.IRClass.Res;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MenuParser {
	private Logger LOGGER;
	
	private Map<String, List<String>> menuMaps = new HashMap<String, List<String>>();

	private List<File> menuFolders = new LinkedList<File>();
	
	private ProcessingEnvironment processingEnv;
	
	private static MenuParser instance;
	
	public static MenuParser getInstance() {
		return instance;
	}
	
	public MenuParser(ProcessingEnvironment processingEnv, Logger logger) {
		this.LOGGER = logger;
		this.processingEnv = processingEnv;
		
		File resFolderFile = FileUtils.getResFolder(processingEnv);
		LOGGER.info("Menu Parsing in: " + resFolderFile.getAbsolutePath());
		
		for (File file : resFolderFile.listFiles()) {
			if (file.isDirectory() && (file.getName().equals("menu") || file.getName().startsWith("menu-"))) {
				menuFolders.add(file);
			}
		}
		
		LOGGER.info("Menu Folders found: " + menuFolders);
		
		MenuParser.instance = this;
	}
	
	public List<String> getMenuObjects(String menuName, IdAnnotationHelper idHelper) {
		List<String> menuObjects = menuMaps.get(menuName);
		
		if (menuObjects == null) {
			for (File menu : menuFolders) {
				for (File file : menu.listFiles()) {
					if (file.isFile() && file.getName().equals(menuName + ".xml")) {
						menuObjects = parse(file, idHelper);
						menuMaps.put(menuName, menuObjects);
						break;
					}
				}
			}
		}
		
		return menuObjects;
	}
	
	private void searchInNode(Element node, IdAnnotationHelper idHelper, List<String> foundObjects ) {
		
		if (node.hasAttribute("android:id")) {
			String id = node.getAttribute("android:id");
			id = id.substring(id.lastIndexOf('/') + 1);
			
			if (idHelper.containsField(id, Res.ID))	foundObjects.add(id);
		}		
		
		NodeList nodes = node.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) 
			if (nodes.item(i) instanceof Element){
			searchInNode((Element)nodes.item(i), idHelper, foundObjects);
		}
	}
	
	private List<String> parse(File xmlMenuFile, IdAnnotationHelper idHelper) {
		LOGGER.info("Menu Parsing: " + xmlMenuFile.getName());
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

		Document doc;
		try {
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			doc = docBuilder.parse(xmlMenuFile);
		} catch (Exception e) {
			LOGGER.error("Could not parse Menu file at path {}", xmlMenuFile.getName(), e);
			return new ArrayList<String>();
		}

		List<String> foundObjects = new LinkedList<String>();
		Element documentElement = doc.getDocumentElement();
		
		searchInNode(documentElement, idHelper, foundObjects);
		
		LOGGER.info("Menu Parsing Found: " + foundObjects);
		
		return foundObjects;
	}
}
