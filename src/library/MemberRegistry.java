package library;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MemberRegistry {
    private final Map<String, Member> members = new HashMap<>();

    public MemberRegistry(List<Member> initialMembers) {
        for (Member member : List.copyOf(initialMembers)) {
            if (members.putIfAbsent(member.id(), member) != null) {
                throw new IllegalArgumentException("Member IDs must be unique");
            }
        }
    }

    public Member requireMember(String memberId) {
        Member member = members.get(memberId);
        if (member == null) {
            throw new IllegalArgumentException("Unknown member: " + memberId);
        }
        return member;
    }

    public Member requireActive(String memberId) {
        Member member = requireMember(memberId);
        if (!member.active()) {
            throw new IllegalStateException("Membership is inactive: " + memberId);
        }
        return member;
    }
}
