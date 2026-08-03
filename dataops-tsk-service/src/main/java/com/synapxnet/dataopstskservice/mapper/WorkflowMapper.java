package com.synapxnet.dataopstskservice.mapper;

import com.synapxnet.dataopstskservice.entity.*;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface WorkflowMapper {
    @Select("SELECT * FROM xnet_dataops_tsk_workflow ORDER BY updated_at DESC")
    List<Workflow> findAll();

    @Select("SELECT * FROM xnet_dataops_tsk_workflow WHERE id = #{id}")
    Workflow findById(@Param("id") Long id);

    @Insert("INSERT INTO xnet_dataops_tsk_workflow (uid, name, description, schedule_cron, status, dag_json, created_by) " +
            "VALUES (#{uid}, #{name}, #{description}, #{scheduleCron}, #{status}, #{dagJson}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Workflow workflow);

    @Update("UPDATE xnet_dataops_tsk_workflow SET name=#{name}, description=#{description}, schedule_cron=#{scheduleCron}, status=#{status}, dag_json=#{dagJson} WHERE id=#{id}")
    int update(Workflow workflow);

    @Update("UPDATE xnet_dataops_tsk_workflow SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM xnet_dataops_tsk_workflow WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    // Nodes
    @Select("SELECT * FROM xnet_dataops_tsk_workflow_node WHERE workflow_id = #{workflowId}")
    List<WorkflowNode> findNodesByWorkflowId(@Param("workflowId") Long workflowId);

    @Insert("INSERT INTO xnet_dataops_tsk_workflow_node (workflow_id, node_key, node_name, node_type, config_json, position_x, position_y) " +
            "VALUES (#{workflowId}, #{nodeKey}, #{nodeName}, #{nodeType}, #{configJson}, #{positionX}, #{positionY})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertNode(WorkflowNode node);

    @Delete("DELETE FROM xnet_dataops_tsk_workflow_node WHERE workflow_id = #{workflowId}")
    int deleteNodesByWorkflowId(@Param("workflowId") Long workflowId);

    // Edges
    @Select("SELECT * FROM xnet_dataops_tsk_workflow_edge WHERE workflow_id = #{workflowId}")
    List<WorkflowEdge> findEdgesByWorkflowId(@Param("workflowId") Long workflowId);

    @Insert("INSERT INTO xnet_dataops_tsk_workflow_edge (workflow_id, source_node_key, target_node_key) VALUES (#{workflowId}, #{sourceNodeKey}, #{targetNodeKey})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertEdge(WorkflowEdge edge);

    @Delete("DELETE FROM xnet_dataops_tsk_workflow_edge WHERE workflow_id = #{workflowId}")
    int deleteEdgesByWorkflowId(@Param("workflowId") Long workflowId);

    // TaskInstance
    @Select("SELECT * FROM xnet_dataops_tsk_task_instance ORDER BY created_at DESC LIMIT 100")
    List<TaskInstance> findRecentInstances();

    @Select("SELECT * FROM xnet_dataops_tsk_task_instance WHERE workflow_id = #{workflowId} ORDER BY created_at DESC")
    List<TaskInstance> findInstancesByWorkflowId(@Param("workflowId") Long workflowId);

    @Select("SELECT * FROM xnet_dataops_tsk_task_instance WHERE id = #{id}")
    TaskInstance findInstanceById(@Param("id") Long id);

    /**
     * 根据稳定实例 UID 查询任务实例。
     *
     * @param uid 任务实例 UID
     * @return 任务实例，不存在时返回 null
     */
    @Select("SELECT * FROM xnet_dataops_tsk_task_instance WHERE uid = #{uid} LIMIT 1")
    TaskInstance findInstanceByUid(@Param("uid") String uid);

    @Insert("INSERT INTO xnet_dataops_tsk_task_instance (uid, workflow_id, status, trigger_type, start_time) VALUES (#{uid}, #{workflowId}, #{status}, #{triggerType}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertInstance(TaskInstance instance);

    @Update("UPDATE xnet_dataops_tsk_task_instance SET status=#{status}, end_time=NOW() WHERE id=#{id}")
    int updateInstanceStatus(TaskInstance instance);

    // NodeInstance
    @Select("SELECT * FROM xnet_dataops_tsk_node_instance WHERE task_instance_id = #{taskInstanceId}")
    List<NodeInstance> findNodeInstancesByTaskId(@Param("taskInstanceId") Long taskInstanceId);

    /**
     * 查询不含大日志字段的节点实例，避免默认读取 log_content。
     *
     * @param taskInstanceId 任务实例数字 ID
     * @return 不含日志内容的节点实例
     */
    @Select("SELECT id, task_instance_id, node_key, status, start_time, end_time "
            + "FROM xnet_dataops_tsk_node_instance WHERE task_instance_id = #{taskInstanceId} ORDER BY id")
    List<NodeInstance> findNodeInstancesWithoutLog(@Param("taskInstanceId") Long taskInstanceId);

    @Insert("INSERT INTO xnet_dataops_tsk_node_instance (task_instance_id, node_key, status) VALUES (#{taskInstanceId}, #{nodeKey}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertNodeInstance(NodeInstance nodeInstance);

    @Update("UPDATE xnet_dataops_tsk_node_instance SET status=#{status}, start_time=#{startTime}, end_time=#{endTime}, log_content=#{logContent} WHERE id=#{id}")
    int updateNodeInstance(NodeInstance nodeInstance);
}
