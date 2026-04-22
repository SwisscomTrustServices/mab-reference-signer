export const CIBA_IDENTIFIER_PATTERN = /^\+?[1-9]\d{6,15}$/;
export const CIBA_IDENTIFIER_ERROR = "Invalid CIBA identifier format (expected E.164-like number)";

export const CIBA_AES_FAMILY = "CIBA_AES";
export const CIBA_QES_FAMILY = "CIBA_QES";
export const CIBA_AES_SIGN_JOURNEY = "CIBA_AES_SIGN";
export const CIBA_AES_IDENT_JOURNEY = "CIBA_AES_IDENT";
export const CIBA_QES_IDENT_JOURNEY = "CIBA_QES_IDENT";
export const CIBA_QES_SIGN_JOURNEY = "CIBA_QES_SIGN";

export const CIBA_DEFAULT_POLL_INTERVAL_SEC = 5;
export const CIBA_DEFAULT_POLL_TIMEOUT_SEC = 120;

export const CIBA_POLL_STATUS_READY = "READY";
export const CIBA_POLL_STATUS_PENDING = "PENDING";

export const PAR_CONTEXT_STORAGE_KEY = "demoSigner.parContext.v1";

export const API = {
    PAR_AUTH: "/api/par/auth",
    PAR_TOKEN: "/api/par/token",
    CIBA_AUTH: "/api/ciba/auth",
    CIBA_WEBFINGER: "/api/ciba/webfinger",
    CIBA_TOKEN: "/api/ciba/token",
    ETSI_SIGN: "/api/etsi/sign"
};