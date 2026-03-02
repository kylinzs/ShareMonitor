package com.codex.sharemonitor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class NavigationSmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigation_navigatesAcrossScreens() {
        rule.onNodeWithText("还没有添加自选股").assertIsDisplayed()

        rule.onNodeWithText("搜索").performClick()
        rule.onNodeWithText("代码 / 名称").assertIsDisplayed()

        rule.onNodeWithText("行情").performClick()
        rule.onNodeWithText("指数").assertIsDisplayed()
        rule.onNodeWithText("板块").assertIsDisplayed()

        rule.onNodeWithText("设置").performClick()
        rule.onNodeWithText("免责声明：本应用仅供自用参考，行情数据来源于公开接口，可能存在延迟与误差。").assertIsDisplayed()
    }
}
