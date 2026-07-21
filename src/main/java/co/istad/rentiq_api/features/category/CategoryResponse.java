package co.istad.rentiq_api.features.category;

public record CategoryResponse(
        Integer id,
        Integer parentId,
        String name,
        String slug,
        Double commissionRate,
        String iconUrl,
        Boolean active
) {
}