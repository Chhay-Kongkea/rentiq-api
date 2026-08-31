package co.istad.rentiq_api.features.userProfile.repository;


import co.istad.rentiq_api.features.userProfile.entity.User;
import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import co.istad.rentiq_api.features.adminDashboard.projection.DashboardCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface UserRepository extends JpaRepository<User, String> {

    long countByAccountStatus(AccountStatus status);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(OffsetDateTime from, OffsetDateTime to);

    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<User> findAllByIdInAndAccountStatus(Collection<String> ids, AccountStatus accountStatus);

    @Query(value = """
            select cast(date_trunc(:groupBy, u.created_at at time zone 'UTC') as date) as period,
                   count(*) as value
            from users u
            where u.created_at >= :from and u.created_at < :to
            group by date_trunc(:groupBy, u.created_at at time zone 'UTC')
            order by period
            """, nativeQuery = true)
    java.util.List<DashboardCountProjection> countRegistrationsByPeriod(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("groupBy") String groupBy);
}
