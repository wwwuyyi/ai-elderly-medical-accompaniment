package com.example.escort.module.admin.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashboardVO {
    private Long todayOrders;
    private Long pendingAuditCount;
    private Long totalUsers;
    private List<Map<String, Object>> orderTrend7Days;
}