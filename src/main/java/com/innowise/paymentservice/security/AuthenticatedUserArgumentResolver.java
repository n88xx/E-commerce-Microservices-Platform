package com.innowise.paymentservice.security;

import com.innowise.paymentservice.exception.UnauthorizedException;
import java.util.Locale;

import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link AuthenticatedUser} controller arguments from the identity headers that the API
 * Gateway adds after validating the JWT.
 */
@Component
public class AuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {

    /** Header carrying the identifier of the authenticated user. */
    public static final String USER_ID_HEADER = "X-User-Id";

    /** Header carrying the role of the authenticated user. */
    public static final String USER_ROLE_HEADER = "X-User-Role";

    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AuthenticatedUser.class.equals(parameter.getParameterType());
    }

    @Override
    public AuthenticatedUser resolveArgument(@NonNull MethodParameter parameter,
            ModelAndViewContainer mavContainer, NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        String userId = webRequest.getHeader(USER_ID_HEADER);
        String role = webRequest.getHeader(USER_ROLE_HEADER);
        if (userId == null || userId.isBlank() || role == null || role.isBlank()) {
            throw new UnauthorizedException("Missing authentication headers");
        }
        try {
            return new AuthenticatedUser(Long.valueOf(userId.trim()), parseRole(role));
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid authentication headers");
        }
    }

    private UserRole parseRole(String role) {
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith(ROLE_PREFIX)) {
            normalized = normalized.substring(ROLE_PREFIX.length());
        }
        return UserRole.valueOf(normalized);
    }
}
