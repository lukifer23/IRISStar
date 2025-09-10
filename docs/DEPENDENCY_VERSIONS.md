# Dependency Version Management

This project centralizes plugin and library versions in a Gradle version catalog located at `gradle/libs.versions.toml`.

## Bumping a version

1. Open `gradle/libs.versions.toml`.
2. Update the desired entry in the `[versions]` section.
3. If needed, adjust corresponding entries in `[libraries]` or `[plugins]` when coordinates change.
4. Run the Gradle build to apply the update:
   ```bash
   ./gradlew build
   ```

All modules (`app` and `llama`) read their versions from this catalog, so updating the catalog updates the entire project.
