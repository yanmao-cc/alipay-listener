package com.alipay.listener;

import com.alipay.listener.service.AlipayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import sun.misc.Signal;
import sun.misc.SignalHandler;

@SpringBootApplication
public class ListenerApplication {

	private final static Logger logger = LoggerFactory.getLogger(ListenerApplication.class);

	public static void main(String[] args) {
		// 在Spring Boot应用中通过监听信号量和注册关闭钩子来实现在进程退出之前执行收尾工作
		// 监听信号量
		SignalHandler signalHandler = new SignalHandler() {
			@Override
			public void handle(Signal signal) {
				logger.info("do signal handle: {}", signal.getName());
				System.exit(0);
			}
		};
		Signal.handle(new Signal("TERM"), signalHandler);
		Signal.handle(new Signal("ABRT"), signalHandler);
		// 注册关闭钩子
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				// 执行收尾工作
				logger.info("do something on shutdown hook");
				if(AlipayService.jobDriver != null) {
					AlipayService.jobDriver.quit();
					AlipayService.jobDriver = null;
				}
				if(AlipayService.loginDriver != null) {
					AlipayService.loginDriver.quit();
					AlipayService.loginDriver = null;
				}
			}
		});

		SpringApplication application = new SpringApplication(ListenerApplication.class);
		application.addListeners(new ApplicationEventListener());
		application.run(args);
	}
}
