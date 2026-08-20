package cn.floriax.qqbot.client

/**
 * QQ 开放平台官方错误码全量定义。
 * 来源：官方 API 调用指南错误码章节（bot.q.qq.com）。
 * err_code != 0 即失败；不要依据 message 判断成败。
 * 按类别提供 isRetryable / isPermissionDenied / isRateLimited / isMsgExpired 等语义判断，便于调用方决定重试、退避或放弃。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
enum class QqErrorCode(val code: Int, val meaning: String) {
    // ── 公共错误 ──
    UNKNOWN_ACCOUNT(10001, "账号异常"),
    UNKNOWN_CHANNEL(10003, "子频道异常"),
    UNKNOWN_GUILD(10004, "频道异常"),
    ERROR_CHECK_ADMIN_FAILED(11281, "检查是否是管理员失败(系统错误,重试一次)"),
    ERROR_CHECK_ADMIN_NOT_PASS(11282, "检查是否是管理员未通过,需提示用户授权"),
    ERROR_WRONG_APPID_11251(11251, "参数中的 appid 错误"),
    ERROR_CHECK_APP_PRIVILEGE_FAILED(11252, "检查应用权限失败(系统错误,重试一次)"),
    ERROR_CHECK_APP_PRIVILEGE_NOT_PASS(11253, "应用未获该接口权限,需向平台申请"),
    ERROR_INTERFACE_FORBIDDEN(11254, "应用接口被封禁"),
    ERROR_WRONG_APPID_11261(11261, "参数中缺少 appid"),
    ERROR_CHECK_ROBOT(11262, "当前接口不支持使用机器人 Bot Token 调用"),
    ERROR_CHECK_GUILD_AUTH(11263, "检查频道权限失败(系统错误,重试一次)"),
    ERROR_GUILD_AUTH_NOT_PASS(11264, "检查频道权限未通过,可提示用户授权"),
    ERROR_ROBOT_HAS_BANED(11265, "机器人已经被封禁"),
    ERROR_WRONG_TOKEN_11241(11241, "参数中缺少 token"),
    ERROR_CHECK_TOKEN_FAILED(11242, "校验 token 失败(系统错误,重试一次)"),
    ERROR_CHECK_TOKEN_NOT_PASS(11243, "校验 token 未通过,token 错误"),
    ERROR_CHECK_USER_AUTH(11273, "检查用户权限失败,当前接口不支持 Bearer Token 调用"),
    ERROR_USER_AUTH_NOT_PASS(11274, "检查用户权限未通过,可提示用户重新授权"),
    ERROR_WRONG_APPID_11275(11275, "无 appid"),
    ERROR_GET_HTTP_HEADER(11301, "HTTP Header 无效"),
    ERROR_GET_HEADER_UIN(11302, "HTTP Header 无效"),
    ERROR_GET_NICK(11303, "获取昵称失败"),
    ERROR_GET_AVATAR(11304, "获取头像失败"),
    ERROR_GET_GUILD_ID(11305, "获取频道 ID 失败"),
    ERROR_GET_GUILD_INFO(11306, "获取频道信息失败"),
    REPLACE_ID_FAILED(12001, "替换 id 失败"),
    REQUEST_INVALID(12002, "请求体错误"),
    RESPONSE_INVALID(12003, "回包错误"),

    // ── 消息类 ──
    CHANNEL_HIT_WRITE_RATE_LIMIT(20028, "子频道消息触发限频"),
    CANNOT_SEND_EMPTY_MESSAGE(50006, "消息为空"),
    INVALID_FORM_BODY(50035, "form-data 内容异常"),
    MARKDOWN_ONLY_WITH_KEYBOARD(50037, "markdown 消息只支持 markdown 或 keyboard 组合"),
    NOT_SAME_CHANNEL_SUB(50038, "非同频道同子频道"),
    GET_MESSAGE_FAILED(50039, "获取消息失败"),
    MESSAGE_TEMPLATE_TYPE_ERROR(50040, "消息模版类型错误"),
    MARKDOWN_HAS_EMPTY(50041, "markdown 有空值"),
    MARKDOWN_LIST_MAX(50042, "markdown 列表长达最大值"),
    GUILD_ID_CONVERT_FAILED(50043, "guild_id 转换失败"),
    CANNOT_REPLY_OWN_MESSAGE(50045, "不能回复机器人自己产生的消息"),
    NOT_AT_MESSAGE(50046, "非 at 机器人消息"),
    NOT_BOT_MESSAGE_OR_AT(50047, "非机器人产生的消息或 at 机器人消息"),
    MESSAGE_ID_EMPTY(50048, "message id 不能为空"),
    KEYBOARD_ONLY_MODIFY(50049, "只能修改含有 keyboard 元素的消息"),
    KEYBOARD_NOT_EMPTY_ON_MODIFY(50050, "修改消息时 keyboard 元素不能为空"),
    MODIFY_ONLY_OWN_MESSAGE(50051, "只能修改机器人自己发送的消息"),
    MODIFY_MESSAGE_ERROR(50053, "修改消息错误"),
    MARKDOWN_TEMPLATE_PARAM_ERROR(50054, "markdown 模版参数错误"),
    INVALID_MARKDOWN_CONTENT(50055, "无效的 markdown content"),
    MARKDOWN_CONTENT_NOT_ALLOWED(50056, "不允许发送 markdown content"),
    MARKDOWN_SYNTAX_OR_TEMPLATE(50057, "markdown 参数只支持原生语法或者模版二选一"),

    // ── 子频道权限 301000-301007 ──
    SUB_CHANNEL_PARAM_ERROR(301000, "参数错误"),
    SUB_CHANNEL_QUERY_INFO_ERROR(301001, "查询频道信息错误"),
    SUB_CHANNEL_QUERY_PERM_ERROR(301002, "查询子频道权限错误"),
    SUB_CHANNEL_MODIFY_PERM_ERROR(301003, "修改子频道权限错误"),
    SUB_CHANNEL_MEMBER_LIMIT(301004, "私密子频道关联的人数到达上限"),
    SUB_CHANNEL_RPC_FAILED(301005, "调用 Rpc 服务失败"),
    SUB_CHANNEL_NOT_MEMBER(301006, "非群成员没有查询权限"),
    SUB_CHANNEL_PARAM_LIMIT(301007, "参数超过数量限制"),

    // ── 日程 302000-302024 ──
    SCHEDULE_PARAM_ERROR(302000, "参数错误"),
    SCHEDULE_QUERY_CHANNEL_ERROR(302001, "查询频道信息错误"),
    SCHEDULE_QUERY_LIST_ERROR(302002, "查询日程列表失败"),
    SCHEDULE_QUERY_ERROR(302003, "查询日程失败"),
    SCHEDULE_MODIFY_ERROR(302004, "修改日程失败"),
    SCHEDULE_DELETE_ERROR(302005, "删除日程失败"),
    SCHEDULE_CREATE_ERROR(302006, "创建日程失败"),
    SCHEDULE_GET_CREATOR_ERROR(302007, "获取创建者信息失败"),
    SCHEDULE_CHANNEL_ID_EMPTY(302008, "子频道 ID 不能为空"),
    SCHEDULE_SYSTEM_ERROR(302009, "频道系统错误,请联系客服"),
    SCHEDULE_NO_MODIFY_PERMISSION(302010, "暂无修改日程权限"),
    SCHEDULE_ALREADY_DELETED(302011, "日程活动已被删除"),
    SCHEDULE_DAILY_LIMIT(302012, "每天只能创建 10 个日程"),
    SCHEDULE_SECURITY_HIT(302013, "创建日程触发安全打击"),
    SCHEDULE_DURATION_OVERFLOW(302014, "日程持续时间超过 7 天"),
    SCHEDULE_START_TIME_INVALID(302015, "开始时间不能早于当前时间"),
    SCHEDULE_END_BEFORE_START(302016, "结束时间不能早于开始时间"),
    SCHEDULE_OBJECT_NULL(302017, "Schedule 对象为空"),
    SCHEDULE_TYPE_CONVERT_ERROR(302018, "参数类型转换失败"),
    SCHEDULE_DOWNSTREAM_ERROR(302019, "调用下游失败,请联系客服"),
    SCHEDULE_CONTENT_VIOLATION(302020, "日程内容违规、账号违规"),
    SCHEDULE_DAILY_CREATE_LIMIT(302021, "频道内当日新增活动达上限"),
    SCHEDULE_BIND_OTHER_CHANNEL(302022, "不能绑定非当前频道的子频道"),
    SCHEDULE_START_BIND_INVALID(302023, "开始时跳转不可绑定日程子频道"),
    SCHEDULE_BIND_NOT_EXIST(302024, "绑定的子频道不存在"),

    // ── 消息推送 304003-304052 ──
    URL_NOT_ALLOWED(304003, "url 未报备"),
    ARK_NOT_ALLOWED(304004, "没有发 ark 消息权限"),
    EMBED_LIMIT(304005, "embed 长度超限"),
    SERVER_CONFIG(304006, "后台配置错误"),
    GET_GUILD_ERROR(304007, "查询频道异常"),
    GET_BOT_ERROR(304008, "查询机器人异常"),
    GET_CHANNEL_ERROR(304009, "查询子频道异常"),
    CHANGE_IMAGE_URL_ERROR(304010, "图片转存错误"),
    NO_TEMPLATE(304011, "模板不存在"),
    GET_TEMPLATE_ERROR(304012, "取模板错误"),
    TEMPLATE_PRIVILEGE(304014, "没有模板权限"),
    SEND_ERROR(304016, "发消息错误"),
    UPLOAD_IMAGE_ERROR(304017, "图片上传错误"),
    SESSION_NOT_EXIST(304018, "机器人没连上 gateway"),
    AT_EVERYONE_TIMES(304019, "@全体成员 次数超限"),
    FILE_SIZE(304020, "文件大小超限"),
    GET_FILE_ERROR(304021, "下载文件错误"),
    PUSH_TIME(304022, "推送消息时间限制"),
    PUSH_MSG_ASYNC_OK(304023, "推送消息异步调用成功,等待人工审核"),
    REPLY_MSG_ASYNC_OK(304024, "回复消息异步调用成功,等待人工审核"),
    BEAT(304025, "消息被打击"),
    MSG_ID_ERROR(304026, "回复的消息 id 错误"),
    MSG_EXPIRE(304027, "回复的消息过期"),
    MSG_PROTECT(304028, "非 At 当前用户的消息不允许回复"),
    CORPUS_ERROR(304029, "调语料服务错误"),
    CORPUS_NOT_MATCH(304030, "语料不匹配"),
    DM_CLOSED(304031, "私信已关闭"),
    DM_NOT_EXIST(304032, "私信不存在"),
    DM_PULL_ERROR(304033, "拉私信错误"),
    DM_NOT_MEMBER(304034, "不是私信成员"),
    PUSH_SUB_CHANNEL_LIMIT(304035, "推送消息超过子频道数量限制"),
    NO_MARKDOWN_TEMPLATE_PERMISSION(304036, "没有 markdown 模板的权限"),
    NO_BUTTON_PERMISSION(304037, "没有发消息按钮组件的权限"),
    BUTTON_NOT_EXIST(304038, "消息按钮组件不存在"),
    BUTTON_PARSE_ERROR(304039, "消息按钮组件解析错误"),
    BUTTON_CONTENT_ERROR(304040, "消息按钮组件消息内容错误"),
    MESSAGE_SETTING_ERROR(304044, "取消息设置错误"),
    SUB_CHANNEL_PUSH_RATE(304045, "子频道主动消息数限频"),
    SUB_CHANNEL_PUSH_NOT_ALLOWED(304046, "不允许在此子频道发主动消息"),
    PUSH_SUB_CHANNEL_OVERFLOW(304047, "主动消息推送超过限制的子频道数"),
    GUILD_PUSH_NOT_ALLOWED(304048, "不允许在此频道发主动消息"),
    DM_PUSH_RATE(304049, "私信主动消息数限频"),
    DM_PUSH_TOTAL_RATE(304050, "私信主动消息总量限频"),
    MESSAGE_GUIDE_REQUEST_ERROR(304051, "消息设置引导请求构造错误"),
    MESSAGE_GUIDE_RATE(304052, "发消息设置引导超频"),

    // ── 撤回 306001-306006 ──
    RETRACT_PARAM_INVALID(306001, "撤回消息参数错误"),
    RETRACT_MSGID_ERROR(306002, "消息 id 错误"),
    RETRACT_GET_MESSAGE_FAILED(306003, "获取消息错误(可重试)"),
    RETRACT_NO_PERMISSION(306004, "没有撤回此消息的权限"),
    RETRACT_ERROR(306005, "消息撤回失败(可重试)"),
    RETRACT_GET_CHANNEL_FAILED(306006, "获取子频道失败(可重试)"),

    // ── 公告/精华 501001-501020 ──
    ANNOUNCE_PARAM_INVALID(501001, "参数校验失败"),
    ANNOUNCE_SUB_CREATE_FAILED(501002, "创建子频道公告失败(可重试)"),
    ANNOUNCE_SUB_DELETE_FAILED(501003, "删除子频道公告失败(可重试)"),
    ANNOUNCE_GET_GUILD_FAILED(501004, "获取频道信息失败(可重试)"),
    ANNOUNCE_MESSAGE_ID_ERROR(501005, "MessageID 错误"),
    ANNOUNCE_GLOBAL_CREATE_FAILED(501006, "创建频道全局公告失败(可重试)"),
    ANNOUNCE_GLOBAL_DELETE_FAILED(501007, "删除频道全局公告失败(可重试)"),
    ANNOUNCE_MESSAGE_ID_NOT_EXIST(501008, "MessageID 不存在"),
    ANNOUNCE_MESSAGE_ID_PARSE_FAILED(501009, "MessageID 解析失败"),
    ANNOUNCE_NOT_SUB_MESSAGE(501010, "此条消息非子频道内消息"),
    PIN_CREATE_FAILED(501011, "创建精华消息失败(可重试)"),
    PIN_DELETE_FAILED(501012, "删除精华消息失败(可重试)"),
    PIN_MAX_COUNT(501013, "精华消息超过最大数量"),
    ANNOUNCE_SECURITY_HIT(501014, "安全打击"),
    ANNOUNCE_NOT_ALLOWED(501015, "此消息不允许设置"),
    ANNOUNCE_RECOMMEND_OVERFLOW(501016, "频道公告子频道推荐超过最大数量"),
    ANNOUNCE_NOT_ADMIN(501017, "非频道主或管理员"),
    ANNOUNCE_RECOMMEND_INVALID(501018, "推荐子频道 ID 无效"),
    ANNOUNCE_TYPE_ERROR(501019, "公告类型错误"),
    ANNOUNCE_RECOMMEND_CREATE_FAILED(501020, "创建推荐子频道类型频道公告失败"),

    // ── 禁言 502001-502010 ──
    MUTE_GUILD_ID_INVALID(502001, "频道 id 无效"),
    MUTE_GUILD_ID_EMPTY(502002, "频道 id 为空"),
    MUTE_USER_ID_INVALID(502003, "用户 id 无效"),
    MUTE_USER_ID_EMPTY(502004, "用户 id 为空"),
    MUTE_TIMESTAMP_ILLEGAL(502005, "timestamp 不合法"),
    MUTE_TIMESTAMP_INVALID(502006, "timestamp 无效"),
    MUTE_PARAM_CONVERT_ERROR(502007, "参数转换错误"),
    MUTE_RPC_FAILED(502008, "rpc 调用失败"),
    MUTE_SECURITY_HIT(502009, "安全打击"),
    MUTE_HEADER_ERROR(502010, "请求头错误"),

    // ── 帖子 503001-503020 ──
    THREAD_GUILD_ID_INVALID(503001, "频道 id 无效"),
    THREAD_GUILD_ID_EMPTY(503002, "频道 id 为空"),
    THREAD_GET_CHANNEL_FAILED(503003, "获取子频道信息失败"),
    THREAD_PUBLISH_RATE(503004, "超出发布帖子的频次限制"),
    THREAD_TITLE_EMPTY(503005, "帖子标题为空"),
    THREAD_CONTENT_EMPTY(503006, "帖子内容为空"),
    THREAD_ID_EMPTY(503007, "帖子ID为空"),
    THREAD_GET_XUIN_FAILED(503008, "获取X-Uin失败"),
    THREAD_ID_INVALID(503009, "帖子ID无效或不合法"),
    THREAD_TINYID_FAILED(503010, "通过Uin获取TinyID失败"),
    THREAD_TIMESTAMP_INVALID(503011, "帖子ID里面的时间戳无效或不合法"),
    THREAD_NOT_EXIST(503012, "帖子不存在或已删除"),
    THREAD_INTERNAL_ERROR(503013, "服务器内部错误"),
    THREAD_JSON_PARSE_FAILED(503014, "帖子JSON内容解析失败"),
    THREAD_CONTENT_CONVERT_FAILED(503015, "帖子内容转换失败"),
    THREAD_LINK_OVERFLOW(503016, "链接数量超过限制"),
    THREAD_WORDS_OVERFLOW(503017, "字数超过限制"),
    THREAD_IMAGE_OVERFLOW(503018, "图片数量超过限制"),
    THREAD_VIDEO_OVERFLOW(503019, "视频数量超过限制"),
    THREAD_TITLE_OVERFLOW(503020, "标题长度超过限制"),

    // ── 频率设置 504001-504004 ──
    RATE_SETTING_PARAM_INVALID(504001, "请求参数无效错误"),
    RATE_SETTING_HEADER_FAILED(504002, "获取 HTTP 头失败"),
    RATE_SETTING_GET_UIN_ERROR(504003, "获取 BOT UIN 错误"),
    RATE_SETTING_GET_INFO_ERROR(504004, "获取消息频率设置信息错误"),

    // ── 频道权限 610001-610014 ──
    GUILD_PERM_GET_GUILD_FAILED(610001, "获取频道 ID 失败"),
    GUILD_PERM_HEADER_FAILED(610002, "获取 HTTP 头失败"),
    GUILD_PERM_GET_UIN_FAILED(610003, "获取机器人号码失败"),
    GUILD_PERM_GET_ROLE_FAILED(610004, "获取机器人角色失败"),
    GUILD_PERM_ROLE_INTERNAL(610005, "获取机器人角色内部错误"),
    GUILD_PERM_LIST_FAILED(610006, "拉取机器人权限列表失败"),
    GUILD_PERM_NOT_IN_GUILD(610007, "机器人不在频道内"),
    GUILD_PERM_INVALID_PARAM(610008, "无效参数"),
    GUILD_PERM_GET_API_FAILED(610009, "获取 API 接口详情失败"),
    GUILD_PERM_API_AUTHED(610010, "API 接口已授权"),
    GUILD_PERM_GET_BOT_FAILED(610011, "获取机器人信息失败"),
    GUILD_PERM_RATE_FAILED(610012, "限频失败"),
    GUILD_PERM_RATED(610013, "已限频"),
    GUILD_PERM_AUTH_LINK_FAILED(610014, "api 授权链接发送失败"),

    // ── 表情表态 620001-620007 ──
    REACTION_PARAM_INVALID(620001, "表情表态无效参数"),
    REACTION_TYPE_OVERFLOW(620002, "已经达到表情反应的类型数量上限"),
    REACTION_ALREADY_SET(620003, "已经设置过该表情表态"),
    REACTION_NOT_SET(620004, "没有设置过该表情表态"),
    REACTION_NO_PERMISSION(620005, "没有权限设置表情表态"),
    REACTION_RATE(620006, "操作限频"),
    REACTION_FAILED(620007, "表情表态操作失败,请重试"),

    // ── 互动回调 630001-630007 ──
    INTERACTION_PARAM_INVALID(630001, "互动回调数据更新无效参数"),
    INTERACTION_GET_APPID_FAILED(630002, "互动回调数据更新获取AppID失败"),
    INTERACTION_APPID_MISMATCH(630003, "互动回调数据AppID不匹配"),
    INTERACTION_STORE_ERROR(630004, "互动回调数据更新内部存储错误"),
    INTERACTION_STORE_READ_ERROR(630005, "互动回调数据更新内部存储读取错误"),
    INTERACTION_READ_APPID_FAILED(630006, "互动回调数据更新读取请求AppID失败"),
    INTERACTION_TOO_LARGE(630007, "互动回调数据太大"),

    // ── 群消息发送 1000000-2999999 段 ──
    GROUP_SECURITY_RATE(1100100, "安全打击：消息被限频"),
    GROUP_SECURITY_SENSITIVE(1100101, "安全打击：内容涉及敏感"),
    GROUP_SECURITY_NO_TRIAL(1100102, "安全打击：暂未获得新功能体验资格"),
    GROUP_SECURITY(1100103, "安全打击"),
    GROUP_SECURITY_GUILD_INVALID(1100104, "安全打击：该群已失效或不存在"),
    GROUP_INTERNAL_ERROR(1100300, "系统内部错误"),
    GROUP_NOT_MEMBER(1100301, "调用方不是群成员"),
    GROUP_GET_CHANNEL_NAME_FAILED(1100302, "获取指定频道名称失败"),
    GROUP_HOME_NOT_ADMIN(1100303, "主页频道非管理员不允许发消息"),
    GROUP_AT_AUTH_FAILED(1100304, "@次数鉴权失败"),
    GROUP_TINYID_CONVERT_FAILED(1100305, "TinyId 转换 Uin 失败"),
    GROUP_NOT_PRIVATE_MEMBER(1100306, "非私有频道成员"),
    GROUP_NOT_WHITELIST(1100307, "非白名单应用子频道"),
    GROUP_CHANNEL_RATE(1100308, "触发频道内限频"),
    GROUP_OTHER_ERROR(1100499, "其他错误"),

    // ── 编辑消息 3000000-3999999 段 ──
    EDIT_SECURITY_HIT(3300006, "安全打击"),

    // 官方错误响应示例中出现的被动回复过期码
    REPLY_MSG_ID_EXPIRED(40034005, "回复消息msg_id已过期"),
    UNKNOWN(-1, "未知错误码");

    /** 官方标注"系统错误,重试一次会好"等可重试语义的码。 */
    val isRetryable: Boolean
        get() = code in RETRYABLE

    /** 权限/封禁类——提示授权或放弃。 */
    val isPermissionDenied: Boolean
        get() = code in PERMISSION_DENIED

    /** 限频类——退避或放弃。 */
    val isRateLimited: Boolean
        get() = code in RATE_LIMITED

    /** 被动回复 msg_id 过期类。 */
    val isMsgExpired: Boolean
        get() = code in MSG_EXPIRED

    companion object {
        private val byCode = entries.associateBy { it.code }

        private val RETRYABLE = setOf(11281, 11252, 11263, 11242, 306003, 306005, 620007)
        private val PERMISSION_DENIED = setOf(
            11253, 11254, 11282, 11264, 11273, 11274, 11265,
            304004, 304014, 304036, 304037, 306004, 501017,
        )
        private val RATE_LIMITED = setOf(
            20028, 1100100, 304045, 304046, 304047, 304048, 304049, 304050,
            304052, 304019, 503004, 610012, 610013, 620006, 1100308,
        )
        private val MSG_EXPIRED = setOf(304026, 304027, 40034005)

        /** 按原始 err_code 查找枚举，未收录时返回 [UNKNOWN]。 */
        fun fromCode(code: Int): QqErrorCode = byCode[code] ?: UNKNOWN
    }
}
