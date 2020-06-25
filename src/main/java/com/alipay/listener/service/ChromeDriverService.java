package com.alipay.listener.service;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChromeDriverService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ChromeDriver driver;

    public ChromeDriver getDriver(boolean isMax,boolean headless){
        try {
            String osName = System.getProperties().getProperty("os.name");
            if(osName.indexOf("Mac") >= 0) {
                System.setProperty("webdriver.chrome.driver", "./driver/chromedriver");
            }else if(osName.indexOf("Windows") >= 0){
                System.setProperty("webdriver.chrome.driver", "./driver/chromedriver.exe");
            }else if(osName.indexOf("Linux") >= 0){
                System.setProperty("webdriver.chrome.driver", "./driver/chromedriver_linux");
            }
            ChromeOptions chromeOptions = new ChromeOptions();
            List<String> arguments = new LinkedList<>();
            arguments.add("lang=zh_CN.UTF-8");
            arguments.add("user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.106 Safari/537.36");
            arguments.add("--no-sandbox");
            arguments.add("--disable-extensions");
            arguments.add("--disable-infobars");
            if(headless) arguments.add("--headless");
            // 最大化
            if(isMax) arguments.add("--start-maximized");
            // mac 最大化
            if(isMax) arguments.add("--kiosk");
            chromeOptions.addArguments(arguments);
            chromeOptions.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
            this.driver = new ChromeDriver(chromeOptions);
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("source", "var newProto = navigator.__proto__;"+
                    "delete newProto.webdriver;"+
                    "window.chrome={app:{isInstalled:false,\"InstallState\":{\"DISABLED\":\"disabled\",\"INSTALLED\":\"installed\",\"NOT_INSTALLED\":\"not_installed\"},\"RunningState\":{\"CANNOT_RUN\":\"cannot_run\",\"READY_TO_RUN\":\"ready_to_run\",\"RUNNING\":\"running\"}},\"runtime\":{\"OnInstalledReason\":{\"CHROME_UPDATE\":\"chrome_update\",\"INSTALL\":\"install\",\"SHARED_MODULE_UPDATE\":\"shared_module_update\",\"UPDATE\":\"update\"},\"OnRestartRequiredReason\":{\"APP_UPDATE\":\"app_update\",\"OS_UPDATE\":\"os_update\",\"PERIODIC\":\"periodic\"},\"PlatformArch\":{\"ARM\":\"arm\",\"ARM64\":\"arm64\",\"MIPS\":\"mips\",\"MIPS64\":\"mips64\",\"X86_32\":\"x86-32\",\"X86_64\":\"x86-64\"},\"PlatformNaclArch\":{\"ARM\":\"arm\",\"MIPS\":\"mips\",\"MIPS64\":\"mips64\",\"X86_32\":\"x86-32\",\"X86_64\":\"x86-64\"},\"PlatformOs\":{\"ANDROID\":\"android\",\"CROS\":\"cros\",\"LINUX\":\"linux\",\"MAC\":\"mac\",\"OPENBSD\":\"openbsd\",\"WIN\":\"win\"},\"RequestUpdateCheckStatus\":{\"NO_UPDATE\":\"no_update\",\"THROTTLED\":\"throttled\",\"UPDATE_AVAILABLE\":\"update_available\"}}};"+
                    "newProto.permissions={query:function(parameters){ return parameters.name === 'notifications' ? Promise.resolve({ state: Notification.permission }) : originalQuery(parameters) }};"+
                    "newProto.plugins=[{},{},{}];"+
                    "newProto.language='zh-CN';newProto.languages=['zh-CN', 'zh', 'en'];" +
                    "navigator.__proto__ = newProto;" );
            this.driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", parameters);
        }catch (Exception e){
            logger.error(e.getLocalizedMessage());
        }
        return this.driver;
    }
}
