package ru.nand.postservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("comments")
public class Comment {

    @Id
    private Long id;

    @Column("post_id")
    private Long postId;

    @Column("author_id")
    private Long authorId;

    @Column("text")
    private String text;

    @Column("date_of_creation")
    private LocalDateTime dateOfCreation;

    @Column("date_of_editing")
    private LocalDateTime dateOfEditing;

}
