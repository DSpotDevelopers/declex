/**
 * Copyright (C) 2016-2017 DSpot Sp. z o.o
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
import android.content.DialogInterface.OnCancelListener;
import android.support.annotation.StringRes;

import com.dspot.declex.annotation.action.ActionFor;
import com.dspot.declex.annotation.action.Assignable;
import com.dspot.declex.annotation.action.FormattedExpression;
import com.dspot.declex.annotation.action.StopOn;

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
    void build(
    		final Runnable Shown, 
    		final Runnable Canceled, 
    		final Runnable Dismissed) {
    	
    	this.Shown = Shown;
    	
    	if (Canceled != null) {
    		dialog.setOnCancelListener(new OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface arg0) {
					Canceled.run();
				}
			});
    	}
    	
    	if (Dismissed != null) {
    		 dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				
				@Override
				public void onDismiss(DialogInterface arg0) {
					Dismissed.run();
				}
			});
    	}
       
    };
	
    void execute() {
		dialog.show();
		if (Shown != null) Shown.run();
	}

    /**
     * @return Internal Android Dialog instance.
     */
    @StopOn("show")
    public Dialog dialog() {
        return this.dialog;
    }
    
    /**
     * Assigns the Internal Android Dialog instance.
     * 
     * @param dialog The variable to which the dialog is going to be assigned
     */
    public ProgressDialogActionHolder dialog(@Assignable("dialog") Dialog dialog) {
    	return this;
    }

    public ProgressDialogActionHolder title(@StringRes int titleRes) {
    	return title(context.getString(titleRes));
    }
    
    public ProgressDialogActionHolder title(@FormattedExpression String title) {
        dialog.setTitle(title);
        return this;
    }

    public ProgressDialogActionHolder message(@StringRes int messageRes) {
    	return message(context.getString(messageRes));
    }
    
    public ProgressDialogActionHolder message(@FormattedExpression String message) {
        dialog.setMessage(message);
        return this;
    }

}
