package cn.floriax.qqbot.webhook

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer

/**
 * QQ 官方回调签名算法的工具封装：secret 重复扩展至 >=32 字节取前 32 字节为 seed。
 * 签名/验签基于 BouncyCastle 轻量 API（JDK 标准库不提供 seed→公钥派生）。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
internal object Ed25519 {

    private const val SEED_SIZE = 32

    /** 将 secret 按 QQ 官方规则重复扩展为 32 字节 seed。 */
    fun seedFromSecret(secret: String): ByteArray {
        val bytes = secret.toByteArray(Charsets.UTF_8)
        val seed = if (bytes.size >= SEED_SIZE) bytes
        else ByteArray(SEED_SIZE).also { out ->
            var i = 0
            while (i < SEED_SIZE) {
                bytes.copyInto(out, i, 0, minOf(bytes.size, SEED_SIZE - i))
                i += bytes.size
            }
        }
        return seed.copyOf(SEED_SIZE)
    }

    /** 从 seed 派生公钥原始字节（32 字节），验签初始化用。 */
    fun publicKeyFrom(secret: String): ByteArray =
        Ed25519PrivateKeyParameters(seedFromSecret(secret), 0).generatePublicKey().encoded

    /** 用 seed 对应的私钥对消息签名。 */
    fun sign(message: String, seed: ByteArray): ByteArray {
        val signer = Ed25519Signer()
        signer.init(true, Ed25519PrivateKeyParameters(seed, 0))
        val bytes = message.toByteArray(Charsets.UTF_8)
        signer.update(bytes, 0, bytes.size)
        return signer.generateSignature()
    }

    /** 验证 Ed25519 签名，任何异常（如非法公钥/签名长度）均视为验证失败。 */
    fun verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean =
        runCatching {
            val verifier = Ed25519Signer()
            verifier.init(false, Ed25519PublicKeyParameters(publicKey, 0))
            verifier.update(message, 0, message.size)
            verifier.verifySignature(signature)
        }.getOrDefault(false)

    /** 字节数组转小写十六进制字符串。 */
    fun toHex(bytes: ByteArray): String =
        buildString(bytes.size * 2) { bytes.forEach { append("%02x".format(it)) } }
}
