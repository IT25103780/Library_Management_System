package lk.lumina;
import java.util.*;import java.math.*;
/** Multiple purchase lines share an invoice reference and commit together. */
public final class Procurement {
 public record Item(long book,int quantity,BigDecimal unitCost){}
 public static void create(long actor,long supplier,long branch,String invoice,String notes,List<Item> items,Long scope)throws Exception{
  Library.require(scope==null||scope==branch,"Select your assigned branch.");
  Library.require(!items.isEmpty()&&items.size()<=20,"Enter at least one purchase line, up to 20.");
  Library.require(invoice!=null&&invoice.length()<=100&&notes.length()<=500,"Invoice or notes are too long.");
  String reference=invoice.isBlank()?"PO-"+UUID.randomUUID().toString().substring(0,12):invoice;
  DB.tx(c->{
   Library.require(DB.one(c,"SELECT id FROM suppliers WHERE id=? AND active=1",supplier)!=null,"Choose an active supplier.");
   Library.require(DB.one(c,"SELECT id FROM branches WHERE id=? AND active=1",branch)!=null,"Choose an active branch.");
   Library.require(DB.one(c,"SELECT id FROM purchases WHERE supplier_id=? AND invoice_ref=? AND status<>'CANCELLED'",supplier,reference)==null,"This supplier invoice is already recorded.");
   Set<Long> seen=new HashSet<>();
   for(Item item:items){
    Library.require(item.quantity()>0&&item.quantity()<=500&&item.unitCost().signum()>=0&&item.unitCost().compareTo(new BigDecimal("10000000"))<0,"Invalid quantity or unit cost.");
    Library.require(seen.add(item.book()),"Combine duplicate books into one purchase line.");
    Library.require(DB.one(c,"SELECT id FROM books WHERE id=? AND active=1 AND format<>'DIGITAL'",item.book())!=null,"Choose an active physical book.");
    DB.update(c,"INSERT INTO purchases(supplier_id,book_id,branch_id,quantity,unit_cost,created_at,invoice_ref,notes) VALUES(?,?,?,?,?,?,?,?)",supplier,item.book(),branch,item.quantity(),item.unitCost(),Library.now(),reference,notes);
   }
   Library.audit(c,actor,"Create purchase","Invoice "+reference+" / "+items.size()+" lines");return null;
  });
 }
}
