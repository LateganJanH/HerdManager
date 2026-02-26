"use client";

/**
 * Catches errors in the root layout (including ChunkLoadError when layout chunk times out).
 * Must define its own <html> and <body>.
 */

const APP_NAME = "HerdManager";

function isChunkLoadError(error: Error): boolean {
  const name = error?.name ?? "";
  const message = (error?.message ?? "").toLowerCase();
  return (
    name === "ChunkLoadError" ||
    message.includes("loading chunk") ||
    message.includes("chunk load failed") ||
    message.includes("timeout")
  );
}

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  const chunkError = isChunkLoadError(error);

  return (
    <html lang="en">
      <body style={{ margin: 0, fontFamily: "system-ui, sans-serif", background: "#faf9f6", color: "#1c1917", minHeight: "100vh", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: "1rem" }}>
        <p style={{ fontSize: "0.875rem", fontWeight: 600, color: "#44403c", textTransform: "uppercase", letterSpacing: "0.05em" }}>
          {APP_NAME}
        </p>
        <h1 style={{ marginTop: "1rem", fontSize: "1.25rem", fontWeight: 700 }}>
          Something went wrong
        </h1>
        <p style={{ marginTop: "0.5rem", color: "#57534e", textAlign: "center", maxWidth: "28rem" }}>
          {chunkError
            ? "A script failed to load (often due to a slow connection or after an update). Refreshing the page usually fixes it."
            : "An unexpected error occurred. You can try again or refresh the page."}
        </p>
        <div style={{ marginTop: "1.5rem", display: "flex", gap: "0.75rem", flexWrap: "wrap", justifyContent: "center" }}>
          <button
            type="button"
            onClick={() => window.location.reload()}
            style={{
              padding: "0.5rem 1rem",
              fontWeight: 500,
              backgroundColor: "#1b4332",
              color: "#fff",
              border: "none",
              borderRadius: "0.375rem",
              cursor: "pointer",
            }}
          >
            Refresh the page
          </button>
          {!chunkError && (
            <button
              type="button"
              onClick={reset}
              style={{
                padding: "0.5rem 1rem",
                fontWeight: 500,
                backgroundColor: "transparent",
                color: "#44403c",
                border: "1px solid #a8a29e",
                borderRadius: "0.375rem",
                cursor: "pointer",
              }}
            >
              Try again
            </button>
          )}
        </div>
      </body>
    </html>
  );
}
