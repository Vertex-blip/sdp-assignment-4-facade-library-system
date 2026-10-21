package library;

public final class Main {
    public static void main(String[] args) {
        LibraryFacade library = new LibraryFacade();
        String loanId = library.borrowBook("S001", "B001");
        System.out.println("Borrowed B001. Loan ID: " + loanId);

        try {
            library.borrowBook("S003", "B001");
        } catch (IllegalStateException error) {
            System.out.println(error.getMessage());
        }

        System.out.println("Fine: " + library.returnBook(loanId) + " KZT");
        System.out.println("Returned copy borrowed by S003: " + library.borrowBook("S003", "B001"));
    }
}
