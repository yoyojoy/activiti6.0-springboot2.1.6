package com.shengyecapital.process.dto.ao;

import com.shengyecapital.common.dto.common.BasePageDto;
import com.shengyecapital.process.dto.BaseEnvVo;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class DeployedProcessListQueryAo extends BasePageDto {

    /**
     * 流程名称
     */
    private String processDefinitionName;

    /**
     * 流程标识key
     */
    private String processDefinitionKey;

    private String startTime;

    private String endTime;

    /**
     * 业务类别
     */
    private String businessType;

    /**
     * 商户ID标识
     */
    private String tenantId;

}
