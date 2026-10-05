package lk.lumina;
import java.sql.*;
import java.util.Locale;

/** Persistent catalog numbering, serialized by a database row lock. */
public final class BookReferences {
 private static final String KEY="last_book_reference";
 private static long highest(Connection c)throws Exception {
  long high=0;
  for(var row:DB.list(c,"SELECT isbn FROM books WHERE isbn LIKE 'LUM-%'")) {
   String value=row.get("isbn").toString();
   if(value.matches("LUM-[0-9]+")) high=Math.max(high,Long.parseLong(value.substring(4)));
  }
  return high;
 }
 public static void initialize()throws Exception {
  DB.tx(c->{
   if(DB.one(c,"SELECT setting_value FROM settings WHERE setting_key=?",KEY)==null)
    DB.update(c,"INSERT INTO settings(setting_key,setting_value) VALUES(?,?)",KEY,"0");
   DB.update(c,"UPDATE settings SET setting_value=setting_value WHERE setting_key=?",KEY);
   long high=Math.max(highest(c),Long.parseLong(DB.one(c,"SELECT setting_value FROM settings WHERE setting_key=?",KEY).get("setting_value").toString()));
   DB.update(c,"UPDATE settings SET setting_value=? WHERE setting_key=?",Long.toString(high),KEY);
   return null;
  });
 }
 private static String format(long value){return String.format(Locale.ROOT,"LUM-%04d",value);}
 public static String preview()throws Exception {
  try(Connection c=DB.connection()){
   long last=Long.parseLong(DB.one(c,"SELECT setting_value FROM settings WHERE setting_key=?",KEY).get("setting_value").toString());
   return format(Math.addExact(Math.max(last,highest(c)),1));
  }
 }
 public static String next(Connection c)throws Exception {
  DB.update(c,"UPDATE settings SET setting_value=setting_value WHERE setting_key=?",KEY);
  long last=Long.parseLong(DB.one(c,"SELECT setting_value FROM settings WHERE setting_key=?",KEY).get("setting_value").toString());
  long next=Math.addExact(Math.max(last,highest(c)),1);
  DB.update(c,"UPDATE settings SET setting_value=? WHERE setting_key=?",Long.toString(next),KEY);
  return format(next);
 }
 public static <T>T transaction(DB.Job<T> job)throws Exception {
  for(int attempt=0;;attempt++)try{return DB.tx(job);}catch(SQLException e){
   if(attempt>=4||!("40001".equals(e.getSQLState())||e.getErrorCode()==1205))throw e;
   Thread.sleep(25L*(attempt+1));
  }
 }
}
