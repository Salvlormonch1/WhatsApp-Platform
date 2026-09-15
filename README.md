# SaaS Platform — WhatsApp AI Assistant

Plataforma de gestión de negocios con asistente IA integrado vía WhatsApp.

## 🚀 Despliegue en Render (producción)

### Paso 1 — Sube el código a GitHub

```bash
git init
git add .
git commit -m "Initial commit"
```

Crea un repositorio en [github.com/new](https://github.com/new) y luego:

```bash
git remote add origin https://github.com/TU_USUARIO/saas-platform.git
git branch -M main
git push -u origin main
```

### Paso 2 — Conecta con Render

1. Ve a [dashboard.render.com](https://dashboard.render.com)
2. Haz clic en **New → Blueprint**
3. Conecta tu cuenta de GitHub y selecciona el repositorio
4. Render detecta el `render.yaml` y crea: PostgreSQL + Backend Quarkus + Frontend Next.js
5. Haz clic en **Apply**

### Paso 3 — Variables de entorno (completar manualmente)

#### En `saas-backend`:

| Variable | Valor |
|---|---|
| `QUARKUS_DATASOURCE_JDBC_URL` | Copia "Internal Database URL" → cambia `postgres://` por `jdbc:postgresql://` |
| `CORS_ORIGINS` | URL del frontend (ej: `https://saas-frontend.onrender.com`) |
| `WHATSAPP_VERIFY_TOKEN` | El token que usarás en Meta for Developers |
| `WHATSAPP_APP_SECRET` | App Secret de Meta (Settings → Basic) |
| `GEMINI_API_KEY` | Tu clave de Google AI Studio |
| `SMTP_USERNAME` | Tu correo Gmail |
| `SMTP_PASSWORD` | App Password de Gmail |
| `SMTP_FROM_ADDRESS` | Tu correo Gmail |

#### En `saas-frontend`:

| Variable | Valor |
|---|---|
| `NEXT_PUBLIC_API_URL` | `https://saas-backend.onrender.com/api/v1` |
| `INTERNAL_API_URL` | Igual que `NEXT_PUBLIC_API_URL` |
| `NEXTAUTH_URL` | `https://saas-frontend.onrender.com` |

### Paso 4 — Webhook de WhatsApp

1. Meta for Developers → tu app → WhatsApp → Configuración → Webhooks → Editar
2. **URL**: `https://saas-backend.onrender.com/api/v1/whatsapp/webhook`
3. **Token**: el mismo que pusiste en `WHATSAPP_VERIFY_TOKEN`
4. Activa el campo `messages`

---

## 💻 Desarrollo local

```bash
docker-compose -f docker-compose.dev.yml up -d
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8090
- Emails (MailHog): http://localhost:8025

---

## ⚠️ Notas

- **Plan gratuito Render**: el backend duerme 15 min sin tráfico → cold start ~30s. Usar Starter ($7/mes) para producción.
- **Claves RSA JWT**: se generan automáticamente al arrancar. Si el servicio se redeploya, los usuarios deben volver a iniciar sesión.
- **PostgreSQL gratuito**: disponible 90 días, luego $7/mes.
