package com.synapxnet.dataopsdobservice.service.impl;

import com.synapxnet.dataopsdobservice.entity.DataMonitor;
import com.synapxnet.dataopsdobservice.entity.MonitorEvent;
import com.synapxnet.dataopsdobservice.entity.DataSla;
import com.synapxnet.dataopsdobservice.mapper.DataMonitorMapper;
import com.synapxnet.dataopsdobservice.service.DataMonitorService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DataMonitorServiceImpl implements DataMonitorService {
    private final DataMonitorMapper mapper;

    public DataMonitorServiceImpl(DataMonitorMapper mapper) { this.mapper = mapper; }

    // ===== Monitors =====
    @Override public List<DataMonitor> listMonitors() { return mapper.findAllMonitors(); }

    @Override public DataMonitor getMonitorById(Long id) {
        DataMonitor monitor = mapper.findMonitorById(id);
        if (monitor == null) throw new IllegalArgumentException("DataMonitor not found: " + id);
        return monitor;
    }

    @Override public DataMonitor createMonitor(DataMonitor monitor) {
        monitor.setUid(UUID.randomUUID().toString());
        if (monitor.getAlertLevel() == null) monitor.setAlertLevel("warning");
        if (monitor.getEnabled() == null) monitor.setEnabled(true);
        mapper.insertMonitor(monitor);
        return monitor;
    }

    @Override public DataMonitor updateMonitor(DataMonitor monitor) {
        mapper.updateMonitor(monitor);
        return mapper.findMonitorById(monitor.getId());
    }

    @Override public void deleteMonitor(Long id) { mapper.deleteMonitor(id); }

    @Override public DataMonitor toggleMonitor(Long id) {
        DataMonitor monitor = getMonitorById(id);
        Boolean newState = !Boolean.TRUE.equals(monitor.getEnabled());
        mapper.toggleMonitor(id, newState);
        return mapper.findMonitorById(id);
    }

    // ===== Events =====
    @Override public List<MonitorEvent> listEvents(Long monitorId, String status) {
        if (monitorId != null && status != null && !status.isEmpty()) {
            return mapper.findEventsByMonitorIdAndStatus(monitorId, status);
        }
        if (monitorId != null) return mapper.findEventsByMonitorId(monitorId);
        if (status != null && !status.isEmpty()) return mapper.findEventsByStatus(status);
        return mapper.findRecentEvents();
    }

    @Override public void acknowledgeEvent(Long id) { mapper.updateEventStatus(id, "acknowledged"); }

    @Override public void resolveEvent(Long id) { mapper.resolveEvent(id); }

    // ===== SLA =====
    @Override public List<DataSla> listSlas() { return mapper.findAllSlas(); }

    @Override public DataSla createSla(DataSla sla) {
        sla.setUid(UUID.randomUUID().toString());
        if (sla.getSlaStatus() == null) sla.setSlaStatus("pending");
        mapper.insertSla(sla);
        return sla;
    }

    @Override public List<Map<String, Object>> getSlaStats() { return mapper.getSlaStats(); }
}
