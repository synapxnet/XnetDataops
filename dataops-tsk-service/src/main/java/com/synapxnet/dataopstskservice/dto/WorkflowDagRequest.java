/* Copyright (C) 2026 Synapxnet. All rights reserved.
This file is Synapxnet Proprietary and Confidential. It is strictly
forbidden to copy, distribute, or use without explicit authorization.
用途：工作流安全保存边界。Purpose: Workflow graph persistence boundary.
Author: maoyo | Department: 研发部 | Date: 2026-09-13 | Version: 1.0.0 | Security Level: INTERNAL
__version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
__maintainer__: maoyo | __email__: synapxnet@gmail.com */
package com.synapxnet.dataopstskservice.dto;

import com.synapxnet.dataopstskservice.entity.WorkflowNode;
import com.synapxnet.dataopstskservice.entity.WorkflowEdge;
import java.util.List;

/** 强类型反序列化工作流节点与边。 Deserialize graph nodes and edges into typed entities. */
public record WorkflowDagRequest(List<WorkflowNode> nodes, List<WorkflowEdge> edges) {}
