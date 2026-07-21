package co.istad.rentiq_api.features.favorite.service;

import co.istad.rentiq_api.features.favorite.dto.response.FavoriteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FavoriteService {

    Page<FavoriteResponse> getFavorites(String userId, Pageable pageable);

    FavoriteResponse addFavorite(String userId, UUID itemId);

    void removeFavorite(String userId, UUID itemId);
}