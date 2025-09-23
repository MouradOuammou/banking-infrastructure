package com.banking.notifications.repository;

import com.banking.notifications.entity.Notification;
import com.banking.notifications.entity.NotificationChannel;
import com.banking.notifications.entity.NotificationStatus;
import com.banking.notifications.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserId(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndStatus(Long userId, NotificationStatus status, Pageable pageable);

    Page<Notification> findByUserIdAndType(Long userId, NotificationType type, Pageable pageable);

    Page<Notification> findByUserIdAndChannel(Long userId, NotificationChannel channel, Pageable pageable);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByStatusAndScheduledAtBefore(NotificationStatus status, LocalDateTime dateTime);

    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.retryCount < 3")
    List<Notification> findFailedNotificationsForRetry();

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.status = 'SENT' AND n.createdAt >= :startDate")
    Long countSentNotificationsByUserSince(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = :status AND n.createdAt BETWEEN :startDate AND :endDate")
    Long countByStatusAndDateRange(@Param("status") NotificationStatus status, 
                                  @Param("startDate") LocalDateTime startDate, 
                                  @Param("endDate") LocalDateTime endDate);

    List<Notification> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);
}
