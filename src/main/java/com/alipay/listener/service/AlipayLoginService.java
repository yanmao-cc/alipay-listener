package com.alipay.listener.service;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.chrome.ChromeDriver;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Set;

public class AlipayLoginService extends QuartzJobBean {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ChromeDriverService driverService;
    private final AlipayService alipayService;

    public AlipayLoginService(ChromeDriverService driverService, AlipayService alipayService) {
        this.driverService = driverService;
        this.alipayService = alipayService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDetail jobDetail = context.getJobDetail();
        JobDataMap dataMap = jobDetail.getJobDataMap();
        ChromeDriver driver = (ChromeDriver)dataMap.get("driver");
        try {
            String currentUrl = driver.getCurrentUrl();
            if (currentUrl.indexOf("login/index.htm") > 0 || currentUrl.indexOf("auth.alipay.com") > 0) {
                logger.warn("等待登录中...");
            } else if (currentUrl.indexOf("checkSecurity.html") > 0) {
                logger.warn("等待安全验证中...");
            } else {
                logger.info("登录成功");
                // 获取全部 cookie
                Set<Cookie> cookies = driver.manage().getCookies();
                // 删除当前定时检测登录任务
                context.getScheduler().deleteJob(jobDetail.getKey());
                // 退出当前登录浏览器
                driver.quit();
                // 获取一个无头浏览器，用于订单扫描
                ChromeDriver orderDriver = driverService.getDriver(true,true);
                orderDriver.get(alipayService.fundApi);
                // 删除所有cookie，重新添加
                orderDriver.manage().deleteAllCookies();
                for (Cookie cookie : cookies){
                    orderDriver.manage().addCookie(cookie);
                }
                // 重新刷新页面
                orderDriver.get(alipayService.fundApi);
                logger.info("开始监听订单...");
                alipayService.run(orderDriver);
            }
        }catch (Exception e){
            driver.quit();
            logger.error(e.getLocalizedMessage());
        }
    }
}
