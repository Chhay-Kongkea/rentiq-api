package co.istad.rentiq_api.features.search.repository;

import co.istad.rentiq_api.features.search.entity.SearchLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SearchLogRepository extends JpaRepository<SearchLog, UUID> {

    Page<SearchLog> findAllByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}