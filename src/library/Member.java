package library;

public record Member(String id, String name, boolean active) {
    public Member {
        if (id == null || id.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Member ID and name must not be blank");
        }
    }
}
