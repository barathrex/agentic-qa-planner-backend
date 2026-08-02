package com.qaassistant.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "qa_plan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QaPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "developer_name")
    private String developerName;

    @Column
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String requirement;

    @Column(name = "implementation_summary", nullable = false, columnDefinition = "TEXT")
    private String implementationSummary;

    @Column(name = "user_flows", columnDefinition = "TEXT")
    private String userFlows;

    @Column(name = "retrieved_guidance", columnDefinition = "TEXT")
    private String retrievedGuidance;

    @Column(columnDefinition = "TEXT")
    private String assumptions;

    @Column(name = "coverage_percentage")
    private Double coveragePercentage;

    @Column(name = "current_version")
    @Builder.Default
    private Integer currentVersion = 1;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AcceptanceCriteria> acceptanceCriteria = new ArrayList<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GeneratedTestCase> testCases = new ArrayList<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QaPlanVersion> versions = new ArrayList<>();
}
