package com.vn.datphong.entity;

import com.vn.datphong.enums.MediaTypeEnum;

import java.time.*;
import java.util.*;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "room_images", uniqueConstraints = {
    @UniqueConstraint(name = "uq_roomimage_0", columnNames = {"room_id", "asset_id"})
})
public class RoomImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false, length = 255)
    private String url;

    /** IMAGE hoặc VIDEO — cùng bảng room_images, phân biệt bằng cột này. */
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 10)
    @Builder.Default
    private MediaTypeEnum mediaType = MediaTypeEnum.IMAGE;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private MediaAsset asset;

    @Column(name = "cover", nullable = false)
    private Boolean cover;

    @Version
    private Long version;
}
