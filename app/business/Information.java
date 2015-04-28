package business;

import java.util.Date;

import models.t_untreated_imformation;

public class Information {

	public long id;
	public Date time;
	public String information;
	public int gatewayId;
	public int count;
	public boolean status;
	
	public void add() {
		t_untreated_imformation info = new t_untreated_imformation();
		
		info.time = new Date();
		info.information = this.information;
		info.count = 0;
		info.status = false;
		
		info.save();
	}
}
