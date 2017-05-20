package com.dspot.declex.wrapper.element;

import java.util.List;

import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

class VirtualExecutableElement extends VirtualElement implements ExecutableElement {
	
	VirtualExecutableElement(ExecutableElement element) {
		super(element);
	}

	@Override
	public List<? extends TypeParameterElement> getTypeParameters() {
		return ((ExecutableElement)element).getTypeParameters();
	}

	@Override
	public TypeMirror getReturnType() {
		return ((ExecutableElement)element).getReturnType();
	}

	@Override
	public List<? extends VariableElement> getParameters() {
		return ((ExecutableElement)element).getParameters();
	}

	@Override
	public TypeMirror getReceiverType() {
		return ((ExecutableElement)element).getReceiverType();
	}

	@Override
	public boolean isVarArgs() {
		return ((ExecutableElement)element).isVarArgs();
	}

	@Override
	public boolean isDefault() {
		return ((ExecutableElement)element).isDefault();
	}

	@Override
	public List<? extends TypeMirror> getThrownTypes() {
		return ((ExecutableElement)element).getThrownTypes();
	}

	@Override
	public AnnotationValue getDefaultValue() {
		return ((ExecutableElement)element).getDefaultValue();
	}
	
}

