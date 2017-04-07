package com.dspot.declex.helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.Option;
import org.androidannotations.holder.GeneratedClassHolder;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;

import com.dspot.declex.action.Actions;
import com.dspot.declex.util.FileUtils;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

public class FilesCacheHelper {
	
	private static Logger LOGGER = LoggerFactory.getLogger(FilesCacheHelper.class);
	
	public static final Option OPTION_CACHE_FILES = new Option("cacheFiles", "false");
	
	//<Generator, FileDetails>
	private Map<String, Set<FileDetails>> generators;
	
	//<Class, Dependencies>
	private Map<String, Set<FileDependency>> generatedClassesDependencies;
	
	private Trees trees;
	private AndroidAnnotationsEnvironment environment;
	
	private static FilesCacheHelper instance;
	
	private static Boolean serviceConnectionPassed;
	
	public static FilesCacheHelper getInstance() {
		return instance;
	}
	
	public static boolean isCacheFilesEnabled() {
		
		boolean optionCacheFiles = instance.environment.getOptionBooleanValue(OPTION_CACHE_FILES);
		if (!optionCacheFiles) return false;
		
		if (serviceConnectionPassed == null) {
			serviceConnectionPassed = false;
			
			Path thisJarPath = null;
			try {
				thisJarPath = Paths.get(FilesCacheHelper.class.getProtectionDomain().getCodeSource().getLocation().toURI());			
				Process p = Runtime.getRuntime().exec("java -jar " + thisJarPath);
				
				BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
				
				String line = in.readLine();
				serviceConnectionPassed = "DecleX Service".equals(line);
				
			} catch (Exception e) {
				
			}
			
			if (!serviceConnectionPassed) {
				LOGGER.warn("Error connecting to DecleX service, please ensure that you get \"DecleX Service\" "
						    + " when you run the command in console \"java -jar " + thisJarPath + "\"");
				LOGGER.warn("DecleX Cached Service couldn't initialize correctly");
			}
		}
		
		return serviceConnectionPassed;
	}
	
	public FilesCacheHelper(AndroidAnnotationsEnvironment environment) {
		instance = this;
		
		serviceConnectionPassed = null;
		
		this.environment = environment;
		this.trees = Trees.instance(environment.getProcessingEnvironment());
		
		loadGeneratedClasses();
		
		if (!isCacheFilesEnabled()) {
			try {
				//Remove cached files
				File file = new File (getExternalCache().getAbsolutePath() + File.separator + "declex_cache.jar");
				if (file.exists()) file.delete();
				
				File cachedClassesFolder = new File(getExternalCache().getAbsolutePath() + File.separator + "classes");
				org.apache.commons.io.FileUtils.deleteDirectory(cachedClassesFolder);				
			} catch (Exception e) {}
		}
	}
	
	private static File getExternalCache() {
		return FileUtils.getPersistenceConfigFile("cache");
	}
	
	private static File getExternalCacheIndex() {
		return new File(getExternalCache().getAbsolutePath() + File.separator + "index.dat");
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
	
	private static void printCacheErrorToLogFile(Throwable e) {
		try {
			File file = new File(getExternalCache().getAbsolutePath() + File.separator + "error.log");
			PrintStream ps = new PrintStream(file);
			e.printStackTrace(ps);
			ps.close();
		} catch (Exception e1){}
	}
	
	private void loadGeneratedClasses() {
		
		LOGGER.debug("Loading Cached Files");
		
		generatedClassesDependencies = new HashMap<>();
		generators = new HashMap<>();
		
		Map<String, Set<FileDetails>> generatorsTemp = loadGenerators();		
		if (generatorsTemp != null) {
			for (Entry<String, Set<FileDetails>> generatorEntry: generatorsTemp.entrySet()) {
				
				Set<FileDetails> fileDetailsList = generators.get(generatorEntry.getKey());
				if (fileDetailsList == null) {
					fileDetailsList = new HashSet<>();					
				}
				
				for (FileDetails generatorDetails : generatorEntry.getValue()) {
					
					final FileDetails details = FileDetails.fromFileDetails(generatorDetails);
					if (details.invalid) {
						continue;
					}
					
					if (isCacheFilesEnabled()) {
						if (!details.isCacheValid()) {
							LOGGER.debug("Removing Cached file because its cache is invalid: " + details.className);
							details.invalidate();
						}
					} else {
						details.removeCache();
					}					
					
					//If the generator was modified, remove this FileDetails
					for (FileDependency dependency : details.dependencies) {
						FileDependency newDependency = FileDependency.fromFileDependency(dependency);
						if (!isFileDependencyValid(newDependency, environment, trees)) {
							LOGGER.debug(
								"Removing Cached file because its dependency changed: " + details.className 
								+ ", dependency : " + dependency
							);							
							details.invalidate();
						}						
					}
					
					if (!details.invalid) {
						fileDetailsList.add(details);
						generatedClassesDependencies.put(details.className, details.dependencies);
					}
				}
	
				if (!fileDetailsList.isEmpty()) {
					generators.put(generatorEntry.getKey(), fileDetailsList);
				}
			}
		}
		
	}
	
	public void validateCurrentCache() {
		if (isCacheFilesEnabled()) {
			for (Entry<String, FileDetails> entry : FileDetails.fileDetailsMap.entrySet()) {
				if (!entry.getValue().invalid) {
					entry.getValue().validate();
				}
			}
		}
	}
	
	public static void runClassCacheCreation(String generatorToWait) {
		
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

		long compilerWaitTime = 0;
		try {

			jar = new FileOutputStream(tempJarFile);
			jarOut = new JarOutputStream(jar);
			
			details: for (FileDetails details : fileDetails) {
								
				if (details.cachedClassesSearchFolder == null) continue;
				
				final String pkg = details.className.substring(0, details.className.lastIndexOf('.'));
				final String className = details.className.substring(details.className.lastIndexOf('.')+1);
				
				if (details.doGenerateJavaCached) {
					details.generateJavaCached();
					details.doGenerateJavaCached = false;
				}
				
				if (details.cached) {
					for (String cachedFile : details.cachedClassFiles.keySet()) {
						
						File file = new File(cachedFile);
						
						//Add file to Jar
						jarOut.putNextEntry(new ZipEntry(pkg.replace('.', '/') + "/" + file.getName()));												
						FileUtils.copyCompletely(new FileInputStream(file), jarOut, null, false);
						jarOut.closeEntry();
					}
					
					continue;
				}
				
				//Wait until the class files be created	
				boolean wasWaiting = false;
				while (!new File(details.cachedClassesSearchFolder + File.separator + className + ".class").exists()) {
					
					if (!wasWaiting) {
						System.out.println("Waiting for: " + details.cachedClassesSearchFolder + File.separator + className + ".class");
					}
					
					//Maximum time waiting for compiler
					if (compilerWaitTime > 30000) {
						details.invalidate();
						System.out.println("Removing from cache: " + details.className);
						continue details;
					}
					
					Thread.sleep(100);
					compilerWaitTime += 100;
					wasWaiting = true;
				};
				
				if (wasWaiting) {
					//Just to be sure all is written by the compiler
					Thread.sleep(1000);
				}
				
				final FileFilter fileFilter = new FileFilter() {								
					@Override
					public boolean accept(File pathname) {
						return pathname.getName().matches(className + "(\\$[a-zA-Z0-9_$]+)*\\.class");
					}
				};					

				try {
					File[] classes = new File(details.cachedClassesSearchFolder).listFiles(fileFilter);
	
					for (File file : classes) {		
						//Add file to Jar
						jarOut.putNextEntry(new ZipEntry(pkg.replace('.', '/') + "/" + file.getName()));												
						FileUtils.copyCompletely(new FileInputStream(file), jarOut, null, false);
						jarOut.closeEntry();

						File cachedFolder = new File(
								getExternalCache().getAbsolutePath() + File.separator
								+ "classes" + File.separator + pkg.replace('.', File.separatorChar)
							);
							cachedFolder.mkdirs();
							
						File externalCachedFile = new File(cachedFolder.getAbsolutePath() 
								+ File.separator 
								+ file.getName());
						
						FileUtils.copyCompletely(
								new FileInputStream(file), 
								new FileOutputStream(externalCachedFile),
								null
							);
						
						details.cachedClassFiles.put(
								externalCachedFile.getAbsolutePath(),
								file.getAbsolutePath()
							);
						details.cached = true;
					}
				
				} catch (Throwable e) {
					printCacheErrorToLogFile(e);
					details.invalidate();
				}

			}
			
		} catch (Exception e) {
			printCacheErrorToLogFile(e);
		} finally {
			try {												
				
				if (jarOut != null) jarOut.close();
				if (jar != null) jar.close();
				
				//Wait until the class files be created	
				boolean wasWaiting = false;
				while (!new File(generatorToWait).exists()) {
					
					if (!wasWaiting) {
						System.out.println("Final waiting for: " + generatorToWait);
					}

					//Maximum time waiting for compiler
					if (compilerWaitTime > 30000) {
						break;
					}
					
					Thread.sleep(100);
					compilerWaitTime += 100;
					wasWaiting = true;
				};
				
				if (wasWaiting) {
					//Just to be sure all is written by the compiler
					Thread.sleep(1000);
				}
				
				final File jarFile = new File(getExternalCache().getAbsolutePath() + File.separator + "declex_cache.jar");
				
				//Copy the cached file
				FileUtils.copyCompletely(
					new FileInputStream(tempJarFile),
					new FileOutputStream(jarFile),
					null
				);
				
				tempJarFile.delete();
				
			} catch (Throwable e) {
				printCacheErrorToLogFile(e);
			} 
		}
		
		saveGenerators(generators);
	}
	
	public void saveGeneratedClasses() {
		
		boolean cacheClassesRequired = false;
		
		Filer filer = environment.getProcessingEnvironment().getFiler();
		if (isCacheFilesEnabled()) {			
			
			try {
				for (Entry<String, FileDetails> entry : FileDetails.fileDetailsMap.entrySet()) {
					
					final String clazz = entry.getKey();
					final FileDetails details = entry.getValue();
					
					//The file was not generated
					if (details.originalFile == null || details.cachedFile == null) {
						LOGGER.warn(
							"Removing Cached Reference because it was not generated: " + details.className
						);
						details.invalidate();
						continue;
					}
					
					if (details.invalid) {
						LOGGER.warn(
								"Cached Reference was invalidated but was not regenerated: " + details.className
							);
						continue;
					}
					if (!generatedClassesDependencies.containsKey(clazz)) continue;
					
					final String pkg = clazz.substring(0, clazz.lastIndexOf('.'));
					final String resourceName = clazz.substring(clazz.lastIndexOf('.')+1) + ".class";
					
					FileObject classFile = filer.getResource(StandardLocation.CLASS_OUTPUT, pkg, resourceName);
					
					// Cache the .class file
					final URI fileUri = classFile.toUri();					
					details.cachedClassesSearchFolder = Paths.get(fileUri).toFile()
							                                 .getParentFile().getAbsolutePath();
					
					cacheClassesRequired = true;
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}				

		}
		
		saveGenerators(generators);
				
		if (cacheClassesRequired) {
			try {
				//This class will be used to detect when the compiler finished
				String generatorClass = null;
				
				Set<String> autoGeneratedGenerators = new HashSet<>();
				if (generators.containsKey(null)) {
					for (FileDetails details : generators.get(null)) {
						autoGeneratedGenerators.add(details.className);
					}
				}
				
				for (String generator : generators.keySet()) {
					if (!autoGeneratedGenerators.contains(generator) && generator != null) {
						generatorClass = generator;
						break;
					}
				}
				
				final String pkg = generatorClass.substring(0, generatorClass.lastIndexOf('.'));
				final String resourceName = generatorClass.substring(generatorClass.lastIndexOf('.')+1) + ".class";				
				FileObject classFile = filer.getResource(StandardLocation.CLASS_OUTPUT, pkg, resourceName);
				String generatorPath = Paths.get(classFile.toUri()).toFile().toString();
				
				Path thisJarPath = Paths.get(FilesCacheHelper.class.getProtectionDomain().getCodeSource().getLocation().toURI());			
				Runtime.getRuntime().exec("java -jar " + thisJarPath + " cache-after \"" + generatorPath + "\"");
				
//				System.out.println("java -jar " + thisJarPath + " cache-after \"" + generatorClass + "\"");
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
		
//		if (environment.getProcessingEnvironment().getOptions().containsKey("logLevel")) {
//		System.out.println("Generators: [");
//		for (Entry<String, Set<FileDetails>> entry : generators.entrySet()) {
//			System.out.println("    " + entry.getKey() + ": [");
//			for (FileDetails details : entry.getValue()) {
//				System.out.println("        " + details);
//			}
//			System.out.println("    ]");
//		}
//		System.out.println("]");
//		
//		System.out.println("Dependencies: [");
//		for (Entry<String, Set<FileDependency>> entry : generatedClassesDependencies.entrySet()) {
//			System.out.println("    " + entry.getKey() + ": [");
//			for (FileDependency dependency : entry.getValue()) {
//				System.out.println("        " + dependency);
//			}
//			System.out.println("    ]");
//		}
//		System.out.println("]");
//	}
				
	}
	
	public void addAncestor(Element ancestor, Element subClass) {
		final TreePath treePath = trees.getPath(ancestor);
		long lastModified = treePath==null? 0 : treePath.getCompilationUnit().getSourceFile().getLastModified();
		
		FileDependency ancestorDependency = FileDependency.newFileDependency(ancestor.asType().toString(), lastModified);
		ancestorDependency.isAncestor = true;

		ancestorDependency.subClasses.add(subClass.asType().toString());
	}

	public Set<String> getAncestorSubClasses(String ancestor) {
		return FileDependency.withGenerator(ancestor).subClasses;
	}
	
	public boolean isAncestor(String possibleAncestor) {
		FileDependency dependency = FileDependency.withGenerator(possibleAncestor);
		return dependency != null && dependency.isAncestor && dependency.isValid;
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
			
			//If generator is an ancestor, add the final classes as generators as well
			FileDependency dependency = FileDependency.withGenerator(generatorClass);
			if (dependency.isAncestor) {
				for (String ancestorSubClass : dependency.subClasses) {
					TypeElement ancestorSubClassElement = environment.getProcessingEnvironment()
							                                         .getElementUtils()
							                                         .getTypeElement(ancestorSubClass);
					if (ancestorSubClassElement != null) {
						addGeneratedClass(clazz, ancestorSubClassElement);
					}
				}
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
			dependency.isValid = true;
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
	
	public Set<String> getGeneratedClasses() {
		return Collections.unmodifiableSet(generatedClassesDependencies.keySet());
	}
	
	public Set<FileDetails> getGeneratedClassesByDependency(String dependency) {
		return generators.get(dependency);
	}
	
	public Set<FileDetails> getFileDetailsList(String clazz) {
		Set<FileDetails> details = new HashSet<>();
		for (FileDependency dependency : generatedClassesDependencies.get(clazz)) {
			Set<FileDetails> generatorFileDetails = generators.get(dependency.generator);
			if (generatorFileDetails != null) {
				details.addAll(generators.get(dependency.generator));
			} else {
				//This should not occur
				System.err.println("Dependency: " + dependency + " doesn't have generators");
			}
		}
		return Collections.unmodifiableSet(details);
	}
	
	public Set<FileDetails> getAutogeneratedClasses() {
		
		if (!generators.containsKey(null)) return new HashSet<>();
		
		Set<FileDetails> details = new HashSet<>(generators.get(null));
		for (FileDetails fileDetails : details) {
			if (generators.containsKey(fileDetails.className)) {
				details.addAll(generators.get(fileDetails.className));
			}
		}
		
		return details;
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
			
			if (!details.isCacheValid()) {
				return false;
			}
			
			return true;
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
		
		//TODO if it is valid, check if this fileDependency has ancestors
		//if so, check that all the ancestors are valid
		
		return dependency.isValid;
    }

	public static class FileDetails implements Serializable {
		
		private static final long serialVersionUID = 1L;
		private static Map<String, FileDetails> fileDetailsMap = new HashMap<>();
		
		private static Executor cacheExecutor = Executors.newFixedThreadPool(8);
		private static AtomicInteger cacheTasksCount = new AtomicInteger(0);
		private static Set<FileDetails> failedGenerations = new HashSet<>();
		
		private String cachedFile;
		private String originalFile;
		private boolean doGenerateJavaCached;
		
		public Map<String, Object> metaData;
		
		//<Cached, Path>
		private Map<String, String> cachedClassFiles;
		private String cachedClassesSearchFolder;
		private boolean cached;
		
		public String className;		
		public boolean isAction;
		
		private transient boolean invalid;
		public transient boolean generated;
		
		public Set<FileDependency> dependencies;
		
		public static boolean isSaving() {
			return cacheTasksCount.get() != 0;
		}
		
		public static Set<FileDetails> getFailedGenerations() {
			return failedGenerations;
		}
		
		public void setGeneratedJavaCache(String cachedFile, String originalFile) {
			this.cachedFile = cachedFile;
			this.originalFile = originalFile;
			
			//If this FileDetails was checked as invalid before, 
			//generating it means that it is valid once again
			invalid = false;
		}
		
		private static FileDetails fromFileDetails(FileDetails from) {
			
			FileDetails newFileDetails = newFileDetails(from.className);
			
			newFileDetails.cachedFile = from.cachedFile;
			newFileDetails.originalFile = from.originalFile;
			newFileDetails.doGenerateJavaCached = from.doGenerateJavaCached;
			
			newFileDetails.cachedClassFiles.putAll(from.cachedClassFiles);
			newFileDetails.cachedClassesSearchFolder = from.cachedClassesSearchFolder;
			
			newFileDetails.cached = from.cached;

			newFileDetails.isAction = from.isAction;
			
			newFileDetails.metaData.putAll(from.metaData);
			
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
				details.metaData = new HashMap<>();
				
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
			
			for (String file : cachedClassFiles.keySet()) {
				if (file != null && new File(file).exists()) {
					try {
						new File(file).delete();
					} catch (Exception e){}
				}				
			}
			
			cachedClassFiles.clear();
			
			cachedClassesSearchFolder = null;
			
			cached = false;
			cachedFile = null;
		}
		
		public void invalidate() {
			invalid = true;
			removeCache();
		}
		
		private void validate() {
			invalid = false;
			if (isAction) {
				Actions.getInstance().addActionHolder(className);
			}
		}
		
		private boolean isCacheValid() {
						
			if (!cached) return false;
			
			if (cachedFile != null) {
				File input = new File(cachedFile);
				if (!input.exists() || !input.canRead() || originalFile==null) {
					cached = false;
				}
			}
			
			for (Entry<String, String> file : cachedClassFiles.entrySet()) {
				if (file != null) {
					File input = new File(file.getKey());
					if (!input.exists() || !input.canRead() || file.getValue()==null) {
						cached = false;
						break;
					}
				}
			}
			
			return cached;
		}
		
		public void generateJavaCached() {
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
			} catch (Exception e) {
				printCacheErrorToLogFile(e);
			}
		}
		
		public void generate() throws FileNotFoundException, IOException {
			
			if (generated) return;
			
			cacheTasksCount.incrementAndGet();
			cacheExecutor.execute(new Runnable() {
				
				@Override
				public void run() {
										
					try {						
						
						for (Entry<String, String> cache : cachedClassFiles.entrySet()) {
							
							File input = new File(cache.getKey());
							File output = new File(cache.getValue());
							
							output.getParentFile().mkdirs();
											
							//Copy the cached file
							FileUtils.copyCompletely(
								new FileInputStream(input),
								new FileOutputStream(output),
								null
							);
						}
						
						//If this FileDetails was checked as invalid before, 
						//generating it means that it is valid once again
						invalid = false; 
						
						doGenerateJavaCached = true;
						
					} catch (Throwable e) {
						failedGenerations.add(FileDetails.this);
						printCacheErrorToLogFile(e);
					} finally {
						cacheTasksCount.decrementAndGet();
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
		
		public boolean isAncestor;
		public Set<String> subClasses;
		
		public transient Boolean isValid;
		
		private static FileDependency withGenerator(String generator) {
			return fileDependencyMap.get(generator);
		}
		
		private static FileDependency fromFileDependency(FileDependency from) {
			FileDependency newFileDependency = newFileDependency(from.generator, from.lastModified);
			newFileDependency.isAncestor = from.isAncestor;
			return newFileDependency;
		}
		
        private static FileDependency newFileDependency(String generator, long lastModified) {
			
			FileDependency dependency = fileDependencyMap.get(generator);
			if (dependency == null) {
				dependency = new FileDependency();
				dependency.subClasses = new HashSet<>();
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
