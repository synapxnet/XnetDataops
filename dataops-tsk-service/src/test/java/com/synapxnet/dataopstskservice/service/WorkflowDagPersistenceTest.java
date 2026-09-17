/* Copyright (C) 2026 Synapxnet. All rights reserved.
This file is Synapxnet Proprietary and Confidential. It is strictly
forbidden to copy, distribute, or use without explicit authorization.
用途：工作流安全保存边界。Purpose: Workflow graph persistence boundary.
Author: maoyo | Department: 研发部 | Date: 2026-09-13 | Version: 1.0.0 | Security Level: INTERNAL
__version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
__maintainer__: maoyo | __email__: synapxnet@gmail.com */
package com.synapxnet.dataopstskservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.dataopstskservice.dto.WorkflowDagRequest;
import com.synapxnet.dataopstskservice.entity.*;
import com.synapxnet.dataopstskservice.mapper.WorkflowMapper;
import com.synapxnet.dataopstskservice.service.impl.WorkflowServiceImpl;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowDagPersistenceTest {
    /** HTTP JSON节点必须反序列化成实体而非Map。 HTTP JSON nodes must deserialize into entities rather than maps. */
    @Test void jsonBindsTypedNodes() throws Exception {
        WorkflowDagRequest request = new ObjectMapper().readValue("{\"nodes\":[{\"nodeKey\":\"collect\",\"nodeName\":\"采集\",\"nodeType\":\"sql\",\"configJson\":\"{}\"}],\"edges\":[]}", WorkflowDagRequest.class);
        assertEquals("collect", request.nodes().get(0).getNodeKey());
    }
    /** 无效JSON与循环依赖在删除旧图前被拒绝。 Invalid JSON and cyclic dependencies are rejected before deleting the existing graph. */
    @Test void invalidGraphNeverDeletesExistingData() {
        WorkflowMapper mapper=mock(WorkflowMapper.class);
        when(mapper.findById(1L)).thenReturn(new Workflow());
        WorkflowServiceImpl service=new WorkflowServiceImpl(mapper);
        WorkflowNode first=node("a"), second=node("b");
        first.setConfigJson("not-json");
        assertThrows(IllegalArgumentException.class,()->service.saveDAG(1L,List.of(first),List.of()));
        first.setConfigJson("{}");
        assertThrows(IllegalArgumentException.class,()->service.saveDAG(1L,List.of(first,second),List.of(edge("a","b"),edge("b","a"))));
        assertThrows(IllegalArgumentException.class,()->service.saveDAG(1L,List.of(first),List.of(edge("a","missing"))));
        verify(mapper,never()).deleteEdgesByWorkflowId(anyLong());
        verify(mapper,never()).deleteNodesByWorkflowId(anyLong());
    }
    /** 保存时先移除依赖并将新实体绑定路径工作流。 Save removes dependencies first and binds new entities to the path workflow. */
    @Test void validGraphUsesSafeReplacementOrder() {
        WorkflowMapper mapper=mock(WorkflowMapper.class);
        when(mapper.findById(9L)).thenReturn(new Workflow());
        WorkflowNode first=node("a"), second=node("b");
        WorkflowEdge edge=edge("a","b");
        new WorkflowServiceImpl(mapper).saveDAG(9L,List.of(first,second),List.of(edge));
        var order=inOrder(mapper);
        order.verify(mapper).findById(9L);
        order.verify(mapper).deleteEdgesByWorkflowId(9L);
        order.verify(mapper).deleteNodesByWorkflowId(9L);
        order.verify(mapper).insertNode(first);
        order.verify(mapper).insertNode(second);
        order.verify(mapper).insertEdge(edge);
        assertEquals(9L,first.getWorkflowId());
        assertEquals(9L,edge.getWorkflowId());
    }
    /** 构造最小有效测试节点。 Build a minimally valid test node. */
    private WorkflowNode node(String key) { WorkflowNode node=new WorkflowNode();node.setNodeKey(key);node.setNodeName(key);node.setNodeType("sql");node.setConfigJson("{}");return node; }
    /** 构造指定起点终点的测试依赖。 Build a test edge between the specified endpoints. */
    private WorkflowEdge edge(String source,String target) {WorkflowEdge edge=new WorkflowEdge();edge.setSourceNodeKey(source);edge.setTargetNodeKey(target);return edge;}
}
