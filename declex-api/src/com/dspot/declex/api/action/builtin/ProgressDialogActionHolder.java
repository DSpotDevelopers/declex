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
package com.dspot.declex.api.action.builtin;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.annotation.Assignable;
import com.dspot.declex.api.action.annotation.FormattedExpression;

@EBean
@ActionFor("ProgressDialog")
public class ProgressDialogActionHolder {

    ProgressDialog dialog;

    @RootContext
    Context context;
        
    private Runnable Shown;
    
    void init() {
        dialog = new ProgressDialog(context);
    }
    
	//Here you can infer any parameter, the first parameter is the next listener 
    void build(Runnable Shown, final Runnable Dismissed) {
    	this.Shown = Shown;
    	
    	if (Dismissed != null) {
    		 dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				
				@Override
				public void onDismiss(DialogInterface arg0) {
					if (Dismissed != null) Dismissed.run();
				}
			});
    	}
       
    };
	
    void execute() {
		dialog.show();
		if (Shown != null) Shown.run();
	}

    public Dialog dialog() {
        return this.dialog;
    }

    public ProgressDialogActionHolder title(@FormattedExpression String title) {
        dialog.setTitle(title);
        return this;
    }

    public ProgressDialogActionHolder message(@FormattedExpression String message) {
        dialog.setMessage(message);
        return this;
    }

    public ProgressDialogActionHolder into(@Assignable("dialog") Dialog dialog) {
    	return this;
    }

}
