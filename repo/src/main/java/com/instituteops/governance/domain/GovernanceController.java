package com.instituteops.governance.domain;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Validated
@RequestMapping("/api/governance")
public class GovernanceController {

    private final GovernanceService governanceService;

    public GovernanceController(GovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    @ResponseBody
    @GetMapping(value = "/students/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportStudentsCsv() {
        String csv = governanceService.exportStudentsCsv();
        return ResponseEntity.ok()
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=students-export.csv")
            .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @ResponseBody
    @PostMapping("/students/import")
    public GovernanceService.CsvImportResult importStudentsCsv(
        @RequestParam(defaultValue = "students-import.csv") String fileName,
        @RequestParam(defaultValue = "false") boolean allowUpdate,
        @RequestBody String csvPayload
    ) {
        return governanceService.importStudentsCsv(fileName, csvPayload, allowUpdate);
    }

    @ResponseBody
    @PostMapping("/students/duplicates/scan")
    public List<GovernanceService.DuplicateCandidate> scanDuplicates() {
        return governanceService.detectStudentDuplicates();
    }

    @ResponseBody
    @PostMapping("/consistency/scan")
    public List<GovernanceService.ConsistencyIssueView> scanConsistency() {
        return governanceService.runConsistencyScan();
    }

    @ResponseBody
    @GetMapping("/consistency/issues")
    public List<GovernanceService.ConsistencyIssueView> consistencyIssues() {
        return governanceService.latestConsistencyIssues();
    }

    @ResponseBody
    @GetMapping("/students/{studentId}/history")
    public List<GovernanceService.ChangeEntry> studentHistory(@PathVariable Long studentId, Authentication authentication) {
        governanceService.assertStudentCanAccessOwnHistory(authentication, studentId);
        return governanceService.changeHistory("STUDENT", studentId);
    }

    @ResponseBody
    @GetMapping("/recycle-bin")
    public List<GovernanceService.RecycleBinView> recycleBin() {
        return governanceService.recycleBin();
    }

    @ResponseBody
    @PostMapping("/recycle-bin/{recycleId}/restore")
    public ResponseEntity<Void> restore(@PathVariable Long recycleId) {
        governanceService.restoreFromRecycleBin(recycleId);
        return ResponseEntity.ok().build();
    }

    @ResponseBody
    @PostMapping("/recycle-bin/purge-expired")
    public ResponseEntity<Integer> purgeExpired() {
        return ResponseEntity.ok(governanceService.purgeExpiredRecycleBin());
    }
}
