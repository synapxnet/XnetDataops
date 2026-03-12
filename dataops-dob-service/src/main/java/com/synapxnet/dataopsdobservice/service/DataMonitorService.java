package com.synapxnet.dataopsdobservice.service;

import com.synapxnet.dataopsdobservice.entity.DataMonitor;
import com.synapxnet.dataopsdobservice.entity.MonitorEvent;
import com.synapxnet.dataopsdobservice.entity.DataSla;
import java.util.List;
import java.util.Map;

public interface DataMonitorService {
    // Monitors
    List<DataMonitor> listMonitors();
    DataMonitor getMonitorById(Long id);
    DataMonitor createMonitor(DataMonitor monitor);
    DataMonitor updateMonitor(DataMonitor monitor);
    void deleteMonitor(Long id);
    DataMonitor toggleMonitor(Long id);

    // Events
    List<MonitorEvent> listEvents(Long monitorId, String status);
    void acknowledgeEvent(Long id);
    void resolveEvent(Long id);

    // SLA
    List<DataSla> listSlas();
    DataSla createSla(DataSla sla);
    List<Map<String, Object>> getSlaStats();
}
