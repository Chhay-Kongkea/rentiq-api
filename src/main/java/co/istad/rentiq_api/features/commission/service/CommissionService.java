package co.istad.rentiq_api.features.commission.service;

import co.istad.rentiq_api.features.commission.dto.request.UpdateCommissionRateRequest;
import co.istad.rentiq_api.features.commission.dto.response.CommissionRateResponse;
import co.istad.rentiq_api.features.commission.dto.response.CommissionReportResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CommissionService {

    List<CommissionRateResponse> listCommissionRates();

    CommissionRateResponse updateCommissionRate(UUID categoryId, UpdateCommissionRateRequest request, String adminId);

    CommissionReportResponse getCommissionReport(LocalDate from, LocalDate to);
}
