package constants;

public class ViewTemplete {

	public static final String V_MEMBER_DETAILS = "SELECT SQL_CALC_FOUND_ROWS "+
	"t_member_details.id AS id,"+
	"t_member_details.platform_id AS platform_id,"+
	"t_platforms.name AS platform_name,"+
	"t_member_details.member_id AS member_id,"+
	"t_member_of_platforms.platform_member_name "+
		"AS member_name,"+
	"t_member_details.serial_number AS serial_number,"+
	"t_member_details.time AS time,"+
	"t_member_details.operation AS operation,"+
	"t_member_details.amount AS amount,"+
	"t_member_details.status AS status,"+
	"t_member_detail_types.name AS name,"+
	"t_member_details.summary AS summary FROM(("+
	"t_member_details JOIN t_member_detail_types ON("+
	"( t_member_details.operation = t_member_detail_types.id ))"+
	") left JOIN t_platforms on t_platforms.id = t_member_details.platform_id )"+
    "left JOIN t_member_of_platforms on ("+
	"t_member_of_platforms.platform_id = t_member_details.platform_id)"+
	"AND(t_member_of_platforms.platform_member_id = t_member_details.member_id)";
	
	public static final String V_MEMBER_EVENTS = "SELECT SQL_CALC_FOUND_ROWS "+
	"t_member_events.id AS id,"+
	"t_member_events.platform_id AS platform_id,"+
	"t_platforms.name AS platform_name,"+
	"t_member_events.member_id AS member_id,"+
	"t_member_of_platforms.platform_member_name AS member_name,"+
	"t_member_events.time AS time,"+
	"t_member_events.type_id AS type_id,"+
	"t_member_event_types.name AS name,"+
	"t_member_events.descrption AS descrption FROM"+
	"((t_member_events JOIN t_member_event_types ON("+
	"(t_member_events.type_id = t_member_event_types.id))"+
	") LEFT JOIN t_platforms on t_platforms.id = t_member_events.platform_id)"+
	"LEFT JOIN t_member_of_platforms ON(("+
	"t_member_of_platforms.platform_id = t_member_events.platform_id )"+
	"AND(t_member_of_platforms.platform_member_id = t_member_events.member_id))";
}
