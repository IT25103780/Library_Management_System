package lk.lumina;
import java.nio.file.*;
import java.util.*;
public final class Config {
 private static final Properties P=new Properties();
 private static Path source;
 static {
  String explicit=System.getProperty("lumina.config",System.getenv().getOrDefault("LUMINA_CONFIG",""));
  if(!explicit.isBlank())source=Path.of(explicit).toAbsolutePath();
  else {
   List<Path> roots=new ArrayList<>();roots.add(Path.of(System.getProperty("user.dir",".")));
   roots.add(Path.of(System.getProperty("catalina.base",".")));
   try{roots.add(Path.of(Config.class.getProtectionDomain().getCodeSource().getLocation().toURI()));}catch(Exception ignored){}
   for(Path root:roots){for(Path dir=root;dir!=null;dir=dir.getParent()){
    Path candidate=dir.resolve("config/demo.properties");
    if(Files.isRegularFile(candidate)){source=candidate;break;}
   }if(source!=null)break;}
  }
  if(source!=null)try(var in=Files.newBufferedReader(source,java.nio.charset.StandardCharsets.UTF_8)){P.load(in);}catch(Exception e){throw new IllegalStateException("Cannot read database configuration: "+source,e);}
 }
 public static String get(String k,String d){return System.getProperty(k,System.getenv().getOrDefault(k.toUpperCase(Locale.ROOT).replace('.','_'),P.getProperty(k,d)));}
 public static String source(){return source==null?"Environment / Java system properties":source.toString();}
 public static boolean demo(){return get("payment.mode","demo").equals("demo");}
 public static Path data(){return Path.of(get("storage.path",System.getProperty("catalina.base",".")+"/lumina-data/"+ModuleAccess.ID)).toAbsolutePath();}
}
