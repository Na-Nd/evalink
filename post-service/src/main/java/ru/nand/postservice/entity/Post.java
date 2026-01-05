package ru.nand.postservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("posts")
public class Post {

    @Id
    private Long id;

    @Column("author_id")
    private Long authorId;

    private String text;

    // Хэштеги как JSONB
    private List<String> tags;

    @Column("date_of_publication")
    private OffsetDateTime dateOfPublication;

    @Column("date_of_update")
    private OffsetDateTime dateOfUpdate;
}
