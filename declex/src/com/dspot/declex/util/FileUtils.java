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
import java.io.FileNotFoundException;

import javax.annotation.processing.ProcessingEnvironment;

import org.androidannotations.internal.helper.FileHelper;

public class FileUtils {
	public static File getConfigFile(String subPath, ProcessingEnvironment processingEnv) {
		
		String folderPath = "";
		try {
			File rootProject = FileHelper.findRootProject(processingEnv);
			folderPath = rootProject.getAbsolutePath();
		} catch (FileNotFoundException e) {
			//Try with gradle
			File gradle = new File("app" + File.separator + "build" + File.separator + "generated" + File.separator + "source" + File.separator + "apt");
			
			if (gradle.exists()) {
				folderPath = gradle.getAbsolutePath();
			} else {
				//Use Ant
				File ant = new File(".");
				folderPath = ant.getAbsolutePath();				
			}
		}
		
		String filePath = folderPath + File.separator + ".declex";
		
		File file = new File(filePath);
		if (!file.exists()) file.mkdir();
		
		if (subPath != null) {
			filePath = filePath + File.separator + subPath;
			file = new File(filePath);
			if (!file.exists()) file.mkdir();
		}
		
		return file;
	}
	
	public static File getResFolder(ProcessingEnvironment processingEnv) {
		
		String folderPath = "";
		try {
			File rootProject = FileHelper.findRootProject(processingEnv);
			folderPath = rootProject.getAbsolutePath();
		} catch (FileNotFoundException e) {
			//Try with gradle
			File gradle = new File("app" + File.separator + "build" + File.separator + "generated" + File.separator + "source" + File.separator + "apt");
			
			if (gradle.exists()) {
				folderPath = gradle.getAbsolutePath();
			} else {
				//Use Ant
				File ant = new File(".");
				folderPath = ant.getAbsolutePath();				
			}
		}
		
		//Ant Structure
		String resFolder = folderPath + File.separator + "res";
		File resFolderFile = new File(resFolder);
		
		if (resFolderFile.exists()) return resFolderFile;
		
		//Graddle Structure
		resFolder = folderPath
				                .replace("\\build\\generated\\source\\apt", "")		//Windows
				                .replace("/build/generated/source/apt", "") +  		//Linux + Mac
				File.separator + "src" + File.separator + "main" + File.separator + "res";
		
		return new File(resFolder);
	}
}
