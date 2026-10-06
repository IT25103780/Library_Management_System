package lk.lumina;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import lk.lumina.config.ApplicationInitializer;
import lk.lumina.config.DatabaseMigrations;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.service.BookReferenceService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BookReferencesTest {
  @BeforeAll
  static void setup() throws Exception {
    System.setProperty("storage.path", Files.createTempDirectory("lumina-reference-").toString());
    System.setProperty("db.mode", "demo");
    JdbcRepository.init();
    ApplicationInitializer.schema();
    DatabaseMigrations.run();
    ApplicationInitializer.seed();
    BookReferenceService.initialize();
  }

  @AfterAll
  static void stop() {
    JdbcRepository.close();
  }

  @Test
  void persistentSequentialAndConcurrentReferences() throws Exception {
    assertEquals("LUM-0007", BookReferenceService.preview());
    assertEquals("LUM-0007", BookReferenceService.transaction(BookReferenceService::next));
    assertEquals("LUM-0008", BookReferenceService.transaction(BookReferenceService::next));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            BookReferenceService.transaction(
                c -> {
                  BookReferenceService.next(c);
                  throw new IllegalArgumentException("rollback");
                }));
    assertEquals("LUM-0009", BookReferenceService.preview());
    BookReferenceService.initialize();
    assertEquals("LUM-0009", BookReferenceService.preview());
    var pool = Executors.newFixedThreadPool(4);
    try {
      List<Callable<String>> jobs = new ArrayList<>();
      for (int i = 0; i < 8; i++)
        jobs.add(() -> BookReferenceService.transaction(BookReferenceService::next));
      Set<String> values = new HashSet<>();
      for (var result : pool.invokeAll(jobs)) values.add(result.get());
      assertEquals(8, values.size());
      assertTrue(values.contains("LUM-0009"));
      assertTrue(values.contains("LUM-0016"));
    } finally {
      pool.shutdownNow();
    }
    JdbcRepository.update(
        "UPDATE settings SET setting_value='9999' WHERE setting_key='last_book_reference'");
    assertEquals("LUM-10000", BookReferenceService.transaction(BookReferenceService::next));
  }
}
