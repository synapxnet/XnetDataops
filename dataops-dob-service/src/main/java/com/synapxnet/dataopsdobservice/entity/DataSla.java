package com.synapxnet.dataopsdobservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DataSla {
    private Long id;
    private String uid;
    private String name;
    private String pipelineName;
    private LocalDateTime expectedCompletionTime;
    private LocalDateTime actualCompletionTime;
    private String slaStatus;
    private String date;
    private LocalDateTime createdAt;
}
