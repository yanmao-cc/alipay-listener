# alipay-listener

支付宝商家转账记录抓取，Mac 、Windows 都有效

#### 技术栈
springboot2.3

selenium

#### 浏览器版本
当前浏览器驱动和版本都是使用的 Chrome 83.0.4103.106 版本

驱动在 driver 目录下，驱动有做过更改，请不要随意更换

为了让浏览器驱动和当前安装的chrome浏览器版本一致，我已将免安装（就是安装好后复制的安装目录的文件）的 Chrome 浏览器传到网盘（github 由于超过100M无法上传），暂时只有windows版本，其它版本可以搜索下载

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

遇到验证码同样需要手动登录。为了您的账户安全，请不要在你不可控的设备上留下你的支付宝帐号密码

api:抓取到的订单需要POST的地址

#### 问题
打包后 out/artifacts/listener_jar 目录中会出现 servelet-api-2.3.jar 会和 Tomact 冲突，需要手动删除
