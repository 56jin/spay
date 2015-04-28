package controllers.YEE.SecureSign;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;


public class RequestHandler extends AbstractHandler {

	public void handle(String arg0, Request arg1, HttpServletRequest arg2,
			HttpServletResponse arg3) throws IOException, ServletException {
		
		try {
			doHandle(arg1, arg2, arg3);
		} catch (Exception e) {
			e.printStackTrace();
			arg3.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	        arg1.setHandled(true);
		}
		
	}

	private void doHandle(Request arg1, HttpServletRequest arg2,
			HttpServletResponse arg3) throws IOException,
			UnsupportedEncodingException {
		
		arg2.setCharacterEncoding("utf-8");
		
		String uri = arg2.getRequestURI();
		System.out.println("request:" + uri);
		
		if ("/sign".equals(uri)) {
			
			String source = arg2.getParameter("req");
			System.out.println("source:" + source);
			
			String sign = SignUtil.sign(source, RestMain.pfxFile, RestMain.password);
			System.out.println("sign:" + sign);
			
			arg3.setStatus(HttpServletResponse.SC_OK);
	        IOUtils.write(sign, arg3.getOutputStream(), "utf-8");
	        arg1.setHandled(true);
		} else if ("/verify".equals(uri)) {
			String req = arg2.getParameter("req");
			String sign = arg2.getParameter("sign");
			System.out.println("req=" + req + ", sign=" + sign);
			
			String ret = "FAIL";
			try {
				ret = SignUtil.verifySign(req, sign, "yeepay.com") ? "SUCCESS" : "FAIL";
			} catch (Throwable e) {
				e.printStackTrace();
				ret = "FAIL";
			}
			arg3.setStatus(HttpServletResponse.SC_OK);
	        IOUtils.write(ret, arg3.getOutputStream(), "utf-8");
	        arg1.setHandled(true);
		}
	}

}
