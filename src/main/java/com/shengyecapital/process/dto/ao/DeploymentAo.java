package com.shengyecapital.process.dto.ao;

import com.shengyecapital.process.dto.BaseEnvVo;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class DeploymentAo extends BaseEnvVo {

    /**
     * 流程文件
     */
    private MultipartFile file;

    /**
     * 业务类别 (登记/回款/业务开通/...)
     */
    private String businessType;

}
