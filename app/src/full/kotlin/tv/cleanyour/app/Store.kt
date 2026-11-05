package tv.cleanyour.app

import tv.cleanyour.core.flows.StoreRules
import tv.cleanyour.core.flows.UpdateChecker
import tv.cleanyour.core.registry.ArtifactFetcher
import tv.cleanyour.core.registry.HttpFetcher
import tv.cleanyour.core.trust.DownloadHosts

object Store : StorePorts {
    override val flavour = "full"
    override val hosts: Set<String> = DownloadHosts.ALL
    override val rules = StoreRules.FULL.copy(addOns = false, updatesOff = false)
    override val updatesMayGoOff = false
    override val releasePage: String = tv.cleanyour.core.flows.ProjectLinks.RELEASES
    override fun updateChecker(http: HttpFetcher): UpdateChecker = GitHubUpdateChecker(http)
    override fun addOnFetcher(make: () -> ArtifactFetcher): ArtifactFetcher = object : ArtifactFetcher {
        override suspend fun fetch(ref: tv.cleanyour.core.model.ArtifactRef, onProgress: (Long, Long) -> Unit) =
            tv.cleanyour.core.registry.FetchResult.Failed(tv.cleanyour.core.registry.FetchFailure.HOST_NOT_ALLOWED, null)
    }
}
