package lk.lumina.config;

import java.util.LinkedHashMap;
import java.util.Map;

/** Editable directory fields used by the workspace and its forms. */
public final class EntityDefinitions {
  public static final Map<String, String[]> ENTITIES = new LinkedHashMap<>();

  static {
    ENTITIES.put("branches", new String[] {"name", "address", "phone", "hours"});
    ENTITIES.put("categories", new String[] {"name", "description"});
    ENTITIES.put("authors", new String[] {"name", "description"});
    ENTITIES.put("publishers", new String[] {"name", "description"});
    ENTITIES.put("suppliers", new String[] {"name", "email", "phone", "address"});
  }
}
