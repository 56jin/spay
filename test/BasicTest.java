import org.apache.commons.lang.StringUtils;
import org.junit.*;

import com.sun.org.apache.bcel.internal.classfile.Code;

import java.util.*;

import play.libs.Codec;
import play.test.*;
import models.*;

public class BasicTest extends UnitTest {

    @Test
    public void aVeryImportantThingToTest() {
       String str = " MTAwMDAwMDcwS0s7O3sicFBheVR5cGUiOiIyIiwic2lnblVzZXJJZCI6Ijk3ODVBMjYxMTlEMDMxODE1RDlCQjcwNkEwQ0E1QzBFMzQwRDk3MDE4Q0Y1ODA1QUNFQ0E1OTQyQjhFMjQ3REI5MGIwYWQ4ZSIsInVzZXJJZCI6IjQ1IiwibWFyayI6ImY2ZWYwNjFkLTg5ZDUtNGY0Ny04NGI1LWU2M2EyZTcxZWI4YyIsInN1cGVydmlzb3JJZCI6IjEiLCJzdGF0dXMiOiItMSIsImlzVmlzaWJsZSI6ImZhbHNlIiwic3VnZ2VzdGlvbiI6IuS4jemAmui/hyJ9;Remark1: aHR0cDovL3d3dy5uaXVtYWlsLmNvbS5jbjo4MDg2L2Zyb250L1BheW1lbnRBY3Rpb24vdHJhbnNmZXJNZXJUb1VzZXJDQg==";
       
       System.out.println(str.length());
       System.out.println(new String(Codec.decodeBASE64(str)));
    }

    
    @Test
    public void moreIDTest() {
    	List<t_member_of_platforms> list = t_member_of_platforms.findAll();
    	
    	for(t_member_of_platforms mem : list){
    		if(StringUtils.isNotBlank(mem.platform_member_account) && mem.platform_member_account.contains("KK;;")){
    			String[] array = mem.platform_member_account.split("KK;;");
    			String account = array[0];
    			String accountId = array[1];
    			mem.platform_member_account = account;
    			mem.platform_member_account_id = accountId;
    			mem.save();
    		}
    	}
    }
}
