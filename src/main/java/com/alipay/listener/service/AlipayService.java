package com.alipay.listener.service;

import com.alipay.listener.model.ConfigModel;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AlipayService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public final String fundApi = "https://mbillexprod.alipay.com/enterprise/fundAccountDetail.htm";
    public final String loginApi = "https://auth.alipay.com?goto=https%3a%2f%2fmbillexprod.alipay.com%2fenterprise%2ffundAccountDetail.htm";
    private final String identity = "alipayLoginListener";
    private final ConfigService configService;
    private final ChromeDriverService driverService;
    private final Scheduler scheduler;

    public AlipayService(Scheduler scheduler, ChromeDriverService driverService, ConfigService configService) {
        this.scheduler = scheduler;
        this.configService = configService;
        this.driverService = driverService;
    }

    public static ChromeDriver jobDriver = null;
    public static ChromeDriver loginDriver = null;

    public void start(){
        if(loginDriver != null) loginDriver.quit();
        ChromeDriver driver = driverService.getDriver(false,false);
        loginDriver = driver;
        try {
            if(driver == null) {
                logger.error("启动失败，请重新启动");
                return;
            }
            driver.get(loginApi);
            Thread.sleep(1000);
            // 检测自动登录
            this.login(driver);

            // 定时任务，检测是否已登录
            JobDataMap dataMap = new JobDataMap();
            dataMap.put("driver",driver);
            JobDetail job = JobBuilder.newJob(AlipayLoginService.class).withIdentity(identity).usingJobData(dataMap).storeDurably().build();

            CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule("*/2 * * * * ?");

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(identity)
                    .withSchedule(cronScheduleBuilder)
                    .build();

            scheduler.scheduleJob(job, trigger);
        }catch (Exception e){
            logger.error(e.getLocalizedMessage());
        }
    }

    public void run(ChromeDriver driver){
        if(jobDriver != null) {
            jobDriver.quit();
            return;
        }
        jobDriver = driver;
        try {
            JobDataMap dataMap = new JobDataMap();
            dataMap.put("driver", driver);
            JobDetail job = JobBuilder.newJob(AlipayJobService.class).withIdentity(identity).usingJobData(dataMap).storeDurably().build();

            CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule("0 */1 * * * ?");

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(identity)
                    .withSchedule(cronScheduleBuilder)
                    .build();

            scheduler.scheduleJob(job, trigger);
            scheduler.triggerJob(job.getKey());
        }catch (Exception e){
            logger.error(e.getLocalizedMessage());
        }
    }

    public void login(ChromeDriver driver) throws InterruptedException {
        ConfigModel configModel = configService.getConfig();
        if(configModel == null || StringUtils.isEmpty(configModel.getUsername())|| StringUtils.isEmpty(configModel.getPassword())) {
            logger.warn("未检测到用户名和密码，不启用自动登录");
            return;
        }

        WebElement showLogin = driver.findElement(By.xpath("//li[@data-status='show_login']"));
        showLogin.click();

        // 账户
        WebElement inputUser = driver.findElement(By.id("J-input-user"));
        if(inputUser.getAttribute("value").length() == 0) {
            for (int i = 0; i < configModel.getUsername().length(); i++) {
                inputUser.sendKeys(configModel.getUsername().substring(i, i + 1));
                Thread.sleep(new Random().nextInt(200));
            }
        }

        // 密码
        WebElement inputPwd = driver.findElement(By.id("password_rsainput"));
        if(inputPwd.getAttribute("value").length() == 0) {
            for (int i = 0; i < configModel.getPassword().length(); i++) {
                inputPwd.sendKeys(configModel.getPassword().substring(i, i + 1));
                Thread.sleep(new Random().nextInt(200));
            }
        }

        WebElement codeElement = driver.findElement(By.id("J-checkcode"));
        if(codeElement.getAttribute("class").indexOf("fn-hide") < 0){
            // 需要验证码
            logger.warn("请输入验证码");
        }else{
            WebElement submitBtn = driver.findElement(By.id("J-login-btn"));
            submitBtn.submit();
        }
    }

    @PreDestroy
    public void destory() throws Exception {
        if(jobDriver != null) {
            jobDriver.quit();
            jobDriver = null;
        }
        if(loginDriver != null) {
            loginDriver.quit();
            loginDriver = null;
        }
    }

    public LocalDateTime prevRefreshTime = LocalDateTime.now();
}
