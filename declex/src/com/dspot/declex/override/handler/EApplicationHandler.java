package com.dspot.declex.override.handler;

import com.dspot.declex.holder.EventHolder;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.holder.EApplicationHolder;

import javax.lang.model.element.Element;

public class EApplicationHandler extends org.androidannotations.internal.core.handler.EApplicationHandler {

    public EApplicationHandler(AndroidAnnotationsEnvironment environment) {
        super(environment);
    }

    @Override
    public void process(Element element, EApplicationHolder holder) {
        super.process(element, holder);

        EventHolder eventHolder = holder.getPluginHolder(new EventHolder(holder));
        eventHolder.setEventRegisteringBlock(holder.getInitBodyBeforeInjectionBlock());
    }
}
