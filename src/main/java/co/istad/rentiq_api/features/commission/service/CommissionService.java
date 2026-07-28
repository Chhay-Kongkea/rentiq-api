package co.istad.rentiq_api.features.commission.service;

import co.istad.rentiq_api.features.commission.dto.request.UpdateCommissionRateRequest;
import co.istad.rentiq_api.features.commission.dto.response.CommissionRateResponse;
import co.istad.rentiq_api.features.commission.dto.response.CommissionReportResponse;

import java.time.LocalDate;
import java.util.List;

public interface CommissionService {

    List<CommissionRateResponse> listCommissionRates();

    CommissionRateResponse updateCommissionRate(Integer categoryId, UpdateCommissionRateRequest request, String adminId);

    CommissionReportResponse getCommissionReport(LocalDate from, LocalDate to);
}
