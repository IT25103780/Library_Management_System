package lk.lumina;
import java.sql.*;
import java.time.*;
import java.util.*;
/** Fictional supporting transactions for a NEW local demo database only. */
public final class DemoFixtures {
 public static void seed()throws Exception{
  if(!Config.get("db.mode","sqlserver").equals("demo"))return;
  if(DB.one("SELECT setting_key FROM settings WHERE setting_key='member_demo_seed'")!=null)return;
  DB.tx(c->{
   Timestamp now=Library.now();
   long reader=DB.id(DB.one(c,"SELECT id FROM users WHERE username='reader'"),"id");
   long other=DB.update(c,"INSERT INTO users(name,email,username,password_hash,role,branch_id,created_at,phone,address,member_type) VALUES(?,?,?,?,?,?,?,?,?,?)","Sample Member","sample.member@lumina.test","sample_member",Security.hash("Lumina@2026!"),"READER",1,now,"0770000001","Kandy","STUDENT");
   // Two unavailable titles support reservation creation and changes at either branch.
   if(ModuleAccess.is("reservations")){
    for(var cp:DB.list(c,"SELECT * FROM copies WHERE book_id IN (1,3) ORDER BY id")){
     long owner=DB.id(cp,"book_id")==1?other:reader;
     DB.update(c,"INSERT INTO loans(user_id,book_id,copy_id,kind,status,start_at,due_at) VALUES(?,?,?,'PHYSICAL','OVERDUE',?,?)",owner,cp.get("book_id"),cp.get("id"),Timestamp.from(Instant.now().minus(Duration.ofDays(17))),Timestamp.from(Instant.now().minus(Duration.ofDays(3))));
     DB.update(c,"UPDATE copies SET status='BORROWED' WHERE id=?",cp.get("id"));
    }
    long returned=DB.update(c,"INSERT INTO loans(user_id,book_id,copy_id,kind,status,start_at,due_at,returned_at) VALUES(?,?,?,'PHYSICAL','RETURNED',?,?,?)",reader,2,4,Timestamp.from(Instant.now().minus(Duration.ofDays(20))),Timestamp.from(Instant.now().minus(Duration.ofDays(6))),Timestamp.from(Instant.now().minus(Duration.ofDays(4))));
    DB.update(c,"INSERT INTO fines(user_id,loan_id,amount,reason,created_at) VALUES(?,?,?,?,?)",reader,returned,40,"Demonstration: returned two days late",now);
    long ready=DB.update(c,"INSERT INTO reservations(user_id,book_id,branch_id,copy_id,status,created_at,ready_until) VALUES(?,4,1,10,'READY',?,?)",reader,now,Timestamp.from(Instant.now().plus(Duration.ofDays(2))));
    DB.update(c,"UPDATE copies SET status='RESERVED' WHERE id=10");
    Library.notify(c,reader,"Ready for collection","Your sample reservation for Thinking in Systems is ready at Lumina Central.","/reservations","demo-ready-"+ready);
   }
   if(ModuleAccess.is("borrowing")||ModuleAccess.is("suppliers")){
    DB.update(c,"INSERT INTO loans(user_id,book_id,copy_id,kind,status,start_at,due_at) VALUES(?,?,?,'PHYSICAL','OVERDUE',?,?)",other,6,18,Timestamp.from(Instant.now().minus(Duration.ofDays(17))),Timestamp.from(Instant.now().minus(Duration.ofDays(3))));
    DB.update(c,"UPDATE copies SET status='BORROWED' WHERE id=18");
   }
   if(ModuleAccess.is("suppliers"))DB.update(c,"INSERT INTO purchases(supplier_id,book_id,branch_id,quantity,unit_cost,created_at,invoice_ref,notes) VALUES(1,2,1,5,750,?,'DEMO-ORDER-001','Sample order ready to receive')",now);
   DB.update(c,"INSERT INTO settings(setting_key,setting_value) VALUES('member_demo_seed','1')");
   return null;
  });
 }
}
