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
     * 业务类型 (ukey申请/企业信息审核/企业认证/...)
     */
    private String businessType;

    /**
     * 流程定义key
     */
    private String processKey;

    /**
     * 流程定义名称
     */
    private String processName;

}
