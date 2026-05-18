package ru.practicum.ewm.controller.private_api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.ParticipationRequestDto;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.service.ParticipationRequestService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
public class PrivateRequestController {
    private final ParticipationRequestService requestService;

    @GetMapping
    public List<ParticipationRequestDto> getUserRequests(@PathVariable Long userId) {
        return requestService.getUserRequests(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addParticipationRequest(@PathVariable Long userId,
                                                           @RequestParam(required = true) Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("eventId must not be null");
        }
        return requestService.addParticipationRequest(userId, eventId);
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId,
                                                 @PathVariable Long requestId) {
        return requestService.cancelRequest(userId, requestId);
    }
}