const owner = document.body.dataset.owner;
const repo = document.body.dataset.repo;
const releaseMeta = document.getElementById("release-meta");
const primaryButton = document.getElementById("download-button");
const secondaryButton = document.getElementById("download-button-secondary");
const releasesUrl = `https://github.com/${owner}/${repo}/releases`;

async function loadLatestRelease() {
  try {
    const response = await fetch(`https://api.github.com/repos/${owner}/${repo}/releases/latest`);
    if (!response.ok) {
      throw new Error(`GitHub API returned ${response.status}`);
    }

    const release = await response.json();
    const apkAsset = release.assets.find((asset) => asset.name.endsWith(".apk"));
    if (!apkAsset) {
      releaseMeta.textContent = "No APK is attached to the latest release yet. Open the releases page for details.";
      primaryButton.href = releasesUrl;
      secondaryButton.href = releasesUrl;
      return;
    }

    primaryButton.href = apkAsset.browser_download_url;
    secondaryButton.href = apkAsset.browser_download_url;
    secondaryButton.textContent = `Download ${release.tag_name}`;
    releaseMeta.textContent = `Latest release: ${release.tag_name} published on ${new Date(release.published_at).toLocaleDateString()}`;
  } catch (error) {
    primaryButton.href = releasesUrl;
    secondaryButton.href = releasesUrl;
    releaseMeta.textContent = "Latest release lookup is unavailable right now. Use the releases page instead.";
  }
}

loadLatestRelease();
