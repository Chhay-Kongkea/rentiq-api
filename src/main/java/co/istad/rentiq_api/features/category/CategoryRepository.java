package co.istad.rentiq_api.features.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    boolean existsByNameIgnoreCase(String name);

    List<Category> findByParentId(Integer parentId);
}