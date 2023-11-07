package art.qiuye.diantao;

import art.qiuye.diantao.action.CtrlUpDownAction;
import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;

import javax.swing.*;
import javax.swing.text.PlainDocument;
import javax.swing.undo.UndoManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static art.qiuye.diantao.Constants.*;
import static art.qiuye.diantao.DIANTAO_UI.getTextArea;

/**
 * @author hy
 * @create 2023/9/29 0029
 */
public class ActionKeys {
    public static final KeyStroke CTRL_Z = KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK);
    public static final AbstractAction CTRL_Z_ACTION = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            JTextArea textArea = getTextArea(e);
            PlainDocument document = (PlainDocument) textArea.getDocument();
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
            JTextArea textArea = getTextArea(e);
            PlainDocument document = (PlainDocument) textArea.getDocument();
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
            JTextArea source = getTextArea(e);
            int linenumber = source.getLineOfOffset(source.getCaretPosition());
            int start = source.getLineStartOffset(linenumber);
            int end = source.getLineEndOffset(linenumber);
            String line = source.getText(start, end - start);
            if (!line.startsWith(BSY)) {
                if (line.startsWith(CMT))
                    source.replaceRange("", start, start + CMT.length());
                else source.insert(CMT, start);
            }
            end = source.getLineEndOffset(linenumber);
            source.setCaretPosition(end);
        }
    };
    public static final KeyStroke CTRL_D = KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK);
    public static final AbstractAction CTRL_D_ACTION = new AbstractAction() {
        @SneakyThrows
        @Override
        public void actionPerformed(ActionEvent e) {
            JTextArea source = getTextArea(e);
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
            JTextArea area = getTextArea(e);
            int linenumber = area.getLineOfOffset(area.getCaretPosition());
            int start = area.getLineStartOffset(linenumber);
            int end = area.getLineEndOffset(linenumber);
            area.replaceRange("", start, end);
        }
    };

    public static final KeyStroke ALT_SHIT_DOWN = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK);

    public static final AbstractAction ALT_SHIFT_DOWN_ACTION = new AbstractAction() {
        @SneakyThrows
        @Override
        public void actionPerformed(ActionEvent e) {
            JTextArea source = getTextArea(e);
            int n1 = source.getLineOfOffset(source.getCaretPosition());//当前行
            if (n1 == source.getLineCount() - 1) return;
            int n1_st = source.getLineStartOffset(n1);
            int n1_end = source.getLineEndOffset(n1);
            String line1 = source.getText(n1_st, n1_end - n1_st);//include \n
            int n2_st = n1_end;
            int n2_end = source.getLineEndOffset(n1 + 1);
            String line2 = source.getText(n2_st, n2_end - n2_st);
            if (n1 == source.getLineCount() - 2) {//倒数第二行
                line2 += LF;
                line1 = line1.replace(LF, "");
            }
            source.replaceRange(line2 + line1, n1_st, n2_end);
            source.setCaretPosition(n1_st + line2.length());
            source.requestFocus();
        }
    };

    public static final KeyStroke ALT_SHIT_UP = KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK);

    public static final AbstractAction ALT_SHIFT_UP_ACTION =new AbstractAction() {
        @SneakyThrows
        @Override
        public void actionPerformed(ActionEvent e) {
            JTextArea source = getTextArea(e);
            int n1 = source.getLineOfOffset(source.getCaretPosition());//当前行
            if (n1 == 0) return;
            int n1_st = source.getLineStartOffset(n1);
            int n1_end = source.getLineEndOffset(n1);
            String line1 = source.getText(n1_st, n1_end - n1_st);//include \n
            int n2_end = n1_st;
            int n2_st = source.getLineStartOffset(n1 - 1);
            String line2 = source.getText(n2_st, n2_end - n2_st);
            if (n1 == source.getLineCount() - 1) {//倒数第二行
                line1 += LF;
                line2 = line2.replace(LF, "");
            }
            source.replaceRange(line1 + line2, n2_st, n1_end);
            source.setCaretPosition(n2_st);
            source.requestFocus();
        }
    };

    public static final KeyStroke CTRL_UP = KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_MASK);

    public static final AbstractAction CTRL_UP_ACTION = new CtrlUpDownAction(1);
    public static final KeyStroke CTRL_DOWN = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_MASK);
    public static final AbstractAction CTRL_DOWN_ACTION = new CtrlUpDownAction(-1);
    public static final AbstractAction DELETE_ITEM_ACTION=new AbstractAction() {
        @SneakyThrows
        @Override
        public void actionPerformed(ActionEvent e) {
            JTextArea area = getTextArea(e);
            area.replaceSelection(null);
        }
    };

    public static final AbstractAction CUT_ITEM_ACTION=new AbstractAction() {
        @SneakyThrows
        @Override
        public void actionPerformed(ActionEvent e) {
            JTextArea area = getTextArea(e);
            area.cut();
        }
    };
    public static final AbstractAction COPY_ITEM_ACTION=new AbstractAction() {
        @SneakyThrows
        @Override
        public void actionPerformed(ActionEvent e) {
            JTextArea area = getTextArea(e);
            area.copy();
        }
    };
    public static final AbstractAction PASTE_ITEM_ACTION=new AbstractAction() {
        @SneakyThrows
        @Override
        public void actionPerformed(ActionEvent e) {
            JTextArea area = getTextArea(e);
            area.paste();
        }
    };
    public static final AbstractAction HEADER_ITEM_ACTION=new AbstractAction() {
        @SneakyThrows
        @Override
        public void actionPerformed(ActionEvent e) {
            JTextArea area = getTextArea(e);
            area.insert("[cmd];1;",area.getCaretPosition());
        }
    };
}
