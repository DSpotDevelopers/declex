package com.dspot.declex.wrapper;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

public class RoundEnvironmentByCache implements RoundEnvironment {

	private RoundEnvironment roundEnvironment;
	private Map<TypeElement, Set<? extends Element>> annotatedElements;
	
	public RoundEnvironmentByCache(RoundEnvironment roundEnvironment, Map<TypeElement, Set<? extends Element>> annotatedElements) {
		this.roundEnvironment = roundEnvironment;
		this.annotatedElements = annotatedElements;
	}

	@Override
	public boolean processingOver() {
		return roundEnvironment.processingOver();
	}

	@Override
	public boolean errorRaised() {
		return roundEnvironment.errorRaised();
	}

	@Override
	public Set<? extends Element> getRootElements() {
		return roundEnvironment.getRootElements();
	}

	@Override
	public Set<? extends Element> getElementsAnnotatedWith(TypeElement annotation) {
		return annotatedElements.get(annotation);
	}

	@Override
	public Set<? extends Element> getElementsAnnotatedWith(Class<? extends Annotation> annotation) {
		//This method is not called by AA
		return roundEnvironment.getElementsAnnotatedWith(annotation);
	}

}
