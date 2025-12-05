# Releasing

## Set up GitHub CLI

Install GitHub CLI

```bash
brew install gh
```

## Creating the release

* Update the changelog
```bash
mate CHANGELOG.md
```

* Create a local release branch from `main` and update `VERSION_NAME` in `gradle.properties` (removing `-SNAPSHOT`) and the README, then run the publish workflow and finish the release:

```bash
git checkout main && \
git pull && \
git checkout -b release_{NEW_VERSION} && \
sed -i '' 's/VERSION_NAME=.*-SNAPSHOT/VERSION_NAME={NEW_VERSION}/' gradle.properties
sed -i '' "s/com.squareup.radiography:radiography:.*'/com.squareup.radiography:radiography:{NEW_VERSION}'/" README.md && \
git commit -am "Prepare {NEW_VERSION} release" && \
./gradlew clean && \
./gradlew build && \
./gradlew connectedCheck && \
// See stoic-plugin/README.md.
./gradlew :stoic-plugin:dist && \
git tag v{NEW_VERSION} && \
git push origin v{NEW_VERSION} && \
gh workflow run publish-release.yml --ref v{NEW_VERSION} && \
gh run list --workflow=publish-release.yml --branch v{NEW_VERSION} --json databaseId --jq ".[].databaseId" | xargs -I{} gh run watch {} --exit-status && \
git checkout main && \
git pull && \
git merge --no-ff --no-edit release_{NEW_VERSION} && \
sed -i '' 's/VERSION_NAME={NEW_VERSION}/VERSION_NAME={NEXT_VERSION}-SNAPSHOT/' gradle.properties && \
git commit -am "Prepare for next development iteration" && \
git push && \
gh release create v{NEW_VERSION} --title v{NEW_VERSION} --notes 'See [Change Log](https://github.com/square/radiography/blob/main/CHANGELOG.md)' stoic-plugin/build/distributions/radiography-stoic-plugin-{NEW_VERSION}.tar.gz
```

* Wait for the release to be available [on Maven Central](https://repo1.maven.org/maven2/com/squareup/radiography/radiography/).
* Tell your friends, update all of your apps, and tweet the new release. 
  As a nice extra touch, mention external contributions.
