package com.nexus.feature.im.data.desensitizer

import com.nexus.feature.im.domain.desensitizer.Desensitizer
import com.nexus.feature.im.domain.desensitizer.DesensitizationRule
import com.nexus.feature.im.domain.model.RawMessage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [Desensitizer] implementation that applies a configurable list of
 * regex-based [DesensitizationRule]s to message content.
 */
@Singleton
class RegexDesensitizer @Inject constructor(
    private val rules: List<DesensitizationRule>,
) : Desensitizer {

    override fun process(message: RawMessage): RawMessage {
        var text = message.content
        for (rule in rules) {
            if (!rule.isEnabled) continue
            text = rule.pattern.replace(text, rule.replacement)
        }
        return if (text == message.content) message else message.copy(content = text)
    }
}

/**
 * Pre-defined desensitization rules covering the 5 required sensitive patterns.
 */
object DefaultDesensitizationRules {

    /** 手机号 */
    val PHONE = DesensitizationRule(
        id = "PHONE",
        name = "手机号",
        pattern = Regex("""(?<![\d])1[3-9]\d{9}(?![\d])"""),
        replacement = "[PHONE]",
    )

    /** 身份证号 */
    val ID_CARD = DesensitizationRule(
        id = "ID_CARD",
        name = "身份证号",
        pattern = Regex("""(?<![\d])\d{17}[\dXx](?![\d])"""),
        replacement = "[ID_CARD]",
    )

    /** 银行卡号 */
    val BANK_CARD = DesensitizationRule(
        id = "BANK_CARD",
        name = "银行卡号",
        pattern = Regex("""(?<![\d])(?:4\d{15}|\d{16,19})(?![\d])"""),
        replacement = "[BANK]",
    )

    /** 密码 */
    val PASSWORD = DesensitizationRule(
        id = "PASSWORD",
        name = "密码",
        pattern = Regex("""(?i)(密码|password|pwd)[\s:=]*\S+"""),
        replacement = "[PWD]",
    )

    /** 地址（省/市/区 + 街道/路/号 组合） */
    val ADDRESS = DesensitizationRule(
        id = "ADDRESS",
        name = "地址",
        pattern = Regex(
            """([\u4e00-\u9fa5]{2,10}(?:省|自治区|市|区|县))\s*[\u4e00-\u9fa5]{2,20}(?:街道|镇|乡|路|街|巷|号|栋|单元|室)[\u4e00-\u9fa5\d\-#]{2,30}"""
        ),
        replacement = "[ADDR]",
    )

    val ALL = listOf(PHONE, ID_CARD, BANK_CARD, PASSWORD, ADDRESS)
}
