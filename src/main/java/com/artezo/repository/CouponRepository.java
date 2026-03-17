package com.artezo.repository;

import com.artezo.entity.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Repository
public interface CouponRepository extends JpaRepository<CouponEntity, Long> {

    Logger LOGGER = Logger.getLogger(CouponRepository.class.getName());

    Optional<CouponEntity> findByCouponCode(String couponCode);

    List<CouponEntity> findByIsActiveTrue();

    @Query("SELECT c FROM CouponEntity c WHERE c.validFrom <= :currentDate AND c.validTo >= :currentDate AND c.isActive = true")
    List<CouponEntity> findActiveCoupons(@Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT c FROM CouponEntity c WHERE c.minOrderAmount <= :orderAmount")
    List<CouponEntity> findCouponsByMinOrderAmount(@Param("orderAmount") Double orderAmount);

    boolean existsByCouponCode(String couponCode);

    default void logMethodCall(String methodName, Object... params) {
        StringBuilder logMessage = new StringBuilder("Repository method called: " + methodName);
        if (params.length > 0) {
            logMessage.append(" with params: ");
            for (Object param : params) {
                logMessage.append(param).append(", ");
            }
        }
        LOGGER.info(logMessage.toString());
    }
}
