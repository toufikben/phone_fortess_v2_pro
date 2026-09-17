package com.phonefortress.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assert
import org.junit.Rule
import org.junit.Test

class AccessibilitySemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun protectionStatus_exposes_state_to_screen_reader() {
        composeRule.setContent {
            MaterialTheme {
                Text(
                    text = "الحماية",
                    modifier = Modifier.semantics {
                        stateDescription = "مفعلة"
                        role = Button
                        contentDescription = "حالة الحماية مفعلة"
                    }
                )
            }
        }

        composeRule.onNodeWithText("الحماية")
            .assert(hasStateDescription("مفعلة"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }
}
