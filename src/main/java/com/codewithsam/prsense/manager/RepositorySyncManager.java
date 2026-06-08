package com.codewithsam.prsense.manager;

// Drives incremental synchronisation — called by webhook push events and the scheduled sync job
public interface RepositorySyncManager {

    // Triggered by git.push webhook — syncs the changed files for this Azure repo
    void syncByAzureRepoId(String project, String azureRepoId);

    // Triggered by scheduler — syncs all registered repositories that are stale or indexed
    void syncAll();
}
