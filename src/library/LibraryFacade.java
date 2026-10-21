package library;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

public final class LibraryFacade {
    private final MemberRegistry members = new MemberRegistry();
    private final BookCatalog books = new BookCatalog();
    private final LoanRegistry loans = new LoanRegistry();
    private final FineService fines = new FineService();
    private final Clock clock;

    public LibraryFacade() {
        this(Clock.systemDefaultZone());
    }

    LibraryFacade(Clock clock) {
        this.clock = clock;
    }

    public String borrowBook(String memberId, String bookId) {
        String loanId = UUID.randomUUID().toString();
        try {
            members.requireActive(memberId);
            fines.requireNoDebt(memberId);
            loans.ensureCanBorrow(memberId, bookId);
            books.reserve(loanId, bookId);
            loans.open(loanId, memberId, bookId, LocalDate.now(clock));
            return loanId;
        } catch (IllegalArgumentException | IllegalStateException error) {
            books.release(loanId);
            throw new IllegalStateException("Cannot borrow: " + error.getMessage(), error);
        }
    }

    public BigDecimal returnBook(String loanId) {
        LoanRegistry.Loan loan = loans.find(loanId);
        if (loan.isReturned()) {
            return loan.fine();
        }
        LocalDate today = LocalDate.now(clock);
        BigDecimal fine = fines.calculate(loan.dueOn(), today);
        loans.close(loanId, today, fine);
        fines.charge(loanId, loan.memberId(), fine);
        books.release(loanId);
        return fine;
    }

    public BigDecimal payFine(String memberId, BigDecimal amount) {
        members.requireMember(memberId);
        return fines.pay(memberId, amount);
    }
}
