package library;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public final class LoanRegistry {
    public record Loan(String id, String memberId, String bookId, LocalDate borrowedOn,
            LocalDate dueOn, LocalDate returnedOn, BigDecimal fine) {
        public boolean isReturned() {
            return returnedOn != null;
        }
    }

    private final Map<String, Loan> loans = new HashMap<>();

    public void ensureCanBorrow(String memberId, String bookId) {
        if (memberId == null || memberId.isBlank() || bookId == null || bookId.isBlank()) {
            throw new IllegalArgumentException("Member and book IDs are required");
        }
        long active = loans.values().stream()
                .filter(loan -> loan.memberId().equals(memberId) && !loan.isReturned()).count();
        if (active >= 3) {
            throw new IllegalStateException("Three active loans are already open");
        }
        boolean duplicate = loans.values().stream().anyMatch(loan ->
                loan.memberId().equals(memberId) && loan.bookId().equals(bookId) && !loan.isReturned());
        if (duplicate) {
            throw new IllegalStateException("Member already borrowed this book");
        }
    }

    public Loan open(String loanId, String memberId, String bookId, LocalDate date) {
        ensureCanBorrow(memberId, bookId);
        if (loans.containsKey(loanId)) {
            throw new IllegalStateException("Loan ID already exists");
        }
        Loan loan = new Loan(loanId, memberId, bookId, date, date.plusDays(14), null, BigDecimal.ZERO);
        loans.put(loanId, loan);
        return loan;
    }

    public Loan find(String loanId) {
        Loan loan = loans.get(loanId);
        if (loan == null) {
            throw new IllegalArgumentException("Unknown loan: " + loanId);
        }
        return loan;
    }

    public Loan close(String loanId, LocalDate date, BigDecimal fine) {
        Loan loan = find(loanId);
        if (loan.isReturned()) {
            return loan;
        }
        if (date.isBefore(loan.borrowedOn())) {
            throw new IllegalArgumentException("Return date precedes borrowing date");
        }
        Loan returned = new Loan(loan.id(), loan.memberId(), loan.bookId(),
                loan.borrowedOn(), loan.dueOn(), date, fine);
        loans.put(loanId, returned);
        return returned;
    }
}
