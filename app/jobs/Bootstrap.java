package jobs;
import java.util.Map;

import constants.Constants;
import constants.GUOConstants;
import constants.IPSConstants;
import constants.LoanConstants;
import business.GateWay;
import play.*;
import play.cache.Cache;
import play.jobs.*;
import play.test.*;
import models.*;

@OnApplicationStart
public class Bootstrap extends Job {
 
    public void doJob() {
    	Map<Long, GateWay> gateWays = GateWay.queryGateWay();
//    	Cache.set("gateWays", gateWays);
    	GateWay gateWay = null;
    	if(gateWays != null) {
    		
    		long ips = Constants.IPS;
    		/*环迅*/
    		gateWay = gateWays.get(ips) != null ? gateWays.get(ips) : null;
    		
    		if(gateWay != null) {
    			IPSConstants.CERT_MD5 = gateWay.keyInfo.get("CERT_MD5");
    			IPSConstants.PUB_KEY = gateWay.keyInfo.get("PUB_KEY");
    			IPSConstants.DES_KEY = gateWay.keyInfo.get("DES_KEY");
    			IPSConstants.DES_IV = gateWay.keyInfo.get("DES_IV");
    		}
    		
    		/*双乾*/
    		long loan = Constants.LOAN;
    		gateWay = gateWays.get(loan) != null ? gateWays.get(loan) : null;
    		
    		if(gateWay != null) {
    			LoanConstants.argMerCode = gateWay.keyInfo.get("argMerCode");
    			LoanConstants.signRate = gateWay.keyInfo.get("signRate");
    			LoanConstants.publicKey = gateWay.keyInfo.get("publicKey");
    			LoanConstants.privateKeyPKCS8 = gateWay.keyInfo.get("privateKeyPKCS8");
    			Logger.info("===================双乾支付常量赋值（job）=================");
    			Logger.info("argMerCode  = %s", LoanConstants.argMerCode==null?"null":"ok");
    			Logger.info("signRate  = %s", LoanConstants.signRate==null?"null":"ok");
    			Logger.info("publicKey  = %s", LoanConstants.publicKey==null?"null":"ok");
    			Logger.info("privateKeyPKCS8 = %s", LoanConstants.privateKeyPKCS8==null?"null":"ok");
    		}
    		
    		long guo = Constants.GUO;
    		gateWay = gateWays.get(guo) != null ? gateWays.get(guo) : null;
    		
    		if(gateWay != null) {
    			GUOConstants.P2P_NAME = gateWay.account;
    			GUOConstants.MER_ID = gateWay.pid;
    			GUOConstants.MER_NAME = gateWay.key;
    		}
    	}
    }
 
}