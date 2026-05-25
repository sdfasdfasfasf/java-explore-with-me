package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.NewUserRequest;
import ru.practicum.ewm.dto.UserDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.UserMapper;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final UserMapper mapper;

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        log.debug("Getting users: ids={}, from={}, size={}", ids, from, size);
        PageRequest page = PageRequest.of(from / size, size);
        List<UserDto> result;
        if (ids == null || ids.isEmpty()) {
            result = userRepository.findAll(page).stream()
                    .map(mapper::toUserDto).collect(Collectors.toList());
        } else {
            result = userRepository.findAllById(ids).stream()
                    .map(mapper::toUserDto).collect(Collectors.toList());
        }
        log.debug("Found {} users", result.size());
        return result;
    }

    @Override
    @Transactional
    public UserDto registerUser(NewUserRequest request) {
        log.info("Registering new user: email={}, name={}", request.getEmail(), request.getName());
        try {
            User user = mapper.toUser(request);
            User saved = userRepository.save(user);
            log.debug("User registered with id={}", saved.getId());
            return mapper.toUserDto(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate email: {}", request.getEmail());
            throw new ConflictException("Email already exists: " + request.getEmail());
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Deleting user id={}", userId);
        if (!userRepository.existsById(userId)) {
            log.warn("User not found for deletion: id={}", userId);
            throw new NotFoundException("User with id=" + userId + " not found");
        }
        requestRepository.deleteByRequesterId(userId);
        eventRepository.deleteByInitiatorId(userId);
        userRepository.deleteById(userId);
        log.debug("User deleted: id={}", userId);
    }
}