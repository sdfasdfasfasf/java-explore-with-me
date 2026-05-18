package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.NewUserRequest;
import ru.practicum.ewm.dto.UserDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.UserMapper;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final UserMapper mapper;

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        if (ids == null || ids.isEmpty()) {
            return userRepository.findAll(page).stream().map(mapper::toUserDto).collect(Collectors.toList());
        } else {
            return userRepository.findAllById(ids).stream().map(mapper::toUserDto).collect(Collectors.toList());
        }
    }

    @Override
    @Transactional
    public UserDto registerUser(NewUserRequest request) {
        User user = mapper.toUser(request);
        return mapper.toUserDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " not found");
        }
        // Удаляем все запросы пользователя
        requestRepository.deleteByRequesterId(userId);
        // Удаляем все события пользователя
        eventRepository.deleteByInitiatorId(userId);
        // Удаляем пользователя
        userRepository.deleteById(userId);
        log.info("Deleted user with id={}", userId);
    }
}