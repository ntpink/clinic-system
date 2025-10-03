package clinic_dao;

import java.sql.Timestamp;

/**
 * Model Bill (hóa đơn).
 * NOTE: Nếu tên cột DB của bạn khác, chỉnh lại trong BillDAO mapping.
 */
public class Bill {
    private int billId;              // bills.bill_id
    private int patientId;           // bills.patient_id
    private double totalAmount;      // bills.total_amount
    private String paymentStatus;    // bills.payment_status  (ví dụ: "UNPAID" | "PAID")
    private Timestamp createdAt;     // bills.created_at

    public Bill() { }

    public Bill(int billId, int patientId, double totalAmount, String paymentStatus, Timestamp createdAt) {
        this.billId = billId;
        this.patientId = patientId;
        this.totalAmount = totalAmount;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
    }

    public int getBillId() { return billId; }
    public void setBillId(int billId) { this.billId = billId; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Bill{" +
                "billId=" + billId +
                ", patientId=" + patientId +
                ", totalAmount=" + totalAmount +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
