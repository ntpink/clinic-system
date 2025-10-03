package clinic_dao;

import clinic_ui.Connect;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDAO {
    private Connection conn; 
    public AppointmentDAO(Connection conn){
        this.conn = conn; 
    }
    public AppointmentDAO(){
        
    }
    // Lấy tất cả lịch khám kèm tên bệnh nhân, tên bác sĩ và số phòng
    public List<AppointmentDisplay> getAllWithNames() {
        List<AppointmentDisplay> list = new ArrayList<>();
        String sql = "SELECT a.appointment_id, p.patient_name, d.doctor_name, a.appointment_date, " +
                     "a.reason, a.status, r.room_number " +
                     "FROM appointments a " +
                     "JOIN patients p ON a.patient_id = p.patient_id " +
                     "JOIN doctors d ON a.doctor_id = d.doctor_id " +
                     "JOIN rooms r ON a.room_id = r.room_id";

        try (Connection conn = Connect.ConnectDB();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                AppointmentDisplay ad = new AppointmentDisplay();
                ad.setAppointmentId(rs.getInt("appointment_id"));
                ad.setPatientName(rs.getString("patient_name"));
                ad.setDoctorName(rs.getString("doctor_name"));
                ad.setAppointmentDate(rs.getTimestamp("appointment_date"));
                ad.setReason(rs.getString("reason"));
                ad.setStatus(rs.getString("status"));
                ad.setRoomNumber(rs.getString("room_number"));
                list.add(ad);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    public boolean insertAppointment(Appointment appointment) throws SQLException {
        String sql = "INSERT INTO appointments(patient_id, doctor_id, room_id, appointment_date, reason, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, appointment.getPatientId());
            pst.setInt(2, appointment.getDoctorId());
            pst.setInt(3, appointment.getRoomId());
            pst.setTimestamp(4, appointment.getAppointmentDate());
            pst.setString(5, appointment.getReason());
            pst.setString(6, appointment.getStatus());

            int rows = pst.executeUpdate();
            return rows > 0; // nếu rows > 0 là insert thành công
        }
    }
    // Thêm lịch khám mới (vẫn dùng ID)
    public boolean add(Appointment a) {
        String sql = "INSERT INTO appointments(patient_id, doctor_id, appointment_date, reason, status, room_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Connect.ConnectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, a.getPatientId());
            ps.setInt(2, a.getDoctorId());
            ps.setTimestamp(3, a.getAppointmentDate());
            ps.setString(4, a.getReason());
            ps.setString(5, a.getStatus());
            ps.setInt(6, a.getRoomId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Cập nhật lịch khám
    public boolean update(Appointment a) {
        String sql = "UPDATE appointments SET patient_id=?, doctor_id=?, appointment_date=?, reason=?, status=?, room_id=? " +
                     "WHERE appointment_id=?";
        try (Connection conn = Connect.ConnectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, a.getPatientId());
            ps.setInt(2, a.getDoctorId());
            ps.setTimestamp(3, a.getAppointmentDate());
            ps.setString(4, a.getReason());
            ps.setString(5, a.getStatus());
            ps.setInt(6, a.getRoomId());
            ps.setInt(7, a.getAppointmentId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Xóa lịch khám
    public boolean deleteAppointment(int appointmentId) {
        String sql = "DELETE FROM appointments WHERE appointment_id=?";
        try (Connection conn = Connect.ConnectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, appointmentId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Tìm kiếm lịch khám theo patientName, doctorName hoặc roomNumber
    public List<AppointmentDisplay> search(String patientName, String doctorName, String roomNumber) {
        List<AppointmentDisplay> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT a.appointment_id, p.patient_name, d.doctor_name, a.appointment_date, " +
            "a.reason, a.status, r.room_number " +
            "FROM appointments a " +
            "JOIN patients p ON a.patient_id = p.patient_id " +
            "JOIN doctors d ON a.doctor_id = d.doctor_id " +
            "JOIN rooms r ON a.room_id = r.room_id WHERE 1=1"
        );

        if (patientName != null && !patientName.trim().isEmpty()) {
            sql.append(" AND p.patient_name LIKE ?");
        }
        if (doctorName != null && !doctorName.trim().isEmpty()) {
            sql.append(" AND d.doctor_name LIKE ?");
        }
        if (roomNumber != null && !roomNumber.trim().isEmpty()) {
            sql.append(" AND r.room_number LIKE ?");
        }

        try (Connection conn = Connect.ConnectDB();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int index = 1;
            if (patientName != null && !patientName.trim().isEmpty()) {
                ps.setString(index++, "%" + patientName + "%");
            }
            if (doctorName != null && !doctorName.trim().isEmpty()) {
                ps.setString(index++, "%" + doctorName + "%");
            }
            if (roomNumber != null && !roomNumber.trim().isEmpty()) {
                ps.setString(index++, "%" + roomNumber + "%");
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AppointmentDisplay ad = new AppointmentDisplay();
                    ad.setAppointmentId(rs.getInt("appointment_id"));
                    ad.setPatientName(rs.getString("patient_name"));
                    ad.setDoctorName(rs.getString("doctor_name"));
                    ad.setAppointmentDate(rs.getTimestamp("appointment_date"));
                    ad.setReason(rs.getString("reason"));
                    ad.setStatus(rs.getString("status"));
                    ad.setRoomNumber(rs.getString("room_number"));
                    list.add(ad);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
