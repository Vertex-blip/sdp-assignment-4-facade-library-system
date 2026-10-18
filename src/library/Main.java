package library;

public final class Main {
    public static void main(String[] args) {
        LibraryFacade library = new LibraryFacade();

        System.out.println("1. Borrow a book using one facade call");
        LoanReceipt receipt = library.borrowBook("S001", "B001");
        display(receipt);

        System.out.println("\n2. Reject borrowing when no copies remain");
        tryBorrow(library, "S003", "B001");

        System.out.println("\n3. Reject an inactive member");
        tryBorrow(library, "S002", "B002");

        System.out.println("\n4. Return the book and release its copy");
        display(library.returnBook(receipt.loanId()));
        System.out.println("Stored loan is returned: " + library.getLoan(receipt.loanId()).isReturned());

        System.out.println("\n5. Another member can borrow the returned copy");
        display(library.borrowBook("S003", "B001"));

        System.out.println("\n6. Reject an unknown book");
        tryBorrow(library, "S001", "B999");
    }

    private static void tryBorrow(LibraryFacade library, String memberId, String bookId) {
        try {
            display(library.borrowBook(memberId, bookId));
        } catch (LibraryException failure) {
            System.out.println(failure.getMessage());
        }
    }

    private static void display(LoanReceipt receipt) {
        System.out.printf("%s | member %s | %s | fine: %s KZT%n", receipt.bookTitle(), receipt.memberId(),
                receipt.isReturned() ? "RETURNED" : "BORROWED", receipt.fineKzt().toPlainString());
        System.out.println("Borrowed: " + receipt.borrowedOn() + " | Due: " + receipt.dueOn());
    }
}
