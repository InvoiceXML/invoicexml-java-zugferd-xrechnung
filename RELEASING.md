# Releasing to Maven Central

Pushing a version tag publishes the release. The Release workflow builds, tests, signs and uploads it, then publishes it on Maven Central once a maintainer approves the run. Credentials never go into this repository.

## Each release

1. Set the new version in `pom.xml`, in the README's Maven and Gradle snippets, and in `CHANGELOG.md`.
2. Run the tests, including the live suite: `./mvnw clean verify -Dinvoicexml.live=true`.
3. Commit, push, then tag and push the tag:

   ```bash
   git tag -a v1.2.3 -m "Release 1.2.3"
   git push origin v1.2.3
   ```

4. Open the *Actions* tab, select the *Release* run and press *Review deployments*, then *Approve and deploy*.

The workflow stops if the tag does not match the version in `pom.xml`. Artifacts appear on Maven Central within about 30 minutes of publishing.

## One-time setup

The `com.invoicexml` namespace is verified on [central.sonatype.com](https://central.sonatype.com) through a TXT record in the `invoicexml.com` DNS zone. Releases are signed with a GPG key published on keyserver.ubuntu.com.

The workflow reads four secrets from the protected `maven-central` environment. That environment only accepts `v*` tags and requires a reviewer's approval before a job can read them. Set them with the GitHub CLI; each command prompts for the value:

```bash
gh secret set CENTRAL_TOKEN_USERNAME --env maven-central
gh secret set CENTRAL_TOKEN_PASSWORD --env maven-central
gh secret set GPG_PASSPHRASE --env maven-central
gpg --export-secret-keys --armor <KEY_ID> | gh secret set GPG_PRIVATE_KEY --env maven-central
```

The token comes from *View Account*, then *Generate User Token* in the Central Portal.

## Manual release (fallback)

1. Put the Central Portal token in `~/.m2/settings.xml` under a server with the id `central`.
2. Run `./mvnw -Prelease clean deploy` from your own terminal, so GPG can ask for the passphrase.
3. The upload waits for review. Press *Publish* on the deployment in the portal.
