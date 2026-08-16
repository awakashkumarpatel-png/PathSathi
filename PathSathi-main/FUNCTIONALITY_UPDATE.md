Path Sathi consolidated functionality pass

- Home quick access now includes Live Map and My Trips.
- Trips can be cancelled or deleted with confirmation.
- Travel memories can be deleted.
- Memory delete is stored offline through Room DAO/repository.
- Removed stray AIService.kt.tmp artifact.
- Existing offline-first architecture and optional online providers are preserved.

Build verification: source-level diff check completed. Android SDK/Gradle build was not run in this environment.
