package library;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BookCatalog {
    private final Map<String, Book> books = new HashMap<>();
    private final Map<String, Integer> availableCopies = new HashMap<>();
    private final Map<String, String> reservations = new HashMap<>();

    public BookCatalog(List<Book> initialBooks) {
        for (Book book : List.copyOf(initialBooks)) {
            if (books.putIfAbsent(book.id(), book) != null) {
                throw new IllegalArgumentException("Book IDs must be unique");
            }
            availableCopies.put(book.id(), book.copies());
        }
    }

    public Book reserve(String loanId, String bookId) {
        Objects.requireNonNull(loanId, "Loan ID is required");
        if (reservations.containsKey(loanId)) {
            throw new IllegalArgumentException("Loan already has a reserved copy");
        }
        Book book = find(bookId);
        int available = availableCopies.get(bookId);
        if (available == 0) {
            throw new IllegalStateException("No copies available: " + book.title());
        }
        availableCopies.put(bookId, available - 1);
        reservations.put(loanId, bookId);
        return book;
    }

    public void release(String loanId) {
        String bookId = reservations.remove(loanId);
        if (bookId != null) {
            availableCopies.merge(bookId, 1, Integer::sum);
        }
    }

    public int availableCopies(String bookId) {
        find(bookId);
        return availableCopies.get(bookId);
    }

    private Book find(String bookId) {
        Book book = books.get(bookId);
        if (book == null) {
            throw new IllegalArgumentException("Unknown book: " + bookId);
        }
        return book;
    }
}
