/**
 * Copyright (C) 2016-2019 DSpot Sp. z o.o
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dspot.declex.action.util;

import com.dspot.declex.action.builder.ActionsBuilder;
import com.dspot.declex.api.action.ActionsTools;
import com.dspot.declex.holder.EnsureImportsHolder;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JFormatter;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.TreePath;
import org.androidannotations.holder.EComponentHolder;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.helger.jcodemodel.JExpr.direct;

public class ExpressionsHelper {

    private boolean isValidating;

    private EComponentHolder holder;

    private ActionsBuilder actionsBuilder;

    private final List<? extends ImportTree> imports;

    private Map<String, String> staticImports = new HashMap<>();

    public ExpressionsHelper(boolean isValidating, ActionsBuilder actionsBuilder, EComponentHolder holder, TreePath treePath) {
        this.isValidating = isValidating;
        this.actionsBuilder = actionsBuilder;
        this.holder = holder;
        this.imports = treePath.getCompilationUnit().getImports();
    }

    public String variableClassFromImports(final String variableClass) {
        return this.variableClassFromImports(variableClass, false);
    }

    public String variableClassFromImports(final String variableClass, boolean ensureImport) {

        for (ImportTree importTree : imports) {
            String lastElementImport = importTree.getQualifiedIdentifier().toString();
            String firstElementName = variableClass;
            String currentVariableClass = "";

            int pointIndex = lastElementImport.lastIndexOf('.');
            if (pointIndex != -1) {
                lastElementImport = lastElementImport.substring(pointIndex + 1);
            }

            pointIndex = firstElementName.indexOf('.');
            if (pointIndex != -1) {
                firstElementName = firstElementName.substring(0, pointIndex);
                currentVariableClass = variableClass.substring(pointIndex);
            }

            while (firstElementName.endsWith("[]")) {
                firstElementName = firstElementName.substring(0, firstElementName.length()-2);
                if (currentVariableClass.isEmpty()) currentVariableClass = currentVariableClass + "[]";
            }

            if (lastElementImport.equals(firstElementName)) {

                if (!isValidating && ensureImport) {
                    EnsureImportsHolder importsHolder = holder.getPluginHolder(new EnsureImportsHolder(holder));
                    if (!importTree.isStatic()) {
                        importsHolder.ensureImport(importTree.getQualifiedIdentifier().toString());
                    } else {
                        staticImports.put(lastElementImport, importTree.getQualifiedIdentifier().toString());
                    }
                }

                return importTree.getQualifiedIdentifier() + currentVariableClass;
            }
        }

        return variableClass;
    }

    public IJExpression stringToExpression(String code) {
        return direct(parseForSpecials(code, false));
    }

    public String expressionToString(IJExpression expression) {
        if (expression == null) {
            throw new IllegalArgumentException("Generable must not be null.");
        }
        final StringWriter stringWriter = new StringWriter();
        final JFormatter formatter = new JFormatter(stringWriter);
        expression.generate(formatter);

        return stringWriter.toString();
    }

    public String parseForSpecials(String expression, boolean ignoreThis) {

        if (isValidating) return expression;

        //Split by string literals (Specials should not be placed inside Strings)
        List<String> literalsSplit = Arrays.asList(expression.split("(?<!\\\\)\""));
        if (literalsSplit.size() <= 1) return parseStringForSpecial(expression, ignoreThis);

        String newExpression = "";
        int i = 0;
        for (String part : literalsSplit) {
            if (i % 2 == 0) { //Not string literals
                newExpression = newExpression + parseStringForSpecial(part, ignoreThis);
            } else {  //String literals


                if (newExpression.endsWith("<!>")) { //This is used to detect injected expressions
                    newExpression = newExpression.substring(0, newExpression.length()-3) + part;
                } else {
                    newExpression = newExpression + "\"" + part + "\"";
                }

            }
            i++;
        }

        if (expression.endsWith("\"\"")) newExpression = newExpression + "\"\"";

        return newExpression;
    }

    private String parseStringForSpecial(String expression, boolean ignoreThis) {

        String generatedClass = holder.getGeneratedClass().name();
        String annotatedClass = holder.getAnnotatedElement().getSimpleName().toString();

        if (!ignoreThis) {
            expression = expression.replaceAll("(?<![a-zA-Z_$.0-9])this(?![a-zA-Z_$0-9])", generatedClass + ".this");
            expression = expression.replaceAll("(?<![a-zA-Z_$.0-9])super(?![a-zA-Z_$0-9])", generatedClass + ".super");
        }

        expression = expression.replaceAll("(?<![a-zA-Z_$.])" + annotatedClass + ".this(?![a-zA-Z_$])", generatedClass + ".this");
        expression = expression.replaceAll("(?<![a-zA-Z_$.])" + annotatedClass + ".super(?![a-zA-Z_$])", generatedClass + ".super");

        if (actionsBuilder.hasSelector()) {
            expression = expression.replace(actionsBuilder.getCurrentSelector(), "");
        }

        //Injections
        expression = expression.replaceAll("\\$inject\\(\\)", "()");
        expression = expression.replaceAll("\\$inject\\(", ActionsTools.class.getCanonicalName() + ".\\$cast(<!>");
        expression = expression.replaceAll("\\$injectItem\\((.*)$", ActionsTools.class.getCanonicalName() + ".\\$item($1<!>");

        //Static imports
        for (Map.Entry<String, String> staticImport : staticImports.entrySet()) {
            expression = expression.replaceAll(
                "(?<![a-zA-Z_$.0-9])" + staticImport.getKey() + "(?![a-zA-Z_$.0-9])", staticImport.getValue()
            );
        }

        return expression;
    }

}
