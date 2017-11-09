package com.dspot.declex.helper;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.rclass.IRClass.Res;

import com.dspot.declex.parser.LayoutsParser;
import com.dspot.declex.parser.LayoutsParser.LayoutObject;

public class ViewsHelper {
	
	private LayoutsParser layoutParser;
	
	private Element element;
	private AndroidAnnotationsEnvironment environment;
	
	private String layoutId;
	private Map<String, LayoutObject> layoutObjects;
	
	public ViewsHelper(Element element, AndroidAnnotationsEnvironment environment) {	
		
		this.layoutParser = LayoutsParser.getInstance();		
		this.element = element;
		this.environment = environment;
		
		getLayoutInformation();
	}
	
	private void getLayoutInformation() {
		
		EActivity activity = element.getAnnotation(EActivity.class);
		if (activity != null) {
			int layout = activity.value();
			if (layout != -1) {
				String idQualifiedName = environment.getRClass()
						.get(Res.LAYOUT).getIdQualifiedName(layout);
				Matcher matcher = Pattern.compile("\\.(\\w+)$").matcher(
						idQualifiedName);

				if (matcher.find()) {
					layoutId = matcher.group(1);
					layoutObjects = layoutParser.getLayoutObjects(layoutId);
				}
			}
		}
		
		EFragment fragment = element.getAnnotation(EFragment.class);
		if (fragment != null) {
			int layout = fragment.value();
			if (layout != -1) {
				String idQualifiedName = environment.getRClass()
						.get(Res.LAYOUT).getIdQualifiedName(layout);
				Matcher matcher = Pattern.compile("\\.(\\w+)$").matcher(
						idQualifiedName);

				if (matcher.find()) {
					layoutId = matcher.group(1);
					layoutObjects = layoutParser.getLayoutObjects(layoutId);
				}
			}
		}
	}
	
	public String getLayoutId() {
		return layoutId;
	}
	
	public Map<String, LayoutObject> getLayoutObjects() {
		return layoutObjects;
	}
	
	public Map<String, LayoutObject> getLayoutObjects(String layoutId) {
		if (layoutId.equals(this.layoutId)) return layoutObjects;
		
		return layoutParser.getLayoutObjects(layoutId);
	}
	
}
