package lk.lumina.controller;

import static lk.lumina.controller.FileResponse.file;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lk.lumina.config.AppConfig;
import lk.lumina.repository.BookRepository;

/** Handles media requests using the existing URLs and form contracts. */
public final class MediaController extends BaseController {
  public static void get(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {

    switch (path) {
      case "/cover":
        {
          var b = BookRepository.findCover(num(q, "id"));
          if (b == null || b.get("cover_path") == null) {
            r.sendRedirect(
                q.getContextPath()
                    + "/assets/cover-"
                    + ((Math.max(1, num(q, "id")) - 1) % 6 + 1)
                    + ".svg");
            return;
          }
          r.setHeader("Cache-Control", "public, max-age=0, must-revalidate");
          String etag = "\"" + b.get("cover_path") + "\"";
          r.setHeader("ETag", etag);
          if (etag.equals(q.getHeader("If-None-Match"))) {
            r.setStatus(304);
            return;
          }
          file(
              q,
              r,
              AppConfig.data().resolve("covers"),
              b.get("cover_path").toString(),
              "image/jpeg");
          break;
        }
      default:
        throw new IllegalArgumentException("Unknown get.");
    }
  }
}
