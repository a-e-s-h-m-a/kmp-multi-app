package com.example.multiapp.domain

interface NetworkClient {
    suspend fun get(path: String): String
}

class FakeNetworkClient : NetworkClient {
    override suspend fun get(path: String): String =
        fakeConfigs[path] ?: "{}"

    companion object {
        private val fakeConfigs: Map<String, String> = mapOf(
            "/config/appOne/customer" to """{"delivery":"customer","payments":true,"reports":false}""",
            "/config/appOne/driver" to """{"delivery":"driver","payments":false,"reports":false}""",
            "/config/appOne/readonly" to """{"delivery":"readonly","payments":false,"reports":false}""",
            "/config/appTwo/admin" to """{"delivery":"admin","payments":false,"reports":true}""",
            "/config/appTwo/merchant" to """{"delivery":"merchant","payments":false,"reports":true}""",
            "/config/appTwo/readonly" to """{"delivery":"readonly","payments":false,"reports":false}""",
            "/config/common/nod" to """{"delivery":null,"payments":false,"reports":false}""",
        )
    }
}
