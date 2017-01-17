package org.jspringbot.keyword.selenium;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import java.util.logging.LogManager;

public class ConfigureJULToUseConfig implements InitializingBean {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        LogManager manager = LogManager.getLogManager();

        Resource resource = applicationContext.getResource("classpath:logging.properties");
        manager.readConfiguration(resource.getInputStream());
    }
}
