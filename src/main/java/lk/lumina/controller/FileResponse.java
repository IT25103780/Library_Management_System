package lk.lumina.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import lk.lumina.util.BusinessRules;

/** Streams covers and authorized PDFs, including range and HEAD requests. */
public final class FileResponse {
  public static void file(
      HttpServletRequest q, HttpServletResponse r, Path folder, String name, String mime)
      throws Exception {
    Path path = folder.resolve(name).normalize();
    BusinessRules.require(
        path.startsWith(folder.normalize()) && Files.isRegularFile(path),
        "File is unavailable. Ask the library to replace it.");
    long size = Files.size(path), start = 0, end = size - 1;
    String range = q.getHeader("Range");
    if (range != null && range.matches("bytes=\\d+-\\d*")) {
      String[] parts = range.substring(6).split("-", -1);
      start = Long.parseLong(parts[0]);
      if (!parts[1].isBlank()) end = Math.min(end, Long.parseLong(parts[1]));
      if (start > end || start >= size) {
        r.setStatus(416);
        r.setHeader("Content-Range", "bytes */" + size);
        return;
      }
      r.setStatus(206);
      r.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + size);
    }
    r.setContentType(mime);
    r.setHeader("Accept-Ranges", "bytes");
    r.setHeader(
        "Content-Disposition",
        "inline; filename=\""
            + (mime.equals("application/pdf") ? "lumina-book.pdf" : "cover.jpg")
            + "\"");
    r.setContentLengthLong(end - start + 1);
    if (q.getMethod().equals("HEAD")) return;
    try (RandomAccessFile f = new RandomAccessFile(path.toFile(), "r")) {
      f.seek(start);
      byte[] buf = new byte[32768];
      long left = end - start + 1;
      while (left > 0) {
        int n = f.read(buf, 0, (int) Math.min(buf.length, left));
        if (n < 0) break;
        r.getOutputStream().write(buf, 0, n);
        left -= n;
      }
    }
  }
}
