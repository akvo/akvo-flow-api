
package org.akvo.flow.api.dao;

import com.gallatinsystems.framework.dao.BaseDAO;
import com.gallatinsystems.survey.domain.SurveyGroup;

import java.util.List;

public class SurveyDAO extends BaseDAO<SurveyGroup> {

    public SurveyDAO() {
	super(SurveyGroup.class);
    }

    public List<SurveyGroup> listAll() {
	return super.listByProperty("projectType", SurveyGroup.ProjectType.PROJECT, "String");
    }
}
