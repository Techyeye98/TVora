package tv.cleanyour.app

import tv.cleanyour.core.flows.StoreRules
import tv.cleanyour.core.flows.UpdateChecker
import tv.cleanyour.core.registry.ArtifactFetcher
import tv.cleanyour.core.registry.HttpFetcher
import tv.cleanyour.core.trust.DownloadHosts

object Store : StorePorts {
    override val flavour = "full"
    override val hosts: Set<String> = DownloadHosts.ALL
    override val rules = StoreRules.FULL.copy(updateCheck = false, updatesOff = false)
    override val updatesMayGoOff = false
    override val releasePage: String? = null
    // A fork cannot install the upstream maintainer's differently signed app updates.
    // Wire TVora's release feed once its repository and signing policy are established.
    override fun updateChecker(http: HttpFetcher) = UpdateChecker { null }
    override fun addOnFetcher(make: () -> ArtifactFetcher): ArtifactFetcher = make()
}
