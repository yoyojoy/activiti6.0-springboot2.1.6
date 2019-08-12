package com.shengyecapital.process.dto.ao;

import com.shengyecapital.process.common.BasePageDto;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class HistoryProcessListQueryAo extends BasePageDto {

    @NotEmpty
    private String processDefinitionKey;

    @NotEmpty
    private String tenantId;
}
