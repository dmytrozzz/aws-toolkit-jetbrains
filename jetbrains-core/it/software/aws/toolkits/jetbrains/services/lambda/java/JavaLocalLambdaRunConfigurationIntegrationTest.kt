// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.lambda.java

import com.intellij.compiler.CompilerTestUtil
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.testFramework.runInEdtAndWait
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.aws.toolkits.jetbrains.core.credentials.MockCredentialsManager
import software.aws.toolkits.jetbrains.services.lambda.execution.local.createHandlerBasedRunConfiguration
import software.aws.toolkits.jetbrains.services.lambda.execution.local.createTemplateRunConfiguration
import software.aws.toolkits.jetbrains.settings.SamSettings
import software.aws.toolkits.jetbrains.utils.addBreakpoint
import software.aws.toolkits.jetbrains.utils.checkBreakPointHit
import software.aws.toolkits.jetbrains.utils.executeRunConfiguration
import software.aws.toolkits.jetbrains.utils.rules.HeavyJavaCodeInsightTestFixtureRule
import software.aws.toolkits.jetbrains.utils.rules.addClass
import software.aws.toolkits.jetbrains.utils.rules.addModule
import software.aws.toolkits.jetbrains.utils.setUpGradleProject

class JavaLocalLambdaRunConfigurationIntegrationTest {
    @Rule
    @JvmField
    val projectRule = HeavyJavaCodeInsightTestFixtureRule()

    private val mockId = "MockCredsId"
    private val mockCreds = AwsBasicCredentials.create("Access", "ItsASecret")

    @Before
    fun setUp() {
        SamSettings.getInstance().savedExecutablePath = System.getenv()["SAM_CLI_EXEC"]

        val fixture = projectRule.fixture
        val module = fixture.addModule("main")
        val psiClass = fixture.addClass(
            module,
            """
            package com.example;

            public class LambdaHandler {
                public String handleRequest(String request) {
                    return request.toUpperCase();
                }
            }
            """
        )

        projectRule.setUpGradleProject()

        runInEdtAndWait {
            fixture.openFileInEditor(psiClass.containingFile.virtualFile)
        }

        MockCredentialsManager.getInstance().addCredentials(mockId, mockCreds)
    }

    @After
    fun tearDown() {
        CompilerTestUtil.disableExternalCompiler(projectRule.project)
        MockCredentialsManager.getInstance().reset()
    }

    @Test
    fun samIsExecuted() {
        val runConfiguration = createHandlerBasedRunConfiguration(
            project = projectRule.project,
            input = "\"Hello World\"",
            credentialsProviderId = mockId
        )
        assertThat(runConfiguration).isNotNull

        val executeLambda = executeRunConfiguration(runConfiguration)

        assertThat(executeLambda.exitCode).isEqualTo(0)
        assertThat(executeLambda.stdout).contains("HELLO WORLD")
    }

    @Test
    fun samIsExecutedWhenRunWithATemplateServerless() {
        val templateFile = projectRule.fixture.addFileToProject(
            "template.yaml", """
            Resources:
              SomeFunction:
                Type: AWS::Serverless::Function
                Properties:
                  Handler: com.example.LambdaHandler::handleRequest
                  CodeUri: main
                  Runtime: java8
                  Timeout: 900
        """.trimIndent()
        )

        val runConfiguration = createTemplateRunConfiguration(
            project = projectRule.project,
            templateFile = templateFile.containingFile.virtualFile.path,
            logicalId = "SomeFunction",
            input = "\"Hello World\"",
            credentialsProviderId = mockId
        )

        assertThat(runConfiguration).isNotNull

        val executeLambda = executeRunConfiguration(runConfiguration)

        assertThat(executeLambda.exitCode).isEqualTo(0)
        assertThat(executeLambda.stdout).contains("HELLO WORLD")
    }

    @Test
    fun samIsExecutedWhenRunWithATemplateLambda() {
        val templateFile = projectRule.fixture.addFileToProject(
            "template.yaml", """
            Resources:
              SomeFunction:
                Type: AWS::Lambda::Function
                Properties:
                  Handler: com.example.LambdaHandler::handleRequest
                  Code: main
                  Runtime: java8
                  Timeout: 900
        """.trimIndent()
        )

        val runConfiguration = createTemplateRunConfiguration(
            project = projectRule.project,
            templateFile = templateFile.containingFile.virtualFile.path,
            logicalId = "SomeFunction",
            input = "\"Hello World\"",
            credentialsProviderId = mockId
        )

        assertThat(runConfiguration).isNotNull

        val executeLambda = executeRunConfiguration(runConfiguration)

        assertThat(executeLambda.exitCode).isEqualTo(0)
        assertThat(executeLambda.stdout).contains("HELLO WORLD")
    }

    @Test
    fun samIsExecutedWithDebugger() {
        projectRule.addBreakpoint()

        val runConfiguration = createHandlerBasedRunConfiguration(
            project = projectRule.project,
            input = "\"Hello World\"",
            credentialsProviderId = mockId
        )
        assertThat(runConfiguration).isNotNull

        val debuggerIsHit = checkBreakPointHit(projectRule.project)

        val executeLambda = executeRunConfiguration(runConfiguration, DefaultDebugExecutor.EXECUTOR_ID)

        assertThat(executeLambda.exitCode).isEqualTo(0)
        assertThat(executeLambda.stdout).contains("HELLO WORLD")

        assertThat(debuggerIsHit.get()).isTrue()
    }
}
