package co.istad.rentiq_api.features.report.entity;

import co.istad.rentiq_api.features.report.enums.ReportStatus;
import co.istad.rentiq_api.features.report.enums.ReportType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reporter_id", nullable = false, length = 255)
    private String reporterId;

    @Column(name = "reported_user_id", length = 255)
    private String reportedUserId;

    @Column(name = "reported_item_id")
    private UUID reportedItemId;

    @Column(name = "reported_review_id")
    private UUID reportedReviewId;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 50)
    private ReportType reportType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<ReportAction> actions = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }

        if (status == null) {
            status = ReportStatus.OPEN;
        }
    }

    public void addAction(ReportAction action) {
        if (action == null) {
            return;
        }

        actions.add(action);
        action.setReport(this);
    }
}