package com.example.videoagent.e2e;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VueSpaE2eTest {

    @LocalServerPort
    private int port;

    private static Process appProcess;

    @BeforeAll
    static void startApp() throws Exception {
        // Build frontend first
        ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "package", "-DskipTests");
        pb.redirectErrorStream(ProcessBuilder.Redirect.INHERIT);
        pb.directory(new File(System.getProperty("user.dir")).getAbsolutePath());
        pb.start();
        pb.waitFor();

        // Start Spring Boot app
        appProcess = new ProcessBuilder("java", "-jar",
                "target/video-agent-0.0.1-SNAPSHOT.jar")
                .directory(new File(System.getProperty("user.dir")).getAbsolutePath())
                .start();

        // Wait for app to start
        Thread.sleep(10000);
    }

    @Test
    void testPageLoadsAndHasExpectedElements() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(false)
                    .setSlowMo(5000));

            Page page = browser.newPage();
            page.navigate("http://localhost:" + port);

            // Wait for Vue app to load
            page.waitForLoadState(LoadState.NETWORKIDLE, Duration.ofSeconds(10));

            // Verify page title
            assertEquals("frontend", page.title());

            // Verify main heading
            assertTrue(page.locator("h1.app-title").isVisible());
            assertTrue(page.locator("h1.app-title").textContent().contains("长视频智能分析助手"));

            // Verify upload section
            assertTrue(page.locator(".card-header", HasText("上传字幕文件")).isVisible());

            // Verify "使用示例字幕" button
            assertTrue(page.locator("button", HasText("使用示例字幕")).isVisible());

            browser.close();
        }
    }

    @Test
    void testUseSampleSubtitleAndVerifyQuickActions() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(false)
                    .setSlowMo(5000));

            Page page = browser.newPage();
            page.navigate("http://localhost:" + port);
            page.waitForLoadState(LoadState.NETWORKIDLE, Duration.ofSeconds(10));

            // Click "使用示例字幕" button
            page.locator("button", HasText("使用示例字幕")).click();

            // Wait for subtitle to load
            Thread.sleep(2000);

            // Verify "字幕信息" card appears
            assertTrue(page.locator(".card-header", HasText("字幕信息")).isVisible());

            // Verify "快捷操作" card appears
            assertTrue(page.locator(".card-header", HasText("快捷操作")).isVisible());

            // Verify quick action buttons
            assertTrue(page.locator("button", HasText("生成摘要")).isVisible());
            assertTrue(page.locator("button", HasText("提取概念")).isVisible());
            assertTrue(page.locator("button", HasText("提取金句")).isVisible());
            assertTrue(page.locator("button", HasText("关键词搜索")).isVisible());

            // Verify "智能问答" card appears
            assertTrue(page.locator(".card-header", HasText("智能问答")).isVisible());

            // Verify chat input
            assertTrue(page.locator("textarea").isVisible());

            // Verify send button
            assertTrue(page.locator("button", HasText("发送")).isVisible());

            // Verify stream checkbox
            assertTrue(page.locator("input[type='checkbox']").isVisible());

            browser.close();
        }
    }

    @Test
    void testChatFunctionality() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(false)
                    .setSlowMo(5000));

            Page page = browser.newPage();
            page.navigate("http://localhost:" + port);
            page.waitForLoadState(LoadState.NETWORKIDLE, Duration.ofSeconds(10));

            // Click "使用示例字幕" button
            page.locator("button", HasText("使用示例字幕")).click();

            // Wait for subtitle to load
            Thread.sleep(2000);

            // Type a question
            page.locator("textarea").fill("这个视频讲了什么?");

            // Click send button
            page.locator("button", HasText("发送")).click();

            // Wait for response
            Thread.sleep(3000);

            // Verify response appears
            assertTrue(page.locator(".message.assistant").isVisible());

            browser.close();
        }
    }

    @AfterAll
    static void stopApp() throws Exception {
        if (appProcess != null) {
                appProcess.destroy();
            }
        }
    }

    // Helper class for text content matching
    static class HasText implements Condition<String> {
        private final String text;

        public HasText(String text) {
            this.text = text;
        }

        @Override
        public boolean test(String actual) {
                return actual.contains(text);
            }
        }
    }
}
