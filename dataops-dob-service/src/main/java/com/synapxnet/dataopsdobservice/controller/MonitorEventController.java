package com.synapxnet.dataopsdobservice.controller;

import com.synapxnet.dataopsdobservice.common.Result;
import com.synapxnet.dataopsdobservice.entity.MonitorEvent;
import com.synapxnet.dataopsdobservice.service.DataMonitorService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dob/events")
public class MonitorEventController {
    private final DataMonitorService service;

    public MonitorEventController(DataMonitorService service) { this.service = service; }

    @GetMapping
    public Result<List<MonitorEvent>> list(@RequestParam(required = false) Long monitorId,
                                           @RequestParam(required = false) String status) {
        return Result.success(service.listEvents(monitorId, status));
    }

    @PutMapping("/{id}/acknowledge")
    public Result<Void> acknowledge(@PathVariable("id") Long id) { service.acknowledgeEvent(id); return Result.success(); }

    @PutMapping("/{id}/resolve")
    public Result<Void> resolve(@PathVariable("id") Long id) { service.resolveEvent(id); return Result.success(); }
}
