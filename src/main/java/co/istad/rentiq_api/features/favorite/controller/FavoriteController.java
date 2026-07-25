package co.istad.rentiq_api.features.favorite.controller;

import co.istad.rentiq_api.features.favorite.dto.response.FavoriteResponse;
import co.istad.rentiq_api.features.favorite.service.FavoriteService;
import co.istad.rentiq_api.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public Page<FavoriteResponse> getFavorites(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = AuthUtils.extractUserId();
        return favoriteService.getFavorites(userId, pageable);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{itemId}")
    public FavoriteResponse addFavorite(@PathVariable UUID itemId) {
        String userId = AuthUtils.extractUserId();
        return favoriteService.addFavorite(userId, itemId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{itemId}")
    public void removeFavorite(@PathVariable UUID itemId) {
        String userId = AuthUtils.extractUserId();
        favoriteService.removeFavorite(userId, itemId);
    }
}