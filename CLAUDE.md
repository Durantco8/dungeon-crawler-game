# Dungeon Crawler — Project Rules

## About this project

A 2D tile-based dungeon crawler in Java 24 + JavaFX 21, built with Maven.
Originally a university assignment; being rebuilt into a portfolio project.
Architecture is MVC with the Observer pattern.

I am the author of this project. You are a tool I am using to build it.

## Git rules — IMPORTANT

- **Never** add `Co-Authored-By: Claude`, `🤖 Generated with [Claude Code]`,
  `Generated with Claude Code`, or `Claude-Session:` to any commit message,
  PR title, or PR body. No attribution footers of any kind.
- Commit messages end with the last line of the body. Nothing appended.
- Use conventional commit prefixes: `feat:`, `fix:`, `refactor:`, `test:`,
  `docs:`, `chore:`. Subject line under 72 characters.
- Work on feature branches, one per phase. Never commit directly to `main`.
- Run `mvn test` before every commit. Do not commit failing tests.
- Never force-push. Never rewrite published history.
- Ask before pushing to the remote or opening a PR.

## Code standards

- Java 24, 2-space indentation, Google Java Format conventions.
- JUnit 5 for tests. The existing `pom.xml` has JUnit 4 — migrate it.
- **Model and board code must never import JavaFX.** Tests for game logic
  run headless in milliseconds.
- Dependency injection over static state. Anything nondeterministic
  (RNG, clock) gets injected through the constructor so it can be faked.
- No `instanceof` chains for dispatch. Use polymorphism or the visitor pattern.
- Prefer immutable value types. `Posn` should be a record.
- Javadoc on every public interface method. Skip it on obvious getters.

## Testing standards

- Every behavior change ships with tests in the same commit.
- Test the model and board directly, never through the view.
- Use seeded RNG so every test is reproducible.
- Aim for meaningful coverage of game logic, not coverage percentage theater.

## Working style

- Before starting a phase, show me the plan and wait for approval.
- Make one logical change per commit. Small, reviewable commits.
- When you find a bug not in scope, tell me. Don't silently fix it.
- If a design decision has real tradeoffs, explain the options rather than
  picking silently — I need to be able to defend these choices in interviews.
- Don't add dependencies without asking.
