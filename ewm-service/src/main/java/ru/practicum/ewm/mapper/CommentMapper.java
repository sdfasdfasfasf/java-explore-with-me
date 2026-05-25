package ru.practicum.ewm.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.dto.NewCommentDto;
import ru.practicum.ewm.model.Comment;

@Component
@RequiredArgsConstructor
public class CommentMapper {

    private final UserMapper userMapper;

    public Comment toComment(NewCommentDto dto) {
        if (dto == null) return null;
        Comment comment = new Comment();
        comment.setText(dto.getText());
        return comment;
    }

    public CommentDto toCommentDto(Comment comment) {
        if (comment == null) return null;
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(userMapper.toUserShortDto(comment.getAuthor()))
                .eventId(comment.getEvent().getId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}