package com.dspot.declex.util.element;

import javax.lang.model.element.VariableElement;

class VirtualVariableElement extends VirtualElement implements VariableElement {
	
	VirtualVariableElement(VariableElement element) {
		super(element);
	}

	@Override
	public Object getConstantValue() {
		return ((VariableElement)element).getConstantValue();
	}

}

