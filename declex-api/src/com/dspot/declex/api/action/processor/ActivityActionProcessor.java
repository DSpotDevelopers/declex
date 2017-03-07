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
package com.dspot.declex.api.action.processor;

import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr.lit;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethod;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JVar;

public class ActivityActionProcessor extends BaseActionProcessor {
	
	public Map<Object, List<Integer>> requestCodes = new HashMap<>();
	
	@Override
	public void validate(ActionInfo actionInfo) {
		super.validate(actionInfo);
		
		ActionMethod init = getActionMethod("init");
				
		if (init.metaData != null) {
		}
	}
	
	@Override
	public void process(ActionInfo actionInfo) {
		super.process(actionInfo);
				
		
		addPostInitBlock(getAction().invoke("setBuilder").arg(getGeneratedClass().staticRef("this")));
			
		for (ActionMethod withResult : actionInfo.methods.get("withResult")) {
			if (withResult.metaData != null) {
				JVar resultCode = getMethodInHolder("getOnActivityResultResultCodeParam");
				JVar data = getMethodInHolder("getOnActivityResultDataParam");

				JBlock block;
				if (withResult.params.size() == 0) {
					int requestCode = getRandomCode();
					addPreBuildBlock(getAction().invoke("withResult").arg(lit(requestCode)));
					
					block = getMethodInHolder("getOnActivityResultCaseBlock", null, requestCode);
					
					block = block._if(getAction().neNull())._then();
							
				} else {
					JMethod method = getMethodInHolder("getOnActivityResultMethod");
					block = method.body()._if(
							getAction().neNull().cand(getAction().invoke("getRequestCode").eq(ref("requestCode")))
						)._then();
					
				}
				
				block = block._if(getAction().invoke("getOnResult").neNull())._then();
				block.add(getAction().invoke("getOnResult").invoke("onActivityResult")
				    		         .arg(getAction().invoke("getRequestCode")).arg(resultCode).arg(data)
				    	  );
				block.assign(getAction(), _null());

				break;
			}	
		}
				
	}

	private Integer getRandomCode() {
		List<Integer> codes = requestCodes.get(getHolder());
		if (codes == null) {
			codes = new LinkedList<>();
			requestCodes.put(getHolder(), codes);
		}
		
		//Generate the random code;
		Integer random;
		do {
			random = (int) (Math.random()*65536); //Only lower than 16bits request codes
		} while (codes.contains(random));
		
		codes.add(random);
		
		return random;
	}
}
