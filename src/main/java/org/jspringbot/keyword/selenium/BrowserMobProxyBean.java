package org.jspringbot.keyword.selenium;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import org.apache.commons.lang.StringUtils;
import org.jspringbot.syntax.HighlightRobotLogger;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class BrowserMobProxyBean implements InitializingBean, DisposableBean {
    private DesiredCapabilities capabilities;
    private boolean enable;
    private BrowserMobProxy server;
    private File harDir;
    private String lastName;
    private Har lastHar;
    private Pattern excludeMimeTypePattern;
    private Pattern includeMimeTypePattern;
    private Pattern excludeUrlPattern;
    private Pattern includeUrlPattern;

    public static final HighlightRobotLogger LOG = HighlightRobotLogger.getLogger(BrowserMobProxyBean.class);

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

    public void setExcludeMimeTypePattern(String excludeMimeTypePattern) {
        if(!StringUtils.equalsIgnoreCase(excludeMimeTypePattern, "none")) {
            this.excludeMimeTypePattern = Pattern.compile(excludeMimeTypePattern, Pattern.CASE_INSENSITIVE);
        }
    }

    public void setIncludeMimeTypePattern(String includeMimeTypePattern) {
        if(!StringUtils.equalsIgnoreCase(includeMimeTypePattern, "none")) {
            this.includeMimeTypePattern = Pattern.compile(includeMimeTypePattern, Pattern.CASE_INSENSITIVE);
        }
    }

    public void setExcludeUrlPattern(String excludeUrlPattern) {
        if(!StringUtils.equalsIgnoreCase(excludeUrlPattern, "none")) {
            this.excludeUrlPattern = Pattern.compile(excludeUrlPattern, Pattern.CASE_INSENSITIVE);
        }
    }

    public void setIncludeUrlPattern(String includeUrlPattern) {
        if(!StringUtils.equalsIgnoreCase(includeUrlPattern, "none")) {
            this.includeUrlPattern = Pattern.compile(includeUrlPattern, Pattern.CASE_INSENSITIVE);
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
            if(isInclude(entry)) {
                size += entry.getResponse().getBodySize();
            }
        }

        return size;
    }

    public List<HarEntry> getHarEntries() throws IOException {
        if(lastHar == null) {
            throw new IllegalStateException("No har found.");
        }

        return lastHar.getLog().getEntries();
    }


    private boolean isInclude(HarEntry entry) {
        String mimeType = entry.getResponse().getContent().getMimeType();
        String url = entry.getRequest().getUrl();

        if(excludeMimeTypePattern != null && excludeMimeTypePattern.matcher(mimeType).matches()) {
            System.out.println("excluded: " + url);
            return false;
        }

        if(includeMimeTypePattern != null && !includeMimeTypePattern.matcher(mimeType).matches()) {
            System.out.println("excluded: " + url);
            return false;
        }

        if(excludeUrlPattern != null && excludeUrlPattern.matcher(url).matches()) {
            System.out.println("excluded: " + url);
            return false;
        }
        if(includeUrlPattern != null && !includeUrlPattern.matcher(url).matches()) {
            System.out.println("excluded: " + url);
            return false;
        }

        return true;
    }

    public int getHarPageLoadTime() throws IOException {
        if(lastHar == null) {
            throw new IllegalStateException("No har found.");
        }

        int time = 0;
        for(HarEntry entry : lastHar.getLog().getEntries()) {
            if(isInclude(entry)) {
                time += entry.getTime();
            }
        }

        return time;
    }

    @Override
    public void destroy() throws Exception {
        if (server != null) {
            server.stop();
        }
    }
}
