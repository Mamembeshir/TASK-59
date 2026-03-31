package com.instituteops.grades.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Validated
@RequestMapping
public class GradeEntryController {

    private final GradeEntryService gradeEntryService;
    private final GradeAuthorizationService gradeAuthorizationService;

    public GradeEntryController(GradeEntryService gradeEntryService, GradeAuthorizationService gradeAuthorizationService) {
        this.gradeEntryService = gradeEntryService;
        this.gradeAuthorizationService = gradeAuthorizationService;
    }

    @GetMapping("/instructor/grades")
    public String gradeLedgerPage(Model model) {
        model.addAttribute("entries", gradeEntryService.latestLedger());
        return "grade-entry";
    }

    @PostMapping("/instructor/grades")
    public String appendEntry(Authentication authentication, @ModelAttribute @Valid GradeFormRequest form) {
        gradeAuthorizationService.assertCanManageGradeEntry(authentication, form.studentId(), form.classId());
        gradeEntryService.appendLedgerEntry(toRequest(form));
        return "redirect:/instructor/grades";
    }

    @ResponseBody
    @PostMapping("/api/grades/preview")
    public GradeEntryService.GradeImpact preview(Authentication authentication, @RequestBody @Valid GradeApiRequest apiRequest) {
        gradeAuthorizationService.assertCanManageGradeEntry(authentication, apiRequest.studentId(), apiRequest.classId());
        return gradeEntryService.preview(toRequest(apiRequest));
    }

    @ResponseBody
    @PostMapping("/api/grades/ledger")
    public ResponseEntity<GradeEntryService.GradeImpact> appendFromApi(Authentication authentication, @RequestBody @Valid GradeApiRequest apiRequest) {
        gradeAuthorizationService.assertCanManageGradeEntry(authentication, apiRequest.studentId(), apiRequest.classId());
        gradeEntryService.appendLedgerEntry(toRequest(apiRequest));
        return ResponseEntity.ok(gradeEntryService.preview(toRequest(apiRequest)));
    }

    @ResponseBody
    @PostMapping("/api/grades/recompute")
    public ResponseEntity<GradeEntryService.RecalculationResult> recompute(Authentication authentication, @RequestBody @Valid RecomputeApiRequest request) {
        gradeAuthorizationService.assertCanManageGradeEntry(authentication, request.studentId(), request.classId());
        return ResponseEntity.ok(gradeEntryService.recomputeStudentClass(request.studentId(), request.classId(), request.reasonCode()));
    }

    private static GradeEntryService.GradeEntryRequest toRequest(GradeFormRequest form) {
        return new GradeEntryService.GradeEntryRequest(
            form.studentId(),
            form.classId(),
            form.classSessionId(),
            form.enrollmentId(),
            form.assessmentKey(),
            form.rawScore(),
            form.maxScore(),
            form.operationType(),
            form.reasonCode(),
            form.previousEntryId()
        );
    }

    private static GradeEntryService.GradeEntryRequest toRequest(GradeApiRequest api) {
        return new GradeEntryService.GradeEntryRequest(
            api.studentId(),
            api.classId(),
            api.classSessionId(),
            api.enrollmentId(),
            api.assessmentKey(),
            api.rawScore(),
            api.maxScore(),
            api.operationType(),
            api.reasonCode(),
            api.previousEntryId()
        );
    }

    public record GradeFormRequest(
        @NotNull Long studentId,
        @NotNull Long classId,
        Long classSessionId,
        Long enrollmentId,
        @NotBlank String assessmentKey,
        @NotNull @DecimalMin("0.0") BigDecimal rawScore,
        @NotNull @DecimalMin("0.001") BigDecimal maxScore,
        String operationType,
        String reasonCode,
        Long previousEntryId
    ) {
    }

    public record GradeApiRequest(
        @NotNull Long studentId,
        @NotNull Long classId,
        Long classSessionId,
        Long enrollmentId,
        @NotBlank String assessmentKey,
        @NotNull @DecimalMin("0.0") BigDecimal rawScore,
        @NotNull @DecimalMin("0.001") BigDecimal maxScore,
        String operationType,
        String reasonCode,
        Long previousEntryId
    ) {
    }

    public record RecomputeApiRequest(
        @NotNull Long studentId,
        @NotNull Long classId,
        @NotBlank String reasonCode
    ) {
    }
}
