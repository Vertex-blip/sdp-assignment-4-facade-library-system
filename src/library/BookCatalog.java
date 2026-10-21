package library;

import java.util.HashMap;
import java.util.Map;

public final class BookCatalog {
    private final Map<String, Integer> copies = new HashMap<>(Map.of("B001", 1, "B002", 2));
    private final Map<String, String> reservations = new HashMap<>();

    public void reserve(String loanId, String bookId) {
        Integer available = copies.get(bookId);
        if (available == null) {
            throw new IllegalArgumentException("Unknown book: " + bookId);
        }
        if (reservations.containsKey(loanId)) {
            throw new IllegalStateException("Loan already has a book");
        }
        if (available == 0) {
            throw new IllegalStateException("No copies available: " + bookId);
        }
        copies.put(bookId, available - 1);
        reservations.put(loanId, bookId);
    }

    public void release(String loanId) {
        String bookId = reservations.remove(loanId);
        if (bookId != null) {
            copies.merge(bookId, 1, Integer::sum);
        }
    }

    public int availableCopies(String bookId) {
        Integer available = copies.get(bookId);
        if (available == null) {
            throw new IllegalArgumentException("Unknown book: " + bookId);
        }
        return available;
    }
}
