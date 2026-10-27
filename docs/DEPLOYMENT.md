# Deploying MediConnect for free

| Part | Host | Plan |
|---|---|---|
| React website | GitHub Pages (`.github/workflows/pages.yml`) | Free |
| Java API + Python AI service | Render, from `render.yaml` | Free (sleeps after 15 min idle) |
| MySQL 8 | Aiven | Free |
| MongoDB | MongoDB Atlas | Free M0 |

The site and the API live on different domains, so the API runs with `CROSS_SITE_AUTH=true`: the
login cookie becomes `SameSite=None; Secure`, and the token is also returned to the site, which sends
it as a bearer header (Safari and others block third-party cookies). CORS allows only the Pages origin.

## 1. MySQL on Aiven
1. Sign up at https://aiven.io and create a **MySQL** service on the **Free** plan.
2. From the service overview copy host, port, user (`avnadmin`) and password.
3. The JDBC URL is
   `jdbc:mysql://<host>:<port>/defaultdb?sslMode=REQUIRED&allowPublicKeyRetrieval=true`

## 2. MongoDB on Atlas
1. Sign up at https://www.mongodb.com/cloud/atlas and create a free **M0** cluster.
2. Database Access: add a user with a password.
3. Network Access: allow `0.0.0.0/0` (Render free services have no fixed IP).
4. Connect → Drivers: copy the `mongodb+srv://…` string and add the database name:
   `mongodb+srv://<user>:<password>@<cluster>/mediconnect?retryWrites=true&w=majority`

## 3. API and AI service on Render
1. Sign up at https://render.com with GitHub.
2. New → **Blueprint** → choose the `mediconnect` repository. Render reads `render.yaml` and creates
   `mediconnect-ai` and `mediconnect-api`.
3. Fill in the secret values: `DB_URL`, `DB_USER`, `DB_PASSWORD` (Aiven), `MONGO_URI` (Atlas), and
   optionally `OPENAI_API_KEY`. `JWT_SECRET` is generated for you.
4. Wait for both deploys. The first API start runs the Flyway migrations and loads the demo data.
5. If Render gives the services different addresses, update `AI_SERVICE_URL` on the API to the AI
   service's URL.

## 4. Website on GitHub Pages
1. Repository Settings → Secrets and variables → Actions → Variables: set `API_URL` to the API's
   Render address, e.g. `https://mediconnect-api.onrender.com`.
2. Actions → Pages → Run workflow (it also runs on every push that changes `client/`).
3. The site is at https://ayushcoder894.github.io/mediconnect/

## Checking it
- `https://<api>/actuator/health` returns `{"status":"UP"}`
- `https://<ai>/health` returns the model version and indexed policy count
- Log in on the site with `patient@mediconnect.dev` / `Demo@1234`

Free Render services sleep after 15 minutes without traffic; the first request then takes about a
minute, and the site shows a notice while it waits.
