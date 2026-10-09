# Releasing to Maven Central

Releases are published by a maintainer from a local machine. Credentials never go into this repository.

## One-time setup

1. **Central Portal account.** Sign in at [central.sonatype.com](https://central.sonatype.com) and verify the `com.invoicexml` namespace. The portal shows a TXT record to add to the `invoicexml.com` DNS zone.
2. **User token.** In the portal, open *View Account* and generate a user token. Add it to `~/.m2/settings.xml`:

   ```xml
   <settings>
     <servers>
       <server>
         <id>central</id>
         <username>TOKEN_USERNAME</username>
         <password>TOKEN_PASSWORD</password>
       </server>
     </servers>
   </settings>
   ```

3. **GPG key.** Create a signing key and publish the public key so Central can verify signatures:

   ```bash
   gpg --full-generate-key
   gpg --list-secret-keys --keyid-format long
   gpg --keyserver keys.openpgp.org --send-keys <KEY_ID>
   ```

   keys.openpgp.org emails a verification link. The key is only usable after you confirm it.

## Each release

1. Set the release version in `pom.xml` and in the README snippets.
2. Run the tests: `./mvnw clean verify`.
3. Commit and tag: `git tag v<version>`.
4. Upload: `./mvnw -Prelease clean deploy`. GPG asks for the key passphrase.
5. The bundle uploads with `autoPublish=false`. Review it under *Deployments* in the portal, then press *Publish*.
6. Push the commit and the tag.

Artifacts appear on Maven Central within about 30 minutes of publishing.
