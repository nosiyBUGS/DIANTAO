package art.qiuye.diantao;

import art.qiuye.diantao.db.DianConfig;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.io.file.LineSeparator;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.setting.Setting;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import lombok.SneakyThrows;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.text.PlainDocument;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static art.qiuye.diantao.Constants.*;

public class DIANTAO_UI {

        private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

        static Log StaticLog = LogFactory.get();
        private static Config config;

        private String device;
        private String adb_path;
        private String path;
        private JCheckBox checkBox_cis;

        private String cmd_start = path + "shell am start -n com.alibaba.android.rimet/com.alibaba.android.rimet.biz.LaunchHomeActivity";
        private String cmd_stop = path + "shell am force-stop com.alibaba.android.rimet";
        //        private JComboBox<Integer> comboBox_count;
//        private JComboBox<Integer> comboBox_time;
//        private JComboBox<Integer> comboBox_interval;
//        private JTextField textField_X;
//        private JComboBox<ComboLabel<Integer>> comboBox_Y;
//        private Vector<ComboLabel<Integer>> items_Y;
        String cmd_cap1 = path + "shell screencap  -p /sdcard/snapcap/cap.png";
        String cmd_cap2 = path + "pull /sdcard/snapcap/cap.png " + path + "cap.png";
        String cmd_tap;
        String cmd_press;
        String cmd_swipe;

        String cmd_current;

        private Types[] types;

        private Thread thread;
        private Thread monitorThread;
        private Vector<ComboLabel<Integer>> items_walk;
        private Vector<ComboLabel<Integer>> items_work;
//        private Vector<ComboLabel<Vector<String>>> items_return;

        private int offset_arow = 0;

        private boolean cis_direction = false;

        private String lastConfig;

        static Setting setting = new Setting();
        private JTextArea ta_plan;
        private JButton button_stop;
        private JButton button_batch;
        private JTextArea status_area;
        private MiniCap miniCap;
        private String[] activityNames;

        private boolean thread_suspend = false;
        private JTextArea status_bar;
        private static JFrame frame;
        private static JDesktopPane desktopPane;

        private static final List<DIANTAO_UI> intances = new ArrayList<>();
        private JInternalFrame iFrame;
        private JPanel manualPane;

        private static final String LF = LineSeparator.LINUX.getValue();
        private static final String CR_LF = LineSeparator.LINUX.getValue();

        public static final Db db = Db.use();

        public static Connection connection;

        private static final Map<Component, Component> referencees = new HashMap<>();
//        private static Map<JInternalFrame, Map<String, Component>> iFrameMap = new HashMap<>();

        static {
                setting.set("host", "smtp.qq.com");
                setting.set("port", "465");
                setting.set("sslEnable", "true");
                setting.set("from", "1281886693@qq.com");
                setting.set("pass", "tqblyhamaeqxijaj");
        }

        private final static MailAccount account = new MailAccount(setting);
        private String current_quick_item;
        private AbstractAction ctrl_s_action;
        private JButton button_save;
        private JButton button_F3;

        public static void main(String[] args) {
                DIANTAO_UI ui = new DIANTAO_UI();

                SwingUtilities.invokeLater(() -> {
                        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
                                e.printStackTrace();
                                JOptionPane.showMessageDialog(null, e, "ERROR-" + config.config_name, JOptionPane.ERROR_MESSAGE);
                        });
                        try {
                                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                        } catch (Exception e) {
                                throw new RuntimeException(e);
                        }
                        frame = new JFrame();
                        frame.setTitle(args[0] + "-操作-" + frame.hashCode());
                        desktopPane = new JDesktopPane();

                });
                ui.main(args[0]);
        }

        private void initDevice() {
                path = adb_path + device;
                cmd_tap = path + "shell input tap ";
                cmd_press = path + "shell input keyevent ";
                cmd_swipe = path + "shell input swipe ";
                cmd_cap = path + "exec-out screencap -p ";
                cmd_current = path + "shell dumpsys activity activities | grep mResumedActivity";
        }

        private void main(String confName) {
                initConfig(confName);
                main(config.devices.get(0), true);
        }

        @SneakyThrows
        private void main(String dev, boolean first) {
                if (!first) initConfig(config.config_name);
                adb_path = config.adb_path;
                device = dev;
                initDevice();
                ThreadUtil.execute(() -> {
                        String c = RuntimeUtil.execForStr(StandardCharsets.UTF_8, adb_path + " connect " + device.trim().split("\\s+")[1]);
                        writeStatus("##adb connect:{}", c);
                });
                ThreadUtil.execute(() -> {
                        try {
                                sleep(1);
                                if (connection == null || connection.isClosed()) {
                                        writeStatus("db connecting...");
                                        connection = db.getConnection();
                                        writeStatus("db connected ");

                                } else writeStatus("db already connect");
                        } catch (SQLException e) {
                                throw new RuntimeException(e);
                        }
                });
                SwingUtilities.invokeLater(() -> {
                        manualPane = new JPanel();
                        iFrame = new JInternalFrame(this + "-" + intances.size(), true, true, true);
                        iFrame.setSize(540, 750);
                        iFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                        iFrame.addInternalFrameListener(new InternalFrameAdapter() {
                                @Override
                                public void internalFrameClosed(InternalFrameEvent e) {
                                        onClose(true);
                                        intances.remove(DIANTAO_UI.this);
                                        StaticLog.info("##closed,threads stop,instances:{}", intances.size());
                                }
                        });
                        if (!intances.isEmpty()) {
                                iFrame.setLocation(100, 0);
                        }
                        intances.add(this);
                        desktopPane.add(iFrame);
                        iFrame.add(manualPane);
                        iFrame.setVisible(true);
                        JMenuBar menuBar = new JMenuBar();
                        JMenu quick_menu = new JMenu("快捷操作");
                        JMenu offset_menu = new JMenu("偏移");
                        JMenu reload_menu = new JMenu("重载配置");
                        JMenu upload_menu = new JMenu("上传配置");
                        JMenu down_menu = new JMenu("下载配置");
                        JMenuBar frameBar = new JMenuBar();
                        JMenu new_win_menu = new JMenu("新窗口");
                        frameBar.add(new_win_menu);
                        iFrame.setJMenuBar(menuBar);
                        frame.setJMenuBar(frameBar);
                        MouseAdapter mouseAdapter = new MouseAdapter() {
                                @Override
                                public void mouseClicked(MouseEvent e) {
                                        if (e.getSource() instanceof JTextArea) {
                                                iFrame = (JInternalFrame) ((JTextArea) e.getSource()).getRootPane().getParent();
                                                ta_plan = (JTextArea) e.getSource();
                                        }
                                        onClose(true);
                                        lastConfig = ta_plan.getText();
                                        iFrame.setVisible(false);
                                        try {
                                                iFrame.setClosed(true);
                                        } catch (PropertyVetoException ex) {
                                                throw new RuntimeException(ex);
                                        }
                                        iFrame.dispose();
                                        main(device, false);

                                }
                        };
                        reload_menu.addMouseListener(mouseAdapter);
                        new_win_menu.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseClicked(MouseEvent e) {
                                        new DIANTAO_UI().main(device, false);
                                }
                        });
                        upload_menu.addMouseListener(new MouseAdapter() {
                                                             @SneakyThrows
                                                             @Override
                                                             public void mouseClicked(MouseEvent e) {
                                                                     DianConfig dianConfig = db.get(new Entity(TB_CONFIG).set(FD_VENDOR, config.config_name)).toBean(DianConfig.class);
                                                                     String quick_value = FileUtil.readUtf8String(config.quicks_path);
                                                                     if (SecureUtil.md5(dianConfig.quick).equals(SecureUtil.md5(quick_value))) {
                                                                             JOptionPane.showMessageDialog(iFrame, config.config_name + ":hash equal,not upload", "info", JOptionPane.INFORMATION_MESSAGE);
                                                                             return;
                                                                     }
                                                                     Entity entity = Entity.create(TB_CONFIG);
                                                                     String config_value = FileUtil.readUtf8String(config.config_path);
//                                                                 int version = Integer.parseInt(ReUtil.getGroup0(PATTERN_VERSION, config_value));
//                                                                 config_value=ReUtil.replaceAll(config_value, PATTERN_VERSION, String.valueOf(version + 1));
                                                                     entity.set(FD_VENDOR, config.config_name)
                                                                             .set("quick", quick_value)
                                                                             .set("config", config_value)
                                                                             .set("updateTime", Timestamp.from(Instant.now()));
                                                                     try {
                                                                             db.insertOrUpdate(entity, FD_VENDOR);
                                                                     } catch (SQLException ex) {
                                                                             ex.printStackTrace();
                                                                             JOptionPane.showMessageDialog(iFrame, ex.getMessage(), "失败", JOptionPane.ERROR_MESSAGE);
                                                                             return;
                                                                     }
                                                                     JOptionPane.showMessageDialog(iFrame, config.config_name, "成功", JOptionPane.INFORMATION_MESSAGE);
                                                             }
                                                     }
                        );
                        down_menu.addMouseListener(new MouseAdapter() {
                                @SneakyThrows
                                @Override
                                public void mouseClicked(MouseEvent e) {
                                        DianConfig dianConfig = null;
                                        try {
                                                Entity entity = db.get(new Entity(TB_CONFIG).set(FD_VENDOR, config.config_name));
                                                dianConfig = entity.toBean(DianConfig.class);
                                                dianConfig.config = JSON.toJavaObject(JSON.parseObject(entity.getStr("config")), Config.class);
                                        } catch (SQLException ex) {
                                                ex.printStackTrace();
                                                JOptionPane.showMessageDialog(iFrame, ex.getMessage(), "下载失败", JOptionPane.ERROR_MESSAGE);
                                                return;
                                        }
                                        if (dianConfig.config.version > config.version) {
                                                File c = new File(config.config_path);
                                                File q = new File(config.quicks_path);
                                                FileUtil.copy(c, new File(config.version_path + c.getName() + "." + config.version), true);
                                                FileUtil.copy(c, new File(config.version_path + q.getName() + "." + config.version), true);
                                        } else {
                                                JOptionPane.showMessageDialog(iFrame, "数据库版本更旧，无需下载：" + dianConfig.config.version);
                                                return;
                                        }
//                                    dianConfig.config = dianConfig.config.replaceAll(CR_LF, LF);
                                        FileUtil.writeUtf8String(dianConfig.config.toString(), config.config_path);

                                        dianConfig.quick = dianConfig.quick.replaceAll(CR_LF, LF);
                                        FileUtil.writeUtf8String(dianConfig.quick, config.quicks_path);
                                        JOptionPane.showMessageDialog(iFrame, "下载成功");
                                }
                        });
                        ButtonGroup offset_buttonGroup = new ButtonGroup();
                        ItemListener offsetItemlistener = e -> {
                                String text = ((JRadioButtonMenuItem) e.getItem()).getText();
                                offset_arow = Integer.parseInt(text);
                        };
                        for (int i = -5; i <= 5; i++) {
                                JRadioButtonMenuItem b = new JRadioButtonMenuItem(String.valueOf(i));
                                b.addItemListener(offsetItemlistener);
                                offset_buttonGroup.add(b);
                                offset_menu.add(b);
                        }
                        menuBar.add(quick_menu);
                        menuBar.add(offset_menu);
                        menuBar.add(reload_menu);
                        menuBar.add(upload_menu);
                        menuBar.add(down_menu);


                        iFrame.addInternalFrameListener(new InternalFrameAdapter() {
                                @Override
                                public void internalFrameActivated(InternalFrameEvent e) {
                                        if (thread == null || thread.getState().equals(Thread.State.TERMINATED))
                                                manualPane.setBackground(Color.CYAN);

                                }

                                @Override
                                public void internalFrameDeactivated(InternalFrameEvent e) {
                                        if (thread == null || thread.getState().equals(Thread.State.TERMINATED))
                                                manualPane.setBackground(Color.WHITE);

                                }
                        });
                        //stop
                        manualPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F2"), "stop");
                        manualPane.getActionMap().put("stop", new AbstractAction() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                        button_stop.doClick();
                                }
                        });
                        //start
                        manualPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F1"), "start");
                        manualPane.getActionMap().put("start", new AbstractAction() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                        button_batch.doClick();
                                }
                        });
                        frame.setContentPane(desktopPane);

                        checkBox_cis = new JCheckBox("顺式", false);
                        checkBox_cis.addActionListener(e -> cis_direction = checkBox_cis.isSelected());
                        manualPane.add(checkBox_cis);

//                    JLabel label_count;
//                    label_count = new JLabel("次数");
//                    manualPane.add(label_count);
//                    Vector<Integer> items_count=config.items_count;
//                    comboBox_count = new JComboBox<>(items_count);
//                    comboBox_count.setMaximumRowCount(items_count.size());
//                    comboBox_count.setEditable(true);
//                    manualPane.add(comboBox_count);

//                    JLabel label_time = new JLabel("时长");
//                    manualPane.add(label_time);
//                    Vector<Integer> items_time=config.items_time;
//                    comboBox_time = new JComboBox<>(items_time);
//                    comboBox_time.setMaximumRowCount(items_time.size());
//                    comboBox_time.setEditable(true);
//                    manualPane.add(comboBox_time);

//                    JLabel label_interval = new JLabel("间隔时间");
//                    manualPane.add(label_interval);
//                    Vector<Integer> items_interval=config.items_interval;
//                    comboBox_interval = new JComboBox<>(items_interval);
//                    comboBox_interval.setMaximumRowCount(items_interval.size());
//                    comboBox_interval.setEditable(true);
//                    manualPane.add(comboBox_interval);

//                    JPanel panel_X=new JPanel();
//                    JLabel label_X = new JLabel("X");
//                    manualPane.add(label_X);
//                    textField_X = new JTextField(common_x.toString(),10);
//                    panel_X.add(label_X);
//                    panel_X.add(textField_X);
//                    JLabel label_Y = new JLabel("Y");
//                    panel_X.add(label_Y);

//                    items_Y = new Vector<>();
                        items_walk = new Vector<>();
                        int walk_base = config.walk_base;
                        items_work = new Vector<>();
                        int work_base = config.work_base;
                        for (int i = 0; i < 10; i++) {
                                items_walk.add(new ComboLabel<>(StrUtil.format("第{}排", i + 1), walk_base + i * config.walk_step, i + 1, T_WALK));
                                items_work.add(new ComboLabel<>(StrUtil.format("打工第{}排", i + 1), work_base + i * config.work_step, i + 1, T_WORK));
                        }
//                    items_work.add(new ComboLabel<>("溢出",10000,-1,OVERFLOW));
//                    items_Y.addAll(items_walk);
//                    items_Y.addAll(items_work);

//                    comboBox_Y = new JComboBox<>(items_Y);
//                    comboBox_Y.setEditable(true);
//                    comboBox_Y.setMaximumRowCount(items_Y.size());
//                    panel_X.add(comboBox_Y);
//                    manualPane.add(panel_X);

                        JLabel label_device = new JLabel("设备");
                        manualPane.add(label_device);
                        if (!first && !config.devices.get(0).equals(dev))
                                config.devices.add(0, dev);

                        JComboBox<String> comboBox_device = new JComboBox<>(config.devices);
                        comboBox_device.setEditable(true);
                        manualPane.add(comboBox_device);
                        comboBox_device.addActionListener(e -> {
                                device = ((JComboBox<String>) e.getSource()).getSelectedItem().toString();
                                initDevice();
                        });


                        JButton button_clear = new JButton("清除");
                        manualPane.add(button_clear);

                        button_save = new JButton("保存(Ctrl+S)");
                        //回写
                        ctrl_s_action = new AbstractAction() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                        JTextArea source = null;
                                        if (e.getSource() instanceof JTextArea)
                                                source = (JTextArea) e.getSource();
                                        else if (e.getSource() instanceof JButton) {
                                                source = (JTextArea) referencees.get(e.getSource());
                                        }
                                        String text = source.getText();
                                        List<String> textList = Arrays.stream(text.split(LF)).filter(StrUtil::isNotBlank).collect(Collectors.toList());
                                        List<String> values = new ArrayList<>(config.quicks.values());
                                        ArrayList<String> collect = new ArrayList<>();
                                        boolean replace_flag = false;
                                        for (int i = 0; i < values.size(); i++) {
                                                String s = values.get(i);
                                                if (s.startsWith(textList.get(0))) {
                                                        collect.add(text);
                                                        replace_flag = true;
                                                } else {
                                                        collect.add(s);
                                                }
                                        }
                                        if (!replace_flag) collect.add(text);
                                        Collections.sort(collect);
                                        FileWriter.create(new File(config.quicks_path), StandardCharsets.UTF_8).writeLines(collect, LineSeparator.LINUX, false);
                                        config.version += 1;
                                        FileUtil.writeUtf8String(config.toString(), config.config_path);
//                                    FileUtil.writeUtf8Lines(collect, config.quicks_path);
                                        writeStatus("save quicks done");
                                        MouseEvent mouseEvent = new MouseEvent(source, 0, 0, 0, 0, 0, 0, 0, 0, false, 0);
                                        mouseAdapter.mouseClicked(mouseEvent);

                                }
                        };
                        button_save.addActionListener(ctrl_s_action);

                        button_stop = new JButton("结束线程(F2)");
                        button_stop.addActionListener(e -> {
                                if (thread != null) {
                                        thread_suspend = false;
                                        onClose(false);
                                        writeStatus("threads stopped");
                                        activityNames = null;
                                }

                        });

                        button_F3 = new JButton("注释(F3)");
                        button_F3.addActionListener(F3_ACTION);
                        ta_plan = new JTextArea(15, 55);
                        referencees.put(button_save, ta_plan);
                        referencees.put(button_F3, ta_plan);
                        referencees.put(iFrame, ta_plan);
                        ta_plan.setLineWrap(true);
                        ta_plan.setText(lastConfig);
                        Font status_font = new Font("宋体", Font.TRUETYPE_FONT, 15);
                        status_area = new JTextArea(5, 50);
                        JScrollPane status_pane = new JScrollPane(status_area);
                        status_area.setBorder(BorderFactory.createTitledBorder("status"));
                        status_area.setLineWrap(true);
                        status_area.setFont(status_font);
                        status_bar = new JTextArea();
                        status_bar.setFont(status_font);
                        JScrollPane bar_pane = new JScrollPane(status_bar);
                        status_bar.setEditable(false);
                        status_bar.setRows(6);
                        status_bar.setLineWrap(true);

                        Font font = new Font("宋体", Font.TRUETYPE_FONT, 18);
                        UndoManager undoManager = new UndoManager();
                        ta_plan.getDocument().addUndoableEditListener(undoManager);
                        ta_plan.setFont(font);
                        //undo
                        ta_plan.getKeymap().addActionForKeyStroke(CTRL_Z, CTRL_Z_ACTION);
                        //redo
                        ta_plan.getKeymap().addActionForKeyStroke(CTRL_SHIFT_Z, CTRL_SHIFT_Z_ACTION);
                        // comment/uncomment line
                        ta_plan.getKeymap().addActionForKeyStroke(F3, F3_ACTION);

                        //duplicate current line
                        ta_plan.getKeymap().addActionForKeyStroke(CTRL_D, CTRL_D_ACTION);

                        //remove current line
                        ta_plan.getKeymap().addActionForKeyStroke(CTRL_Y, CTRL_Y_ACTION);

                        ta_plan.getKeymap().addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK), ctrl_s_action);
                        //更新
                        ta_plan.getKeymap().addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_MASK), new AbstractAction() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                        JTextArea source = (JTextArea) e.getSource();
                                        String origin = FileUtil.readUtf8String(config.quicks_path);
                                        origin = origin.replace(current_quick_item, source.getText());
                                        FileUtil.writeUtf8String(origin, config.quicks_path);
                                        config.version += 1;
                                        FileUtil.writeUtf8String(config.toString(), config.config_path);
                                        writeStatus("update quicks done");
                                        MouseEvent mouseEvent = new MouseEvent(source, 0, 0, 0, 0, 0, 0, 0, 0, false, 0);
                                        mouseAdapter.mouseClicked(mouseEvent);
                                }
                        });
                        //shift up
                        ta_plan.getKeymap().addActionForKeyStroke(CTRL_SHIT_UP, CTRL_SHIFT_UP_ACTION);
                        //shift down
                        ta_plan.getKeymap().addActionForKeyStroke(CTRL_SHIT_DOWN, CTRL_SHIFT_DOWN_ACTION);

                        Set<Map.Entry<String, String>> quick_entries = config.quicks.entrySet();
                        for (Map.Entry<String, String> entry : quick_entries) {
                                JMenuItem item = new JMenuItem(entry.getKey());
                                item.addActionListener(e -> {
                                        if (e.getModifiers() == KeyEvent.BUTTON3_MASK) {
                                                quick_menu.remove(item);
                                                config.quicks.remove(item.getActionCommand());
                                                FileWriter.create(new File(config.quicks_path), StandardCharsets.UTF_8).writeLines(config.quicks.values(), LineSeparator.LINUX, false);
                                                writeStatus("##item removed:" + e.getActionCommand());
                                        } else {
                                                ta_plan.setText(entry.getValue());
                                                current_quick_item = entry.getValue();
                                                activityNames = null;
                                                offset_arow = 0;
                                        }

                                });
                                quick_menu.add(item);
                        }
                        button_clear.addActionListener(e -> {
                                ta_plan.setText(null);
                        });

                        button_batch = new JButton("执行计划(F1)");
                        button_batch.addActionListener(e -> {
                                String[] lines = ta_plan.getText().trim().split(LF);
                                SlotGroup group = new SlotGroup();
                                ArrayList<String> collect = new ArrayList<>();
                                boolean ref_flag = false;
                                for (String line : lines) {
                                        if (line.startsWith(IDT_ACTIVITY)) {
                                                String[] split = line.split(";");
                                                group.activityNames = split[1].split(",");//[act];name1,name2
                                                activityNames = group.activityNames;
                                                if (split.length == 3 && split[2].equals(C_SLEEP))
                                                        group.c_sleep = true;
                                                collect.add(line);
                                        } else if (!line.startsWith(IDT_REF))
                                                collect.add(line);
                                        else if (line.startsWith(IDT_REF)) {
                                                ref_flag = true;
                                                String[] subs = line.split(";");
                                                String v = config.quicks.get(subs[1]);
                                                collect.add(CMT + line);
                                                String[] split = v.split(LF);
                                                String[] range = Arrays.copyOfRange(split, 1, split.length);
                                                collect.addAll(Arrays.asList(range));
                                        }
                                }
                                if (ref_flag) {
                                        ta_plan.setText(null);
                                        StringBuilder sb = new StringBuilder();
                                        for (String s : collect) {
                                                sb.append(s);
                                                sb.append(LF);
                                        }
                                        ta_plan.append(sb.toString());
                                        lines = collect.toArray(new String[collect.size()]);
                                }

                                lines = Arrays.stream(lines).filter(e1 -> !e1.contains(CMT) && !e1.contains(BSY) && !e1.contains(IDT_ACTIVITY) && StrUtil.isNotBlank(e1)).toArray(String[]::new);
                                ArrayList<Slot> slots = new ArrayList<>(lines.length);
                                group.slots = slots;
                                int j = 0;
                                for (int i = 0; i < lines.length; i++) {
                                        String line = lines[i];
                                        String[] params = line.split(";", -1);
                                        Slot slot = new Slot();
                                        slot.index = i;
                                        slot.params = params;
                                        if (params[0].startsWith(IDT_CMD)) {
                                                slot.type = T_CMD;
                                                slot.次数 = Integer.parseInt(params[1]);
                                                slot.cmds = line.substring(params[0].length() + params[1].length() + 2);
                                                if (slot.cmds.contains(INS_JJ)) {
                                                        String group0 = ReUtil.getGroup0(PATTERN_JJ, slot.ex_cmds);
                                                        j = Integer.parseInt(group0.substring(INS_JJ.length()));
                                                }
                                        } else {
                                                slot.次数 = Integer.parseInt(params[0]);
                                                slot.间隔_广告时长 = parseSecond(params[1]);
                                                slot.间隔_滑动 = Integer.parseInt(params[2]);
                                                group.totalTime += slot.间隔_滑动;
                                                String idt = params[3];
                                                ComboLabel<Integer> item = null;
                                                String[] sp = idt.split(" ");
                                                if (sp.length == 1) {//relative location
                                                        if (slot.ex_cmds != null && slot.ex_cmds.contains(INS_JJ)){
                                                                String group0 = ReUtil.getGroup0(PATTERN_JJ, slot.ex_cmds);
                                                                j = Integer.parseInt(group0.substring(INS_JJ.length()));
                                                        }
                                                        int idx = Integer.parseInt(idt.substring(1)) + offset_arow;
                                                        slot.origin_arow = idx;
                                                        slot.arow = idx - j;
                                                        if (idt.startsWith("#")) {//T_WALK
                                                                item = items_walk.get(idx - 1 - j);
                                                                slot.type = T_WALK;
                                                        } else if (idt.startsWith("$")) {
                                                                item = items_work.get(idx - 1 - j);
                                                                slot.type = T_WORK;
                                                        }
                                                        slot.X = common_x;
                                                        slot.Y = item.getValue();
                                                } else {//point
                                                        slot.X = Integer.valueOf(sp[0]);
                                                        slot.Y = Integer.valueOf(sp[1]);
                                                        slot.type = T_OTHER;
                                                }
                                                slot.ex_cmds = line.substring(params[0].length() + params[1].length() + params[2].length() + params[3].length() + 4);
                                                if (slot.type.equals(T_WALK) || slot.type.equals(T_WORK)) {
                                                        if (j > 0) {
                                                                int k = i - 1;
                                                                Slot pre = slots.get(k);
                                                                while (pre.origin_arow == null)
                                                                        pre = slots.get(--k);//略过非#
                                                                if (!pre.origin_arow.equals(slot.origin_arow)) {
                                                                        j++;
                                                                } else {//拷贝上一份 j不回退
                                                                        slot.arow = pre.arow;
                                                                        slot.Y = pre.Y;
                                                                }
                                                        } else j++;
                                                }
                                        }
                                        slots.add(slot);
                                }

                                onClose(false);
                                thread = new Thread(() -> {
                                        System.gc();
                                        manualPane.setBackground(Color.GREEN);
                                        beginBatch(group);
                                        manualPane.setBackground(Color.CYAN);
                                        StaticLog.info("## set cyan");
                                }, "thread");
                                thread.start();
                                final Pattern current_pattern = Pattern.compile("[\\w\\.]*/[\\w\\.]*");
                                monitorThread = new Thread(() -> {
                                        while (true) {
                                                String pre = RuntimeUtil.execForStr(cmd_current);
                                                String current = ReUtil.getGroup0(current_pattern, pre);
                                                if (current == null) current = pre;
                                                writeBar("{}\nthread:{}\nsuspend:{}\ncurrent:{}\nactNames:{}", LocalTime.now().format(TIME_FORMATTER), thread.getState(), thread_suspend, current, activityNames);
                                                if (activityNames != null) {
//                                                    StaticLog.info("##current {} ", current);
                                                        boolean contain = false;
                                                        for (String activityName : activityNames) {
//                                                            StaticLog.info("##activityName {} ", activityName);
                                                                if (current.contains(activityName)) {
                                                                        contain = true;
                                                                        break;
                                                                } else {
//                                                                 StaticLog.info("##activityName {} ,contain {}", activityName,contain);
                                                                }
                                                        }
                                                        if (!contain) {
//                                                            StaticLog.info("##activityNames {} ", activityNames);
//                                                            StaticLog.info("##contain {} ", contain);
                                                                if (!thread_suspend) {
                                                                        if (!group.c_sleep && thread.getState().equals(Thread.State.TIMED_WAITING)) {
                                                                                writeStatus("active thread interrupt");
                                                                                thread.interrupt();
                                                                        } else {
                                                                                writeStatus("active thread suspend");
                                                                                thread.suspend();
                                                                        }
                                                                        thread_suspend = true;
                                                                        writeStatus(StrUtil.format("current:{},require:{}", current, activityNames));
                                                                }
                                                        } else {
                                                                if (thread_suspend) {
                                                                        writeStatus("active thread resume");
                                                                        thread_suspend = false;
                                                                        thread.resume();
                                                                }
                                                        }
                                                        sleep(2);
                                                }
                                        }
                                }, "monitorThread");

                                monitorThread.start();

                        });
                        JButton button_down = new JButton("下移(ctrl+shift+↓)");
//                        referencees.put(button_down, ta_plan);
                        button_down.addActionListener(CTRL_SHIFT_DOWN_ACTION);
                        JButton button_up = new JButton("上移(ctrl+shift+↑)");
//                        referencees.put(button_up, ta_plan);
                        button_up.addActionListener(CTRL_SHIFT_UP_ACTION);
                        JPanel panel_plan = new JPanel();
                        panel_plan.add(button_F3);
                        panel_plan.add(button_batch);
                        JPanel panel_shift = new JPanel();
                        panel_shift.add(button_down);
                        panel_shift.add(button_up);
                        JPanel panel_ta = new JPanel();
                        JScrollPane ta_sc_pane = new JScrollPane(ta_plan, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                        panel_ta.setLayout(new BoxLayout(panel_ta, BoxLayout.Y_AXIS));
                        panel_ta.add(ta_sc_pane);
                        panel_ta.add(status_pane);
                        panel_ta.add(bar_pane);
                        JPanel panel_stop = new JPanel();
                        panel_stop.add(button_save);
                        panel_stop.add(button_stop);

                        manualPane.add(panel_stop);
                        manualPane.add(panel_plan);
                        manualPane.add(panel_shift);
                        manualPane.add(panel_ta);
                        if (first) {
                                frame.pack();
                                frame.setSize(600, 800);
                                frame.setLocation(600, 100);
                                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                                frame.setVisible(true);
                        }

                });

        }

        private void onClose(boolean closed) {
                if (thread != null) {
                        thread.stop();
                        manualPane.setBackground(Color.CYAN);
                }
                if (monitorThread != null) {
                        monitorThread.stop();
                }
                if (closed) {
                        referencees.remove(button_save);
                        referencees.remove(button_F3);
                        referencees.remove(iFrame);
                }
        }

        @SneakyThrows
        private int getLinePos(int lineunmber) {
                int j = 0;
//            while (!ta_plan.getText(j++, 1).equals(LF));//no need to pass first line
                int last_length = 0;
                while (lineunmber-- >= 0) {
                        int k = ta_plan.getLineOfOffset(j);
                        int start = ta_plan.getLineStartOffset(k);
                        int end = ta_plan.getLineEndOffset(k);
                        last_length = end - start;
                        String line = ta_plan.getText(start, last_length);
                        if (line.startsWith(CMT) || line.startsWith(BSY) || line.startsWith(IDT_ACTIVITY))
                                lineunmber++;
                        j = end;
                }
                return j - last_length;
        }

        private void writeBar(String format, Object... arguments) {
                SwingUtilities.invokeLater(() -> {
                        status_bar.setText(StrUtil.format(format, arguments));
                });
        }

        private void writeStatus(String text, Object... arguments) {
                writeStatus(StrUtil.format(text, arguments), true);
        }

        private void writeStatus(String text, boolean writeConsole) {
                SwingUtilities.invokeLater(() -> {
                        if (writeConsole)
                                System.out.println(text);
                        status_area.append(LocalTime.now().format(TIME_FORMATTER) + ":" + text + LF);
                });

        }

        @SneakyThrows
        public void beginBatch(SlotGroup group) {

                List<Slot> slots = group.slots;
                for (Slot slot : slots)
                        begin(slot, false, group);
                if (slots.size() > 1 || group.totalTime > 100) {
                        try {
                                MailUtil.send(account, "1281886693@qq.com", config.config_name + ":" + current_quick_item + ":任务完成:", JSON.toJSONString(slots), true);
                        } catch (RuntimeException e) {
                                e.printStackTrace();
                        }
                }
                Process process = RuntimeUtil.exec(StrUtil.format("mshta vbscript:msgbox(\"{}任务完成\",64,\"消息\")(window.close)", config.config_name));


        }

        @SneakyThrows
        private void initConfig(String confName) {
                this.config = JSONObject.parseObject(FileUtil.getInputStream(System.getProperty("user.dir") + "/exclude/config-" + confName + ".json"), Config.class, Feature.OrderedField);
                String quicks_path = System.getProperty("user.dir") + "/exclude/quicks-" + confName + ".txt";
                String config_path = System.getProperty("user.dir") + "/exclude/config-" + confName + ".json";
                config.version_path = System.getProperty("user.dir") + "/exclude/versions/";
                config.quicks_path = quicks_path;
                config.config_path = config_path;
                List<String> list = FileUtil.readUtf8Lines(quicks_path).stream().filter(StrUtil::isNotBlank).collect(Collectors.toList());
                TreeMap<String, String> map = new TreeMap<>();
                for (int i = 0; i < list.size(); i++) {
                        String line = list.get(i);
                        if (line.startsWith(BSY)) {
                                StringBuilder sb = new StringBuilder(line);
                                sb.append(LF);
                                int j;
                                for (j = i + 1; j < list.size(); j++) {
                                        if (list.get(j).startsWith(BSY))
                                                break;
                                        sb.append(list.get(j)).append(LF);
                                }
                                map.put(line.substring(BSY.length()), sb.toString());
                                i = j - 1;
                        }
                }
                config.quicks = map;
                config.config_name = confName;
                miniCap = new MiniCap(config);
        }

        @SneakyThrows
        public void begin(Slot slot, boolean notify, SlotGroup group) {
                initSlot(slot);
                types = new Types[]{Types.视频, Types.孵化};
                Types type = types[0];
                String name = type.name();
                switch (type) {
                        case 视频:
                                writeStatus("c = " + slot.次数);

                                String point = slot.X + " " + slot.Y;
//                                writeStatus("point = " + point);
//                                writeStatus("arow = " + slot.arow);
                                String[] cmds = null;
                                if (slot.cmds != null)
                                        cmds = slot.cmds.split(";");
                                while (slot.次数-- > 0) {
                                        int pos = getLinePos(slot.index);
                                        boolean nom = false;
                                        if (slot.type.equals(T_CMD)) {
                                                nom = slot.cmds.contains(INS_NO_MODIFY);
                                                execute_cmd(cmds, group);
                                                pos += slot.params[0].length() + 1;

                                        } else {
                                                nom = slot.ex_cmds.contains(INS_NO_MODIFY);
                                                tap(point);
                                                watchAdv(slot);
                                        }
                                        if (!nom) {
                                                int finalPos = pos;
                                                SwingUtilities.invokeAndWait(() -> {
                                                        ta_plan.replaceRange(slot.次数 + "", finalPos, finalPos + String.valueOf(slot.次数 + 1).length());
                                                });
                                        }
                                        writeStatus(StrUtil.format("{} c={}", name, slot.次数));
                                }
                                writeStatus(StrUtil.format("{}任务完成", name));
                                if (notify) {
                                        if (slot.次数 > 1) {
                                                try {
                                                        MailUtil.send(account, "1281886693@qq.com", config.config_name + "任务完成:", JSON.toJSONString(slot), true);

                                                } catch (RuntimeException e) {
                                                        e.printStackTrace();
                                                }

                                        }
                                        Process process = RuntimeUtil.exec("mshta vbscript:msgbox(\"视频任务完成\",64,\"消息\")(window.close)");
                                }
                                break;
                }
        }

        @SneakyThrows
        private void execute_cmd(String cmds[], SlotGroup group) {
                for (String cmd : cmds) {
                        boolean sleeped = false;
                        if (cmd.startsWith("sw")) {
                                if (cmd.equals(INS_SW_FROM)) {
                                        swipe(config.from);
                                        continue;
                                }//swf
                                if (cmd.equals(INS_SW_REVERSE)) {
                                        writeStatus("single swr not supported");
                                }//swr
                                if (cmd.equals(INS_SW_TO)) {
                                        swipe(config.to);
                                        continue;
                                }//swt
                                final Pattern pt = Pattern.compile("^[a-z]+");
                                String prefix = ReUtil.getGroup0(pt, cmd);
                                String[] subs = cmd.substring(prefix.length()).split("\\s");
                                String p = null;
                                boolean reverse_flag = false;
                                if (cmd.startsWith(INS_SW_FROM))
                                        p = config.from;
                                else if (cmd.startsWith(INS_SW_TO))
                                        p = config.to;
                                else if (cmd.startsWith(INS_SW_REVERSE))
                                        reverse_flag = true;
                                else {
                                        writeStatus("combine sw not supported");
                                        return;
                                }

                                if (subs.length == 2) {//sw65 7
                                        int total = Integer.parseInt(subs[0]);
                                        int t = Integer.parseInt(subs[1]);
                                        for (int i = 0; i < total / t + 1; i++) {
                                                if (reverse_flag) {
                                                        if (i % 2 == 0)
                                                                p = config.from;
                                                        else p = config.to;
                                                }
                                                sleep(t);
                                                swipe(p);
                                        }
                                } else if (subs.length == 4) {
                                        p = StrUtil.format("{} {} {} {}", subs[0], subs[1], subs[2], subs[3]);
                                        swipe(p);
                                        continue;
                                } else if (subs.length == 6) {
                                        int total = Integer.parseInt(subs[4]);
                                        int t = Integer.parseInt(subs[5]);
                                        for (int i = 0; i < total / t + 1; i++) {
                                                if (reverse_flag) {
                                                        if (i % 2 == 0)
                                                                p = StrUtil.format("{} {} {} {}", subs[0], subs[1], subs[2], subs[3]);
                                                        else
                                                                p = StrUtil.format("{} {} {} {}", subs[2], subs[3], subs[0], subs[1]);
                                                } else
                                                        p = StrUtil.format("{} {} {} {}", subs[0], subs[1], subs[2], subs[3]);
                                                sleep(t);
                                                swipe(p);

                                        }
                                }
                        } else if (cmd.startsWith("s")) {
                                int anInt = parseSecond(cmd.substring(1));
                                if (group != null)
                                        group.totalTime += anInt;
                                sleep(anInt);
                                sleeped = true;
                        } else if (cmd.startsWith("t")) {
                                tap(cmd.substring(1));
                        } else if (cmd.startsWith("b")) {
                                back();
                        } else if (cmd.startsWith("h")) {
                                home();
                        } else if (cmd.startsWith("if")) {//if0 0 100 200 看直播领钻石
                                int n = 10;
                                String[] subs = cmd.substring(2).split("\\s");
                                int x = Integer.parseInt(subs[0]);
                                int y = Integer.parseInt(subs[1]);
                                int width = Integer.parseInt(subs[2]);
                                int height = Integer.parseInt(subs[3]);
                                HttpRequest request = HttpUtil.createPost("http://snap.ie.sogou.com/recognition")
                                        .contentType(ContentType.MULTIPART.getValue())
                                        .header("Origin", "se-extension://ext-1055834318")
                                        .header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.81 Safari/537.36 SE 2.X MetaSr 1.0");
                                while (n-- > 0) {
                                        tap(subs[7] + " " + subs[8]);
                                        sleep(1);
                                        ByteArrayInputStream inputStream = new ByteArrayInputStream(miniCap.frame());
                                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                        ImgUtil.cut(inputStream, outputStream, new Rectangle(x, y, width, height));
                                        request.form("file", outputStream.toByteArray(), "file");
                                        String body;
                                        try {

                                                body = request.execute().body();
                                        } catch (IORuntimeException e) {
                                                writeStatus(e.getMessage());
                                                body = request.execute().body();
                                                e.printStackTrace();
                                        }
                                        StaticLog.info("## ocr body {} ", body);
                                        if (body.contains(subs[4])) {//包含
                                                tap(subs[5] + " " + subs[6]);
                                                inputStream.close();
                                                outputStream.close();
                                                break;
                                        }

                                }
                        } else if (cmd.startsWith("at")) {//at 26:00
                                String[] split = cmd.split("\\s");
                                String s0 = split[0];
                                String[] ss0 = split[1].split(":");
                                int t0h = Integer.parseInt(ss0[0]);
                                int t0m = Integer.parseInt(ss0[1]);
                                int t0 = t0h * 3600 + t0m * 60;
                                int now = LocalTime.now().toSecondOfDay();
                                if (now < t0) {
                                        int inter = t0 - now;
                                        writeStatus("qulify sleep {}", inter);
                                        sleep(inter);
                                } else {
                                        if (s0.equals("atc")) {
                                                writeStatus("now exceed {},continue cmd", split[1]);
                                                continue;
                                        } else if (s0.equals("atb")) {
                                                writeStatus("now exceed {},break cmd", split[1]);
                                                break;
                                        } else writeStatus("not support at,use atc or atb instead");
                                }
                        }
                        if (!sleeped) sleep(1);//操作间隔
                }
        }


        enum Types {
                孵化,
                视频;
        }

        private int parseSecond(String format) {//such as 1h2m3s
                if (format.charAt(format.length() - 1) <= '9') format += "s";//if end with number
                int anInt = 0;
                int level = 1;
                int len = format.length();
                for (int i = 0; i < len; i++) {
                        char v = format.charAt(len - 1 - i);
                        if (v <= '9') anInt += (v - '0') * Math.pow(10, i - 1) * level;
                        else if (v == 'm') {
                                level = 60;
                                len = len - i;
                                i = 0;
                        } else if (v == 'h') {
                                level = 3600;
                                len = len - i;
                                i = 0;
                        } else if (v == 's') {
                                level = 1;
                                len = len - i;
                                i = 0;
                        }
                }
                return anInt;
        }

        private void initSlot(Slot s) {
                s.关闭广告_竖屏 = Commons.关闭广告_竖屏;
                s.间隔_广告间隔 = Commons.间隔_广告间隔;
        }

        private void watchAdv(Slot s) {
                this.sleep(s.间隔_滑动);
                int direction = 0;
                if (s.ex_cmds != null) {
                        if (s.ex_cmds.contains(INS_SW_FROM)) {
                                s.ex_cmds = s.ex_cmds.replace(INS_SW_FROM, "");
                                direction = 1;
                        } else if (s.ex_cmds.contains(INS_SW_TO)) {
                                s.ex_cmds = s.ex_cmds.replace(INS_SW_TO, "");
                                direction = 2;
                        } else if (s.ex_cmds.contains(INS_SW_REVERSE)) {
                                s.ex_cmds = s.ex_cmds.replace(INS_SW_REVERSE, "");
                                direction = 3;
                        }
                }
                if (s.间隔_滑动 > 0) {
                        int times = s.间隔_广告时长 / s.间隔_滑动;
                        for (int i = 0; i < times + 1; i++) {
                                if (direction == 0) {
                                        if (cis_direction || i % 2 == 0)
                                                this.swipe(config.from);
                                        else
                                                this.swipe(config.to);
                                } else if (direction == 1)
                                        swipe(config.from);
                                else if (direction == 2)
                                        swipe(config.to);
                                else if (direction == 3)
                                        if (i % 2 == 0)
                                                this.swipe(config.from);
                                        else
                                                this.swipe(config.to);

                                this.sleep(s.间隔_滑动);

                        }
                } else
                        this.sleep(s.间隔_广告时长);
                this.back();
                if (StrUtil.isNotBlank(s.ex_cmds)) {
                        execute_cmd(s.ex_cmds.split(";"), null);
                }
                this.sleep(s.间隔_广告间隔);
        }

        class Commons {
                public static final String 关闭广告_竖屏 = "998 164";//981 176
                public static final int 间隔_广告间隔 = 5;

        }


        void tap(String p) {
                sleep(2);
                String cmd = cmd_tap + randomPoint(p);
//        StaticLog.info("##tap cmd {} ", cmd);
                String execForStr = RuntimeUtil.execForStr(cmd);
                if (StrUtil.isNotBlank(execForStr)) writeStatus(StrUtil.format("##execForStr tap:{} ", execForStr));
        }

        void swipe(String s) {
                String execForStr = RuntimeUtil.execForStr(cmd_swipe + randomPoint(s));
                if (StrUtil.isNotBlank(execForStr)) writeStatus(StrUtil.format("##execForStr swipe:{} ", execForStr));
        }

        void swipeNoRad(String s) {
                String execForStr = RuntimeUtil.execForStr(cmd_swipe + s);
                if (StrUtil.isNotBlank(execForStr)) StaticLog.info("##execForStr swipeNoRad:{} ", execForStr);
        }

        void back() {
                sleep(2);
                RuntimeUtil.execForStr(cmd_press + " KEYCODE_BACK");
        }

        void home() {
                sleep(2);
                RuntimeUtil.execForStr(cmd_press + " KEYCODE_HOME");
        }

        void sleep(int i) {
                long begin = System.currentTimeMillis();
                int millis = i * 1000;
                try {
                        Thread.sleep(millis);
                } catch (InterruptedException e) {
                        long cost = System.currentTimeMillis() - begin;
                        if (millis > cost) {
                                writeStatus("thread suspend");
                                thread_suspend = true;
                                thread.suspend();
                                writeStatus("thread resume,left:" + ((millis - cost) / 1000));
                                sleep((int) ((millis - cost) / 1000 + 1));
                        } else throw new IllegalStateException("millos<=cost:" + millis + ":" + cost);
                }
        }

        public boolean horizontal() {//0 2 false 竖屏;1 3 true 横屏
                String cmd = path + "adb shell dumpsys input | grep SurfaceOrientation";
                String s = RuntimeUtil.execForStr(cmd).trim().substring(20);
                int i = Integer.parseInt(s);
                if (i == 3 || i == 2) throw new IllegalStateException("illegal SurfaceOrientation:" + i);
                return i == 1 || i == 3;
        }

        private final HttpRequest ocr_request = HttpUtil.createPost("http://snap.ie.sogou.com/recognition");
        private String cmd_cap;

        @SneakyThrows
        public String ocr() {
                StaticLog.info(cmd_cap);
                int n = 100;
                while (n-- > 0) {
                        Process process = RuntimeUtil.exec(cmd_cap);
                        InputStream inputStream = process.getInputStream();
                        BufferedImage bufferedImage = ImageIO.read(inputStream);
                        System.out.println("input x,y,w,h,name:");
                        Scanner sc = new Scanner(System.in);
                        //                int x=20,y=900,w=1200,h=900;
                        int x = sc.nextInt(), y = sc.nextInt(), w = sc.nextInt(), h = sc.nextInt();
//                        System.out.printf("%d %d %d %d\n",x,y,w,h);
                        String path = System.getProperty("user.dir") + "/exclude/screen.png";
                        Image cut = ImgUtil.cut(bufferedImage, new Rectangle(x, y, w, h));
                        FileOutputStream out = new FileOutputStream(new File(path));
                        ImgUtil.write(cut, ImgUtil.IMAGE_TYPE_PNG, out);
                        out.close();

                }

                ocr_request.contentType(ContentType.MULTIPART.getValue())
//                        .form("file", ImgUtil.toBytes(cut,ImgUtil.IMAGE_TYPE_PNG), "file")
                        .header("Origin", "se-extension://ext-1055834318")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.81 Safari/537.36 SE 2.X MetaSr 1.0");
//                String resp=ocr_request.execute().body();
//                StaticLog.info("##ocr {} ", resp);
                return null;
        }

        public boolean tapText(String t) {
                JSONArray array = JSON.parseObject(ocr()).getJSONArray("result");
                JSONObject json = null;
                OcrFrame ocrFrame = null;
                for (Object o : array) {
                        JSONObject j = (JSONObject) o;
                        if (j.getString("content").contains(t)) {
                                json = j;
                                break;
                        }
                }
                if (json != null) {
                        ocrFrame = json.toJavaObject(OcrFrame.class);
                        JSONArray frame = json.getJSONArray("frame");
                        ArrayList<Pair<Integer, Integer>> pairs = new ArrayList<>(2);
                        String[] split0 = frame.getString(0).split(",");
                        Pair<Integer, Integer> p0 = new Pair<>(Integer.parseInt(split0[0]), Integer.parseInt(split0[1]));
                        String[] split2 = frame.getString(2).split(",");
                        Pair<Integer, Integer> p2 = new Pair<>(Integer.parseInt(split2[0]), Integer.parseInt(split2[1]));
                        Integer x = (p0.getKey() + p2.getKey()) / 2;
                        Integer y = (p0.getValue() + p2.getValue()) / 2;
                        String XY = x + " " + y;
                        StaticLog.info("XY={}", XY);
                        tap(XY);
                        return true;
                } else {
                        StaticLog.warn("no text find {},no tap", t);
                        return false;
                }
        }

        private String randomPoint(String p) {
                if (!config.random_point) return p;
                String[] split = p.split("\\s");
                int[] xys = new int[split.length];
                for (int i = 0; i < split.length; i++) {
                        int k = Integer.parseInt(split[i]);
                        k += RandomUtil.randomInt(-10, 11);
                        xys[i] = k;
                }
                return ArrayUtil.join(xys, " ");
        }

        /*********************action varibles**************/
        public static final KeyStroke CTRL_Z = KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK);
        public static final AbstractAction CTRL_Z_ACTION = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                        PlainDocument document = ((PlainDocument) ((JTextArea) e.getSource()).getDocument());
                        UndoManager manager = (UndoManager) document.getUndoableEditListeners()[0];
                        if (manager.canUndo()) {
                                manager.undo();
                        }
                }
        };

        public static final KeyStroke CTRL_SHIFT_Z = KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK);
        public static final AbstractAction CTRL_SHIFT_Z_ACTION = new AbstractAction() {
                @SneakyThrows
                @Override
                public void actionPerformed(ActionEvent e) {
                        PlainDocument document = ((PlainDocument) ((JTextArea) e.getSource()).getDocument());
                        UndoManager manager = (UndoManager) document.getUndoableEditListeners()[0];
                        if (manager.canRedo()) {
                                manager.redo();
                        }
                }
        };
        public static final KeyStroke F3 = KeyStroke.getKeyStroke("F3");
        public static final AbstractAction F3_ACTION = new AbstractAction() {
                @SneakyThrows
                @Override
                public void actionPerformed(ActionEvent e) {
                        JTextArea source = null;
                        if (e.getSource() instanceof JTextArea)
                                source = (JTextArea) e.getSource();
                        else if (e.getSource() instanceof JButton)
                                source = ((JTextArea) referencees.get(e.getSource()));

                        int linenumber = source.getLineOfOffset(source.getCaretPosition());
                        int start = source.getLineStartOffset(linenumber);
                        int end = source.getLineEndOffset(linenumber);
                        String line = source.getText(start, end - start);
                        if (!line.startsWith(BSY)) {
                                if (line.startsWith(CMT))
                                        source.replaceRange("", start, start + CMT.length());
                                else source.insert(CMT, start);
                        }
                        if (source.getCaretPosition() < source.getText().length())
                                source.setCaretPosition(end + CMT.length());
                }
        };
        public static final KeyStroke CTRL_D = KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK);
        public static final AbstractAction CTRL_D_ACTION = new AbstractAction() {
                @SneakyThrows
                @Override
                public void actionPerformed(ActionEvent e) {
                        JTextArea source = (JTextArea) e.getSource();
                        int linenumber = source.getLineOfOffset(source.getCaretPosition());
                        int start = source.getLineStartOffset(linenumber);
                        int end = source.getLineEndOffset(linenumber);
                        String line = source.getText(start, end - start);
                        if (StrUtil.isNotBlank(line)) {
                                if (linenumber == source.getLineCount() - 1)//the last line
                                        source.insert(LF + line, end);
                                else
                                        source.insert(line, end);
                        }
                }
        };

        public static final KeyStroke CTRL_Y = KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_MASK);
        public static final AbstractAction CTRL_Y_ACTION = new AbstractAction() {
                @SneakyThrows
                @Override
                public void actionPerformed(ActionEvent e) {
                        JTextArea source = (JTextArea) e.getSource();
                        int linenumber = source.getLineOfOffset(source.getCaretPosition());
                        int start = source.getLineStartOffset(linenumber);
                        int end = source.getLineEndOffset(linenumber);
//                                    String line = source.getText(start, end - start);
                        source.replaceRange("", start, end);
                }
        };

        public static final KeyStroke CTRL_SHIT_DOWN = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK);

        public static final AbstractAction CTRL_SHIFT_DOWN_ACTION = new AbstractAction() {
                @SneakyThrows
                @Override
                public void actionPerformed(ActionEvent e) {
                        JTextArea source = null;
                        if (e.getSource() instanceof JTextArea)
                                source = (JTextArea) e.getSource();
                        else if (e.getSource() instanceof JButton)
                                source = ((JTextArea) referencees.get(e.getSource()));

                        int n1 = source.getLineOfOffset(source.getCaretPosition());//当前行
                        if (n1 == source.getLineCount() - 1) return;
                        int n1_st = source.getLineStartOffset(n1);
                        int n1_end = source.getLineEndOffset(n1);
                        String line1 = source.getText(n1_st, n1_end - n1_st);//include \n
                        int n2_st = n1_end;
                        int n2_end = source.getLineEndOffset(n1 + 1);
                        String line2 = source.getText(n2_st, n2_end - n2_st);
                        if (n1 == source.getLineCount() - 2) {//倒数第二行
                                line2 += "\n";
                                line1 = line1.replace(LF, "");
                        }
                        source.replaceRange(line2 + line1, n1_st, n2_end);
                        source.setCaretPosition(n1_st + line2.length());
                        source.requestFocus();
                }
        };

        public static final KeyStroke CTRL_SHIT_UP = KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK);

        public static final AbstractAction CTRL_SHIFT_UP_ACTION=new AbstractAction() {
                @SneakyThrows
                @Override
                public void actionPerformed(ActionEvent e) {
                        JTextArea source = null;
                        if (e.getSource() instanceof JTextArea)
                                source = (JTextArea) e.getSource();
                        else if (e.getSource() instanceof JButton)
                                source = ((JTextArea) referencees.get(e.getSource()));

                        int n1 = source.getLineOfOffset(source.getCaretPosition());//当前行
                        if (n1 == 0) return;
                        int n1_st = source.getLineStartOffset(n1);
                        int n1_end = source.getLineEndOffset(n1);
                        String line1 = source.getText(n1_st, n1_end - n1_st);//include \n
                        int n2_end = n1_st;
                        int n2_st = source.getLineStartOffset(n1 - 1);
                        String line2 = source.getText(n2_st, n2_end - n2_st);
                        if (n1 == source.getLineCount() - 1) {//倒数第二行
                                line1 += "\n";
                                line2 = line2.replace(LF, "");
                        }
                        source.replaceRange(line1 + line2, n2_st, n1_end);
                        source.setCaretPosition(n2_end - 1);
                        source.requestFocus();
                }
        };
}
