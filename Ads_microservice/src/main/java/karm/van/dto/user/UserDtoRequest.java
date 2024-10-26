package karm.van.dto.user;

import java.util.List;

public record UserDtoRequest(Long id,
                             String name,
                             String email,
                             List<String> role,
                             String firstName,
                             String lastName,
                             String description,
                             String country,
                             String roleInCommand,
                             String skills) {
}
