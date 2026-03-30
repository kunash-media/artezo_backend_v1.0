package com.artezo.dto.stats.orders;

import java.math.BigDecimal;

public interface OrderStats {
    Long getTotalOrdersCount();
    BigDecimal getTotalSpent();
}