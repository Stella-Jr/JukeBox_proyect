export const API_URL = "http://localhost:8080/api";

export async function apiRegister(username: string, password: string) {
  const res = await fetch(`${API_URL}/rooms/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => null);
    throw new Error(err?.message || "Error al registrar usuario");
  }
  return res.json();
}

export async function apiLogin(username: string, password: string) {
  const res = await fetch(`${API_URL}/rooms/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => null);
    throw new Error(err?.message || "Credenciales invalidas");
  }
  return res.json();
}
