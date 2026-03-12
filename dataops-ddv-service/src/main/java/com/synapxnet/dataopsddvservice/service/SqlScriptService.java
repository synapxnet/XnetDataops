package com.synapxnet.dataopsddvservice.service;

import com.synapxnet.dataopsddvservice.entity.SqlScript;
import com.synapxnet.dataopsddvservice.entity.QueryHistory;
import com.synapxnet.dataopsddvservice.entity.SavedQuery;
import java.util.List;

public interface SqlScriptService {
    List<SqlScript> listScripts();
    SqlScript getScriptById(Long id);
    SqlScript createScript(SqlScript script);
    SqlScript updateScript(SqlScript script);
    void deleteScript(Long id);
    List<QueryHistory> getHistory(Long datasourceId);
    QueryHistory recordHistory(QueryHistory history);
    List<SavedQuery> listSavedQueries();
    SavedQuery createSavedQuery(SavedQuery saved);
    SavedQuery updateSavedQuery(SavedQuery saved);
    void deleteSavedQuery(Long id);
}
