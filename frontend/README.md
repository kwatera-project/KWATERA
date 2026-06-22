[← Back to the project README](../README.md)

# Frontend

The KWATERA frontend is a React and TypeScript application built with Vite. It provides guest booking, owner management, administration, billing, reporting, and meter-reading user flows. The production container builds the application with Bun and serves the static files through nginx.

## Main responsibilities

- Render guest, owner, and administrator interfaces.
- Handle authentication state and role-specific navigation.
- Send API requests through the API Gateway.
- Provide a static demo mode for the GitHub Pages deployment.

## Default port

After Docker Compose startup: [http://localhost:5173](http://localhost:5173)

API calls use `VITE_API_BASE_URL` when set and otherwise target `http://localhost:8090`.

## Local development

Bun is the supported local package manager:

```bash
bun install --frozen-lockfile
bun run dev
```

Run `bun run lint` and `bun run build` before submitting frontend changes.
