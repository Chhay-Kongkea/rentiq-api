package co.istad.rentiq_api.features.category;


import co.istad.rentiq_api.common.config.auditing.BasedEntity;
import co.istad.rentiq_api.features.category.cateogryDto.CategorySpecificationField;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category extends BasedEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String slug;

    @Column(name = "commission_rate", precision = 5, scale = 4)
    private BigDecimal commissionRate = new BigDecimal("0.1000");

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "is_active")
    private Boolean active = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specification_fields", columnDefinition = "jsonb")
    private List<CategorySpecificationField> specificationFields = new ArrayList<>();

    @PrePersist
    void initializeSpecificationFields() {
        if (specificationFields == null) {
            specificationFields = new ArrayList<>();
        }
    }
}
