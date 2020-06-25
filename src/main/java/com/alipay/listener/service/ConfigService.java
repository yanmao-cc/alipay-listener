package com.alipay.listener.service;

import com.alipay.listener.model.ConfigModel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

@Service
public class ConfigService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String path = "./config.ini";

    public ConfigModel getConfig(){
        File configFile = new File(path);
        if(configFile.exists()){
            try{
                InputStreamReader inputReader = new InputStreamReader(new FileInputStream(configFile),"UTF-8");
                BufferedReader bf = new BufferedReader(inputReader);
                // 按行读取字符串
                String config;
                ConfigModel configModel = new ConfigModel();
                while ((config = bf.readLine()) != null) {
                    if(StringUtils.isEmpty(config)) continue;
                    String value = config.substring(config.indexOf(":") + 1);
                    if(config.indexOf("username:") == 0){
                        configModel.setUsername(value);
                    }
                    else if(config.indexOf("password:") == 0){
                        configModel.setPassword(value);
                    }
                    else if(config.indexOf("api:") == 0){
                        configModel.setApi(value);
                    }
                }
                bf.close();
                inputReader.close();
                return configModel;
            }catch (Exception e){
                logger.error(e.getLocalizedMessage());
            }
        }
        return null;
    }
}
