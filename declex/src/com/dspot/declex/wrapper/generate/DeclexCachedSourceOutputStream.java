package com.dspot.declex.wrapper.generate;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.tools.JavaFileObject;

import org.androidannotations.helper.FilesCacheHelper;
import org.androidannotations.helper.FilesCacheHelper.FileDetails;
import com.dspot.declex.util.FileUtils;

public class DeclexCachedSourceOutputStream extends OutputStream {

	private FilesCacheHelper cacheHelper;
	private JavaFileObject sourceFile;
	private OutputStream wrappedStream;
	private String className;
	
	private static Executor cacheExecutor = Executors.newFixedThreadPool(8);

	public DeclexCachedSourceOutputStream(JavaFileObject sourceFile, String className)
			throws IOException {
		this.sourceFile = sourceFile;
		this.wrappedStream = sourceFile.openOutputStream();
		this.className = className;
		
		this.cacheHelper = FilesCacheHelper.getInstance();
	}

	@Override
	public void write(int b) throws IOException {
		wrappedStream.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		wrappedStream.write(b, off, len);
	}

	@Override
	public void write(byte[] b) throws IOException {
		wrappedStream.write(b);
	}

	@Override
	public void flush() throws IOException {
		wrappedStream.flush();
	}

	@Override
	public void close() throws IOException {
		wrappedStream.close();
		
		cacheExecutor.execute(new Runnable() {
			
			@Override
			public void run() {
				copyFileToCache();
			}
		});
	}

	private void copyFileToCache() {
		// Cache the closed file
		URI fileUri = sourceFile.toUri();
		
		//Get unique name for cached file
		final String pkg = className.substring(0, className.lastIndexOf('.'));
		final String java = className.substring(className.lastIndexOf('.') + 1) + ".java";
		File cachedFolder = new File(
				FilesCacheHelper.getExternalCache().getAbsolutePath() + File.separator
				+ "java" + File.separator + pkg.replace('.', File.separatorChar)
			);
		cachedFolder.mkdirs();
			
		File externalCachedFile = new File(
			cachedFolder.getAbsolutePath() + File.separator + java
		);
		
		FileUtils.copyCompletely(fileUri, externalCachedFile);
		
		FileDetails details = cacheHelper.getFileDetails(className);
		details.setGeneratedJavaCache(externalCachedFile.getAbsolutePath(), Paths.get(fileUri).toString());
	}	
}
