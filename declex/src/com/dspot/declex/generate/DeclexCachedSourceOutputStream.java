package com.dspot.declex.generate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Paths;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.JavaFileObject;

import com.dspot.declex.helper.FilesCacheHelper;
import com.dspot.declex.helper.FilesCacheHelper.FileDetails;
import com.dspot.declex.util.FileUtils;

public class DeclexCachedSourceOutputStream extends OutputStream {

	private FilesCacheHelper cacheHelper;
	private JavaFileObject sourceFile;
	private OutputStream wrappedStream;
	private ProcessingEnvironment env;
	private String className;

	public DeclexCachedSourceOutputStream(JavaFileObject sourceFile, String className, ProcessingEnvironment env)
			throws IOException {
		this.sourceFile = sourceFile;
		this.wrappedStream = sourceFile.openOutputStream();
		this.env = env;
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
		copyFileToCache();
	}

	private void copyFileToCache() {
		// Cache the closed file
		URI fileUri = sourceFile.toUri();
		
		//Get unique name for cached file
		File externalCacheFolder = FileUtils.getPersistenceConfigFile("cache", env);
		
		File externalCachedFile = new File(externalCacheFolder.getAbsolutePath() 
				+ File.separator 
				+ className.replace('.', '_'));
		
		copyCompletely(fileUri, externalCachedFile);
		
		FileDetails details = cacheHelper.getFileDetails(className);
		details.cachedFile = externalCachedFile.getAbsolutePath();
		details.originalFile = Paths.get(fileUri).toString();
	}
	
	private static void copyCompletely(URI input, File out) {
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

			FileUtils.copyCompletely(in, new FileOutputStream(out));
		} catch (IllegalArgumentException e) {
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}
}
