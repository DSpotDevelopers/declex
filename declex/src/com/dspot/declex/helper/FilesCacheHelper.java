package com.dspot.declex.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
	
	private Map<String, Set<FileDetails>> generators;
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
			
			Map<String, Set<FileDetails>> generatorsTemp = (Map<String, Set<FileDetails>>) ois.readObject();
			
			Map<String, TypeElement> generatorsTypeElements = new HashMap<>();
			for (Entry<String, Set<FileDetails>> generatorEntry: generatorsTemp.entrySet()) {
				
				long lastModified = 0;
				if (generatorEntry.getKey() != null) {
					
					TypeElement generatorElement = generatorsTypeElements.get(generatorEntry.getKey());
					if (generatorElement == null) {
						generatorElement = environment.getProcessingEnvironment().getElementUtils()
 								  					  .getTypeElement(generatorEntry.getKey());	
						if (generatorElement == null) {
							//The generator element doesn't exists anymore
							System.out.println("Removing Generated because file was removed: " + generatorEntry.getKey());
							continue;
						}
						
						System.out.println("Checked generated: " + generatorEntry.getKey());
					}
					
					lastModified = trees.getPath(generatorElement).getCompilationUnit()
							            .getSourceFile().getLastModified();
				}
				
				Set<FileDetails> fileDetailsList = new HashSet<>();
				
				for (FileDetails details : generatorEntry.getValue()) {
					
					if (environment.getOptionBooleanValue(OPTION_CACHE_FILES)) {
						if (details.cachedFile == null || !(new File(details.cachedFile).exists())) continue;
					} else {
						if (details.cachedFile != null && new File(details.cachedFile).exists()) {
							try {
								new File(details.cachedFile).delete();
							} catch (Exception e){}
						}
						details.cachedFile = null;
					}
					
					//If the generator was modified, remove this FileDetails
					for (FileDependency dependency : details.dependencies) {
						if (dependency.lastModified != lastModified) {
							System.out.println("Removing Generated because dependency changed: " + details.className);
							continue;
						}						
					}
					
					System.out.println("Remembering Generated: " + details.className + " with Cache: " + details.cachedFile);
					
					fileDetailsList.add(FileDetails.fromFileDetails(details));
					generatedClasses.put(details.className, generatorEntry.getKey());
				}

				if (!fileDetailsList.isEmpty()) {
					generators.put(generatorEntry.getKey(), fileDetailsList);
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
		
		System.out.println("Saving Generators: " + generators);
	}
	
	public void addGeneratedClass(String clazz, Element generator) {
		
		if (generator != null && !(generator instanceof TypeElement)) {
			throw new RuntimeException("Element " + generator + " should be a TypeElement");
		}
		
		String generatorName = generator==null? null : generator.asType().toString();
		long generatorLastModified = 0;
		
		if (generatorName != null) {
			//If generator was also generated, use the Generator of the Generator
			if (generatedClasses.containsKey(generatorName)) {
				generatorName = generatedClasses.get(generatorName);
				
				for (FileDependency dependency : getFileDetails(generator.asType().toString()).dependencies) {
					if (dependency.generator.equals(generatorName)) {
						generatorLastModified = dependency.lastModified;
						break;
					}
				}
				
			} else {
				final TreePath treePath = trees.getPath(generator);
				generatorLastModified = treePath.getCompilationUnit().getSourceFile().getLastModified();
			}
		} else {
			//If a generator was previously assign, do not assign null generator
			if (generatedClasses.containsKey(clazz)) {
				return;
			}
		}
		
		//Remove reference from Null generator if any
		if (generatorName != null && generatedClasses.containsKey(clazz) 
			&& generatedClasses.get(clazz) == null) {
			Set<FileDetails> generatedClassesByGenerator = generators.get(null);
			generatedClassesByGenerator.remove(FileDetails.newFileDetails(clazz));
		}
		
		Set<FileDetails> generatedClassesByGenerator = generators.get(generatorName);
		if (generatedClassesByGenerator == null) {
			generatedClassesByGenerator = new HashSet<>();
			generators.put(generatorName, generatedClassesByGenerator);
		}
		
		FileDetails details = FileDetails.newFileDetails(clazz);
		if (generatorName != null) {
			details.dependencies.add(FileDependency.newFileDependency(generatorName, generatorLastModified));
		}
		generatedClassesByGenerator.add(details);
		
		generatedClasses.put(clazz, generatorName);
	}
	
	public Set<FileDetails> getFileDetailsList(String clazz) {
		return Collections.unmodifiableSet(generators.get(generatedClasses.get(clazz)));
	}
	
	public FileDetails getFileDetails(String clazz) {
		
		if (!generatedClasses.containsKey(clazz)) {
			throw new RuntimeException("The clazz " + clazz + " is not registered as a generated class");
		}
		
		Set<FileDetails> detailsList = generators.get(generatedClasses.get(clazz));
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
		
		public List<FileDependency> dependencies = new LinkedList<>();
		
		private static FileDetails fromFileDetails(FileDetails from) {
			
			FileDetails newFileDetails = newFileDetails(from.className);
			
			newFileDetails.cachedFile = from.cachedFile;
			newFileDetails.originalFile = from.originalFile;
			
			for (FileDependency dependency : from.dependencies) {
				newFileDetails.dependencies.add(FileDependency.fromFileDependency(dependency));
			}
			
			return newFileDetails;
		}
		
		private static FileDetails newFileDetails(String className) {
			
			FileDetails details = fileDetailsMap.get(className);
			if (details == null) {
				details = new FileDetails();
				fileDetailsMap.put(className, details);
			}
	
			details.className = className;
			
			return details;
		}
				
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof FileDetails)) return false;
			return this.className.equals(((FileDetails)obj).className);
		}
		
		@Override
		public String toString() {
			return this.className;
		}
	}
	
	public static class FileDependency implements Serializable {

		private static final long serialVersionUID = 1L;

		private static Map<String, FileDependency> fileDependencyMap = new HashMap<>();
		
		public String generator;
		public long lastModified;
		
		private static FileDependency fromFileDependency(FileDependency from) {
			return newFileDependency(from.generator, from.lastModified);
		}
		
        private static FileDependency newFileDependency(String generator, long lastModified) {
			
			FileDependency dependency = fileDependencyMap.get(generator);
			if (dependency == null) {
				dependency = new FileDependency();
				fileDependencyMap.put(generator, dependency);
			}
					
			dependency.generator = generator;
			dependency.lastModified = lastModified;
			
			return dependency;
		}
        		
	}
	
}
