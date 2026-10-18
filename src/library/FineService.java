package library;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class FineService {
    private static final BigDecimal DAILY_FINE_KZT = new BigDecimal("100.00");
    private final Map<String, BigDecimal> balances = new HashMap<>();
    private final Set<String> chargedLoans = new HashSet<>();

    public BigDecimal calculate(LocalDate dueOn, LocalDate returnedOn) {
        Objects.requireNonNull(dueOn, "Due date is required");
        Objects.requireNonNull(returnedOn, "Return date is required");
        long overdueDays = Math.max(0, ChronoUnit.DAYS.between(dueOn, returnedOn));
        return DAILY_FINE_KZT.multiply(BigDecimal.valueOf(overdueDays));
    }

    public void charge(String loanId, String memberId, BigDecimal amount) {
        Objects.requireNonNull(loanId, "Loan ID is required");
        Objects.requireNonNull(memberId, "Member ID is required");
        Objects.requireNonNull(amount, "Fine amount is required");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Fine cannot be negative");
        }
        if (chargedLoans.add(loanId)) {
            balances.merge(memberId, amount, BigDecimal::add);
        }
    }

    public void requireNoDebt(String memberId) {
        if (balanceOf(memberId).signum() > 0) {
            throw new IllegalStateException("Unpaid fine: " + balanceOf(memberId).toPlainString() + " KZT");
        }
    }

    public BigDecimal pay(String memberId, BigDecimal amount) {
        Objects.requireNonNull(memberId, "Member ID is required");
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new IllegalArgumentException("Payment must be positive with at most two decimal places");
        }
        BigDecimal balance = balanceOf(memberId);
        if (amount.compareTo(balance) > 0) {
            throw new IllegalArgumentException("Payment exceeds the outstanding fine");
        }
        BigDecimal remaining = balance.subtract(amount);
        balances.put(memberId, remaining);
        return remaining;
    }

    public BigDecimal balanceOf(String memberId) {
        Objects.requireNonNull(memberId, "Member ID is required");
        return balances.getOrDefault(memberId, new BigDecimal("0.00"));
    }
}
