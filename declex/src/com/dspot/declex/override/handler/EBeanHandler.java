package com.dspot.declex.override.handler;

import com.dspot.declex.holder.EventHolder;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.holder.EBeanHolder;

import javax.lang.model.element.Element;

public class EBeanHandler extends org.androidannotations.internal.core.handler.EBeanHandler {

    public EBeanHandler(AndroidAnnotationsEnvironment environment) {
        super(environment);
    }

    @Override
    public void process(Element element, EBeanHolder holder) {
        super.process(element, holder);

        EventHolder eventHolder = holder.getPluginHolder(new EventHolder(holder));
        eventHolder.setEventRegisteringBlock(holder.getInitBodyBeforeInjectionBlock());
    }

}
