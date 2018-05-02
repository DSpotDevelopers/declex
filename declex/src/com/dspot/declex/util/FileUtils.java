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
package com.dspot.declex.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.FileChannel;

import javax.annotation.processing.ProcessingEnvironment;

import org.androidannotations.internal.helper.FileHelper;

public class FileUtils {

	public static File getPersistenceConfigFile(String subPath) {
		String folderPath = new File(".declex").getAbsolutePath();		
		
		File file = new File(folderPath);
		if (!file.exists()) file.mkdir();
		
		if (subPath != null) {
			folderPath = folderPath + File.separator + subPath;
			file = new File(folderPath);
			if (!file.exists()) file.mkdir();
		}
		
		return file;		
	}
	
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
	
	
	public static void copyCompletely(URI input, File out) {
		try {
			InputStream in = null;
			try {
				File f = new File(input);
				if (f.exists())
					in = new FileInputStream(f);
			} catch (Exception notAFile) {
			}

			File dir = out.getParentFile();
			dir.mkdirs();

			if (in == null)
				in = input.toURL().openStream();

			FileUtils.copyCompletely(in, new FileOutputStream(out), null);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void copyCompletely(InputStream input, OutputStream output, byte[] buf) throws IOException {
		copyCompletely(input, output, buf, true);
	}
	
	public static void copyCompletely(InputStream input, OutputStream output, byte[] buf, boolean closeOutput) throws IOException {
		copyCompletely(input, output, buf, closeOutput, true);
	}

	public static void copyCompletely(InputStream input, OutputStream output, byte[] buf, boolean closeOutput, boolean closeInput) throws IOException {
		copyCompletely(input, output, null, buf, closeOutput, closeInput);
	}
	
	public static void copyCompletely(InputStream input, OutputStream output, OutputStream extraOutput, byte[] buf, boolean closeOutput, boolean closeInput) throws IOException {
		// if both are file streams, use channel IO
		if ((output instanceof FileOutputStream)
				&& (input instanceof FileInputStream)) {
			try {
				FileChannel target = ((FileOutputStream) output).getChannel();
				FileChannel source = ((FileInputStream) input).getChannel();
				
				source.transferTo(0, source.size(), target);

				source.close();
				target.close();

				return;
			} catch (Exception e) { /* failover to byte stream version */
				System.out.println("Info: failover to byte stream version");
			}
		}

		if (buf == null) buf = new byte[8192];
		while (true) {
			int length = input.read(buf);
			if (length < 0)
				break;
			output.write(buf, 0, length);
			
			if (extraOutput != null) {
				extraOutput.write(buf, 0, length);
			}			
		}

		if (closeInput) {
			try {
				input.close();
			} catch (IOException ignore) {}
		}
		
		if (closeOutput) {
			try {
				output.close();
			} catch (IOException ignore) {}
		}
		
		if (extraOutput != null) {
			try {
				extraOutput.close();
			} catch (IOException ignore) {}
		}
	}

}
