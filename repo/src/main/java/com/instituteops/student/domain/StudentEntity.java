package com.instituteops.student.domain;

import com.instituteops.shared.crypto.AesStringAttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "students")
public class StudentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_no", nullable = false, unique = true, length = 64)
    private String studentNo;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Convert(converter = AesStringAttributeConverter.class)
    @Column(name = "contact_email_encrypted")
    private String contactEmail;

    @Convert(converter = AesStringAttributeConverter.class)
    @Column(name = "contact_phone_encrypted")
    private String contactPhone;

    @Convert(converter = AesStringAttributeConverter.class)
    @Column(name = "contact_address_encrypted")
    private String contactAddress;

    @Convert(converter = AesStringAttributeConverter.class)
    @Column(name = "emergency_contact_encrypted")
    private String emergencyContact;
}
