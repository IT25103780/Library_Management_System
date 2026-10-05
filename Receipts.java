package lk.lumina;
import java.sql.*;import java.util.*;import java.io.*;import java.awt.Color;
import org.apache.pdfbox.pdmodel.*;import org.apache.pdfbox.pdmodel.font.*;
public final class Receipts {
 public static void capture(Connection c,long id)throws Exception{
  var p=DB.one(c,"SELECT p.*,u.name,u.email,b.title,b.isbn,l.start_at,l.due_at FROM payments p JOIN users u ON u.id=p.user_id LEFT JOIN fines f ON f.id=p.fine_id LEFT JOIN loans fl ON fl.id=f.loan_id LEFT JOIN books b ON b.id=COALESCE(p.book_id,fl.book_id) LEFT JOIN loans l ON l.id=p.loan_id WHERE p.id=?",id);
  if(p==null||p.get("receipt_name")!=null)return;
  Object start=p.get("start_at");if(p.get("due_at")!=null)start=Timestamp.from(((Timestamp)p.get("due_at")).toInstant().minus(java.time.Duration.ofDays(((Number)p.get("duration_days")).longValue())));
  DB.update(c,"UPDATE payments SET receipt_title=?,receipt_isbn=?,receipt_name=?,receipt_email=?,receipt_start=?,receipt_due=? WHERE id=? AND receipt_name IS NULL",p.get("title"),p.get("isbn"),p.get("name"),p.get("email"),start,p.get("due_at"),id);
 }
 public static Map<String,Object> forOwner(long id,long uid)throws Exception{return DB.one("SELECT p.*,COALESCE(p.receipt_name,u.name) AS name,COALESCE(p.receipt_email,u.email) AS email,COALESCE(p.receipt_title,b.title) AS title,COALESCE(p.receipt_isbn,b.isbn) AS isbn FROM payments p JOIN users u ON u.id=p.user_id LEFT JOIN books b ON b.id=p.book_id WHERE p.id=? AND p.user_id=? AND p.status='SUCCESSFUL'",id,uid);}
 private static String printable(Object v){String s=Objects.toString(v,"—");StringBuilder b=new StringBuilder();for(char ch:s.toCharArray()){try{PDType1Font.HELVETICA.encode(String.valueOf(ch));b.append(ch);}catch(Exception e){b.append('?');}}return b.toString();}
 private static void text(PDPageContentStream out,PDFont font,float size,float x,float y,Object value)throws IOException{out.beginText();out.setFont(font,size);out.newLineAtOffset(x,y);out.showText(printable(value));out.endText();}
 public static void pdf(Map<String,Object> p,OutputStream target)throws Exception{
  try(PDDocument doc=new PDDocument()){PDPage page=new PDPage();doc.addPage(page);try(PDPageContentStream out=new PDPageContentStream(doc,page)){
   out.setNonStrokingColor(new Color(245,239,230));out.addRect(0,0,612,792);out.fill();out.setNonStrokingColor(Color.WHITE);out.addRect(30,30,552,732);out.fill();out.setNonStrokingColor(new Color(43,27,23));out.addRect(30,650,552,112);out.fill();out.setNonStrokingColor(new Color(201,162,39));text(out,PDType1Font.TIMES_ROMAN,27,54,715,"LUMINA");text(out,PDType1Font.HELVETICA,9,54,691,"THE LIBRARY / PAYMENT RECEIPT");text(out,PDType1Font.HELVETICA_BOLD,11,472,712,"PAID");
   out.setNonStrokingColor(new Color(43,27,23));text(out,PDType1Font.TIMES_ROMAN,32,54,610,"Thank you.");text(out,PDType1Font.HELVETICA,10,54,586,"Your payment has been recorded.");
   if("DEMO".equals(p.get("provider"))){out.setNonStrokingColor(new Color(111,78,55));text(out,PDType1Font.HELVETICA_BOLD,10,54,559,"DEMONSTRATION RECEIPT - NO MONEY CHARGED");}
   String method=Objects.toString(p.get("method"),"")+(p.get("card_last4")==null?"":" / ending "+p.get("card_last4"));
   String[][] rows={{"Receipt","LUM-"+p.get("id")},{"Paid by",Objects.toString(p.get("name"),"")},{"Payment date",Web.date(p.get("paid_at"))+" (Sri Lanka)"},{"Description",Objects.toString(p.get("title"),"Library fine")},{"Provider / method",p.get("provider")+" / "+method},{"Transaction reference",Objects.toString(p.get("provider_ref"),"")},{"Borrowing starts",Web.date(p.get("receipt_start"))},{"Borrowing expires",Web.date(p.get("receipt_due"))}};
   float y=525;for(String[] row:rows){if(row[0].startsWith("Borrowing")&&p.get("loan_id")==null)continue;out.setNonStrokingColor(new Color(120,105,94));text(out,PDType1Font.HELVETICA,9,54,y,row[0]);out.setNonStrokingColor(new Color(43,27,23));String value=printable(row[1]);List<String> lines=new ArrayList<>();StringBuilder line=new StringBuilder();for(char ch:value.toCharArray()){if(PDType1Font.HELVETICA.getStringWidth(line.toString()+ch)/1000*10>325){lines.add(line.toString());line.setLength(0);}line.append(ch);}lines.add(line.toString());for(String l:lines){text(out,PDType1Font.HELVETICA,10,215,y,l);y-=14;}y-=15;}
   out.setNonStrokingColor(new Color(245,239,230));out.addRect(48,y-43,516,48);out.fill();out.setNonStrokingColor(new Color(43,27,23));text(out,PDType1Font.HELVETICA_BOLD,11,60,y-24,"TOTAL PAID");text(out,PDType1Font.TIMES_ROMAN,23,365,y-25,Web.money(p.get("amount")));

   out.setNonStrokingColor(new Color(120,105,94));text(out,PDType1Font.HELVETICA,9,54,53,"Lumina Library · A world within reach");
  }doc.save(target);}
 }
}
