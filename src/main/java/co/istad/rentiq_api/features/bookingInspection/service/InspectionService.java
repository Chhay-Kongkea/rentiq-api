package co.istad.rentiq_api.features.bookingInspection.service;

import co.istad.rentiq_api.features.bookingInspection.dto.request.AddInspectionImagesRequest;
import co.istad.rentiq_api.features.bookingInspection.dto.request.UpsertInspectionRequest;
import co.istad.rentiq_api.features.bookingInspection.dto.response.InspectionImageResponse;
import co.istad.rentiq_api.features.bookingInspection.dto.response.InspectionResponse;

import java.util.List;
import java.util.UUID;

public interface InspectionService {

    InspectionResponse createInspection(String vendorId, UUID bookingId, UpsertInspectionRequest request);

    InspectionResponse getInspection(String userId, UUID bookingId);

    InspectionResponse updateInspection(String vendorId, UUID bookingId, UpsertInspectionRequest request);

    List<InspectionImageResponse> addImages(String vendorId, UUID bookingId, AddInspectionImagesRequest request);

    List<InspectionImageResponse> listImages(String userId, UUID bookingId);

    void deleteImage(String vendorId, UUID bookingId, UUID imageId);
}