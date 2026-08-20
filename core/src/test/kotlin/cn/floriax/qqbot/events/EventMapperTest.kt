package cn.floriax.qqbot.events

import cn.floriax.qqbot.bus.EventPayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * EventMapper 的单元测试：验证群@消息、C2C 消息到强类型事件的映射、
 * 未知事件类型回退到 UnknownEvent，以及 d 字段解码失败时的兜底行为。
 * 字段名以官方事件文档为准：C2C 用 author.user_openid，群聊用 author.member_openid。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
class EventMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    // 官方文档 payload 结构 + 群@消息 d 字段（以官方 payload 文档为准）
    private val groupPayload = """
        {
          "id": "ROBOT1.0_abcdefghij",
          "op": 0,
          "s": 5,
          "t": "GROUP_AT_MESSAGE_CREATE",
          "d": {
            "id": "msgid-123",
            "content": " 你好机器人",
            "timestamp": "2026-08-19T10:00:00+08:00",
            "author": { "member_openid": "member-openid-1" },
            "group_openid": "group-openid-1"
          }
        }
    """.trimIndent()

    @Test
    fun `group at message maps to strong type`() {
        val payload = json.decodeFromString<EventPayload>(groupPayload)
        val event = EventMapper.map(payload)
        val msg = assertIs<GroupAtMessage>(event)
        assertEquals("msgid-123", msg.rawId)
        assertEquals(" 你好机器人", msg.content)
        assertEquals("member-openid-1", msg.authorId)
        assertEquals("group-openid-1", msg.groupId)
        assertEquals("2026-08-19T10:00:00+08:00", msg.timestamp)
    }

    @Test
    fun `c2c message maps to strong type`() {
        val c2cPayload = """
            {
              "id": "ROBOT1.0_c2c",
              "op": 0,
              "t": "C2C_MESSAGE_CREATE",
              "d": {
                "id": "c2c-msg-1",
                "content": "hello",
                "timestamp": "2026-08-19T10:00:00+08:00",
                "author": { "user_openid": "user-openid-2" }
              }
            }
        """.trimIndent()
        val payload = json.decodeFromString<EventPayload>(c2cPayload)
        val msg = assertIs<C2cMessage>(EventMapper.map(payload))
        assertEquals("hello", msg.content)
        assertEquals("user-openid-2", msg.authorId)
    }

    @Test
    fun `group message maps with mentionedBot from mentions`() {
        val payload = json.decodeFromString<EventPayload>(
            """
            {
              "id": "ROBOT1.0_gm",
              "op": 0,
              "t": "GROUP_MESSAGE_CREATE",
              "d": {
                "id": "gm-1",
                "content": " <@1D5589C9D3184B1ADD421F8ED83F7DA2> 在吗",
                "timestamp": "2026-08-20T10:00:00+08:00",
                "author": { "member_openid": "member-openid-3" },
                "group_openid": "group-openid-1",
                "mentions": [
                  { "id": "U1", "member_openid": "member-openid-3", "username": "某人", "bot": false },
                  { "id": "BOT", "member_openid": "", "username": "机器人", "bot": true }
                ]
              }
            }
            """.trimIndent()
        )
        val msg = assertIs<GroupMessage>(EventMapper.map(payload))
        assertEquals("gm-1", msg.rawId)
        assertEquals("member-openid-3", msg.authorId)
        assertEquals("group-openid-1", msg.groupId)
        assertEquals("在吗", msg.content, "content 应剥离 <@…> 标记并 trim")
        assertTrue(msg.mentionedBot, "mentions 含 bot=true 时应为 true")

        // 无 @ 的普通消息
        val noMention = json.decodeFromString<EventPayload>(
            """{"id":"gm-2","op":0,"t":"GROUP_MESSAGE_CREATE",
                "d":{"id":"gm-2","content":"闲聊","author":{"member_openid":"m4"},
                     "group_openid":"g1","mentions":[]}}"""
        )
        val msg2 = assertIs<GroupMessage>(EventMapper.map(noMention))
        assertTrue(!msg2.mentionedBot, "无 mentions 时应为 false")
    }

    @Test
    fun `unknown t maps to UnknownEvent`() {
        val payload = json.decodeFromString<EventPayload>(
            """{"id":"e1","op":0,"t":"GUILD_MEMBER_ADD","d":{"user":{"id":"x"}}}"""
        )
        val event = assertIs<UnknownEvent>(EventMapper.map(payload))
        assertEquals("GUILD_MEMBER_ADD", event.type)
        assertTrue(event.data is JsonObject)
    }

    @Test
    fun `decode failure falls back to UnknownEvent`() {
        // 已知 t 但 d 结构不符合 → UnknownEvent 兜底
        val payload = EventPayload(
            id = "e2", op = 0, t = "GROUP_AT_MESSAGE_CREATE",
            d = Json.decodeFromString("""{"author": 123}""")
        )
        val event = EventMapper.map(payload)
        assertIs<UnknownEvent>(event)
    }
}
