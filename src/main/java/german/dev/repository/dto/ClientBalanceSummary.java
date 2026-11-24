package german.dev.repository.dto;

import java.math.BigDecimal;

public record ClientBalanceSummary(String fullName, long accountCount, BigDecimal totalBalance,
                                   BigDecimal averageBalance) {

}
