package com.notapos.service;

import com.notapos.entity.Payment;
import com.notapos.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for Payment operations.
 * 
 * Handles payment processing, tips, and split checks.
 * 
 * @author CJ
 */

@Service
public class PaymentService {
    
    private final PaymentRepository paymentRepository; 

    @Autowired
    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<Payment> getAllPayments() {                         // Get all payments
        return paymentRepository.findAll();
    }

    public Optional<Payment> getPaymentById(Long id) {              // Get payment by ID
        return paymentRepository.findById(id);
    }

    public List<Payment> getPaymentsByOrder(Long orderId) {         // Get payment by Order
        return paymentRepository.findByOrderId(orderId);
    }

    public List<Payment> getPaymentsByMethod(String paymentMethod) {    // Get payments by method (cash, card, etc.)
        return paymentRepository.findByPaymentMethod(paymentMethod);
    }

    public List<Payment> getPaymentsByStatus(String status) {           // Get payment by status
        return paymentRepository.findByStatus(status);
    }

    public List<Payment> getPaymentsBetween(LocalDateTime start, LocalDateTime end) {           // Get payments between certain times
        return paymentRepository.findPaymentsBetween(start, end);
    }

    public BigDecimal calculateTotalTips(LocalDateTime start, LocalDateTime end) {              // Get total tips between certain times
        BigDecimal total = paymentRepository.calculateTotalTips(start, end);
        return total != null ? total : BigDecimal.ZERO;
    }

    public Payment createPayment(Payment payment) {                                         // Create new payment
        return paymentRepository.save(payment);
    }

    public Payment updatePayment(Long id, Payment updatedPayment) {                                     // Update existing payment
        Payment existing = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));

        existing.setAmount(updatedPayment.getAmount());
        existing.setTipAmount(updatedPayment.getTipAmount());
        existing.setStatus(updatedPayment.getStatus());

        return paymentRepository.save(existing);
    }

    public void deletePayment(Long id) {                                                        // Delete existing payment
        paymentRepository.deleteById(id);
    }
}
