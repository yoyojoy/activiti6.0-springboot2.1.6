package com.shengyecapital.process.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author jacky.liu
 */
@Data
public abstract class BasePageDto {
    @ApiModelProperty(value = "pageNum")
    @NotNull(message = "pageNum不能为空")
    private Integer pageNum = 1;

    @ApiModelProperty(value = "pageSize")
    @NotNull(message = "pageSize不能为空")
    private Integer pageSize = 10;
}
