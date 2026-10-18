package library;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanReceipt(
        String loanId,
        String memberId,
        String bookId,
        String bookTitle,
        LocalDate borrowedOn,
        LocalDate dueOn,
        LocalDate returnedOn,
        BigDecimal fineKzt) {

    public boolean isReturned() {
        return returnedOn != null;
    }
}
