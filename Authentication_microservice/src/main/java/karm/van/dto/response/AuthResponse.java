package karm.van.dto.response;

public record AuthResponse(String jwtToken, String refreshToken) {
}
