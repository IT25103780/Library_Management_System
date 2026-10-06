package lk.lumina.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.security.Security;
import lk.lumina.service.BookReferenceService;
import lk.lumina.service.MailService;
import lk.lumina.service.NotificationService;
import lk.lumina.service.ReminderService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

@WebListener
public class ApplicationInitializer implements ServletContextListener {
  private ScheduledExecutorService timer;

  public void contextInitialized(ServletContextEvent event) {
    try {
      JdbcRepository.init();
      schema();
      DatabaseMigrations.run();
      seed();
      BookReferenceService.initialize();
      indexes();
      timer =
          Executors.newSingleThreadScheduledExecutor(
              r -> {
                Thread t = new Thread(r, "lumina-reminders");
                t.setDaemon(true);
                return t;
              });
      timer.scheduleWithFixedDelay(
          () -> {
            try {
              ReminderService.reminders();
              MailService.deliver();
            } catch (Exception e) {
              event.getServletContext().log("Reminder/email processing failed; will retry", e);
            }
          },
          15,
          60,
          TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Lumina could not start. Check database and storage configuration.", e);
    }
  }

  public void contextDestroyed(ServletContextEvent event) {
    if (timer != null) timer.shutdownNow();
    JdbcRepository.close();
  }

  public static void schema() throws Exception {
    String schema =
        new String(
            ApplicationInitializer.class.getResourceAsStream("/schema.sql").readAllBytes(),
            java.nio.charset.StandardCharsets.UTF_8);
    try (Connection c = JdbcRepository.connection()) {
      for (String sql : schema.split(";")) {
        if (sql.isBlank()) continue;
        String table = sql.trim().split(" ")[2];
        boolean exists = false;
        try (ResultSet r =
            c.getMetaData()
                .getTables(c.getCatalog(), c.getSchema(), null, new String[] {"TABLE"})) {
          while (r.next()) if (table.equalsIgnoreCase(r.getString("TABLE_NAME"))) exists = true;
        }
        if (!exists)
          try (Statement s = c.createStatement()) {
            s.execute(sql);
          }
      }
    }
  }

  public static void indexes() throws Exception {
    try (Connection c = JdbcRepository.connection()) {
      String[][] indexes = {
        {"loans", "ix_loans_access", "user_id,kind,status,due_at"},
        {"payments", "ix_payments_owner", "user_id,status"},
        {"notifications", "ix_notifications_unread", "user_id,is_read,id"},
        {"copies", "ix_copies_availability", "book_id,branch_id,status"},
        {"reservations", "ix_reservations_queue", "status,book_id,branch_id,id"},
        {"books", "ix_books_category", "active,category_id"}
      };
      for (String[] index : indexes) {
        boolean exists = false;
        try (ResultSet rs =
            c.getMetaData().getIndexInfo(c.getCatalog(), c.getSchema(), index[0], false, false)) {
          while (rs.next())
            if (index[1].equalsIgnoreCase(rs.getString("INDEX_NAME"))) exists = true;
        }
        if (!exists)
          try (Statement s = c.createStatement()) {
            s.execute("CREATE INDEX " + index[1] + " ON " + index[0] + "(" + index[2] + ")");
          }
      }
    }
  }

  public static void seed() throws Exception {
    if (JdbcRepository.one("SELECT * FROM users") != null) {
      ensureSamples();
      return;
    }
    JdbcRepository.tx(
        c -> {
          var now = Timestamp.from(Instant.now());
          long branch =
              JdbcRepository.update(
                  c,
                  "INSERT INTO branches(name,address,phone,hours) VALUES(?,?,?,?)",
                  "Lumina Central",
                  "Colombo Â· Main reading room",
                  "011 555 0180",
                  "Monâ€“Sat 8:30 amâ€“6:00 pm");
          JdbcRepository.update(
              c,
              "INSERT INTO branches(name,address,phone,hours) VALUES(?,?,?,?)",
              "The Garden Branch",
              "Kandy Â· Garden reading room",
              "081 555 0180",
              "Monâ€“Sat 9:00 amâ€“5:00 pm");
          String pass = Security.hash(AppConfig.get("bootstrap.password", "Lumina@2026!"));
          for (String role :
              List.of("ADMIN", "LIBRARIAN", "BRANCH_MANAGER", "MANAGEMENT", "READER")) {
            String user = role.toLowerCase(Locale.ROOT);
            JdbcRepository.update(
                c,
                "INSERT INTO"
                    + " users(name,email,username,password_hash,phone,address,role,branch_id,created_at)"
                    + " VALUES(?,?,?,?,?,?,?,?,?)",
                role.equals("READER") ? "Demo Reader" : "Lumina " + role.replace('_', ' '),
                user + "@lumina.test",
                user,
                pass,
                "0770000000",
                "Colombo",
                role,
                branch,
                now);
          }
          String[] cats = {
            "Literature", "Science & nature", "History", "Technology", "Art & philosophy", "Travel"
          };
          for (String cat : cats)
            JdbcRepository.update(
                c,
                "INSERT INTO categories(name,description) VALUES(?,?)",
                cat,
                "Explore the " + cat.toLowerCase() + " collection.");
          JdbcRepository.update(
              c,
              "INSERT INTO publishers(name,description) VALUES(?,?)",
              "Lumina Editions",
              "Original demonstration reading material.");
          String[] authors = {"Lumina Editorial", "The Reading Room", "Lumina Field Notes"};
          for (String a : authors)
            JdbcRepository.update(
                c,
                "INSERT INTO authors(name,description) VALUES(?,?)",
                a,
                "The Lumina original sample collection.");
          String[] titles = {
            "The Art of Paying Attention",
            "A Field Guide to Wonder",
            "Letters from the Old World",
            "Thinking in Systems",
            "The Quiet Architecture",
            "Beyond the Familiar"
          };
          String[] desc = {
            "An invitation to slow down, look closer, and rediscover the remarkable in everyday"
                + " life.",
            "Follow the patterns of the natural world, from the smallest leaf to the widest night"
                + " sky.",
            "A reading-room journey through memory, archives, and the stories we choose to"
                + " preserve.",
            "A thoughtful introduction to connections, feedback, and the structures behind everyday"
                + " decisions.",
            "Explore the relationship between space, stillness, and the art of making room for"
                + " thought.",
            "Notes on curiosity, new places, and finding another way to see the world."
          };
          Files.createDirectories(AppConfig.data().resolve("pdf"));
          Files.createDirectories(AppConfig.data().resolve("covers"));
          for (int i = 0; i < titles.length; i++) {
            String file = "sample-" + (i + 1) + ".pdf";
            samplePdf(AppConfig.data().resolve("pdf").resolve(file), titles[i], desc[i]);
            long book =
                JdbcRepository.update(
                    c,
                    "INSERT INTO"
                        + " books(isbn,title,author_id,category_id,publisher_id,description,publication_year,edition,format,pdf_path,fee,duration_days,created_at)"
                        + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    "LUM-000" + (i + 1),
                    titles[i],
                    (i % 3) + 1,
                    i + 1,
                    1,
                    desc[i],
                    2026,
                    "First edition",
                    "BOTH",
                    file,
                    150 + i * 25,
                    7,
                    now);
            for (int j = 0; j < 3; j++)
              JdbcRepository.update(
                  c, "INSERT INTO copies(book_id,branch_id) VALUES(?,?)", book, j == 2 ? 2 : 1);
          }
          JdbcRepository.update(
              c,
              "INSERT INTO suppliers(name,email,phone,address) VALUES(?,?,?,?)",
              "Lumina Book Supply",
              "supply@example.test",
              "0115550190",
              "Colombo");
          for (String[] pair :
              new String[][] {
                {"physical_days", "14"},
                {"max_loans", "5"},
                {"fine_per_day", "20"},
                {"reservation_hours", "48"}
              })
            if (JdbcRepository.one(
                    c, "SELECT setting_key FROM settings WHERE setting_key=?", pair[0])
                == null)
              JdbcRepository.update(
                  c,
                  "INSERT INTO settings(setting_key,setting_value) VALUES(?,?)",
                  pair[0],
                  pair[1]);
          for (var u : JdbcRepository.list(c, "SELECT id FROM users"))
            NotificationService.notify(
                c,
                JdbcRepository.id(u, "id"),
                "Welcome to Lumina",
                "Your reading room is ready. Explore the collection and make yourself at home.",
                "/catalog",
                "welcome-" + u.get("id"));
          return null;
        });
  }

  private static void ensureSamples() throws Exception {
    Files.createDirectories(AppConfig.data().resolve("pdf"));
    Files.createDirectories(AppConfig.data().resolve("covers"));
    for (var b :
        JdbcRepository.list(
            "SELECT title,description,pdf_path FROM books WHERE isbn LIKE 'LUM-%'")) {
      String file = Objects.toString(b.get("pdf_path"), "");
      if (file.matches("sample-[1-6][.]pdf")) {
        Path path = AppConfig.data().resolve("pdf").resolve(file);
        if (!Files.exists(path))
          samplePdf(path, b.get("title").toString(), b.get("description").toString());
      }
    }
  }

  private static void samplePdf(Path dest, String title, String description) throws Exception {
    try (PDDocument d = new PDDocument()) {
      for (int page = 1; page <= 4; page++) {
        PDPage p = new PDPage();
        d.addPage(p);
        try (PDPageContentStream out = new PDPageContentStream(d, p)) {
          out.setNonStrokingColor(43, 27, 23);
          out.addRect(0, 680, 612, 112);
          out.fill();
          out.beginText();
          out.setNonStrokingColor(245, 239, 230);
          out.setFont(PDType1Font.TIMES_ROMAN, 25);
          out.newLineAtOffset(48, 732);
          out.showText(title);
          out.endText();
          out.beginText();
          out.setNonStrokingColor(36, 26, 22);
          out.setFont(PDType1Font.HELVETICA, 12);
          out.setLeading(24);
          out.newLineAtOffset(48, 630);
          for (String line :
              new String[] {
                "LUMINA ORIGINAL SAMPLE  /  CHAPTER " + page,
                "",
                "This short original sample is included to test the online reader.",
                "Upload your own licensed PDF from the staff book workspace.",
                "",
                "A library is a place to pause, to ask a question, and to discover.",
                "Each shelf opens another conversation. Each page offers a",
                "different perspective on a world that rewards our attention.",
                "",
                "Reading begins with curiosity. Give an idea a little time and",
                "it can change the way you notice the familiar things around you.",
                "",
                "Your reading position is saved by the Lumina reader.",
                "",
                "Page " + page + " of 4"
              }) {
            out.showText(line);
            out.newLine();
          }
          out.endText();
        }
      }
      d.save(dest.toFile());
    }
  }
}
