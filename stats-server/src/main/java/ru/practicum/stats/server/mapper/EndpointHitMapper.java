package ru.practicum.stats.server.mapper;

import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.server.model.EndpointHitEntity;

public class EndpointHitMapper {

    public static EndpointHitEntity toEntity(EndpointHit dto) {
        if (dto == null) return null;
        return EndpointHitEntity.builder()
                .id(dto.getId())
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();
    }

    public static EndpointHit toDto(EndpointHitEntity entity) {
        if (entity == null) return null;
        return EndpointHit.builder()
                .id(entity.getId())
                .app(entity.getApp())
                .uri(entity.getUri())
                .ip(entity.getIp())
                .timestamp(entity.getTimestamp())
                .build();
    }
}