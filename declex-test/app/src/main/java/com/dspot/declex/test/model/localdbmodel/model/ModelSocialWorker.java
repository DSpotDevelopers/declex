package com.dspot.declex.test.model.localdbmodel.model;

import com.activeandroid.annotation.Column;
import com.dspot.declex.annotation.LocalDBModel;
import com.activeandroid.Model;
import com.dspot.declex.test.model.usemodel.model.ModelAddress_;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;

@LocalDBModel
public class ModelSocialWorker extends Model {
    @Column
    String workPlace;

    @Column
    String professionalFunctions;

    @Column
    boolean studyUniversity;

    @NotEmpty
    @Column
    float salary;

    @Column
    ModelAddress_ address;
}
