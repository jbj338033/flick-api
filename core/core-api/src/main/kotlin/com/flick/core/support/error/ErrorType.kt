package com.flick.core.support.error

import org.springframework.boot.logging.LogLevel
import org.springframework.http.HttpStatus

enum class ErrorType(
    val status: HttpStatus,
    val message: String,
    val logLevel: LogLevel = LogLevel.WARN,
) {
    // Common
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", LogLevel.ERROR),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation failed"),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid token"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token expired"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied"),
    DAUTH_LOGIN_FAILED(HttpStatus.BAD_REQUEST, "DAuth login failed"),
    DAUTH_TOKEN_EXCHANGE_FAILED(HttpStatus.BAD_REQUEST, "DAuth token exchange failed"),
    DAUTH_USER_FETCH_FAILED(HttpStatus.BAD_REQUEST, "Failed to fetch user from DAuth"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "Insufficient balance"),

    // Booth
    BOOTH_NOT_FOUND(HttpStatus.NOT_FOUND, "Booth not found"),
    BOOTH_ALREADY_EXISTS(HttpStatus.CONFLICT, "User already has a booth"),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found"),
    PRODUCT_NOT_IN_BOOTH(HttpStatus.BAD_REQUEST, "Product does not belong to this booth"),
    PRODUCT_SOLD_OUT(HttpStatus.BAD_REQUEST, "Product is sold out"),
    PRODUCT_STOCK_INSUFFICIENT(HttpStatus.BAD_REQUEST, "Insufficient product stock"),
    PRODUCT_HAS_ACTIVE_ORDERS(HttpStatus.CONFLICT, "Product has active orders"),
    OPTION_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "Option group not found"),
    PRODUCT_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Product option not found"),
    OPTION_NOT_IN_PRODUCT(HttpStatus.BAD_REQUEST, "Option does not belong to this product"),
    OPTION_QUANTITY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Quantity selection not allowed for this option"),
    REQUIRED_OPTION_MISSING(HttpStatus.BAD_REQUEST, "Required option group is missing"),
    OPTION_MAX_SELECTIONS_EXCEEDED(HttpStatus.BAD_REQUEST, "Max selections exceeded for option group"),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Order not found"),
    ORDER_ALREADY_CONFIRMED(HttpStatus.BAD_REQUEST, "Order is already confirmed"),

    // Payment
    PAYMENT_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "Payment code has expired"),
    PAYMENT_CODE_INVALID(HttpStatus.BAD_REQUEST, "Invalid payment code"),
    PAYMENT_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Payment request not found"),
    PAYMENT_CODE_GENERATION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "Failed to generate payment code", LogLevel.ERROR),

    // Notice
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "Notice not found"),

    // Purchase Limit
    PURCHASE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "Purchase limit exceeded"),

    // Order Cancel
    ORDER_NOT_CANCELLABLE(HttpStatus.BAD_REQUEST, "Order is not cancellable"),
    ORDER_CANCEL_FORBIDDEN(HttpStatus.FORBIDDEN, "Not allowed to cancel this order"),

    // Refund
    REFUND_ALREADY_REQUESTED(HttpStatus.CONFLICT, "Refund already requested"),
    REFUND_NO_BALANCE(HttpStatus.BAD_REQUEST, "No balance to refund"),

    // Grant
    GRANT_ALREADY_CLAIMED(HttpStatus.CONFLICT, "Grant already claimed"),

    // Notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Notification not found"),

    // FacePay
    FACE_EMBEDDING_INVALID(HttpStatus.BAD_REQUEST, "Invalid face embedding dimension"),
    NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet"),
}
