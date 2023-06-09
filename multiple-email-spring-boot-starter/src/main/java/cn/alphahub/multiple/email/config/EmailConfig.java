package cn.alphahub.multiple.email.config;

import cn.alphahub.multiple.email.annotation.Email;
import cn.hutool.core.collection.CollUtil;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.alphahub.multiple.email.config.EmailConfig.EmailProperties;
import static cn.alphahub.multiple.email.config.EmailConfig.EmailTemplateProperties;
import static cn.alphahub.multiple.email.config.EmailConfig.EmailThreadPoolProperties;

/**
 * 邮件配置类
 *
 * @author lwj
 * @version 1.0
 * @date 2021-09-06
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConfigurationPropertiesScan({"cn.alphahub.multiple.email.config"})
@EnableConfigurationProperties({MailProperties.class, EmailProperties.class, EmailTemplateProperties.class, EmailThreadPoolProperties.class})
public class EmailConfig {

    /**
     * 填充邮件模板配置列表元数据Map
     *
     * @param mailProperties          spring原生电子邮件支持的配置属性
     * @param emailTemplateProperties 多邮件模板配置列表元数据属性
     * @return 填充邮件模板配置列表元数据Map
     */
    @Bean(name = {"emailPropertiesMap"})
    public Map<String, MailProperties> emailPropertiesMap(@Valid MailProperties mailProperties, @Valid EmailTemplateProperties emailTemplateProperties) {
        Map<String, MailProperties> mailPropertiesMap = new ConcurrentHashMap<>(100);
        mailPropertiesMap.put(Email.DEFAULT_TEMPLATE, mailProperties);
        List<EmailProperties> templates = emailTemplateProperties.getEmailTemplates();
        if (CollUtil.isEmpty(templates)) {
            return mailPropertiesMap;
        }
        Map<String, MailProperties> propertiesMap = templates.stream().collect(Collectors.toMap(EmailProperties::getTemplateName, EmailProperties::getMailProperties));
        mailPropertiesMap.putAll(propertiesMap);
        return mailPropertiesMap;
    }

    /**
     * 邮件发送对象Map
     * <p>spring初始化时把所有邮件模板的发送对象实例创建好注入IOC，对象只创建一次</p>
     *
     * @param emailPropertiesMap 填充邮件模板配置列表元数据Map
     * @return javaMailSenderMap邮件发送对象实例
     */
    @Bean(name = {"javaMailSenderMap"})
    public Map<String, JavaMailSender> javaMailSenderMap(@Qualifier("emailPropertiesMap") Map<String, MailProperties> emailPropertiesMap) {
        Map<String, JavaMailSender> javaMailSenderMap = new ConcurrentHashMap<>(100);
        emailPropertiesMap.forEach((templateName, properties) -> {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(properties.getHost());
            if (properties.getPort() != null) {
                sender.setPort(properties.getPort());
            }
            sender.setUsername(properties.getUsername());
            sender.setPassword(properties.getPassword());
            sender.setProtocol(properties.getProtocol());
            if (properties.getDefaultEncoding() != null) {
                sender.setDefaultEncoding(properties.getDefaultEncoding().name());
            }
            if (!properties.getProperties().isEmpty()) {
                Properties asProperties = new Properties();
                asProperties.putAll(properties.getProperties());
                sender.setJavaMailProperties(asProperties);
            }
            javaMailSenderMap.put(templateName, sender);
            if (log.isInfoEnabled()) {
                log.info("Loading instance of java mail sender, Template name '{}', SMTP server '{}'.", templateName, properties.getHost());
            }
        });
        return javaMailSenderMap;
    }


    /**
     * client wrapper
     *
     * @param emailPropertiesMap emailPropertiesMap
     * @param javaMailSenderMap  MailWrapper
     * @return MailWrapper
     */
    @Bean
    public MailWrapper clientWrapper(@Qualifier("emailPropertiesMap") Map<String, MailProperties> emailPropertiesMap,
                                     @Qualifier("javaMailSenderMap") Map<String, JavaMailSender> javaMailSenderMap) {
        return new MailWrapper(emailPropertiesMap, javaMailSenderMap);
    }

    /**
     * 线程池
     *
     * @param emailThreadPoolProperties thread pool properties
     * @return thread pool executor
     */
    @Bean(name = {"emailThreadPoolExecutor"})
    @ConditionalOnMissingBean(value = {ThreadPoolExecutor.class})
    public ThreadPoolExecutor emailThreadPoolExecutor(EmailThreadPoolProperties emailThreadPoolProperties) {
        return new ThreadPoolExecutor(
                emailThreadPoolProperties.getCorePoolSize(),
                emailThreadPoolProperties.getMaximumPoolSize(),
                emailThreadPoolProperties.getKeepAliveTime(),
                emailThreadPoolProperties.getTimeUnit(),
                new LinkedBlockingQueue<>(emailThreadPoolProperties.getCapacity()),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * 多邮件模板配置列表元数据属性
     */
    @Data
    @ConfigurationProperties(prefix = "spring.mail")
    public static class EmailTemplateProperties {
        /**
         * 多邮件模板配置列表
         */
        private List<EmailProperties> emailTemplates;
    }

    /**
     * 单个邮件模板配置元数据
     */
    @Data
    @ConfigurationProperties(prefix = "spring.mail.email-templates")
    public static class EmailProperties {
        /**
         * 邮件模板名称
         */
        private String templateName;
        /**
         * 邮件模板配置信息
         */
        @NestedConfigurationProperty
        private MailProperties mailProperties;
    }

    /**
     * 线程池配置参数
     */
    @Data
    @ConfigurationProperties(prefix = "spring.mail.thread")
    public static class EmailThreadPoolProperties {
        /**
         * 核心线程池数量
         */
        private Integer corePoolSize = 5;
        /**
         * 最大线程数
         */
        private Integer maximumPoolSize = 50;
        /**
         * 存活时间
         */
        private Long keepAliveTime = 10L;
        /**
         * 存活时间单位，默认：{@code TimeUnit.SECONDS}
         *
         * @see TimeUnit
         */
        private TimeUnit timeUnit = TimeUnit.SECONDS;
        /**
         * 最大任务数量
         */
        private Integer capacity = 200;
    }
}

