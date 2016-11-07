package com.dspot.declex.override.handler;

import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.holder.EActivityHolder;

import com.dspot.declex.override.holder.ActivityActionHolder;


public class EActivityHandler extends org.androidannotations.internal.core.handler.EActivityHandler {

	public EActivityHandler(AndroidAnnotationsEnvironment environment) {
		super(environment);
	}
	
	@Override
	public void process(Element element, EActivityHolder holder) {
		super.process(element, holder);
		
		ActivityActionHolder actionHolder = holder.getPluginHolder(new ActivityActionHolder(holder));
		actionHolder.getActivityAction();
	}

}
