package com.dspot.declex.share.holder;

import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.direct;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.helper.IdAnnotationHelper;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.holder.FoundViewHolder;
import org.androidannotations.plugin.PluginClassHolder;
import org.androidannotations.rclass.IRClass.Res;

import com.dspot.declex.api.extension.Extension;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.LayoutsParser;
import com.dspot.declex.util.MenuParser;
import com.dspot.declex.util.ParamUtils;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMod;

public class ViewsHolder extends
		PluginClassHolder<EComponentWithViewSupportHolder> {

	// <Layout Id, <Id, Class>>
	private Map<String, Map<String, String>> layoutObjects = new HashMap<>();
	private Map<String, Map<String, org.w3c.dom.Element>> layoutNodes = new HashMap<>();
	private List<String> menuObjects;
	
	private String defLayoutId = null;

	private LayoutsParser layoutParser;
	private MenuParser menuParser;

	private IdAnnotationHelper annotationHelper;

	private Map<String, ViewInfo> views = new HashMap<>();

	public ViewsHolder(EComponentWithViewSupportHolder holder,
			IdAnnotationHelper annotationHelper) {
		super(holder);

		this.annotationHelper = annotationHelper;

		this.layoutParser = LayoutsParser.getInstance();
		this.menuParser = MenuParser.getInstance();
		getDefaultLayout();
	}

	private void getDefaultLayout() {
		Map<String, String> defLayoutObjects = null;
		Map<String, org.w3c.dom.Element> defLayoutNodes = null;

		EActivity activity = holder().getAnnotatedElement().getAnnotation(
				EActivity.class);

		if (activity != null) {
			int layout = activity.value();
			if (layout != -1) {
				String idQualifiedName = environment().getRClass()
						.get(Res.LAYOUT).getIdQualifiedName(layout);
				Matcher matcher = Pattern.compile("\\.(\\w+)$").matcher(
						idQualifiedName);

				if (matcher.find()) {
					defLayoutId = matcher.group(1);
					defLayoutObjects = layoutParser.getLayoutObjects(
							defLayoutId, annotationHelper);
					defLayoutNodes = layoutParser.getLayoutNodes(defLayoutId);
				}
			}
		}

		EFragment fragment = holder().getAnnotatedElement().getAnnotation(
				EFragment.class);
		if (fragment != null) {
			int layout = fragment.value();
			if (layout != -1) {
				String idQualifiedName = environment().getRClass()
						.get(Res.LAYOUT).getIdQualifiedName(layout);
				Matcher matcher = Pattern.compile("\\.(\\w+)$").matcher(
						idQualifiedName);

				if (matcher.find()) {
					defLayoutId = matcher.group(1);
					defLayoutObjects = layoutParser.getLayoutObjects(
							defLayoutId, annotationHelper);
					defLayoutNodes = layoutParser.getLayoutNodes(defLayoutId);
				}
			}
		}

		if (defLayoutId != null) {
			layoutObjects.put(defLayoutId, defLayoutObjects);
			layoutNodes.put(defLayoutId, defLayoutNodes);
		}
	}

	public String getDefLayoutId() {
		return defLayoutId;
	}

	public void setDefLayoutId(String defLayoutId) {
		this.defLayoutId = defLayoutId;
	}

	public List<String> getMenuObjects() {
		if (menuObjects == null) {
			OptionsMenu optionMenu = holder().getAnnotatedElement()
					.getAnnotation(OptionsMenu.class);
			if (optionMenu != null) {
				int menu = optionMenu.value()[0];
				if (menu != -1) {
					String idQualifiedName = environment().getRClass()
							.get(Res.MENU).getIdQualifiedName(menu);
					Matcher matcher = Pattern.compile("\\.(\\w+)$").matcher(
							idQualifiedName);

					if (matcher.find()) {
						menuObjects = menuParser.getMenuObjects(
								matcher.group(1), annotationHelper);
					}
				}
			}

			if (menuObjects == null)
				menuObjects = new ArrayList<>(0);
		}

		return menuObjects;
	}

	public String addLayoutWithView(int layoutId) {
		
		String idQualifiedName = environment().getRClass().get(Res.LAYOUT)
				.getIdQualifiedName(layoutId);
		Matcher matcher = Pattern.compile("\\.(\\w+)$")
				.matcher(idQualifiedName);
		
		// Generate the view for the layout that should be inferred
		if (matcher.find()) {
			final String layoutIdString = matcher.group(1);
			String viewName = layoutIdString + DeclexConstant.VIEW;
			
			if (!views.containsKey(viewName)) {
				if (getGeneratedClass().fields().get(viewName)==null &&
						!TypeUtils.fieldInElement(viewName, holder().getAnnotatedElement()))
					getGeneratedClass().field(JMod.PRIVATE, environment().getClasses().VIEW, viewName);
				
				views.put(viewName, new ViewInfo());
			}			
			
			JFieldRef view = ref(viewName);
			JInvocation inflater = holder().getContextRef().invoke(
					"getLayoutInflater");
			holder().getOnViewChangedBodyBeforeInjectionBlock().assign(
					view,
					inflater.invoke("inflate").arg(direct(idQualifiedName))
							.arg(_null())
			);

			addLayout(layoutIdString);

			layoutObjects.get(layoutIdString).put(viewName, CanonicalNameConstants.VIEW);
			layoutNodes.get(layoutIdString).put(viewName, null);

			return layoutIdString;
		}

		return null;
	}

	public void addLayout(String layoutId) {

		if (!layoutObjects.containsKey(layoutId)) {
			Map<String, String> elementLayoutObjects = layoutParser
					.getLayoutObjects(layoutId, annotationHelper);
			layoutObjects.put(layoutId, elementLayoutObjects);

			Map<String, org.w3c.dom.Element> elementLayoutNodes = layoutParser
					.getLayoutNodes(layoutId);
			layoutNodes.put(layoutId, elementLayoutNodes);
		}

	}

	public void checkFieldNameInInvocation(String fieldName, JInvocation invocation) {
		if (this.viewsDeclared(fieldName + DeclexConstant.VIEW)) {
			invocation.arg(ref(fieldName + DeclexConstant.VIEW));
		} else {
			if (this.layoutContainsId(fieldName)) {
				this.createAndAssignView(fieldName);
				invocation.arg(ref(fieldName + DeclexConstant.VIEW));
			} else {
				ParamUtils.injectParam(fieldName, invocation);	
			}
		}
	}
	
	public boolean viewsDeclared(String id) {
		return views.containsKey(id);
	}
	
	public boolean layoutContainsId(String id) {
		return layoutContainsId(id, null);
	}

	public boolean layoutContainsId(String id, String layoutId) {
		if (layoutId == null)
			layoutId = defLayoutId;
		if (layoutId == null)
			return false;

		return layoutObjects.get(layoutId).containsKey(id);
	}

	public String getClassNameFromId(String id) {
		return getClassNameFromId(id, null);
	}

	public String getClassNameFromId(String id, String layoutId) {
		if (layoutId == null)
			layoutId = defLayoutId;
		if (layoutId == null)
			return null;

		return layoutObjects.get(layoutId).get(id);
	}

	public org.w3c.dom.Element getXMLElementFromId(String id) {
		return getXMLElementFromId(id, null);
	}

	public org.w3c.dom.Element getXMLElementFromId(String id, String layoutId) {
		if (layoutId == null)
			layoutId = defLayoutId;
		if (layoutId == null)
			return null;

		return layoutNodes.get(layoutId).get(id);
	}

	public Map<String, String> getLayoutObjects(String layoutId) {
		if (layoutId == null)
			layoutId = defLayoutId;
		if (layoutId == null)
			return null;

		return layoutObjects.get(layoutId);
	}

	public JFieldRef createAndAssignView(String fieldName) {
		return this.createAndAssignView(fieldName, null);
	}

	public JFieldRef createAndAssignView(String fieldName,
			IWriteInBloc writeInBlock) {
		String viewName = fieldName + DeclexConstant.VIEW;
		JFieldRef view = ref(viewName);

		JFieldRef idRef = environment().getRClass().get(Res.ID)
				.getIdStaticRef(fieldName, environment());
		AbstractJClass viewClass = getJClass(getClassNameFromId(fieldName));
		FoundViewHolder foundViewHolder = holder().getFoundViewHolder(idRef,
				viewClass);
		JBlock body = foundViewHolder.getIfNotNullBlock();

		if (!views.containsKey(viewName)) {
			if (getGeneratedClass().fields().get(viewName)==null &&
					!TypeUtils.fieldInElement(viewName, holder().getAnnotatedElement()))
				getGeneratedClass().field(JMod.PRIVATE, viewClass, viewName);
			
			body.assign(view, cast(viewClass, foundViewHolder.getRef()));
			views.put(viewName, new ViewInfo());
		}

		if (writeInBlock != null)
			writeInBlock.writeInBlock(viewName, viewClass, view, body);

		return view;
	}

	public void findFieldsAndMethods(String className, String fieldName,
			Element element, Map<String, IdInfoHolder> fields,
			Map<String, IdInfoHolder> methods, boolean getter) {
		findFieldsAndMethods(className, fieldName, element, fields, methods,
				getter, false, null);
	}

	public void findFieldsAndMethods(String className, String fieldName,
			Element element, Map<String, IdInfoHolder> fields,
			Map<String, IdInfoHolder> methods, boolean getter, boolean isList,
			String layoutId) {
		TypeElement typeElement = environment().getProcessingEnvironment()
				.getElementUtils().getTypeElement(className);
		findFieldsAndMethods(fieldName, typeElement, fields, methods,
				className, getLayoutObjects(layoutId), getter, isList);

		// Apply to Extensions
		List<? extends TypeMirror> superTypes = environment()
				.getProcessingEnvironment().getTypeUtils()
				.directSupertypes(typeElement.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = environment().getProcessingEnvironment()
					.getElementUtils().getTypeElement(type.toString());

			if (superElement.getAnnotation(Extension.class) != null) {
				findFieldsAndMethods(type.toString(), fieldName, element,
						fields, methods, getter, isList, layoutId);
			}

			break;
		}
	}

	private void findFieldsAndMethods(String fieldName,
			TypeElement typeElement, Map<String, IdInfoHolder> fields,
			Map<String, IdInfoHolder> methods, String className,
			Map<String, String> layoutObjects, boolean getter, boolean isList) {
		String classSimpleName = getJClass(className).name();
		for (String id : layoutObjects.keySet()) {
			String startsWith = null;
			String originalId = id;

			if (id.startsWith(fieldName)) {
				startsWith = fieldName;
			}

			if (id.startsWith(fieldName + "_")) {
				startsWith = fieldName + "_";
			}

			if (id.startsWith(classSimpleName + "_")) {
				startsWith = classSimpleName + "_";
			}

			if (id.startsWith(classSimpleName.toLowerCase() + "_")) {
				startsWith = classSimpleName.toLowerCase() + "_";
			}

			if (startsWith != null) {
				id = id.substring(startsWith.length());
			} else {
				if (!isList)
					continue;
			}

			deepFieldsAndMethodsSearch(id, null, typeElement, fields, methods,
					originalId, layoutObjects.get(originalId), getter);
		}
	}

	private void deepFieldsAndMethodsSearch(String id, String prevField,
			TypeElement testElement, Map<String, IdInfoHolder> fields,
			Map<String, IdInfoHolder> methods, String originalId,
			String idClass, boolean getter) {
		if (id == null || id.isEmpty())
			return;

		final String normalizedId = id.substring(0, 1).toLowerCase()
				+ id.substring(1);

		List<? extends Element> elems = testElement.getEnclosedElements();
		for (Element elem : elems) {
			final String elemName = elem.getSimpleName().toString();
			final String completeElemName = prevField == null ? elemName
					: prevField + "." + elemName;

			if (elem.getKind() == ElementKind.FIELD) {

				// If the class element is not a primitive, then call the method
				// in that element combinations (recursive DFS)
				if (!elem.asType().getKind().isPrimitive()) {
					String elemType = TypeUtils.typeFromTypeString(elem
							.asType().toString(), environment());
					if (elemType.endsWith("_"))
						elemType = elemType.substring(0, elemType.length() - 1);

					TypeElement fieldTypeElement = environment()
							.getProcessingEnvironment().getElementUtils()
							.getTypeElement(elemType);

					if (fieldTypeElement != null
							&& !fieldTypeElement.toString().equals(
									String.class.getCanonicalName())) {

						if (id.startsWith(elemName)
								|| normalizedId.startsWith(elemName)) {
							int extraToRemove = id.startsWith(elemName + "_") ? 1
									: 0;
							deepFieldsAndMethodsSearch(
									id.substring(elemName.length()
											+ extraToRemove), completeElemName,
									fieldTypeElement, fields, methods,
									originalId, idClass, getter);
						}
					}
				}

				if (id.equals(elemName) || normalizedId.equals(elemName)) {
					fields.put(completeElemName, new IdInfoHolder(originalId,
							elem, elem.asType(), idClass,
							new ArrayList<String>(0)));
				}
			}

			if (elem.getKind() == ElementKind.METHOD) {
				if (id.equals(elemName) || normalizedId.equals(elemName)) {
					// Only setter methods
					ExecutableElement exeElem = (ExecutableElement) elem;

					if (exeElem.getParameters().size() < (getter ? 0 : 1))
						continue;

					List<String> extraParams = new LinkedList<String>();
					for (int i = (getter ? 0 : 1); i < exeElem.getParameters()
							.size(); i++) {
						extraParams.add(exeElem.getParameters().get(i)
								.getSimpleName().toString());
					}

					TypeMirror paramType = null;
					if (getter) {
						paramType = exeElem.getReturnType();
					} else {
						paramType = exeElem.getParameters().get(0).asType();
					}

					methods.put(completeElemName, new IdInfoHolder(originalId,
							elem, paramType, idClass, extraParams));
				}
			}
		}
	}

	public static class IdInfoHolder {
		public String idName;
		public String viewClass;

		public TypeMirror type;

		public List<String> extraParams;
		public Element element;

		public IdInfoHolder(String idName, Element element, TypeMirror type,
				String className, List<String> extraParams) {
			super();
			this.element = element;
			this.idName = idName;
			this.type = type;
			this.viewClass = className;
			this.extraParams = extraParams;
		}

		@Override
		public String toString() {
			return idName + (type != null ? ": " + type.toString() : "") + " "
					+ extraParams;
		}
	}

	public static interface IWriteInBloc {
		public void writeInBlock(String viewName, AbstractJClass viewClass,
				JFieldRef view, JBlock block);
	}

	public static abstract class WriteInBlockWithResult<T> implements
			IWriteInBloc {
		protected T result;

		public T getResult() {
			return result;
		}
	}

	private class ViewInfo {

	}

}
