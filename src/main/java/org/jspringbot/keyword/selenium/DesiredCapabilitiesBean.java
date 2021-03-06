package org.jspringbot.keyword.selenium;

import com.google.common.io.Files;
import com.saucelabs.common.Utils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DesiredCapabilitiesBean implements InitializingBean, DisposableBean {
    private static final Logger LOGGER = Logger.getLogger(DesiredCapabilitiesBean.class);

    private DesiredCapabilities capabilities;

    private Proxy proxy;

    private File baseDir;

    private ChromeOptions chromeOptions;

    private List<String> chromeOptionArguments;

    private Map<String, Object> mobileEmulation;

    private LoggingPreferences logPrefs;

    private String chromeDriverVersion;

    private String ieDriverVersion;

    private boolean archAutodetect = false;

    private String archValue = "32";

    private File tempDir;

    private File chromeDriverFile;

    public DesiredCapabilitiesBean(DesiredCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    public void setFirefoxProfile(FirefoxProfile profile) {
        capabilities.setCapability(FirefoxDriver.PROFILE, profile);
    }

    public void setChromeDriverVersion(String chromeDriverVersion) {
        this.chromeDriverVersion = chromeDriverVersion;
    }

    public void setIeDriverVersion(String ieDriverVersion) {
        this.ieDriverVersion = ieDriverVersion;
    }

    public void setBaseDir(String baseStrDir) {
        if (StringUtils.isNotBlank(baseStrDir) && !StringUtils.equalsIgnoreCase(baseStrDir, "none")) {
            baseDir = new File(baseStrDir);
            if (!baseDir.isDirectory()) {
                baseDir.mkdirs();
            }
        }
    }

    public void setArchAutodetect(boolean archAutodetect) {
        this.archAutodetect = archAutodetect;
    }

    public void setArchValue(String archValue) {
        this.archValue = archValue;
    }

    public void setChromeDrivers(Map<OsCheck.OSType, Resource> chromeDrivers) throws IOException {
        OsCheck.OSType osType = OsCheck.getOperatingSystemType();

        Resource chromeDriver = chromeDrivers.get(osType);

        if (chromeDriver == null) {
            throw new IllegalArgumentException("Unsupported OS " + osType.name());
        }

        File driverDir = createDriverDir();

        File downloadedFile = new File(driverDir, chromeDriver.getFilename());

        if (!downloadedFile.isFile()) {
            LOGGER.info("Chrome driver version: " + chromeDriverVersion);
            LOGGER.info("Downloading driver: " + chromeDriver.getURL());
            IOUtils.copy(chromeDriver.getInputStream(), new FileOutputStream(downloadedFile));
        }

        LOGGER.info("Chrome driver file: " + downloadedFile.getAbsolutePath());

        tempDir = Files.createTempDir();
        chromeDriverFile = unzip(new FileInputStream(downloadedFile), tempDir);
        chromeDriverFile.setExecutable(true);

        System.setProperty("webdriver.chrome.driver", chromeDriverFile.getAbsolutePath());
    }

    @Override
    public void destroy() throws Exception {
        if(chromeDriverFile != null && chromeDriverFile.isFile() && chromeDriverFile.isFile()) {
            chromeDriverFile.delete();
        }

        if(tempDir != null && tempDir.isDirectory()) {
            tempDir.delete();
        }
    }

    private File createDriverDir() {
        File driverDir;
        if (baseDir != null) {
            driverDir = baseDir;
        } else {
            String userHome = System.getProperty("user.home");
            driverDir = new File(userHome, "jspringbot");

            if (!driverDir.isDirectory()) {
                driverDir.mkdirs();
            }
        }
        return driverDir;
    }

    public void setIeDriver(Map<String, Resource> resourceMap) throws IOException {
        File driverDir = createDriverDir();

        String arch = archValue;
        if (archAutodetect) {
            arch = System.getProperty("sun.arch.data.model");
        }

        Resource resource = resourceMap.get(arch);

        File downloadedFile = new File(driverDir, resource.getFilename());
        if (!downloadedFile.isFile()) {
            LOGGER.info("Internet driver version" + ieDriverVersion);
            LOGGER.info("Downloading driver: " + resource.getURL());
            IOUtils.copy(resource.getInputStream(), new FileOutputStream(downloadedFile));
        }

        LOGGER.info("IE driver file: " + downloadedFile.getAbsolutePath());

        File driver = unzip(new FileInputStream(downloadedFile), driverDir);
        driver.setExecutable(true);

        System.setProperty("webdriver.ie.driver", driver.getAbsolutePath());
    }

    public void setChromeOptions(ChromeOptions chromeOptions) {
        this.chromeOptions = chromeOptions;
    }

    public void setChromeDeviceMetrics(String deviceMetrics) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (StringUtils.isNotBlank(deviceMetrics) && !StringUtils.equalsIgnoreCase(deviceMetrics, "none")) {
            if (mobileEmulation == null) {
                mobileEmulation = new HashMap<String, Object>();
            }

            String[] metrics = StringUtils.split(deviceMetrics, "x");

            if (metrics.length < 2) {
                throw new IllegalArgumentException("Expected <width>x<height>x<pixel_ratio> but was " + deviceMetrics);
            }

            final String[] NAMES = {"width", "height", "pixelRatio"};
            final Class[] CLASSES = {Integer.class, Integer.class, Double.class};
            Map<String, Object> deviceMetricsMap = new HashMap<String, Object>();

            for (int i = 0; i < metrics.length && i < NAMES.length; i++) {
                deviceMetricsMap.put(NAMES[i], classValueOf(CLASSES[i], metrics[i]));
            }

            mobileEmulation.put("deviceMetrics", deviceMetricsMap);
        }
    }

    private Object classValueOf(Class clazz, String item) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = clazz.getDeclaredMethod("valueOf", String.class);

        return method.invoke(clazz, item);
    }

    public void setChromeDeviceEmulation(String deviceEmulation) {
        if (StringUtils.isNotBlank(deviceEmulation) && !StringUtils.equalsIgnoreCase(deviceEmulation, "none")) {
            if (mobileEmulation == null) {
                mobileEmulation = new HashMap<String, Object>();
            }

            mobileEmulation.put("deviceName", deviceEmulation);
        }
    }

    public void setChromeDeviceUserAgent(String userAgent) {
        if (StringUtils.isNotBlank(userAgent) && !StringUtils.equalsIgnoreCase(userAgent, "none")) {
            if (mobileEmulation == null) {
                mobileEmulation = new HashMap<String, Object>();
            }

            mobileEmulation.put("userAgent", userAgent);
        }
    }

    public void setChromeBrowserLog(String level) {
        if (StringUtils.isNotBlank(level) && !StringUtils.equalsIgnoreCase(level, "none")) {
            if (logPrefs == null) {
                logPrefs = new LoggingPreferences();
            }

            logPrefs.enable(LogType.BROWSER, Level.parse(level));
        }
    }

    public void setChromePerformanceLog(String level) {
        if (StringUtils.isNotBlank(level) && !StringUtils.equalsIgnoreCase(level, "none")) {
            if (logPrefs == null) {
                logPrefs = new LoggingPreferences();
            }

            logPrefs.enable(LogType.PERFORMANCE, Level.parse(level));
        }
    }

    public static File unzip(InputStream in, File dir) throws IOException {
        ZipInputStream zin = null;
        byte[] buf = new byte[2048];

        File entryFile = null;
        try {
            zin = new ZipInputStream(in);

            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                FileOutputStream out = null;
                entryFile = new File(dir, entry.getName());

                try {
                    out = new FileOutputStream(entryFile);
                    int len;
                    while ((len = zin.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                } finally {
                    IOUtils.closeQuietly(out);
                }
            }
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(zin);
        }

        return entryFile;
    }

    public void setChromeLogFile(String logFile) {
        if (StringUtils.isNotBlank(logFile) && !StringUtils.equalsIgnoreCase(logFile, "none")) {
            File file = new File(logFile);
            File dir = file.getParentFile();

            if (dir != null && !dir.isDirectory()) {
                dir.mkdirs();
            }

            System.setProperty("webdriver.chrome.logfile", file.getAbsolutePath());
        }
    }

    public void setBrowserName(String browserName) {
        if (!StringUtils.equalsIgnoreCase(browserName, "none")) {
            capabilities.setCapability(CapabilityType.BROWSER_NAME, browserName);
        }
    }

    public void setVersion(String version) {
        if (!StringUtils.equalsIgnoreCase(version, "none")) {
            capabilities.setCapability(CapabilityType.VERSION, version);
        }
    }

    public void setPlatform(String platform) {
        if (!StringUtils.equalsIgnoreCase(platform, "none")) {
            capabilities.setCapability(CapabilityType.PLATFORM, platform);
        }
    }

    public void setPlatformVersion(String platformVersion) {
        if (!StringUtils.equalsIgnoreCase(platformVersion, "none")) {
            capabilities.setCapability("platformVersion", platformVersion);
        }
    }

    public void setDeviceName(String deviceName) {
        if (!StringUtils.equalsIgnoreCase(deviceName, "none")) {
            capabilities.setCapability("deviceName", deviceName);
        }
    }

    public void setDeviceOrientation(String deviceOrientation) {
        if (!StringUtils.equalsIgnoreCase(deviceOrientation, "none")) {
            if (StringUtils.equals(String.valueOf(capabilities.getCapability("deviceType")), "phone")) {
                capabilities.setCapability("deviceOrientation", deviceOrientation);
            } else {
                capabilities.setCapability("device-orientation", deviceOrientation);
            }
        }
    }

    public void setDeviceType(String deviceType) {
        if (!StringUtils.equalsIgnoreCase(deviceType, "none")) {
            capabilities.setCapability("deviceType", deviceType);
        }
    }

    public void setAppiumVersion(String appiumVersion) {
        if (!StringUtils.equalsIgnoreCase(appiumVersion, "none")) {
            capabilities.setCapability("appiumVersion", appiumVersion);
        }
    }

    public void setName(String name) {
        if (!StringUtils.equalsIgnoreCase(name, "none")) {
            capabilities.setCapability("name", name);
        }
    }

    public void setBuild(String build) {
        if (!StringUtils.equalsIgnoreCase(build, "none")) {
            capabilities.setCapability("build", build);
        } else {
            Map<String, Object> map = new HashMap<String, Object>();
            Utils.addBuildNumberToUpdate(map);

            if (map.containsKey("build")) {
                capabilities.setCapability("build", map.get("build"));
            }
        }
    }

    public void setTunnelId(String tunnelId) {
        if (!StringUtils.equalsIgnoreCase(tunnelId, "none")) {
            capabilities.setCapability("tunnel-identifier", tunnelId);
        }
    }

    public void setMaxDuration(String maxDuration) {
        if (!StringUtils.equalsIgnoreCase(maxDuration, "none")) {
            capabilities.setCapability("maxDuration", Integer.parseInt(maxDuration));
        }
    }

    public void setProxy(String proxyHost) {
        if (!StringUtils.equalsIgnoreCase(proxyHost, "none")) {
            proxy = new Proxy();

            proxy.setFtpProxy(proxyHost)
                    .setHttpProxy(proxyHost)
                    .setSslProxy(proxyHost);

            capabilities.setCapability(CapabilityType.PROXY, proxy);
        }
    }

    public void setSslProxy(String proxyHost) {
        if (!StringUtils.equalsIgnoreCase(proxyHost, "none")) {
            proxy = new Proxy();
            proxy.setSslProxy(proxyHost);

            capabilities.setCapability(CapabilityType.PROXY, proxy);
        }
    }

    public void setFtpProxy(String proxyHost) {
        if (!StringUtils.equalsIgnoreCase(proxyHost, "none")) {
            proxy = new Proxy();
            proxy.setFtpProxy(proxyHost);

            capabilities.setCapability(CapabilityType.PROXY, proxy);
        }
    }

    public void setHttpProxy(String proxyHost) {
        if (!StringUtils.equalsIgnoreCase(proxyHost, "none")) {
            proxy = new Proxy();
            proxy.setHttpProxy(proxyHost);

            capabilities.setCapability(CapabilityType.PROXY, proxy);
        }
    }

    @SuppressWarnings("unchecked")
    public void setCapabilities(String properties) throws JSONException {
        if (!StringUtils.equalsIgnoreCase(properties, "none")) {
            JSONObject obj = new JSONObject(properties);

            Iterator<String> itr = obj.keys();
            while (itr.hasNext()) {
                String key = itr.next();
                capabilities.setCapability(key, obj.getString(key));
            }
        }
    }

    public void setChromeOptionArguments(List<String> arguments) {
        this.chromeOptionArguments = arguments;
    }

    public void afterPropertiesSet() throws Exception {
        if (MapUtils.isNotEmpty(mobileEmulation)) {
            if(chromeOptions == null) {
                chromeOptions = new ChromeOptions();
            }
            chromeOptions.setExperimentalOption("mobileEmulation", mobileEmulation);
        }

        if(CollectionUtils.isNotEmpty(chromeOptionArguments)) {
            if(chromeOptions == null) {
                chromeOptions = new ChromeOptions();
            }

            chromeOptions.addArguments(chromeOptionArguments);
        }
        if (chromeOptions != null) {
            capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
        }
        if (logPrefs != null) {
            capabilities.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);
        }
    }
}
