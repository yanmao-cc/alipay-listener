package com.alipay.listener;

import com.alipay.listener.service.AlipayService;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.stereotype.Component;

@Component
public class ApplicationEventListener  implements ApplicationListener {

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ContextStoppedEvent || applicationEvent instanceof ContextClosedEvent){
            if(AlipayService.jobDriver != null) {
                AlipayService.jobDriver.quit();
                AlipayService.jobDriver = null;
            }
            if(AlipayService.loginDriver != null) {
                AlipayService.loginDriver.quit();
                AlipayService.loginDriver = null;
            }
        }
    }
}
