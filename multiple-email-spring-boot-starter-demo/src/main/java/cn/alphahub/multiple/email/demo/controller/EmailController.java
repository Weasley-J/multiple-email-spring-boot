package cn.alphahub.multiple.email.demo.controller;


import cn.alphahub.multiple.email.EmailTemplate;
import cn.alphahub.multiple.email.annotation.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public void sendSimpleEmail(@RequestBody @Validated SimpleMailMessageDomain message) {
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

    /**
     * 发送给定的简单邮件消息
     *
     * @apiNote 次方便没有标注注解@Email，则会采用默认方法邮件模板[spring.mail.xxx]发送邮件
     */
    @PostMapping("/simple/send/nested")
    public void sendSimpleEmailNested() {
        SimpleMailMessageDomain domain = new SimpleMailMessageDomain();
        domain.setSubject("232323");
        domain.setTo("abc@qq.com");
        domain.setText("cbd");
        log.info("send simple email:{}", domain);
        emailTemplate.send(domain);
        EmailController controller = (EmailController) AopContext.currentProxy();
        controller.sendMimeEmail(null,null);
    }
}
