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

import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;

import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.annotation.Assignable;
import com.dspot.declex.api.action.annotation.FormattedExpression;
import com.dspot.declex.api.action.annotation.StopOn;
import com.dspot.declex.api.action.runnable.dialog.DialogClickRunnable;
import com.dspot.declex.api.action.runnable.dialog.DialogMultiChoiceClickRunnable;

@EBean
@ActionFor("AlertDialog")
public class AlertDialogActionHolder {
    AlertDialog.Builder builder;

    String positiveButtonText;
    int positiveButtonRes;

    String negativeButtonText;
    int negativeButtonRes;

	String[] multiChoiceItems;
	int multiChoiceRes;
	
	String[] items;
	int itemsRes;
	
    AlertDialog dialog;

    @RootContext
    Context context;
        
    void init() {
        builder = new AlertDialog.Builder(context);
    }
    
	//Here you can infer any parameter, the first parameter is the next listener 
    void build(
    		final DialogClickRunnable PositiveButtonPressed, 
    		final DialogClickRunnable NegativeButtonPressed, 
    		final DialogClickRunnable ItemSelected, 
    		final DialogMultiChoiceClickRunnable MultiChoiceSelected,
    		final Runnable Canceled, 
    		final Runnable Dismissed) { 

		if (negativeButtonText != null) {
			builder.setNegativeButton(negativeButtonText, NegativeButtonPressed);
		} else if (negativeButtonRes != 0) {
			builder.setNegativeButton(negativeButtonRes, NegativeButtonPressed);
		}
		
		if (positiveButtonText != null) {
			builder.setPositiveButton(positiveButtonText, PositiveButtonPressed);
		} else if (positiveButtonRes != 0) {
			builder.setPositiveButton(positiveButtonRes, PositiveButtonPressed);
		}
		
		if (items != null) {
			builder.setItems(items, ItemSelected);
		} else if (itemsRes != 0) {
			builder.setItems(itemsRes, ItemSelected);
		}
		
		if (multiChoiceItems != null) {
			builder.setMultiChoiceItems(multiChoiceItems, null, MultiChoiceSelected);
		} else if (multiChoiceRes != 0) {
			builder.setMultiChoiceItems(multiChoiceRes, null, MultiChoiceSelected);
		}
		
        dialog = builder.create();
        
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
					if (Dismissed != null) Dismissed.run();
				}
			});
		}
    };
	
    void execute() {
		dialog.show();
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
    public AlertDialogActionHolder dialog(@Assignable("dialog") Dialog dialog) {
    	return this;
    }
    
    /**
     * @return Android Builder layer to access the underlying Notifications Builder object
     * 
     * @see android.support.v4.app.NotificationCompat.Builder Notifications Builder
     */
    @StopOn("create")
    public AlertDialog.Builder builder() {
    	return this.builder;
    }

    public AlertDialogActionHolder title(@FormattedExpression String title) {
        builder.setTitle(title);
        return this;
    }

    public AlertDialogActionHolder message(@FormattedExpression String message) {
        builder.setMessage(message);
        return this;
    }

    public AlertDialogActionHolder positiveButton(@FormattedExpression String title) {
        positiveButtonText = title;
        return this;
    }

    public AlertDialogActionHolder positiveButton(int res) {
        positiveButtonRes = res;
        return this;
    }

    public AlertDialogActionHolder negativeButton(@FormattedExpression String title) {
        negativeButtonText = title;
        return this;
    }

    public AlertDialogActionHolder negativeButton(int res) {
        negativeButtonRes = res;
        return this;
    }
	
	public AlertDialogActionHolder multiChoice(String ... items) {
		multiChoiceItems = items;
        return this;
    }
	
	public AlertDialogActionHolder multiChoice(List<String> items) {
		multiChoiceItems = new String[items.size()];
		multiChoiceItems = items.toArray(multiChoiceItems);
        return this;
    }
	
	public AlertDialogActionHolder multiChoice(int res) {
		multiChoiceRes = res;
        return this;
    }
	
	public AlertDialogActionHolder items(String ... items) {
		this.items = items;
        return this;
    }
	
	public AlertDialogActionHolder items(List<String> items) {
		this.items = new String[items.size()];
		this.items = items.toArray(this.items);
        return this;
    }
	
	public AlertDialogActionHolder items(int res) {
		this.itemsRes = res;
        return this;
    }

}
