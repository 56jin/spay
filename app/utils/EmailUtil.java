package utils;

import org.apache.commons.lang.StringUtils;

import constants.Constants;
import play.Logger;
import play.libs.Mail;

public class EmailUtil {

	public static void sendEmail(String email, String title, String content) {
		if(StringUtils.isBlank(content)) {
			
			return;
		}
		
		try{
			org.apache.commons.mail.HtmlEmail sendEmail = new org.apache.commons.mail.HtmlEmail();
			sendEmail.setFrom(Constants.EMAIL);
			sendEmail.addTo(email);
			sendEmail.setSubject(title);
			sendEmail.setCharset("utf-8");
			sendEmail.setMsg(content);
			Mail.send(sendEmail); 
		}catch (Exception e) {
			e.printStackTrace();
			Logger.info("发送邮件时："+e.getMessage());
		}
	}
}
