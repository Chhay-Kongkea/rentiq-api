package co.istad.rentiq_api.features.report.mapper;

import co.istad.rentiq_api.features.report.dto.request.CreateReportRequest;
import co.istad.rentiq_api.features.report.dto.response.ReportResponse;
import co.istad.rentiq_api.features.report.entity.Report;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ReportMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reporterId", source = "authenticatedUserId")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "actions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Report toEntity(CreateReportRequest request, String authenticatedUserId);

    @Mapping(target = "actionCount",
            expression = "java(getActionCount(report))"
    )
    ReportResponse toResponse(Report report);

    default int getActionCount(Report report) {
        if (report.getActions() == null) {
            return 0;
        }

        return report.getActions().size();
    }
}