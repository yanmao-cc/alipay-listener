# alipay-listener

#### 浏览器版本
当前浏览器驱动和版本都是使用的 Chrome 83.0.4103.106 版本

驱动在 driver 目录下

免安装的 Chrome 浏览器由于超过100M无法上传，我已上传到网盘

```$xslt
链接：https://pan.baidu.com/s/1s6FkbcTHDBesFx9vDG54HA 
提取码：yqvx
```

下载后在 service/ChromeDriverService 中设置：

```$xslt
if(osName.indexOf("Windows") >= 0){
    chromeOptions.setBinary("./binary/chrome-windows/chrome.exe");
    ...
}
```

#### 配置文件
在项目根目录下创建 config.ini 可以写入以下参数

username:支付宝用户名，填写后可自动登录，不填写就手动登录

password:支付宝密码，填写后可自动登录，不填写就手动登录

api:抓取到的订单需要POST的地址

#### 问题
打包后 out/artifacts/listener_jar 目录中会出现 servelet-api-2.3.jar 会和 Tomact 冲突，需要手动删除