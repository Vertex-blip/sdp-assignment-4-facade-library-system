package library;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

public final class LibraryFacadeTest {
    private static final LocalDate BORROW_DATE = LocalDate.of(2026, 10, 1);
    private static int passed;

    public static void main(String[] args) {
        run("Borrow reserves a copy and creates a 14-day loan", LibraryFacadeTest::borrowing);
        run("Inactive and unknown members cannot change stock", LibraryFacadeTest::invalidMembers);
        run("Missing and blank book IDs produce facade errors", LibraryFacadeTest::invalidBookIds);
        run("Unavailable copies do not create extra loans", LibraryFacadeTest::unavailableCopy);
        run("A member cannot borrow the same title twice", LibraryFacadeTest::duplicateBorrowing);
        run("The fourth active loan is rejected", LibraryFacadeTest::loanLimit);
        run("Returning a book releases a copy and a loan slot", LibraryFacadeTest::returning);
        run("Returning twice cannot increase stock twice", LibraryFacadeTest::repeatedReturn);
        run("Early and due-date returns have no fine", LibraryFacadeTest::onTimeReturns);
        run("An overdue return charges 100 KZT per day", LibraryFacadeTest::overdueReturn);
        run("Repeated overdue returns cannot charge twice", LibraryFacadeTest::repeatedOverdueReturn);
        run("Debt blocks borrowing until the full fine is paid", LibraryFacadeTest::finePayment);
        run("Invalid payments leave the balance unchanged", LibraryFacadeTest::invalidPayments);
        run("Unknown loans cannot alter existing loans", LibraryFacadeTest::unknownLoans);
        run("A failed loan opening releases its reserved copy", LibraryFacadeTest::reservationRollback);
        run("A return before borrowing leaves the loan active", LibraryFacadeTest::invalidReturnDate);
        run("Subsystems work directly without a facade", LibraryFacadeTest::directSubsystemUse);
        run("Catalog reservations and releases preserve copy counts", LibraryFacadeTest::catalogReservations);
        System.out.println("All " + passed + " tests passed.");
    }

    private static void borrowing() {
        Fixture fixture = new Fixture();
        LoanReceipt receipt = fixture.facadeAt(BORROW_DATE).borrowBook("S001", "B001");
        check(receipt.memberId().equals("S001"), "Wrong member");
        check(receipt.bookTitle().equals("Clean Code"), "Wrong book");
        check(receipt.borrowedOn().equals(BORROW_DATE), "Wrong borrowing date");
        check(receipt.dueOn().equals(BORROW_DATE.plusDays(14)), "Wrong due date");
        check(!receipt.isReturned(), "New loan is already returned");
        check(fixture.catalog.availableCopies("B001") == 0, "Copy was not reserved");
        check(fixture.loans.activeLoanCount("S001") == 1, "Loan was not stored");
        check(fixture.facadeAt(BORROW_DATE).getLoan(receipt.loanId()).equals(receipt), "Lookup lost receipt");
    }

    private static void invalidMembers() {
        Fixture fixture = new Fixture();
        LibraryFacade facade = fixture.facadeAt(BORROW_DATE);
        expectFailure(() -> facade.borrowBook("S002", "B001"), "inactive");
        expectFailure(() -> facade.borrowBook("UNKNOWN", "B001"), "Unknown member");
        expectFailure(() -> facade.borrowBook(null, "B001"), "Unknown member");
        check(fixture.catalog.availableCopies("B001") == 1, "Rejected member reserved a copy");
    }

    private static void invalidBookIds() {
        Fixture fixture = new Fixture();
        LibraryFacade facade = fixture.facadeAt(BORROW_DATE);
        expectFailure(() -> facade.borrowBook("S001", "UNKNOWN"), "Unknown book");
        expectFailure(() -> facade.borrowBook("S001", null), "must not be blank");
        expectFailure(() -> facade.borrowBook("S001", "  "), "must not be blank");
        check(fixture.loans.activeLoanCount("S001") == 0, "Invalid book created a loan");
    }

    private static void unavailableCopy() {
        Fixture fixture = new Fixture();
        LibraryFacade facade = fixture.facadeAt(BORROW_DATE);
        facade.borrowBook("S001", "B001");
        expectFailure(() -> facade.borrowBook("S003", "B001"), "No copies available");
        check(fixture.catalog.availableCopies("B001") == 0, "Stock became invalid");
        check(fixture.loans.activeLoanCount("S003") == 0, "Unavailable book created a loan");
    }

    private static void duplicateBorrowing() {
        Fixture fixture = new Fixture();
        LibraryFacade facade = fixture.facadeAt(BORROW_DATE);
        facade.borrowBook("S001", "B002");
        expectFailure(() -> facade.borrowBook("S001", "B002"), "already borrowed");
        check(fixture.catalog.availableCopies("B002") == 1, "Duplicate borrowing consumed a copy");
    }

    private static void loanLimit() {
        Fixture fixture = new Fixture();
        LibraryFacade facade = fixture.facadeAt(BORROW_DATE);
        facade.borrowBook("S001", "B001");
        facade.borrowBook("S001", "B002");
        facade.borrowBook("S001", "B003");
        expectFailure(() -> facade.borrowBook("S001", "B004"), "limit of 3");
        check(fixture.catalog.availableCopies("B004") == 1, "Loan limit consumed a copy");
        check(fixture.loans.activeLoanCount("S001") == 3, "Loan limit was exceeded");
    }

    private static void returning() {
        Fixture fixture = new Fixture();
        LibraryFacade facade = fixture.facadeAt(BORROW_DATE);
        LoanReceipt receipt = facade.borrowBook("S001", "B001");
        LoanReceipt returned = facade.returnBook(receipt.loanId());
        check(returned.isReturned(), "Return was not recorded");
        check(returned.returnedOn().equals(BORROW_DATE), "Wrong return date");
        check(fixture.catalog.availableCopies("B001") == 1, "Copy was not released");
        check(fixture.loans.activeLoanCount("S001") == 0, "Loan slot was not released");
        facade.borrowBook("S003", "B001");
    }

    private static void repeatedReturn() {
        Fixture fixture = new Fixture();
        LibraryFacade facade = fixture.facadeAt(BORROW_DATE);
        LoanReceipt receipt = facade.borrowBook("S001", "B001");
        LoanReceipt returned = facade.returnBook(receipt.loanId());
        check(facade.returnBook(receipt.loanId()).equals(returned), "Repeated return changed the receipt");
        check(fixture.catalog.availableCopies("B001") == 1, "Repeated return inflated stock");
        facade.borrowBook("S003", "B001");
        facade.returnBook(receipt.loanId());
        check(fixture.catalog.availableCopies("B001") == 0, "Old return released another member's copy");
    }

    private static void onTimeReturns() {
        for (int days : new int[] {0, 13, 14}) {
            Fixture fixture = new Fixture();
            LoanReceipt receipt = fixture.facadeAt(BORROW_DATE).borrowBook("S001", "B001");
            LoanReceipt returned = fixture.facadeAt(BORROW_DATE.plusDays(days)).returnBook(receipt.loanId());
            checkMoney(returned.fineKzt(), "0.00");
            checkMoney(fixture.fines.balanceOf("S001"), "0.00");
        }
    }

    private static void overdueReturn() {
        Fixture fixture = new Fixture();
        LoanReceipt receipt = fixture.facadeAt(BORROW_DATE).borrowBook("S001", "B001");
        LoanReceipt returned = fixture.facadeAt(BORROW_DATE.plusDays(17)).returnBook(receipt.loanId());
        checkMoney(returned.fineKzt(), "300.00");
        checkMoney(fixture.fines.balanceOf("S001"), "300.00");
        check(fixture.catalog.availableCopies("B001") == 1, "Overdue return did not release the copy");
    }

    private static void repeatedOverdueReturn() {
        Fixture fixture = new Fixture();
        LoanReceipt receipt = fixture.facadeAt(BORROW_DATE).borrowBook("S001", "B001");
        fixture.facadeAt(BORROW_DATE.plusDays(17)).returnBook(receipt.loanId());
        LoanReceipt repeated = fixture.facadeAt(BORROW_DATE.plusDays(20)).returnBook(receipt.loanId());
        checkMoney(repeated.fineKzt(), "300.00");
        checkMoney(fixture.fines.balanceOf("S001"), "300.00");
    }

    private static void finePayment() {
        Fixture fixture = new Fixture();
        LoanReceipt receipt = fixture.facadeAt(BORROW_DATE).borrowBook("S001", "B001");
        LibraryFacade facade = fixture.facadeAt(BORROW_DATE.plusDays(17));
        facade.returnBook(receipt.loanId());
        expectFailure(() -> facade.borrowBook("S001", "B002"), "Unpaid fine");
        checkMoney(facade.payFine("S001", new BigDecimal("100.00")), "200.00");
        expectFailure(() -> facade.borrowBook("S001", "B002"), "Unpaid fine");
        checkMoney(facade.payFine("S001", new BigDecimal("200.00")), "0.00");
        facade.borrowBook("S001", "B002");
        check(fixture.loans.activeLoanCount("S001") == 1, "Paid fine did not unblock borrowing");
    }

    private static void invalidPayments() {
        Fixture fixture = new Fixture();
        fixture.fines.charge("OLD-LOAN", "S001", new BigDecimal("300.00"));
        LibraryFacade facade = fixture.facadeAt(BORROW_DATE);
        for (BigDecimal amount : new BigDecimal[] {null, BigDecimal.ZERO,
                new BigDecimal("-1.00"), new BigDecimal("0.001"), new BigDecimal("301.00")}) {
            expectFailure(() -> facade.payFine("S001", amount), "Cannot pay fine");
        }
        expectFailure(() -> facade.payFine("UNKNOWN", new BigDecimal("100.00")), "Unknown member");
        checkMoney(fixture.fines.balanceOf("S001"), "300.00");
    }

    private static void unknownLoans() {
        Fixture fixture = new Fixture();
        LibraryFacade facade = fixture.facadeAt(BORROW_DATE);
        LoanReceipt receipt = facade.borrowBook("S001", "B001");
        expectFailure(() -> facade.getLoan("UNKNOWN"), "Unknown loan");
        expectFailure(() -> facade.returnBook("UNKNOWN"), "Unknown loan");
        expectFailure(() -> facade.returnBook(null), "Unknown loan");
        check(!facade.getLoan(receipt.loanId()).isReturned(), "Unknown return changed a real loan");
        check(fixture.catalog.availableCopies("B001") == 0, "Unknown return changed stock");
    }

    private static void reservationRollback() {
        Fixture fixture = new Fixture();
        Clock failingClock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                throw new IllegalStateException("Clock unavailable");
            }
        };
        LibraryFacade facade = new LibraryFacade(fixture.members, fixture.catalog, fixture.loans,
                fixture.fines, failingClock);
        expectFailure(() -> facade.borrowBook("S001", "B001"), "Clock unavailable");
        check(fixture.catalog.availableCopies("B001") == 1, "Failed submission leaked its reservation");
        check(fixture.loans.activeLoanCount("S001") == 0, "Failed submission created a loan");
        fixture.facadeAt(BORROW_DATE).borrowBook("S003", "B001");
    }

    private static void invalidReturnDate() {
        Fixture fixture = new Fixture();
        LoanReceipt receipt = fixture.facadeAt(BORROW_DATE).borrowBook("S001", "B001");
        LibraryFacade earlier = fixture.facadeAt(BORROW_DATE.minusDays(1));
        expectFailure(() -> earlier.returnBook(receipt.loanId()), "Return date");
        check(fixture.catalog.availableCopies("B001") == 0, "Invalid return released the copy");
        check(!fixture.loans.find(receipt.loanId()).isReturned(), "Invalid return closed the loan");
        checkMoney(fixture.fines.balanceOf("S001"), "0.00");
    }

    private static void directSubsystemUse() {
        Fixture fixture = new Fixture();
        Member member = fixture.members.requireActive("S001");
        fixture.fines.requireNoDebt(member.id());
        fixture.loans.ensureCanBorrow(member.id(), "B001");
        Book book = fixture.catalog.reserve("DIRECT-LOAN", "B001");
        LoanReceipt receipt = fixture.loans.open("DIRECT-LOAN", member.id(), book, BORROW_DATE);
        LocalDate returnDate = receipt.dueOn().plusDays(1);
        BigDecimal fine = fixture.fines.calculate(receipt.dueOn(), returnDate);
        fixture.loans.close(receipt.loanId(), returnDate, fine);
        fixture.fines.charge(receipt.loanId(), member.id(), fine);
        fixture.catalog.release(receipt.loanId());
        checkMoney(fixture.fines.balanceOf(member.id()), "100.00");
        check(fixture.catalog.availableCopies("B001") == 1, "Direct subsystem composition failed");
    }

    private static void catalogReservations() {
        Fixture fixture = new Fixture();
        fixture.catalog.reserve("FIRST", "B002");
        fixture.catalog.reserve("SECOND", "B002");
        check(fixture.catalog.availableCopies("B002") == 0, "Multiple copies were not reserved");
        fixture.catalog.release("FIRST");
        fixture.catalog.release("FIRST");
        fixture.catalog.release("UNKNOWN");
        check(fixture.catalog.availableCopies("B002") == 1, "Release must be idempotent");
        fixture.catalog.release("SECOND");
        check(fixture.catalog.availableCopies("B002") == 2, "Copy count was not restored");
    }

    private static void run(String name, Runnable test) {
        test.run();
        passed++;
        System.out.println("PASS: " + name);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void checkMoney(BigDecimal actual, String expected) {
        check(actual.compareTo(new BigDecimal(expected)) == 0, "Expected " + expected + " but got " + actual);
    }

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
        } catch (LibraryException failure) {
            check(failure.getMessage().contains(message), "Unexpected error: " + failure.getMessage());
            check(failure.getCause() != null, "Original subsystem error was lost");
            return;
        }
        throw new AssertionError("Expected a LibraryException containing: " + message);
    }

    private static final class Fixture {
        private final MemberRegistry members = new MemberRegistry(List.of(new Member("S001", "Muhammad", true),
                new Member("S002", "Inactive member", false), new Member("S003", "Aruzhan", true)));
        private final BookCatalog catalog = new BookCatalog(List.of(new Book("B001", "Clean Code", 1),
                new Book("B002", "Head First Design Patterns", 2),
                new Book("B003", "Effective Java", 1), new Book("B004", "Refactoring", 1)));
        private final LoanRegistry loans = new LoanRegistry();
        private final FineService fines = new FineService();

        private LibraryFacade facadeAt(LocalDate date) {
            Clock clock = Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
            return new LibraryFacade(members, catalog, loans, fines, clock);
        }
    }
}
