
package org.akvo.flow.api.dao;

import java.util.List;

import com.gallatinsystems.common.Constants;
import com.gallatinsystems.framework.dao.BaseDAO;
import com.gallatinsystems.framework.servlet.PersistenceFilter;
import com.gallatinsystems.survey.domain.SurveyGroup;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

public class FolderDAO extends BaseDAO<SurveyGroup> {

    public FolderDAO() {
	super(SurveyGroup.class);
    }

    public List<SurveyGroup> listAll() {
	return super.listByProperty("projectType", SurveyGroup.ProjectType.PROJECT_FOLDER, "String");
    }
}
