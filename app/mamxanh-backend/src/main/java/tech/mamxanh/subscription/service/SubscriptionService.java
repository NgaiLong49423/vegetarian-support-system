package tech.mamxanh.subscription.service;

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
import tech.mamxanh.subscription.dto.response.*;
import tech.mamxanh.subscription.entity.*;
import tech.mamxanh.subscription.repository.AiConfigurationRepository;
import tech.mamxanh.subscription.repository.AiCreditAdministrationRepository;
import tech.mamxanh.subscription.repository.PaymentTransactionRepository;
import tech.mamxanh.subscription.repository.SubscriptionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
public class SubscriptionService {

    private static final SecureRandom ORDER_CODES = new SecureRandom();
    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final PayOsClient payOsClient;
    private final Clock clock;
    private final PaymentSettlementService settlement;
    private final PaymentOrderService orders;
    private final AiConfigurationRepository aiConfiguration;
    private final AiCreditAdministrationRepository aiCredit;

    @Autowired
    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            UserRepository userRepository,
            PayOsClient payOsClient,
            Clock clock, PaymentSettlementService settlement, PaymentOrderService orders,
            AiConfigurationRepository aiConfiguration, AiCreditAdministrationRepository aiCredit) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.userRepository = userRepository;
        this.payOsClient = payOsClient;
        this.clock = clock;
        this.settlement = settlement;
        this.orders = orders;
        this.aiConfiguration = aiConfiguration;
        this.aiCredit = aiCredit;
    }

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
            PaymentTransactionRepository paymentTransactionRepository, UserRepository userRepository,
            PayOsClient payOsClient, Clock clock, PaymentSettlementService settlement,
            PaymentOrderService orders, AiConfigurationRepository aiConfiguration) {
        this(subscriptionRepository, paymentTransactionRepository, userRepository, payOsClient, clock,
                settlement, orders, aiConfiguration, null);
    }

    /**
     * AC-13.1: Lấy danh sách 3 gói hội viên chính thức (Free, Plus, Pro) kèm thông tin giá tiền và hạn mức token tháng.
     */
    @Transactional(readOnly = true)
    public List<PlanResponse> getAvailablePlans() {
        var configuration = aiConfiguration.findConfiguration();
        long freeQuota = aiConfiguration.currentFreeQuota(clock.instant());
        return List.of(
                plan(configuration, "FREE", freeQuota, "Gói miễn phí với hạn mức AI hàng tháng"),
                plan(configuration, "PLUS", null, "Thêm ngân sách AI hàng tháng cho tài khoản của bạn"),
                plan(configuration, "PRO", null, "Ngân sách AI hàng tháng cao hơn cho tài khoản của bạn")
        );
    }

    private static PlanResponse plan(tech.mamxanh.subscription.entity.AiConfigurationSnapshot configuration,
            String tier, Long overrideQuota, String description) {
        var configured = configuration.plans().stream().filter(value -> value.tier().equals(tier)).findFirst()
                .orElseThrow(() -> new IllegalStateException("AI plan configuration missing: " + tier));
        long quota = overrideQuota == null ? configured.tokenQuota() : overrideQuota;
        List<String> features = features(configuration, tier, quota);
        String name = switch (tier) { case "FREE" -> "FREE"; case "PLUS" -> "PLUS"; default -> "PRO"; };
        return new PlanResponse(tier, "FREE".equals(tier) ? 0 : configured.priceVnd(), name,
                description, features, "FREE".equals(tier)
                        ? "Tháng dương lịch" : "Mỗi tháng trong chu kỳ gói, không tự động gia hạn");
    }

    private static List<String> features(tech.mamxanh.subscription.entity.AiConfigurationSnapshot configuration,
            String tier, long quota) {
        List<String> features = new java.util.ArrayList<>();
        features.add("Các chức năng AI theo vai trò và quyền nghiệp vụ của tài khoản");
        features.add("Hạn mức " + String.format(java.util.Locale.forLanguageTag("vi-VN"), "%,d", quota) + " token mỗi tháng");
        var allowedModels = configuration.models().stream().filter(model -> model.enabled()).toList();
        String modelNames = allowedModels.stream().map(model -> model.displayName()
                + (model.freeDefault() ? " (mặc định)" : "")).collect(java.util.stream.Collectors.joining(", "));
        if (!modelNames.isBlank()) features.add("Model Gemini: " + modelNames);
        features.add("Có thể dùng AI Credit để tiếp tục khi vượt hạn mức tháng");
        return List.copyOf(features);
    }

    /**
     * Khởi tạo giao dịch nâng cấp gói (PLUS/PRO), tạo bản ghi PENDING trong database và gọi payOS để lấy link VietQR.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public CheckoutResponse createCheckout(Long userId, SubscriptionTier tier) {
        if (tier == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Vui lòng chọn gói đăng ký hợp lệ (PLUS hoặc PRO).");
        }

        User user = validateActiveMember(userId);
        payOsClient.validateCheckoutConfiguration();

        long orderCode = generateOrderCode();
        var configuredPlan = aiConfiguration.findConfiguration().plans().stream()
                .filter(plan -> plan.tier().equals(tier.name())).findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_RESPONSE_INVALID));
        int amount = configuredPlan.priceVnd();
        if (amount <= 0) throw new AppException(ErrorCode.VALIDATION_FAILED, "Gói trả phí phải có giá lớn hơn 0đ.");
        String description = "Nang cap " + tier.name();

        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        LocalDateTime expiresAt = now.plusMinutes(15);
        PaymentTransactionEntity transaction = new PaymentTransactionEntity(user.getId(), String.valueOf(orderCode), amount);
        transaction.setSubscriptionTier(tier);
        transaction.setTokenQuotaSnapshot(configuredPlan.tokenQuota());
        transaction.setCreatedAt(now);
        transaction.setExpiresAt(expiresAt);
        orders.create(transaction);

        // No SQL transaction spans provider I/O. On an ambiguous provider error keep PENDING:
        // the remote order may exist and a later verified webhook must still find it.
        CreatePaymentLinkResponse payOsResponse = payOsClient.createPaymentLink(orderCode, amount, description,
                Math.toIntExact(expiresAt.toEpochSecond(ZoneOffset.UTC)));
        if (payOsResponse == null || !payOsResponse.isSuccess() || payOsResponse.data() == null) {
            orders.markFailed(transaction.getOrderCode());
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
     * Khởi tạo giao dịch nạp tiền mua AI Credit dự phòng, tính toán số credit tương ứng và tạo link thanh toán payOS có hạn 15 phút.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public CreditCheckoutResponse createCreditCheckout(Long userId, int amountVnd) {
        User user = validateActiveMember(userId);
        var rate = aiConfiguration.findConfiguration().credit();
        if (amountVnd < rate.minimumTopUpVnd() || amountVnd > rate.maximumTopUpVnd()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Số tiền nạp phải từ " + rate.minimumTopUpVnd() + " đến " + rate.maximumTopUpVnd() + " VND.");
        }
        payOsClient.validateCheckoutConfiguration();
        BigDecimal credits = rate.creditsPer1000Vnd().multiply(BigDecimal.valueOf(amountVnd))
                .divide(BigDecimal.valueOf(1000), 9, RoundingMode.HALF_UP).stripTrailingZeros();
        long orderCode = generateOrderCode();
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        LocalDateTime expiresAt = now.plusMinutes(15);
        PaymentTransactionEntity payment = new PaymentTransactionEntity(user.getId(), String.valueOf(orderCode), amountVnd);
        payment.setCreatedAt(now);
        payment.setProductType(PaymentTransactionEntity.ProductType.AI_CREDIT);
        payment.setCreditAmountSnapshot(credits);
        payment.setCreditsPer1000VndSnapshot(rate.creditsPer1000Vnd());
        payment.setExpiresAt(expiresAt);
        orders.create(payment);

        CreatePaymentLinkResponse link = payOsClient.createPaymentLink(orderCode, amountVnd, "Nap AI Credit",
                Math.toIntExact(expiresAt.toEpochSecond(ZoneOffset.UTC)));
        if (link == null || !link.isSuccess() || link.data() == null) {
            orders.markFailed(payment.getOrderCode());
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Không thể khởi tạo liên kết nạp AI Credit. Vui lòng thử lại sau.");
        }
        return new CreditCheckoutResponse(orderCode, amountVnd, credits, rate.creditsPer1000Vnd(), expiresAt,
                link.data().checkoutUrl(), link.data().qrCode());
    }

    /**
     * Tra cứu số dư ví AI Credit hiện có của tài khoản cùng với tỷ giá nạp tiền và giới hạn min/max.
     */
    @Transactional(readOnly = true)
    public MyAiCreditResponse getMyAiCredit(Long userId) {
        validateActiveMember(userId);
        var credit = aiConfiguration.findConfiguration().credit();
        return new MyAiCreditResponse(aiCredit.balance(userId), credit.creditsPer1000Vnd(),
                credit.minimumTopUpVnd(), credit.maximumTopUpVnd());
    }

    /**
     * Xử lý Webhook IPN từ payOS: xác thực chữ ký HMAC-SHA256, kiểm tra idempotency và kích hoạt gói hoặc nạp credit.
     */
    public void handlePayOsWebhook(PayOsWebhookPayload payload) {
        if (!payOsClient.verifyWebhook(payload)) throw new AppException(ErrorCode.PAYMENT_SIGNATURE_INVALID);
        Long orderCode = payload.getOrderCode();
        Integer amount = payload.getAmount();
        if (orderCode == null || orderCode <= 0 || amount == null || amount <= 0) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Dữ liệu webhook thiếu hoặc sai mã đơn hàng/số tiền.");
        }
        if (!"VND".equals(payload.data().get("currency"))
                || !(payload.data().get("code") instanceof String signedCode) || signedCode.isBlank()) throw new AppException(ErrorCode.PAYMENT_SIGNATURE_INVALID);
        settlement.settle(String.valueOf(orderCode), amount,
                payload.isPaymentSuccess() ? PaymentStatus.PAID : PaymentStatus.CANCELLED,
                payload.getTransactionAtUtc());
    }

    /**
     * Lấy thông tin gói hội viên hiện tại; tự động chuyển về gói FREE nếu gói cũ đã hết hạn mà không tự động trừ tiền.
     */
    @Transactional
    public MySubscriptionResponse getMySubscription(Long userId) {
        validateActiveMember(userId);

        Optional<SubscriptionEntity> activeSubOpt = subscriptionRepository.findByUserIdAndStatus(
                userId, SubscriptionStatus.ACTIVE);

        if (activeSubOpt.isEmpty()) {
            return freeSubscription();
        }

        SubscriptionEntity sub = activeSubOpt.get();
        LocalDateTime now = LocalDateTime.now(clock);

        // AC-13.4: Auto-downgrade to Free when validity period expires without auto-charge
        if (now.isAfter(sub.getEndsAt()) || now.isEqual(sub.getEndsAt())) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            sub.setUpdatedAt(now);
            subscriptionRepository.save(sub);
            log.info("Subscription for user {} expired at {}. Reverting to FREE.", userId, sub.getEndsAt());
            return freeSubscription();
        }

        // Match the AI guard's starts_at <= now < ends_at window without cancelling a future period.
        if (!sub.isActiveAt(now)) return freeSubscription();

        List<String> unlockedFeatures = planFeatures(sub.getTier().name(), sub.effectiveQuota(aiConfiguration.paidQuotaPolicyCutover()));

        return new MySubscriptionResponse(
                sub.getTier().name(),
                sub.getStatus().name(),
                true,
                sub.getStartsAt(),
                sub.getEndsAt(),
                unlockedFeatures
        );
    }

    private MySubscriptionResponse freeSubscription() {
        return new MySubscriptionResponse("FREE", "ACTIVE", true, null, null,
                planFeatures("FREE", null));
    }

    private List<String> planFeatures(String tier, Long quotaSnapshot) {
        var configuration = aiConfiguration.findConfiguration();
        long quota = quotaSnapshot == null ? aiConfiguration.currentFreeQuota(clock.instant()) : quotaSnapshot;
        return features(configuration, tier, quota);
    }

    /**
     * Tra cứu lịch sử các giao dịch thanh toán nâng gói và nạp credit của tài khoản từ cơ sở dữ liệu.
     */
    @Transactional(readOnly = true)
    public List<TransactionHistoryResponse> getTransactionHistory(Long userId) {
        validateActiveMember(userId);
        var transactions = paymentTransactionRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        var subscriptionIds = transactions.stream()
                .filter(tx -> tx.getStatus() == PaymentStatus.PAID)
                .map(PaymentTransactionEntity::getSubscriptionId).filter(java.util.Objects::nonNull).distinct().toList();
        var purchasedSubscriptions = subscriptionIds.isEmpty() ? java.util.Map.<Long, SubscriptionEntity>of()
                : subscriptionRepository.findAllById(subscriptionIds).stream()
                        .filter(sub -> sub.getUserId().equals(userId))
                        .collect(java.util.stream.Collectors.toMap(SubscriptionEntity::getId, sub -> sub));
        return transactions.stream()
                .map(tx -> new TransactionHistoryResponse(
                        tx.getId(),
                        tx.getOrderCode(),
                        tx.getAmountVnd(),
                        tx.getStatus().name(),
                        tx.getCreatedAt(),
                        tx.getPaidAt(),
                        tx.getProductType() == PaymentTransactionEntity.ProductType.AI_CREDIT ? null
                                : tx.getSubscriptionTier() != null ? tx.getSubscriptionTier().name()
                                : tx.getAmountVnd() == 99_000 ? "PRO"
                                : tx.getAmountVnd() == 49_000 ? "PLUS" : null,
                        tx.getStatus() == PaymentStatus.PAID && tx.getSubscriptionId() != null
                                && purchasedSubscriptions.containsKey(tx.getSubscriptionId())
                                ? purchasedSubscriptions.get(tx.getSubscriptionId()).getEndsAt() : null,
                        tx.getProductType(), tx.getCreditAmountSnapshot(), tx.getExpiresAt(),
                        tx.getCreditsPer1000VndSnapshot()
                ))
                .toList();
    }

    /**
     * Hàm kiểm tra bảo mật: Xác thực tài khoản có tồn tại, đang hoạt động (ACTIVE) và có vai trò Member (Customer/Expert).
     */
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

    /**
     * Sinh mã đơn hàng ngẫu nhiên (orderCode) trong phạm vi số nguyên dương an toàn của JavaScript (tránh tràn số FE).
     */
    private long generateOrderCode() {
        // Positive, JS-safe integer; random IDs avoid collisions across instances/restarts.
        return ORDER_CODES.nextLong(1, 9_007_199_254_740_992L);
    }
}
