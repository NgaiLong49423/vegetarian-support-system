package tech.mamxanh.subscription.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.subscription.entity.PaymentStatus;
import tech.mamxanh.subscription.entity.PaymentTransactionEntity;
import tech.mamxanh.subscription.repository.PaymentTransactionRepository;

/** Commit the local order before provider I/O so early webhooks and ambiguous timeouts retain an audit trail. */
@Service
public class PaymentOrderService {
    private final PaymentTransactionRepository payments;
    public PaymentOrderService(PaymentTransactionRepository payments) { this.payments = payments; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(PaymentTransactionEntity order) { payments.saveAndFlush(order); }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String orderCode) {
        payments.findByOrderCodeForUpdate(orderCode).ifPresent(order -> {
            // Never overwrite a successful callback that raced the create-link response.
            if (order.getStatus() == PaymentStatus.PENDING) order.setStatus(PaymentStatus.FAILED);
        });
    }
}
