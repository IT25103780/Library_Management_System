package lk.lumina.config;

import lk.lumina.repository.JdbcRepository;

/** Connection diagnostic, without schema writes. */
public final class DatabaseCheck {
  public static void main(String[] args) throws Exception {
    try {
      JdbcRepository.init();
      try (var c = JdbcRepository.connection()) {
        System.out.println("Configuration: " + AppConfig.source());
        System.out.println(
            "Connected: " + c.getMetaData().getDatabaseProductName() + " / " + c.getCatalog());
        System.out.println("Connection successful. No records changed.");
      }
    } finally {
      JdbcRepository.close();
    }
  }
}
