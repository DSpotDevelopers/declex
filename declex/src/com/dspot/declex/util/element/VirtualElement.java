package com.dspot.declex.util.element;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

public class VirtualElement implements Element {

	private Element reference;
	
	protected Element element;
	private Element enclosingElement;
	
	public static VirtualElement from(Element element) {
		if (element instanceof ExecutableElement) {
			return new VirtualExecutableElement((ExecutableElement) element);
		}
		
		if (element instanceof VariableElement) {
			return new VirtualVariableElement((VariableElement)element);
		}
		
		return new VirtualElement(element);
	}
	
	VirtualElement(Element element) {
		this.element = element;
	}
	
	public Element getReference() {
		return reference;
	}
	
	public void setReference(Element reference) {
		this.reference = reference;
	}
		
	public Element getElement() {
		return element;
	}
	
	@Override
	public <A extends Annotation> A[] getAnnotationsByType(
			Class<A> annotationType) {
		return element.getAnnotationsByType(annotationType);
	}

	@Override
	public TypeMirror asType() {
		return element.asType();
	}

	@Override
	public ElementKind getKind() {
		return element.getKind();
	}

	@Override
	public Set<Modifier> getModifiers() {
		return element.getModifiers();
	}

	@Override
	public Name getSimpleName() {
		return element.getSimpleName();
	}

	@Override
	public Element getEnclosingElement() {
		return enclosingElement;
	}
	
	public void setEnclosingElement(Element enclosingElement) {
		this.enclosingElement = enclosingElement;
	}

	@Override
	public List<? extends Element> getEnclosedElements() {
		return element.getEnclosedElements();
	}

	@Override
	public List<? extends AnnotationMirror> getAnnotationMirrors() {
		return element.getAnnotationMirrors();
	}

	@Override
	public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
		return element.getAnnotation(annotationType);
	}

	@Override
	public <R, P> R accept(ElementVisitor<R, P> v, P p) {
		return element.accept(v, p);
	}
		
	@Override
	public String toString() {
		return element.toString();
	}


}
