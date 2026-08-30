# Jenkins pipeline: verified end to end

Required evidence: a working Jenkins pipeline via the Docker setup (assignment
"Jenkins", 8 marks).

## What was done

1. Built `docker/jenkins/` (`docker compose build`) - a Jenkins controller
   with Maven, headless Chrome, and (after the fix below) the Pipeline/Git/JUnit
   plugins baked in.
2. Started it (`docker compose up -d`) and confirmed the setup wizard was
   correctly skipped (`JAVA_OPTS=-Djenkins.install.runSetupWizard=false`).
3. Created a Pipeline job, `clinic-booking-pipeline`, configured as
   "Pipeline script from SCM": Git, `https://github.com/BIRUK-MULATU/clinic_booking-.git`,
   branch `*/main`, script path `Jenkinsfile` - i.e. it runs the exact
   `Jenkinsfile` committed at the repo root, unmodified.
4. Ran it four times while shaking out real problems (see below).

## Real defects hit and fixed along the way

- **First build attempt:** the base `jenkins/jenkins:lts-jdk17` image ships
  almost no plugins, so "New Item" didn't even offer a Pipeline job type.
  Installed `workflow-aggregator`, `git`, and `junit` manually through the
  UI to unblock the demo, then fixed the Dockerfile properly (see below) so
  this isn't a manual step for the next person.
- **Build #1 failed:** branch specifier defaulted to `*/master`, but this
  repo's default branch is `main`. `ERROR: ... couldn't find remote ref
  refs/heads/master`.
- **Build #2 failed differently:** while correcting the branch specifier, an
  input mis-click also overwrote the "Script Path" field with `*/main`
  instead of leaving it as `Jenkinsfile`, producing a confusing
  `ERROR: Unable to find */main from git ...`. Fixed both fields and
  disabled "Lightweight checkout" (more reliable branch resolution than the
  lightweight/JGit path for this setup).
- **Build #3 ran the real pipeline and failed for a real reason:** DEF-003 -
  `BookingJourneyIT` failed with `NoSuchElementException` on
  `#confirmation-status`, a flaky Selenium wait bug that only showed up
  under the container's tighter CPU budget (never seen in GitHub Actions).
  Fixed in `AbstractPage` (explicit `WebDriverWait`); see `docs/defect-log.md`
  DEF-003 for the full root-cause writeup.
- **Dockerfile follow-up:** rather than leave "install three plugins by
  hand" as a hidden setup step, added `docker/jenkins/plugins.txt` and a
  `jenkins-plugin-cli` install step to the Dockerfile. Rebuilt the image
  and started a completely fresh container (`docker compose down -v` +
  `up -d`) to confirm Pipeline is now offered immediately with no manual
  plugin installation - screenshot-verified.

## Final result

**Build #4: `Finished: SUCCESS`.** All 11 integration/system tests passed
(the same 9 integration + 2 system tests as the GitHub Actions run), the
80% branch-coverage gate passed, JUnit results were published via the
`junit` pipeline step, and the JaCoCo report was archived via
`archiveArtifacts` - both visible in the job's build page ("Latest Test
Result (no failures)", "Last Successful Artifacts").

This was run locally against the Docker Jenkins setup and is not itself
hosted anywhere persistent - the evidence is this document plus the commits
(`185d051` fixes DEF-003; the Dockerfile/plugins.txt fix landed in the same
area) and the defect log entries.
