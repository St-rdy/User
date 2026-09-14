package com.stardy.user.dto;

import java.util.List;
import java.util.stream.IntStream;

public record ProfileImageListResponseDto(List<ProfileImageResponseDto> images) {

    public static ProfileImageListResponseDto from(List<String> imageUrls) {
        List<ProfileImageResponseDto> images = IntStream.range(0, imageUrls.size())
                .mapToObj(index -> new ProfileImageResponseDto(index + 1L, imageUrls.get(index)))
                .toList();

        return new ProfileImageListResponseDto(images);
    }

    public record ProfileImageResponseDto(Long id, String url) {
    }
}
