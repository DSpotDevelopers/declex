package com.dspot.declex.wrapper.generate;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;

import org.androidannotations.internal.generation.SourceCodeWriter;
import org.androidannotations.internal.process.OriginatingElements;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;

import org.androidannotations.helper.FilesCacheHelper;
import com.helger.jcodemodel.JPackage;

public class DeclexSourceCodeWriter extends SourceCodeWriter {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DeclexSourceCodeWriter.class);
	
	public DeclexSourceCodeWriter(Filer filer, OriginatingElements originatingElements, Charset charset) {
		super(filer, originatingElements, charset);
	}
	
	@Override
	public OutputStream openBinary(JPackage pkg, String fileName)
			throws IOException {
		String qualifiedClassName = toQualifiedClassName(pkg, fileName);
		
		Element[] classOriginatingElements = originatingElements.getClassOriginatingElements(qualifiedClassName);

		if (classOriginatingElements.length == 0) {
			LOGGER.debug("Generating class with no originating element: {}", qualifiedClassName);
		} else {
			LOGGER.debug("Generating class: {}", qualifiedClassName);	
		}

		try {
			JavaFileObject sourceFile;

			sourceFile = filer.createSourceFile(qualifiedClassName, classOriginatingElements);
			
			if (FilesCacheHelper.isCacheFilesEnabled()) {
				DeclexCachedSourceOutputStream sourceOutputStream = 
						new DeclexCachedSourceOutputStream(sourceFile, qualifiedClassName);
				
				return sourceOutputStream;
			} else {
				return sourceFile.openOutputStream();
			}
		} catch (FilerException e) {
			LOGGER.error("Could not generate source file for {}", qualifiedClassName, e.getMessage());
			/*
			 * This exception is expected, when some files are created twice. We
			 * cannot delete existing files, unless using a dirty hack. Files a
			 * created twice when the same file is created from different
			 * annotation rounds. Happens when renaming classes, and for
			 * Background executor. It also probably means I didn't fully
			 * understand how annotation processing works. If anyone can point
			 * me out...
			 */
			return VOID_OUTPUT_STREAM;
		}
	}

}
