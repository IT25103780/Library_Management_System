package lk.lumina;
import com.zaxxer.hikari.*;
import java.sql.*;
import java.util.*;
public final class DB {
 private static HikariDataSource pool;
 public static void init() throws Exception {
  java.nio.file.Files.createDirectories(Config.data());
  HikariConfig h=new HikariConfig();
  String mode=Config.get("db.mode","demo");
  if(!Set.of("demo","sqlserver").contains(mode))throw new IllegalArgumentException("db.mode must be sqlserver or demo.");
  System.out.println("Lumina database mode: "+mode+"; configuration: "+Config.source());
  h.setDriverClassName(mode.equals("sqlserver")?"com.microsoft.sqlserver.jdbc.SQLServerDriver":"org.h2.Driver");
  h.setJdbcUrl(mode.equals("sqlserver")?Config.get("db.url","jdbc:sqlserver://localhost:1433;databaseName=Lumina_users;encrypt=true;trustServerCertificate=true"):"jdbc:h2:file:"+Config.data().resolve("lumina").toString().replace('\\','/')+";MODE=MSSQLServer;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
  h.setUsername(mode.equals("sqlserver")?Config.get("db.user","sa"):"sa");h.setPassword(mode.equals("sqlserver")?Config.get("db.password",""):"");
  h.setMaximumPoolSize(8);h.setConnectionTimeout(10000);h.setPoolName("LuminaPool");pool=new HikariDataSource(h);
 }
 public static Connection connection()throws SQLException{return pool.getConnection();}
 public interface Job<T>{T run(Connection c)throws Exception;}
 public static <T>T tx(Job<T> job)throws Exception{try(Connection c=connection()){c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);try{T result=job.run(c);c.commit();return result;}catch(Exception e){c.rollback();throw e;}}}
 private static PreparedStatement prepare(Connection c,String sql,Object...args)throws SQLException{PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]);return s;}
 public static List<Map<String,Object>> list(Connection c,String sql,Object...args)throws SQLException{try(PreparedStatement s=prepare(c,sql,args);ResultSet r=s.executeQuery()){List<Map<String,Object>> out=new ArrayList<>();while(r.next()){Map<String,Object> row=new LinkedHashMap<>();for(int i=1;i<=r.getMetaData().getColumnCount();i++)row.put(r.getMetaData().getColumnLabel(i).toLowerCase(Locale.ROOT),r.getObject(i));out.add(row);}return out;}}
 public static List<Map<String,Object>> list(String sql,Object...args)throws SQLException{try(Connection c=connection()){return list(c,sql,args);}}
 public static Map<String,Object> one(Connection c,String sql,Object...args)throws SQLException{var a=list(c,sql,args);return a.isEmpty()?null:a.get(0);}
 public static Map<String,Object> one(String sql,Object...args)throws SQLException{try(Connection c=connection()){return one(c,sql,args);}}
 public static long update(Connection c,String sql,Object...args)throws SQLException{try(PreparedStatement s=prepare(c,sql,args)){int n=s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){if(r.next()&&r.getObject(1) instanceof Number value)return value.longValue();return n;}}}
 public static long update(String sql,Object...args)throws SQLException{try(Connection c=connection()){return update(c,sql,args);}}
 public static void close(){if(pool!=null)pool.close();}
 public static long id(Map<String,Object> r,String k){return ((Number)r.get(k)).longValue();}
}
