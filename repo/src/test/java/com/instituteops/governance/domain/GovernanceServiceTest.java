package com.instituteops.governance.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituteops.security.UserIdentityService;
import com.instituteops.security.repo.UserRepository;
import com.instituteops.student.model.StudentProfileEntity;
import com.instituteops.student.model.StudentProfileLookupRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class GovernanceServiceTest {

    @Mock
    private StudentProfileLookupRepository studentProfileRepository;
    @Mock
    private DuplicateDetectionResultRepository duplicateDetectionResultRepository;
    @Mock
    private ChangeHistoryRepository changeHistoryRepository;
    @Mock
    private RecycleBinRepository recycleBinRepository;
    @Mock
    private BulkJobRepository bulkJobRepository;
    @Mock
    private ConsistencyIssueRepository consistencyIssueRepository;
    @Mock
    private GovernanceAuditService governanceAuditService;
    @Mock
    private UserIdentityService userIdentityService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private GovernanceService service;

    @BeforeEach
    void setUp() {
        service = new GovernanceService(
            studentProfileRepository,
            duplicateDetectionResultRepository,
            changeHistoryRepository,
            recycleBinRepository,
            bulkJobRepository,
            consistencyIssueRepository,
            governanceAuditService,
            userIdentityService,
            userRepository,
            new ObjectMapper(),
            jdbcTemplate
        );
    }

    @Test
    void detectStudentDuplicates_flagsExactAndFuzzyMatches() {
        StudentProfileEntity a = student(1L, "STU-1", "John", "Smith", LocalDate.parse("2010-01-01"));
        StudentProfileEntity b = student(2L, "STU-2", "John", "Smith", LocalDate.parse("2010-01-01"));
        StudentProfileEntity c = student(3L, "STU-3", "Jahn", "Smith", LocalDate.parse("2010-01-01"));
        when(studentProfileRepository.findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc()).thenReturn(List.of(a, b, c));
        when(duplicateDetectionResultRepository.findTop200ByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(duplicateDetectionResultRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.detectStudentDuplicates();

        org.mockito.Mockito.verify(duplicateDetectionResultRepository, org.mockito.Mockito.atLeast(2)).save(any());
    }

    @Test
    void importStudentsCsv_rejectsInvalidHeader() {
        assertThatThrownBy(() -> service.importStudentsCsv("students.csv", "bad,header\n1,2", false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported CSV header");
    }

    @Test
    void exportImport_roundTripMaintainsImportCompatibleSchema() {
        StudentProfileEntity source = student(10L, "STU-10", "Jane,Ann", "Doe", LocalDate.parse("2011-03-05"));
        source.setPreferredName("J\"A");
        source.setContactEmail("jane@example.com");
        source.setContactPhone("12345");
        source.setContactAddress("Line 1\nLine 2, City");
        source.setEmergencyContact("Parent, One");
        when(studentProfileRepository.findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc()).thenReturn(List.of(source), List.of());
        when(userIdentityService.resolveCurrentUserId()).thenReturn(Optional.of(1L));
        when(studentProfileRepository.findByStudentNoIgnoreCaseAndDeletedAtIsNull("STU-10")).thenReturn(Optional.empty());
        when(studentProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bulkJobRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(duplicateDetectionResultRepository.findTop200ByOrderByCreatedAtDesc()).thenReturn(List.of());

        String csv = service.exportStudentsCsv();
        GovernanceService.CsvImportResult result = service.importStudentsCsv("roundtrip.csv", csv, false);

        assertThat(result.createdRows()).isEqualTo(1);
        ArgumentCaptor<StudentProfileEntity> captor = ArgumentCaptor.forClass(StudentProfileEntity.class);
        verify(studentProfileRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        StudentProfileEntity imported = captor.getAllValues().getLast();
        assertThat(imported.getFirstName()).isEqualTo("Jane,Ann");
        assertThat(imported.getContactAddress()).contains("Line 2, City");
        assertThat(imported.getEmergencyContact()).isEqualTo("Parent, One");
    }

    @Test
    void importStudentsCsv_supportsQuotedCommas() {
        String csv = "student_no,first_name,last_name,preferred_name,date_of_birth,contact_email,contact_phone,contact_address,emergency_contact\n"
            + "STU-22,\"Jane,Ann\",Doe,,2011-03-05,jane@example.com,12345,\"Street, Block A\",\"Parent, One\"";

        when(userIdentityService.resolveCurrentUserId()).thenReturn(Optional.of(1L));
        when(studentProfileRepository.findByStudentNoIgnoreCaseAndDeletedAtIsNull("STU-22")).thenReturn(Optional.empty());
        when(studentProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bulkJobRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(studentProfileRepository.findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc()).thenReturn(List.of());
        when(duplicateDetectionResultRepository.findTop200ByOrderByCreatedAtDesc()).thenReturn(List.of());

        service.importStudentsCsv("quoted.csv", csv, false);

        ArgumentCaptor<StudentProfileEntity> captor = ArgumentCaptor.forClass(StudentProfileEntity.class);
        verify(studentProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getFirstName()).isEqualTo("Jane,Ann");
        assertThat(captor.getValue().getContactAddress()).isEqualTo("Street, Block A");
    }

    @Test
    void importStudentsCsv_createsStudentOnHappyPath() {
        String csv = "student_no,first_name,last_name,preferred_name,date_of_birth,contact_email,contact_phone,contact_address,emergency_contact\n"
            + "STU-10,Jane,Doe,,2011-03-05,jane@example.com,123456,Main St,Parent";

        when(userIdentityService.resolveCurrentUserId()).thenReturn(Optional.of(1L));
        when(studentProfileRepository.findByStudentNoIgnoreCaseAndDeletedAtIsNull("STU-10")).thenReturn(Optional.empty());
        when(studentProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bulkJobRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(studentProfileRepository.findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc()).thenReturn(List.of());
        when(duplicateDetectionResultRepository.findTop200ByOrderByCreatedAtDesc()).thenReturn(List.of());

        GovernanceService.CsvImportResult result = service.importStudentsCsv("students.csv", csv, false);

        assertThat(result.createdRows()).isEqualTo(1);
        assertThat(result.failedRows()).isEqualTo(0);
        assertThat(result.status()).isEqualTo("COMPLETED");
    }

    private static StudentProfileEntity student(Long id, String studentNo, String firstName, String lastName, LocalDate dob) {
        StudentProfileEntity entity = new StudentProfileEntity();
        setId(entity, id);
        entity.setStudentNo(studentNo);
        entity.setFirstName(firstName);
        entity.setLastName(lastName);
        entity.setDateOfBirth(dob);
        entity.setStatus("ACTIVE");
        return entity;
    }

    private static void setId(StudentProfileEntity entity, Long id) {
        try {
            java.lang.reflect.Field f = StudentProfileEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
