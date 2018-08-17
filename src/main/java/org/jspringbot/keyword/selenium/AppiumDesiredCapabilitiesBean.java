package org.jspringbot.keyword.selenium;

import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.remote.MobilePlatform;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;

public class AppiumDesiredCapabilitiesBean {
    private static final Log LOGGER = LogFactory.getLog(AppiumDesiredCapabilitiesBean.class);

    private DesiredCapabilities capabilities;

    public AppiumDesiredCapabilitiesBean(DesiredCapabilities capabilities) {
        this.capabilities = capabilities;

        //Set the Desired Capabilities
        capabilities.setCapability(MobileCapabilityType.BROWSER_NAME, "");
    }


    public void setBrowserName(String browserName) {
        if (!StringUtils.equalsIgnoreCase(browserName, "none")) {
            capabilities.setCapability(MobileCapabilityType.BROWSER_NAME, browserName);
        }
    }

    public void setApp(String app) {
        if (!StringUtils.equalsIgnoreCase(app, "none")) {
            File appFile = new File(app);
            capabilities.setCapability(MobileCapabilityType.APP, appFile.getAbsolutePath());
        }
    }

    public void setDeviceName(String deviceName) {
        if (!StringUtils.equalsIgnoreCase(deviceName, "none")) {
            capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, deviceName);
        }
    }

    public void setPlatformName(String platformName) {
        if (!StringUtils.equalsIgnoreCase(platformName, "none")) {
            capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, platformName);
        }
    }

    public void setPlatformVersion(String platformVersion) {
        if (!StringUtils.equalsIgnoreCase(platformVersion, "none")) {
            capabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, platformVersion);
        }
    }

    public void setNoReset(String noReset) {
        if (!StringUtils.equalsIgnoreCase(noReset, "none")) {
            capabilities.setCapability(MobileCapabilityType.NO_RESET, Boolean.parseBoolean(noReset));
        }
    }

    public void setAppPackage(String appPackage) {
        if (!StringUtils.equalsIgnoreCase(appPackage, "none")) {
            capabilities.setCapability("appPackage", appPackage);
        }
    }

    public void setAppActivity(String appActivity) {
        if (!StringUtils.equalsIgnoreCase(appActivity, "none")) {
            capabilities.setCapability("appActivity", appActivity);
        }
    }
}
