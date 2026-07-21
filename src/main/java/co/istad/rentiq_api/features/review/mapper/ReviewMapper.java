package co.istad.rentiq_api.features.review.mapper;

import co.istad.rentiq_api.features.review.dto.response.ReviewImageResponse;
import co.istad.rentiq_api.features.review.dto.response.ReviewResponse;
import co.istad.rentiq_api.features.review.entity.Review;
import co.istad.rentiq_api.features.review.entity.ReviewImage;
import co.istad.rentiq_api.features.review.repository.ReviewImageRepository;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class ReviewMapper {

    @Autowired
    protected ReviewImageRepository reviewImageRepository;

    public ReviewResponse toResponse(Review review) {
        var images = reviewImageRepository.findByReviewIdOrderBySortOrderAsc(review.getId())
                .stream()
                .map(this::toImageResponse)
                .toList();

        return toResponse(review, images);
    }

    // MapStruct generates this: maps Review's fields directly, plus takes
    // the already-computed images list as a separate source parameter.
    protected abstract ReviewResponse toResponse(Review review, List<ReviewImageResponse> images);

    public abstract ReviewImageResponse toImageResponse(ReviewImage image);
}