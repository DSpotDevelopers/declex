package com.dspot.declex.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
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
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.Option;
import org.androidannotations.holder.GeneratedClassHolder;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import com.dspot.declex.DeclexProcessor;
import com.dspot.declex.action.Actions;
import com.dspot.declex.util.FileUtils;
import com.dspot.declex.util.TypeUtils;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.KryoSerializableSerializer;
import com.esotericsoftware.kryo.serializers.MapSerializer;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

public class FilesCacheHelper {
	
	private static Logger LOGGER = LoggerFactory.getLogger(FilesCacheHelper.class);
	
	private static long DELAY_AFTER_COMPILER_WAIT = 2000;//In ms
	
	public static final Option OPTION_CACHE_FILES = new Option("cacheFiles", "false");
	public static final Option OPTION_CACHE_FILES_COMPILER_WAIT = new Option("cacheFilesCompilerWaitTimeout", "30");
	public static final Option OPTION_DEBUG_CACHE = new Option("debugCache", "false");
	
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

			String java = "\"" + System.getProperty("java.home") + File.separator + "bin" + File.separator + "java\"";

			Path thisJarPath = null;
			try {
				thisJarPath = Paths.get(FilesCacheHelper.class.getProtectionDomain().getCodeSource().getLocation().toURI());
				
				Process p = Runtime.getRuntime().exec(java + " -jar \"" + thisJarPath + "\"");
				
				BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
				
				String line = in.readLine();
				serviceConnectionPassed = "DecleX Service".equals(line);
				
			} catch (Exception e) {
				
			}
			
			if (!serviceConnectionPassed) {
				LOGGER.warn("Error connecting to DecleX service, please ensure that you get \"DecleX Service\" "
						    + " when you run the command in console \"" + java + "\" -jar \"" + thisJarPath + "\"");
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
	
	public static File getExternalCache() {
		return FileUtils.getPersistenceConfigFile("cache");
	}
	
	private static File getExternalCacheIndex() {
		return new File(getExternalCache().getAbsolutePath() + File.separator + "index.dat");
	}
	
	private static Kryo getKryo() {
		Kryo kryo = new Kryo();
		
		kryo.register(FileDetails.class);
		kryo.register(FileDependency.class);
		
		return kryo;
	}
	
	private static Serializer<?> getGeneratorsSerializer() {
		MapSerializer serializer = new MapSerializer();
		serializer.setValueClass(
			HashSet.class, 
			new CollectionSerializer(FileDetails.class, FileDetails.getSerializer())
		);
		
		return serializer;
	}
	
	@SuppressWarnings("unchecked")
	private static Map<String, Set<FileDetails>> loadGenerators() throws Exception {
		
		Map<String, Set<FileDetails>> generatorsTemp = null;

		File externalCache = getExternalCacheIndex();
		if (!externalCache.exists()) return null;
		
		Kryo kryo = getKryo();
		Input input = new Input(new FileInputStream(getExternalCacheIndex()));
		
		try {
			String version = input.readString();
			if (version == null || !version.equals(FileDetails.VERSION)) {
				throw new RuntimeException("Index File Version mismatch");
			}
			
			generatorsTemp = kryo.readObject(input, HashMap.class, getGeneratorsSerializer());
		} finally {
			input.close();
		}
				
		return generatorsTemp;
	}
	
	private static void saveGenerators(Map<String, Set<FileDetails>> generators) {
		
		Output output = null;
		try {
			
			Kryo kryo = getKryo();
			output = new Output(new FileOutputStream(getExternalCacheIndex()));
			
			output.writeString(FileDetails.VERSION);
			kryo.writeObject(output, generators, getGeneratorsSerializer());
			
			output.close();
			
		} catch (Exception e) {
			printCacheErrorToLogFile(e);
		} finally {
			if (output != null) {
				output.close();
			}
		}
	}
	
	private static void clearCacheErrorLogFile() {
		try {
			File file = new File(getExternalCache().getAbsolutePath() + File.separator + "error.log");
			file.delete();
		} catch (Exception e1){}		
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
				
		Map<String, Set<FileDetails>> generatorsTemp = null;		
		try {
			generatorsTemp = loadGenerators();
		} catch (Exception e) {
			LOGGER.debug("Error loading cache index file: {}", e.getMessage());
		}		
		
		if (generatorsTemp != null) {
		
			FileDetails.initialize();
			
			boolean generatorsScanRequired = true;
			scanGenerators: while (generatorsScanRequired) {
		
				generatedClassesDependencies.clear();
				generators.clear();

				generatorsScanRequired = false;
				
				for (Entry<String, Set<FileDetails>> generatorEntry: generatorsTemp.entrySet()) {
					
					Set<FileDetails> fileDetailsList = generators.get(generatorEntry.getKey());
					if (fileDetailsList == null) {
						fileDetailsList = new HashSet<>();					
					}
					
					for (FileDetails details : generatorEntry.getValue()) {
						
						if (details.invalid) {
							continue;
						}
						
						if (isCacheFilesEnabled()) {
							if (!details.isCacheValid()) {
								LOGGER.debug("Removing Cached file because its cache is invalid: " + details.className 
										     + ". This will invalidate all its dependencies");
								details.invalidate();
								
								//Invalidate all the dependencies so that the class be generated again
								for (FileDependency dependency : details.dependencies) {
									dependency.isValid = false;
								}
								
								generatorsScanRequired = true;
								continue scanGenerators;
							}
						} else {
							details.removeCache();
						}	
						
						//If the generator was modified, remove this FileDetails
						for (FileDependency dependency : details.dependencies) {
							try {
								if (!isFileDependencyValid(dependency, environment, trees)) {
									LOGGER.debug(
										"Removing Cached file because its dependency changed: " + details.className 
										+ ", dependency : " + dependency 
										+ (dependency.isAncestor? ". This is an ancestor dependency, all current cached file dependencies will be invalidated." : "")
									);							
									details.invalidate();
									
									//If the dependency is an ancestor all the dependencies should be invalidated
									if (dependency.isAncestor) {
										//Invalidate all the dependencies so that the class be generated again
										for (FileDependency dependency2 : details.dependencies) {
											dependency2.isValid = false;
										}
										
										generatorsScanRequired = true;
										continue scanGenerators;
									}
									
								}														
							} catch (CacheDependencyRemovedException e) {
								LOGGER.debug(
									"Removing Cached file because its dependency was removed: " + details.className 
									+ ", dependency : " + dependency 
									+ ". All current cached file dependencies will be invalidated."
								);							
								details.invalidate();
																
								details.dependencies.remove(dependency);
								generatorsScanRequired = true;
								continue scanGenerators;
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
		
		if (environment.getOptionBooleanValue(OPTION_DEBUG_CACHE)) {
			System.out.println("Generators: [");
			for (Entry<String, Set<FileDetails>> entry : generators.entrySet()) {
				System.out.println("    " + entry.getKey() + ": [");
				for (FileDetails details : entry.getValue()) {
					System.out.println("        " + details);
				}
				System.out.println("    ]");
			}
			System.out.println("]");
			
			System.out.println("Dependencies: [");
			for (Entry<String, Set<FileDependency>> entry : generatedClassesDependencies.entrySet()) {
				System.out.println("    " + entry.getKey() + ": [");
				for (FileDependency dependency : entry.getValue()) {
					System.out.println("        " + dependency);
				}
				System.out.println("    ]");
			}
			System.out.println("]");
		}
		
	}
	
	public void validateCurrentCache() {
		if (isCacheFilesEnabled()) {
			for (FileDetails details : FileDetails.fileDetailsMap.values()) {
				if (!details.invalid) {
					details.validate(environment);
				}
			}
			for (FileDependency dependency : FileDependency.fileDependencyMap.values()) {
				if (dependency.isValid != null && dependency.isValid) {
					dependency.validate(environment);
				}
			}
		}
	}
	
	public static void runClassCacheCreation(int compilerWaitTime) {
		
		clearCacheErrorLogFile();
		
		Set<FileDetails> fileDetails = new HashSet<>();
		Set<FileDependency> fileDependencies = new HashSet<>();
		Map<String, Set<FileDetails>> generators = new HashMap<>();
		
		System.out.println("Loading Cached File Details");
		
		try {
			Map<String, Set<FileDetails>> generatorsTemp = loadGenerators();
			if (generatorsTemp != null) {
				for (Entry<String, Set<FileDetails>> generatorEntry: generatorsTemp.entrySet()) {
					
					Set<FileDetails> fileDetailsList = generators.get(generatorEntry.getKey());
					if (fileDetailsList == null) {
						fileDetailsList = new HashSet<>();					
					}
					
					for (FileDetails details : generatorEntry.getValue()) {
						fileDetailsList.add(details);
						fileDetails.add(details);
						
						//Create file dependencies
						for (FileDependency dependency : details.dependencies) {
							fileDependencies.add(dependency);			
						}
						
					}
					
					generators.put(generatorEntry.getKey(), fileDetailsList);
				}
			} else {
				System.out.println("Cached Files Index doesn't exists or it is corrupted");
				return;
			}
		} catch (Throwable e) {
			printCacheErrorToLogFile(e);
			return;
		}

		System.out.println("Creating DecleX Cache Jar");
		
		final File tempJarFile = new File(getExternalCache().getAbsolutePath() + File.separator + "declex_cache_temp.jar");
		if (tempJarFile.exists()) tempJarFile.delete();		
		
		FileOutputStream jar = null;
		JarOutputStream jarOut = null;

		try {

			Manifest manifest = new Manifest();
			Attributes global = manifest.getMainAttributes();
			global.put(Attributes.Name.MANIFEST_VERSION, "1.0");
			global.put(new Attributes.Name("Cache-Version"), FileDetails.VERSION);
			global.put(new Attributes.Name("Created-By"), "DecleX, DSpot Sp. z o.o");

			jar = new FileOutputStream(tempJarFile);
			jarOut = new JarOutputStream(jar, manifest);

			detailsLoop: for (FileDetails details : fileDetails) {
				
				try {
					
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
					
					//Wait until the .class files be created	
					boolean wasWaiting = false;
					while (!new File(details.cachedClassesSearchFolder + File.separator + className + ".class").exists()) {
						
						if (!wasWaiting) {
							System.out.println("Waiting for: " + details.cachedClassesSearchFolder + File.separator + className + ".class");
						}
						
						//Maximum time waiting for compiler
						if (compilerWaitTime <= 0) {
							details.invalidate();
							System.out.println("Removing from cache: " + details.className);
							continue detailsLoop;
						}
						
						Thread.sleep(100);
						compilerWaitTime -= 100;
						wasWaiting = true;
					};
					
					if (wasWaiting) {
						//Just to be sure all is written by the compiler
						Thread.sleep(DELAY_AFTER_COMPILER_WAIT);
					}
					
					final FileFilter fileFilter = new FileFilter() {								
						@Override
						public boolean accept(File pathname) {
							return pathname.getName().matches(className + "(\\$[a-zA-Z0-9_$]+)*\\.class");
						}
					};					
				
					//Cache all the generated .class files
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
					details.invalidate();
					System.out.println("Removing from cache: " + details.className + " because of an error. Check \"error.log\" for more info.");
					printCacheErrorToLogFile(e);					
				}

			}
			
		} catch (Exception e) {
			printCacheErrorToLogFile(e);
		} finally {
			try {												
				
				if (jarOut != null) jarOut.close();
				if (jar != null) jar.close();
				
				//Wait until the dependencies .class files be created
				for (FileDependency dependency : fileDependencies) {

					if (dependency.generatedClassFile == null) continue;

					boolean wasWaiting = false;
					while (!new File(dependency.generatedClassFile).exists()) {
						
						if (!wasWaiting) {
							System.out.println("Waiting for Dependency: " + dependency.generator);
						}

						//Maximum time waiting for compiler
						if (compilerWaitTime <= 0) {
							break;
						}
						
						Thread.sleep(100);
						compilerWaitTime -= 100;
						wasWaiting = true;
					};
					
					if (wasWaiting) {
						//Just to be sure all is written by the compiler
						Thread.sleep(DELAY_AFTER_COMPILER_WAIT);
					}	
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
						if (!details.isInner) {
							LOGGER.warn(
								"Removing Cached Reference because it was not generated: " + details.className
							);
							details.invalidate();
							continue;
						} else {
							details.cached = true;
						}
					}
					
					if (details.invalid && !details.isInner) {
						LOGGER.warn(
								"Cached Reference was invalidated but was not regenerated: " + details.className
							);
						continue;
					}
					if (!generatedClassesDependencies.containsKey(clazz)) continue;
					if (details.isInner) continue;
					
					final String pkg = clazz.substring(0, clazz.lastIndexOf('.'));
					final String resourceName = clazz.substring(clazz.lastIndexOf('.')+1) + ".class";
					
					FileObject classFile = filer.getResource(StandardLocation.CLASS_OUTPUT, pkg, resourceName);
					
					// Cache the .class file
					final URI fileUri = classFile.toUri();					
					details.cachedClassesSearchFolder = Paths.get(fileUri).toFile()
							                                 .getParentFile().getAbsolutePath();
					
					cacheClassesRequired = true;
				}
				
				for (FileDependency dependency : FileDependency.fileDependencyMap.values()) {
					if (dependency.isValid != null && dependency.isValid) {
						final String clazz = dependency.generator;
						final String pkg = clazz.substring(0, clazz.lastIndexOf('.'));
						final String resourceName = clazz.substring(clazz.lastIndexOf('.')+1) + ".class";						
						FileObject classFile = filer.getResource(StandardLocation.CLASS_OUTPUT, pkg, resourceName);
						dependency.generatedClassFile = Paths.get(classFile.toUri()).toFile().getAbsolutePath();
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}				

		}
		
		saveGenerators(generators);
				
		if (cacheClassesRequired) {
			try {
				
				final String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
				
				URL[] urls = ((URLClassLoader) environment.getClass().getClassLoader()).getURLs();
				String[] classPathArray = new String[urls.length+1];
				for (int i = 0; i < urls.length; i++) {
					classPathArray[i+1] = Paths.get(urls[i].toURI()).toFile().getAbsolutePath();
				}
				classPathArray[0] = Paths.get(FilesCacheHelper.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(); 
				
				final String classpath = StringUtils.join(classPathArray, File.pathSeparator);				
				String compilerWaitTime = environment.getOptionValue(OPTION_CACHE_FILES_COMPILER_WAIT);				
				
				Process process = new ProcessBuilder(
		                java,
		                "-classpath", classpath,
		                DeclexProcessor.class.getCanonicalName(),
		                "cache",
		                compilerWaitTime
					).start();
				
				//System.out.println("Cache Service Code: " + process.waitFor());
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
						
	}
	
	public void addAncestor(Element ancestor, Element subClass) {
		//Do not accept ancestors of generated elements
		if (generatedClassesDependencies.containsKey(subClass.asType().toString())) {
			return;
		} 
		
		final String subClassName = subClass.asType().toString();
						
		final TreePath treePath = trees.getPath(ancestor);
		long lastModified = treePath==null? 0 : treePath.getCompilationUnit().getSourceFile().getLastModified();
		
		FileDependency ancestorDependency = FileDependency.newFileDependency(ancestor.asType().toString(), lastModified);
		ancestorDependency.isAncestor = true;
		ancestorDependency.isValid = true;
		ancestorDependency.adi = environment.getADIOnElement(ancestor);
		
		ancestorDependency.subClasses.add(subClassName);
	}

	public Set<String> getAncestorSubClasses(String ancestor) {
		return FileDependency.withGenerator(ancestor).subClasses;
	}
	
	public boolean isAncestor(String possibleAncestor) {
		FileDependency dependency = FileDependency.withGenerator(possibleAncestor);
		return dependency != null && dependency.isAncestor && dependency.isValid != null && dependency.isValid;
	}
	
	public void addGeneratedClass(String clazz, Element generator) {
		this.addGeneratedClass(clazz, generator, false);
	}
	
	public void addGeneratedClass(String clazz, Element generator, boolean canBeUpdated) {
		
		if (generator != null && !(generator instanceof TypeElement)) {
			throw new RuntimeException("Element " + generator + " should be a TypeElement");
		}
		
		boolean isInner = generator != null 
				&& !generator.getEnclosingElement().getKind().equals(ElementKind.PACKAGE);
		if (isInner) {
			generator = TypeUtils.getRootElement(generator);
		}
		
		final String generatorClass = generator == null? null : generator.asType().toString();
		
		Set<FileDependency> dependencies = new HashSet<>();
		if (generator != null) {
			final TreePath treePath = trees.getPath(generator);
			long lastModified = treePath==null? 0 : treePath.getCompilationUnit().getSourceFile().getLastModified();
			
			FileDependency dependency = FileDependency.newFileDependency(generatorClass, lastModified); 
			dependency.adi = environment.getADIForClass(generatorClass);
			dependencies.add(dependency); 
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
		details.isInner = isInner;
		details.canBeUpdated = canBeUpdated;
		details.dependencies.addAll(dependencies);
		details.adi = environment.getADIForClass(clazz);
		generatedClassesDependencies.put(clazz, details.dependencies);
		
		//If canBeUpdated and a class is added, then invalidate it
		if (canBeUpdated) {
			details.invalidate();			
		}
		
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
				LOGGER.error("Dependency: " + dependency + " doesn't have generators");
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
	
	public FileDependency getFileDependency(String clazz) {
		return FileDependency.withGenerator(clazz);
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
	
	private boolean isFileDependencyValid(FileDependency dependency, AndroidAnnotationsEnvironment environment, Trees trees) throws CacheDependencyRemovedException {
    	if (dependency.isValid != null) return dependency.isValid;
    	TypeElement generatorElement = environment.getProcessingEnvironment().getElementUtils()
						  					      .getTypeElement(dependency.generator);

		if (generatorElement == null) {
			throw new CacheDependencyRemovedException();
		} else {
			dependency.isValid = dependency.lastModified == trees.getPath(generatorElement)
									       .getCompilationUnit()
    			    					   .getSourceFile().getLastModified();
		}
		
		if (dependency.isValid && dependency.isAncestor) {
			
			for (String subClass : dependency.subClasses) {
				
				Element subClassElement = environment.getProcessingEnvironment().getElementUtils().getTypeElement(subClass);
				if (subClassElement == null) {
					dependency.isValid = false;
					dependency.subClasses.clear();
					return false;
				}				

				//Check if "dependency" continue being a super class for this subClass
				if (!TypeUtils.isSubtype(subClassElement, dependency.generator, environment.getProcessingEnvironment())) {
					dependency.isValid = false;
					dependency.subClasses.clear();
					return false;
				}
								
			}
			
		}
				
		return dependency.isValid;
    }

	public static class FileDetails implements KryoSerializable {
		
		private static Map<String, FileDetails> fileDetailsMap = new HashMap<>();
		
		private static Executor cacheExecutor = Executors.newFixedThreadPool(8);
		private static AtomicInteger cacheTasksCount = new AtomicInteger(0);
		private static Set<FileDetails> failedGenerations = new HashSet<>();
		
		private static boolean initializing;
		
		private static final String VERSION = "1.0.0";
		private static Set<String> classFilesInJar;

		public String className;		

		private String cachedFile;
		private String originalFile;
		private boolean doGenerateJavaCached;
		
		public Map<String, Object> metaData;
		
		//<Cached, Path>
		private Map<String, String> cachedClassFiles;
		private String cachedClassesSearchFolder;
		private boolean cached;
		
		public boolean isAction;
		public boolean isInner;
		public boolean canBeUpdated; //Used to indicate that these details can be updated during processing
		
		private Set<Class<? extends Annotation>> adi; //Used for Annotations Dependency Injection
		
		public Set<FileDependency> dependencies;
		
		private transient boolean invalid;
		public transient boolean generated;
		private transient boolean preGenerated;
		
		private FileDetails(String className) {
			this.dependencies = new HashSet<>();
			this.className = className;				
			this.cachedClassFiles = new HashMap<>();
			this.metaData = new HashMap<>();
		}
		
		private static void initialize() {
			initializing = true;
			cacheExecutor.execute(new Runnable() {
				
				@Override
				public void run() {
					try {					
						final File jarFile = new File(getExternalCache().getAbsolutePath() + File.separator + "declex_cache.jar");
						InputStream jar = new FileInputStream(jarFile);
						JarInputStream jarIn = new JarInputStream(jar);
						
						Manifest manifest = jarIn.getManifest();
						String jarVersion = manifest.getMainAttributes().getValue(new Attributes.Name("Cache-Version"));
						if (jarVersion == null || !jarVersion.equals(VERSION)) {
							LOGGER.warn("Cache Jar File Error: Different Version");
							classFilesInJar = null;
							return;
						}
						
						classFilesInJar = new HashSet<>();
						
						JarEntry entry;
						while ((entry = jarIn.getNextJarEntry() ) != null) {
							classFilesInJar.add(entry.getName());
						}
						jarIn.close();
						
					} catch (Throwable e) {
						LOGGER.warn("Cache Jar File Error: {}", e.getMessage());
						classFilesInJar = null;
					} finally {
						initializing = false;
					}
				}
			});
		}
		
		@Override
		public void write(Kryo kryo, Output output) {
			output.writeString(cachedFile);
			output.writeString(originalFile);
			output.writeBoolean(doGenerateJavaCached);
			
			kryo.writeObject(output, metaData, new MapSerializer());
			
			kryo.writeObject(output, cachedClassFiles, new MapSerializer());
			output.writeString(cachedClassesSearchFolder);
			output.writeBoolean(cached);
			
			output.writeBoolean(isAction);
			output.writeBoolean(isInner);
			output.writeBoolean(canBeUpdated);
			
			kryo.writeObjectOrNull(output, adi, new CollectionSerializer());
			
			kryo.writeObject(
				output, 
				dependencies,
				new CollectionSerializer(FileDependency.class, FileDependency.getSerializer())
			);
		}

		@Override
		@SuppressWarnings("unchecked")
		public void read(Kryo kryo, Input input) {
			cachedFile = input.readString();
			originalFile = input.readString();			
			doGenerateJavaCached = input.readBoolean();
			
			metaData = kryo.readObject(input, HashMap.class, new MapSerializer());
			
			cachedClassFiles = kryo.readObject(input, HashMap.class, new MapSerializer());
			cachedClassesSearchFolder = input.readString();			
			cached = input.readBoolean();
			
			isAction = input.readBoolean();
			isInner = input.readBoolean();
			canBeUpdated = input.readBoolean();
			
			adi = kryo.readObjectOrNull(input, HashSet.class, new CollectionSerializer());
			
			dependencies = kryo.readObject(
				input, 
				HashSet.class,
				new CollectionSerializer(FileDependency.class, FileDependency.getSerializer())
			);
		}
		
		private static Serializer<?> getSerializer() {
			return new KryoSerializableSerializer() {
				@Override
				public void write(Kryo kryo, Output output, KryoSerializable object) {
					output.writeString(((FileDetails)object).className);
					super.write(kryo, output, object);
				}
				
				@Override
				public KryoSerializable read(Kryo kryo, Input input,Class<KryoSerializable> type) {
					String className = input.readString();
					KryoSerializable object = newFileDetails(className);
					
					kryo.reference(object);
					object.read(kryo, input);
					
					return object;
				}
			};
		}
		
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
		
		private static FileDetails newFileDetails(String className) {
			
			FileDetails details = fileDetailsMap.get(className);
			if (details == null) {
				details = new FileDetails(className);					
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
		
		private void validate(AndroidAnnotationsEnvironment environment) {
			invalid = false;
			
			if (isAction) {
				Actions.getInstance().addActionHolder(className);
			}
			
			if (adi != null && !adi.isEmpty()) {
				for (Class<? extends Annotation> dependency : adi) {
					environment.addAnnotationToADI(className, dependency);
				}
			}
		}
		
		private boolean isCacheValid() {
			
			while (initializing) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {}
			}
						
			if (!cached || classFilesInJar == null) return false;
			
			if (cachedFile != null) {
				File input = new File(cachedFile);
				if (!input.exists() || !input.canRead() || originalFile==null) {
					cached = false;
				}
			}
			
			if (cached) {
				String path = className.substring(0, className.lastIndexOf('.')+1);
				path = path.replace('.', '/');
								
				for (Entry<String, String> file : cachedClassFiles.entrySet()) {
					if (file != null) {
						File input = new File(file.getKey());
						if (!input.exists() || !input.canRead() || file.getValue()==null) {
							cached = false;
							break;
						}
						
						String classFileName = file.getValue().substring(file.getValue().lastIndexOf(File.separator) + 1);
						if (!classFilesInJar.contains(path + classFileName)) {
							cached = false;
							break;
						}
						
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
		
		public void preGenerate() {
			
			if (preGenerated) return;
			preGenerated = true;
			
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
		}
		
		public void generate() {
			
			if (generated) return;
			generated = true;
			
			preGenerate();
		}
				
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof FileDetails)) return false;
			return this.className.equals(((FileDetails)obj).className);
		}
		
		@Override
		public String toString() {
			return this.className 
					+ (dependencies.isEmpty()? "" : " \n            Dependencies: " + dependencies)
					+ (cachedClassFiles.isEmpty()? "" : " \n            Cache Classes: " + cachedClassFiles)
					+ (metaData.isEmpty()? "" : " \n            MetaData: " + metaData)
					+ (adi==null || adi.isEmpty()? "" : " \n            ADI: " + adi);
		}
	}
	
	public static class FileDependency implements KryoSerializable {

		private static Map<String, FileDependency> fileDependencyMap = new HashMap<>();
		
		private String generator;
		private long lastModified;
		
		private String generatedClassFile;
		
		private boolean isAncestor;
		private Set<String> subClasses;
		
		public boolean isAction;
		
		private Set<Class<? extends Annotation>> adi; //Used for Annotations Dependency Injection
		
		private transient Boolean isValid;
		
		private FileDependency(String generator) {
			this.generator = generator;			
			this.subClasses = new HashSet<>();
		}
		
		@Override
		public void write(Kryo kryo, Output output) {
			output.writeString(generatedClassFile);
			
			output.writeBoolean(isAncestor);
			kryo.writeObject(output, subClasses, new CollectionSerializer());
			
			output.writeBoolean(isAction);
			
			kryo.writeObjectOrNull(output, adi, new CollectionSerializer());
		}

		@Override
		@SuppressWarnings("unchecked")
		public void read(Kryo kryo, Input input) {
			generatedClassFile = input.readString();
			
			isAncestor = input.readBoolean();
			subClasses = kryo.readObject(input, HashSet.class, new CollectionSerializer());
			
			isAction = input.readBoolean();		
			
			adi = kryo.readObjectOrNull(input, HashSet.class, new CollectionSerializer());
		}
		
		private static Serializer<?> getSerializer() {
			return new KryoSerializableSerializer() {
				@Override
				public void write(Kryo kryo, Output output, KryoSerializable object) {
					output.writeString(((FileDependency)object).generator);
					output.writeLong(((FileDependency)object).lastModified);
					super.write(kryo, output, object);
				}
				
				@Override
				public KryoSerializable read(Kryo kryo, Input input,Class<KryoSerializable> type) {
					
					String generator = input.readString();
					long lastModified = input.readLong();
					KryoSerializable object = newFileDependency(generator, lastModified);
					
					kryo.reference(object);
					object.read(kryo, input);
					
					return object;
				}
			};
		}
		
		private static FileDependency withGenerator(String generator) {
			return fileDependencyMap.get(generator);
		}
		
        private static FileDependency newFileDependency(String generator, long lastModified) {
			
			FileDependency dependency = fileDependencyMap.get(generator);
			if (dependency == null) {
				dependency = new FileDependency(generator);
				fileDependencyMap.put(generator, dependency);
			}
			
			dependency.lastModified = lastModified;
			
			return dependency;
		}
        
        private void validate(AndroidAnnotationsEnvironment environment) {
			if (isAction) {
				Actions.getInstance().addActionHolder(generator);
			}
			
			if (adi != null && !adi.isEmpty()) {
				for (Class<? extends Annotation> dependency : adi) {
					environment.addAnnotationToADI(generator, dependency);
				}
			}
		}
        
        @Override
		public boolean equals(Object obj) {
			if (!(obj instanceof FileDependency)) return false;
			return this.generator.equals(((FileDependency)obj).generator);
		}
        
        @Override
        public String toString() {
        	return this.generator + (subClasses.isEmpty()? "" : ", SubClasses" + subClasses)
        			+ (adi==null || adi.isEmpty()? "" : ", ADI: " + adi);
        }
        		
	}
	
	private class CacheDependencyRemovedException extends Exception {
		
	}
}
