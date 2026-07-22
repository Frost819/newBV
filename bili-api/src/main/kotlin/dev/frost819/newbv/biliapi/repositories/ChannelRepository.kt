package dev.frost819.newbv.biliapi.repositories

import dev.frost819.newbv.biliapi.grpc.utils.generateChannel
import io.grpc.ManagedChannel

class ChannelRepository {
    var defaultChannel: ManagedChannel? = null

    fun initDefaultChannel(accessKey: String, buvid: String) {
        defaultChannel?.shutdownNow()
        defaultChannel = generateChannel(accessKey, buvid)
    }
}
