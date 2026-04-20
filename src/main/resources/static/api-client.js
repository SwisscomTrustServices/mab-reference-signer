function parseJsonOrNull(text) {
    if (!text) return null;
    try {
        return JSON.parse(text);
    } catch {
        return null;
    }
}

async function request(url, options) {
    const res = await fetch(url, options);
    const text = await res.text();
    return {
        ok: res.ok,
        status: res.status,
        text,
        json: parseJsonOrNull(text)
    };
}

export function postMultipart(url, formData) {
    return request(url, {
        method: "POST",
        body: formData,
        headers: {Accept: "application/json"}
    });
}

export function postJson(url, payload) {
    return request(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Accept: "application/json"
        },
        body: JSON.stringify(payload)
    });
}

export function getJson(url, params = {}) {
    const qs = new URLSearchParams(params).toString();
    const fullUrl = qs ? `${url}?${qs}` : url;
    return request(fullUrl, {
        method: "GET",
        headers: {Accept: "application/json"}
    });
}

