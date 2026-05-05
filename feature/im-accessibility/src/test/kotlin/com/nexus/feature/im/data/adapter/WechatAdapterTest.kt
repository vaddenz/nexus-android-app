package com.nexus.feature.im.data.adapter

import com.google.common.truth.Truth.assertThat
import com.nexus.feature.im.domain.model.NodeSnapshot
import org.junit.Test

class WechatAdapterTest {

    private val adapter = WechatAdapter()

    @Test
    fun `parseMessage extracts sender and content from typical chat item`() {
        val root = NodeSnapshot(
            children = listOf(
                NodeSnapshot(text = "李总"),
                NodeSnapshot(text = "今天下午的会议改到三点了"),
            ),
        )

        val msg = adapter.parseMessage(root)

        assertThat(msg).isNotNull()
        assertThat(msg!!.senderName).isEqualTo("李总")
        assertThat(msg.content).isEqualTo("今天下午的会议改到三点了")
        assertThat(msg.confidence).isGreaterThan(0.5f)
    }

    @Test
    fun `parseMessage returns null for empty tree`() {
        val root = NodeSnapshot()
        assertThat(adapter.parseMessage(root)).isNull()
    }

    @Test
    fun `parseMessage skips system texts`() {
        val root = NodeSnapshot(
            children = listOf(
                NodeSnapshot(text = "微信"),
                NodeSnapshot(text = "通讯录"),
            ),
        )
        assertThat(adapter.parseMessage(root)).isNull()
    }

    @Test
    fun `supportedPackages contains WeChat`() {
        assertThat(adapter.supportedPackages).contains("com.tencent.mm")
    }

    @Test
    fun `parseMessage handles longer content correctly`() {
        val root = NodeSnapshot(
            children = listOf(
                NodeSnapshot(text = "产品经理"),
                NodeSnapshot(text = "需求文档已经更新了，请大家在本周五前完成评审并反馈意见。"),
            ),
        )

        val msg = adapter.parseMessage(root)
        assertThat(msg).isNotNull()
        assertThat(msg!!.senderName).isEqualTo("产品经理")
        assertThat(msg.content).contains("需求文档")
    }
}
