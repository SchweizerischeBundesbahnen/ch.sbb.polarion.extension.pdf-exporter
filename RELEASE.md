
# Guidelines for Releases

## Code Ownership and Automated Release Management

Releases within our project are exclusively overseen by designated code owners, as outlined in the [/ .github / CODEOWNERS](/.github/CODEOWNERS) file. Our release process is automated using the Release Please GitHub action, which is configured in [/ .github / workflows / release-please.yml](/.github/workflows/release-please.yml).

## Overview of the Workflow:

1. For the initial release, the Release Please GitHub action generates a pull request entitled "chore(main): release 1.0.0", marking the commencement of version 1.0.0. This pull request necessitates approval as required.

2. Following the triumphant launch of the initial version, the action automatically generates another pull request to advance the version to 1.0.1-SNAPSHOT. Prompt approval of this pull request is encouraged post the inaugural release.

3. Whenever alterations are introduced to the main branch, the Release Please GitHub action dynamically creates or updates the existing pull request, titled "chore(main): release X.Y.Z", where X.Y.Z denotes the version calculated based on Git message history.

4. Subsequent to each release, the process iterates to update the version to X.Y.Z-SNAPSHOT, thereby preparing a new pull request for the forthcoming X.Y.Z version release.


For comprehensive information, please consult the [Release Please documentation](https://github.com/googleapis/release-please).
