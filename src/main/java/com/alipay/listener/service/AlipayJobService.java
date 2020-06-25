package com.alipay.listener.service;

import com.alipay.listener.model.OrderModel;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AlipayJobService extends QuartzJobBean {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final AlipayService alipayService;
    private final LocalOrderService localOrderService;

    public AlipayJobService(AlipayService alipayService, LocalOrderService localOrderService) {
        this.alipayService = alipayService;
        this.localOrderService = localOrderService;
    }

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        JobDataMap dataMap = jobDetail.getJobDataMap();
        ChromeDriver driver = (ChromeDriver)dataMap.get("driver");
        try {
            String currentUrl = driver.getCurrentUrl();
            // 登录失效
            if(currentUrl.indexOf("https://mbillexprod.alipay.com/enterprise/fundAccountDetail.htm") < 0) {
                // 删除定时任务
                jobExecutionContext.getScheduler().deleteJob(jobDetail.getKey());
                // 退出当前浏览器
                driver.quit();
                // 打开登录页面，重新开始
                alipayService.start();
                return;
            }
            try {
                driver.executeScript("document.getElementById('pc-merchant-onlineService').remove()");

            }catch (NoSuchElementException | NoClassDefFoundError | WebDriverException ex){}
            Duration duration = Duration.between(alipayService.prevRefreshTime,LocalDateTime.now());
            if(duration.toMinutes() >= 5){
                driver.navigate().refresh();
                logger.info("刷新一次页面，延长Cookie有效期");
                alipayService.prevRefreshTime = LocalDateTime.now();
                Thread.sleep(500);
                jobExecutionContext.getScheduler().triggerJob(jobDetail.getKey());
                return;
            }
            Wait<ChromeDriver> driverWait = new FluentWait<>(driver)
                    .withTimeout(Duration.ofSeconds(10))
                    .pollingEvery(Duration.ofMillis(20))
                    .ignoring(NoSuchElementException.class).ignoring(WebDriverException.class).ignoring(NoClassDefFoundError.class);


            // 查找 root 部分
            WebElement rootElement = null;
            try {
                rootElement = driverWait.until(webdriver ->  webdriver.findElement(By.id("root")));

            }catch (NoSuchElementException | NoClassDefFoundError | WebDriverException  ex){
                driver.navigate().refresh();
                jobExecutionContext.getScheduler().triggerJob(jobDetail.getKey());
                return;
            }

            Wait<WebElement> rootWait = new FluentWait<>(rootElement)
                    .withTimeout(Duration.ofSeconds(10))
                    .pollingEvery(Duration.ofMillis(20))
                    .ignoring(NoSuchElementException.class).ignoring(WebDriverException.class).ignoring(NoClassDefFoundError.class);

            // 查找时间段，日期比当前时间晚主动刷新
            WebElement dateRangeElement = rootWait.until(rootEle -> rootEle.findElement(By.xpath(".//div[starts-with(@class,'dateRangeWrapper_')]")));

            List<WebElement> dateInputs = dateRangeElement.findElements(By.xpath(".//input[@class='ant-calendar-range-picker-input']"));
            if(dateInputs.size() == 2){
                String endDateString = dateInputs.get(1).getAttribute("value");
                LocalDateTime endDate = LocalDateTime.parse(endDateString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                if(endDate.isBefore(LocalDateTime.now())){
                    driver.navigate().refresh();
                    try {
                        rootElement = driverWait.until(webdriver ->  webdriver.findElement(By.id("root")));
                    }catch (NoSuchElementException | NoClassDefFoundError | WebDriverException  ex){
                        driver.navigate().refresh();
                        Thread.sleep(500);
                        jobExecutionContext.getScheduler().triggerJob(jobDetail.getKey());
                        return;
                    }
                }
            }
            // 查找展开参数按钮
            WebElement moreQueryBtn = rootElement.findElement(By.xpath(".//a[starts-with(@class,'moreQueryTips_')]"));
            if("展开".equals(moreQueryBtn.getAttribute("textContent"))){
                driver.executeScript("window.scrollTo(0, 0);");
                Thread.sleep(200);
                moreQueryBtn.click();
                Thread.sleep(200);
            }
            // 查找转账类型
            WebElement accountTypeElement = rootElement.findElement(By.xpath(".//div[@id='accountType']/div"));
            if(!"转账".equals(accountTypeElement.getAttribute("textContent"))) {
                String accountTypeControlsId = accountTypeElement.getAttribute("aria-controls");
                accountTypeElement.click();
                Thread.sleep(200);
                WebElement transferElement = driver.findElement(By.xpath(".//div[@id='" + accountTypeControlsId + "']/ul/li[text()='转账']"));
                transferElement.click();
            }
            // 查找最近7天按钮
            WebElement timeDivBtn = rootElement.findElement(By.xpath(".//div[text()='最近7天']"));

            String timeClass = timeDivBtn.getAttribute("class");
            if(timeClass.indexOf("active") < 0){
                timeDivBtn.click();
            }else {
                // 查找查询按钮
                WebElement searchBtn = rootElement.findElement(By.xpath(".//span[text()='查 询']/parent::button"));
                searchBtn.click();
            }
            Thread.sleep(500);
            // 查找列表详情页面
            WebElement detailElement = rootElement.findElement(By.xpath(".//div[starts-with(@class,'Detail_')]"));
            try {
                rootElement.findElement(By.xpath(".//div[starts-with(@class,'emptyText_')]"));
                // 暂无记录
                logger.warn("暂无账单信息");
                return;
            }catch (NoSuchElementException | NoClassDefFoundError | WebDriverException ex){

            }
            try {
                // 查找分页选项
                Wait<WebElement> wait = new FluentWait<>(detailElement)
                        .withTimeout(Duration.ofSeconds(5))
                        .pollingEvery(Duration.ofMillis(20))
                        .ignoring(NoSuchElementException.class).ignoring(WebDriverException.class).ignoring(NoClassDefFoundError.class);

                // 设置分页条数
                WebElement paginationElement = wait.until(element -> element.findElement(By.cssSelector("li.ant-pagination-options")));
                // 查询选择器
                WebElement paginationSelect = paginationElement.findElement(By.cssSelector("div.ant-pagination-options-size-changer div.ant-select-selection"));
                String paginationControlsId = paginationSelect.getAttribute("aria-controls");
                WebElement lastPageElement = null;
                try{
                    lastPageElement = driver.findElement(By.xpath("//div[@id='" + paginationControlsId + "']/ul/li[last()]"));
                    String currentPageText = paginationSelect.getAttribute("textContent");
                    String lastPageText = lastPageElement.getAttribute("textContent");
                    if(!currentPageText.equals(lastPageText)){
                        paginationSelect.click();
                    }
                }catch (NoSuchElementException | NoClassDefFoundError | WebDriverException e){
                    driver.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                    paginationSelect.click();
                    Thread.sleep(500);
                    lastPageElement = driver.findElement(By.xpath("//div[@id='" + paginationControlsId + "']/ul/li[last()]"));
                }

                // 获取最大的页码并选中
                String currentPageText = paginationSelect.getAttribute("textContent");
                String lastPageText = lastPageElement.getAttribute("textContent");
                if(!currentPageText.equals(lastPageText)){
                    lastPageElement.click();
                }
            }catch (NoSuchElementException | NoClassDefFoundError | WebDriverException ex){
                logger.error(ex.getLocalizedMessage());
            }
            // 读取记录
            WebElement tableWrapperElement = rootElement.findElement(By.xpath(".//div[starts-with(@class,'ant-table-wrapper table_')]"));
            List<WebElement> trElements = tableWrapperElement.findElements(By.xpath(".//div[@class='ant-table-body']/table/tbody/tr"));
            List<OrderModel> orderModels = new LinkedList<>();
            LinkedHashMap<String,OrderModel> allOrders = localOrderService.getList();
            for (WebElement tr : trElements) {
                List<WebElement> tdElements = tr.findElements(By.tagName("td"));
                // 时间
                String createdTime = tdElements.get(0).getAttribute("textContent");
                // 支付宝流水号
                String order = tdElements.get(1).findElement(By.xpath(".//span[starts-with(@class,'displayText_')]")).getAttribute("title");
                // 商户订单号
                String mOrder = "";
                try{
                    mOrder = tdElements.get(2).findElement(By.xpath(".//span[starts-with(@class,'displayText_')]")).getAttribute("title");
                }catch (NoSuchElementException | NoClassDefFoundError | WebDriverException ex ){}
                // 付款者信息
                String infoHtml = tdElements.get(3).findElement(By.xpath(".//div")).getAttribute("innerHTML");
                String[] infoArray = infoHtml.split("<br>");
                // 类型
                String type = tdElements.get(4).getAttribute("textContent").trim();
                if(!"转账".equals(type)) continue;
                // 金额
                String amountText = tdElements.get(5).getAttribute("textContent").trim();
                Double amount = Double.valueOf(amountText);
                if(amount <= 0) continue;
                if(!allOrders.containsKey(order)){
                    OrderModel orderModel = new OrderModel(order,amount,infoArray[0],infoArray[1],createdTime);
                    orderModels.add(orderModel);
                    // 发送到api
                    new Thread(new HttpSendThread(orderModel)).start();
                }
                logger.info(MessageFormat.format("查询到 {0} , {1} , {2} {3}",order,amountText,infoArray[0],allOrders.containsKey(order) ? "订单已存在" : "已记录订单"));
            }
            if(orderModels.size() > 0){
                Thread thread = new Thread(new LocalOrderThread(orderModels));
                thread.start();
            }

        }catch (NoSuchElementException | NoClassDefFoundError | WebDriverException ex){
            logger.error(ex.getLocalizedMessage());
        }catch (Exception e){
            logger.error(e.getLocalizedMessage());
        }
    }
}
