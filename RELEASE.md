# Guidelines for Releases

## Code Ownership and Automated Release Management

Releases within our project are exclusively overseen by designated code owners, as outlined in the [/ .github / CODEOWNERS](/.github/CODEOWNERS) file. Our release process is automated using the Release Please GitHub action, which is configured in [/ .github / workflows / release-please.yml](/.github/workflows/release-please.yml).

## Overview of the Workflow:

1. For the initial release, the Release Please GitHub action generates a pull request entitled "chore(main): release 1.0.0", marking the commencement of version 1.0.0. This pull request necessitates approval as required.

2. Following the triumphant launch of the initial version, the action automatically generates another pull request to advance the version to 1.0.1-SNAPSHOT. Prompt approval of this pull request is encouraged post the inaugural release.

3. Whenever alterations are introduced to the main branch, the Release Please GitHub action dynamically creates or updates the existing pull request, titled "chore(main): release X.Y.Z", where X.Y.Z denotes the version calculated based on Git message history.

4. Subsequent to each release, the process iterates to update the version to X.Y.Z-SNAPSHOT, thereby preparing a new pull request for the forthcoming X.Y.Z version release.


### Branch Protection

The [`release-please-guard`](/.github/workflows/release-please-guard.yml) workflow prevents PRs from merging while a release-please snapshot PR is pending. This ensures the snapshot version bump lands immediately after the release, avoiding misordered commits on the target branch. To enforce this, add `release-please-guard` as a required status check in your branch protection rules for `main` and any `release-v*` branches, and enable "Require branches to be up to date before merging". This ensures PRs that previously passed the guard are rechecked when a release-please PR is opened.

For comprehensive information, please consult the [Release Please documentation](https://github.com/googleapis/release-please).

## LTS (Long-Term Support) Releases

LTS branches allow releasing bug fixes and security patches for older major versions while development continues on `main`.

### Branch Naming Convention

LTS branches must follow the pattern `release-v{major}`, for example:
- `release-v6` - LTS branch for v6.x releases
- `release-v7` - LTS branch for v7.x releases

### Creating an LTS Branch

Create the branch from the last tag of the major version:
```bash
git checkout v6.4.2
git checkout -b release-v6
git push origin release-v6
```

The `release-v*` pattern in the workflow automatically recognizes new LTS branches.

### LTS Release Workflow

1. Cherry-pick or commit fixes to the LTS branch (e.g., `release-v6`)
2. Push changes - Release Please automatically creates a release PR
3. Merge the release PR - this triggers:
   - Tag creation (e.g., `v6.4.3`)
   - GitHub Release with artifacts
   - Maven Central deployment

### Deployment Matrix

| Branch | Version Type | Maven Central | GitHub Packages | GitHub Release |
|--------|--------------|---------------|-----------------|----------------|
| `main` | SNAPSHOT | - | ✓ | - |
| `main` | Release | ✓ | - | ✓ |
| `release-v*` | SNAPSHOT | - | - | - |
| `release-v*` | Release | ✓ | - | ✓ |

### Notes

- SNAPSHOT versions from LTS branches are NOT deployed to GitHub Packages
- Only release versions from LTS branches are deployed to Maven Central
- Each LTS branch operates independently with its own release PR
