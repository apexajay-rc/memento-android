# MEMENTO — v1.1

MEMENTO is a local-first personal time-awareness and execution system. It is not a generic task manager and does not predict an individual's date of death.

## v1.1 capabilities

- Elapsed-days counter based on the configured date of birth (20 March 2006).
- One daily directive with a concrete next physical action and a planned focus budget.
- Local persistence with Room: missions, focus sessions and daily reviews survive app restarts.
- 25- or 50-minute focus sessions, derived from timestamps rather than an in-memory countdown.
- Manual distraction logging during a focus session.
- Session-end self-assessment: record whether the session produced meaningful progress.
- Seven-day execution panel: committed missions, completed missions, planned focus, tracked focus and the plan-to-tracked gap.
- End-of-day audit: mission outcome, main time leak and tomorrow's next action.
- Home-screen Glance widget reading the same Room database as the app.
- GitHub Actions workflow to build a debug APK.

## Build the APK

The project uses Android Gradle Plugin 8.7.3, Kotlin 1.9.25, KSP 1.9.25-1.0.20, Room 2.6.1, Compose and Glance.

In Android Studio, open the project root and let Gradle sync. Or build from a configured Android SDK environment:

```bash
gradle --no-daemon :app:assembleDebug
```

The APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions builds the same debug APK and uploads it as the `MEMENTO-debug-apk` artifact.

## Data and measurement semantics

All data is stored locally in `memento.db`. No account, cloud backend or analytics service is used.

- A focus session's elapsed time is calculated from its recorded start and finish timestamps. If the app is killed during a running session, the timer continues against wall-clock time and is reconstructed when the app reopens.
- `Tracked focus` is elapsed timer time, not independent proof of productive work.
- After a session, the user must self-report whether it produced meaningful progress. This is kept separate from elapsed time.
- The seven-day plan-to-tracked gap compares minutes planned across committed daily directives with elapsed minutes from reviewed focus sessions. It is a behavioural indicator, not a measure of cognitive output.
- The life counter displays elapsed days only. It does not calculate or display a personal death countdown.

## Current limitations

- One primary directive per calendar day.
- Focus sessions are not paused; they can be ended early and reviewed.
- Daily review is manual. No forced lock-screen interception or invasive overlay is used.
- The widget refreshes after app state changes and at Android's scheduled widget update interval; Android may batch widget refreshes.
- No Google Calendar, Slack, Habitica or other external integrations yet. Those should be added only after the local data model and daily workflow are proven useful.

## Next development sequence

1. Compile and install v1.1; verify mission persistence, timer recovery, session review, daily audit and widget refresh.
2. Add mission history and edit/correction flows.
3. Add optional reminders and daily rollover handling.
4. Evaluate calendar import/sync with explicit user-selected permissions.
5. Consider external integrations only where they reduce manual entry rather than add another dashboard.
