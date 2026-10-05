package lk.lumina;
/** Connection diagnostic, without schema writes. */
public final class DatabaseCheck {
 public static void main(String[] args)throws Exception {
  try{DB.init();try(var c=DB.connection()){
   System.out.println("Configuration: "+Config.source());
   System.out.println("Connected: "+c.getMetaData().getDatabaseProductName()+" / "+c.getCatalog());
   System.out.println("Connection successful. No records changed.");
  }}finally{DB.close();}
 }
}
