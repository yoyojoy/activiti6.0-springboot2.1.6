package com.shengyecapital.process.model;

import lombok.Data;

import java.util.Date;

@Deprecated
@Data
public class Item {
    private int id;
    private String name;
    private String detail;
    private Date createTime;
}
