package art.qiuye.diantao;

import art.qiuye.diantao.db.DianConfig;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.io.file.LineSeparator;
import cn.hutool.core.io.watch.SimpleWatcher;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.watchers.DelayWatcher;
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
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.setting.Setting;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.SneakyThrows;

import javax.imageio.ImageIO;
import javax.swing.Timer;
import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.text.DefaultCaret;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static art.qiuye.diantao.ActionKeys.*;
import static art.qiuye.diantao.Constants.*;
import static cn.hutool.core.util.StrUtil.isNotBlank;
import static cn.hutool.core.util.StrUtil.str;

public class DIANTAO_UI {

        private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

        private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-hh-mm-ss");

        static Log StaticLog = LogFactory.get();
        private static Config config;
        private String adb_path;
        private String path;
        private JCheckBox checkBox_cis;

        private String cmd_start;
        private String cmd_kill;
        String cmd_cap1 = path + "shell screencap  -p /sdcard/snapcap/cap.png";
        String cmd_cap2 = path + "pull /sdcard/snapcap/cap.png " + path + "cap.png";
        String cmd_tap;
        String cmd_keyevent;
        String cmd_swipe;

        String cmd_current;

        private Thread thread;
        private Thread monitorThread;
        private Vector<ComboLabel<Integer>> items_walk;
        private Vector<ComboLabel<Integer>> items_work;

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
        private JInternalFrame iFrame;
        private JPanel manualPane;


        public static final Db db = Db.use();

        public static Connection connection;

        private static final Map<Object, Component> referencees = new HashMap<>();

        private int compensate;
        private static final Map<String, String> security_swf = new HashMap<>(3);

        static {
                setting.set("host", "smtp.qq.com");
                setting.set("port", "465");
                setting.set("sslEnable", "true");
                setting.set("from", "1281886693@qq.com");
                setting.set("pass", "tqblyhamaeqxijaj");
                security_swf.put("11st","140 1314 950 1340");
                security_swf.put("oppo","110 1260 950 1280");
                security_swf.put("meizu","120 1380 930 1390");
        }


        private final static MailAccount account = new MailAccount(setting);
        private String current_quick_item;

        private int source=1;//1 quick; 2 common
        private AbstractAction ctrl_s_action;
        private JButton button_save;
        private JButton button_F3;
        private JButton button_back;
        private JButton button_forward;
        private static Thread.UncaughtExceptionHandler exceptionHandler = (t, e) -> {
                if(e instanceof ThreadDeath)return;
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, e, "ERROR-" + config.config_name, JOptionPane.ERROR_MESSAGE);
        };
        private JPopupMenu popupMenu;
        private AbstractAction ctrl_u_action;
        private Slot g_slot=new Slot();
        private JButton button_count_inc;
        private JButton button_count_dec;
        private JMenu quick_menu;
        private JMenu common_menu;

        private static List<DIANTAO_UI> intances = new ArrayList<>();

        public static void main(String[] args) {
                DIANTAO_UI ui = new DIANTAO_UI();
                SwingUtilities.invokeLater(() -> {

                        Thread.currentThread().setUncaughtExceptionHandler(exceptionHandler);
                        try {
                                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                        } catch (Exception e) {
                                throw new RuntimeException(e);
                        }
                        frame = new JFrame();
                        frame.setTitle(args[0]);
                        desktopPane = new JDesktopPane();

                });
                ui.main(args[0]);
        }

        private void initDevice() {
                path = adb_path + config.current_device;
                cmd_tap = path + "shell input tap ";
                cmd_keyevent = path + "shell input keyevent ";
                cmd_swipe = path + "shell input swipe ";
                cmd_cap = path + "exec-out screencap -p ";
                cmd_current = path + "shell dumpsys activity activities | grep mResumedActivity";
                cmd_start=path + "shell am start -n ";
                cmd_kill =path + "shell am force-stop ";
        }

        private void main(String confName) {
                initConfig(confName);
                main(config.devices.get(0), true);

                watchMonitor=WatchMonitor.create(Config.commons_path,WatchMonitor.ENTRY_MODIFY);
                SimpleWatcher watcher = new SimpleWatcher() {
                        @Override
                        public void onModify(WatchEvent<?> event, Path currentPath) {
                                for (DIANTAO_UI intance : intances) {
                                        intance.updateMenu(2);
                                }
                        }
                };
                DelayWatcher delayWatcher = new DelayWatcher(watcher, 1000);
                watchMonitor.setWatcher(delayWatcher);
                watchMonitor.start();
                writeStatus("watchMonitor started");
        }

        @SneakyThrows
        private void main(String dev, boolean first) {
                intances.add(this);
                initConfig(config.config_name);
                adb_path = config.adb_path;
                config.current_device = dev;
                initDevice();
                ThreadUtil.execute(() -> {
                        String c = RuntimeUtil.execForStr(StandardCharsets.UTF_8, adb_path + " connect " + config.current_device.trim().split("\\s+")[1]);
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
                        iFrame = new JInternalFrame(this.toString(), true, true, true);
                        iFrame.setSize(540, 750);
                        iFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                        iFrame.addInternalFrameListener(new InternalFrameAdapter() {
                                @Override
                                public void internalFrameClosed(InternalFrameEvent e) {
                                        onClose(true);
                                }
                        });
                        desktopPane.add(iFrame);
                        iFrame.add(manualPane);
                        iFrame.setVisible(true);
                        JMenuBar menuBar = new JMenuBar();
                        quick_menu = new JMenu("快捷操作");
                        JMenu offset_menu = new JMenu("偏移");
                        JMenu config_menun=new JMenu("配置");
                                JMenuItem reload_menu = new JMenuItem("重载配置");
                                JMenuItem upload_menu = new JMenuItem("上传配置");
                                JMenuItem down_menu = new JMenuItem("下载配置");
                                config_menun.add(reload_menu);
                                config_menun.add(upload_menu);
                                config_menun.add(down_menu);
                        common_menu = new JMenu("公共配置");
                        JMenu variable_menu = new JMenu("编辑变量");
                        JMenu source_menu = new JMenu("source");
                        JMenuBar frameBar = new JMenuBar();
                        JMenu new_win_menu = new JMenu("新窗口");
                        frameBar.add(new_win_menu);
                        iFrame.setJMenuBar(menuBar);
                        frame.setJMenuBar(frameBar);
                        MouseAdapter mouseAdapter = new MouseAdapter() {
                                @SneakyThrows
                                @Override
                                public void mousePressed(MouseEvent e) {
                                        if (e.getSource() instanceof JTextArea) {
                                                iFrame = (JInternalFrame) ((JTextArea) e.getSource()).getRootPane().getParent();
                                                ta_plan = (JTextArea) e.getSource();
                                        }
                                        onClose(true);
                                        lastConfig = ta_plan.getText();
                                        iFrame.setVisible(false);
                                        iFrame.setClosed(true);
                                        iFrame.dispose();
                                        main(config.current_device, false);
                                }
                        };
                        reload_menu.addMouseListener(mouseAdapter);
                        new_win_menu.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mousePressed(MouseEvent e) {
                                        new DIANTAO_UI().main(config.current_device ,true);
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

                        ButtonGroup souce_buttonGroup = new ButtonGroup();
                        ItemListener sourceItemlistener = e -> {
                                String text = ((JRadioButtonMenuItem) e.getItem()).getText();
                                this.source = Integer.parseInt(text);
                        };
                        for (int i = 1; i <= 2; i++) {
                                JRadioButtonMenuItem b = new JRadioButtonMenuItem(String.valueOf(i));
                                b.addItemListener(sourceItemlistener);
                                souce_buttonGroup.add(b);
                                source_menu.add(b);
                        }

                        menuBar.add(quick_menu);
                        menuBar.add(offset_menu);
//                        menuBar.add(reload_menu);
//                        menuBar.add(upload_menu);
//                        menuBar.add(down_menu);
                        menuBar.add(config_menun);
                        menuBar.add(common_menu);
                        menuBar.add(variable_menu);
                        menuBar.add(source_menu);


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

                        items_walk = new Vector<>();
                        int walk_base = config.walk_base;
                        items_work = new Vector<>();
                        int work_base = config.work_base;
                        for (int i = 0; i < 10; i++) {
                                items_walk.add(new ComboLabel<>(StrUtil.format("第{}排", i + 1), walk_base + i * config.walk_step, i + 1, T_WALK));
                                items_work.add(new ComboLabel<>(StrUtil.format("打工第{}排", i + 1), work_base + i * config.work_step, i + 1, T_WORK));
                        }

                        JLabel label_device = new JLabel("设备");
                        manualPane.add(label_device);
//                        if (!first && !config.devices.get(0).equals(dev))
//                                config.devices.add(0, dev);

                        JComboBox<String> comboBox_device = new JComboBox<>(config.devices);
                        if(!first){
                                comboBox_device.addItem(config.current_device);
                                comboBox_device.setSelectedIndex(comboBox_device.getItemCount()-1);
                        }

                        comboBox_device.setSelectedItem(config.current_device);
                        comboBox_device.setEditable(true);
                        manualPane.add(comboBox_device);
                        comboBox_device.addActionListener(e -> {
                                config.current_device = ((JComboBox<String>) e.getSource()).getSelectedItem().toString();
                                initDevice();
                        });


                        JButton button_clear = new JButton("清除");
                        manualPane.add(button_clear);

                        button_save = new JButton("保存(Ctrl+S)");
                        //回写
                        ctrl_s_action = new AbstractAction() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                        JTextArea source = getTextArea(e);
                                        String text = source.getText().trim();
                                        List<String> textList = Arrays.asList(text.split(LF));
                                        int s_source=DIANTAO_UI.this.source;
                                        Collection<String> qv=null;
                                        String path=null;
                                        if(s_source==1){
                                                qv=config.quicks.values();
                                                path=config.quicks_path;
                                        }
                                        else if(s_source==2) {
                                                qv = config.commons.values();
                                                path=config.commons_path;
                                        }
                                        List<String> values = new ArrayList<>(qv);
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
                                        FileWriter.create(new File(path), StandardCharsets.UTF_8).writeLines(collect, LineSeparator.LINUX, false);
                                        config.version += 1;
                                        String s = FileUtil.readUtf8String(config.config_path);
                                        s = s.replaceFirst(PATTERN_VERSION_S, String.valueOf(config.version));
                                        FileUtil.writeUtf8String(s ,config.config_path);
                                        if(s_source==1)updateMenu(s_source);
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
                        popupMenu = new JPopupMenu();
                        JMenuItem delete_one_line = new JMenuItem("删除当前行");
                        delete_one_line.addActionListener(CTRL_Y_ACTION);

                        JMenuItem delete_item = new JMenuItem("删除");
                        delete_item.addActionListener(DELETE_ITEM_ACTION);

                        JMenuItem decrease_item = new JMenuItem("count-1(Ctrl+↓)");
                        decrease_item.addActionListener(CTRL_DOWN_ACTION);

                        JMenuItem increase_item = new JMenuItem("count+1(Ctrl+↑)");
                        increase_item.addActionListener(CTRL_UP_ACTION);

                        JMenuItem update_item = new JMenuItem("更新(Ctrl+U)");

                        JMenuItem shift_up_item = new JMenuItem("上移(alt+shift+↑)");
                        shift_up_item.addActionListener(ALT_SHIFT_UP_ACTION);

                        JMenuItem shift_down_item = new JMenuItem("下移(alt+shift+↓)");
                        shift_down_item.addActionListener(ALT_SHIFT_DOWN_ACTION);

                        JMenuItem cut_item = new JMenuItem("剪切");
                        cut_item.addActionListener(CUT_ITEM_ACTION);

                        JMenuItem copy_item = new JMenuItem("复制");
                        copy_item.addActionListener(COPY_ITEM_ACTION);

                        JMenuItem paste_item = new JMenuItem("粘贴");
                        paste_item.addActionListener(PASTE_ITEM_ACTION );

                        JMenuItem header_item = new JMenuItem("header");
                        header_item.addActionListener(HEADER_ITEM_ACTION);

                        popupMenu.add(delete_one_line);
                        popupMenu.add(delete_item);
                        popupMenu.add(decrease_item);
                        popupMenu.add(increase_item);
                        popupMenu.add(header_item);
                        popupMenu.add(copy_item);
                        popupMenu.add(paste_item);
                        popupMenu.add(cut_item);
                        popupMenu.add(shift_up_item);
                        popupMenu.add(shift_down_item);
                        popupMenu.add(update_item);

                        ta_plan.setComponentPopupMenu(popupMenu);

                        referencees.put(ta_plan, ta_plan);
                        referencees.put(button_save, ta_plan);
                        referencees.put(button_F3, ta_plan);
                        referencees.put(iFrame, ta_plan);
                        referencees.put(popupMenu, ta_plan);
                        ta_plan.setLineWrap(true);
                        ta_plan.setText(lastConfig);
                        Font status_font = new Font("宋体", Font.TRUETYPE_FONT, 15);
                        status_area = new JTextArea(5, 50);
                        ((DefaultCaret) status_area.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
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
                        inputMap(ta_plan,CTRL_Z, CTRL_Z_ACTION);
                        //redo
                        inputMap(ta_plan,CTRL_SHIFT_Z, CTRL_SHIFT_Z_ACTION);
                        // comment/uncomment line
                        inputMap(ta_plan,F3, F3_ACTION);

                        //duplicate current line
                        inputMap(ta_plan,CTRL_D, CTRL_D_ACTION);

                        //remove current line
                        inputMap(ta_plan,CTRL_Y, CTRL_Y_ACTION);

                        inputMap(ta_plan,KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK), ctrl_s_action);
                        //更新
                        ctrl_u_action = new AbstractAction() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                        JTextArea source = getTextArea(e);
                                        int s_source=DIANTAO_UI.this.source;
                                        String path=null;
                                        switch (s_source){
                                                case 1:
                                                        path=config.quicks_path;
                                                        break;
                                                case 2:
                                                        path = Config.commons_path;
                                                        break;
                                        }
                                        String origin = FileUtil.readUtf8String(path);
                                        origin = origin.replace(current_quick_item, source.getText());
                                        FileUtil.writeUtf8String(origin, path);
                                        config.version += 1;
                                        String s = FileUtil.readUtf8String(config.config_path);
                                        s = s.replaceFirst(PATTERN_VERSION_S, String.valueOf(config.version));
                                        FileUtil.writeUtf8String(s, config.config_path);
                                        if(s_source==1)updateMenu(s_source);
                                }
                        };
                        update_item.addActionListener(ctrl_u_action);

                        inputMap(ta_plan,KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_MASK), ctrl_u_action);
                        //shift up
                        inputMap(ta_plan,ALT_SHIT_UP, ALT_SHIFT_UP_ACTION);

                        //shift down
                        inputMap(ta_plan,ALT_SHIT_DOWN,ALT_SHIFT_DOWN_ACTION);

                        inputMap(ta_plan,CTRL_UP, CTRL_UP_ACTION);
                        inputMap(ta_plan,CTRL_DOWN, CTRL_DOWN_ACTION);

                        updateMenu(1);
                        updateMenu(2);


                        variable_menu.addMouseListener(new MouseAdapter() {

                                @Override
                                public void mouseClicked(MouseEvent e) {
                                        JDialog dialog = new JDialog(frame);
                                        dialog.setLayout(new FlowLayout());
                                        JTextArea area = new JTextArea(JSON.toJSONString(config.variables, true));
                                        area.getDocument().addUndoableEditListener(new UndoManager());
                                        area.setColumns(37);
                                        area.setRows(24);

                                        inputMap(area,ALT_SHIT_UP, ALT_SHIFT_UP_ACTION);
                                        inputMap(area,ALT_SHIT_DOWN,ALT_SHIFT_DOWN_ACTION);
                                        inputMap(area,CTRL_Z, CTRL_Z_ACTION);
                                        inputMap(area,CTRL_SHIFT_Z, CTRL_SHIFT_Z_ACTION);
                                        inputMap(area,CTRL_D, CTRL_D_ACTION);
                                        inputMap(area,CTRL_Y, CTRL_Y_ACTION);
                                        JScrollPane scrollPane = new JScrollPane(area);
                                        dialog.add(scrollPane);
                                        dialog.setSize(350,550);
                                        dialog.setLocation(frame.getLocation());
                                        JButton save = new JButton("保存");
                                        save.addActionListener(ev->{
                                                String s = FileUtil.readUtf8String(config.config_path);
                                                TreeMap<String, String> variables = JSONUtil.toBean(area.getText(), TreeMap.class);
                                                String pretty = JSON.toJSONString(variables, SerializerFeature.PrettyFormat);
                                                s=ReUtil.replaceAll(s, PATTERN_VARIABLES_SAVE, pretty);

                                                FileUtil.writeUtf8String(s, config.config_path);
                                                config.variables = variables;
                                                writeStatus("variables saved");
                                        });
                                        save.setSize(10,10);
                                        dialog.add(save);
                                        dialog.setVisible(true);
                                }
                        });
                        button_clear.addActionListener(e -> {
                                ta_plan.setText(null);
                        });

                        button_batch = new JButton("执行计划(F1)");
                        button_batch.addActionListener(e -> {
                                String text = ta_plan.getText();
                                SlotGroup group = new SlotGroup();

                                String act = ReUtil.getGroup0(PATTENR_ACTIVITY_STR, text);
                                if(act!=null){
                                        String[] split = act.split(";");
                                        group.activityNames = split[0].split(",");//[act];name1,name2
                                        activityNames = group.activityNames;
                                        if (split.length == 2 && split[1].equals(C_SLEEP))
                                                group.c_sleep = true;
                                }

                                String[] lines = text.split(LF);
                                List<String> collect = new ArrayList<>();
                                StringBuilder sb = new StringBuilder();
                                for (String line : lines) {
                                        collect.add(line);
                                }
                                collect = nestRef(collect, 0);
                                for (String s : collect) {
                                        sb.append(s);
                                        sb.append(LF);
                                }
                                ta_plan.setText(sb.toString());
                                List<String> fluid=new ArrayList<>();
                                sb.setLength(0);
                                for (String s : collect) {
                                        s = nestVariable(s);
                                        fluid.add(s);
                                        sb.append(s);
                                        sb.append(LF);
                                }
                                text=sb.toString();
                                collect.clear();

                                //begin generate slot
                                collect = fluid.stream().filter(e1 -> !e1.contains(CMT) && !e1.contains(BSY) && !e1.contains(IDT_ACTIVITY) && StrUtil.isNotBlank(e1)).collect(Collectors.toList());
                                ArrayList<Slot> slots = new ArrayList<>(collect.size());
                                group.slots = slots;
                                int j = 0;
                                for (int i = 0; i < collect.size(); i++) {
                                        String line = collect.get(i);
                                        String[] params = line.split(";");
                                        Slot slot = new Slot();
                                        slot.index = i;
                                        slot.params = params;
                                        if (params[0].startsWith(IDT_CMD)) {
                                                slot.type = T_CMD;
                                                slot.count = Integer.parseInt(params[1]);
                                                slot.cmds = line.substring(params[0].length() + params[1].length() + 2);
                                                if (slot.cmds.contains(INS_JJ)) {
                                                        String group0 = ReUtil.getGroup0(PATTERN_JJ, slot.cmds);
                                                        j = Integer.parseInt(group0.substring(INS_JJ.length()));
                                                }
                                        } else {
                                                slot.count = Integer.parseInt(params[0]);
                                                slot.total_duration = parseSecond(params[1]);
                                                String[] d = params[2].split("\\s");
                                                if(d.length==1)
                                                        slot.interval = Integer.parseInt(params[2]);
                                                else {
                                                        slot.interval = Integer.parseInt(d[0]);
                                                        slot.duration += d[1];
                                                }

                                                group.totalTime += slot.interval;
                                                String idt = params[3];
                                                ComboLabel<Integer> item = null;
                                                String[] sp = idt.split(" ");
                                                if (sp.length == 1) {//relative location
                                                        if (slot.ex_cmds != null && slot.ex_cmds.contains(INS_JJ)){
                                                                String group0 = ReUtil.getGroup0(PATTERN_JJ, slot.ex_cmds);
                                                                j = Integer.parseInt(group0.substring(INS_JJ.length()));
                                                        }
                                                        String bg = ReUtil.getGroup0(PATTERN_AROW_BEGIN, idt);
                                                        slot.arow_down=bg.length()==1;
                                                        int idx = Integer.parseInt(idt.substring(bg.length())) + offset_arow;
                                                        slot.origin_arow = idx;
                                                        slot.arow = idx - j;
                                                        Vector<ComboLabel<Integer>> items;
                                                        if (idt.startsWith("#")) {//T_WALK
                                                               items=items_walk;
                                                                slot.type = T_WALK;
                                                        } else if (idt.startsWith("$")) {
                                                                items=items_work;
                                                                slot.type = T_WORK;
                                                        }
                                                        if(slot.arow_down)
                                                                item = items_work.get(idx - 1 - j);
                                                        else
                                                                item = items_work.get(idx - 1);//absolute
                                                        slot.X = common_x;
                                                        slot.Y = item.getValue();
                                                } else {//point
                                                        slot.X = Integer.valueOf(sp[0]);
                                                        slot.Y = Integer.valueOf(sp[1]);
                                                        slot.type = T_OTHER;
                                                }
                                                if(params.length>4)
                                                        slot.ex_cmds = line.substring(params[0].length() + params[1].length() + params[2].length() + params[3].length() + 4);
                                                else
                                                        slot.ex_cmds = "";
                                                if (slot.type.equals(T_WALK) || slot.type.equals(T_WORK)) {
                                                        if(slot.arow_down)//ignore
                                                         j++;
                                                }
                                        }
                                        slots.add(slot);
                                }

                                onClose(false);
                                thread = new Thread(() -> {
                                        manualPane.setBackground(Color.GREEN);
                                        beginBatch(group);
                                        monitorThread.stop();
                                        manualPane.setBackground(Color.CYAN);
                                }, "thread");
                                thread.setUncaughtExceptionHandler(exceptionHandler);
                                thread.start();
                                final Pattern current_pattern = Pattern.compile("[\\w\\.]*/[\\w\\.]*");
                                monitorThread = new Thread(() -> {
                                        int swipe_count=3;
                                        while (true) {
                                                String pre = RuntimeUtil.execForStr(cmd_current);
                                                String current = ReUtil.getGroup0(current_pattern, pre);
                                                if (current == null) current = pre;
                                                writeBar("{}\nthread:{}\nsuspend:{}\ncurrent:{}\nactNames:{}", LocalTime.now().format(TIME_FORMATTER), thread.getState(), thread_suspend, current, activityNames);
                                                //com.taobao.live/com.alibaba.wireless.security.open.middletier.fc.ui.ContainerActivity
                                                //com.taobao.live/mtopsdk.extra.antiattack.CheckCodeValidateActivity
                                                //fixme
                                                if(current.contains("com.alibaba.wireless.security") || current.contains("mtopsdk.extra.antiattack.CheckCodeValidateActivity")){
                                                        compensate=0;
                                                        String cod=security_swf.get(config.config_name);
                                                        if(cod!=null){
                                                                if(swipe_count<=0){
                                                                        swipe_count=3;
                                                                        back();
                                                                }else {
                                                                        swipe_count--;
                                                                        compensate+=2;
                                                                        sleep(2);
                                                                        swipe(cod);
                                                                }
                                                        }else back();
                                                                if(g_slot.interval ==0){
                                                                        compensate+=4;
                                                                }else compensate+=2;
                                                                thread.interrupt();

                                                }

                                                if (activityNames != null) {
                                                        boolean contain = false;
                                                        for (String activityName : activityNames) {
                                                                if (current.contains(activityName)) {
                                                                        contain = true;
                                                                        break;
                                                                }
                                                        }
                                                        if (!contain) {
                                                                if (!thread_suspend) {
                                                                        if (!group.c_sleep && thread.getState().equals(Thread.State.TIMED_WAITING)) {
                                                                                writeStatus("active thread interrupt");
                                                                                compensate=0;
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
                                monitorThread.setUncaughtExceptionHandler(exceptionHandler);
                                monitorThread.start();

                        });
                        button_count_inc = new JButton("count+");
                        referencees.put(button_count_inc, ta_plan);
                        button_count_inc.addMouseListener(new MouseAdapter() {
                                private Timer timer= new Timer(50, CTRL_UP_ACTION);
                                @Override
                                public void mousePressed(MouseEvent e) {
                                        referencees.put(timer, ta_plan);
                                        String command = String.valueOf(e.getButton());
                                        CTRL_UP_ACTION.actionPerformed(new ActionEvent(timer,0,command));
                                        timer.setInitialDelay(300);
                                        timer.setActionCommand(command);
                                        timer.start();
                                }

                                @Override
                                public void mouseReleased(MouseEvent e) {
                                        timer.stop();
                                        referencees.remove(timer);
                                }
                        });
                        button_count_dec = new JButton("count-");
                        button_count_dec.addMouseListener(new MouseAdapter() {
                                private Timer timer= new Timer(50, CTRL_DOWN_ACTION);
                                @Override
                                public void mousePressed(MouseEvent e) {
                                        referencees.put(timer, ta_plan);
                                        String command = String.valueOf(e.getButton());
                                        CTRL_DOWN_ACTION.actionPerformed(new ActionEvent(timer,0,command));
                                        timer.setInitialDelay(300);
                                        timer.setActionCommand(command);
                                        timer.start();
                                }

                                @Override
                                public void mouseReleased(MouseEvent e) {
                                        timer.stop();
                                        referencees.remove(timer);
                                }
                        });
                        button_back = new JButton("后退(Ctrl+z)");
                        referencees.put(button_back, ta_plan);
                        button_back.addActionListener(CTRL_Z_ACTION);
                        button_forward = new JButton("前进(Ctrl+shift+z)");
                        referencees.put(button_forward,ta_plan);
                        button_forward.addActionListener(CTRL_SHIFT_Z_ACTION);
                        JPanel panel_plan = new JPanel();
                        panel_plan.add(button_F3);
                        panel_plan.add(button_save);
                        JPanel panel_shift = new JPanel();
                        panel_shift.add(button_count_inc);
                        panel_shift.add(button_count_dec);
                        panel_shift.add(button_back);
                        panel_shift.add(button_forward);
                        JPanel panel_ta = new JPanel();
                        JScrollPane ta_sc_pane = new JScrollPane(ta_plan, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                        panel_ta.setLayout(new BoxLayout(panel_ta, BoxLayout.Y_AXIS));
                        panel_ta.add(ta_sc_pane);
                        panel_ta.add(status_pane);
                        panel_ta.add(bar_pane);
                        JPanel panel_stop = new JPanel();
                        panel_stop.add(button_batch);
                        panel_stop.add(button_stop);

                        manualPane.add(panel_stop);
                        manualPane.add(panel_plan);
                        manualPane.add(panel_shift);
                        manualPane.add(panel_ta);
                        if (first && !frame.isVisible()) {
                                frame.pack();
                                frame.setSize(600, 800);
                                frame.setLocation(600, 100);
                                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                                frame.setVisible(true);
                        }

                });

        }

        private static void inputMap(JComponent component,KeyStroke stroke,Action action){
                component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke,stroke);
                component.getActionMap().put(stroke,action);
        }

        private String msg = "";
        private String nestVariable(String s){
                List<String> groups = ReUtil.findAllGroup0(PATTERN_VARIABLES, s);
                int limit=5;
                while (!groups.isEmpty() && limit-->0){
                        for (String g : groups) {
                                int b_index=g.indexOf('(');
                                String value;
                                if(b_index>0){
                                        value=config.variables.get(g.substring(0,b_index));
                                    String[] bk_values = ReUtil.getGroup0(PATTERN_BRACKET, g).split(",");
                                        for (String bkValue : bk_values)
                                                value = value.replaceFirst("\\$\\$", bkValue);
                                        g=g.replaceFirst("\\(","\\\\(");
                                        g=g.replaceFirst("\\)","\\\\)");
                                }else value = config.variables.get(g);
                                if(value!=null){
                                        s = s.replaceAll(g, value);
                                }else {
                                        msg+=StrUtil.format("not found value for variable {}\n",g);
                                }
                        }
                        groups= ReUtil.findAllGroup0(PATTERN_VARIABLES, s);
                }
                if(isNotBlank(msg)){
                        JOptionPane.showMessageDialog(iFrame,msg);
                        msg = "";
                }
                return s;
        }
        private List<String> nestRef(List<String> collect,int level){
                List<String> temp = new ArrayList<>();
                boolean nest_flag=false;
                for (String line : collect) {
                        TreeMap<String,String> map=null;
                        if(line.startsWith(IDT_REF_COMMON))map=config.commons;
                        else if(line.startsWith(IDT_REF)) map = config.quicks;
                        if (map!=null) {
                                nest_flag = true;
                                String[] subs = line.split(";");
                                String v = map.get(subs[1]);
                                String[] split = v.split(LF);
                                for (int i = 1; i < split.length; i++)
                                        temp.add(split[i]);
                        }else if(!line.startsWith(IDT_ACTIVITY) || level==0)
                                temp.add(line);
                }
                if(collect instanceof ArrayList)collect.clear();
                if(nest_flag){
                       return nestRef(temp,level+1);
                }
                else return temp;
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
                        referencees.remove(ta_plan);
                        referencees.remove(iFrame);
                        referencees.remove(button_count_inc);
                        referencees.remove(button_count_dec);
                        referencees.remove(button_back);
                        referencees.remove(button_forward);
                        referencees.remove(popupMenu);
                        iFrame.removeAll();
                        intances.remove(this);
                }
        }

        @SneakyThrows
        private int getLinePos(int lineunmber) {
                int j = 0;
                int last_length = 0;
                while (lineunmber-- >= 0) {
                        String line=null;
                        while (StrUtil.isBlank(line)){
                                int k = ta_plan.getLineOfOffset(j);
                                int start = ta_plan.getLineStartOffset(k);
                                int end = ta_plan.getLineEndOffset(k);
                                last_length = end - start;
                                line = ta_plan.getText(start, last_length);
                                j = end;
                        }
                        if (line.startsWith(CMT) || line.startsWith(BSY) || line.startsWith(IDT_ACTIVITY))
                                lineunmber++;
                }
                return j - last_length;
        }

        private void writeBar(String format, Object... arguments) {
                SwingUtilities.invokeLater(() -> {
                        status_bar.setText(StrUtil.format(format, arguments));
                });
        }

        private void writeStatus(Object text, Object... arguments) {
                if(text==null) text = "null";
                writeStatus(StrUtil.format(text.toString(), arguments), true);
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
                for (Slot slot : slots) {
                        ta_plan.setCaretPosition(getLinePos(slot.index));
                        begin(slot, false, group);
                }
                if (slots.size() > 1 || group.totalTime > 100) {
                        try {
                                MailUtil.send(account, "1281886693@qq.com", config.config_name + ":" + current_quick_item + ":任务完成:", JSON.toJSONString(slots), true);
                        } catch (RuntimeException e) {
                                e.printStackTrace();
                        }
                }
                RuntimeUtil.exec(StrUtil.format("mshta vbscript:msgbox(\"{}任务完成\",64,\"{}\")(window.close)",config.config_name,LocalDateTime.now().format(TIME_FORMATTER)));

        }
        private static WatchMonitor watchMonitor;

        @SneakyThrows
        private void initConfig(String confName) {
                this.config = JSONObject.parseObject(FileUtil.getInputStream(System.getProperty("user.dir") + "/exclude/config-" + confName + ".json"), Config.class, Feature.OrderedField);
                String quicks_path = System.getProperty("user.dir") + "/exclude/quicks-" + confName + ".txt";
                String config_path = System.getProperty("user.dir") + "/exclude/config-" + confName + ".json";
                config.version_path = System.getProperty("user.dir") + "/exclude/versions/";
                config.quicks_path = quicks_path;
                config.config_path = config_path;
                config.quicks = readQuciks(quicks_path);
                config.commons = readQuciks(Config.commons_path);

                config.config_name = confName;
                miniCap = new MiniCap(config);
        }
        public void updateMenu(int type){////1 quicks menun;2 common_menun
                Map<String,String> map=null;
                JMenu menu=null;
                String path=null;
                switch (type){
                        case 1:
                                path=config.quicks_path;
                                config.quicks = readQuciks(path);
                                map=config.quicks;
                                menu=quick_menu;
                                break;
                        case 2:
                                path= Config.commons_path;
                                config.commons = readQuciks(path);
                                map=config.commons;
                                menu=common_menu;
                                break;
                }
                menu.removeAll();
                for (Map.Entry<String, String> entry : map.entrySet()) {
                        JMenuItem item = new JMenuItem(entry.getKey());
                        String finalPath = path;
                        JMenu finalMenu = menu;
                        Map<String, String> finalMap = map;
                        item.addActionListener(e -> {
                                if (e.getModifiers() == KeyEvent.BUTTON3_MASK) {
                                        int option = JOptionPane.showInternalConfirmDialog(iFrame, "移除选项?"+item.getText()+" source:"+source);
                                        if(option==JOptionPane.YES_OPTION){
                                                finalMenu.remove(item);
                                                finalMap.remove(item.getActionCommand());
                                                FileWriter.create(new File(finalPath), StandardCharsets.UTF_8).writeLines(finalMap.values(), LineSeparator.LINUX, false);
                                                writeStatus("##item removed:{},source:{}" ,e.getActionCommand(),source);
                                                return;
                                        }
                                }else if (e.getModifiers() == KeyEvent.BUTTON2_MASK) {
                                        String[] split = entry.getValue().split(LF);
                                        StringBuilder sb = new StringBuilder();
                                        for (int i = 0; i < split.length; i++) {
                                                String line = split[i];
                                                if(line.startsWith(BSY) || line.startsWith(IDT_ACTIVITY))continue;
                                                sb.append(line).append(LF);
                                        }
                                        ta_plan.insert(sb.toString(),ta_plan.getCaretPosition());

                                }
                                else {
                                        ta_plan.setText(entry.getValue());
                                }
                                current_quick_item=entry.getValue();
                                activityNames = null;
                                offset_arow = 0;
                                source=type;
                        });
                        menu.add(item);
                }
                writeStatus("menu updated,source {}",type);
        }
        private TreeMap<String, String> readQuciks(String path){
                List<String> list = FileUtil.readUtf8Lines(path);
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
                                map.put(line.substring(BSY.length()), sb.toString().trim());
                                i = j - 1;
                        }
                }
                list.clear();
                return map;
        }
        @SneakyThrows
        public void begin(Slot slot, boolean notify, SlotGroup group) {
                writeStatus("c = " + slot.count);
                String point = slot.X + " " + slot.Y;
                String[] cmds = null;
                if (slot.cmds != null)
                        cmds = slot.cmds.split(";");
                while (slot.count-- > 0) {
                        int pos = getLinePos(slot.index);
                        boolean nom;
                        if (slot.type.equals(T_CMD)) {
                                nom = slot.cmds.contains(INS_NO_MODIFY);
                                execute_cmd(cmds, group);
                                pos += slot.params[0].length() + 1;
                        } else {
                                nom = slot.ex_cmds.contains(INS_NO_MODIFY);
                                tap(point);
                                g_slot=slot;
                                watchAdv(g_slot);
                        }
                        if (!nom) {
                                int finalPos = pos;
                                SwingUtilities.invokeAndWait(() -> {
                                        ta_plan.replaceRange(slot.count + "", finalPos, finalPos + String.valueOf(slot.count + 1).length());
                                        ta_plan.setCaretPosition(finalPos);
                                });
                        }
                        writeStatus(StrUtil.format("c={}", slot.count));
                }
                writeStatus("任务完成");
                if (notify) {
                        if (slot.count > 1) {
                                try {
                                        MailUtil.send(account, "1281886693@qq.com", config.config_name + "任务完成:", JSON.toJSONString(slot), true);

                                } catch (RuntimeException e) {
                                        e.printStackTrace();
                                }

                        }
                        RuntimeUtil.exec(StrUtil.format("mshta vbscript:msgbox(\"{}任务完成\",64,\"{}\")(window.close)",config.config_name,LocalDateTime.now().format(TIME_FORMATTER)));
                }
        }

        @SneakyThrows
        private void execute_cmd(String cmds[], SlotGroup group) {
                for (String cmd : cmds) {
                        boolean sleeped = false;
                        String code=ReUtil.getGroup0(PATTERN_OP_CODE,cmd);
                        if(code==null)continue;
                        else if (code.startsWith("sw")) {
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
                                String[] subs = cmd.substring(code.length()).split("\\s");
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
                                if(subs.length==1){
                                     swipe(p+" "+subs[0]);
                                }
                                else if (subs.length == 2) {//sw65 7
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
                                        continue;
                                } else if (subs.length == 4) {
                                        p = StrUtil.format("{} {} {} {}", subs[0], subs[1], subs[2], subs[3]);
                                        swipe(p);
                                        continue;

                                } else if(subs.length==5){
                                        p = StrUtil.format("{} {} {} {} {}", subs[0], subs[1], subs[2], subs[3],subs[4]);
                                        swipe(p);
                                        continue;
                                }
                                else if (subs.length == 6) {
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
                        } else if (code.equals("s")) {
                                double d;
                                if(cmd.contains(".")) d = Double.parseDouble(cmd.substring(code.length()));
                                else
                                        d = parseSecond(cmd.substring(1));
                                if (group != null)
                                        group.totalTime += d;
                                sleep(d);
                                sleeped = true;
                        } else if (code.equals("t")) {
                                String sub = cmd.substring(1);
                                if(sub.startsWith("#")){
                                        int index = Integer.parseInt(sub.substring(1));
                                        tap(common_x+" "+items_walk.get(index - 1+offset_arow).getValue());
                                }else if(sub.startsWith("$")){
                                        int index = Integer.parseInt(sub.substring(1));
                                        tap(common_x+" "+items_work.get(index - 1+offset_arow).getValue());
                                }else
                                        tap(sub);
                        } else if (code.equals("b")) {
                                back();
                        } else if (code.equals("h")) {
                                keyevent("KEYCODE_HOME");
                        } else if (code.equals("recent")) {
                                keyevent("KEYCODE_APP_SWITCH");
                        } else if (code.equals("if")) {//if0 0 100 200 看直播领钻石
//                                ### adb -s 192.168.1.20 shell LD_LIBRARY_PATH=/data/local/tmp /data/local/tmp/minicap -P 1080x1920@1080x1920/0
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
                                        sleep(0.5);
                                        byte[] bytes = miniCap.frame();
                                        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
//                                        FileUtil.writeBytes(bytes, System.getProperty("user.dir") + "/ocr.png");
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
                                        writeStatus("## ocr body {} ", body);
                                        if (body.contains(subs[4])) {//包含
                                                tap(subs[5] + " " + subs[6]);
                                                break;
                                        }
                                        inputStream.close();outputStream.close();
                                }
                        } else if(code.equals("ocrt")){//ocrt 0 0 100 200 可跳槽,name
                                int n = 10;
                                String[] subs = cmd.substring("ocrt".length()).trim().split("\\s");
                                int x = Integer.parseInt(subs[0]);
                                int y = Integer.parseInt(subs[1]);
                                int width = Integer.parseInt(subs[2]);
                                int height = Integer.parseInt(subs[3]);
                                HttpRequest request = HttpUtil.createPost("http://snap.ie.sogou.com/recognition")
                                        .contentType(ContentType.MULTIPART.getValue())
                                        .header("Origin", "se-extension://ext-1055834318")
                                        .header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.81 Safari/537.36 SE 2.X MetaSr 1.0");
//                                ByteArrayInputStream inputStream = new ByteArrayInputStream(miniCap.frame());
                                FileInputStream inputStream = new FileInputStream(System.getProperty("user.dir") + "/ocr.png");
                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                BufferedImage bufferedImage = ImageIO.read(inputStream);
                                Image cutImage = ImgUtil.cut(bufferedImage, new Rectangle(x, y, width, height));
                                Graphics2D graphics2D = bufferedImage.createGraphics();
                                graphics2D.setColor(Color.WHITE);
                                graphics2D.fillRect(0,0,bufferedImage.getWidth(),bufferedImage.getHeight());
                                graphics2D.drawImage(cutImage, x, y, width, height, Color.WHITE, null);
                                ImageIO.write(bufferedImage, ImgUtil.IMAGE_TYPE_PNG,outputStream);
                                request.form("file", outputStream.toByteArray(), "file");
                                FileUtil.writeBytes(outputStream.toByteArray(), System.getProperty("user.dir") + "/out.png");
                                String body = request.execute().body();
                                StaticLog.info("## ocr body {} ", body);
                                inputStream.close();outputStream.close();
                        }else if(code.equals("screen")){
                                String screen_path = Config.screen_path + config.config_name+"-"  +LocalDateTime.now().format(DATE_TIME_FORMATTER)+ ".jpg";
                                writeStatus("screenshot:{}",screen_path);
                                StopWatch stopWatch = new StopWatch();
                                stopWatch.start();
                                FileUtil.writeBytes(miniCap.frame(),screen_path);
                                stopWatch.stop();
                                writeStatus("stopWatch:{}ms",stopWatch.getTotalTimeMillis());
                        }
                        else if (code.startsWith("at")) {//at 26:00
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
                        }else if (code.equals("bf")) {//bf 21:00
                                String[] split = cmd.split("\\s");
                                String[] ss0 = split[1].split(":");
                                int t0h = Integer.parseInt(ss0[0]);
                                int t0m = Integer.parseInt(ss0[1]);
                                int t0 = t0h * 3600 + t0m * 60;
                                int now = LocalTime.now().toSecondOfDay();
                                if (now < t0) {
                                        writeStatus("now within {},continue cmd", split[1]);
                                } else {
                                        writeStatus("now exceed {},break cmd", split[1]);
                                        break;
                                }

                        }else if(code.equals("c")){//c10,print hello
                                String[] sp = cmd.split(",");
                                int count= Integer.parseInt(sp[0].substring(1));
                                String[] subCmds = Arrays.copyOfRange(sp, 1, sp.length);
                                for (int i = 0; i < count; i++) {
                                        execute_cmd(subCmds,null);
                                }
                        }else if(code.equals("start")){
                                start(cmd.substring(code.length()));
                        }else if(code.equals("kill")){
                                kill(cmd.substring(code.length()));
                        }
                        else if(code.equals("print")){
                                writeStatus(cmd.split("\\s")[1]);
                        }else if(code.equals("keyevent")){
                                keyevent(cmd.substring(code.length()));
                        }
                        if (!sleeped) sleep(1);//操作间隔
                }
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

        private void watchAdv(Slot s) {
                this.sleep(s.interval);
                if(s.ex_cmds.contains(INS_PREB)) back();
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
                if (s.interval > 0) {
                        int times = s.total_duration / s.interval;
                        for (int i = 0; i < times + 1; i++) {
                                if (direction == 0) {
                                        if (cis_direction || i % 2 == 0)
                                                if(s.interval==3)
                                                        keyevent("KEYCODE_PAGE_DOWN");
                                                else
                                                        this.swipe(config.from);
                                        else
                                                if(s.interval==3)
                                                        keyevent("KEYCODE_PAGE_UP");
                                                else
                                                        this.swipe(config.to);
                                } else if (direction == 1)
                                        swipe(config.from+s.duration);
                                else if (direction == 2)
                                        swipe(config.to+s.duration);
                                else if (direction == 3)
                                        if (i % 2 == 0)
                                                this.swipe(config.from+s.duration);
                                        else
                                                this.swipe(config.to+s.duration);

                                this.sleep(s.interval);
                        }
                } else
                        this.sleep(s.total_duration);
                this.back();
                if (StrUtil.isNotBlank(s.ex_cmds)) {
                        execute_cmd(s.ex_cmds.split(";"), null);
                }
                this.sleep(s.time_slot);
        }

        void tap(String p) {
                sleep(2);
                String cmd = cmd_tap + randomPoint(p);
                String execForStr = RuntimeUtil.execForStr(cmd);
                if (StrUtil.isNotBlank(execForStr)) writeStatus(StrUtil.format("##execForStr tap:{} ", execForStr));
        }

        void start(String name) {
                String cmd = cmd_start+name;
                String execForStr = RuntimeUtil.execForStr(cmd);
                if (StrUtil.isNotBlank(execForStr)) writeStatus(StrUtil.format("##execForStr start:{} ", execForStr));
        }

        void kill(String name) {
                String cmd = cmd_kill +name;
                String execForStr = RuntimeUtil.execForStr(cmd);
                if (StrUtil.isNotBlank(execForStr)) writeStatus(StrUtil.format("##execForStr kill:{} ", execForStr));
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
                keyevent(cmd_keyevent + "KEYCODE_BACK");
        }

        void keyevent(String key){
                RuntimeUtil.execForStr(cmd_keyevent + key);
        }

        void sleep(double d) {
                long begin = System.currentTimeMillis();
                long millis = (long) (d * 1000);
                try {
                        Thread.sleep(millis);
                } catch (InterruptedException e) {
                        int cost_ms = (int) (System.currentTimeMillis() - begin);
                        //fixme
                        if(compensate>0){
                                int d1 = (int)d - cost_ms / 1000 + compensate;
                                compensate=0;
                                writeStatus("interrupt, continue g_slot sleep {}",d1);
                                sleep(d1);
                        }else if (millis > cost_ms) {
                                writeStatus("thread suspend");
                                thread_suspend = true;
                                thread.suspend();
                                writeStatus("thread resume,left:" + ((millis - cost_ms) / 1000));
                                sleep((int) ((millis - cost_ms) / 1000 + 1));
                        } else throw new IllegalStateException("millos<=cost_ms:" + millis + ":" + cost_ms);
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
        public static JTextArea getTextArea(ActionEvent e){
                Object source = e.getSource();
                JTextArea area=null;
                if(source instanceof JMenuItem)
                        area= (JTextArea) referencees.get(((Component)source).getParent());
                else if (source instanceof JButton || source instanceof javax.swing.Timer)
                        area = ((JTextArea) referencees.get(source));
                else area= (JTextArea) e.getSource();
                return area;
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
}
