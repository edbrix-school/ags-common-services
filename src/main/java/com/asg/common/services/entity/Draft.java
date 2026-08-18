package com.asg.common.services.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "GLOBAL_DOC_DRAFT",
        indexes = {
                @Index(name = "IDX_DOC_DRAFT_UNQ", columnList = "DOC_ID, COMPANY_POID, USER_POID", unique = true)
        }
)
@Getter @Setter @NoArgsConstructor
public class Draft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DRAFT_ID")
    private Long id;

    @Column(name = "DOC_ID", length = 100, nullable = false)
    private String docId;

    @Column(name = "COMPANY_POID", nullable = false)
    private Long companyPoid;

    @Column(name = "USER_POID", nullable = false)
    private Long userPoid;

    @Lob
    @Column(name = "DRAFT_DATA", columnDefinition = "TEXT")
    private String draftData;

    @CreationTimestamp
    @Column(name = "CREATED_DATE", updatable = false)
    private OffsetDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "LAST_MODIFIED_DATE")
    private OffsetDateTime lastModifiedDate;
}
