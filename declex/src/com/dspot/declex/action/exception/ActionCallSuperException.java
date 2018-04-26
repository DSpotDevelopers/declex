package com.dspot.declex.action.exception;

import javax.lang.model.element.Element;

public class ActionCallSuperException extends ActionProcessingException {

    private static final long serialVersionUID = 1L;
    private Element element;

    public ActionCallSuperException(Element element) {
        super("");
        this.element = element;
    }

    public Element getElement() {
        return element;
    }

}
