# GitHub sync setup

Use this guide to connect HerdManager to GitHub and keep the repo in sync.

## One-time setup

### 1. Initialize Git (if not already)

From the project root (`HerdManager/`):

```bash
git init
```

### 2. Create a repo on GitHub

- Go to [GitHub](https://github.com/new).
- Create a new repository (e.g. `HerdManager`).
- Do **not** add a README, .gitignore, or license if you want to push this existing project.

### 3. Add remote and push

Replace `YOUR_USERNAME` and `YOUR_REPO` with your GitHub user/org and repo name:

```bash
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
```

Or with SSH:

```bash
git remote add origin git@github.com:YOUR_USERNAME/YOUR_REPO.git
```

Stage and commit, then push (use `main` or `master` to match your default branch):

```bash
git add .
git status
git commit -m "Initial commit: Android app, web dashboard, docs"
git branch -M main
git push -u origin main
```

## After setup

- **Pull latest:** `git pull`
- **Push changes:** `git add .` (or specific files), `git commit -m "message"`, `git push`
- **Check remote:** `git remote -v`

## Notes

- **`.gitignore`** already excludes build outputs, `node_modules/`, `android/.gradle/`, `google-services.json`, and local env files. Do not commit `google-services.json`; each developer adds their own (see [android/FIREBASE-SETUP.md](../android/FIREBASE-SETUP.md)).
- For **Android** to build after clone, add `android/app/google-services.json` from Firebase Console or the build will fail at `processDebugGoogleServices`.
