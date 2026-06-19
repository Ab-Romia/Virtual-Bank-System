# Virtual Bank Frontend

A single-page web client for the Virtual Bank API. Sign in, view accounts,
open accounts, deposit funds, make transfers, and follow each transfer to its
outcome with its audit history.

## Stack

- React 19 with Vite and TypeScript
- TanStack Query for server state
- Zustand for the auth session
- Tailwind CSS v4
- axios with a token interceptor

## Running

```bash
npm install
npm run dev
```

The dev server runs on http://localhost:5173, which is the origin the gateway
allows for CORS.

## Configuration

The client talks to the gateway through a single base URL. Set it with
`VITE_API_BASE`; it defaults to `http://localhost:8080/api`.

```bash
cp .env.example .env
# then edit VITE_API_BASE if your gateway is elsewhere
```

The JWT from login is stored client-side and attached as
`Authorization: Bearer <token>` on every request. A `401` clears the session and
returns you to the login screen.

## Scripts

```bash
npm run dev      # start the dev server
npm run build    # type-check and produce a production build
npm run preview  # serve the production build locally
npm run lint     # run ESLint
npm test         # run the unit tests (Vitest)
```

## Structure

```
src/
  api/         axios client, typed endpoints, query hooks, API types
  auth/        Zustand session store and the route guard
  components/  UI primitives, app shell, transfer form and progress
  pages/       login, dashboard, transfer, assistant
  lib/         money and date formatting, UUID helper
  test/        Vitest setup and component tests
```
