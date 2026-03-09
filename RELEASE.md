# Guidelines for Releases

## Code Ownership and Automated Release Management

Releases within our project are exclusively overseen by designated code owners, as outlined in the [/ .github / CODEOWNERS](/.github/CODEOWNERS) file. Our release process is automated using the Release Please GitHub action, which is configured in [/ .github / workflows / release-please.yml](/.github/workflows/release-please.yml).

## Overview of the Workflow:

1. For the initial release, the Release Please GitHub action generates a pull request entitled "chore(main): release 1.0.0", marking the commencement of version 1.0.0. This pull request necessitates approval as required.

2. Following the triumphant launch of the initial version, the action automatically generates another pull request to advance the version to 1.0.1-SNAPSHOT. Prompt approval of this pull request is encouraged post the inaugural release.

3. Whenever alterations are introduced to the main branch, the Release Please GitHub action dynamically creates or updates the existing pull request, titled "chore(main): release X.Y.Z", where X.Y.Z denotes the version calculated based on Git message history.

4. Subsequent to each release, the process iterates to update the version to X.Y.Z-SNAPSHOT, thereby preparing a new pull request for the forthcoming X.Y.Z version release.


For comprehensive information, please consult the [Release Please documentation](https://github.com/googleapis/release-please).

## LTS (Long-Term Support) Releases

LTS branches allow releasing bug fixes and security patches for older major versions while development continues on `main`.

### Branch Naming Convention

LTS branches follow the pattern: `release-v{major}` (e.g., `release-v6`, `release-v7`)

### Creating an LTS Branch

Create the branch from the last tag of the major version:

```bash
git checkout v11.1.1          # Last tag of the major version
git checkout -b release-v11
git push origin release-v11
```

### Releasing from LTS Branches

1. Cherry-pick or commit fixes to the LTS branch
2. Push changes (Release Please automatically creates a release PR)
3. Merge the release PR to trigger:
    - Tag creation (e.g., `v11.1.2`)
    - GitHub Release with artifacts
    - Maven Central deployment

### Deployment Matrix

| Branch | Version Type | Maven Central | GitHub Packages | GitHub Release |
|--------|--------------|---------------|-----------------|----------------|
| `main` | SNAPSHOT | - | ✓ | - |
| `main` | Release | ✓ | - | ✓ |
| `release-v*` | SNAPSHOT | - | - | - |
| `release-v*` | Release | ✓ | - | ✓ |

Each LTS branch operates independently with its own release PR.
