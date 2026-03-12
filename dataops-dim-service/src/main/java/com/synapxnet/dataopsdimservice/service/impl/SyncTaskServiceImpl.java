package com.synapxnet.dataopsdimservice.service.impl;

import com.synapxnet.dataopsdimservice.entity.SyncTask;
import com.synapxnet.dataopsdimservice.entity.FieldMapping;
import com.synapxnet.dataopsdimservice.entity.SyncLog;
import com.synapxnet.dataopsdimservice.mapper.SyncTaskMapper;
import com.synapxnet.dataopsdimservice.service.SyncTaskService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SyncTaskServiceImpl implements SyncTaskService {

    private final SyncTaskMapper syncTaskMapper;

    public SyncTaskServiceImpl(SyncTaskMapper syncTaskMapper) {
        this.syncTaskMapper = syncTaskMapper;
    }

    @Override
    public List<SyncTask> listAll() { return syncTaskMapper.findAll(); }

    @Override
    public SyncTask getById(Long id) {
        SyncTask task = syncTaskMapper.findById(id);
        if (task == null) throw new IllegalArgumentException("SyncTask not found: " + id);
        return task;
    }

    @Override
    public SyncTask create(SyncTask task) {
        task.setUid(UUID.randomUUID().toString());
        if (task.getStatus() == null) task.setStatus("draft");
        if (task.getSyncMode() == null) task.setSyncMode("full");
        syncTaskMapper.insert(task);
        return task;
    }

    @Override
    public SyncTask update(SyncTask task) {
        syncTaskMapper.update(task);
        return syncTaskMapper.findById(task.getId());
    }

    @Override
    public void delete(Long id) { syncTaskMapper.deleteById(id); }

    @Override
    public void updateStatus(Long id, String status) { syncTaskMapper.updateStatus(id, status); }

    @Override
    public List<FieldMapping> getMappings(Long taskId) { return syncTaskMapper.findMappingsByTaskId(taskId); }

    @Override
    public void saveMappings(Long taskId, List<FieldMapping> mappings) {
        syncTaskMapper.deleteMappingsByTaskId(taskId);
        for (FieldMapping m : mappings) {
            m.setTaskId(taskId);
            syncTaskMapper.insertMapping(m);
        }
    }

    @Override
    public List<SyncLog> getLogs(Long taskId) { return syncTaskMapper.findLogsByTaskId(taskId); }
}
