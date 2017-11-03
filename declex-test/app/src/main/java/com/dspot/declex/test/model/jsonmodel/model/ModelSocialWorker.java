package com.dspot.declex.test.model.jsonmodel.model;


import com.dspot.declex.annotation.JsonModel;
import com.dspot.declex.test.model.usemodel.model.ModelAddress_;

@JsonModel
public class ModelSocialWorker {
    String work_place;
    String professional_functions;
    boolean study_university;

    ModelAddress_ address;
}
