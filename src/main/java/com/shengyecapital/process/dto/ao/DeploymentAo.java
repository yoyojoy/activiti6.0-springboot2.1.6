package com.shengyecapital.process.dto.ao;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class DeploymentAo {

    /**
     * 流程文件
     */
    private MultipartFile file;

    /**
     * 业务类别 (登记/回款/业务开通/...)
     */
    private String businessType;

}
