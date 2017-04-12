package com.dspot.declex.helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
import java.util.jar.JarFile;
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
	
	public static final Option OPTION_CACHE_FILES = new Option("cacheFiles", "false");
	public static final Option OPTION_DEBUG_CACHE = new Option("debugCache", "false");
	public static final Option OPTION_CACHE_FILES_IN_PROCESS = new Option("cacheFilesInProcess", "true");

	private static Executor cacheExecutor = Executors.newSingleThreadExecutor();
	
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
				if (getExternalCacheJar().exists()) getExternalCacheJar().delete();
				
				File cachedClassesFolder = new File(getExternalCache().getAbsolutePath() + File.separator + "java");
				org.apache.commons.io.FileUtils.deleteDirectory(cachedClassesFolder);				
			} catch (Exception e) {}
		}
		
		//Remove all the files related to the cache service
		if (environment.getOptionBooleanValue(OPTION_CACHE_FILES_IN_PROCESS)) {
			getExternalCacheCompilerDone().delete();
			getExternalCacheGenerate().delete();
			getExternalCacheGenerateLock().delete();
		}
	}
	
	public static File getExternalCache() {
		return FileUtils.getPersistenceConfigFile("cache");
	}
	
	private static File getExternalCacheJar() {
		return new File(getExternalCache().getAbsolutePath() + File.separator + "declex_cache.jar");
	}
	
	private static File getExternalCacheIndex() {
		return new File(getExternalCache().getAbsolutePath() + File.separator + "index.dat");
	}

	private static File getExternalCacheGenerate() {
		return new File(getExternalCache().getAbsolutePath() + File.separator + "generate.dat");
	}
	
	private static File getExternalCacheGenerateLock() {
		return new File(getExternalCache().getAbsolutePath() + File.separator + "generate.lock");
	}
	
	private static File getExternalCacheCompilerDone() {
		return new File(getExternalCache().getAbsolutePath() + File.separator + "compiler.done");
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
		Input input = new Input(new FileInputStream(getExternalCacheIndex()), 65536);
		
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
			output = new Output(new FileOutputStream(getExternalCacheIndex()), 65536);
			
			output.writeString(FileDetails.VERSION);
			kryo.writeObject(output, generators, getGeneratorsSerializer());
			
		} catch (Exception e) {
			printCacheErrorToLogFile(e, "-cache");
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
	
	private static void printCacheErrorToLogFile(Throwable e, String id) {
		try {
			File file = new File(getExternalCache().getAbsolutePath() + File.separator + "error" + id + ".log");
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
		
			FileDetails.initialization();
			
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
								
								Set<FileDetails> detailsToInvalidate = new HashSet<>();
								detailsToInvalidate.add(details);
								
								if (generatorsTemp.containsKey(dependency.generator)) {
									detailsToInvalidate.addAll(generatorsTemp.get(dependency.generator));
									generatorsTemp.remove(dependency.generator);
								}
								
								for (FileDetails details2 : detailsToInvalidate) {
									LOGGER.debug(
										"Removing Cached file because its dependency was removed: " + details2.className 
										+ ", dependency : " + dependency 
										+ ". All current cached file dependencies will be invalidated."
									);							
									details2.invalidate();
									details2.dependencies.remove(dependency);		
									
									for (FileDependency dependency2 : details2.dependencies) {
										dependency2.isValid = false;
									}
								}
								
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
	
	public static void runClassCacheCreation() {
				
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
			printCacheErrorToLogFile(e, "-cache");
			return;
		}
		
		//Wait till compiler finishes, a gradle task is needed to create the compiler.done file
		int compilerTimeout = 5 * 60000; //5 minutes timeout
		while (!getExternalCacheCompilerDone().exists() && compilerTimeout > 0) {
			try {
				Thread.sleep(10);
				compilerTimeout -= 10;
			} catch (InterruptedException e) {}			
		}
		while (getExternalCacheCompilerDone().exists() && compilerTimeout > 0) {
			getExternalCacheCompilerDone().delete();
			try {
				Thread.sleep(10);
				compilerTimeout -= 10;
			} catch (InterruptedException e) {}	
		}
		if (compilerTimeout < 0) {
			System.out.println("Compiler Timed Out");
			return;
		}

		System.out.println("Creating DecleX Cache Jar");
		long timeStart = System.currentTimeMillis();
		
		final File tempJarFile = new File(getExternalCache().getAbsolutePath() + File.separator + "declex_cache_temp.jar");
		if (tempJarFile.exists()) tempJarFile.delete();		
		
		JarFile jarFileInput = null;
		
		OutputStream tempJar = null;
		JarOutputStream tempJarOut = null;

		try {

			Manifest manifest = new Manifest();
			Attributes global = manifest.getMainAttributes();
			global.put(Attributes.Name.MANIFEST_VERSION, "1.0");
			global.put(new Attributes.Name("Cache-Version"), FileDetails.VERSION);
			global.put(new Attributes.Name("Created-By"), "DecleX, DSpot Sp. z o.o");

			tempJar = new FileOutputStream(tempJarFile);
			tempJarOut = new JarOutputStream(new BufferedOutputStream(tempJar), manifest);
			final byte[] buf = new byte[64536];

			for (FileDetails details : fileDetails) {
				
				try {
					
					if (details.searchFolderToCache == null) continue;
					
					final String pkg = details.className.substring(0, details.className.lastIndexOf('.'));
					final String className = details.className.substring(details.className.lastIndexOf('.')+1);
					
					if (details.doGenerateJavaCached) {
						details.generateJavaCached();
						details.doGenerateJavaCached = false;
					}
					
					if (details.cached) {
						
						if (jarFileInput == null) {
							jarFileInput = new JarFile(getExternalCacheJar(), false);
						}
						
						for (String cachedFile : details.cachedClasses.keySet()) {
							
							if (details.canBeUpdated) {
								String path = details.className.substring(0, details.className.lastIndexOf('.')+1);
								path = path.replace('.', '/');
								cachedFile = path + cachedFile.substring(cachedFile.lastIndexOf(File.separator) + 1);								
							}
							
							JarEntry entry = jarFileInput.getJarEntry(cachedFile);
							if (entry == null) {
								details.invalidate();
								System.out.println("Removing cached file because it was not found in the cached Jar: " + details.className);
								break;
							}
							
							//Add file to Jar 
							tempJarOut.putNextEntry(new ZipEntry(cachedFile));												
							FileUtils.copyCompletely(jarFileInput.getInputStream(entry), tempJarOut, buf, false);
							tempJarOut.closeEntry();
						}
						
						continue;
					}
					
					final FileFilter fileFilter = new FileFilter() {								
						@Override
						public boolean accept(File pathname) {
							return pathname.getName().matches(className + "(\\$[a-zA-Z0-9_$]+)*\\.class");
						}
					};					
				
					File[] classes = new File(details.searchFolderToCache).listFiles(fileFilter);	
					if (classes.length == 0) {
						System.out.println("Removing cached file because it was not created by the compiler: " + details.className);
						details.invalidate();
						continue;
					}
					
					for (File file : classes) {	
						final String cachedClass = pkg.replace('.', '/') + "/" + file.getName();						
						final String cachedAsFile = getExternalCache().getAbsolutePath() + File.separator
								+ "java" + File.separator + cachedClass.replace("/", File.separator);
						
						OutputStream extraOutput = null;
						if (details.canBeUpdated) {							
							extraOutput = new FileOutputStream(cachedAsFile);
						}
						
						//Add file to Jar
						tempJarOut.putNextEntry(new ZipEntry(cachedClass));												
						FileUtils.copyCompletely(new FileInputStream(file), tempJarOut, extraOutput, buf, false, true);
						tempJarOut.closeEntry();
						
						if (details.canBeUpdated) {
							details.cachedClasses.put(cachedAsFile, file.getAbsolutePath());
						} else {
							details.cachedClasses.put(cachedClass, file.getAbsolutePath());
						}
					}	
					
					details.cached = true;
				
				} catch (Throwable e) {
					details.invalidate();
					System.out.println("Removing from cache: " + details.className + " because of an error. Check \"error.log\" for more info.");
					printCacheErrorToLogFile(e, "-cache");					
				}

			}
			
		} catch (Exception e) {
			printCacheErrorToLogFile(e, "-cache");
		} finally {
			try {												
				
				if (tempJarOut != null) tempJarOut.close();
				if (tempJar != null) tempJar.close();	
				if (jarFileInput != null) jarFileInput.close();
								
				//Copy the cached file
				FileUtils.copyCompletely(
					new FileInputStream(tempJarFile),
					new FileOutputStream(getExternalCacheJar()),
					null
				);
				
				tempJarFile.delete();
				
				System.out.println("Writing cache in: " + (System.currentTimeMillis() - timeStart) + "ms");
				
			} catch (Throwable e) {
				printCacheErrorToLogFile(e, "-cache");
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
					details.searchFolderToCache = Paths.get(fileUri).toFile()
							                                 .getParentFile().getAbsolutePath();
					
					cacheClassesRequired = true;
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}				

		}
		
		saveGenerators(generators);
				
		if (cacheClassesRequired) {
			startDeclexServiceWith("cache");
		}
						
	}
	
	public void ensureSources() {
		FileDetails.finalization();
	}
	
	public void preGenerateSources() {
		generateSources();
	}
	
	private void generateSources() {
		
		if (!environment.getOptionBooleanValue(OPTION_CACHE_FILES_IN_PROCESS)) return;
		
		if (!FileDetails.preGenerateSources.isEmpty()) {
						
			//Wait for any previous instruction to write sources
			int preGenerateWaitTimeout = 60000;
			while (getExternalCacheGenerate().exists() && preGenerateWaitTimeout > 0) {
				try {
					Thread.sleep(10);
					preGenerateWaitTimeout -= 10;
				} catch (InterruptedException e) {}
			}
			
			if (preGenerateWaitTimeout <= 0) {
				LOGGER.error("An error ocurred while caching in different process. "
						+ "\"" + getExternalCacheGenerate().getAbsolutePath() + "\" file was not removed.");
				getExternalCacheGenerate().delete();
				return;
			}
			
			//Run the written in background
			cacheExecutor.execute(new Runnable() {
				
				@Override
				public void run() {
					//Write the preGenerate file with the sources to write
					Output output = null;
					try {
						
						Kryo kryo = getKryo();
						output = new Output(new FileOutputStream(getExternalCacheGenerate()), 8192);
						kryo.writeObject(output, FileDetails.preGenerateSources, new MapSerializer());
						
						startDeclexServiceWith("generate");

						FileDetails.preGenerateSources.clear();

					} catch (Exception e) {
						LOGGER.error("An error ocurred while caching in different process: " + e.getMessage());
					} finally {
						if (output != null) {
							output.close();
						}
					}
				
				}
			});			
		}
	}

	@SuppressWarnings("unchecked")
	public static void runGenerateSources(int retries) {		
		
		retries--;
		
		Map<String, String> generateSources = null;
		//Read the generate file with the sources to write
		Input input = null;
		try {
			
			Kryo kryo = getKryo();
			input = new Input(new FileInputStream(getExternalCacheGenerate()), 8192);
			generateSources = kryo.readObject(input, HashMap.class, new MapSerializer());
							
		} catch (Exception e) {
			printCacheErrorToLogFile(e, "-generate");
			
			//Retry
			if (retries > 0) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {}
				runGenerateSources(retries);
				return;
			}
			
		} finally {
			if (input != null) {
				input.close();
				
				if (generateSources != null) {
					getExternalCacheGenerate().delete();
				}
			}
		}

		if (generateSources != null) {
			
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
			System.out.println("Starting caching process at " + sdf.format(new Date()));
			
			try {
				
				System.out.println("Waiting To Lock File");
				final RandomAccessFile count = new RandomAccessFile(getExternalCacheGenerateLock(), "rw");
				
				//Wait till lock the file
				FileLock lock = null;
				while (lock == null) {
					try {
						lock = count.getChannel().lock();
					} catch (OverlappingFileLockException e) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e1) {}
					}							
				};
				
				System.out.println("Initializing Copy of " + generateSources.size() + " files");
				
				final long timeStart = System.currentTimeMillis();
				
				byte[] copyBuf = new byte[65536];
				JarInputStream cachedJarFileInput = new JarInputStream(
						new BufferedInputStream(new FileInputStream(getExternalCacheJar())), 
						false
					);
				
				JarEntry entry;
				while ((entry = cachedJarFileInput.getNextJarEntry()) != null) {
					if (generateSources.containsKey(entry.getName())) {
						
						try {			
							File output = new File(generateSources.get(entry.getName()));
							
							if (!output.exists()) {
								output.getParentFile().mkdirs();
								
								OutputStream outStream = new FileOutputStream(output);
								
								//Copy the cached file
								FileUtils.copyCompletely(
										cachedJarFileInput, 
										outStream, 
										copyBuf, true, false);										
							}
						} catch (Exception e) {
							printCacheErrorToLogFile(e, "-generate");
							System.out.println("Error writing " + entry.getName() + ", " + e.getMessage());
						} 
					}
				}
				
				System.out.println("Writing cache in: " + (System.currentTimeMillis() - timeStart) + "ms");
				
				if (cachedJarFileInput != null) {
					cachedJarFileInput.close();
					cachedJarFileInput = null;
				}						
				
				lock.release();
				count.close();
				Files.delete(getExternalCacheGenerateLock().toPath());
				
				System.out.println("Finalized Copy");
								
			} catch (Throwable e) {
				e.printStackTrace();
				printCacheErrorToLogFile(e, "-generate");
				
				//Retry
				if (retries > 0) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {}
					runGenerateSources(retries);
					return;
				}
			}

		}
				
	}
	
	private void startDeclexServiceWith(String ... params) {
		try {
			
			final String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
			
			int declexCacheIndex = 0;
			URL[] urls = ((URLClassLoader) environment.getClass().getClassLoader()).getURLs();
			String[] classPathArray = new String[urls.length+1];
			for (int i = 0; i < urls.length; i++) {
				final String urlPath = Paths.get(urls[i].toURI()).toFile().getAbsolutePath();
				classPathArray[i+1] = urlPath;
				
				if (urlPath.equals(getExternalCacheJar().getAbsolutePath())) {
					declexCacheIndex = i+1;
				}
			}
						
			classPathArray[0] = Paths.get(FilesCacheHelper.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
			
			//Remove declex_cache.jar from classpath 
			if (declexCacheIndex != 0) {
				classPathArray[declexCacheIndex] = classPathArray[classPathArray.length-1];
				classPathArray = Arrays.copyOf(classPathArray, classPathArray.length-1);
			}
			
			final String classpath = StringUtils.join(classPathArray, File.pathSeparator);				
				
			String[] cmd = combineString(new String[]
				{
					java,
	                "-classpath", classpath,
	                DeclexProcessor.class.getCanonicalName()
				}, 
				params
			);
			
			Process process = new ProcessBuilder(cmd).start();
			
			//System.out.println("Cache Service Code: " + process.waitFor());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String[] combineString(String[] first, String[] second){
        int length = first.length + second.length;
        String[] result = new String[length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
	
	public void addAncestor(Element ancestor, Element subClass) {
		//Do not accept ancestors of generated elements
		if (generatedClassesDependencies.containsKey(subClass.asType().toString())) {
			return;
		} 
		
		final String subClassName = subClass.asType().toString();
						
		final TreePath treePath = trees.getPath(ancestor);
		FileDependency ancestorDependency = FileDependency.newFileDependency(ancestor.asType().toString());
		ancestorDependency.sourceFile = Paths.get(treePath.getCompilationUnit().getSourceFile().toUri()).toFile().getAbsolutePath();
		ancestorDependency.sourceFileLastModified = new File(ancestorDependency.sourceFile).lastModified();
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
			FileDependency dependency = FileDependency.newFileDependency(generatorClass);
			dependency.sourceFile = Paths.get(treePath.getCompilationUnit().getSourceFile().toUri()).toFile().getAbsolutePath();
			dependency.sourceFileLastModified = new File(dependency.sourceFile).lastModified();
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
		final FileDetails details = FileDetails.newFileDetails(clazz);
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
    	
    	File sourceFile = new File(dependency.sourceFile);
    	if (!sourceFile.exists()) {
    		throw new CacheDependencyRemovedException();
    	}

    	dependency.isValid = sourceFile.lastModified() == dependency.sourceFileLastModified;
		
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
		
		private static AtomicInteger cacheTasksCount = new AtomicInteger(0);
		private static Set<FileDetails> failedGenerations = new HashSet<>();
		
		private static boolean initializing;
		
		private static Map<String, String> preGenerateSources = new HashMap<>();
		
		private static final String VERSION = "1.2.1.14";
		private static Set<String> classFilesInJar;
		private static JarFile cachedFilesJar;
		
		public String className;		

		private String cachedFile;
		private String originalFile;
		private boolean doGenerateJavaCached;
		
		public Map<String, Object> metaData;
		
		//<Cached, Path>
		private Map<String, String> cachedClasses;
		private String searchFolderToCache;
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
			this.cachedClasses = new HashMap<>();
			this.metaData = new HashMap<>();
		}
		
		private static void initialization() {
			initializing = true;
			cacheExecutor.execute(new Runnable() {
				
				@Override
				public void run() {
					InputStream jar = null;
					try {					
						jar = new FileInputStream(getExternalCacheJar());
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
						if (jar != null) {
							try {
								jar.close();
							} catch (IOException e) {}
						}
						initializing = false;
					}
				}
			});
		}
		
		private static void finalization() {
			if (cachedFilesJar != null) {
				try {
					cachedFilesJar.close();
				} catch (IOException e) {}
			}
		}
		
		@Override
		public void write(Kryo kryo, Output output) {
			output.writeString(cachedFile);
			output.writeString(originalFile);
			output.writeBoolean(doGenerateJavaCached);
			
			kryo.writeObject(output, metaData, new MapSerializer());
			
			kryo.writeObject(output, cachedClasses, new MapSerializer());
			output.writeString(searchFolderToCache);
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
			
			cachedClasses = kryo.readObject(input, HashMap.class, new MapSerializer());
			searchFolderToCache = input.readString();			
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
			
			cachedClasses.clear();
			
			searchFolderToCache = null;
			
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
			} else {
				if (!isInner) {
					cached = false;
				}
			}
			
			if (cached) {
				String path = className.substring(0, className.lastIndexOf('.')+1);
				path = path.replace('.', '/');
								
				for (Entry<String, String> file : cachedClasses.entrySet()) {
					
					if (file.getValue()==null) {
						cached = false;
						break;
					}
					
					String classFileName = file.getKey();
					if (canBeUpdated) {
						File input = new File(file.getKey());
						if (!input.exists() || !input.canRead()) {
							cached = false;
							break;
						}
						
						classFileName = path + file.getValue().substring(file.getValue().lastIndexOf(File.separator) + 1);
					}
					
					if (!classFilesInJar.contains(classFileName)) {
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
				printCacheErrorToLogFile(e, "-cache");
			}
		}
		
		public void preGenerate(AndroidAnnotationsEnvironment environment) {
			
			if (preGenerated) return;
			preGenerated = true;
						
			if (environment.getOptionBooleanValue(OPTION_CACHE_FILES_IN_PROCESS) && !canBeUpdated) {
				for (Entry<String, String> cachedClass : cachedClasses.entrySet()) {
					
					preGenerateSources.put(
						cachedClass.getKey(), 
						canBeUpdated? "canBeUpdated:" + cachedClass.getValue() : cachedClass.getValue() 
					);
				}
				
				//If this FileDetails was checked as invalid before, 
				//generating it means that it is valid once again
				invalid = false; 
				
				doGenerateJavaCached = true;

			} else {
				cacheTasksCount.incrementAndGet();
				cacheExecutor.execute(new Runnable() {
					
					@Override
					public void run() {
											
						try {		
							
							for (Entry<String, String> cache : cachedClasses.entrySet()) {
								
								File output = new File(cache.getValue());
								if (!output.exists()) {
									
									output.getParentFile().mkdirs();
									
									if (!canBeUpdated) {
										if (cachedFilesJar == null) {
											cachedFilesJar = new JarFile(getExternalCacheJar());
										}
										JarEntry entry = cachedFilesJar.getJarEntry(cache.getKey());
														
										//Copy the cached file
										FileUtils.copyCompletely(
											cachedFilesJar.getInputStream(entry),
											new FileOutputStream(output),
											null
										);
									} else {
										
										File input = new File(cache.getKey());
										
										//Copy the cached file
										FileUtils.copyCompletely(
											new FileInputStream(input),
											new FileOutputStream(output),
											null
										);
									}
								}
							}
							
							//If this FileDetails was checked as invalid before, 
							//generating it means that it is valid once again
							invalid = false; 
							
							doGenerateJavaCached = true;
							
						} catch (Throwable e) {
							failedGenerations.add(FileDetails.this);
							printCacheErrorToLogFile(e, "-cache");
						} finally {
							cacheTasksCount.decrementAndGet();
						}
					}
				});				
			}
			
		}
		
		public void generate(AndroidAnnotationsEnvironment environment) {
			
			if (generated) return;
			generated = true;
			
			preGenerate(environment);
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
					+ (cachedClasses.isEmpty()? "" : " \n            Cache Classes: " + cachedClasses)
					+ (metaData.isEmpty()? "" : " \n            MetaData: " + metaData)
					+ (adi==null || adi.isEmpty()? "" : " \n            ADI: " + adi);
		}
	}
	
	public static class FileDependency implements KryoSerializable {

		private static Map<String, FileDependency> fileDependencyMap = new HashMap<>();
		
		private String generator;
		
		private String sourceFile;
		private long sourceFileLastModified;
		
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
			
			output.writeString(sourceFile);
			output.writeLong(sourceFileLastModified);
			
			output.writeBoolean(isAncestor);
			kryo.writeObject(output, subClasses, new CollectionSerializer());
			
			output.writeBoolean(isAction);
			
			kryo.writeObjectOrNull(output, adi, new CollectionSerializer());
		}

		@Override
		@SuppressWarnings("unchecked")
		public void read(Kryo kryo, Input input) {
			
			sourceFile = input.readString();
			sourceFileLastModified = input.readLong();
			
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
					super.write(kryo, output, object);
				}
				
				@Override
				public KryoSerializable read(Kryo kryo, Input input,Class<KryoSerializable> type) {
					
					String generator = input.readString();
					KryoSerializable object = newFileDependency(generator);
					
					kryo.reference(object);
					object.read(kryo, input);
					
					return object;
				}
			};
		}
		
		private static FileDependency withGenerator(String generator) {
			return fileDependencyMap.get(generator);
		}
		
        private static FileDependency newFileDependency(String generator) {
			
			FileDependency dependency = fileDependencyMap.get(generator);
			if (dependency == null) {
				dependency = new FileDependency(generator);
				fileDependencyMap.put(generator, dependency);
			}
			
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

		private static final long serialVersionUID = 1L;
		
	}
}
