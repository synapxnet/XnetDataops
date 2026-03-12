package com.synapxnet.dataopsdgvservice.service;

import com.synapxnet.dataopsdgvservice.entity.*;
import java.util.List;

public interface MetaTableService {
    List<MetaTable> listTables(Long datasourceId);
    MetaTable getTableById(Long id);
    MetaTable createTable(MetaTable table);
    MetaTable updateTable(MetaTable table);
    void deleteTable(Long id);
    List<MetaColumn> getColumns(Long metaTableId);
    void saveColumns(Long metaTableId, List<MetaColumn> columns);
    List<DataLineage> listLineage(Long tableId);
    DataLineage createLineage(DataLineage lineage);
    void deleteLineage(Long id);
    List<DataTag> listTags();
    DataTag createTag(DataTag tag);
    DataTag updateTag(DataTag tag);
    void deleteTag(Long id);
    List<DataTag> getTableTags(Long metaTableId);
    void addTableTag(Long metaTableId, Long tagId);
    void removeTableTag(Long metaTableId, Long tagId);
}
