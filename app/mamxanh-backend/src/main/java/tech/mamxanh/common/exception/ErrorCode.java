package tech.mamxanh.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Stable machine-readable error codes returned in the {@code code} property of every
 * {@code application/problem+json} response (docs/api/API.md section 4). Frontend logic
 * branches on these codes and the HTTP status, never on {@code detail} text.
 */
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed", "Dữ liệu gửi lên không hợp lệ."),
    EMAIL_ALREADY_USED(HttpStatus.CONFLICT, "Email already used", "Email này đã được sử dụng."),
    VERIFICATION_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "Verification token invalid",
            "Liên kết xác minh không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu gửi lại email xác minh."),
    RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "Resend too soon",
            "Bạn vừa yêu cầu gửi email xác minh. Vui lòng thử lại sau ít phút."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials", "Email hoặc mật khẩu không chính xác."),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Email not verified",
            "Tài khoản chưa xác minh email. Vui lòng mở liên kết xác minh trong hộp thư."),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "Account locked",
            "Tài khoản đã bị quản trị viên khóa. Vui lòng liên hệ quản trị viên."),
    LOGIN_TEMPORARILY_BLOCKED(HttpStatus.TOO_MANY_REQUESTS, "Login temporarily blocked",
            "Bạn đã nhập sai mật khẩu quá nhiều lần. Vui lòng thử lại sau ít phút."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Unauthenticated", "Bạn cần đăng nhập để thực hiện thao tác này."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied", "Bạn không có quyền thực hiện thao tác này."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Not found", "Không tìm thấy tài nguyên được yêu cầu."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", "Phương thức HTTP không được hỗ trợ."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type",
            "Định dạng nội dung không được hỗ trợ."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "File too large",
            "Dung lượng tệp vượt quá 5 MB cho phép."),
    UNSUPPORTED_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "Unsupported image type",
            "Chỉ hỗ trợ tệp hình ảnh định dạng JPEG, PNG hoặc WebP."),
    MAX_RECIPE_MEDIA_EXCEEDED(HttpStatus.BAD_REQUEST, "Max recipe media exceeded",
            "Mỗi bài công thức chỉ được có tối đa 5 ảnh minh họa."),
    INVALID_COVER_IMAGE_CONFIGURATION(HttpStatus.BAD_REQUEST, "Invalid cover image configuration",
            "Bài công thức có ảnh bắt buộc phải có đúng một ảnh được chọn làm ảnh bìa."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "Hệ thống gặp lỗi. Vui lòng thử lại sau.");

    private final HttpStatus status;
    private final String title;
    private final String defaultDetail;

    ErrorCode(HttpStatus status, String title, String defaultDetail) {
        this.status = status;
        this.title = title;
        this.defaultDetail = defaultDetail;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }

    public String defaultDetail() {
        return defaultDetail;
    }

    /** Fallback code for framework-generated problems that have no explicit application code. */
    public static ErrorCode fromStatus(int status) {
        return switch (status) {
            case 400 -> VALIDATION_FAILED;
            case 401 -> UNAUTHENTICATED;
            case 403 -> ACCESS_DENIED;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            default -> INTERNAL_ERROR;
        };
    }
}
