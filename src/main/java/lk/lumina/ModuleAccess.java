package lk.lumina;
import java.util.*;
import jakarta.servlet.http.*;
/** Build-time boundary for one independently runnable member project. */
public final class ModuleAccess {
 public static final String ID="borrowing", TITLE="Borrowing & Returns", MEMBER="Liyanage L.R.D.S.B.", STUDENT="IT25103779", USE_CASE="UC-04", DESCRIPTION="Make every loan easy to follow. Issue books, record returns and keep borrowing history accurate.";
 public static final List<String> ENTITIES=List.of();
 public static final String[][] LINKS={{"Circulation desk","/circulation"},{"My borrowing history","/my-books"}};
 public static final String[][] STATS={{"Borrowing records","loans"},{"Physical copies","copies"},{"Members","users WHERE role='READER'"}};
 public static final String[] CRUD={"Issue an available copy to an eligible member.","View active loans, overdue items and borrowing history.","Record returns and change eligible due dates.","Void a mistaken loan while preserving the audit trail."};
 private static final Set<String> GET=Set.of("/","/api/notifications","/book","/catalog","/circulation","/cover","/dashboard","/home","/login","/my-books","/notifications","/profile");
 private static final Set<String> POST=Set.of("/login","/logout","/profile");
 private static final Set<String> ACTIONS=Set.of("borrow","due-date","issue","read-notifications","return","void-loan");
 public static boolean is(String id){return ID.equals(id);}
 public static boolean showColumn(String key){return !Set.of("user_id","loan_id","book_id","supplier_id","copy_id","page_number","read_count","adjusted","fee","duration_days").contains(key);}
 public static boolean canManage(Map<String,Object> u){if(u==null)return false;String role=u.get("role").toString();return is("users")||is("suppliers")?role.equals("ADMIN"):Set.of("ADMIN","LIBRARIAN").contains(role);}
 public static boolean linkVisible(String target,Map<String,Object> u){
  if(u==null)return false;
  if(target.startsWith("/manage")||target.equals("/audit")||target.equals("/circulation"))return canManage(u);
  if(target.equals("/reports"))return Web.manager(u);
  return true;
 }
 public static boolean containsLink(String path){return GET.contains(path.split("\\?")[0]);}
 public static String safeLink(String path){return path!=null&&path.startsWith("/")&&!path.startsWith("//")&&containsLink(path)?path:"/home";}
 public static boolean allowed(HttpServletRequest q,Map<String,Object> u){
  String path=q.getServletPath();if(path.isBlank())path="/";
  String entity=Objects.toString(q.getParameter("entity"),"");
  String action=Objects.toString(q.getParameter("action"),"");
  boolean posting=q.getMethod().equals("POST");
  if(path.equals("/action")){
   if(!posting||!ACTIONS.contains(action))return false;
   if(Set.of("toggle","delete-record").contains(action)&&!ENTITIES.contains(entity))return false;
   if(u!=null&&!Set.of("borrow","read-notifications","reserve","cancel-reservation","reservation-edit").contains(action)&&!canManage(u))return false;
  }else if(!(posting?POST:GET).contains(path))return false;
  if(path.equals("/manage")&&(!ENTITIES.contains(entity)||(u!=null&&!canManage(u))))return false;
  if(Set.of("/book-edit","/circulation","/audit","/member").contains(path)&&u!=null&&!canManage(u))return false;
  return true;
 }
}
