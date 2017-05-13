package com.dspot.declex.api.external;

import com.dspot.declex.api.action.runnable.OnFailedRunnable;

public interface PopulateModelListener {
	void populateModel(Runnable afterPopulate, OnFailedRunnable onFailed);
}
