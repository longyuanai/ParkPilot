/*
 * Copyright (c) 2026 Jiawei Huang (黄家威). All Rights Reserved.
 *
 * Project: ParkPilot (智泊领航) - Smart Parking & EV Charging Core Engine
 * Licensed under the PolyForm Noncommercial License 1.0.0 (the "License").
 * You may not use this file except in compliance with the License.
 *
 * Commercial Use strictly requires a separate commercial license from the author.
 * For commercial licensing, please refer to COMMERCIAL.md or contact: 1507205727@qq.com
 */
package com.zhixingyuebo.test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import javax.imageio.ImageIO;

/** 只读验收界面：展示本次原服务调用输出及真实 JDBC SELECT 前后快照。 */
public final class EvidenceWindow {
    private static final Set<String> EXPORTED_CASES = new HashSet<>(Arrays.asList(
            "TC-01", "TC-02", "TC-03", "TC-04", "TC-05", "TC-06", "A-01", "M-01"));
    private EvidenceWindow() { }

    /** 从本次 Swing 验收界面直接导出像素，内容来自已运行用例的输出和数据库快照。 */
    public static void export(java.util.List<TestCaseVerifier.Result> results, String schema, Path output) throws Exception {
        Files.createDirectories(output);
        for (TestCaseVerifier.Result result : results) {
            if (!EXPORTED_CASES.contains(result.id)) continue;
            BufferedImage[] image = new BufferedImage[1];
            SwingUtilities.invokeAndWait(() -> image[0] = capture(result, schema));
            if (!ImageIO.write(image[0], "png", output.resolve(result.id + ".png").toFile())) {
                throw new IOException("无法保存验收截图 " + result.id);
            }
        }
    }

    private static BufferedImage capture(TestCaseVerifier.Result result, String schema) {
        String console = normalize(result.console);
        String database = normalize(result.database);
        JPanel page = new JPanel(new BorderLayout(18, 16));
        page.setBackground(new Color(243, 247, 251));
        page.setBorder(BorderFactory.createEmptyBorder(24, 28, 22, 28));
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 6));
        header.setOpaque(false);
        JLabel title = new JLabel("智行悦泊  |  " + result.id + "  " + result.title + "  " + (result.passed ? "PASS" : "FAIL"));
        title.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 28));
        title.setForeground(new Color(21, 42, 65));
        header.add(title);
        header.add(new JLabel("本轮独立 MySQL 测试库  " + schema));
        page.add(header, BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridLayout(1, 2, 16, 0));
        columns.setOpaque(false);
        columns.add(evidenceColumn("控制台输出", console));
        columns.add(evidenceColumn("JDBC 查询前后快照", database));
        page.add(columns, BorderLayout.CENTER);
        page.add(new JLabel("来源  TestDatabaseRunner / TestCaseVerifier 本轮实际执行输出"), BorderLayout.SOUTH);
        int width = 1640;
        int lines = Math.max(console.split("\n").length, database.split("\n").length);
        int height = Math.max(520, 220 + (lines + 4) * 23);
        page.setSize(width, height);
        layout(page);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            page.printAll(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static JPanel evidenceColumn(String heading, String text) {
        JPanel column = new JPanel(new BorderLayout(0, 12));
        column.setBackground(Color.WHITE);
        column.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JLabel label = new JLabel(heading);
        label.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 20));
        label.setForeground(new Color(16, 80, 91));
        column.add(label, BorderLayout.NORTH);
        JTextArea contents = new JTextArea(text);
        contents.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 17));
        contents.setEditable(false);
        contents.setLineWrap(true);
        contents.setWrapStyleWord(false);
        column.add(contents, BorderLayout.CENTER);
        return column;
    }

    private static String normalize(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container) layout((Container) child);
        }
    }
    public static void showAndWait(java.util.List<TestCaseVerifier.Result> results,String schema) throws Exception {
        CountDownLatch closed=new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame=new JFrame("智行悦泊 真实运行验收 黄家威 20233130174");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            javax.swing.Timer timeout = new javax.swing.Timer(15 * 60 * 1000, event -> frame.dispose());
            timeout.setRepeats(false);
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosed(WindowEvent event) {
                    timeout.stop();
                    closed.countDown();
                }
            });
            JPanel panel=new JPanel(new BorderLayout(8,8));panel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
            JLabel title=new JLabel("智行悦泊 真实 JDBC 验收  |  "+schema+"  |  所有数据来自独立测试库");
            title.setFont(new Font("Microsoft YaHei UI",Font.BOLD,18));panel.add(title,BorderLayout.NORTH);
            JTabbedPane tabs=new JTabbedPane();tabs.setFont(new Font("Microsoft YaHei UI",Font.PLAIN,16));
            Set<String> selected=new HashSet<>(Arrays.asList("TC-01","TC-02","TC-03","TC-04","TC-05","F-01","F-05","A-01","B-01","M-01"));
            for(TestCaseVerifier.Result result:results)if(selected.contains(result.id)) {
                JPanel item=new JPanel(new BorderLayout(6,6));
                JLabel heading=new JLabel(result.id+"  "+result.title+"  "+(result.passed?"PASS":"FAIL"));
                heading.setFont(new Font("Microsoft YaHei UI",Font.BOLD,20));item.add(heading,BorderLayout.NORTH);
                JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,area("实际控制台输出\n\n"+result.console),area(result.database));
                split.setResizeWeight(0.51);item.add(split,BorderLayout.CENTER);tabs.addTab(result.id,item);
            }
            panel.add(tabs,BorderLayout.CENTER);
            panel.add(new JLabel("当前数据库保留 TC-02 复现结果供只读核验。关闭窗口或 15 分钟后自动清理测试库。"),BorderLayout.SOUTH);
            frame.setContentPane(panel);frame.setSize(1800,980);frame.setLocationRelativeTo(null);frame.setVisible(true);
            timeout.start();
        });
        closed.await();
    }
    private static JScrollPane area(String text) {
        JTextArea area=new JTextArea(text.replace('\r','\n'));area.setEditable(false);
        area.setFont(new Font("Microsoft YaHei UI",Font.PLAIN,16));area.setMargin(new Insets(10,10,10,10));area.setCaretPosition(0);
        return new JScrollPane(area);
    }
}
