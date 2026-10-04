package tech.mamxanh.subscription.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.entity.AccountStatus;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.entity.User;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.payos.PayOsClient;
import tech.mamxanh.integration.payos.dto.CreatePaymentLinkResponse;
import tech.mamxanh.integration.payos.dto.PayOsWebhookPayload;
import tech.mamxanh.subscription.dto.response.CheckoutResponse;
import tech.mamxanh.subscription.dto.response.MySubscriptionResponse;
import tech.mamxanh.subscription.dto.response.PlanResponse;
import tech.mamxanh.subscription.dto.response.TransactionHistoryResponse;
import tech.mamxanh.subscription.entity.PaymentStatus;
import tech.mamxanh.subscription.entity.PaymentTransactionEntity;
import tech.mamxanh.subscription.entity.SubscriptionEntity;
import tech.mamxanh.subscription.entity.SubscriptionStatus;
import tech.mamxanh.subscription.entity.SubscriptionTier;
import tech.mamxanh.subscription.repository.PaymentTransactionRepository;
import tech.mamxanh.subscription.repository.SubscriptionRepository;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final PayOsClient payOsClient;
    private final Clock clock;

    @Autowired
    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            UserRepository userRepository,
            PayOsClient payOsClient,
            Clock clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.userRepository = userRepository;
        this.payOsClient = payOsClient;
        this.clock = clock;
    }

    /**
     * AC-13.1: Displays the exact 3 official Phase 1 tiers without annual plans, discounts or trials.
     */
    @Transactional(readOnly = true)
    public List<PlanResponse> getAvailablePlans() {
        return List.of(
                new PlanResponse(
                        "FREE",
                        0,
                        "Gói FREE",
                        "Gói trải nghiệm mặc định dành cho mọi tài khoản",
                        List.of(
                                "AI Chatbot hỗ trợ ẩm thực chay",
                                "Gợi ý món theo nguyên liệu sẵn có"
                        ),
                        "Không thời hạn"
                ),
                new PlanResponse(
                        SubscriptionTier.PLUS.name(),
                        SubscriptionTier.PLUS.priceVnd(),
                        SubscriptionTier.PLUS.displayName(),
                        SubscriptionTier.PLUS.description(),
                        List.of(
                                "Toàn bộ quyền lợi gói FREE",
                                "AI hỗ trợ soạn bài viết và công thức",
                                "AI gợi ý biến tấu món chay sáng tạo"
                        ),
                        "Tháng (không tự động gia hạn)"
                ),
                new PlanResponse(
                        SubscriptionTier.PRO.name(),
                        SubscriptionTier.PRO.priceVnd(),
                        SubscriptionTier.PRO.displayName(),
                        SubscriptionTier.PRO.description(),
                        List.of(
                                "Toàn bộ quyền lợi gói PLUS",
                                "AI lập thực đơn tuần 7 ngày",
                                "AI lập thực đơn theo dinh dưỡng cá nhân"
                        ),
                        "Tháng (không tự động gia hạn)"
                )
        );
    }

    /**
     * Initializes payment request and redirects to payOS VietQR.
     */
    @Transactional
    public CheckoutResponse createCheckout(Long userId, SubscriptionTier tier) {
        if (tier == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Vui lòng chọn gói đăng ký hợp lệ (PLUS hoặc PRO).");
        }

        User user = validateActiveMember(userId);

        long orderCode = generateOrderCode();
        int amount = tier.priceVnd();
        String description = "Nang cap " + tier.name();

        PaymentTransactionEntity transaction = new PaymentTransactionEntity(user.getId(), String.valueOf(orderCode), amount);
        paymentTransactionRepository.save(transaction);

        CreatePaymentLinkResponse payOsResponse = payOsClient.createPaymentLink(orderCode, amount, description);
        if (payOsResponse == null || !payOsResponse.isSuccess() || payOsResponse.data() == null) {
            transaction.setStatus(PaymentStatus.FAILED);
            paymentTransactionRepository.save(transaction);
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Không thể khởi tạo liên kết thanh toán payOS. Vui lòng thử lại sau.");
        }

        return new CheckoutResponse(
                orderCode,
                amount,
                tier.name(),
                payOsResponse.data().checkoutUrl(),
                payOsResponse.data().qrCode()
        );
    }

    /**
     * Handles payOS webhook IPN callback.
     * Enforces AC-13.2, AC-13.3 (idempotency), AC-13.5 (signature & amount validation), AC-13.6 (cancelled/failed).
     */
    @Transactional
    public void handlePayOsWebhook(PayOsWebhookPayload payload) {
        // AC-13.5: Verify signature
        if (!payOsClient.verifyWebhook(payload)) {
            log.warn("SECURITY ALERT: Invalid payOS webhook signature rejected");
            throw new AppException(ErrorCode.PAYMENT_SIGNATURE_INVALID);
        }

        Long orderCode = payload.getOrderCode();
        if (orderCode == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Dữ liệu webhook thiếu mã đơn hàng orderCode.");
        }

        PaymentTransactionEntity transaction = paymentTransactionRepository.findByOrderCode(String.valueOf(orderCode))
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));

        // AC-13.3: Idempotent processing
        if (transaction.getStatus() == PaymentStatus.PAID) {
            log.info("Idempotent check: orderCode {} has already been processed as PAID. No-op.", orderCode);
            return;
        }

        // AC-13.5: Verify amount tampering
        Integer paidAmount = payload.getAmount();
        if (paidAmount == null || paidAmount != transaction.getAmountVnd()) {
            log.warn("SECURITY ALERT: Order {} amount tampered! Expected: {}, Received: {}",
                    orderCode, transaction.getAmountVnd(), paidAmount);
            transaction.setStatus(PaymentStatus.FAILED);
            paymentTransactionRepository.save(transaction);
            throw new AppException(ErrorCode.PAYMENT_SIGNATURE_INVALID, "Số tiền thanh toán không khớp với đơn hàng.");
        }

        // AC-13.6: Handle cancelled or failed payment
        if (!payload.isPaymentSuccess()) {
            log.info("Payment for orderCode {} failed or was cancelled.", orderCode);
            transaction.setStatus(PaymentStatus.CANCELLED);
            paymentTransactionRepository.save(transaction);
            return;
        }

        // AC-13.2: Activate paid subscription
        LocalDateTime now = LocalDateTime.now(clock);
        transaction.setStatus(PaymentStatus.PAID);
        transaction.setPaidAt(now);

        SubscriptionTier tier = (transaction.getAmountVnd() == SubscriptionTier.PRO.priceVnd())
                ? SubscriptionTier.PRO
                : SubscriptionTier.PLUS;

        // Expire any existing active subscription to satisfy UQ_SUBSCRIPTION_active filtered index
        Optional<SubscriptionEntity> existingActive = subscriptionRepository.findByUserIdAndStatus(
                transaction.getUserId(), SubscriptionStatus.ACTIVE);
        existingActive.ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            sub.setUpdatedAt(now);
            subscriptionRepository.save(sub);
        });

        // 30 days subscription period
        SubscriptionEntity newSubscription = new SubscriptionEntity(
                transaction.getUserId(),
                tier,
                now,
                now.plusDays(30)
        );
        newSubscription = subscriptionRepository.save(newSubscription);

        transaction.setSubscriptionId(newSubscription.getId());
        paymentTransactionRepository.save(transaction);
        log.info("Successfully activated subscription {} for userId {}", tier, transaction.getUserId());
    }

    /**
     * AC-13.4 & AC-13.7: Returns active subscription status or default Free if expired/none.
     */
    @Transactional
    public MySubscriptionResponse getMySubscription(Long userId) {
        validateActiveMember(userId);

        Optional<SubscriptionEntity> activeSubOpt = subscriptionRepository.findByUserIdAndStatus(
                userId, SubscriptionStatus.ACTIVE);

        if (activeSubOpt.isEmpty()) {
            return MySubscriptionResponse.free();
        }

        SubscriptionEntity sub = activeSubOpt.get();
        LocalDateTime now = LocalDateTime.now(clock);

        // AC-13.4: Auto-downgrade to Free when validity period expires without auto-charge
        if (now.isAfter(sub.getEndsAt()) || now.isEqual(sub.getEndsAt())) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            sub.setUpdatedAt(now);
            subscriptionRepository.save(sub);
            log.info("Subscription for user {} expired at {}. Reverting to FREE.", userId, sub.getEndsAt());
            return MySubscriptionResponse.free();
        }

        // AC-13.7: Display paid plan and expiration date
        List<String> unlockedFeatures = (sub.getTier() == SubscriptionTier.PRO)
                ? List.of(
                        "AI Chatbot hỗ trợ ẩm thực chay",
                        "Gợi ý món theo nguyên liệu sẵn có",
                        "AI hỗ trợ soạn bài viết và công thức",
                        "AI gợi ý biến tấu món chay sáng tạo",
                        "AI lập thực đơn tuần 7 ngày",
                        "AI lập thực đơn theo dinh dưỡng cá nhân"
                )
                : List.of(
                        "AI Chatbot hỗ trợ ẩm thực chay",
                        "Gợi ý món theo nguyên liệu sẵn có",
                        "AI hỗ trợ soạn bài viết và công thức",
                        "AI gợi ý biến tấu món chay sáng tạo"
                );

        return new MySubscriptionResponse(
                sub.getTier().name(),
                sub.getStatus().name(),
                true,
                sub.getStartsAt(),
                sub.getEndsAt(),
                unlockedFeatures
        );
    }

    @Transactional(readOnly = true)
    public List<TransactionHistoryResponse> getTransactionHistory(Long userId) {
        validateActiveMember(userId);
        return paymentTransactionRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(tx -> new TransactionHistoryResponse(
                        tx.getId(),
                        tx.getOrderCode(),
                        tx.getAmountVnd(),
                        tx.getStatus().name(),
                        tx.getCreatedAt(),
                        tx.getPaidAt()
                ))
                .toList();
    }

    private User validateActiveMember(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy thông tin tài khoản."));
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Tài khoản hiện không ở trạng thái hoạt động.");
        }
        if (user.getRole() != Role.CUSTOMER && user.getRole() != Role.EXPERT) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Chỉ tài khoản Member mới có quyền mua gói AI.");
        }
        return user;
    }

    private long generateOrderCode() {
        // PayOS accepts a positive integer up to 9007199254740991 (2^53 - 1)
        long timestamp = System.currentTimeMillis() % 1_000_000_000L;
        int random = ThreadLocalRandom.current().nextInt(100, 999);
        return timestamp * 1000L + random;
    }
}
