package com.instituteops.student.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "classes")
public class CourseClassRefEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_code", nullable = false)
    private String classCode;

    @Column(name = "class_name", nullable = false)
    private String className;

    public Long getId() {
        return id;
    }

    public String getClassCode() {
        return classCode;
    }

    public String getClassName() {
        return className;
    }
}
