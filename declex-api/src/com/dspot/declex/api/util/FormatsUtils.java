/**
 * Copyright (C) 2016 DSpot Sp. z o.o
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
package com.dspot.declex.api.util;

import static com.helger.jcodemodel.JExpr.direct;
import static com.helger.jcodemodel.JExpr.lit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.helger.jcodemodel.IJExpression;

public class FormatsUtils {
	//This term (?!(?:\\s*\"[^\"]+\"\\s*:)), is to exclude JSON structures 
	public static final String FORMAT_SYNTAX_REGX = "\\{(?!(?:\\s*\"[^\"]+\"\\s*:))([^\\}\\{]+)(\\{(\\d+)\\})*\\}";
	
	public static IJExpression expressionFromString(String value) {
		Matcher matcher = Pattern.compile(FORMAT_SYNTAX_REGX).matcher(value);
		
		IJExpression expression = null;
		int prev = 0; 
		while (matcher.find()) {
			String match = matcher.group(1);
			if (matcher.groupCount() == 3 && matcher.group(3) != null && !matcher.group(3).equals("")) {
				match = match + ".split(\"\\\\s*[,;:]\\\\s*\")[" + matcher.group(3) + "]";
			}
			
			if (expression == null) {
				if (prev != matcher.start()) {
					expression = lit(value.substring(prev, matcher.start()))
							     .plus(direct(match).plus(lit("")));
				} else {
					expression = direct(match).plus(lit(""));	
				} 
			} else {
				if (prev != matcher.start()) {
					expression = expression.plus(lit(value.substring(prev, matcher.start())));
				} 
				
				expression = expression.plus(direct(match)).plus(lit(""));
			}
			
			prev = matcher.end();
		}
		
		if (expression != null) {
			if (prev != value.length()) {
				expression = expression.plus(lit(value.substring(prev)));
			}
			
			return expression;
		}
		
		return lit(value);
	}
	
	public static String fieldToGetter(String name) {
		Matcher matcher = Pattern.compile("_(\\w)").matcher(name);
		while (matcher.find()) {
			name = name.replaceFirst(matcher.group(0), matcher.group(1).toUpperCase());
		}

	    return "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
	}
	
	public static String fieldToSetter(String name) {
		Matcher matcher = Pattern.compile("_(\\w)").matcher(name);
		while (matcher.find()) {
			name = name.replaceFirst(matcher.group(0), matcher.group(1).toUpperCase());
		}
		
	    return "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
	}

}
