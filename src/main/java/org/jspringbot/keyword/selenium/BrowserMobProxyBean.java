package org.jspringbot.keyword.selenium;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.IOException;

public class BrowserMobProxyBean implements InitializingBean, DisposableBean {
    private DesiredCapabilities capabilities;
    private boolean enable;
    private BrowserMobProxy server;
    private File harDir;
    private String lastName;
    private Har lastHar;

    public BrowserMobProxyBean(DesiredCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public void setHarDir(String harDir) {
        if(!StringUtils.equalsIgnoreCase(harDir,"none")) {
            this.harDir = new File(harDir);

            if (!this.harDir.isDirectory()) {
                this.harDir.mkdirs();
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if(enable) {
            server = new BrowserMobProxyServer();
            server.start();
            int port = server.getPort();

            Proxy proxy = ClientUtil.createSeleniumProxy(server);
            proxy.setHttpProxy("127.0.0.1:" + port);
            proxy.setSslProxy("127.0.0.1:" + port);

            capabilities.setCapability(CapabilityType.PROXY, proxy);

            System.out.println("BrowserMobProxyServer@" + port);
        }
    }

    public void newHar(String name) {
        validate();

        lastName = name;
        server.newHar(name);
    }

    private void validate() {
        if(!enable || server == null) {
            throw new IllegalStateException("BrowserMobProxyServer should be enabled.");
        }
    }

    public void getHar() throws IOException {
        validate();

        if(harDir == null) {
            throw new IllegalStateException("No configured har saved directory.");
        }
        if(lastName == null) {
            throw new IllegalStateException("New Har should be invoked before save Har.");
        }

        lastHar = server.getHar();
    }

    public void saveHar() throws IOException {
        if(lastHar == null) {
            throw new IllegalStateException("No har found.");
        }

        File newFile = new File(harDir, lastName + ".har");
        lastHar.writeTo(newFile);
    }

    public int getHarTransferredSize() throws IOException {
        if(lastHar == null) {
            throw new IllegalStateException("No har found.");
        }

        int size = 0;
        for(HarEntry entry : lastHar.getLog().getEntries()) {
            size += entry.getResponse().getBodySize();
        }

        return size;
    }

    public int getHarPageLoadTime() throws IOException {
        if(lastHar == null) {
            throw new IllegalStateException("No har found.");
        }

        int time = 0;
        for(HarEntry entry : lastHar.getLog().getEntries()) {
            time += entry.getTime();
        }

        return time;
    }

    @Override
    public void destroy() throws Exception {
        server.stop();
    }
}
