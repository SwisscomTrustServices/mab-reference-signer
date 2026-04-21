package org.sts.demo.signer.web.dto;

public record CibaWebfingerResponse(
        boolean onboarded,
        String subject,
        String eligible,
        String platform,
        String status,
        String evidenceExpiryDate,
        String message
) {}

