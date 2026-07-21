package co.istad.rentiq_api.features.report.mapper;

import co.istad.rentiq_api.features.report.dto.request.CreateReportActionRequest;
import co.istad.rentiq_api.features.report.dto.response.ReportActionResponse;
import co.istad.rentiq_api.features.report.entity.Report;
import co.istad.rentiq_api.features.report.entity.ReportAction;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ReportActionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "report", source = "report")
    @Mapping(target = "adminId", source = "authenticatedAdminId")
    @Mapping(target = "createdAt", ignore = true)
    ReportAction toEntity(CreateReportActionRequest request, Report report, String authenticatedAdminId);

    @Mapping(target = "reportId", source = "report.id")
    ReportActionResponse toResponse(
            ReportAction action
    );
}