package library;

public record Book(String id, String title, int copies) {
    public Book {
        if (id == null || id.isBlank() || title == null || title.isBlank()) {
            throw new IllegalArgumentException("Book ID and title must not be blank");
        }
        if (copies <= 0) {
            throw new IllegalArgumentException("A book must have at least one copy");
        }
    }
}
