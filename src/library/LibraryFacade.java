package library;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class LibraryFacade {
    private final MemberRegistry members;
    private final BookCatalog catalog;
    private final LoanRegistry loans;
    private final FineService fines;
    private final Clock clock;

    public LibraryFacade() {
        this(new MemberRegistry(List.of(new Member("S001", "Muhammad", true),
                        new Member("S002", "Inactive member", false), new Member("S003", "Aruzhan", true))),
                new BookCatalog(List.of(new Book("B001", "Clean Code", 1),
                        new Book("B002", "Head First Design Patterns", 2),
                        new Book("B003", "Effective Java", 1), new Book("B004", "Refactoring", 1))),
                new LoanRegistry(), new FineService(), Clock.systemDefaultZone());
    }

    LibraryFacade(MemberRegistry members, BookCatalog catalog, LoanRegistry loans, FineService fines, Clock clock) {
        this.members = Objects.requireNonNull(members);
        this.catalog = Objects.requireNonNull(catalog);
        this.loans = Objects.requireNonNull(loans);
        this.fines = Objects.requireNonNull(fines);
        this.clock = Objects.requireNonNull(clock);
    }

    public LoanReceipt borrowBook(String memberId, String bookId) {
        String loanId = UUID.randomUUID().toString();
        try {
            members.requireActive(memberId);
            fines.requireNoDebt(memberId);
            loans.ensureCanBorrow(memberId, bookId);
            Book book = catalog.reserve(loanId, bookId);
            return loans.open(loanId, memberId, book, LocalDate.now(clock));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            catalog.release(loanId);
            throw new LibraryException("Cannot borrow book: " + failure.getMessage(), failure);
        }
    }

    public LoanReceipt returnBook(String loanId) {
        try {
            LoanReceipt receipt = loans.find(loanId);
            if (receipt.isReturned()) {
                return receipt;
            }
            LocalDate returnedOn = LocalDate.now(clock);
            BigDecimal fine = fines.calculate(receipt.dueOn(), returnedOn);
            LoanReceipt returned = loans.close(loanId, returnedOn, fine);
            fines.charge(loanId, receipt.memberId(), fine);
            catalog.release(loanId);
            return returned;
        } catch (IllegalArgumentException | IllegalStateException failure) {
            throw new LibraryException("Cannot return book: " + failure.getMessage(), failure);
        }
    }

    public LoanReceipt getLoan(String loanId) {
        try {
            return loans.find(loanId);
        } catch (IllegalArgumentException failure) {
            throw new LibraryException("Cannot find loan: " + failure.getMessage(), failure);
        }
    }

    public BigDecimal payFine(String memberId, BigDecimal amount) {
        try {
            members.requireMember(memberId);
            return fines.pay(memberId, amount);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            throw new LibraryException("Cannot pay fine: " + failure.getMessage(), failure);
        }
    }
}
