package org.jiu;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Maven 骨架生成的占位单元测试。
 */
public class AppTest
        extends TestCase {
    public AppTest(String testName) {
        super(testName);
    }

    /**
     * 返回包含本类全部用例的测试套件。
     */
    public static Test suite() {
        return new TestSuite(AppTest.class);
    }

    /**
     * 占位用例，仅验证测试框架可正常运行。
     */
    public void testApp() {
        assertTrue(true);
    }
}
