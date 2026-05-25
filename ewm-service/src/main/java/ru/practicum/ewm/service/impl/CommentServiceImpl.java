package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.dto.NewCommentDto;
import ru.practicum.ewm.dto.UpdateCommentDto;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ForbiddenException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.service.CommentService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto dto) {
        log.info("Adding comment to event {} by user {}", eventId, userId);
        User author = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));
        if (event.getState() != EventState.PUBLISHED) {
            throw new BadRequestException("Cannot comment on unpublished event");
        }
        Comment comment = commentMapper.toComment(dto);
        comment.setAuthor(author);
        comment.setEvent(event);
        return commentMapper.toCommentDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto dto) {
        log.info("Updating comment {} by user {}", commentId, userId);
        Comment comment = getCommentEntity(commentId);
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Only author can edit comment");
        }
        comment.setText(dto.getText());
        return commentMapper.toCommentDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        log.info("Deleting comment {} by user {}", commentId, userId);
        Comment comment = getCommentEntity(commentId);
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Only author can delete comment");
        }
        commentRepository.delete(comment);
    }

    @Override
    public List<CommentDto> getCommentsByEvent(Long eventId, int from, int size) {
        log.debug("Getting comments for event {}, from={}, size={}", eventId, from, size);
        PageRequest page = PageRequest.of(from / size, size);
        return commentRepository.findByEventId(eventId, page).stream().map(commentMapper::toCommentDto).collect(Collectors.toList());
    }

    @Override
    public CommentDto getCommentById(Long commentId) {
        log.debug("Getting comment by id={}", commentId);
        return commentMapper.toCommentDto(getCommentEntity(commentId));
    }

    @Override
    @Transactional
    public void adminDeleteComment(Long commentId) {
        log.info("Admin deleting comment {}", commentId);
        Comment comment = getCommentEntity(commentId);
        commentRepository.delete(comment);
    }

    private Comment getCommentEntity(Long id) {
        return commentRepository.findById(id).orElseThrow(() -> new NotFoundException("Comment not found with id=" + id));
    }
}