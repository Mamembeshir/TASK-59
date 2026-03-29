package com.instituteops.web;

import com.instituteops.recommender.domain.RecommenderService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    private final RecommenderService recommenderService;
    private final JdbcTemplate jdbcTemplate;

    public HomeController(RecommenderService recommenderService, JdbcTemplate jdbcTemplate) {
        this.recommenderService = recommenderService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("username", authentication == null ? "guest" : authentication.getName());
        model.addAttribute("recommendations", recommenderService.recommendationsForCurrentUser(5));
        model.addAttribute("activeStudents", queryLong("SELECT COUNT(*) FROM students WHERE deleted_at IS NULL AND status = 'ACTIVE'"));
        model.addAttribute(
            "pendingGroupBuyOrders",
            queryLong("SELECT COUNT(*) FROM group_buy_orders WHERE order_status IN ('PENDING_GROUP','INVENTORY_LOCKED')")
        );
        model.addAttribute("openSystemAlerts", queryLong("SELECT COUNT(*) FROM system_alerts WHERE acknowledged = FALSE"));
        model.addAttribute(
            "todayRevenue",
            queryLong("SELECT COALESCE(SUM(amount), 0) FROM payment_transactions WHERE DATE(recorded_at) = CURRENT_DATE AND voided = FALSE")
        );
        return "dashboard";
    }

    @GetMapping("/admin")
    public String admin() {
        return "redirect:/governance";
    }

    @GetMapping("/registrar")
    public String registrar() {
        return "redirect:/governance";
    }

    @GetMapping("/instructor")
    public String instructor() {
        return "role-page";
    }

    @ResponseBody
    @RequestMapping("/api/internal/ping")
    public String internalPing() {
        return "pong";
    }

    private long queryLong(String sql) {
        Number value = jdbcTemplate.queryForObject(sql, Number.class);
        return value == null ? 0L : value.longValue();
    }
}
