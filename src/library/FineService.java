package library;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class FineService {
    private final Map<String, BigDecimal> balances = new HashMap<>();
    private final Set<String> chargedLoans = new HashSet<>();

    public BigDecimal calculate(LocalDate dueOn, LocalDate returnedOn) {
        long days = Math.max(0, ChronoUnit.DAYS.between(dueOn, returnedOn));
        return BigDecimal.valueOf(days).multiply(new BigDecimal("100.00"));
    }

    public void charge(String loanId, String memberId, BigDecimal fine) {
        if (chargedLoans.add(loanId)) {
            balances.merge(memberId, fine, BigDecimal::add);
        }
    }

    public void requireNoDebt(String memberId) {
        if (balanceOf(memberId).signum() > 0) {
            throw new IllegalStateException("Unpaid fine: " + memberId);
        }
    }

    public BigDecimal pay(String memberId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2
                || amount.compareTo(balanceOf(memberId)) > 0) {
            throw new IllegalArgumentException("Invalid fine payment");
        }
        BigDecimal remaining = balanceOf(memberId).subtract(amount);
        balances.put(memberId, remaining);
        return remaining;
    }

    public BigDecimal balanceOf(String memberId) {
        return balances.getOrDefault(memberId, BigDecimal.ZERO);
    }
}
