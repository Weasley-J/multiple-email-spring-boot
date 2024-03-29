package cn.alphahub.multiple.email;

import cn.alphahub.multiple.email.aspect.EmailAspect;
import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

;

/**
 * 邮件模板方法默认实现
 *
 * @author lwj
 * @version 1.0
 * @date 2021-09-07 10:44
 */
@Slf4j
@Component
@Validated
@AutoConfigureBefore({MailProperties.class, JavaMailSender.class})
public class EmailTemplate {
    /**
     * default mail properties
     */
    @Autowired(required = false)
    private MailProperties defaultMailProperties;
    /**
     * default java mail sender
     */
    @Autowired(required = false)
    private JavaMailSender defaultJavaMailSender;
    /**
     * thread pool executor
     */
    @Autowired
    private ThreadPoolExecutor emailThreadPoolExecutor;

    /**
     * 获取邮件是发送实例
     *
     * @return JavaMailSender
     */
    private JavaMailSender getMailSender() {
        JavaMailSender mailSender = EmailAspect.MAIL_SENDER_THREAD_LOCAL.get();
        if (Objects.isNull(mailSender)) {
            return this.defaultJavaMailSender;
        }
        return mailSender;
    }

    /**
     * 获取电子邮件支持的配置属性
     *
     * @return MailProperties
     */
    private MailProperties getMailProperties() {
        MailProperties properties = EmailAspect.MAIL_PROPERTIES_THREAD_LOCAL.get();
        if (Objects.isNull(properties)) {
            return this.defaultMailProperties;
        }
        return properties;
    }

    /**
     * 发送给定的简单邮件消息
     *
     * @param domain the message to send
     * @throws MailException Base class for all mail exceptions
     */
    public void send(@Valid SimpleMailMessageDomain domain) throws MailException {
        SimpleMailMessage simpleMessage = new SimpleMailMessage();
        simpleMessage.setFrom(this.getMailProperties().getUsername());
        simpleMessage.setTo(domain.getTo());
        if (ObjectUtils.isNotEmpty(domain.getCc())) {
            simpleMessage.setCc(domain.getCc());
        }
        simpleMessage.setSentDate(Objects.nonNull(domain.getSentDate()) ? Date.from(domain.getSentDate().atZone(ZoneId.systemDefault()).toInstant()) : new Date());
        simpleMessage.setSubject(domain.getSubject());
        simpleMessage.setText(domain.getText());
        JavaMailSender mailSender = this.getMailSender();

        RequestAttributes mainThreadRequestAttributes = RequestContextHolder.getRequestAttributes();
        CompletableFuture<Void> sendResponseFuture = CompletableFuture.runAsync(() -> {
            log.info("Current send simple message thread info: '{}' '{}' '{}'", Thread.currentThread().getId(), Thread.currentThread().getThreadGroup().getName(), Thread.currentThread().getName());
            RequestContextHolder.setRequestAttributes(mainThreadRequestAttributes);
            mailSender.send(simpleMessage);
        }, emailThreadPoolExecutor);

        try {
            CompletableFuture.allOf(sendResponseFuture).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("发送给定的简单邮件消息失败 {},{}", JSONUtil.toJsonStr(domain), e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 发送带附件的邮件
     *
     * @param domain metadata of message to send
     * @param file   Nullable, support for spring MVC upload file received in the request, can be null.
     * @throws MessagingException messaging exception
     */
    public void send(@Valid MimeMessageDomain domain, @Nullable MultipartFile file) throws MessagingException {
        JavaMailSender mailSender = this.getMailSender();
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setFrom(this.getMailProperties().getUsername());
        helper.setTo(domain.getTo());
        helper.setCc(domain.getCc());
        helper.setSentDate(Objects.nonNull(domain.getSentDate()) ? Date.from(domain.getSentDate().atZone(ZoneId.systemDefault()).toInstant()) : new Date());
        helper.setSubject(domain.getSubject());
        helper.setText(domain.getText(), true);
        if (Objects.nonNull(file) && !file.isEmpty()) {
            helper.addAttachment(Objects.requireNonNull(file.getOriginalFilename(), "The attachment file name (including file suffix) cannot be empty"), file);
        }
        if (StringUtils.isNoneBlank(domain.getFilepath())) {
            File newFile = new File(domain.getFilepath());
            helper.addAttachment(FileUtil.getName(domain.getFilepath()), newFile);
        }
        RequestAttributes mainThreadRequestAttributes = RequestContextHolder.getRequestAttributes();
        CompletableFuture<Void> sendResponseFuture = CompletableFuture.runAsync(() -> {
            log.info("Current send mime mime message thread info: '{}' '{}' '{}'", Thread.currentThread().getId(), Thread.currentThread().getThreadGroup().getName(), Thread.currentThread().getName());
            RequestContextHolder.setRequestAttributes(mainThreadRequestAttributes);
            mailSender.send(mimeMessage);
        }, emailThreadPoolExecutor);

        try {
            CompletableFuture.allOf(sendResponseFuture).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("发送带附件的邮件 {},{}", JSONUtil.toJsonStr(domain), e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 简单邮件消息对象
     * <p>支持文本消息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleMailMessageDomain {
        /**
         * 收件人的邮箱
         */
        @Email(regexp = "^([A-Za-z0-9_\\-\\.])+\\@([A-Za-z0-9_\\-\\.])+\\.([A-Za-z]{2,4})$", message = "收件人邮箱格式不正确")
        @NotBlank(message = "收件人邮箱不能为空")
        private String to;
        /**
         * 抄送邮箱（非必填）
         */
        private String[] cc;
        /**
         * 邮件发送日期, 默认当前时刻: {@code new Date()} 提交格式: yyyy-MM-dd HH:mm:ss
         */
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime sentDate;
        /**
         * 邮件主题、邮件标题
         */
        @NotBlank(message = "邮件主题不能为空")
        private String subject;
        /**
         * 邮件正文、邮件内容
         */
        @NotBlank(message = "邮件正文不能为空")
        private String text;
    }

    /**
     * 带附件邮件消息对象
     * <p>支持附件：图片、文件等资源
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MimeMessageDomain {
        /**
         * 收件人的邮箱
         */
        @NotBlank(message = "收件人邮箱不能为空")
        @Email(regexp = "^([A-Za-z0-9_\\-\\.])+\\@([A-Za-z0-9_\\-\\.])+\\.([A-Za-z]{2,4})$", message = "收件人邮箱格式不正确")
        private String to;
        /**
         * 抄送邮箱（非必填）
         */
        private String[] cc;
        /**
         * 邮件发送日期, 默认当前时刻: {@code new Date()} 提交格式: yyyy-MM-dd HH:mm:ss
         */
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime sentDate;
        /**
         * 邮件主题、邮件标题
         */
        @NotBlank(message = "邮件主题不能为空")
        private String subject;
        /**
         * 邮件正文、邮件内容 （可以是html字符串）
         */
        @NotBlank(message = "邮件正文不能为空")
        private String text;
        /**
         * 附件文件的路径 （没有附件文件不用还传）
         */
        private String filepath;
    }
}
