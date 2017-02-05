package com.dspot.declex.override.handler;

import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.holder.EFragmentHolder;

import com.dspot.declex.override.holder.FragmentActionHolder;


public class EFragmentHandler extends org.androidannotations.internal.core.handler.EFragmentHandler {

	public EFragmentHandler(AndroidAnnotationsEnvironment environment) {
		super(environment);
	}
	
	@Override
	public void validate(Element element, ElementValidation valid) {
		super.validate(element, valid);
		
		FragmentActionHolder.createInformationForActionHolder(element, getEnvironment());
	}
	
	@Override
	public void process(Element element, EFragmentHolder holder) {
		super.process(element, holder);
		
		FragmentActionHolder actionHolder = holder.getPluginHolder(new FragmentActionHolder(holder));
		actionHolder.getFragmentAction();
	}

}
