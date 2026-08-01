import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.Painter;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.romraider.Settings;
import com.romraider.Version;
import com.romraider.maps.Rom;
import com.romraider.maps.RomID;
import com.romraider.maps.Table2D;
import com.romraider.maps.Table2DView;
import com.romraider.maps.Table3D;
import com.romraider.maps.Table3DView;
import com.romraider.swing.CustomToolbarLayout;
import com.romraider.swing.MDIDesktopPane;
import com.romraider.swing.RomCellRenderer;
import com.romraider.swing.RomTree;
import com.romraider.swing.TableFrame;
import com.romraider.swing.TableTreeNode;
import com.romraider.swing.TableToolBar;
import com.romraider.theme.DarkNimbusLookAndFeel;
import com.romraider.theme.HiDpiIconScaler;
import com.romraider.theme.MacNativeMenuBar;
import com.romraider.theme.RomRaiderBootstrap;
import com.romraider.theme.ThemePalette;
import com.romraider.util.SettingsManager;

public final class ThemeSmokeTest {
    private ThemeSmokeTest() {
        throw new UnsupportedOperationException();
    }

    public static void main(String[] args) throws Exception {
        useMinimalSettings();
        RomRaiderBootstrap.prepareMacLookAndFeel();
        verifyNonNativeMenuKeepsMnemonicUnderline();
        verifyLightDesktopColor();
        verifyLightLoggerTabs();
        UIManager.setLookAndFeel(new DarkNimbusLookAndFeel());
        verifyDarkDesktopContrast();
        verifyDarkLoggerMessageForeground();
        verifyBranding();
        verifyInternalMenuBarDefaults();
        verifyMapInternalFrame();

        Table2D table2D = new Table2D();
        table2D.setName("2D smoke test");
        Table2DView view2D = new Table2DView(table2D);
        assertName("2D smoke test", view2D.getName());
        verifyTableFrame(new TableFrame("2D map", view2D));

        Table3D table3D = new Table3D();
        table3D.setName("3D smoke test");
        Table3DView view3D = new Table3DView(table3D);
        assertName("3D smoke test", view3D.getName());
        verifyTableFrame(new TableFrame("3D map", view3D));
        verifyDetachedMapIcons(table3D);
        verifyTableFrameHandoff();

        verifyHiDpiIcon();
        verifyDarkControls();
        verifyToolbarWidths();
        verifyToolbarAlignment();
        verifyRomTreeClickHandling();
        verifyRomDescriptionDoesNotWrap();
        verifyToolbarBorders();
    }

    private static void verifyLightDesktopColor() throws Exception {
        Color desktop = ThemePalette.editorDesktopBackground();
        if (!Color.WHITE.equals(desktop)) {
            throw new AssertionError(
                    "Light editor desktop is not white: " + desktop);
        }
    }

    private static void verifyNonNativeMenuKeepsMnemonicUnderline() {
        String appleProperty = "apple.laf.useScreenMenuBar";
        String legacyProperty = "com.apple.macos.useScreenMenuBar";
        String previousAppleValue = System.getProperty(appleProperty);
        String previousLegacyValue = System.getProperty(legacyProperty);
        try {
            System.clearProperty(appleProperty);
            System.clearProperty(legacyProperty);
            JMenuBar menuBar = new JMenuBar();
            JMenu fileMenu = new JMenu("File");
            fileMenu.setMnemonic('F');
            fileMenu.setDisplayedMnemonicIndex(0);
            menuBar.add(fileMenu);

            MacNativeMenuBar.install(menuBar);
            if (fileMenu.getDisplayedMnemonicIndex() != 0) {
                throw new AssertionError(
                        "Non-native menu lost its mnemonic underline");
            }
        } finally {
            restoreProperty(appleProperty, previousAppleValue);
            restoreProperty(legacyProperty, previousLegacyValue);
        }
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    private static void verifyDarkDesktopContrast() {
        Color desktop = ThemePalette.editorDesktopBackground();
        Color mapSurface = UIManager.getColor("Panel.background");
        if (desktop == null
                || mapSurface == null
                || desktop.equals(mapSurface)
                || brightness(desktop) >= brightness(mapSurface)) {
            throw new AssertionError(
                    "Dark map surface does not contrast with editor desktop: "
                            + desktop + " / " + mapSurface);
        }
    }

    private static int brightness(Color color) {
        return color.getRed() + color.getGreen() + color.getBlue();
    }

    private static void verifyLightLoggerTabs() throws Exception {
        JTabbedPane tabs = new JTabbedPane();
        Class<?> loggerClass = Class.forName(
                "com.romraider.logger.ecu.EcuLogger",
                false,
                ThemeSmokeTest.class.getClassLoader());
        Method method = loggerClass.getDeclaredMethod(
                "useReadableTabForeground", JTabbedPane.class);
        method.setAccessible(true);
        method.invoke(null, tabs);
        if (tabs.getForeground() == null
                || tabs.getForeground() instanceof UIResource) {
            throw new AssertionError(
                    "Light logger tab foreground remains an Aqua UIResource");
        }
    }

    private static void verifyDarkLoggerMessageForeground() throws Exception {
        Class<?> loggerClass = Class.forName(
                "com.romraider.logger.ecu.EcuLogger",
                false,
                ThemeSmokeTest.class.getClassLoader());
        Method method = loggerClass.getDeclaredMethod(
                "readableLabelForeground");
        method.setAccessible(true);
        Color actual = (Color) method.invoke(null);
        Color expected = UIManager.getColor("Label.foreground");
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(
                    "Logger message does not use the dark label foreground: "
                            + actual);
        }
    }

    private static void useMinimalSettings() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        Method allocateInstance =
                unsafeClass.getMethod("allocateInstance", Class.class);
        Settings settings =
                (Settings) allocateInstance.invoke(unsafe, Settings.class);
        settings.setCellSize(new Dimension(42, 18));
        settings.setTableClickCount(1);

        Field field = SettingsManager.class.getDeclaredField("settings");
        field.setAccessible(true);
        field.set(null, settings);
    }

    private static void assertName(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(
                    "Expected component name " + expected + ", got " + actual);
        }
    }

    private static void verifyBranding() {
        if (!"RomRaiderHD".equals(Version.PRODUCT_NAME)) {
            throw new AssertionError(
                    "Unexpected product name " + Version.PRODUCT_NAME);
        }
        if (Version.ABOUT_ICON.getIconWidth() != 109
                || Version.ABOUT_ICON.getIconHeight() != 104) {
            throw new AssertionError("Unexpected RomRaiderHD app icon size");
        }
    }

    private static void verifyInternalMenuBarDefaults() {
        String uiClass = new JMenuBar().getUI().getClass().getName();
        if ("com.apple.laf.AquaMenuBarUI".equals(uiClass)) {
            throw new AssertionError(
                    "AquaMenuBarUI leaked into internal map windows");
        }
    }

    private static void verifyMapInternalFrame() {
        JInternalFrame frame =
                new JInternalFrame("Map", true, true, true, true);
        verifyTableFrame(frame);
    }

    private static void verifyTableFrame(JInternalFrame frame) {
        String uiClass = frame.getUI().getClass().getName();
        if (!"com.apple.laf.AquaInternalFrameUI".equals(uiClass)) {
            throw new AssertionError(
                    "Map window has unexpected UI delegate: " + uiClass);
        }
        JMenuBar menuBar = frame.getJMenuBar();
        if (menuBar != null) {
            String menuUiClass = menuBar.getUI().getClass().getName();
            if ("com.apple.laf.AquaMenuBarUI".equals(menuUiClass)) {
                throw new AssertionError(
                        "Map menu uses the top-level Aqua menu delegate");
            }
            frame.setSize(640, 480);
            frame.setVisible(true);
            frame.doLayout();
            BufferedImage image = new BufferedImage(
                    640, 480, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            frame.paint(graphics);
            graphics.dispose();
        }
        if (frame instanceof TableFrame) {
            verifyMacTableFrameFeatures((TableFrame) frame);
        }
    }

    private static void verifyMacTableFrameFeatures(TableFrame frame) {
        if (!frame.getBorder().getClass().getName()
                .endsWith("$RetinaTitleBorder")) {
            throw new AssertionError(
                    "Map title does not use the Retina border");
        }
        KeyStroke closeKey = KeyStroke.getKeyStroke(
                KeyEvent.VK_W, InputEvent.META_DOWN_MASK);
        Object actionKey = frame.getRootPane().getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .get(closeKey);
        Action action = actionKey == null ? null
                : frame.getRootPane().getActionMap().get(actionKey);
        if (action == null) {
            throw new AssertionError("Map has no Cmd+W close action");
        }
        try {
            Method activation = TableFrame.class.getDeclaredMethod(
                    "setMacCloseShortcutActive", boolean.class);
            activation.setAccessible(true);
            activation.invoke(frame, Boolean.TRUE);
            if (!closeKey.equals(
                    frame.getTableMenuBar().getClose().getAccelerator())) {
                throw new AssertionError(
                        "Active map does not expose the Cmd+W accelerator");
            }
            Object windowActionKey = frame.getRootPane().getInputMap(
                    JComponent.WHEN_IN_FOCUSED_WINDOW).get(closeKey);
            if (windowActionKey == null) {
                throw new AssertionError(
                        "Active map has no focused-window Cmd+W binding");
            }
            activation.invoke(frame, Boolean.FALSE);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(
                    "Cannot verify the active map shortcut", error);
        }
    }

    private static void verifyTableFrameHandoff() throws Exception {
        MDIDesktopPane desktop = new MDIDesktopPane();
        desktop.setSize(800, 600);

        Table2D firstTable = new Table2D();
        firstTable.setName("First map");
        TableFrame firstFrame = new TableFrame(
                "First map", new Table2DView(firstTable));
        firstFrame.removeInternalFrameListener(firstFrame);

        Table2D secondTable = new Table2D();
        secondTable.setName("Second map");
        TableFrame secondFrame = new TableFrame(
                "Second map", new Table2DView(secondTable));
        secondFrame.removeInternalFrameListener(secondFrame);

        desktop.add(firstFrame);
        desktop.add(secondFrame);
        desktop.activateTableFrame(firstFrame);
        if (desktop.getComponentZOrder(firstFrame) != 0) {
            throw new AssertionError("First map was not moved to front");
        }
        TableFrame nextFrame = desktop.closeTableFrame(firstFrame);
        if (firstTable.getTableFrame() != null) {
            throw new AssertionError(
                    "Closed map remains attached to its table");
        }
        if (nextFrame != secondFrame) {
            throw new AssertionError("Next open map was not selected");
        }

        desktop.activateTableFrame(nextFrame);
        if (desktop.getComponentZOrder(secondFrame) != 0) {
            throw new AssertionError(
                    "Next open map was not moved to front");
        }
        desktop.closeTableFrame(secondFrame);

        Table2D detachedTable = new Table2D();
        detachedTable.setName("Detached map");
        TableFrame detachedFrame = new TableFrame(
                "Detached map", new Table2DView(detachedTable));
        detachedFrame.dispose();
        if (detachedTable.getTableFrame() != null) {
            throw new AssertionError(
                    "Disposed map remains attached to its table");
        }
    }

    private static void verifyHiDpiIcon() throws Exception {
        BufferedImage source =
                new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
        ImageIcon icon =
                HiDpiIconScaler.scale(new ImageIcon(source), 50);
        if (icon.getIconWidth() != 24 || icon.getIconHeight() != 24) {
            throw new AssertionError("Unexpected logical icon dimensions");
        }

        Image denseVariant =
                getResolutionVariant(icon.getImage(), 48, 48);
        if (denseVariant.getWidth(null) != 48 ||
                denseVariant.getHeight(null) != 48) {
            throw new AssertionError("Unexpected Retina icon dimensions");
        }

        BufferedImage originalSource =
                new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        ImageIcon original =
                HiDpiIconScaler.original(
                        new ImageIcon(originalSource), 16, 16);
        Image nativeVariant =
                getResolutionVariant(original.getImage(), 16, 16);
        if (nativeVariant != originalSource) {
            throw new AssertionError(
                    "Original artwork is replaced at its native size");
        }
        Image upscaledVariant =
                getResolutionVariant(original.getImage(), 32, 32);
        if (upscaledVariant.getWidth(null) != 32
                || upscaledVariant.getHeight(null) != 32) {
            throw new AssertionError(
                    "Original artwork has no dense Retina variant");
        }
    }

    private static Image getResolutionVariant(Image image, int width,
            int height) {
        return ((MultiResolutionImage) image).getResolutionVariant(
                width, height);
    }

    private static void verifyDetachedMapIcons(Table3D table) {
        ImageIcon first = RomCellRenderer.getIconForTable(table);
        ImageIcon second = RomCellRenderer.getIconForTable(table);
        if (first == second) {
            throw new AssertionError(
                    "Tree and map title share a mutable ImageIcon");
        }

        first.setImage(new BufferedImage(
                7, 7, BufferedImage.TYPE_INT_ARGB));
        ImageIcon third = RomCellRenderer.getIconForTable(table);
        if (third.getIconWidth() != 20 || third.getIconHeight() != 20) {
            throw new AssertionError(
                    "Map title resizing changed the tree icon");
        }
    }

    @SuppressWarnings("unchecked")
    private static void verifyDarkControls() {
        Dimension buttonSize =
                (Dimension) UIManager.get("ScrollBar.buttonSize");
        if (!new Dimension(0, 0).equals(buttonSize)) {
            throw new AssertionError("Scrollbar arrow buttons are visible");
        }

        Painter<JComponent> comboPainter =
                (Painter<JComponent>) UIManager.get(
                        "ComboBox[Enabled].backgroundPainter");
        assertPaintsColor(
                comboPainter, new Color(58, 65, 68), 80, 24,
                "Combo box background");

        Painter<JComponent> buttonPainter =
                (Painter<JComponent>) UIManager.get(
                        "Button[Enabled].backgroundPainter");
        assertPaintsColor(
                buttonPainter, new Color(58, 65, 68), 80, 24,
                "Button background");
        assertContainsColor(
                buttonPainter, new Color(89, 97, 100), 80, 24,
                "Button border");

        Painter<JComponent> fieldPainter =
                (Painter<JComponent>) UIManager.get(
                        "FormattedTextField[Enabled].backgroundPainter");
        assertPaintsColor(
                fieldPainter, new Color(58, 65, 68), 80, 24,
                "Formatted text field background");

        Painter<JComponent> focusedFieldBorder =
                (Painter<JComponent>) UIManager.get(
                        "FormattedTextField[Focused].borderPainter");
        assertContainsColor(
                focusedFieldBorder, new Color(83, 155, 248), 80, 24,
                "Formatted text field focus border");

        Painter<JComponent> arrowPainter =
                (Painter<JComponent>) UIManager.get(
                        "ArrowButton[Enabled].foregroundPainter");
        assertContainsColor(
                arrowPainter, new Color(220, 227, 231), 24, 19,
                "Combo box arrow");

        Painter<JComponent> tabPainter =
                (Painter<JComponent>) UIManager.get(
                        "TabbedPane:TabbedPaneTab[Enabled]"
                                + ".backgroundPainter");
        assertPaintsColor(
                tabPainter, new Color(58, 65, 68), 80, 24,
                "Unselected tab background");

        Painter<JComponent> selectedTabPainter =
                (Painter<JComponent>) UIManager.get(
                        "TabbedPane:TabbedPaneTab[Selected]"
                                + ".backgroundPainter");
        assertPaintsColor(
                selectedTabPainter, new Color(62, 142, 246), 80, 24,
                "Selected tab background");
    }

    private static void verifyToolbarWidths() throws Exception {
        TableToolBar toolbar = new TableToolBar();
        assertPreferredWidth(toolbar, "incrementByFine", 64);
        assertPreferredWidth(toolbar, "incrementByCoarse", 64);
        assertPreferredWidth(toolbar, "setValueText", 64);
        assertPreferredWidth(toolbar, "scaleSelection", 120);
        assertPreferredWidth(toolbar, "clearOverlay", 120);
    }

    private static void assertPreferredWidth(TableToolBar toolbar,
            String fieldName, int expectedWidth) throws Exception {
        Field field = TableToolBar.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        Component component = (Component) field.get(toolbar);
        int actualWidth = component.getPreferredSize().width;
        if (actualWidth != expectedWidth) {
            throw new AssertionError(
                    fieldName + " has width " + actualWidth
                            + ", expected " + expectedWidth);
        }
    }

    private static void verifyToolbarAlignment() {
        JPanel panel = new JPanel(new CustomToolbarLayout(
                FlowLayout.LEFT, 0, 0, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        JPanel toolbar = new JPanel();
        toolbar.setPreferredSize(new Dimension(80, 20));
        panel.add(toolbar);
        panel.setSize(200, 31);
        panel.doLayout();

        if (toolbar.getX() != 8) {
            throw new AssertionError(
                    "Toolbar left position is " + toolbar.getX()
                            + ", expected 8");
        }
        if (toolbar.getY() != 8) {
            throw new AssertionError(
                    "Toolbar vertical position is " + toolbar.getY()
                            + ", expected 8");
        }

        panel.setSize(200, 20);
        panel.doLayout();
        if (toolbar.getY() != 2) {
            throw new AssertionError(
                    "Toolbar optical offset is " + toolbar.getY()
                            + ", expected 2");
        }
    }

    private static void verifyRomTreeClickHandling() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode row = new DefaultMutableTreeNode("row");
        root.add(row);

        TestRomTree tree = new TestRomTree(root);
        tree.setSize(320, 100);
        tree.expandPath(new TreePath(root.getPath()));
        tree.doLayout();

        Rectangle bounds = tree.getRowBounds(0);
        if (bounds == null) {
            throw new AssertionError("ROM tree row has no bounds");
        }
        int x = Math.min(tree.getWidth() - 1,
                bounds.x + bounds.width + 80);
        int y = bounds.y + bounds.height / 2;
        if (tree.getPathForLocation(x, y) != null) {
            throw new AssertionError(
                    "ROM tree test point is not outside the label");
        }

        MouseEvent singleClick = new MouseEvent(
                tree, MouseEvent.MOUSE_CLICKED, 0, 0,
                x, y, 1, false, MouseEvent.BUTTON1);
        tree.sendMouseEvent(new MouseEvent(
                tree, MouseEvent.MOUSE_PRESSED, 0, 0,
                x, y, 1, false, MouseEvent.BUTTON1));
        tree.sendMouseEvent(new MouseEvent(
                tree, MouseEvent.MOUSE_RELEASED, 0, 0,
                x, y, 1, false, MouseEvent.BUTTON1));
        tree.sendMouseEvent(singleClick);
        if (tree.getLastSelectedPathComponent() != row) {
            throw new AssertionError(
                    "ROM tree row whitespace is not clickable");
        }

        Method clickCheck = RomTree.class.getDeclaredMethod(
                "isConfiguredTableClick", MouseEvent.class);
        clickCheck.setAccessible(true);
        if (!Boolean.TRUE.equals(clickCheck.invoke(tree, singleClick))) {
            throw new AssertionError("Single table click was ignored");
        }

        MouseEvent doubleClick = new MouseEvent(
                tree, MouseEvent.MOUSE_CLICKED, 0, 0,
                x, y, 2, false, MouseEvent.BUTTON1);
        if (!Boolean.TRUE.equals(clickCheck.invoke(tree, doubleClick))) {
            throw new AssertionError(
                    "A continued click sequence was ignored");
        }

        Settings settings = SettingsManager.getSettings();
        settings.setTableClickCount(2);
        if (!Boolean.FALSE.equals(clickCheck.invoke(tree, singleClick))
                || !Boolean.TRUE.equals(clickCheck.invoke(tree, doubleClick))) {
            throw new AssertionError(
                    "Double-click table setting was not respected");
        }
        settings.setTableClickCount(1);

        verifyRomTreePressPathIsStable();
        verifySequentialTablePresses();
    }

    private static void verifyRomTreePressPathIsStable() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode first = new DefaultMutableTreeNode("first");
        DefaultMutableTreeNode pressed =
                new DefaultMutableTreeNode("pressed");
        root.add(first);
        root.add(pressed);

        TestRomTree tree = new TestRomTree(root);
        tree.setSize(320, 120);
        tree.expandPath(new TreePath(root.getPath()));
        tree.doLayout();

        Rectangle bounds = tree.getRowBounds(1);
        if (bounds == null) {
            throw new AssertionError("ROM tree pressed row has no bounds");
        }
        int x = tree.getWidth() - 1;
        int y = bounds.y + bounds.height / 2;
        tree.sendMouseEvent(new MouseEvent(
                tree, MouseEvent.MOUSE_PRESSED, 0, 0,
                x, y, 1, false, MouseEvent.BUTTON1));

        DefaultMutableTreeNode inserted =
                new DefaultMutableTreeNode("inserted");
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        model.insertNodeInto(inserted, root, 0);
        tree.doLayout();

        tree.sendMouseEvent(new MouseEvent(
                tree, MouseEvent.MOUSE_RELEASED, 0, 0,
                x, y, 1, false, MouseEvent.BUTTON1));
        tree.sendMouseEvent(new MouseEvent(
                tree, MouseEvent.MOUSE_CLICKED, 0, 0,
                x, y, 1, false, MouseEvent.BUTTON1));
        if (tree.getLastSelectedPathComponent() != pressed) {
            throw new AssertionError(
                    "ROM tree click followed the shifted release row");
        }
    }

    private static void verifySequentialTablePresses() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        TestRomTree tree = new TestRomTree(root);
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setSize(340, 160);
        scrollPane.doLayout();
        tree.setSize(320, 140);

        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        RomID romID = new RomID();
        Rom rom = new Rom(romID);
        model.insertNodeInto(rom, root, root.getChildCount());

        Table2D firstTable = new Table2D();
        firstTable.setName("First table");
        TableTreeNode firstNode = new TableTreeNode(firstTable);
        model.insertNodeInto(firstNode, rom, rom.getChildCount());

        Table2D secondTable = new Table2D();
        secondTable.setName("Second table");
        TableTreeNode secondNode = new TableTreeNode(secondTable);
        model.insertNodeInto(secondNode, rom, rom.getChildCount());

        tree.expandPath(new TreePath(root.getPath()));
        tree.expandPath(new TreePath(rom.getPath()));
        tree.doLayout();

        pressTreeRow(tree, 1, 1);
        if (tree.getShownTableCount() != 1
                || tree.getLastShownTable() != firstNode) {
            throw new AssertionError(
                    "First table did not open on mouse press");
        }

        pressTreeRow(tree, 2, 2);
        if (tree.getShownTableCount() != 2
                || tree.getLastShownTable() != secondNode) {
            throw new AssertionError(
                    "Sequential table press was ignored");
        }

        Settings settings = SettingsManager.getSettings();
        settings.setTableClickCount(2);
        pressTreeRow(tree, 1, 1);
        if (tree.getShownTableCount() != 2
                || tree.getLastSelectedNode() != firstNode) {
            throw new AssertionError(
                    "First click did not select the table ROM");
        }
        settings.setTableClickCount(1);
    }

    private static void pressTreeRow(TestRomTree tree, int row,
            int clickCount) {
        Rectangle bounds = tree.getRowBounds(row);
        if (bounds == null) {
            throw new AssertionError("ROM tree table row has no bounds");
        }
        int x = tree.getWidth() - 1;
        int y = bounds.y + bounds.height / 2;
        tree.sendMouseEvent(new MouseEvent(
                tree, MouseEvent.MOUSE_PRESSED, 0, 0,
                x, y, clickCount, false, MouseEvent.BUTTON1));
        tree.sendMouseEvent(new MouseEvent(
                tree, MouseEvent.MOUSE_RELEASED, 0, 0,
                x, y, clickCount, false, MouseEvent.BUTTON1));
        tree.sendMouseEvent(new MouseEvent(
                tree, MouseEvent.MOUSE_CLICKED, 0, 0,
                x, y, clickCount, false, MouseEvent.BUTTON1));
    }

    private static final class TestRomTree extends RomTree {
        private static final long serialVersionUID = 1L;
        private int shownTableCount;
        private TableTreeNode lastShownTable;
        private Object lastSelectedNode;

        private TestRomTree(DefaultMutableTreeNode root) {
            super(root);
        }

        private void sendMouseEvent(MouseEvent event) {
            super.processMouseEvent(event);
        }

        @Override
        protected void showTable(TableTreeNode selectedRow) {
            shownTableCount++;
            lastShownTable = selectedRow;
        }

        @Override
        protected void setLastSelectedRom(Object selectedNode) {
            lastSelectedNode = selectedNode;
        }

        private int getShownTableCount() {
            return shownTableCount;
        }

        private TableTreeNode getLastShownTable() {
            return lastShownTable;
        }

        private Object getLastSelectedNode() {
            return lastSelectedNode;
        }
    }

    private static void assertPaintsColor(Painter<JComponent> painter,
            Color expected, int width, int height, String description) {
        BufferedImage image = paint(painter, width, height);
        Color actual =
                new Color(image.getRGB(width / 2, height / 2), true);
        if (!expected.equals(actual)) {
            throw new AssertionError(
                    description + " has unexpected color " + actual);
        }
    }

    private static void assertContainsColor(Painter<JComponent> painter,
            Color expected, int width, int height, String description) {
        BufferedImage image = paint(painter, width, height);
        int expectedRgb = expected.getRGB();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (image.getRGB(x, y) == expectedRgb) {
                    return;
                }
            }
        }
        throw new AssertionError(description + " is not visible");
    }

    private static BufferedImage paint(Painter<JComponent> painter,
            int width, int height) {
        if (painter == null) {
            throw new AssertionError("Missing theme painter");
        }
        BufferedImage image = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        painter.paint(graphics, new JPanel(), width, height);
        graphics.dispose();
        return image;
    }

    private static void verifyRomDescriptionDoesNotWrap() {
        RomID id = new RomID();
        id.setVersion("VQ25HR");
        id.setMake("Nissan");
        id.setModel("Skyline 250GT with a deliberately long description");
        Rom rom = new Rom(id);
        rom.setFileName("1JK08A-stock.bin");

        JTree tree = new JTree(rom);
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setSize(180, 200);
        scrollPane.doLayout();
        tree.setSize(170, 200);

        Component component =
                new RomCellRenderer().getTreeCellRendererComponent(
                        tree, rom, false, true, false, 0, false);
        JPanel panel = (JPanel) component;
        JLabel description = (JLabel) panel.getComponent(1);
        View htmlView =
                (View) description.getClientProperty(BasicHTML.propertyKey);
        htmlView.setSize(80, 100);

        float lineHeight =
                description.getFontMetrics(description.getFont()).getHeight();
        if (htmlView.getPreferredSpan(View.Y_AXIS) > lineHeight + 1) {
            throw new AssertionError("ROM description wraps");
        }
        if (panel.getPreferredSize().width <= tree.getParent().getWidth()) {
            throw new AssertionError(
                    "ROM description cannot extend the tree width");
        }
    }

    private static void verifyToolbarBorders() throws Exception {
        TableToolBar toolbar = new TableToolBar();
        if (toolbar.isFloatable()) {
            throw new AssertionError(
                    "Table toolbar exposes a Nimbus drag handle");
        }
        String[] fields = {
            "incrementFine",
            "decrementFine",
            "incrementCoarse",
            "decrementCoarse",
            "enable3d",
            "setValue",
            "multiply",
            "colorCells",
            "refreshCompare",
            "clearOverlay"
        };
        for (String fieldName : fields) {
            Field field = TableToolBar.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            JButton button = (JButton) field.get(toolbar);
            if (!(button.getBorder() instanceof EmptyBorder)) {
                throw new AssertionError(
                        fieldName + " has a visible outer border");
            }
            Insets insets = button.getBorder().getBorderInsets(button);
            if (insets.top != 1 || insets.left != 1
                    || insets.bottom != 1 || insets.right != 1) {
                throw new AssertionError(
                        fieldName + " has unexpected border insets");
            }
        }
    }
}
