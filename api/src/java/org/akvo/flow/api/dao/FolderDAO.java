
package org.akvo.flow.api.dao;

import com.gallatinsystems.framework.dao.BaseDAO;
import com.gallatinsystems.survey.domain.SurveyGroup;

import java.util.List;

public class FolderDAO extends BaseDAO<SurveyGroup> {

    public FolderDAO() {
	super(SurveyGroup.class);
    }

    public List<SurveyGroup> listAll() {
	return super.listByProperty("projectType", SurveyGroup.ProjectType.PROJECT_FOLDER, "String");
    }
}
