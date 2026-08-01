package com.qaassistant.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_case_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private GeneratedTestCase testCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acceptance_criteria_id", nullable = false)
    private AcceptanceCriteria acceptanceCriteria;
}
