package co.istad.rentiq_api.features.category;

import co.istad.rentiq_api.config.auditing.BasedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "parent_id")
    private Integer parentId;


    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String slug;

    @Column(name = "commission_rate")
    private Double commissionRate = 0.1000;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "is_active")
    private Boolean active = true;
}