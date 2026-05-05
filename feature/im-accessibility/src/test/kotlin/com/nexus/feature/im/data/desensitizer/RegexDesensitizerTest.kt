package com.nexus.feature.im.data.desensitizer

import com.google.common.truth.Truth.assertThat
import com.nexus.feature.im.domain.model.RawMessage
import org.junit.Test

class RegexDesensitizerTest {

    private val desensitizer = RegexDesensitizer(DefaultDesensitizationRules.ALL)

    private fun message(content: String) = RawMessage(
        packageName = "com.tencent.mm",
        senderName = "Test",
        content = content,
        timestampMs = 0L,
    )

    @Test
    fun `phone number is masked`() {
        val result = desensitizer.process(message("联系我 13812345678 随时"))
        assertThat(result.content).isEqualTo("联系我 [PHONE] 随时")
    }

    @Test
    fun `id card is masked`() {
        val result = desensitizer.process(message("身份证号 110101199001011234"))
        assertThat(result.content).isEqualTo("身份证号 [ID_CARD]")
    }

    @Test
    fun `bank card is masked`() {
        val result = desensitizer.process(message("卡号 6222021234567890123"))
        assertThat(result.content).isEqualTo("卡号 [BANK]")
    }

    @Test
    fun `password keyword is masked`() {
        val result = desensitizer.process(message("密码: mySecret123"))
        assertThat(result.content).isEqualTo("[PWD]")
    }

    @Test
    fun `address is masked`() {
        val result = desensitizer.process(message("送到 北京市海淀区中关村大街1号"))
        assertThat(result.content).contains("[ADDR]")
    }

    @Test
    fun `multiple rules apply in order`() {
        val result = desensitizer.process(message("电话13812345678，地址北京市海淀区中关村大街1号"))
        assertThat(result.content).contains("[PHONE]")
        assertThat(result.content).contains("[ADDR]")
    }

    @Test
    fun `message without sensitive data is unchanged`() {
        val original = "下午三点开会讨论项目进度"
        val result = desensitizer.process(message(original))
        assertThat(result.content).isEqualTo(original)
    }
}
