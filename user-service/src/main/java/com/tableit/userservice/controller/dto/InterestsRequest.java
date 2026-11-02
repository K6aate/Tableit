package com.tableit.userservice.controller.dto;

import java.util.List;

// Body of PUT /api/users/me/interests: the complete new selection of interest codes.
public record InterestsRequest(List<String> codes) {
}
