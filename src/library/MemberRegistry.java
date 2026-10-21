package library;

import java.util.Map;

public final class MemberRegistry {
    private final Map<String, Boolean> members = Map.of(
            "S001", true, "S002", false, "S003", true);

    public void requireMember(String memberId) {
        if (!members.containsKey(memberId)) {
            throw new IllegalArgumentException("Unknown member: " + memberId);
        }
    }

    public void requireActive(String memberId) {
        requireMember(memberId);
        if (!members.get(memberId)) {
            throw new IllegalStateException("Inactive member: " + memberId);
        }
    }
}
