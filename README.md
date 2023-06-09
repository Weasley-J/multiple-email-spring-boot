# multiple-email-spring-boot-starter

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/mojohaus/templating-maven-plugin.svg?label=License)](http://www.apache.org/licenses/)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.weasley-j/multiple-email-spring-boot-starter)](https://search.maven.org/artifact/io.github.weasley-j/multiple-email-spring-boot-starter)

本项目已提交至maven中央仓库，你可以直接在项目`pom.xml`中引入使用, 找个[最新版](https://search.maven.org/search?q=multiple-email-spring-boot-starter)引入坐标即可:

```xml
<dependency>
  <groupId>io.github.weasley-j</groupId>
  <artifactId>multiple-email-spring-boot-starter</artifactId>
  <version>${multiple-email.verison}</version>
</dependency>
```

**版本适配说明**

| 版本  | 适配`spring-boot`版本 | JDK版本 | 备注 |
| ----- | --------------------- | ------- | ---- |
| 1.x.x | 2.x.x                 | >=  17  |      |
| 3.x.x | 3.x.x                 | < 17    |      |



## 1 快速上手

> 本`starter`提供的核心功能：
>
> 1. 提供多邮件模板发送邮件支持
> 2. 使用`@Email(name=“yourTemplateName”)`指定以哪一个邮箱模板发送
> 3. 使用`EmailTemplate`直接`@Resouce`注入`IOC`，然后调用此对象示例的`send`方法直接发送
> 4. 支持两种类型的邮件：（1）简单文本邮件;（2）带附件支持HTML显示的邮件

对`spring-boot-starter-mail`进行增强, [实践项目](https://github.com/Weasley-J/lejing-mall)，以下是一些运行要求:

| item        | requirement                           | remark |
| ----------- | ------------------------------------- | ------ |
| SpringBoot  | 2.2.0.RELEASE  <= version <= 3.0.0-M3 |        |
| JDK         | JDK1.8 or latest                      |        |
| Environment | Spring Web Application                |        |

## 2 采用`application.yml`配置

### 2.1 修改配置文件: application-email.yml

```yaml
spring:
  #默认邮件模板
  mail:
    host: "smtp.189.cn"
    port: 465
    username: "xxx@189.cn"
    password: "your_password"
    protocol: "smtp"
    properties:
      mail:
        smtp:
          ssl:
            enable: true
        debug: false
    # 多邮件模板配置列表
    email-templates:
      #qq邮件模板
      - template-name: EmailQQ
        mail-properties:
          host: "smtp.qq.com"
          port: 465
          username: "xxx@qq.com"
          password: "your_password"
          protocol: "smtp"
          properties:
            mail:
              smtp:
                ssl:
                  enable: true
              debug: false
      #outlook邮件模板
      - template-name: EmailOffice365
        mail-properties:
          host: "smtp.office365.com"
          port: 587
          username: "xxx@outlook.com"
          password: "your_password"
          protocol: "smtp"
          properties:
            mail:
              smtp:
                starttls:
                  enable: true
              debug: false
      #163邮件模板
      - template-name: Email163
        mail-properties:
          host: "smtp.163.com"
          port: 587
          username: "xxx@163.com"
          password: "your_password"
          protocol: "smtp"
          properties:
            mail:
              smtp:
                ssl:
                  enable: true
              debug: false
```

### 2.2 需要发邮件服务导入`multiple-email-spring-boot-starter`的maven坐标

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>lejing-mall</artifactId>
        <groupId>cn.alphahub.mall</groupId>
        <version>1.2.1</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <groupId>cn.alphahub.mall</groupId>
    <artifactId>lejing-site-reserve</artifactId>
    <version>1.2.1</version>
    <description>乐璟商城-场地预约服务</description>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
    </properties>

    <dependencies>
        <!-- 邮件支持模块 -->
        <dependency>
            <groupId>io.github.weasley-j</groupId>
            <artifactId>multiple-email-spring-boot-starter</artifactId>
            <version>1.0.7</version>
        </dependency>
          <!-- spring web启动器 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- spring boot测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>${project.artifactId}</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

### 2.3 需要发邮件的服务加载`multiple-email-spring-boot-starter` 模块邮件的配置元数据

路径: `lejing-common/multiple-email-spring-boot-starter/src/main/resources/application-email.yml`

**application-email.yml**里面的元数据作为**共享配置数据**的方式引入其他需要发送邮件的服务共享，通过**spring.profiles.include=email**
的方式引入，也可以直接写在`application.yml`里面；

需要发邮件的目标服务的**application.yml**配置如下：

```yaml
spring:
  application:
    name: lejing-site-reserve
  profiles:
    active: dev
    #加载common工程配置文件 'application-email.yml' 邮件配置元数据, spring.profiles.include: email
    include: email
```

### 2.4 使用核心注解@Email指定邮件模板

```java
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 提供不同邮件模板发送邮件的注解
 *
 * @author lwj
 * @version 1.0.0
 * @apiNote 基于此注解解析不同的邮件模板, 使用注解@Email指定以哪个模板发送邮件
 */
@Documented
@Target({TYPE, TYPE_USE, TYPE_PARAMETER, METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Email {
    /**
     * 默认模板名称
     */
    String DEFAULT_TEMPLATE = "DEFAULT";

    /**
     * 邮件模板名称，默认：DEFAULT
     *
     * @return 邮件模板名称
     */
    String name() default DEFAULT_TEMPLATE;
}
```

使用方式见`1.6`节`EmailController`

### 2.5 使用EmailTemplate发送邮件

- 核心模型

![image-20210910235419528](https://alphahub-test-bucket.oss-cn-shanghai.aliyuncs.com/image/image-20210910235419528.png)

- `EmailTemplate`提供了两个核心方法如下：

```java
    /**
     * 发送给定的简单邮件消息
     *
     * @param domain the message to send
     * @throws MailException Base class for all mail exceptions
     */
    public void send(@Valid SimpleMailMessageDomain domain) throws MailException {
        // no dump
    }

    /**
     * 发送带附件的邮件
     *
     * @param domain metadata of message to send
     * @param file   Nullable, support for spring MVC upload file received in the request, can be null.
     * @throws MailException Base class for all mail exceptions
     */
    public void send(@Valid MimeMessageDomain domain, @Nullable MultipartFile file) throws MessagingException {
         // no dump
    }
```

### 2.6 编写EmailController发送邮件

```java
import cn.alphahub.multiple.email.EmailTemplate;
import cn.alphahub.multiple.email.annotation.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.mail.MessagingException;

import static cn.alphahub.multiple.email.EmailTemplate.MimeMessageDomain;
import static cn.alphahub.multiple.email.EmailTemplate.SimpleMailMessageDomain;

/**
 * 邮件Controller
 *
 * @author lwj
 * @version 1.0
 * @date 2021-09-09 14:01
 */
@Slf4j
@RestController
@RequestMapping("/site/email")
public class EmailController {

    @Resource
    private EmailTemplate emailTemplate;

    /**
     * 发送给定的简单邮件消息
     *
     * @param message 简单邮件消息对象
     * @return ok
     * @apiNote 次方便没有标注注解@Email，则会采用默认方法邮件模板[spring.mail.xxx]发送邮件
     */
    @PostMapping("/simple/send")
    public void sendSimpleEmail(@ModelAttribute(name = "message") @Validated SimpleMailMessageDomain message) {
        log.info("send simple email:{}", message);
        emailTemplate.send(message);
    }

    /**
     * 发送带附件的邮件消息
     *
     * @param message Mime邮件消息对象
     * @param file    选择文件上传，和参数filepath二选一即可
     * @return tips
     * @apiNote 此方法标注注解@Email，则会采用注解值里面name的属性值的参数发送邮件
     */
    @Email(name = "EmailOffice365")
    @PostMapping("/mime/send")
    public void sendMimeEmail(@ModelAttribute(name = "message") @Validated MimeMessageDomain message,
                              @RequestPart(name = "file", required = false) MultipartFile file
    ) {
        log.info("send mime email:{}", message);
        try {
            emailTemplate.send(message, file);
        } catch (MessagingException e) {
            log.error("domain:{},{}", message, e.getLocalizedMessage(), e);
        }
    }
}
```

**方法说明：**

- 发送给定的简单邮件消息

```java
    /**
     * 发送给定的简单邮件消息
     *
     * @param message 简单邮件消息对象
     * @return ok
     * @apiNote 次方便没有标注注解@Email，则会采用默认方法邮件模板[spring.mail.xxx]发送邮件
     */
    @PostMapping("/simple/send")
    public void sendSimpleEmail(@ModelAttribute(name = "message") @Validated SimpleMailMessageDomain message) ;
```

此方法没有标注`@Email`注解指定邮件模板，则会使用默认邮件模板发送。

- 发送带附件的邮件消息

```java
    /**
     * 发送带附件的邮件消息
     *
     * @param message Mime邮件消息对象
     * @param file    选择文件上传，和参数filepath二选一即可
     * @return tips
     * @apiNote 此方法标注注解@Email，则会采用注解值里面name的属性值的参数发送邮件
     */
    @Email(name = "EmailOffice365")
    @PostMapping("/mime/send")
    public void sendMimeEmail(@ModelAttribute(name = "message") @Validated MimeMessageDomain message,
                              @RequestPart(name = "file", required = false) MultipartFile file
    );
```

此方法有标注`@Email(name = "EmailOffice365")`
注解指定邮件模板，指定以配置文件`lejing-common/multiple-email-spring-boot-starter/src/main/resources/application-email.yml`
里面的`EmailOffice365`邮件模板发送，在处理逻辑是会调用对应的对应的`JavaMailSender`实例执行发短信的逻辑。

### 2.7 效果演示

![image-20210910233435972](https://alphahub-test-bucket.oss-cn-shanghai.aliyuncs.com/image/image-20210910233435972.png)

你可申请好对应的邮件配置信息后，启动**LejingSiteReserveApplication**访问里面的index文件查看效果。

**LejingSiteReserveApplication**的最小基础软件配置：

- nocos
- mysql

参数示例：

![image-20210910233858832](https://alphahub-test-bucket.oss-cn-shanghai.aliyuncs.com/image/image-20210910233858832.png)

## 3 总结&提示

配置完后，意味着**`lejing-site-reserve`**这个服务已经整合了多模板邮件发送功能。

关于**`spring.profiles.active=dev`**和**`spring.profiles.include=email`**的加载顺序：

- 后者在**spring**启动的时候会优先加载**`spring.profiles.include=email`**里面的邮件配置元数据
- 然后再加载**`spring.profiles.active=dev`**的元数据
- **`spring.profiles.include`**引入元数据会覆盖当前服务的同名属性

## 4 关于注解`@Email`作用在类和方法的优先级问题

- 当注解`@Email`同时作用类，和方法上时，类上`@Email`注解的优先级高于方法上注解`@Email`的优先级
- 当注解`@Email`作用方法上时，该方法邮件客户端的为注解`@Email`指定的邮件客户端
- 当注解`@Email`作用类上时，该类所有邮件模板方法发送邮件的客户端都以注解`@Email`指定为准客户端

## 5 关于`Spring IOC`容器中的同一个`Bean`实例里面被`@Email`注解标注的方法间嵌套调用的问题

请参考模块`lejing-common/lejing-common-sms-support`[🔗](https://github.com/Weasley-J/lejing-mall/tree/main/lejing-common/lejing-common-sms-support#5-%E5%85%B3%E4%BA%8Espring-ioc%E5%AE%B9%E5%99%A8%E4%B8%AD%E7%9A%84%E5%90%8C%E4%B8%80%E4%B8%AAbean%E5%AE%9E%E4%BE%8B%E9%87%8C%E9%9D%A2%E8%A2%ABsms%E6%B3%A8%E8%A7%A3%E6%A0%87%E6%B3%A8%E7%9A%84%E6%96%B9%E6%B3%95%E9%97%B4%E5%B5%8C%E5%A5%97%E8%B0%83%E7%94%A8%E7%9A%84%E9%97%AE%E9%A2%98)
的`README.md`文档第**5**小节，性质一模一样，注解不一样而已.

