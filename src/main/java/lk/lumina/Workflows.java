package lk.lumina;

import java.sql.*;
import java.time.*;
import java.math.*;
import java.util.*;
import static lk.lumina.Library.*;

/** Transactional staff operations; callers must enforce role permissions. */
public final class Workflows {
 public static void scope(Map<String,Object> row,Long branch){
  require(row!=null,"Record not found.");
  if(branch!=null)require(DB.id(row,"branch_id")==branch,"This record belongs to another branch.");
 }
 public static BigDecimal paid(Connection c,long fine)throws Exception{
  return (BigDecimal)DB.one(c,"SELECT COALESCE(SUM(amount),0) AS n FROM payments WHERE fine_id=? AND status='SUCCESSFUL'",fine).get("n");
 }
 public static BigDecimal balance(Connection c,long fine)throws Exception{
  var f=DB.one(c,"SELECT amount FROM fines WHERE id=?",fine);require(f!=null,"Fine not found.");
  return ((BigDecimal)f.get("amount")).subtract(paid(c,fine));
 }
 public static void fineStatus(Connection c,long fine)throws Exception{
  var f=DB.one(c,"SELECT * FROM fines WHERE id=?",fine);if(f==null||Set.of("VOID","WAIVED").contains(f.get("status")))return;
  BigDecimal paid=paid(c,fine),amount=(BigDecimal)f.get("amount");
  DB.update(c,"UPDATE fines SET status=? WHERE id=?",paid.compareTo(amount)>=0?"PAID":paid.signum()>0?"PARTIALLY_PAID":"OUTSTANDING",fine);
 }
 public static void dueDate(long actor,long id,Timestamp due,String reason,Long branch)throws Exception{
  require(reason!=null&&!reason.isBlank(),"Enter a reason for the date change.");
  DB.tx(c->{var l=DB.one(c,"SELECT l.*,cp.branch_id FROM loans l JOIN copies cp ON cp.id=l.copy_id WHERE l.id=? AND l.kind='PHYSICAL'",id);scope(l,branch);
   require(Set.of("ACTIVE","OVERDUE").contains(l.get("status")),"Only an open loan can be changed.");
   require(due.after((Timestamp)l.get("start_at"))&&due.after(now()),"The new due date must be in the future and after issue.");
   require(DB.one(c,"SELECT id FROM reservations WHERE book_id=? AND branch_id=? AND status='WAITING'",l.get("book_id"),l.get("branch_id"))==null,"Another member is waiting for this title.");
   require(DB.one(c,"SELECT id FROM fines WHERE loan_id=? AND status NOT IN ('VOID','WAIVED')",id)==null,"Resolve an assessed fine before extending this loan.");
   DB.update(c,"UPDATE loans SET due_at=?,status='ACTIVE' WHERE id=?",due,id);
   audit(c,actor,"Change due date","Loan "+id+": "+reason);
   Library.notify(c,DB.id(l,"user_id"),"Due date updated","Your new due date is "+Web.date(due)+". "+reason,"/my-books","due-change-"+UUID.randomUUID());return null;});
 }
 public static void voidLoan(long actor,long id,String reason,Long branch)throws Exception{
  require(reason!=null&&!reason.isBlank(),"Enter a correction reason.");
  DB.tx(c->{var l=DB.one(c,"SELECT l.*,cp.branch_id FROM loans l JOIN copies cp ON cp.id=l.copy_id WHERE l.id=? AND l.kind='PHYSICAL'",id);scope(l,branch);
   require(Set.of("ACTIVE","OVERDUE","RETURNED").contains(l.get("status")),"Loan already voided.");
   require(DB.one(c,"SELECT id FROM fines WHERE loan_id=? AND status NOT IN ('VOID','WAIVED')",id)==null,"Void or settle the associated fine first.");
   if(!l.get("status").equals("RETURNED"))DB.update(c,"UPDATE copies SET status='AVAILABLE' WHERE id=?",l.get("copy_id"));
   DB.update(c,"UPDATE loans SET status='VOID' WHERE id=?",id);audit(c,actor,"Void mistaken loan","Loan "+id+": "+reason);fulfill(c);return null;});
 }
 public static void moveReservation(long actor,long id,long destination,boolean staff,Long branch)throws Exception{
  DB.tx(c->{var r=DB.one(c,"SELECT * FROM reservations WHERE id=?",id);scope(r,branch);
   require(staff||DB.id(r,"user_id")==actor,"This reservation belongs to another member.");
   require(Set.of("WAITING","READY").contains(r.get("status")),"Only an open reservation can be modified.");
   require(DB.id(r,"branch_id")!=destination,"Choose a different collection branch.");
   if(branch!=null)require(destination==branch,"Choose your assigned branch.");
   require(DB.one(c,"SELECT id FROM branches WHERE id=? AND active=1",destination)!=null,"Choose an active branch.");
   if(r.get("copy_id")!=null)DB.update(c,"UPDATE copies SET status='AVAILABLE' WHERE id=?",r.get("copy_id"));
   DB.update(c,"UPDATE reservations SET branch_id=?,copy_id=NULL,status='WAITING',ready_until=NULL,created_at=? WHERE id=?",destination,now(),id);
   audit(c,actor,"Change reservation branch","Reservation "+id+"; queue time restarted");fulfill(c);return null;});
 }
 public static void assess(Connection c,Map<String,Object> loan)throws Exception{
  long seconds=Duration.between(((Timestamp)loan.get("due_at")).toInstant(),Instant.now()).getSeconds();if(seconds<=0)return;
  long days=(seconds+86399)/86400;BigDecimal amount=BigDecimal.valueOf(days).multiply(BigDecimal.valueOf(setting(c,"fine_per_day")));
  var f=DB.one(c,"SELECT * FROM fines WHERE loan_id=?",loan.get("id"));
  if(f==null){long id=DB.update(c,"INSERT INTO fines(user_id,loan_id,amount,reason,created_at) VALUES(?,?,?,?,?)",loan.get("user_id"),loan.get("id"),amount,days+" overdue day(s)",now());
   Library.notify(c,DB.id(loan,"user_id"),"Overdue fine assessed","An overdue fine is available in Fines. It can grow until the book is returned.","/fines","fine-"+id);
  }else if(DB.id(f,"adjusted")==0&&!Set.of("VOID","WAIVED").contains(f.get("status"))){
   DB.update(c,"UPDATE fines SET amount=?,reason=? WHERE id=?",amount,days+" overdue day(s)",f.get("id"));fineStatus(c,DB.id(f,"id"));
  }
 }
 public static void assessOverdue(long actor,Long branch)throws Exception{
  DB.tx(c->{for(var l:DB.list(c,"SELECT l.* FROM loans l JOIN copies cp ON cp.id=l.copy_id WHERE l.kind='PHYSICAL' AND l.status IN ('ACTIVE','OVERDUE') AND l.due_at<?"+(branch==null?"":" AND cp.branch_id="+branch),now()))assess(c,l);
   audit(c,actor,"Assess overdue fines","Open physical loans");return null;});
 }
 public static long offlinePayment(long actor,long fine,BigDecimal amount,String method,String reference,Long branch)throws Exception{
  require(amount.signum()>0,"Payment amount must be positive.");require(Set.of("CASH","BANK_TRANSFER","OTHER_OFFLINE").contains(method),"Choose an offline payment method.");
  return DB.tx(c->{var f=DB.one(c,"SELECT f.*,cp.branch_id FROM fines f JOIN loans l ON l.id=f.loan_id JOIN copies cp ON cp.id=l.copy_id WHERE f.id=?",fine);scope(f,branch);
   require(Set.of("OUTSTANDING","PARTIALLY_PAID").contains(f.get("status")),"This fine is not outstanding.");
   require(DB.one(c,"SELECT id FROM payments WHERE fine_id=? AND status='PENDING'",fine)==null,"Resolve the pending online order before recording an offline payment.");
   require(amount.compareTo(balance(c,fine))<=0,"Payment exceeds the outstanding balance.");
   long id=DB.update(c,"INSERT INTO payments(order_ref,user_id,fine_id,amount,provider,method,provider_ref,status,created_at,paid_at) VALUES(?,?,?,?,'OFFLINE',?,?,'SUCCESSFUL',?,?)","OFF-"+UUID.randomUUID(),f.get("user_id"),fine,amount,method,reference,now(),now());
   Receipts.capture(c,id);fineStatus(c,fine);audit(c,actor,"Record fine payment","Payment "+id+" / Fine "+fine+" / "+amount);
   Library.notify(c,DB.id(f,"user_id"),"Fine payment recorded","Received "+Web.money(amount)+". Your receipt is available in Payments.","/payments","offline-"+id);return id;});
 }
 public static void adjustFine(long actor,long fine,BigDecimal amount,String reason,Long branch,boolean voided)throws Exception{
  require(!reason.isBlank(),"A reason is required.");require(amount.signum()>=0,"Amount cannot be negative.");
  DB.tx(c->{var f=DB.one(c,"SELECT f.*,cp.branch_id FROM fines f JOIN loans l ON l.id=f.loan_id JOIN copies cp ON cp.id=l.copy_id WHERE f.id=?",fine);scope(f,branch);
   require(!Set.of("VOID","WAIVED").contains(f.get("status")),"This fine is already closed.");
   require(DB.one(c,"SELECT id FROM payments WHERE fine_id=? AND status='PENDING'",fine)==null,"Resolve pending payments first.");
   BigDecimal paid=paid(c,fine);require(amount.compareTo(paid)>=0&&(!voided||paid.signum()==0),"Reverse recorded payments before reducing the fine below the paid amount.");
   DB.update(c,"UPDATE fines SET amount=?,reason=?,adjusted=1,status=? WHERE id=?",amount,reason,voided?"VOID":amount.signum()==0?"WAIVED":"OUTSTANDING",fine);fineStatus(c,fine);
   audit(c,actor,voided?"Void fine":"Adjust fine","Fine "+fine+": "+reason);return null;});
 }
 public static void receive(long actor,long id,int quantity,Long branch)throws Exception{
  DB.tx(c->{var p=DB.one(c,"SELECT * FROM purchases WHERE id=?",id);scope(p,branch);
   require(Set.of("ORDERED","PARTIALLY_RECEIVED").contains(p.get("status")),"Purchase is already closed.");
   int received=((Number)p.get("received_quantity")).intValue(),ordered=((Number)p.get("quantity")).intValue();
   require(quantity>0&&quantity<=ordered-received,"Enter a quantity no greater than the remaining order.");
   for(int i=0;i<quantity;i++)DB.update(c,"INSERT INTO copies(book_id,branch_id) VALUES(?,?)",p.get("book_id"),p.get("branch_id"));
   received+=quantity;DB.update(c,"UPDATE purchases SET received_quantity=?,status=? WHERE id=?",received,received==ordered?"RECEIVED":"PARTIALLY_RECEIVED",id);
   audit(c,actor,"Receive stock","Purchase "+id+": "+quantity+" copies");fulfill(c);return null;});
 }
 public static void editCopy(long actor,long id,long destination,String shelf,Long branch,boolean delete)throws Exception{
  DB.tx(c->{var cp=DB.one(c,"SELECT * FROM copies WHERE id=?",id);scope(cp,branch);
   require(!Set.of("BORROWED","RESERVED").contains(cp.get("status")),"Return or release the copy first.");
   if(delete){require(DB.one(c,"SELECT id FROM loans WHERE copy_id=?",id)==null&&DB.one(c,"SELECT id FROM reservations WHERE copy_id=?",id)==null,"This copy has history; mark it WITHDRAWN instead.");DB.update(c,"DELETE FROM copies WHERE id=?",id);}
   else{if(branch!=null)require(destination==branch,"Choose your assigned branch.");require(DB.one(c,"SELECT id FROM branches WHERE id=? AND active=1",destination)!=null,"Select an active branch.");DB.update(c,"UPDATE copies SET branch_id=?,shelf=? WHERE id=?",destination,shelf,id);fulfill(c);}
   audit(c,actor,delete?"Delete unused copy":"Edit copy","Copy "+id);return null;});
 }
}
