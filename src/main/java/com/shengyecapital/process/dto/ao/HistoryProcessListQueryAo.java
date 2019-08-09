package com.shengyecapital.process.dto.ao;

import com.shengyecapital.process.common.BasePageDto;
import lombok.Data;

@Data
public class HistoryProcessListQueryAo extends BasePageDto {

    private String processDefinitionKey;

    private String tenantId;
}
