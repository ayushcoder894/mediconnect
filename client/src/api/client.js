/**
 * Thin fetch wrapper.
 *
 * Same-origin deployment (Docker + nginx): the JWT lives in an httpOnly cookie set by
 * the API, so scripts never see it. Hosted split deployment (site on GitHub Pages, API
 * on another domain, VITE_API_URL set): browsers may drop that cross-site cookie, so the
 * token returned at login is kept for this tab and sent as a bearer header.
 */
export const API_URL = (import.meta.env.VITE_API_URL || "").replace(/\/$/, "");
export const HOSTED = API_URL !== "";
const TOKEN_KEY = "mc_token";

export class ApiError extends Error {
  constructor(status, message, fields) {
    super(message);
    this.status = status;
    this.fields = fields || {};
  }
}

function readToken() {
  try {
    return HOSTED ? sessionStorage.getItem(TOKEN_KEY) : null;
  } catch {
    return null;
  }
}

export function setToken(token) {
  if (!HOSTED) return;
  try {
    if (token) sessionStorage.setItem(TOKEN_KEY, token);
    else sessionStorage.removeItem(TOKEN_KEY);
  } catch {
    // storage blocked (private mode): the cookie may still work
  }
}

function headers(json) {
  const h = {};
  if (json) h["Content-Type"] = "application/json";
  const token = readToken();
  if (token) h.Authorization = `Bearer ${token}`;
  return h;
}

async function request(method, path, body) {
  const res = await fetch(API_URL + path, {
    method,
    credentials: "include",
    headers: headers(body !== undefined),
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  if (res.status === 204) return null;
  const text = await res.text();
  const data = text ? safeJson(text) : null;
  if (!res.ok) {
    throw new ApiError(res.status, data?.message || "Something went wrong. Please try again.", data?.fields);
  }
  return data;
}

function safeJson(text) {
  try {
    return JSON.parse(text);
  } catch {
    return { message: text };
  }
}

export const api = {
  get: (path) => request("GET", path),
  post: (path, body = {}) => request("POST", path, body),
  put: (path, body) => request("PUT", path, body),
  patch: (path, body) => request("PATCH", path, body),
  del: (path) => request("DELETE", path),
};

/**
 * Opens a PDF endpoint in a new tab. Same-origin: a plain link (the cookie goes along).
 * Hosted: fetch it with the bearer header and open the downloaded file.
 */
export async function openPdf(path) {
  if (!HOSTED) {
    window.open(path, "_blank", "noopener");
    return;
  }
  // Open the tab synchronously so popup blockers allow it, then point it at the file
  const tab = window.open("", "_blank");
  const res = await fetch(API_URL + path, { credentials: "include", headers: headers(false) });
  if (!res.ok) {
    tab?.close();
    throw new ApiError(res.status, "Could not download the PDF");
  }
  const url = URL.createObjectURL(await res.blob());
  if (tab) tab.location.href = url;
  else window.location.href = url;
}

export function query(params) {
  const q = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== "") q.set(k, v);
  });
  const s = q.toString();
  return s ? `?${s}` : "";
}
