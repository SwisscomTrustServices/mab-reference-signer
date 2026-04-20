import {
    downloadPdfFromBase64,
    getCheckedValue,
    safeJsonPretty,
    setAuthLinkEnabled,
    setEnabled,
    setStatus,
    truncateForUi
} from "/ui-utils.js";
import {getJson, postJson, postMultipart} from "/api-client.js";

const el = {
    parForm: document.getElementById("parForm"),
    cibaForm: document.getElementById("cibaForm"),
    tokenForm: document.getElementById("tokenForm"),
    tabPar: document.getElementById("tabPar"),
    tabCiba: document.getElementById("tabCiba"),
    panelParJourneys: document.getElementById("panelParJourneys"),
    panelCibaJourneys: document.getElementById("panelCibaJourneys"),
    parStep1Card: document.getElementById("parStep1Card"),
    panelPar: document.getElementById("panelPar"),
    cibaStep1Card: document.getElementById("cibaStep1Card"),
    cibaStep2Card: document.getElementById("cibaStep2Card"),
    cibaStep3Card: document.getElementById("cibaStep3Card"),
    step2Card: document.getElementById("step2Card"),
    tokenCard: document.getElementById("tokenCard"),
    btnPar: document.getElementById("btnPar"),
    btnToken: document.getElementById("btnToken"),
    btnSign: document.getElementById("btnSign"),
    btnCibaCheck: document.getElementById("btnCibaCheck"),
    btnCibaAuth: document.getElementById("btnCibaAuth"),
    btnCibaPollToken: document.getElementById("btnCibaPollToken"),
    btnCibaStopPoll: document.getElementById("btnCibaStopPoll"),
    btnDownloadPdf: document.getElementById("btnDownloadPdf"),
    parStatus: document.getElementById("parStatus"),
    cibaStatus: document.getElementById("cibaStatus"),
    cibaPollStatus: document.getElementById("cibaPollStatus"),
    cibaPollCountdown: document.getElementById("cibaPollCountdown"),
    tokenStatus: document.getElementById("tokenStatus"),
    signStatus: document.getElementById("signStatus"),
    authStatus: document.getElementById("authStatus"),
    downloadStatus: document.getElementById("downloadStatus"),
    authLink: document.getElementById("authLink"),
    parOut: document.getElementById("parOut"),
    cibaWebfingerOut: document.getElementById("cibaWebfingerOut"),
    cibaOut: document.getElementById("cibaOut"),
    cibaTokenOut: document.getElementById("cibaTokenOut"),
    tokenOut: document.getElementById("tokenOut"),
    signOut: document.getElementById("signOut"),
    authCode: document.getElementById("authCode"),
    pdf: document.getElementById("pdf"),
    cibaPdf: document.getElementById("cibaPdf"),
    cibaIdentifier: document.getElementById("cibaIdentifier"),
    cibaIdentifierStatus: document.getElementById("cibaIdentifierStatus"),
    cibaPdfGroup: document.getElementById("cibaPdfGroup")
};

const CIBA_IDENTIFIER_PATTERN = /^\+?[1-9]\d{6,15}$/;
const CIBA_IDENTIFIER_ERROR = "Invalid CIBA identifier format (expected E.164-like number)";
const CIBA_AES_FAMILY = "CIBA_AES";
const CIBA_QES_FAMILY = "CIBA_QES";
const CIBA_AES_IDENT_JOURNEY = "CIBA_AES_IDENT";
const CIBA_AES_SIGN_JOURNEY = "CIBA_AES_SIGN";
const CIBA_QES_IDENT_JOURNEY = "CIBA_QES_IDENT";
const CIBA_QES_SIGN_JOURNEY = "CIBA_QES_SIGN";
const CIBA_DEFAULT_POLL_INTERVAL_SEC = 5;
const CIBA_DEFAULT_POLL_TIMEOUT_SEC = 120;
const CIBA_SPINNER_FRAMES = ["-", "\\", "|", "/"];
const CIBA_POLL_STATUS_READY = "READY";
const CIBA_POLL_STATUS_PENDING = "PENDING";

const API = {
    PAR_AUTH: "/api/par/auth",
    PAR_TOKEN: "/api/par/token",
    CIBA_AUTH: "/api/ciba/auth",
    CIBA_WEBFINGER: "/api/ciba/webfinger",
    CIBA_TOKEN: "/api/ciba/token",
    ETSI_SIGN: "/api/etsi/sign"
};

const state = {
    lastAuthorizationUrl: null,
    lastState: null,
    lastNonce: null,
    lastAccessToken: null,
    pendingCibaAuthReqId: null,
    lastSignedPdfBase64: null,
    lastSignedPdfFilename: null,
    cibaOnboarded: null,
    cibaPollIntervalSec: CIBA_DEFAULT_POLL_INTERVAL_SEC,
    cibaPollDeadlineAtMs: 0,
    cibaPollingActive: false,
    cibaPollCancelled: false
};


function getSelectedCibaFamily() {
    const selected = getCheckedValue("cibaJourney");
    return selected || CIBA_AES_FAMILY;
}

function getCibaWebfingerJourney(cibaFamily) {
    return cibaFamily === CIBA_QES_FAMILY ? CIBA_QES_SIGN_JOURNEY : CIBA_AES_SIGN_JOURNEY;
}

function getCibaAuthJourney(cibaFamily, onboarded) {
    if (cibaFamily === CIBA_QES_FAMILY) {
        return onboarded ? CIBA_QES_SIGN_JOURNEY : CIBA_QES_IDENT_JOURNEY;
    }
    return onboarded ? CIBA_AES_SIGN_JOURNEY : CIBA_AES_IDENT_JOURNEY;
}

function setStep1Tab(tab) {
    const parActive = tab === "par";
    el.tabPar.classList.toggle("active", parActive);
    el.tabCiba.classList.toggle("active", !parActive);

    el.panelParJourneys.classList.toggle("hidden", !parActive);
    el.panelParJourneys.classList.toggle("active", parActive);
    el.panelCibaJourneys.classList.toggle("hidden", parActive);
    el.panelCibaJourneys.classList.toggle("active", !parActive);

    el.panelPar.classList.toggle("hidden", !parActive);
    el.panelPar.classList.toggle("active", parActive);
    el.parStep1Card.classList.toggle("hidden", !parActive);

    el.cibaStep1Card.classList.toggle("hidden", parActive);
    el.cibaStep2Card.classList.toggle("hidden", parActive);
    el.cibaStep3Card.classList.toggle("hidden", parActive);

    el.step2Card.classList.toggle("hidden", !parActive);
    el.tokenCard.classList.toggle("hidden", !parActive);
}

function updateCibaJourneyUi() {
    const showSign = state.cibaOnboarded === true;
    el.cibaPdfGroup.classList.toggle("hidden", !showSign);
    if (!showSign) {
        el.cibaPdf.value = "";
    }
}

function resetCibaCheckState() {
    state.cibaOnboarded = null;
    el.cibaWebfingerOut.textContent = "{}";
    setStatus(el.cibaIdentifierStatus, "muted", "");
}

function resetForStep1() {
    state.lastAuthorizationUrl = null;
    state.lastState = null;
    state.lastNonce = null;
    state.lastAccessToken = null;
    state.pendingCibaAuthReqId = null;
    state.lastSignedPdfBase64 = null;
    state.lastSignedPdfFilename = null;
    resetCibaCheckState();

    el.authLink.href = "#";
    setAuthLinkEnabled(false);
    setStatus(el.authStatus, "muted", "Create PAR first.");

    el.tokenOut.textContent = "{}";
    el.cibaTokenOut.textContent = "{}";
    el.signOut.textContent = "{}";
    el.cibaWebfingerOut.textContent = "{}";
    clearStatuses(el.tokenStatus, el.signStatus, el.cibaPollStatus);
    setCibaPollCountdown("", false);
    state.cibaPollingActive = false;
    state.cibaPollCancelled = false;
    el.btnCibaStopPoll.classList.add("hidden");

    setEnabled(el.btnDownloadPdf, false);
    clearStatuses(el.downloadStatus, el.cibaIdentifierStatus);
    el.btnCibaPollToken.disabled = true;
    el.btnCibaPollToken.classList.add("disabled");
    updateCibaJourneyUi();
}

function setCibaPollingUi(active) {
    state.cibaPollingActive = active;
    el.btnCibaStopPoll.classList.toggle("hidden", !active);

    const canStart = Boolean(state.pendingCibaAuthReqId) && !active;
    el.btnCibaPollToken.disabled = !canStart;
    el.btnCibaPollToken.classList.toggle("disabled", !canStart);
}

async function runWithDisabledButton(button, operation) {
    button.disabled = true;
    try {
        return await operation();
    } finally {
        button.disabled = false;
    }
}

function setCibaPollNotReady(message) {
    setStatus(el.cibaPollStatus, "err", message);
    setStatus(el.tokenStatus, "err", "CIBA token not ready");
    setCibaPollCountdown("", false);
}

function clearStatuses(...elements) {
    elements.forEach((element) => setStatus(element, "muted", ""));
}

async function checkCibaWebfinger(identifier) {
    const journey = getCibaWebfingerJourney(getSelectedCibaFamily());
    const response = await getJson(API.CIBA_WEBFINGER, {identifier, journey});
    el.cibaWebfingerOut.textContent = truncateForUi(safeJsonPretty(response.text));
    if (!response.ok || !response.json) {
        state.cibaOnboarded = null;
        setStatus(el.cibaIdentifierStatus, "err", "Webfinger check failed");
        updateCibaJourneyUi();
        return;
    }

    const json = response.json;
    state.cibaOnboarded = Boolean(json.onboarded);
    setStatus(
            el.cibaIdentifierStatus,
            state.cibaOnboarded ? "ok" : "err",
            json.message || (state.cibaOnboarded ? "Identifier onboarded" : "Identifier not onboarded")
    );
    updateCibaJourneyUi();
}

async function handleParSubmit(e) {
    e.preventDefault();
    el.parOut.textContent = "{}";
    setStatus(el.parStatus, "muted", "Calling /api/par/auth...");
    resetForStep1();

    const file = el.pdf.files[0];
    const journey = getCheckedValue("journey");
    if (!file) return setStatus(el.parStatus, "err", "Please select a PDF");
    if (!journey) return setStatus(el.parStatus, "err", "Please select a signing journey");

    try {
        await runWithDisabledButton(el.btnPar, async () => {
            const fd = new FormData();
            fd.append("pdf", file);
            fd.append("journey", journey);

            const response = await postMultipart(API.PAR_AUTH, fd);
            el.parOut.textContent = truncateForUi(safeJsonPretty(response.text));
            if (!response.ok) return setStatus(el.parStatus, "err", `PAR failed (HTTP ${response.status})`);
            if (!response.json) return setStatus(el.parStatus, "err", "PAR response is not valid JSON");

            const json = response.json;
            state.lastAuthorizationUrl = json.authorizationUrl;
            state.lastState = json.state;
            state.lastNonce = json.nonce;

            if (!state.lastAuthorizationUrl) return setStatus(el.parStatus, "err", "Missing authorizationUrl in /api/par/auth response");
            if (!state.lastState || !state.lastNonce) return setStatus(el.parStatus, "err", "Missing state/nonce in /api/par/auth response");

            el.authLink.href = state.lastAuthorizationUrl;
            setAuthLinkEnabled(true);
            setStatus(el.parStatus, "ok", "PAR created");
            setStatus(el.authStatus, "ok", "Ready - click Open QTSP Auth");
            el.authCode.focus();
        });
    } catch (err) {
        setStatus(el.parStatus, "err", "Network/JS error calling /api/par/auth");
        el.parOut.textContent = String(err);
    }
}

async function handleCibaCheckClick(e) {
    e.preventDefault();
    const identifier = (el.cibaIdentifier.value || "").trim();
    if (!identifier) return setStatus(el.cibaStatus, "err", "Please enter an identifier");
    if (!CIBA_IDENTIFIER_PATTERN.test(identifier)) return setStatus(el.cibaStatus, "err", CIBA_IDENTIFIER_ERROR);

    setStatus(el.cibaStatus, "muted", "Checking onboarding via /api/ciba/webfinger...");
    await checkCibaWebfinger(identifier);
    if (state.cibaOnboarded === true) {
        setStatus(el.cibaStatus, "ok", "Onboarded");
    } else if (state.cibaOnboarded === false) {
        setStatus(el.cibaStatus, "ok", "Not onboarded");
    }
}

async function startCibaAuth() {
    const identifier = (el.cibaIdentifier.value || "").trim();
    if (!identifier) return setStatus(el.cibaStatus, "err", "Please enter an identifier");
    if (!CIBA_IDENTIFIER_PATTERN.test(identifier)) return setStatus(el.cibaStatus, "err", CIBA_IDENTIFIER_ERROR);
    if (state.cibaOnboarded === null) return setStatus(el.cibaStatus, "err", "Please run Check first");

    const cibaFamily = getSelectedCibaFamily();
    const journey = getCibaAuthJourney(cibaFamily, state.cibaOnboarded === true);
    const isSign = journey === CIBA_AES_SIGN_JOURNEY || journey === CIBA_QES_SIGN_JOURNEY;
    const file = el.cibaPdf.files[0];
    if (isSign && state.cibaOnboarded !== true) {
        return setStatus(el.cibaStatus, "err", "Identifier is not onboarded for CIBA sign");
    }
    if (isSign && !file) {
        return setStatus(el.cibaStatus, "err", "Please select a PDF for CIBA sign");
    }

    setStatus(el.cibaStatus, "muted", "Calling /api/ciba/auth...");
    el.cibaOut.textContent = "{}";
    state.pendingCibaAuthReqId = null;
    state.cibaPollCancelled = false;
    setCibaPollingUi(false);
    setStatus(el.cibaPollStatus, "muted", "");
    try {
        await runWithDisabledButton(el.btnCibaAuth, async () => {
            const fd = new FormData();
            if (file) fd.append("pdf", file);
            fd.append("identifier", identifier);
            fd.append("journey", journey);

            const response = await postMultipart(API.CIBA_AUTH, fd);
            el.cibaOut.textContent = truncateForUi(safeJsonPretty(response.text));
            if (!response.ok) return setStatus(el.cibaStatus, "err", `CIBA failed (HTTP ${response.status})`);
            if (!response.json) return setStatus(el.cibaStatus, "err", "CIBA response is not valid JSON");

            const json = response.json;
            state.lastState = json.state || state.lastState;
            state.lastNonce = json.nonce || state.lastNonce;
            const authReqId = json.authReqId || json.auth_req_id || null;
            const identProcessData = json.identProcessData || json.ident_process_data || null;

            const intervalNum = Number(json.interval);
            const expiresNum = Number(json.expiresIn || json.expires_in);
            state.cibaPollIntervalSec = Number.isFinite(intervalNum) && intervalNum > 0 ? intervalNum : CIBA_DEFAULT_POLL_INTERVAL_SEC;
            const timeoutSec = Number.isFinite(expiresNum) && expiresNum > 0 ? expiresNum : CIBA_DEFAULT_POLL_TIMEOUT_SEC;
            state.cibaPollDeadlineAtMs = Date.now() + timeoutSec * 1000;

            if (!authReqId) return setStatus(el.cibaStatus, "err", "CIBA start succeeded but authReqId is missing");
            state.pendingCibaAuthReqId = authReqId;
            setCibaPollingUi(false);

            if (!isSign) {
                if (!identProcessData) return setStatus(el.cibaStatus, "err", "CIBA ident response missing identProcessData");
                setStatus(el.cibaStatus, "ok", "Opening ident process in new tab - you can start task 3 now.");
                const identTab = window.open("about:blank", "_blank");
                if (!identTab) return setStatus(el.cibaStatus, "err", "Popup blocked. Please allow popups and retry CIBA ident.");
                identTab.opener = null;
                identTab.location.href = identProcessData;
                return;
            }

            setStatus(el.cibaStatus, "ok", "CIBA task 2 succeeded - continue with task 3 (token polling)");
        });
    } catch (err) {
        setStatus(el.cibaStatus, "err", "Network/JS error calling /api/ciba/auth");
        el.cibaOut.textContent = String(err);
    }
}

function sleep(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

function setCibaPollCountdown(text, isActive) {
    el.cibaPollCountdown.textContent = text;
    el.cibaPollCountdown.classList.toggle("polling-active", Boolean(isActive));
}

async function waitForNextPoll(seconds, deadlineAtMs) {
    const waitUntil = Math.min(Date.now() + Math.max(0, seconds) * 1000, deadlineAtMs);
    let frame = 0;
    setCibaPollCountdown("", true);
    while (Date.now() < waitUntil && !state.cibaPollCancelled) {
        const remainSec = Math.max(0, (waitUntil - Date.now()) / 1000);
        const spinner = CIBA_SPINNER_FRAMES[frame % CIBA_SPINNER_FRAMES.length];
        setCibaPollCountdown(`next poll in ${remainSec.toFixed(1)}s ${spinner}`, true);
        frame += 1;
        await sleep(100);
    }
    setCibaPollCountdown("", false);
}

async function pollCibaTokenUiLoop(authReqId) {
    setStatus(el.cibaPollStatus, "muted", "CIBA polling started...");
    const deadlineAtMs = state.cibaPollDeadlineAtMs > 0
        ? state.cibaPollDeadlineAtMs
        : Date.now() + CIBA_DEFAULT_POLL_TIMEOUT_SEC * 1000;

    let attempt = 0;
    while (Date.now() < deadlineAtMs) {
        if (state.cibaPollCancelled) {
            setStatus(el.cibaPollStatus, "muted", "CIBA polling stopped");
            clearStatuses(el.tokenStatus);
            setCibaPollCountdown("", false);
            return;
        }

        attempt += 1;
        setStatus(el.tokenStatus, "muted", `Polling /api/ciba/token... (attempt ${attempt})`);
        const response = await postJson(API.CIBA_TOKEN, {
            state: state.lastState,
            nonce: state.lastNonce,
            authReqId
        });
        el.cibaTokenOut.textContent = truncateForUi(safeJsonPretty(response.text));

        if (!response.ok) {
            setCibaPollNotReady(`CIBA token polling failed (HTTP ${response.status})`);
            return;
        }
        if (!response.json) {
            setCibaPollNotReady("CIBA poll response is not valid JSON");
            return;
        }

        const pollJson = response.json;
        const status = String(pollJson.status || "").toUpperCase();
        if (status === CIBA_POLL_STATUS_READY) {
            const token = pollJson.token || {};
            state.lastAccessToken = token.accessToken || token.access_token || null;
            if (!state.lastAccessToken) {
                setCibaPollNotReady("CIBA token response missing accessToken");
                return;
            }
            setCibaPollCountdown("", false);
            setStatus(el.cibaPollStatus, "ok", "CIBA SAD token ready");
            setStatus(el.tokenStatus, "ok", "CIBA token OK (ready to sign)");
            return;
        }

        if (status !== CIBA_POLL_STATUS_PENDING) {
            setCibaPollNotReady(`Unexpected CIBA poll status: ${status || "<empty>"}`);
            return;
        }

        const nextPollInSec = Number(pollJson.nextPollInSec);
        const intervalSec = Number.isFinite(nextPollInSec) && nextPollInSec > 0
            ? nextPollInSec
            : state.cibaPollIntervalSec;
        await waitForNextPoll(intervalSec, deadlineAtMs);
    }

    setCibaPollNotReady("CIBA polling timed out");
}

async function handleTokenSubmit(e) {
    e.preventDefault();
    el.tokenOut.textContent = "{}";
    el.signOut.textContent = "{}";
    setStatus(el.tokenStatus, "muted", "Calling /api/par/token...");
    setStatus(el.signStatus, "muted", "");

    const code = (el.authCode.value || "").trim();
    if (!code) return setStatus(el.tokenStatus, "err", "Please paste the authorization code");
    if (!state.lastState || !state.lastNonce) return setStatus(el.tokenStatus, "err", "Missing state/nonce in UI - run step 1 again");

    try {
        await runWithDisabledButton(el.btnToken, async () => {
            const response = await postJson(API.PAR_TOKEN, {
                code,
                state: state.lastState,
                nonce: state.lastNonce
            });
            el.tokenOut.textContent = truncateForUi(safeJsonPretty(response.text));
            if (!response.ok) return setStatus(el.tokenStatus, "err", `Token exchange failed (HTTP ${response.status})`);
            if (!response.json) return setStatus(el.tokenStatus, "err", "Token response is not valid JSON");

            const json = response.json;
            state.lastAccessToken = json.accessToken || json.access_token || null;
            if (!state.lastAccessToken) return setStatus(el.tokenStatus, "err", "Token OK but missing accessToken in response");
            setStatus(el.tokenStatus, "ok", "Token OK (ready to sign)");
        });
    } catch (err) {
        setStatus(el.tokenStatus, "err", "Network/JS error calling /api/par/token");
        el.tokenOut.textContent = String(err);
    }
}

async function handleSignClick() {
    el.signOut.textContent = "{}";
    setStatus(el.signStatus, "muted", "Calling /api/etsi/sign...");

    if (!state.lastState || !state.lastNonce) return setStatus(el.signStatus, "err", "Missing state/nonce - run step 1 again");
    if (!state.lastAccessToken) return setStatus(el.signStatus, "err", "Missing access token - complete step 3 first");

    try {
        await runWithDisabledButton(el.btnSign, async () => {
            const response = await postJson(API.ETSI_SIGN, {
                state: state.lastState,
                nonce: state.lastNonce,
                sadJwt: state.lastAccessToken
            });
            el.signOut.textContent = truncateForUi(safeJsonPretty(response.text));
            if (!response.json) {
                setStatus(el.signStatus, "err", "ETSI sign response is not valid JSON");
                return;
            }

            const json = response.json;

            state.lastSignedPdfBase64 = json.signedPdf || null;
            state.lastSignedPdfFilename = json.responseId ? `signed-${json.responseId}.pdf` : "signed-document.pdf";
            setEnabled(el.btnDownloadPdf, Boolean(state.lastSignedPdfBase64));
            setStatus(el.downloadStatus, state.lastSignedPdfBase64 ? "ok" : "err", state.lastSignedPdfBase64 ? "Signed PDF ready to download" : "No signedPdfBase64 in response");

            if (!response.ok) return setStatus(el.signStatus, "err", `ETSI sign failed (HTTP ${response.status})`);
            setStatus(el.signStatus, "ok", "ETSI sign OK");
        });
    } catch (err) {
        setStatus(el.signStatus, "err", "Network/JS error calling /api/etsi/sign");
        el.signOut.textContent = String(err);
    }
}

function handleDownloadClick() {
    if (!state.lastSignedPdfBase64) return setStatus(el.downloadStatus, "err", "No signed PDF available - run step 4 first");
    try {
        downloadPdfFromBase64(state.lastSignedPdfBase64, state.lastSignedPdfFilename);
        setStatus(el.downloadStatus, "ok", "Download started");
    } catch {
        setStatus(el.downloadStatus, "err", "Failed to download PDF (base64 decode error?)");
    }
}

async function handleCibaPollTokenClick() {
    const authReqId = state.pendingCibaAuthReqId;
    if (!authReqId) {
        setStatus(el.cibaPollStatus, "err", "No CIBA authReqId available - run step 2 first");
        return;
    }

    if (state.cibaPollingActive) return;

    state.cibaPollCancelled = false;
    setCibaPollingUi(true);
    try {
        await pollCibaTokenUiLoop(authReqId);
    } finally {
        setCibaPollCountdown("", false);
        setCibaPollingUi(false);
    }
}

function handleCibaStopPollClick() {
    if (!state.cibaPollingActive) return;
    state.cibaPollCancelled = true;
    setCibaPollCountdown("", false);
    setStatus(el.cibaPollStatus, "muted", "Stopping CIBA polling...");
}

function init() {
    setEnabled(el.btnDownloadPdf, false);
    setAuthLinkEnabled(false);
    el.authLink.href = "#";

    el.tabPar.addEventListener("click", () => setStep1Tab("par"));
    el.tabCiba.addEventListener("click", () => setStep1Tab("ciba"));
    el.parForm.addEventListener("submit", handleParSubmit);
    el.cibaForm.addEventListener("submit", (e) => e.preventDefault());
    el.btnCibaCheck.addEventListener("click", handleCibaCheckClick);
    el.btnCibaAuth.addEventListener("click", startCibaAuth);
    el.btnCibaPollToken.addEventListener("click", handleCibaPollTokenClick);
    el.btnCibaStopPoll.addEventListener("click", handleCibaStopPollClick);
    el.tokenForm.addEventListener("submit", handleTokenSubmit);
    el.btnSign.addEventListener("click", handleSignClick);
    el.btnDownloadPdf.addEventListener("click", handleDownloadClick);
    el.authLink.addEventListener("click", (e) => {
        if (!state.lastAuthorizationUrl) {
            e.preventDefault();
            setStatus(el.authStatus, "err", "Create PAR first.");
        }
    });

    el.cibaIdentifier.addEventListener("input", () => {
        resetCibaCheckState();
        updateCibaJourneyUi();
    });

    document.querySelectorAll('input[name="cibaJourney"]').forEach((radio) => {
        radio.addEventListener("change", () => {
            resetCibaCheckState();
            updateCibaJourneyUi();
        });
    });

    setStep1Tab("par");
    updateCibaJourneyUi();
}

init();

