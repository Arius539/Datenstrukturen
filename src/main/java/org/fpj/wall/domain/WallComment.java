package org.fpj.wall.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@AllArgsConstructor
public class WallComment {

    private Long id;
    private String content;
    private Long wallOwnerId;
    private Long authorId;
    private Timestamp createdAt;

    public WallComment(final String content, final Long wallOwnerId,
                       final Long authorId, final Timestamp createdAt){
        this(null, content, wallOwnerId, authorId, createdAt);
    }



}
