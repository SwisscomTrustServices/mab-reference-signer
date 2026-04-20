export function setEnabled(btn, enabled) {
    btn.disabled = !enabled;
    btn.classList.toggle("disabled", !enabled);
}

export function setStatus(el, cls, text) {
    el.className = cls || "muted";
    el.textContent = text || "";
}

export function safeJsonPretty(text) {
    if (!text) return "(empty body)";
    try {
        return JSON.stringify(JSON.parse(text), null, 2);
    } catch {
        return text;
    }
}

export function truncateForUi(str, max = 4000) {
    if (!str) return str;
    return str.length > max ? str.substring(0, max) + "\n...(truncated)..." : str;
}

export function setAuthLinkEnabled(enabled) {
    const link = document.getElementById("authLink");
    link.classList.toggle("disabled", !enabled);
}

export function getCheckedValue(name) {
    const el = document.querySelector(`input[name="${name}"]:checked`);
    return el ? el.value : null;
}

export function downloadPdfFromBase64(base64, filename) {
    const byteCharacters = atob(base64);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
    }

    const blob = new Blob([new Uint8Array(byteNumbers)], {type: "application/pdf"});
    const url = URL.createObjectURL(blob);

    const a = document.createElement("a");
    a.href = url;
    a.download = filename || "signed-document.pdf";
    document.body.appendChild(a);
    a.click();

    URL.revokeObjectURL(url);
    a.remove();
}

