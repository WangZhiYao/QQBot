package cn.floriax.qqbot.webhook

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Ed25519 工具类测试：覆盖 secret 到 seed 的折叠/截断规则、公钥派生稳定性、
 * 签名与验签往返、篡改消息验签失败以及 hex 编码。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
class Ed25519Test {

    @Test
    fun `secret shorter than 32 bytes is doubled and truncated to seed`() {
        // 官方文档示例：secret "naOC0ocQE3shWLAfffVLB1rhYPG7"(29字节) → seed 为其重复后取前32字节
        val seed = Ed25519.seedFromSecret("naOC0ocQE3shWLAfffVLB1rhYPG7")
        val expected = "naOC0ocQE3shWLAfffVLB1rhYPG7naOC".toByteArray()
        assertEquals(32, seed.size)
        assertTrue(seed.contentEquals(expected))
    }

    @Test
    fun `secret longer than 32 bytes is truncated`() {
        val secret = "a".repeat(40)
        val seed = Ed25519.seedFromSecret(secret)
        assertEquals(32, seed.size)
        assertTrue(seed.contentEquals("a".repeat(32).toByteArray()))
    }

    @Test
    fun `public key derived from same secret is stable`() {
        val a = Ed25519.publicKeyFrom("naOC0ocQE3shWLAfffVLB1rhYPG7")
        val b = Ed25519.publicKeyFrom("naOC0ocQE3shWLAfffVLB1rhYPG7")
        assertTrue(a.contentEquals(b))
        assertEquals(32, a.size)
    }

    @Test
    fun `sign and verify roundtrip`() {
        val secret = "naOC0ocQE3shWLAfffVLB1rhYPG7"
        val seed = Ed25519.seedFromSecret(secret)
        val pub = Ed25519.publicKeyFrom(secret)
        val sig = Ed25519.sign("1725442341" + "Arq0D5A61EgUu4OxUvOp", seed)
        assertTrue(Ed25519.verify("1725442341Arq0D5A61EgUu4OxUvOp".toByteArray(), sig, pub))
    }

    @Test
    fun `verify fails on tampered message`() {
        val secret = "naOC0ocQE3shWLAfffVLB1rhYPG7"
        val sig = Ed25519.sign("msg", Ed25519.seedFromSecret(secret))
        assertFalse(Ed25519.verify("msg2".toByteArray(), sig, Ed25519.publicKeyFrom(secret)))
    }

    @Test
    fun `hex encode roundtrip`() {
        val bytes = byteArrayOf(0, 1, -2, 127)
        assertEquals("0001fe7f", Ed25519.toHex(bytes))
    }
}
