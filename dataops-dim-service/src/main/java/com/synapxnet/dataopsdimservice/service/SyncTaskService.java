package com.synapxnet.dataopsdimservice.service;

import com.synapxnet.dataopsdimservice.entity.SyncTask;
import com.synapxnet.dataopsdimservice.entity.FieldMapping;
import com.synapxnet.dataopsdimservice.entity.SyncLog;
import java.util.List;

public interface SyncTaskService {
    List<SyncTask> listAll();
    SyncTask getById(Long id);
    SyncTask create(SyncTask task);
    SyncTask update(SyncTask task);
    void delete(Long id);
    void updateStatus(Long id, String status);
    List<FieldMapping> getMappings(Long taskId);
    void saveMappings(Long taskId, List<FieldMapping> mappings);
    List<SyncLog> getLogs(Long taskId);
}
