package org.sts.demo.signer.web.dto;

public record CibaStartResponse(
        String authReqId,
        String state,
        String nonce
) {}