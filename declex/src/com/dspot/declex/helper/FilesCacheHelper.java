package com.dspot.declex.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.Option;
import org.androidannotations.holder.GeneratedClassHolder;

import com.dspot.declex.util.FileUtils;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

public class FilesCacheHelper {
	
	public static final Option OPTION_CACHE_FILES = new Option("cacheFiles", "false");
	
	private Map<String, List<FileDetails>> generators;
	private Map<String, String> generatedClasses;
	
	private Trees trees;
	private AndroidAnnotationsEnvironment environment;
	
	private static FilesCacheHelper instance;
	
	public static FilesCacheHelper getInstance() {
		return instance;
	}
	
	public FilesCacheHelper(AndroidAnnotationsEnvironment environment) {
		this.environment = environment;
		this.trees = Trees.instance(environment.getProcessingEnvironment());
		
		loadGeneratedClasses();	
		
		instance = this;
	}
	
	@SuppressWarnings("unchecked")
	private void loadGeneratedClasses() {
		
		generatedClasses = new HashMap<>();
		generators = new HashMap<>();
		
		File externalCacheFolder = FileUtils.getPersistenceConfigFile("cache", environment.getProcessingEnvironment());
		File externalCacheIndex = new File(externalCacheFolder.getAbsolutePath() + File.separator + "index.dat");
		
		ObjectInputStream ois = null;
		try {
			FileInputStream fin = new FileInputStream(externalCacheIndex);
			ois = new ObjectInputStream(fin);
			
			Map<String, List<FileDetails>> generatorsTemp = (Map<String, List<FileDetails>>) ois.readObject();
			
			for (Entry<String, List<FileDetails>> generatorEntry: generatorsTemp.entrySet()) {
				
				List<FileDetails> fileDetailsList = new LinkedList<>();
				generators.put(generatorEntry.getKey(), fileDetailsList);
				
				for (FileDetails details : generatorEntry.getValue()) {
					fileDetailsList.add(FileDetails.fromFileDetails(details));
					generatedClasses.put(details.className, generatorEntry.getKey());
				}
			}
			
		} catch (IOException | ClassNotFoundException e) {
			
		} finally {
			if (ois != null) {
				try {
					ois.close();
				} catch (IOException e) {}
			}
		}
	}
	
	public void saveGeneratedClasses() {
		File externalCacheFolder = FileUtils.getPersistenceConfigFile("cache", environment.getProcessingEnvironment());
		File externalCacheIndex = new File(externalCacheFolder.getAbsolutePath() + File.separator + "index.dat");
		
		ObjectOutputStream oos = null;
		try {
			FileOutputStream fout = new FileOutputStream(externalCacheIndex);
			oos = new ObjectOutputStream(fout);
			oos.writeObject(generators);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (IOException e) {}
			}
		}			
	}
	
	public void addGeneratedClass(String clazz, Element generator) {
		
		if (generator != null && !(generator instanceof TypeElement)) {
			throw new RuntimeException("Element " + generator + " should be a TypeElement");
		}
		
		final String generatorName = generator==null? null : generator.asType().toString();		
		List<FileDetails> generatedClassesByGenerator = generators.get(generatorName);
		if (generatedClassesByGenerator == null) {
			generatedClassesByGenerator = new LinkedList<>();
			generators.put(generatorName, generatedClassesByGenerator);
		}
		
		if (generator != null) {
			final TreePath treePath = trees.getPath(generator);
			generatedClassesByGenerator.add(
				FileDetails.newFileDetails(clazz, treePath.getCompilationUnit().getSourceFile().getLastModified())
			);
		} else {
			generatedClassesByGenerator.add(
					FileDetails.newFileDetails(clazz, 0)
				);
		}
		
		generatedClasses.put(clazz, generatorName);
	}
	
	public List<FileDetails> getFileDetailsList(String clazz) {
		for (FileDetails d : generators.get(generatedClasses.get(clazz))) {
			System.out.println("DD: " + d.className + " : " + d.lastModified);
		}
		return generators.get(generatedClasses.get(clazz));
	}
	
	public FileDetails getFileDetails(String clazz) {
		
		if (!generatedClasses.containsKey(clazz)) {
			throw new RuntimeException("The clazz " + clazz + " is not registered as a generated class");
		}
		
		List<FileDetails> detailsList = generators.get(generatedClasses.get(clazz));
		for (FileDetails details : detailsList) {
			if (details.className.equals(clazz)) {
				return details;
			}
		}
		
		generatedClasses.remove(clazz);
		
		return null;
	}
	
	public boolean hasCachedFile(String clazz) {
		if (generatedClasses.containsKey(clazz)) {
			FileDetails details = getFileDetails(clazz);
			return details != null && details.cachedFile != null;
		}
		
		return false;
	}
	
	public boolean isClassGenerated(String clazz, GeneratedClassHolder holder) {
		return this.isClassGenerated(clazz, holder.getAnnotatedElement());
	}
	
	public boolean isClassGenerated(String clazz, TypeElement element) {
		clazz = variableClassFromImports(clazz, element);
		return generatedClasses.containsKey(clazz);
	}
	
	private String variableClassFromImports(final String variableClass, TypeElement element) {
		
    	final TreePath treePath = trees.getPath(element);
		
		for (ImportTree importTree : treePath.getCompilationUnit().getImports()) {
			String lastElementImport = importTree.getQualifiedIdentifier().toString();
			String firstElementName = variableClass;
			String currentVariableClass = "";
			
			int pointIndex = lastElementImport.lastIndexOf('.');
			if (pointIndex != -1) {
				lastElementImport = lastElementImport.substring(pointIndex + 1);
			}
			
			pointIndex = firstElementName.indexOf('.');
			if (pointIndex != -1) {
				firstElementName = firstElementName.substring(0, pointIndex);
				currentVariableClass = variableClass.substring(pointIndex);
			}
			
			while (firstElementName.endsWith("[]")) {
				firstElementName = firstElementName.substring(0, firstElementName.length()-2);
				if (currentVariableClass.isEmpty()) currentVariableClass = currentVariableClass + "[]";
			}
			
			if (lastElementImport.equals(firstElementName)) {
				return importTree.getQualifiedIdentifier() + currentVariableClass;
			}
		}
		
		return variableClass;
	}

	public static class FileDetails implements Serializable {
		
		private static Map<String, FileDetails> fileDetailsMap = new HashMap<>();
		
		private static final long serialVersionUID = 1L;
		
		public String cachedFile;
		public String originalFile;
		public String className;
		public long lastModified;
		
		private static FileDetails fromFileDetails(FileDetails from) {
			
			FileDetails newFileDetails = newFileDetails(from.className, from.lastModified);
			
			newFileDetails.cachedFile = from.cachedFile;
			newFileDetails.originalFile = from.originalFile;
			
			return newFileDetails;
		}
		
		private static FileDetails newFileDetails(String className, long lastModified) {
			
			FileDetails details = fileDetailsMap.get(className);
			if (details == null) {
				details = new FileDetails();
				fileDetailsMap.put(className, details);
			}
					
			details.className = className;
			details.lastModified = lastModified;
			
			return details;
		}
				
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof FileDetails)) return false;
			return this.className.equals(((FileDetails)obj).className);
		}
	}
	
}
