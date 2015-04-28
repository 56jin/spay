package controllers.YEE.SecureSign;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.jetty.server.Server;

public class RestMain {
	
	public static String pfxFile;
	
	public static String password;
	
	public static int port;
	
	public static void run() throws Exception {
		
		Server server = new Server(port);
		server.setHandler(new RequestHandler());
		server.start();
		server.join();
	}

	public static void main(String[] args) {
		
		try {
			
			loadConfig();
			
			run();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void loadConfig() throws IOException {
		InputStream input = ClassLoader.getSystemClassLoader().getResourceAsStream("config.properties");
		if (input == null) {
			throw new IOException("not found config.properties");
		}
		Properties prop = new Properties();
		prop.load(input);
		
		pfxFile = prop.getProperty("pfx");
		password = prop.getProperty("password");
		port = Integer.parseInt(prop.getProperty("port", "8080"));
		if (pfxFile == null || password == null) {
			throw new IOException("not set pfx or password");
		}
		
		System.out.println("启动中:pfx=" + pfxFile + ",password=" + password + ",port=" + port);
		System.out.println("当前工作路径:" + new File(".").getAbsolutePath());
		
		// 试签名
		System.out.println("测试签名...");
		SignUtil.sign("hello world", RestMain.pfxFile, RestMain.password);
		
		System.out.println("启动成功");
	}

}
