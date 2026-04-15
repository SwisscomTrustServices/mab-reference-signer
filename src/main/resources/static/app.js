// ---------- helpers ----------
function setEnabled(btn, enabled) {
    btn.disabled = !enabled;
    if (enabled) btn.classList.remove("disabled");
    else btn.classList.add("disabled");
}

function downloadPdfFromBase64(base64, filename) {
    const byteCharacters = atob(base64);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);

    const blob = new Blob([byteArray], { type: "application/pdf" });
    const url = URL.createObjectURL(blob);

    const a = document.createElement("a");
    a.href = url;
    a.download = filename || "signed-document.pdf";
    document.body.appendChild(a);
    a.click();

    URL.revokeObjectURL(url);
    a.remove();
}

function setStatus(el, cls, text) {
    el.className = cls || "muted";
    el.textContent = text || "";
}

function safeJsonPretty(text) {
    if (!text) return "(empty body)";
    try {
        return JSON.stringify(JSON.parse(text), null, 2);
    } catch {
        return text;
    }
}

function truncateForUi(str, max = 4000) {
    if (!str) return str;
    return str.length > max ? (str.substring(0, max) + "\n…(truncated)…") : str;
}

function setAuthLinkEnabled(enabled) {
    const link = document.getElementById("authLink");
    if (enabled) link.classList.remove("disabled");
    else link.classList.add("disabled");
}

function getSelectedJourney() {
    const el = document.querySelector('input[name="journey"]:checked');
    return el ? el.value : null;
}

// ---------- elements ----------
const parForm = document.getElementById('parForm');
const tokenForm = document.getElementById('tokenForm');

const btnPar = document.getElementById('btnPar');
const btnToken = document.getElementById('btnToken');
const btnSign = document.getElementById('btnSign');

const parStatus = document.getElementById('parStatus');
const tokenStatus = document.getElementById('tokenStatus');
const signStatus = document.getElementById('signStatus');

const authStatus = document.getElementById('authStatus');
const authLink = document.getElementById('authLink');

const parOut = document.getElementById('parOut');
const tokenOut = document.getElementById('tokenOut');
const signOut = document.getElementById('signOut');

const authCode = document.getElementById('authCode');

const btnDownloadPdf = document.getElementById('btnDownloadPdf');
const downloadStatus = document.getElementById('downloadStatus');
setEnabled(btnDownloadPdf, false);

// ---------- state we keep in the browser ----------
let lastAuthorizationUrl = null;
let lastState = null;
let lastNonce = null;
let lastClientSessionId = null;
let lastAccessToken = null;
let lastSignedPdfBase64 = null;
let lastSignedPdfFilename = null;

// Start disabled until PAR succeeded
setAuthLinkEnabled(false);
authLink.href = "#";

// ---------- Step 1: PAR ----------
parForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    parOut.textContent = "{}";
    setStatus(parStatus, "muted", "Calling /api/par…");

    // Reset following steps
    lastAuthorizationUrl = null;
    lastState = null;
    lastNonce = null;
    lastClientSessionId = null;
    lastAccessToken = null;
    tokenOut.textContent = "{}";
    signOut.textContent = "{}";
    setStatus(tokenStatus, "muted", "");
    setStatus(signStatus, "muted", "");

    authLink.href = "#";
    setAuthLinkEnabled(false);
    setStatus(authStatus, "muted", "Create PAR first.");

    lastSignedPdfBase64 = null;
    lastSignedPdfFilename = null;
    setEnabled(btnDownloadPdf, false);
    setStatus(downloadStatus, "muted", "");

    const file = document.getElementById('pdf').files[0];
    if (!file) {
        setStatus(parStatus, "err", "Please select a PDF");
        return;
    }

    const journey = getSelectedJourney();
    if (!journey) {
        setStatus(parStatus, "err", "Please select a signing journey");
        return;
    }

    btnPar.disabled = true;

    try {
        const fd = new FormData();
        fd.append("pdf", file);
        fd.append("journey", journey);

        const res = await fetch("/api/par", {
            method: "POST",
            body: fd,
            headers: {"Accept": "application/json"}
        });

        const text = await res.text();
        parOut.textContent = truncateForUi(safeJsonPretty(text));

        if (!res.ok) {
            setStatus(parStatus, "err", `PAR failed (HTTP ${res.status})`);
            return;
        }

        const json = JSON.parse(text);

        lastAuthorizationUrl = json.authorizationUrl;
        lastState = json.state;
        lastNonce = json.nonce;
        lastClientSessionId = json.clientSessionId;

        if (!lastAuthorizationUrl) {
            setStatus(parStatus, "err", "Missing authorizationUrl in /api/par response");
            return;
        }
        if (!lastState || !lastNonce) {
            setStatus(parStatus, "err", "Missing state/nonce in /api/par response (UI needs these for later steps)");
            return;
        }

        // Enable step 2
        authLink.href = lastAuthorizationUrl;
        setAuthLinkEnabled(true);
        setStatus(parStatus, "ok", "PAR created");
        setStatus(authStatus, "ok", "Ready — click “Open QTSP Auth”");

        authCode.focus();

    } catch (err) {
        setStatus(parStatus, "err", "Network/JS error calling /api/par");
        parOut.textContent = String(err);
    } finally {
        btnPar.disabled = false;
    }
});

// Prevent accidental navigation when disabled
authLink.addEventListener("click", (e) => {
    if (!lastAuthorizationUrl) {
        e.preventDefault();
        setStatus(authStatus, "err", "Create PAR first.");
    } else {
        setStatus(authStatus, "ok", "Auth opened — after redirect, copy code and paste in step 3.");
    }
});

// ---------- Step 3: Token ----------
tokenForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    tokenOut.textContent = "{}";
    signOut.textContent = "{}";
    setStatus(tokenStatus, "muted", "Calling /api/token…");
    setStatus(signStatus, "muted", "");

    const code = (authCode.value || "").trim();
    if (!code) {
        setStatus(tokenStatus, "err", "Please paste the authorization code");
        return;
    }
    if (!lastState || !lastNonce) {
        setStatus(tokenStatus, "err", "Missing state/nonce in UI — run step 1 again");
        return;
    }

    btnToken.disabled = true;

    try {
        const res = await fetch("/api/token", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json"
            },
            body: JSON.stringify({code, state: lastState, nonce: lastNonce})
        });

        const text = await res.text();
        tokenOut.textContent = truncateForUi(safeJsonPretty(text));

        if (!res.ok) {
            setStatus(tokenStatus, "err", `Token exchange failed (HTTP ${res.status})`);
            return;
        }

        const json = JSON.parse(text);
        lastAccessToken = json.accessToken || json.access_token || null;

        if (!lastAccessToken) {
            setStatus(tokenStatus, "err", "Token OK but missing accessToken in response");
            return;
        }

        setStatus(tokenStatus, "ok", "Token OK (ready to sign)");

    } catch (err) {
        setStatus(tokenStatus, "err", "Network/JS error calling /api/token");
        tokenOut.textContent = String(err);
    } finally {
        btnToken.disabled = false;
    }
});

// ---------- Step 4: ETSI Sign ----------
btnSign.addEventListener('click', async () => {
    signOut.textContent = "{}";
    setStatus(signStatus, "muted", "Calling /api/etsi/sign…");

    if (!lastState || !lastNonce) {
        setStatus(signStatus, "err", "Missing state/nonce — run step 1 again");
        return;
    }
    if (!lastAccessToken) {
        setStatus(signStatus, "err", "Missing access token — complete step 3 first");
        return;
    }

    btnSign.disabled = true;

    try {
        const res = await fetch("/api/etsi/sign", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json"
            },
            body: JSON.stringify({
                state: lastState,
                nonce: lastNonce,
                sadJwt: lastAccessToken
            })
        });

        const text = await res.text();

        signOut.textContent = truncateForUi(safeJsonPretty(text));

        const json = JSON.parse(text);

        lastSignedPdfBase64 = json.signedPdf || null;
        lastSignedPdfFilename = json.responseId ? `signed-${json.responseId}.pdf` : "signed-document.pdf";

        if (!lastSignedPdfBase64) {
            setEnabled(btnDownloadPdf, false);
            setStatus(downloadStatus, "err", "No signedPdfBase64 in response");
        } else {
            setEnabled(btnDownloadPdf, true);
            setStatus(downloadStatus, "ok", "Signed PDF ready to download");
        }

        if (!res.ok) {
            setStatus(signStatus, "err", `ETSI sign failed (HTTP ${res.status})`);
            return;
        }

        setStatus(signStatus, "ok", "ETSI sign OK");

    } catch (err) {
        setStatus(signStatus, "err", "Network/JS error calling /api/etsi/sign");
        signOut.textContent = String(err);
    } finally {
        btnSign.disabled = false;
    }

    btnDownloadPdf.addEventListener('click', () => {
        if (!lastSignedPdfBase64) {
            setStatus(downloadStatus, "err", "No signed PDF available — run step 4 first");
            return;
        }
        try {
            downloadPdfFromBase64(lastSignedPdfBase64, lastSignedPdfFilename);
            setStatus(downloadStatus, "ok", "Download started");
        } catch (e) {
            setStatus(downloadStatus, "err", "Failed to download PDF (base64 decode error?)");
        }
    });
});

