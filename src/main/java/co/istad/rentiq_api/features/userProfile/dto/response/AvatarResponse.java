package co.istad.rentiq_api.features.userProfile.dto.response;


import lombok.Builder;

@Builder
public record AvatarResponse(String avatarUrl) {}