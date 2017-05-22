<#--

    Copyright (C) 2016-2017 DSpot Sp. z o.o

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed To in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.

-->
<@class_head>
package com.dspot.declex.localdb;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.activeandroid.util.SQLiteUtils;
import com.activeandroid.Model;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.activeandroid.query.Delete;

public class User extends Model {
</@class_head>
	
	
	//============================================================
	//						@LocalDBModel
	//============================================================
	
	private static ${className} getLocalDBModel(Context context,  Map<String, Object> args) {
		
		String query = getLocalDBModelQueryDefault();
		String orderBy = "";
		if (args != null) {
			if (args.containsKey("query")) query = (String)args.get("query");
			if (args.containsKey("orderBy")) orderBy = (String)args.get("orderBy");
		}
		
		Matcher matcher = Pattern.compile("@(\\w+)\\(([^)]+)\\)").matcher(query);
		while (matcher.find()) {
			if (matcher.group(1).equals("db"))
				query = query.replace(matcher.group(0), matcher.group(2));
			else
				query = query.replace(matcher.group(0), "");
		}
		
		matcher = Pattern.compile("@(\\w+)\\(([^)]+)\\)").matcher(orderBy);
		if (matcher.find()) {
			if (matcher.group(1).equals("db"))
				orderBy = orderBy.replace(matcher.group(0), matcher.group(2));
			else
				orderBy = orderBy.replace(matcher.group(0), "");
		} 
		
		if (query.toLowerCase().trim().startsWith("select ")) {
			java.util.List<${className}> models = SQLiteUtils.rawQuery(${className}.class, query, null);
			if (models.size() > 0) return models.get(0);
			
			return null;
		}
		
		From exeQuery;
		boolean isDelete = false;
		if (query.toLowerCase().trim().startsWith("delete ")) {
			exeQuery = new Delete().from(${className}.class);
			query = query.substring(7);
			isDelete = true;
		} else {
			exeQuery = new Select().from(${className}.class);
		}
		
        if ((query!= null)&&(!query.equals(""))) {
            exeQuery = exeQuery.where(query);
        }
        
        if ((orderBy!= null)&&(!orderBy.equals(""))) {
            exeQuery = exeQuery.orderBy(orderBy);
        }
        
        if (isDelete) {
        	exeQuery.execute();
        } else {
        	${className} instance = exeQuery.executeSingle();
        	
            if (instance!= null) {
            	instance.${fullInitVar} = true;
                instance.rebind(context);
                instance.${fullInitVar} = false;
                return instance;
            }
        }
                
        return null;
	}
	
	private ${className} putLocalDBModel(Map<String, Object> args) {
		
		String query = getLocalDBModelQueryDefault();
		if (args != null) {
			if (args.containsKey("query")) query = (String) args.get("query");
		}
		
		if (query.equals("db-ignore")) return this;
		
		if (query.toLowerCase().trim().equals("delete")) {
			${className}.delete(${className}.class, this.getId());
			return this;
		}
		
		if (query.toLowerCase().trim().startsWith("delete ")) {
			getLocalDBModel(context_, args);
			return this;
		}
		
		try {
			<#list columnFields as column, type>
			if (${column} != null)
			<#if isList[column?index]=="true">
				for(${type} field : ${column}) {
					field.save();
				}
			<#else>
				${column}.save();
			</#if>
			</#list>
			this.save();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
        return this;
	}
	
	private static java.util.List<${className}> getLocalDBModelList(Context context,  Map<String, Object> args) {
		
		String query = getLocalDBModelQueryDefault();
		String orderBy = "";
		if (args != null) {
			if (args.containsKey("query")) query = (String) args.get("query");
			if (args.containsKey("orderBy")) orderBy = (String) args.get("orderBy");
		}
		
		if (query.toLowerCase().trim().startsWith("select ")) {
			java.util.List<${className}> models = SQLiteUtils.rawQuery(${className}.class, query, null);
			return models;
		}
		
		From exeQuery;
		if (query.toLowerCase().trim().startsWith("delete ")) {
			exeQuery = new Delete().from(${className}.class);
			query = query.substring(7);
		} else {
			exeQuery = new Select().from(${className}.class);
		}
		
        if ((query!= null)&&(!query.equals(""))) {
            exeQuery = exeQuery.where(query);
        }
        
        if ((orderBy!= null)&&(!orderBy.equals(""))) {
            exeQuery = exeQuery.orderBy(orderBy);
        }
        
        java.util.List<${className}> models = exeQuery.execute();
        for (${className} model : models) {
        	model.${fullInitVar} = true;
            model.rebind(context);
            model.${fullInitVar} = false;
        }
        
        return models;
	}
	
<@class_footer>	
}
</@class_footer>