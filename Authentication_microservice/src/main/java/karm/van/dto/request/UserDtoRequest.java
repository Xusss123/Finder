package karm.van.dto.request;

import java.util.List;

public record UserDtoRequest(String name,
                             String password,
                             String email,
                             List<String> role,
                             String firstName,
                             String lastName,
                             String description,
                             String country,
                             String roleInCommand,
                             String skills) {
}
