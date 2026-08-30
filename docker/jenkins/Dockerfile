# Jenkins controller with everything the Jenkinsfile needs already
# baked in: JDK 17 (base image), Maven, and headless Chrome for the
# Selenium system tests. One image running "agent any" instead of
# Docker-in-Docker build agents - fewer moving parts, and every line
# here is easy to explain on its own.
FROM jenkins/jenkins:lts-jdk17

USER root

RUN apt-get update \
    && apt-get install -y --no-install-recommends maven wget gnupg \
    && wget -q -O - https://dl.google.com/linux/linux_signing_key.pub \
       | gpg --dearmor -o /usr/share/keyrings/google-chrome.gpg \
    && echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-chrome.gpg] http://dl.google.com/linux/chrome/deb/ stable main" \
       > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends google-chrome-stable \
    && rm -rf /var/lib/apt/lists/*

USER jenkins

# The base image ships almost no plugins - without this, "New Item"
# doesn't even offer a Pipeline job type. workflow-aggregator is the
# whole Pipeline suite the Jenkinsfile needs; git lets a Pipeline job
# check out from GitHub; junit publishes the Surefire/Failsafe XML
# reports the Jenkinsfile's post block reads.
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# Lab-only convenience: skip the first-run setup wizard. Never do this
# for a Jenkins controller that is actually exposed to anyone else.
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"
