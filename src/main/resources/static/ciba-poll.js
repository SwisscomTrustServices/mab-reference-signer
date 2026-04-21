import {setStatus} from "/ui-utils.js";
import {postJson} from "/api-client.js";
import {truncateForUi, safeJsonPretty} from "/ui-utils.js";
import {el, state} from "/dom.js";
import {
    API,
    CIBA_DEFAULT_POLL_TIMEOUT_SEC,
    CIBA_POLL_STATUS_PENDING,
    CIBA_POLL_STATUS_READY
} from "/constants.js";

const SPINNER_FRAMES = ["-", "\\", "|", "/"];

function sleep(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

export function setCibaPollCountdown(text, isActive) {
    el.cibaPollCountdown.textContent = text;
    el.cibaPollCountdown.classList.toggle("polling-active", Boolean(isActive));
}

export function setCibaPollingUi(active) {
    state.cibaPollingActive = active;
    el.btnCibaStopPoll.classList.toggle("hidden", !active);

    const canStart = Boolean(state.pendingCibaAuthReqId) && !active;
    el.btnCibaPollToken.disabled = !canStart;
    el.btnCibaPollToken.classList.toggle("disabled", !canStart);
}

async function waitForNextPoll(seconds, deadlineAtMs) {
    const waitUntil = Math.min(Date.now() + Math.max(0, seconds) * 1000, deadlineAtMs);
    let frame = 0;
    setCibaPollCountdown("", true);
    while (Date.now() < waitUntil && !state.cibaPollCancelled) {
        const remainSec = Math.max(0, (waitUntil - Date.now()) / 1000);
        const spinner = SPINNER_FRAMES[frame % SPINNER_FRAMES.length];
        setCibaPollCountdown(`next poll in ${remainSec.toFixed(1)}s ${spinner}`, true);
        frame += 1;
        await sleep(100);
    }
    setCibaPollCountdown("", false);
}

function setCibaPollNotReady(message) {
    setStatus(el.cibaPollStatus, "err", message);
    setStatus(el.tokenStatus, "err", "CIBA token not ready");
    setCibaPollCountdown("", false);
}

export async function pollCibaTokenUiLoop(authReqId) {
    setStatus(el.cibaPollStatus, "muted", "CIBA polling started...");
    const deadlineAtMs = state.cibaPollDeadlineAtMs > 0
        ? state.cibaPollDeadlineAtMs
        : Date.now() + CIBA_DEFAULT_POLL_TIMEOUT_SEC * 1000;

    let attempt = 0;
    while (Date.now() < deadlineAtMs) {
        if (state.cibaPollCancelled) {
            setStatus(el.cibaPollStatus, "muted", "CIBA polling stopped");
            setStatus(el.tokenStatus, "muted", "");
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

