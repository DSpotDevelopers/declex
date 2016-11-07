package com.dspot.declex.define;

import java.util.HashMap;
import java.util.Map;

import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.plugin.PluginClassHolder;

public class DefineHolder extends PluginClassHolder<EComponentWithViewSupportHolder> {

	private Map<String, String> normalDefine = new HashMap<>();
	private Map<String, String> regexDefine = new HashMap<>();

	
	public DefineHolder(EComponentWithViewSupportHolder holder) {
		super(holder);
	}

	public Map<String, String> getNormalDefine() {
		return normalDefine;
	}
	
	public Map<String, String> getRegexDefine() {
		return regexDefine;
	}
}
