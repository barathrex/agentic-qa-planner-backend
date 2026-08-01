package com.qaassistant.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "acceptance_criteria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcceptanceCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private QaPlan plan;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "criteria_index", nullable = false)
    private Integer criteriaIndex;
}
