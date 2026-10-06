package lk.lumina.service;

import java.sql.Connection;
import lk.lumina.repository.SettingsRepository;

/** Settings operations, preserving existing validation and transaction boundaries. */
public final class SettingsService {
  public static int setting(Connection c, String key) throws Exception {
    return Integer.parseInt(SettingsRepository.findByKey(c, key).get("setting_value").toString());
  }
}
