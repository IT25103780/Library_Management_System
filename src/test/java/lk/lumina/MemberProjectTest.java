package lk.lumina;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.math.*;
import java.util.*;
import java.lang.reflect.*;
import jakarta.servlet.http.*;

/** Tests real database transactions and the independent member boundary. */
public class MemberProjectTest {
 @TempDir Path folder;
 @BeforeEach void open()throws Exception{
  System.setProperty("db.mode","demo");System.setProperty("storage.path",folder.toString());
  DB.init();Bootstrap.schema();Migrations.run();Bootstrap.seed();BookReferences.initialize();
 }
 @AfterEach void close(){DB.close();System.clearProperty("db.mode");System.clearProperty("storage.path");}
 private HttpServletRequest request(String method,String path,Map<String,String> values){return (HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{HttpServletRequest.class},(p,m,a)->switch(m.getName()){case "getServletPath"->path;case "getMethod"->method;case "getParameter"->values.get(a[0]);default->null;});}
 @Test void routesAndEntitiesAreIsolated(){
  var admin=Map.<String,Object>of("id",1L,"role","ADMIN");
  var reader=Map.<String,Object>of("id",5L,"role","READER");
  assertFalse(ModuleAccess.allowed(request("POST","/action",Map.of("action","pay-fine")),admin));
  assertFalse(ModuleAccess.allowed(request("GET","/payment-admin",Map.of()),admin));
  assertFalse(ModuleAccess.allowed(request("POST","/checkout",Map.of()),admin));
  for(String entity:List.of("books","copies","users","branches","categories","authors","publishers","suppliers","purchases")){
   assertEquals(ModuleAccess.ENTITIES.contains(entity),ModuleAccess.allowed(request("POST","/manage",Map.of("entity",entity)),admin),entity);
   assertFalse(ModuleAccess.allowed(request("POST","/manage",Map.of("entity",entity)),reader));
   if(!ModuleAccess.ENTITIES.contains(entity))assertFalse(ModuleAccess.allowed(request("POST","/action",Map.of("action","delete-record","entity",entity)),admin));
  }
  assertEquals("/home",ModuleAccess.safeLink("/reader?id=1"));
  assertEquals("/catalog",ModuleAccess.safeLink("/catalog"));
 }
 @Test void duplicateIssueRollsBackAndReturnReleasesCopy()throws Exception{
  Library.issue(1,5,1,null);
  assertThrows(IllegalArgumentException.class,()->Library.issue(1,5,1,null));
  assertEquals(1,DB.id(DB.one("SELECT COUNT(*) AS n FROM loans"),"n"));
  long loan=DB.id(DB.one("SELECT id FROM loans WHERE copy_id=1"),"id");
  assertEquals("BORROWED",DB.one("SELECT status FROM copies WHERE id=1").get("status"));
  Library.returnBook(1,loan,null);
  assertEquals("AVAILABLE",DB.one("SELECT status FROM copies WHERE id=1").get("status"));
  assertEquals(0,DB.id(DB.one("SELECT COUNT(*) AS n FROM fines"),"n"));
  assertThrows(IllegalArgumentException.class,()->Library.returnBook(1,loan,null));
 }
 @Test void reservationRequiresUnavailableBookAndPreservesQueue()throws Exception{
  assertThrows(IllegalArgumentException.class,()->Library.reserve(1,1,1));
  Library.issue(1,5,1,null);Library.issue(1,5,2,null);
  Library.reserve(1,1,1);Library.reserve(2,1,1);
  assertThrows(IllegalArgumentException.class,()->Library.reserve(1,1,1));
  long loan=DB.id(DB.one("SELECT id FROM loans WHERE copy_id=1"),"id");Library.returnBook(1,loan,null);
  var first=DB.one("SELECT * FROM reservations WHERE user_id=1");
  assertEquals("READY",first.get("status"));
  assertEquals("WAITING",DB.one("SELECT status FROM reservations WHERE user_id=2").get("status"));
  Library.cancelReservation(1,DB.id(first,"id"),false,null);
  assertEquals("READY",DB.one("SELECT status FROM reservations WHERE user_id=2").get("status"));
  assertEquals("RESERVED",DB.one("SELECT status FROM copies WHERE id=1").get("status"));
 }
 @Test void overdueFineSupportsPartialPaymentAndRejectsOverpayment()throws Exception{
  Library.issue(1,5,1,null);long loan=DB.id(DB.one("SELECT id FROM loans WHERE copy_id=1"),"id");
  DB.update("UPDATE loans SET due_at=? WHERE id=?",Timestamp.from(Instant.now().minus(Duration.ofDays(2))),loan);
  Library.returnBook(1,loan,null);var fine=DB.one("SELECT * FROM fines WHERE loan_id=?",loan);long id=DB.id(fine,"id");
  assertTrue(((BigDecimal)fine.get("amount")).compareTo(new BigDecimal("40"))>=0);
  Workflows.offlinePayment(1,id,new BigDecimal("10"),"CASH","TEST-RECEIPT",null);
  assertEquals("PARTIALLY_PAID",DB.one("SELECT status FROM fines WHERE id=?",id).get("status"));
  assertThrows(IllegalArgumentException.class,()->Workflows.offlinePayment(1,id,new BigDecimal("99999"),"CASH","",null));
  assertEquals(1,DB.id(DB.one("SELECT COUNT(*) AS n FROM payments WHERE fine_id=?",id),"n"));
  assertThrows(IllegalArgumentException.class,()->Workflows.adjustFine(1,id,BigDecimal.ZERO,"Invalid correction",null,true));
 }
 @Test void receiptOfStockCannotExceedOrder()throws Exception{
  Procurement.create(1,1,1,"TEST-ORDER","",List.of(new Procurement.Item(1,3,new BigDecimal("100"))),null);
  long order=DB.id(DB.one("SELECT id FROM purchases WHERE invoice_ref='TEST-ORDER'"),"id");
  long before=DB.id(DB.one("SELECT COUNT(*) AS n FROM copies"),"n");
  Workflows.receive(1,order,2,null);
  assertEquals("PARTIALLY_RECEIVED",DB.one("SELECT status FROM purchases WHERE id=?",order).get("status"));
  assertThrows(IllegalArgumentException.class,()->Workflows.receive(1,order,2,null));
  assertEquals(before+2,DB.id(DB.one("SELECT COUNT(*) AS n FROM copies"),"n"));
  Workflows.receive(1,order,1,null);
  assertEquals("RECEIVED",DB.one("SELECT status FROM purchases WHERE id=?",order).get("status"));
 }
}
