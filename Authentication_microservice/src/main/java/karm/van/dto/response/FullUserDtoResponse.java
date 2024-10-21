package karm.van.dto.response;

import java.util.List;

public record FullUserDtoResponse(Long id,
                                  String name,
                                  String email,
                                  List<String> role,
                                  String firstName,
                                  String lastName,
                                  String description,
                                  String country,
                                  String roleInCommand,
                                  String skills,
                                  ProfileImageDtoResponse profileImage,
                                  List<UserCardResponse> userCards) {
}
