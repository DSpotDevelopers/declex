package com.dspot.declex.helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.Option;
import org.androidannotations.holder.GeneratedClassHolder;

import com.dspot.declex.util.FileUtils;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

public class FilesCacheHelper {
	
	public static final Option OPTION_CACHE_FILES = new Option("cacheFiles", "false");
	
	//<Generator, FileDetails>
	private Map<String, Set<FileDetails>> generators;
	private Set<String> ancestors;
	
	//<Class, Dependencies>
	private Map<String, Set<FileDependency>> generatedClassesDependencies;
	
	private Trees trees;
	private AndroidAnnotationsEnvironment environment;
	
	private static FilesCacheHelper instance;
	
	public static FilesCacheHelper getInstance() {
		return instance;
	}
	
	public static boolean isCacheFilesEnabled() {
		return instance.environment.getOptionBooleanValue(OPTION_CACHE_FILES);
	}
	
	public FilesCacheHelper(AndroidAnnotationsEnvironment environment) {
		instance = this;
		
		this.environment = environment;
		this.trees = Trees.instance(environment.getProcessingEnvironment());
		
		loadGeneratedClasses();	
	}
	
	private static File getExternalCache() {
		return FileUtils.getPersistenceConfigFile("cache");
	}
	
	private static File getExternalCacheIndex() {
		return new File(getExternalCache().getAbsolutePath() + File.separator + "index.dat");
	}
	
	private static File getExternalCacheAncestorsIndex() {
		return new File(getExternalCache().getAbsolutePath() + File.separator + "ancestors_index.dat");
	}
	
	@SuppressWarnings("unchecked")
	private static Map<String, Set<FileDetails>> loadGenerators() {
		ObjectInputStream ois = null;
		Map<String, Set<FileDetails>> generatorsTemp = null;
		try {
			
			FileInputStream fin = new FileInputStream(getExternalCacheIndex());
			InputStream buffer = new BufferedInputStream(fin);
			ois = new ObjectInputStream(buffer);

			generatorsTemp = (Map<String, Set<FileDetails>>) ois.readObject();
			
		} catch (Throwable e) {
		} finally {
			if (ois != null) {
				try {
					ois.close();
				} catch (IOException e) {}
			}
		}
		
		return generatorsTemp;
	}
	
	@SuppressWarnings("unchecked")
	private static Set<String> loadAncestors() {
		Set<String> ancestorsTemp = null;
		ObjectInputStream ois = null;
		try {
			InputStream fin = new FileInputStream(getExternalCacheAncestorsIndex());
			InputStream buffer = new BufferedInputStream(fin);
			ois = new ObjectInputStream(buffer);
			
			ancestorsTemp = (Set<String>) ois.readObject();
			
		} catch (Throwable e) {
		} finally {
			if (ois != null) {
				try {
					ois.close();
				} catch (IOException e) {}
			}
		}
		
		return ancestorsTemp;
	}
	
	private static void saveGenerators(Map<String, Set<FileDetails>> generators) {
		ObjectOutputStream oos = null;
		try {
			OutputStream fout = new FileOutputStream(getExternalCacheIndex());
			OutputStream buffer = new BufferedOutputStream(fout);
			oos = new ObjectOutputStream(buffer);
			oos.writeObject(generators);
		} catch (IOException e) {
			printCacheErrorToLogFile(e);
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (IOException e) {}
			}
		}
	}
	
	private static void saveAncestors(Set<String> ancestors) {
		ObjectOutputStream oos = null;
		try {
			OutputStream fout = new FileOutputStream(getExternalCacheAncestorsIndex());
			OutputStream buffer = new BufferedOutputStream(fout);
			oos = new ObjectOutputStream(buffer);
			oos.writeObject(ancestors);
		} catch (IOException e) {
			printCacheErrorToLogFile(e);
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (IOException e) {}
			}
		}
	}
	
	private static void saveGeneratorsAndAncestors(Map<String, Set<FileDetails>> generators, Set<String> ancestors) {
		
		saveGenerators(generators);
		
		saveAncestors(ancestors);		
		
//		if (environment.getProcessingEnvironment().getOptions().containsKey("logLevel")) {
//			System.out.println("Generators: [");
//			for (Entry<String, Set<FileDetails>> entry : generators.entrySet()) {
//				System.out.println("    " + entry.getKey() + ": [");
//				for (FileDetails details : entry.getValue()) {
//					System.out.println("        " + details);
//				}
//				System.out.println("    ]");
//			}
//			System.out.println("]");
//			
//			System.out.println("Dependencies: [");
//			for (Entry<String, Set<FileDependency>> entry : generatedClassesDependencies.entrySet()) {
//				System.out.println("    " + entry.getKey() + ": [");
//				for (FileDependency dependency : entry.getValue()) {
//					System.out.println("        " + dependency);
//				}
//				System.out.println("    ]");
//			}
//			System.out.println("]");
	}
	
	private static void printCacheErrorToLogFile(Exception e) {
		try {
			File file = new File(getExternalCache().getAbsolutePath() + File.separator + "error.log");
			PrintStream ps = new PrintStream(file);
			e.printStackTrace(ps);
			ps.close();
		} catch (Exception e1){}
	}
	
	private void loadGeneratedClasses() {
		
		generatedClassesDependencies = new HashMap<>();
		generators = new HashMap<>();
		ancestors = new HashSet<>();
		
		Set<String> ancestorsTemp = loadAncestors();
		if (ancestorsTemp != null) {
			ancestors.addAll(ancestorsTemp);
		}
		
		Map<String, Set<FileDetails>> generatorsTemp = loadGenerators();		
		if (generatorsTemp != null) {
			for (Entry<String, Set<FileDetails>> generatorEntry: generatorsTemp.entrySet()) {
				
				Set<FileDetails> fileDetailsList = generators.get(generatorEntry.getKey());
				if (fileDetailsList == null) {
					fileDetailsList = new HashSet<>();					
				}
				
				for (FileDetails details : generatorEntry.getValue()) {
					
					if (isCacheFilesEnabled()) {
						if (details.cachedFile == null || !(new File(details.cachedFile).exists())) continue;
					} else {
						details.removeCache();
					}
					
					//If the generator was modified, remove this FileDetails
					for (FileDependency dependency : details.dependencies) {
						FileDependency newDependency = FileDependency.fromFileDependency(dependency);
						if (!isFileDependencyValid(newDependency, environment, trees)) {
							System.out.println(
								"Removing FileDetails because dependency changed: " + details.className 
								+ ", dependency : " + dependency
							);							
							details.invalidate();							
							continue;
						}						
					}
					
					if (!details.invalid) {
						fileDetailsList.add(FileDetails.fromFileDetails(details));
						generatedClassesDependencies.put(details.className, details.dependencies);
					}
				}
	
				if (!fileDetailsList.isEmpty()) {
					generators.put(generatorEntry.getKey(), fileDetailsList);
				}
			}
		}
		
	}
	
	public static void runClassCacheCreation() {
		
		Set<FileDetails> fileDetails = new HashSet<>();
		Map<String, Set<FileDetails>> generators = new HashMap<>();
		
		System.out.println("Loading Cached File Details");
		
		Map<String, Set<FileDetails>> generatorsTemp = loadGenerators();
		if (generatorsTemp != null) {
			for (Entry<String, Set<FileDetails>> generatorEntry: generatorsTemp.entrySet()) {
				
				Set<FileDetails> fileDetailsList = generators.get(generatorEntry.getKey());
				if (fileDetailsList == null) {
					fileDetailsList = new HashSet<>();					
				}
				
				for (FileDetails details : generatorEntry.getValue()) {
					FileDetails newDetails = FileDetails.fromFileDetails(details);
					fileDetailsList.add(newDetails);
					fileDetails.add(newDetails);
				}
				
				generators.put(generatorEntry.getKey(), fileDetailsList);
			}
		} else {
			System.out.println("Cached Files Index doesn't exists or it is corrupted");
			return;
		}

		System.out.println("Creating DecleX Cache Jar");
		
		final File tempJarFile = new File(getExternalCache().getAbsolutePath() + File.separator + "declex_cache_temp.jar");
		if (tempJarFile.exists()) tempJarFile.delete();		
		
		FileOutputStream jar = null;
		JarOutputStream jarOut = null;

		try {

			jar = new FileOutputStream(tempJarFile);
			jarOut = new JarOutputStream(jar);
		
			for (FileDetails details : fileDetails) {
				
				if (details.cachedClassesSearchFolder == null) continue;
				
				final String pkg = details.className.substring(0, details.className.lastIndexOf('.'));
				final String className = details.className.substring(details.className.lastIndexOf('.')+1);
				
				//Wait until the class files be created
				while (!new File(details.cachedClassesSearchFolder + File.separator + className + ".class").exists()) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {}
				};
				
				final FileFilter fileFilter = new FileFilter() {								
					@Override
					public boolean accept(File pathname) {
						return pathname.getName().matches(className + "(\\$[a-zA-Z0-9_$]+)*\\.class");
					}
				};					
										
				File[] classes = new File(details.cachedClassesSearchFolder).listFiles(fileFilter);
				
				for (File file : classes) {
					File cachedFolder = new File(
						getExternalCache().getAbsolutePath() + File.separator
						+ "classes" + File.separator + pkg.replace('.', File.separatorChar)
					);
					cachedFolder.mkdirs();
					
					File externalCachedFile = new File(cachedFolder.getAbsolutePath() 
							+ File.separator 
							+ file.getName());
																							
					try {
						jarOut.putNextEntry(new ZipEntry(pkg.replace('.', '/') + "/" + file.getName()));												
						FileUtils.copyCompletely(
								new FileInputStream(file), 
								jarOut,
								null, false
							);
						jarOut.closeEntry();
						
						FileUtils.copyCompletely(
								new FileInputStream(file), 
								new FileOutputStream(externalCachedFile),
								null, false
							);
					} catch (IOException e) {
						printCacheErrorToLogFile(e);
					}
					
					details.cachedClassFiles.put(
						externalCachedFile.getAbsolutePath(),
						file.getAbsolutePath()
					);
				}
			}
			
		} catch (Exception e) {
			printCacheErrorToLogFile(e);
		} finally {
			try {												
				
				if (jarOut != null) jarOut.close();
				if (jar != null) jar.close();
				
				final File jarFile = new File(getExternalCache().getAbsolutePath() + File.separator + "declex_cache.jar");
				
				//Copy the cached file
				FileUtils.copyCompletely(
					new FileInputStream(tempJarFile),
					new FileOutputStream(jarFile),
					null
				);
				
				tempJarFile.delete();
				
			} catch (Exception e) {
				printCacheErrorToLogFile(e);
			} 
		}
		
		saveGenerators(generators);
	}
	
	public void saveGeneratedClasses() {
		
		if (isCacheFilesEnabled()) {
			Filer filer = environment.getProcessingEnvironment().getFiler();
			
			try {
				for (Entry<String, FileDetails> entry : FileDetails.fileDetailsMap.entrySet()) {
					
					final String clazz = entry.getKey();
					if (!generatedClassesDependencies.containsKey(clazz)) continue;
					
					final String pkg = clazz.substring(0, clazz.lastIndexOf('.'));
					final String resourceName = clazz.substring(clazz.lastIndexOf('.')+1) + ".class";
					
					FileObject classFile = filer.getResource(StandardLocation.CLASS_OUTPUT, pkg, resourceName);
					
					// Cache the .class file
					final URI fileUri = classFile.toUri();
					
					final FileDetails details = entry.getValue();
					details.cachedClassesSearchFolder = Paths.get(fileUri).toFile()
							                                 .getParentFile().getAbsolutePath();
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}				

		}
		
		saveGeneratorsAndAncestors(generators, ancestors);
				
		if (isCacheFilesEnabled()) {
			try {
				Path thisJarPath = Paths.get(FilesCacheHelper.class.getProtectionDomain().getCodeSource().getLocation().toURI());			
				Runtime.getRuntime().exec("java -jar " + thisJarPath + " cache");
				
//				System.out.println("java -jar " + thisJarPath + " cache");
//				
//				BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
//				
//				String line;
//		        while ((line = in.readLine()) != null) {
//		          System.out.println("CACHE: " + line);
//		        }
//		        in.close();
				
			} catch (URISyntaxException | IOException e) {
				e.printStackTrace();
			}
		}
				
	}
	
	public void addAncestor(String ancestor, Element generator) {
		if (generator != null && !(generator instanceof TypeElement)) {
			throw new RuntimeException("Element " + generator + " should be a TypeElement");
		}
		
		if (generators.containsKey(ancestor) && generators.containsKey(generator.asType().toString())) {
			
			for (FileDetails fileDetails : generators.get(ancestor)) {	
				addGeneratedClass(fileDetails.className, generator);
			}
			
			ancestors.add(ancestor);
			
			System.out.println("Adding acestor: " + ancestor + " of " + generator);
		}
	}
	
	public void addGeneratedClass(String clazz, Element generator) {
		
		if (generator != null && !(generator instanceof TypeElement)) {
			throw new RuntimeException("Element " + generator + " should be a TypeElement");
		}
		
		final String generatorClass = generator == null? null : generator.asType().toString();
		
		Set<FileDependency> dependencies = new HashSet<>();
		if (generator != null) {
			final TreePath treePath = trees.getPath(generator);
			long lastModified = treePath==null? 0 : treePath.getCompilationUnit().getSourceFile().getLastModified();			
			dependencies.add(FileDependency.newFileDependency(generatorClass, lastModified)); 
		}

		boolean dependenciesDelegated = false;
		if (!dependencies.isEmpty()) {
			//If generator was also generated, use the Dependencies of the Generator
			if (generatedClassesDependencies.containsKey(generatorClass)) {
				dependencies = generatedClassesDependencies.get(generatorClass);
				dependenciesDelegated = true;
			} 
		} else {
			//If a generator was previously assigned, do not assign null generator
			if (generatedClassesDependencies.containsKey(clazz)) {
				return;
			}
		}
		
		//Create FileDetails
		FileDetails details = FileDetails.newFileDetails(clazz);
		details.dependencies.addAll(dependencies);
		generatedClassesDependencies.put(clazz, details.dependencies);
		
		if (dependencies.isEmpty()) {
			//Add reference in Null generator
			Set<FileDetails> generatedClassesByGenerator = generators.get(null);
			if (generatedClassesByGenerator == null) {
				generatedClassesByGenerator = new HashSet<>();
				generators.put(null, generatedClassesByGenerator);
			}
			generatedClassesByGenerator.add(details);
		} else {
			//Remove reference from Null generator if any
			if (generators.containsKey(null)) {
				generators.get(null).remove(details);
			}
		}
		
		//Add the FileDetails to all its generators
		for (FileDependency dependency : dependencies) {
			Set<FileDetails> generatedClassesByGenerator = generators.get(dependency.generator);
			if (generatedClassesByGenerator == null) {
				generatedClassesByGenerator = new HashSet<>();
				generators.put(dependency.generator, generatedClassesByGenerator);
			}
			generatedClassesByGenerator.add(details);
		}
		
		//If "clazz" is a generator and it delegated dependencies, 
		//then update all dependencies of the generated classes,
		//also "clazz" should not longer be a generator
		if (dependenciesDelegated && generators.containsKey(clazz)) {
			for (FileDetails fileDetails : generators.get(clazz)) {				
				fileDetails.dependencies.addAll(dependencies);				
				fileDetails.dependencies.remove(FileDependency.withGenerator(clazz));
				generatedClassesDependencies.put(fileDetails.className, fileDetails.dependencies);
				
				for (FileDependency dependency : dependencies) {
					Set<FileDetails> generatedClassesByGenerator = generators.get(dependency.generator);
					generatedClassesByGenerator.add(fileDetails);
				}
			}
			generators.remove(clazz);
		}
		
	}
	
	public Set<FileDetails> getFileDetailsList(String clazz) {
		Set<FileDetails> details = new HashSet<>();
		for (FileDependency dependency : generatedClassesDependencies.get(clazz)) {
			details.addAll(generators.get(dependency.generator));
		}
		return Collections.unmodifiableSet(details);
	}
	
	public FileDetails getFileDetails(String clazz) {
		
		if (!generatedClassesDependencies.containsKey(clazz)) {
			throw new RuntimeException("The clazz " + clazz + " is not registered as a generated class");
		}
		
		return FileDetails.newFileDetails(clazz);
	}
	
	public boolean hasCachedFile(String clazz) {
		if (generatedClassesDependencies.containsKey(clazz)) {
			FileDetails details = getFileDetails(clazz);
			return details != null && details.cachedFile != null && new File(details.cachedFile).exists();
		}
		
		return false;
	}
	
	public boolean isClassGenerated(String clazz, GeneratedClassHolder holder) {
		return this.isClassGenerated(clazz, holder.getAnnotatedElement());
	}
	
	public boolean isClassGenerated(String clazz, TypeElement element) {
		clazz = variableClassFromImports(clazz, element);
		return generatedClassesDependencies.containsKey(clazz);
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
	
	private boolean isFileDependencyValid(FileDependency dependency, AndroidAnnotationsEnvironment environment, Trees trees) {
    	if (dependency.isValid != null) return dependency.isValid;
    	TypeElement generatorElement = environment.getProcessingEnvironment().getElementUtils()
						  					      .getTypeElement(dependency.generator);

		if (generatorElement == null) {
			dependency.isValid = false;
		} else {
			dependency.isValid = dependency.lastModified == trees.getPath(generatorElement)
									       .getCompilationUnit()
    			    					   .getSourceFile().getLastModified();
		}

		return dependency.isValid;
    }

	public static class FileDetails implements Serializable {
		
		private static final long serialVersionUID = 1L;
		private static Map<String, FileDetails> fileDetailsMap = new HashMap<>();
		
		private static Executor cacheExecutor = Executors.newFixedThreadPool(4);
		private static int cacheTasksCount = 0;
		
		public String cachedFile;
		public String originalFile;
		
		public Map<String, String> cachedClassFiles;
		public String cachedClassesSearchFolder;
		
		public String className;
		
		public boolean isAction;
		public boolean isAncestor;
		
		public transient boolean invalid;
		public transient boolean generated;
		
		public Set<FileDependency> dependencies;
		
		public static boolean isSaving() {
			return cacheTasksCount != 0;
		}
		
		private static FileDetails fromFileDetails(FileDetails from) {
			
			FileDetails newFileDetails = newFileDetails(from.className);
			
			newFileDetails.cachedFile = from.cachedFile;
			newFileDetails.originalFile = from.originalFile;
			newFileDetails.isAction = from.isAction;
			
			newFileDetails.cachedClassFiles.putAll(from.cachedClassFiles);
			newFileDetails.cachedClassesSearchFolder = from.cachedClassesSearchFolder;
			
			for (FileDependency dependency : from.dependencies) {
				newFileDetails.dependencies.add(FileDependency.fromFileDependency(dependency));
			}
			
			return newFileDetails;
		}
		
		private static FileDetails newFileDetails(String className) {
			
			FileDetails details = fileDetailsMap.get(className);
			if (details == null) {
				details = new FileDetails();
				details.dependencies = new HashSet<>();
				details.className = className;
				
				details.cachedClassFiles = new HashMap<>();
				
				fileDetailsMap.put(className, details);
			}			
			
			return details;
		}
		
		private void removeCache() {
			if (cachedFile != null && new File(cachedFile).exists()) {
				try {
					new File(cachedFile).delete();
				} catch (Exception e){}
			}
			cachedFile = null;
		}
		
		private void invalidate() {
			invalid = true;
			removeCache();
		}
		
		public void generate(ProcessingEnvironment env) throws FileNotFoundException, IOException {
			
			if (generated) return;
			
			cacheTasksCount++;
			cacheExecutor.execute(new Runnable() {
				
				@Override
				public void run() {
										
					try {
						File input = new File(cachedFile);
						File output = new File(originalFile);
						
						if (!output.exists()) {
							output.getParentFile().mkdirs();
							
							//Copy the cached file
							FileUtils.copyCompletely(
								new FileInputStream(input),
								new FileOutputStream(output),
								null
							);
						}
						
						for (Entry<String, String> cache : cachedClassFiles.entrySet()) {
							
							input = new File(cache.getKey());
							output = new File(cache.getValue());
							
							output.getParentFile().mkdirs();
											
							//Copy the cached file
							FileUtils.copyCompletely(
								new FileInputStream(input),
								new FileOutputStream(output),
								null
							);
						}
						
					} catch (Exception e) {
						printCacheErrorToLogFile(e);
					} finally {
						cacheTasksCount--;
					}
				}
			});
			
			generated = true;
			
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

		private static final long serialVersionUID = 2L;
		private static Map<String, FileDependency> fileDependencyMap = new HashMap<>();
		
		public String generator;
		public long lastModified;
		
		public transient Boolean isValid;
		
		private static FileDependency withGenerator(String generator) {
			return fileDependencyMap.get(generator);
		}
		
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
        
        @Override
		public boolean equals(Object obj) {
			if (!(obj instanceof FileDependency)) return false;
			return this.generator.equals(((FileDependency)obj).generator);
		}
        
        @Override
        public String toString() {
        	return this.generator;
        }
        		
	}
	
}
