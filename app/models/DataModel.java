package models;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import play.Logger;
import play.db.jpa.JPA;
import play.db.jpa.Model;

/**
 * 数据处理模型
 * @author Administrator
 *
 */
public class DataModel extends Model{

    /**
     * 添加数据记录
     * @param tableName 表名称
     * @param params 字段
     * @param cls 类
     * @return
     */
    public int create(String tableName, Map<String,Object> params, Class cls){
    	  int result = 0;
    	  String fields = "";
    	  String values = "";
    	  if(params.size() <=0){
    		return result;  
    	  }
    	  
    	  StringBuffer sql = new StringBuffer("insert into ");
    	  sql.append(tableName);
    	  sql.append(" (");
    	  for (Field field : cls.getDeclaredFields()) {
    		  if(params.get(field.getName()) != null){
    			  fields += field.getName() + ", ";
        		  values += "?, ";
    		  }
    	  }
    	  
    	  fields = fields.substring(0, fields.length() - 2);
    	  values = values.substring(0, values.length() - 2);
    	  sql.append(fields);
    	  sql.append(") values (");
    	  sql.append(values);
    	  sql.append(")");
    	  
		  Query query = JPA.em().createNativeQuery(sql.toString());
		  int n = 1;
		  for (Field field : cls.getDeclaredFields()) {
    		  if(params.get(field.getName()) != null){
    		    query.setParameter(n, params.get(field.getName()));
    		    n++;
    		  }
    	  }
		  
		  result = query.executeUpdate();
		  
		  return result;  
      }
      
	/**
	 * 编辑数据记录
	 * @param tableName 表名称
	 * @param params 字段
	 * @param cls 类
	 * @return
	 */
	public static int edit(String tableName, Map<String,Object> params, long id, Class cls){
    	  
		  int result = 0;
    	  String fields = "";
    	  if(params.size() <=0){
    		return result;  
    	  }
    	  
    	  StringBuffer sql = new StringBuffer("update ");
    	  sql.append(tableName);
    	  sql.append(" set ");
    	  for (Field field : cls.getDeclaredFields()) {
    		  if(params.get(field.getName()) != null){
    			  fields += field.getName() + " = ?, ";
    		  }
    	  }
    	  
    	  fields = fields.substring(0, fields.length() - 2);
    	  sql.append(fields);
    	  sql.append(" where id = ?");
    	  
		  Query query = JPA.em().createNativeQuery(sql.toString());
		  int n = 1;
		  for (Field field : cls.getDeclaredFields()) {
    		  if(params.get(field.getName()) != null){
    		    query.setParameter(n, params.get(field.getName()));
    		    n++;
    		  }
    	  }
		  
		  //设置id的值
		  query.setParameter(n, id);
		  
		  result = query.executeUpdate();
		  
		  return result;
      }
	
	/**
	 * 删除数据记录
	 * @param tableName 表名称
	 * @param id 
	 * @return
	 */
	public static int del(String tableName, long id){
		int result = 0;
		String sql = "delete from " + tableName + " where id = :id";
		Query query = JPA.em().createNativeQuery(sql.toString());
		query.setParameter("id", id);
		result = query.executeUpdate();
		
		return result;
	}
}
