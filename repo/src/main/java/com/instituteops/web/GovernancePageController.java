package com.instituteops.web;

import com.instituteops.governance.domain.GovernanceService;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/governance")
public class GovernancePageController {

    private final GovernanceService governanceService;

    public GovernancePageController(GovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    @GetMapping
    public String page(@RequestParam(required = false) Long historyStudentId, Authentication authentication, Model model) {
        model.addAttribute("duplicates", governanceService.latestDuplicateCandidates());
        model.addAttribute("consistencyIssues", governanceService.latestConsistencyIssues());
        model.addAttribute("recycleBin", governanceService.recycleBin());
        model.addAttribute("historyStudentId", historyStudentId);
        if (historyStudentId != null) {
            governanceService.assertStudentCanAccessOwnHistory(authentication, historyStudentId);
            model.addAttribute("changeHistory", governanceService.changeHistory("STUDENT", historyStudentId));
        } else {
            model.addAttribute("changeHistory", java.util.List.of());
        }
        return "governance-dashboard";
    }

    @PostMapping("/import")
    public String importCsv(
        @RequestParam("file") MultipartFile file,
        @RequestParam(defaultValue = "false") boolean allowUpdate,
        RedirectAttributes redirect
    ) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("CSV file is required");
            }
            String csv = new String(file.getBytes(), StandardCharsets.UTF_8);
            GovernanceService.CsvImportResult result = governanceService.importStudentsCsv(file.getOriginalFilename(), csv, allowUpdate);
            redirect.addFlashAttribute(
                "successMessage",
                "Import completed: " + result.createdRows() + " created, " + result.updatedRows() + " updated, " + result.failedRows() + " failed"
            );
        } catch (Exception ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/governance";
    }

    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportCsv() {
        byte[] csv = governanceService.exportStudentsCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=students-export.csv")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .contentLength(csv.length)
            .body(new ByteArrayResource(csv));
    }

    @PostMapping("/duplicates/scan")
    public String scanDuplicates(RedirectAttributes redirect) {
        int count = governanceService.detectStudentDuplicates().size();
        redirect.addFlashAttribute("successMessage", "Duplicate scan completed with " + count + " candidate pairs");
        return "redirect:/governance";
    }

    @PostMapping("/consistency/scan")
    public String scanConsistency(RedirectAttributes redirect) {
        int count = governanceService.runConsistencyScan().size();
        redirect.addFlashAttribute("successMessage", "Consistency scan completed with " + count + " findings");
        return "redirect:/governance";
    }

    @PostMapping("/duplicates/{id}/review")
    public String reviewDuplicate(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            governanceService.markDuplicateReviewed(id);
            redirect.addFlashAttribute("successMessage", "Duplicate candidate marked as reviewed");
        } catch (Exception ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/governance";
    }

    @PostMapping("/recycle/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            governanceService.restoreFromRecycleBin(id);
            redirect.addFlashAttribute("successMessage", "Recycle-bin record restored");
        } catch (Exception ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/governance";
    }

    @PostMapping("/recycle/purge")
    public String purge(RedirectAttributes redirect) {
        int purged = governanceService.purgeExpiredRecycleBin();
        redirect.addFlashAttribute("successMessage", "Purged " + purged + " expired recycle-bin records");
        return "redirect:/governance";
    }
}
