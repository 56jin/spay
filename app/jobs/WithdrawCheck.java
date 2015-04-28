package jobs;

import constants.Constants;
import business.DealDetail;
import play.jobs.Job;
import play.jobs.On;
import utils.ErrorInfo;

/**
 * 易宝接口定时查询银行订单回退记录并回调P2P
 * @author zhs
 * @date 2015-1-6 下午02:24:06
 */
@On("0 50 23 * * ?")
public class WithdrawCheck extends Job{

	public void doJob(){
		if(Constants.IS_WITHDRAW_JOB){
			DealDetail detail = new DealDetail();
			ErrorInfo error = new ErrorInfo();
			detail.checkWithdraw(error);
		}
	}
}
