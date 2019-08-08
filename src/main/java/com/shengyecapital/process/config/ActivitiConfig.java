package com.shengyecapital.process.config;

import org.activiti.engine.*;
import org.activiti.engine.impl.persistence.StrongUuidGenerator;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 配置activiti
 */
@Configuration
public class ActivitiConfig {
    @Autowired
    private DataSource dataSource;
    @Autowired
    private Environment env;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public SpringProcessEngineConfiguration springProcessEngineConfiguration(){
        SpringProcessEngineConfiguration springProcessEngineConfiguration = new SpringProcessEngineConfiguration();
        springProcessEngineConfiguration.setTransactionManager(transactionManager);
        springProcessEngineConfiguration.setDataSource(dataSource);
        springProcessEngineConfiguration
                .setIdGenerator(new StrongUuidGenerator())
                .setDatabaseType(env.getProperty("spring.activiti.database-schema"))
                .setDatabaseSchemaUpdate(env.getProperty("spring.activiti.database-schema-update"))
                .setLabelFontName(env.getProperty("spring.activiti.labelFontName"))
                .setActivityFontName(env.getProperty("spring.activiti.activityFontName"))
                .setAsyncExecutorActivate(Boolean.valueOf(env.getProperty("spring.activiti.async-executor-activate")))
                .setCreateDiagramOnDeploy(true)
                .setDbIdentityUsed(Boolean.valueOf(env.getProperty("spring.activiti.db-identity-used")))
                .setHistory(env.getProperty("spring.activiti.history-level"))
                .setDbHistoryUsed(Boolean.valueOf(env.getProperty("spring.activiti.db-history-used")));
        return springProcessEngineConfiguration;
    }

    public ProcessEngine getProcessEngine() {
        return this.springProcessEngineConfiguration().buildProcessEngine();
    }

    @Bean
    public RuntimeService getRuntimeService(){
        return this.getProcessEngine().getRuntimeService();
    }

    @Bean
    public RepositoryService getRepositoryService(){
        return this.getProcessEngine().getRepositoryService();
    }

    @Bean
    public FormService getFormService(){
        return this.getProcessEngine().getFormService();
    }

    @Bean
    public IdentityService getIdentityService(){
        return this.getProcessEngine().getIdentityService();
    }

    @Bean
    public HistoryService getHistoryService(){
        return this.getProcessEngine().getHistoryService();
    }

    @Bean
    public ManagementService getManagementService(){
        return this.getProcessEngine().getManagementService();
    }

    @Bean
    public TaskService getTaskService(){
        return this.getProcessEngine().getTaskService();
    }

    @Bean
    public DynamicBpmnService getDynamicBpmnService(){
        return this.getProcessEngine().getDynamicBpmnService();
    }

}
