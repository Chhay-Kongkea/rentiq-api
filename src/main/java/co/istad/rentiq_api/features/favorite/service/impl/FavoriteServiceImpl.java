package co.istad.rentiq_api.features.favorite.service.impl;

import co.istad.rentiq_api.features.favorite.dto.response.FavoriteResponse;
import co.istad.rentiq_api.features.favorite.entity.Favorite;
import co.istad.rentiq_api.features.favorite.entity.FavoriteId;
import co.istad.rentiq_api.features.favorite.exception.FavoriteAlreadyExistsException;
import co.istad.rentiq_api.features.favorite.exception.FavoriteItemNotFoundException;
import co.istad.rentiq_api.features.favorite.exception.FavoriteNotFoundException;
import co.istad.rentiq_api.features.favorite.mapper.FavoriteMapper;
import co.istad.rentiq_api.features.favorite.repository.FavoriteRepository;
import co.istad.rentiq_api.features.favorite.service.FavoriteService;
import co.istad.rentiq_api.features.item.entity.Item;
// ASSUMPTION: adjust package if your ItemRepository lives elsewhere.
import co.istad.rentiq_api.features.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ItemRepository itemRepository;
    private final FavoriteMapper favoriteMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<FavoriteResponse> getFavorites(String userId, Pageable pageable) {
        return favoriteRepository.findByIdUserId(userId, pageable)
                .map(favoriteMapper::toResponse);
    }

    @Override
    @Transactional
    public FavoriteResponse addFavorite(String userId, UUID itemId) {
        if (favoriteRepository.existsByIdUserIdAndIdItemId(userId, itemId)) {
            throw new FavoriteAlreadyExistsException(itemId);
        }

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new FavoriteItemNotFoundException(itemId));

        Favorite favorite = Favorite.builder()
                .id(new FavoriteId(userId, itemId))
                .item(item)
                .createdAt(Instant.now())
                .build();

        favoriteRepository.save(favorite);
        favoriteRepository.incrementFavoriteCount(itemId);

        return favoriteMapper.toResponse(favorite);
    }

    @Override
    @Transactional
    public void removeFavorite(String userId, UUID itemId) {
        if (!favoriteRepository.existsByIdUserIdAndIdItemId(userId, itemId)) {
            throw new FavoriteNotFoundException(itemId);
        }
        favoriteRepository.deleteByIdUserIdAndIdItemId(userId, itemId);
        favoriteRepository.decrementFavoriteCount(itemId);
    }
}