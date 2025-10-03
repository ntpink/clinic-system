package clinic_dao;

import clinic_ui.Connect; // dùng Connect.ConnectDB() sẵn có của bạn

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng bills.
 * Cột mặc định đang dùng:
 *   - bill_id (INT PK, AI)
 *   - patient_id (INT, FK -> patients.patient_id)
 *   - total_amount (DECIMAL/DOUBLE)
 *   - payment_status (VARCHAR)  -- "UNPAID"/"PAID"
 *   - created_at (DATETIME/TIMESTAMP)
 *
 * Nếu DB của bạn dùng tên cột khác, sửa lại các SQL bên dưới cho trùng.
 */
public class BillDAO {

    private final Connection conn;

    /** Lấy connection tự động qua Connect.ConnectDB() */
    public BillDAO() throws SQLException {
        this.conn = Connect.ConnectDB();
    }

    /** Truyền connection sẵn có (tái sử dụng) */
    public BillDAO(Connection externalConn) {
        this.conn = externalConn;
    }

    /* ===================== CRUD cơ bản ===================== */

    public List<Bill> getAll() throws SQLException {
        String sql = "SELECT bill_id, patient_id, total_amount, payment_status, created_at " +
                     "FROM bills ORDER BY bill_id DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Bill> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        }
    }

    public Bill getById(int billId) throws SQLException {
        String sql = "SELECT bill_id, patient_id, total_amount, payment_status, created_at " +
                     "FROM bills WHERE bill_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    /** Insert và trả về ID tự sinh (bill_id) */
    public int insert(Bill b) throws SQLException {
        String sql = "INSERT INTO bills (patient_id, total_amount, payment_status, created_at) " +
                     "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, b.getPatientId());
            ps.setDouble(2, b.getTotalAmount());
            ps.setString(3, b.getPaymentStatus());
            ps.setTimestamp(4, b.getCreatedAt());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    /** Update theo bill_id */
    public boolean update(Bill b) throws SQLException {
        String sql = "UPDATE bills SET patient_id=?, total_amount=?, payment_status=?, created_at=? " +
                     "WHERE bill_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, b.getPatientId());
            ps.setDouble(2, b.getTotalAmount());
            ps.setString(3, b.getPaymentStatus());
            ps.setTimestamp(4, b.getCreatedAt());
            ps.setInt(5, b.getBillId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int billId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM bills WHERE bill_id=?")) {
            ps.setInt(1, billId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Đánh dấu đã thanh toán */
    public boolean markPaid(int billId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE bills SET payment_status='PAID' WHERE bill_id=?")) {
            ps.setInt(1, billId);
            return ps.executeUpdate() > 0;
        }
    }

    /* ===================== Dữ liệu cho UI ===================== */

    /**
     * Dùng cho bảng ở màn Bill: lấy kèm tên bệnh nhân.
     * Trả về từng hàng dạng: [bill_id, patient_name_or_id, created_at_string, total, status]
     */
    public List<Object[]> listForTable() throws SQLException {
        String sql =
            "SELECT b.bill_id, b.patient_id, b.created_at, b.total_amount, b.payment_status, " +
            "       p.full_name AS patient_name " +
            "FROM bills b " +
            "LEFT JOIN patients p ON p.patient_id = b.patient_id " +
            "ORDER BY b.bill_id DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Object[]> rows = new ArrayList<>();
            while (rs.next()) {
                int billId = rs.getInt("bill_id");
                int patientId = rs.getInt("patient_id");
                String patientName = rs.getString("patient_name");
                Object patientDisplay = (patientName != null && !patientName.isEmpty())
                        ? patientName : ("ID " + patientId);

                Timestamp createdAt = rs.getTimestamp("created_at");
                double total = rs.getDouble("total_amount");
                String status = rs.getString("payment_status");

                rows.add(new Object[]{
                        billId,
                        patientDisplay,
                        createdAt == null ? "" : createdAt.toString(),
                        total,
                        status,
                        "View"
                });
            }
            return rows;
        }
    }

    /**
     * Dòng cho Dashboard (BillID, Patient, Fee, Status) theo filter.
     * day: lọc theo ngày (DATE(created_at)=day); month/year: lọc theo tháng & năm; chỉ year: lọc theo năm.
     * status: null hoặc "" thì bỏ qua.
     */
    public List<Object[]> dashboardRows(java.sql.Date day, Integer month, Integer year, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT b.bill_id, " +
            "       COALESCE(p.full_name, CONCAT('ID ', b.patient_id)) AS patient, " +
            "       b.total_amount, b.payment_status " +
            "FROM bills b LEFT JOIN patients p ON p.patient_id = b.patient_id " +
            "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (day != null) {
            sql.append(" AND DATE(b.created_at) = ? ");
            params.add(day);
        }
        if (month != null && year != null) {
            sql.append(" AND MONTH(b.created_at) = ? AND YEAR(b.created_at) = ? ");
            params.add(month);
            params.add(year);
        } else if (year != null) {
            sql.append(" AND YEAR(b.created_at) = ? ");
            params.add(year);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND b.payment_status = ? ");
            params.add(status);
        }
        sql.append(" ORDER BY b.bill_id DESC");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new Object[]{
                        rs.getInt(1),            // Bill ID
                        rs.getString(2),         // Patient
                        rs.getDouble(3),         // Fee
                        rs.getString(4)          // Status
                    });
                }
                return rows;
            }
        }
    }

    /** Tổng tiền Dashboard theo cùng bộ lọc trên */
    public double dashboardTotal(java.sql.Date day, Integer month, Integer year, String status) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(b.total_amount),0) FROM bills b WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (day != null) {
            sql.append(" AND DATE(b.created_at) = ? ");
            params.add(day);
        }
        if (month != null && year != null) {
            sql.append(" AND MONTH(b.created_at) = ? AND YEAR(b.created_at) = ? ");
            params.add(month);
            params.add(year);
        } else if (year != null) {
            sql.append(" AND YEAR(b.created_at) = ? ");
            params.add(year);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND b.payment_status = ? ");
            params.add(status);
        }

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        }
    }

    /* ===================== helpers ===================== */

    private Bill mapRow(ResultSet rs) throws SQLException {
        Bill b = new Bill();
        b.setBillId(rs.getInt("bill_id"));
        b.setPatientId(rs.getInt("patient_id"));
        b.setTotalAmount(rs.getDouble("total_amount"));
        b.setPaymentStatus(rs.getString("payment_status"));
        b.setCreatedAt(rs.getTimestamp("created_at"));
        return b;
    }
}
