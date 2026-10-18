package library;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class LoanRegistry {
    private static final int LOAN_PERIOD_DAYS = 14;
    private static final int MAX_ACTIVE_LOANS = 3;
    private final Map<String, LoanReceipt> loans = new LinkedHashMap<>();

    public void ensureCanBorrow(String memberId, String bookId) {
        if (memberId == null || memberId.isBlank() || bookId == null || bookId.isBlank()) {
            throw new IllegalArgumentException("Member ID and book ID must not be blank");
        }
        if (activeLoanCount(memberId) >= MAX_ACTIVE_LOANS) {
            throw new IllegalStateException("Member has reached the limit of " + MAX_ACTIVE_LOANS + " active loans");
        }
        boolean alreadyBorrowed = loans.values().stream()
                .anyMatch(loan -> !loan.isReturned() && loan.memberId().equals(memberId)
                        && loan.bookId().equals(bookId));
        if (alreadyBorrowed) {
            throw new IllegalStateException("Member already borrowed this book");
        }
    }

    public LoanReceipt open(String loanId, String memberId, Book book, LocalDate borrowedOn) {
        Objects.requireNonNull(loanId, "Loan ID is required");
        Objects.requireNonNull(book, "Book is required");
        Objects.requireNonNull(borrowedOn, "Borrowing date is required");
        if (loans.containsKey(loanId)) {
            throw new IllegalArgumentException("Loan ID must be unique");
        }
        ensureCanBorrow(memberId, book.id());
        LoanReceipt receipt = new LoanReceipt(loanId, memberId, book.id(), book.title(),
                borrowedOn, borrowedOn.plusDays(LOAN_PERIOD_DAYS), null, new BigDecimal("0.00"));
        loans.put(loanId, receipt);
        return receipt;
    }

    public LoanReceipt find(String loanId) {
        LoanReceipt receipt = loans.get(loanId);
        if (receipt == null) {
            throw new IllegalArgumentException("Unknown loan: " + loanId);
        }
        return receipt;
    }

    public LoanReceipt close(String loanId, LocalDate returnedOn, BigDecimal fineKzt) {
        LoanReceipt receipt = find(loanId);
        if (receipt.isReturned()) {
            return receipt;
        }
        Objects.requireNonNull(returnedOn, "Return date is required");
        Objects.requireNonNull(fineKzt, "Fine is required");
        if (returnedOn.isBefore(receipt.borrowedOn()) || fineKzt.signum() < 0) {
            throw new IllegalArgumentException("Return date and fine must be valid");
        }
        LoanReceipt returned = new LoanReceipt(receipt.loanId(), receipt.memberId(), receipt.bookId(),
                receipt.bookTitle(), receipt.borrowedOn(), receipt.dueOn(), returnedOn, fineKzt);
        loans.put(loanId, returned);
        return returned;
    }

    public long activeLoanCount(String memberId) {
        return loans.values().stream()
                .filter(loan -> loan.memberId().equals(memberId) && !loan.isReturned())
                .count();
    }
}
