interface ThomswatchRuntimeConfig {
  apiBaseUrl?: string;
}

declare global {
  interface Window {
    __THOMSWATCH_CONFIG__?: ThomswatchRuntimeConfig;
  }
}

export function apiUrl(path: string): string {
  const baseUrl = window.__THOMSWATCH_CONFIG__?.apiBaseUrl?.trim().replace(/\/+$/, '') ?? '';
  return `${baseUrl}${path}`;
}
