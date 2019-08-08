package com.shengyecapital.process.service.item;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Deprecated
@Data
public class ProcessJson {

    private Base base;
    private List<Task> tasks;
    private List<Customer> customers;

    @Data
    public class Base{
        String processDefinitionKey;
        String processDefinitionName;
    }

    @Data
    public class Task{
        String id;
        String taskKey;
        String customerKey;
        String name;
        String[] assigneeList;
        Map<String, Object> variables;
    }

    @Data
    public class Customer{
        String key;
        String name;
        String processDealType;
    }
}
