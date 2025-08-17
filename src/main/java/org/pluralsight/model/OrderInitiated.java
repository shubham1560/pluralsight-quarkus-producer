package org.pluralsight.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class OrderInitiated {
    private String orderId;
    private String customerName;
    private String productName;
    private int quantity;
    private double totalAmount;
    private LocalDateTime orderDate;
    private String status;

    public OrderInitiated() {
        this.orderId = UUID.randomUUID().toString();
        this.orderDate = LocalDateTime.now();
        this.status = "INITIATED";
    }

    public OrderInitiated(String customerName, String productName, int quantity, double totalAmount) {
        this();
        this.customerName = customerName;
        this.productName = productName;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "OrderInitiated{" +
                "orderId='" + orderId + '\'' +
                ", customerName='" + customerName + '\'' +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", totalAmount=" + totalAmount +
                ", orderDate=" + orderDate +
                ", status='" + status + '\'' +
                '}';
    }
}
