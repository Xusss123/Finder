package karm.van.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import karm.van.dto.EmailDataDto;
import karm.van.dto.RecoveryMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConsumerService {
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String emailSender;

    @RabbitListener(queues = "${rabbitmq.queue.email.name}")
    public void emailQueueConsume(EmailDataDto emailDataDto) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

        helper.setFrom(emailSender);
        helper.setTo(emailDataDto.email());
        helper.setSubject("You have successfully published your ad");

        String htmlContent = "<h2 style='color:blue;'>" + emailDataDto.cardDto().title() + "</h2>" +
                "<p>" + emailDataDto.cardDto().text() + "</p>" +
                "<p>Thank you for using our service!</p>" +
                "<hr style='border: none; border-top: 1px solid #ccc;'/>" +
                "<footer style='font-size: 0.9em; color: #666; padding-top: 10px;'>" +
                "Contact the author about cooperation:<br/>" +
                "<a href='https://t.me/VE_N_IK' style='text-decoration: none; color: #0e76a8;'>Telegram: @VE_N_IK</a><br/>" +
                "<a href='https://www.instagram.com/qfbnlbcz/' style='text-decoration: none; color: #C13584;'>Instagram: qfbnlbcz</a><br/>" +
                "<a href='https://github.com/Xusss123' style='text-decoration: none; color: #333;'>Github: Xusss123</a>" +
                "</footer>";

        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);

    }

    @RabbitListener(queues = "${rabbitmq.queue.recovery.name}")
    public void emailQueueConsume(RecoveryMessageDto recoveryMessageDto) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

        helper.setFrom(emailSender);
        helper.setTo(recoveryMessageDto.email());
        helper.setSubject("Reset your password");

        String htmlContent =
                "<p> Your recovery-url: <a href='" + recoveryMessageDto.recoveryUrl() + "'>Click here to recover your password</a></p>" +
                "<p>Thank you for using our service!</p>" +
                "<hr style='border: none; border-top: 1px solid #ccc;'/>" +
                "<footer style='font-size: 0.9em; color: #666; padding-top: 10px;'>" +
                "Contact the author about cooperation:<br/>" +
                "<a href='https://t.me/VE_N_IK' style='text-decoration: none; color: #0e76a8;'>Telegram: @VE_N_IK</a><br/>" +
                "<a href='https://www.instagram.com/qfbnlbcz/' style='text-decoration: none; color: #C13584;'>Instagram: qfbnlbcz</a><br/>" +
                "<a href='https://github.com/Xusss123' style='text-decoration: none; color: #333;'>Github: Xusss123</a>" +
                "</footer>";

        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);

    }

}
