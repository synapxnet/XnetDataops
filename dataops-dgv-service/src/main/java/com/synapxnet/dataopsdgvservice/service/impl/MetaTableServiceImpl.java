package com.synapxnet.dataopsdgvservice.service.impl;

import com.synapxnet.dataopsdgvservice.entity.*;
import com.synapxnet.dataopsdgvservice.mapper.MetaTableMapper;
import com.synapxnet.dataopsdgvservice.service.MetaTableService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class MetaTableServiceImpl implements MetaTableService {
    private final MetaTableMapper mapper;

    public MetaTableServiceImpl(MetaTableMapper mapper) { this.mapper = mapper; }

    @Override public List<MetaTable> listTables(Long datasourceId) {
        if (datasourceId != null) return mapper.findTablesByDatasourceId(datasourceId);
        return mapper.findAllTables();
    }

    @Override public MetaTable getTableById(Long id) {
        MetaTable t = mapper.findTableById(id);
        if (t == null) throw new IllegalArgumentException("MetaTable not found: " + id);
        return t;
    }

    @Override public MetaTable createTable(MetaTable table) {
        table.setUid(UUID.randomUUID().toString());
        if (table.getTableType() == null) table.setTableType("table");
        mapper.insertTable(table);
        return table;
    }

    @Override public MetaTable updateTable(MetaTable table) {
        mapper.updateTable(table);
        return mapper.findTableById(table.getId());
    }

    @Override public void deleteTable(Long id) { mapper.deleteTable(id); }

    @Override public List<MetaColumn> getColumns(Long metaTableId) { return mapper.findColumnsByTableId(metaTableId); }

    @Override public void saveColumns(Long metaTableId, List<MetaColumn> columns) {
        mapper.deleteColumnsByTableId(metaTableId);
        for (MetaColumn c : columns) { c.setMetaTableId(metaTableId); mapper.insertColumn(c); }
    }

    @Override public List<DataLineage> listLineage(Long tableId) {
        if (tableId != null) return mapper.findLineageByTableId(tableId);
        return mapper.findAllLineage();
    }

    @Override public DataLineage createLineage(DataLineage lineage) {
        lineage.setUid(UUID.randomUUID().toString());
        mapper.insertLineage(lineage);
        return lineage;
    }

    @Override public void deleteLineage(Long id) { mapper.deleteLineage(id); }

    @Override public List<DataTag> listTags() { return mapper.findAllTags(); }

    @Override public DataTag createTag(DataTag tag) { mapper.insertTag(tag); return tag; }

    @Override public DataTag updateTag(DataTag tag) { mapper.updateTag(tag); return tag; }

    @Override public void deleteTag(Long id) { mapper.deleteTag(id); }

    @Override public List<DataTag> getTableTags(Long metaTableId) { return mapper.findTagsByTableId(metaTableId); }

    @Override public void addTableTag(Long metaTableId, Long tagId) { mapper.insertTableTag(metaTableId, tagId); }

    @Override public void removeTableTag(Long metaTableId, Long tagId) { mapper.deleteTableTag(metaTableId, tagId); }
}
