package org.intern.shopeefoodclone.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    // --- BAD REQUEST (400) ---

    // General
    INVALID_INPUT(40000, "Invalid input. Please check your data", HttpStatus.BAD_REQUEST),

    // Authentication & User
    INVALID_CREDENTIALS(40001, "Invalid credentials. Please check your username and password.", HttpStatus.BAD_REQUEST),
    USER_ALREADY_EXISTS(40010, "A user with this username or email already exists.", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS(40011, "A user with this email address already exists.", HttpStatus.BAD_REQUEST),
    PHONE_NUMBER_ALREADY_EXISTS(40012, "A user with this phone number already exists.", HttpStatus.BAD_REQUEST),
    SAME_PASSWORD(40013, "Your new password cannot be the same as your old password.", HttpStatus.BAD_REQUEST),
    ACCOUNT_BLOCKED(40014, "Your account has been blocked. Please contact support for assistance.", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL_FORMAT(40015, "The email address you entered is not valid.", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(40016, "The user you are looking for could not be found.", HttpStatus.BAD_REQUEST),
    GOOGLE_TOKEN_INVALID(40017, "The Google token is invalid or expired.", HttpStatus.BAD_REQUEST),

    // Restaurant & Menu
    RESTAURANT_NOT_FOUND(40030, "The restaurant could not be found.", HttpStatus.BAD_REQUEST),
    MENU_ITEM_NOT_FOUND(40031, "The selected food item is no longer available.", HttpStatus.BAD_REQUEST),
    RESTAURANT_CLOSED(40032, "The restaurant is currently closed.", HttpStatus.BAD_REQUEST),
    OUT_OF_STOCK(40033, "One or more items in your order are out of stock.", HttpStatus.BAD_REQUEST),

    // Order & Cart
    CART_IS_EMPTY(40040, "Your cart is empty. Please add items before checkout.", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND(40041, "The order could not be found.", HttpStatus.BAD_REQUEST),
    CANNOT_CANCEL_ORDER(40042, "This order can no longer be canceled as it is already being prepared or delivered.", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_STATUS(40043, "Invalid order status update.", HttpStatus.BAD_REQUEST),
    DIFFERENT_RESTAURANT_IN_CART(40044, "Your cart contains items from a different restaurant. Please clear your cart first.", HttpStatus.BAD_REQUEST),

    // Delivery & Driver
    DRIVER_NOT_FOUND(40050, "The delivery driver could not be found.", HttpStatus.BAD_REQUEST),
    DELIVERY_ADDRESS_NOT_FOUND(40051, "Delivery address is missing or invalid.", HttpStatus.BAD_REQUEST),
    OUT_OF_DELIVERY_AREA(40052, "Your address is out of the delivery area for this restaurant.", HttpStatus.BAD_REQUEST),

    // Payment & Vouchers
    PAYMENT_FAILED(40060, "Payment failed. Please try a different payment method.", HttpStatus.BAD_REQUEST),
    VOUCHER_INVALID(40061, "The voucher code is invalid or has expired.", HttpStatus.BAD_REQUEST),
    VOUCHER_USAGE_LIMIT_REACHED(40062, "You have reached the usage limit for this voucher.", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_ORDER_VALUE(40063, "Your order value does not meet the minimum requirement for this voucher.", HttpStatus.BAD_REQUEST),

    // --- UNAUTHENTICATED (401) ---
    UNAUTHENTICATED(40100, "You must be logged in to perform this action.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(40101, "Invalid or expired token. Please log in again.", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN(40102, "Your session has expired. Please log in again.", HttpStatus.UNAUTHORIZED),

    // --- FORBIDDEN (403) ---
    FORBIDDEN(40300, "You do not have permission to access this resource.", HttpStatus.FORBIDDEN),
    USER_NOT_VERIFIED(40301, "Your account is not verified. A new OTP has been sent to your email.", HttpStatus.FORBIDDEN),

    // --- NOT FOUND (404) ---
    RESOURCE_NOT_FOUND(40400, "No endpoint found for this request", HttpStatus.NOT_FOUND),
    VERIFICATION_TOKEN_NOT_FOUND(40404, "The verification token is invalid or has expired.", HttpStatus.NOT_FOUND),

    // --- TOO MANY REQUESTS (429) ---
    TOO_MANY_REQUESTS(42900, "Too many requests. Please wait %s seconds before trying again.", HttpStatus.TOO_MANY_REQUESTS);

    private final int code;
    private final String detailedMessage;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.detailedMessage = message;
        this.statusCode = statusCode;
    }
}
