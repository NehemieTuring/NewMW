package com.mutuelle.service;

import com.mutuelle.entity.Payment;
import com.mutuelle.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public List<Payment> getMemberPayments(Long memberId) {
        return paymentRepository.findByMemberIdOrderByPaymentDateDesc(memberId);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
}
