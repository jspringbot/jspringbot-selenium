package org.jspringbot.keyword.selenium;

import com.saucelabs.common.SauceOnDemandAuthentication;
import com.saucelabs.common.Utils;
import com.saucelabs.saucerest.SauceREST;
import org.apache.commons.lang.StringUtils;
import org.jspringbot.lifecycle.LifeCycleAdapter;
import org.jspringbot.lifecycle.RobotListenerHandler;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

public class SauceLabsLifeCycleHandler extends LifeCycleAdapter implements InitializingBean, RobotListenerHandler {

    @Autowired
    private SauceOnDemandAuthentication authentication;
    @Autowired
    private RemoteWebDriver remoteWebDriver;
    private SauceREST sauceREST;

    private boolean failed = false;

    @Override
    public void endSuite(String name, Map attributes) {
        if(StringUtils.equals(String.valueOf(attributes.get("status")), "FAIL")) {
            fail();
        }
    }

    @Override
    public void endTest(String name, Map attributes) {
        if(StringUtils.equals(String.valueOf(attributes.get("status")), "FAIL")) {
            fail();
        }
    }

    public void fail() {
        if(failed) {
            return;
        }

        String sessionId = remoteWebDriver.getSessionId().toString();
        Map<String, Object> updates = new HashMap<String, Object>();
        updates.put("passed", false);
        Utils.addBuildNumberToUpdate(updates);
        this.sauceREST.updateJobInfo(sessionId, updates);

        failed = true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        sauceREST = new SauceREST(authentication.getUsername(), authentication.getAccessKey());
    }

    @Override
    public void logMessage(Map map) {
    }

    @Override
    public void message(Map map) {
    }

    @Override
    public void outputFile(String s) {
    }

    @Override
    public void logFile(String s) {
    }

    @Override
    public void reportFile(String s) {
    }

    @Override
    public void debugFile(String s) {
    }

    @Override
    public void close() {
        if(!failed) {
            String sessionId = remoteWebDriver.getSessionId().toString();
            Map<String, Object> updates = new HashMap<String, Object>();
            updates.put("passed", true);
            Utils.addBuildNumberToUpdate(updates);
            this.sauceREST.updateJobInfo(sessionId, updates);
            String authLink = this.sauceREST.getPublicJobLink(sessionId);
            System.out.println("Job link: " + authLink);
        }
    }
}
