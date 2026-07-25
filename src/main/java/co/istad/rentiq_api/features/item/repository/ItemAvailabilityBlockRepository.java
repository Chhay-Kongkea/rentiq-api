package co.istad.rentiq_api.features.item.repository;

import co.istad.rentiq_api.features.item.entity.ItemAvailabilityBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemAvailabilityBlockRepository extends JpaRepository<ItemAvailabilityBlock, UUID> {

    List<ItemAvailabilityBlock> findByItem_IdOrderByStartDateAsc(UUID itemId);

    Optional<ItemAvailabilityBlock> findByIdAndItem_Id(UUID id, UUID itemId);

    @Query("""
            select count(b) > 0 from ItemAvailabilityBlock b
            where b.item.id = :itemId
              and b.startDate <= :end
              and b.endDate >= :start
            """)
    boolean existsOverlapping(@Param("itemId") UUID itemId,
                               @Param("start") LocalDate start,
                               @Param("end") LocalDate end);
}
