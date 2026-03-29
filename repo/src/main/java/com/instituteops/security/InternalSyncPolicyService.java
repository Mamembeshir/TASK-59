package com.instituteops.security;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InternalSyncPolicyService {

    static final String INTERNAL_SYNC_NAME = "LAN_OPTIONAL_SYNC";

    private final JdbcTemplate jdbcTemplate;

    public InternalSyncPolicyService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public InternalSyncPolicy currentPolicy() {
        List<InternalSyncPolicy> rows = jdbcTemplate.query(
            "SELECT enabled, lan_only FROM sync_config WHERE sync_name = ?",
            (rs, rowNum) -> new InternalSyncPolicy(rs.getBoolean("enabled"), rs.getBoolean("lan_only")),
            INTERNAL_SYNC_NAME
        );
        if (rows.isEmpty()) {
            return new InternalSyncPolicy(false, true);
        }
        return rows.get(0);
    }

    public boolean isTrustedLanAddress(String remoteAddress) {
        if (!StringUtils.hasText(remoteAddress)) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(remoteAddress.trim());
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
                return true;
            }
            if (address instanceof Inet6Address inet6) {
                byte first = inet6.getAddress()[0];
                return (first & (byte) 0xfe) == (byte) 0xfc;
            }
            return false;
        } catch (UnknownHostException ex) {
            return false;
        }
    }

    public record InternalSyncPolicy(boolean enabled, boolean lanOnly) {
    }
}
