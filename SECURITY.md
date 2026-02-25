# Security

## Reporting a vulnerability

If you discover a security issue, please report it responsibly. Do not open a public issue.

- **Option A:** Email the maintainers (see repository contacts or CONTRIBUTING.md).
- **Option B:** If this repo is on GitHub, use [GitHub Security Advisories](https://docs.github.com/en/code-security/security-advisories) (Security tab → Advisories).

Include a clear description, steps to reproduce, and impact. We will acknowledge and work on a fix.

## Secure development

- **Secrets:** Do not commit API keys, Firebase config (`google-services.json`), or `.env.local` with real values. Use `.env.example` and document required variables. Firebase keys in the web app are public by design (`NEXT_PUBLIC_*`); restrict usage with Firestore/Storage rules.
- **Dependencies:** Keep dependencies up to date. Dependabot is enabled for this repo; review and merge security-related PRs.
