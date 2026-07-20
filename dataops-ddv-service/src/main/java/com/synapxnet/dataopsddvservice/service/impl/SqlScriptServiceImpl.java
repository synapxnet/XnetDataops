package com.synapxnet.dataopsddvservice.service.impl;

import com.synapxnet.dataopsddvservice.entity.SqlScript;
import com.synapxnet.dataopsddvservice.entity.QueryHistory;
import com.synapxnet.dataopsddvservice.entity.SavedQuery;
import com.synapxnet.dataopsddvservice.mapper.SqlScriptMapper;
import com.synapxnet.dataopsddvservice.service.SqlScriptService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class SqlScriptServiceImpl implements SqlScriptService {
    private final SqlScriptMapper mapper;

    public SqlScriptServiceImpl(SqlScriptMapper mapper) { this.mapper = mapper; }

    @Override public List<SqlScript> listScripts() { return mapper.findAllScripts(); }

    @Override public SqlScript getScriptById(Long id) {
        SqlScript s = mapper.findScriptById(id);
        if (s == null) throw new IllegalArgumentException("Script not found: " + id);
        return s;
    }

    @Override public SqlScript createScript(SqlScript script) {
        script.setUid(UUID.randomUUID().toString());
        if (script.getStatus() == null) script.setStatus("draft");
        if (script.getScriptType() == null) script.setScriptType("sql");
        if (script.getFolderPath() == null) script.setFolderPath("/");
        mapper.insertScript(script);
        return script;
    }

    @Override public SqlScript updateScript(SqlScript script) {
        mapper.updateScript(script);
        return mapper.findScriptById(script.getId());
    }

    @Override public void deleteScript(Long id) { mapper.deleteScript(id); }

    @Override public List<QueryHistory> getHistory(Long datasourceId) {
        if (datasourceId != null) return mapper.findHistoryByDatasource(datasourceId);
        return mapper.findRecentHistory();
    }

    @Override public QueryHistory recordHistory(QueryHistory history) {
        history.setUid(UUID.randomUUID().toString());
        mapper.insertHistory(history);
        return history;
    }

    @Override public List<SavedQuery> listSavedQueries() { return mapper.findAllSaved(); }

    @Override public SavedQuery createSavedQuery(SavedQuery saved) {
        saved.setUid(UUID.randomUUID().toString());
        mapper.insertSaved(saved);
        return saved;
    }

    @Override public SavedQuery updateSavedQuery(SavedQuery saved) {
        mapper.updateSaved(saved);
        return mapper.findSavedById(saved.getId());
    }

    @Override public void deleteSavedQuery(Long id) { mapper.deleteSaved(id); }
}
