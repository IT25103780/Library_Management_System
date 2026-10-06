package lk.lumina.controller;

import static lk.lumina.security.AccessControl.branch;
import static lk.lumina.security.AccessControl.staffOnly;
import static lk.lumina.service.FileStorageService.upload;
import static lk.lumina.util.ViewUtils.text;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lk.lumina.repository.BookRepository;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.service.AuditService;
import lk.lumina.service.BookReferenceService;
import lk.lumina.service.ReservationService;
import lk.lumina.util.BusinessRules;

/** Handles book requests using the existing URLs and form contracts. */
public final class BookController extends BaseController {
  public static void get(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {

    switch (path) {
      case "/catalog":
        {
          {
            String filter = " WHERE b.active=1";
            List<Object> args = new ArrayList<>();
            if (!p(q, "q").isBlank()) {
              filter += " AND (b.title LIKE ? OR b.isbn LIKE ? OR a.name LIKE ? OR p.name LIKE ?)";
              for (int i = 0; i < 4; i++) args.add("%" + p(q, "q") + "%");
            }
            if (num(q, "category") > 0) {
              filter += " AND b.category_id=?";
              args.add(num(q, "category"));
            }
            if (Set.of("DIGITAL", "PHYSICAL").contains(p(q, "format"))) {
              filter += " AND b.format IN (?, 'BOTH')";
              args.add(p(q, "format"));
            }
            if (p(q, "available").equals("1"))
              filter +=
                  " AND (b.pdf_path IS NOT NULL OR EXISTS(SELECT 1 FROM copies x WHERE"
                      + " x.book_id=b.id AND x.status='AVAILABLE'))";
            if (num(q, "branch") > 0) {
              filter +=
                  " AND EXISTS(SELECT 1 FROM copies x WHERE x.book_id=b.id AND x.branch_id=?)";
              args.add(num(q, "branch"));
            }
            int page = (int) Math.max(1, Math.min(10000, num(q, "page")));
            q.setAttribute("page", page);
            q.setAttribute(
                "total",
                BookRepository.countCatalog(String.valueOf(filter), args.toArray()).get("n"));
            String order = p(q, "sort").equals("title") ? "b.title" : "b.id DESC";
            q.setAttribute(
                "books",
                BookRepository.searchCatalog(
                    String.valueOf(filter),
                    String.valueOf(order),
                    String.valueOf(((page - 1) * 12)),
                    args.toArray()));
            q.setAttribute("categories", BookRepository.listActiveCategories());
            q.setAttribute("branches", BookRepository.listActiveBranches());
            view(q, r, "catalog");
            break;
          }
        }

      case "/book":
        {
          q.setAttribute("book", BookRepository.findCatalogBook(num(q, "id")));
          BusinessRules.require(q.getAttribute("book") != null, "Book not found.");
          q.setAttribute("branches", BookRepository.listBranchAvailability(num(q, "id")));
          view(q, r, "book");
          break;
        }

      case "/book-edit":
        {
          staffOnly(u);
          q.setAttribute("nextReference", BookReferenceService.preview());
          q.setAttribute("book", num(q, "id") > 0 ? BookRepository.findById(num(q, "id")) : null);
          for (String t : List.of("authors", "publishers", "categories", "branches"))
            q.setAttribute(t, BookRepository.listActiveDirectory(String.valueOf(t)));
          view(q, r, "book-edit");
          break;
        }
      default:
        throw new IllegalArgumentException("Unknown get.");
    }
  }

  public static void post(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {
    long uid = JdbcRepository.id(u, "id");
    switch (path) {
      case "/book-edit":
        {
          staffOnly(u);
          saveBook(q, u);
          go(q, r, "/manage?entity=books", "Book and inventory saved.");
          return;
        }
      default:
        throw new IllegalArgumentException("Unknown post.");
    }
  }

  private static void saveBook(HttpServletRequest q, Map<String, Object> u) throws Exception {
    long id = num(q, "id");
    var old = id > 0 ? BookRepository.findById(id) : null;
    BusinessRules.require(id == 0 || old != null, "Book not found.");
    String title = required(q, "title", 240), format = p(q, "format");
    BusinessRules.require(
        Set.of("PHYSICAL", "DIGITAL", "BOTH").contains(format), "Choose a book format.");
    BigDecimal fee = price(q, "fee");
    BusinessRules.require(
        format.equals("PHYSICAL") || fee.signum() > 0,
        "Digital books require a fee greater than zero.");
    int days = positive(q, "duration_days", 365);
    int year = positive(q, "publication_year", 2100);
    String description = required(q, "description", 4000);
    String edition = required(q, "edition", 60);
    List<Path> created = new ArrayList<>();
    try {
      String cover = upload(q.getPart("cover"), false, created);
      String pdf = upload(q.getPart("pdf"), true, created);
      if (cover == null && old != null) cover = text(old, "cover_path");
      if (pdf == null && old != null) pdf = text(old, "pdf_path");
      BusinessRules.require(
          format.equals("PHYSICAL") || pdf != null && !pdf.isBlank(),
          "Upload a PDF for a digital book.");
      final String coverFile = cover == null || cover.isBlank() ? null : cover;
      final String pdfFile = pdf == null || pdf.isBlank() ? null : pdf;
      BookReferenceService.transaction(
          c -> {
            String savedIsbn =
                old == null ? BookReferenceService.next(c) : old.get("isbn").toString();
            long saved = id;
            for (String t : List.of("authors", "categories", "publishers")) {
              String key =
                  t.equals("categories") ? "category_id" : t.substring(0, t.length() - 1) + "_id";
              BusinessRules.require(
                  BookRepository.findActiveDirectoryRecord(c, String.valueOf(t), num(q, key))
                      != null,
                  "Select an active " + key + ".");
            }
            if (id == 0)
              saved =
                  BookRepository.insertBook(
                      c,
                      title,
                      savedIsbn,
                      num(q, "author_id"),
                      num(q, "category_id"),
                      num(q, "publisher_id"),
                      description,
                      year,
                      edition,
                      format,
                      fee,
                      days,
                      coverFile,
                      pdfFile,
                      BusinessRules.now());
            else
              BookRepository.updateBook(
                  c,
                  title,
                  savedIsbn,
                  num(q, "author_id"),
                  num(q, "category_id"),
                  num(q, "publisher_id"),
                  description,
                  year,
                  edition,
                  format,
                  fee,
                  days,
                  coverFile,
                  pdfFile,
                  id);
            int copies = (int) num(q, "copies");
            BusinessRules.require(
                copies >= 0 && copies <= 500, "Add at most 500 copies at a time.");
            if (copies > 0) {
              BusinessRules.require(
                  !format.equals("DIGITAL"), "Digital-only books cannot have physical copies.");
              long br = num(q, "branch_id");
              if (branch(u) != null)
                BusinessRules.require(br == branch(u), "Select your assigned branch.");
              BusinessRules.require(
                  BookRepository.findActiveBranch(c, br) != null, "Select an active branch.");
              for (int i = 0; i < copies; i++) BookRepository.insertCopy(c, saved, br);
            }
            ReservationService.fulfill(c);
            AuditService.audit(c, JdbcRepository.id(u, "id"), "Save book", "Book " + saved);
            return null;
          });
    } catch (Exception ex) {
      for (Path file : created) Files.deleteIfExists(file);
      throw ex;
    }
  }
}
