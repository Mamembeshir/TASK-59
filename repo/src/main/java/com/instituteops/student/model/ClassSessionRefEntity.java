package com.instituteops.student.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "class_sessions")
public class ClassSessionRefEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "session_no", nullable = false)
    private Integer sessionNo;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    public Long getId() {
        return id;
    }

    public Long getClassId() {
        return classId;
    }

    public Integer getSessionNo() {
        return sessionNo;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }
}
